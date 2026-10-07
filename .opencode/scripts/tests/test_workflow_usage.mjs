import test from "node:test"
import assert from "node:assert/strict"
import { readFile, access } from "node:fs/promises"
import { tmpdir } from "node:os"
import path from "node:path"
import { createOpencodeClient } from "@opencode-ai/sdk/client"
import { Service } from "@opencode/client/service"
import plugin from "../../plugins/workflow-usage.js"
const WorkflowUsage = plugin.server
import { measurementExport, nativeMeasurements, nativeUsageBridge, localUsageServerUrl } from "../workflow_usage_bridge.mjs"

const directory = path.join(tmpdir(), "workflow-project")
const sessionID = "ses_actualActive"
const context = { sessionID, directory, abort: new AbortController().signal }
const message = (id = "msg_one") => ({
  info: { id, role: "assistant", providerID: "test", modelID: "test-model",
    time: { created: 1000, completed: 3000 }, cost: 0.01,
    tokens: { input: 100, output: 20, reasoning: 5, cache: { read: 30, write: 2 } },
    system: ["SECRET"], summary: "SECRET", extra: "SECRET" },
  parts: [{ type: "text", text: "SECRET" },
    { type: "tool", state: { input: "SECRET", output: "SECRET" } },
    { type: "reasoning", text: "SECRET", time: { start: 1200, end: 2000 } },
    { type: "step-finish", cost: 0.01, tokens: { input: 100, output: 20, reasoning: 5,
      cache: { read: 30, write: 2, extra: "SECRET" }, extra: "SECRET" }, snapshot: "SECRET" },
    { type: "retry", error: { message: "SECRET" } }],
})

function clientFor(messages = [message()], info = { id: sessionID, directory }, status = 200) {
  const requests = []
  const client = createOpencodeClient({ baseUrl: "http://actual-active-server.invalid",
    headers: { authorization: "Basic fixture-only" },
    fetch: async (request) => {
      const url = new URL(request.url)
      requests.push(request)
      assert.equal(url.origin, "http://actual-active-server.invalid")
      assert.equal(url.searchParams.get("directory"), directory)
      assert.equal(url.searchParams.has("limit"), false)
      assert.equal(request.headers.get("authorization"), "Basic fixture-only")
      const body = status !== 200 ? { name: "NotFoundError", data: { message: "Session not found" } }
        : url.pathname.endsWith("/message") ? messages : info
      return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } })
    },
  })
  return { client, requests }
}

test("SDK retains active backend and auth; complete history and privacy preserved", async () => {
    const history = Array.from({ length: 151 }, (_, i) => message(`msg_${i}`))
    history.unshift({ info: { id: "msg_user", role: "user" }, parts: [{ type: "text", text: "SECRET" }] })
    const { client, requests } = clientFor(history)
    const hooks = await WorkflowUsage({ client })
    try {
      const result = JSON.parse(await hooks.tool.workflow_usage_snapshot.execute({}, context))
      assert.equal(result.session_id, sessionID)
      const data = JSON.parse(await readFile(result.usage_export, "utf8"))
      assert.equal(data.messages.length, 151)
      assert.equal(data.info.id, sessionID)
      assert.equal(data.workflowUsage.capturedMs, result.captured_ms)
      assert.equal(data.messages[0].parts[1].tokens.reasoning, 5)
      assert.equal(data.messages[0].parts[2].type, "retry")
      assert.equal(JSON.stringify(data).includes("SECRET"), false)
      assert.equal(JSON.stringify(data).includes("fixture-only"), false)
      assert.deepEqual(requests.map((request) => new URL(request.url).pathname).sort(),
        [`/session/${sessionID}`, `/session/${sessionID}/message`])
      await hooks.dispose()
      await assert.rejects(access(result.usage_export))
    } finally { await hooks.dispose() }
})

test("malformed counters cannot smuggle text or nested data into metric files", () => {
  const malformed = message()
  malformed.info.modelID = { secret: "SECRET" }
  malformed.info.cost = "SECRET"
  malformed.parts[2].time.start = { secret: "SECRET" }
  malformed.parts[3].tokens.input = { secret: "SECRET" }
  const measured = measurementExport({ id: sessionID, directory }, [malformed], sessionID, directory, Date.now())
  assert.equal(JSON.stringify(measured).includes("SECRET"), false)
  assert.equal(measured.messages[0].parts[1].tokens.input, null)
})

test("simultaneous sessions retain distinct IDs and temp exports", async () => {
  const client = { session: {
    get: async (options) => ({ data: { id: options.path.id, directory: options.query.directory } }),
    messages: async () => ({ data: [] }),
  } }
  const hooks = await WorkflowUsage({ client })
  try {
    const captures = await Promise.all(["ses_first", "ses_second"].map((id) =>
      hooks.tool.workflow_usage_snapshot.execute({}, { ...context, sessionID: id }).then(JSON.parse)))
    assert.notEqual(captures[0].usage_export, captures[1].usage_export)
    for (const capture of captures)
      assert.equal(JSON.parse(await readFile(capture.usage_export, "utf8")).info.id, capture.session_id)
  } finally { await hooks.dispose() }
})

test("each invocation produces a fresh export", async () => {
  const hooks = await WorkflowUsage(clientFor())
  try {
    const first = JSON.parse(await hooks.tool.workflow_usage_snapshot.execute({}, context))
    const second = JSON.parse(await hooks.tool.workflow_usage_snapshot.execute({}, context))
    assert.notEqual(first.usage_export, second.usage_export)
    assert.ok(second.captured_ms >= first.captured_ms)
  } finally { await hooks.dispose() }
})

test("unavailable active session fails instead of querying another backend", async () => {
  const hooks = await WorkflowUsage(clientFor([], undefined, 404))
  try { await assert.rejects(hooks.tool.workflow_usage_snapshot.execute({}, context)) }
  finally { await hooks.dispose() }
})

test("wrong session, wrong project, incomplete history and duplicate messages fail", () => {
  for (const [info, messages] of [
    [{ id: "ses_other", directory }, []],
    [{ id: sessionID, directory: path.join(directory, "other") }, []],
    [{ id: sessionID, directory }, undefined],
    [{ id: sessionID, directory }, [message(), message()]],
    [{ id: sessionID, directory }, [{ info: message().info }]],
  ]) assert.throws(() => measurementExport(info, messages, sessionID, directory, Date.now()))
})

test("invalid session context is rejected before any server call", async () => {
  const hooks = await WorkflowUsage({ client: { session: {} } })
  try {
    await assert.rejects(hooks.tool.workflow_usage_snapshot.execute({}, { ...context, sessionID: undefined }))
    await assert.rejects(hooks.tool.workflow_usage_snapshot.execute({}, { ...context, directory: undefined }))
  } finally { await hooks.dispose() }
})

const nativeMessage = (id = "msg_v2") => ({
  id, type: "assistant", model: { providerID: "test", id: "test-model" },
  time: { created: 1000, completed: 3000 }, cost: .01,
  tokens: { input: 100, output: 20, reasoning: 5, cache: { read: 30, write: 2 } },
  content: [{ type: "reasoning", text: "SECRET", time: { created: 1200, completed: 2000 } },
    { type: "tool", state: { input: "SECRET", content: "SECRET" } }],
  providerState: { token: "SECRET" }, snapshot: "SECRET",
})

test("V2 definition registers native tool and returns its cleanup", async () => {
  let definition
  const cleanup = await plugin.setup({ tool: { transform: async (callback) => callback({ add: (d) => { definition = d } }) } })
  assert.equal(plugin.id, "workflow-usage")
  assert.equal(definition.name, "workflow_usage_snapshot")
  assert.deepEqual(definition.input, { type: "object", properties: {}, additionalProperties: false })
  assert.equal(definition.options.codemode, false)
  await cleanup()
})

test("V2 preserves full history including completed compaction; whitelist removes content", async () => {
  const history = Array.from({ length: 151 }, (_, i) => nativeMessage(`msg_${i}`))
  history.splice(50, 0, { id: "msg_compacted", type: "compaction", status: "completed",
    model: { providerID: "test", id: "test-model" }, time: { created: 1500 }, cost: .2,
    tokens: { input: 192185, output: 1354, reasoning: 0, cache: { read: 5376, write: 0 } }, summary: "SECRET" })
  const data = { info: { id: sessionID, location: { directory } }, messages: history }
  const calls = []
  const ctx = { location: { directory }, session: { get: async (input) => {
    calls.push(input.sessionID); return data.info
  } } }
  const bridge = nativeUsageBridge(ctx, async () => ({ session: { export: async (input) => {
    calls.push(input.sessionID); return data
  } } }))
  try {
    const result = JSON.parse(await bridge.capture({ sessionID, signal: new AbortController().signal }))
    const exported = JSON.parse(await readFile(result.usage_export, "utf8"))
    assert.equal(exported.messages.length, 152)
    assert.equal(exported.messages[0].parts[0].time.start, 1200)
    assert.equal(exported.messages[50].info.completed, true)
    assert.equal(exported.messages[50].info.time.completed, undefined)
    assert.equal(exported.messages[0].info.retryEvents, null)
    assert.equal(JSON.stringify(exported).includes("SECRET"), false)
    assert.deepEqual(calls, [sessionID, sessionID])
  } finally { await bridge.dispose() }
})

test("V2 rejects malformed content, wrong active location and mismatched exported session", async () => {
  assert.throws(() => nativeMeasurements({ info: {}, messages: [ { ...nativeMessage(), content: undefined } ] }))
  let connected = false
  const bridge = nativeUsageBridge({ location: { directory }, session: {
    get: async () => ({ location: { directory: path.join(directory, "other") } }),
  } }, async () => { connected = true })
  try { await assert.rejects(bridge.capture({ sessionID })); assert.equal(connected, false) }
  finally { await bridge.dispose() }
  const bad = nativeMeasurements({ info: { id: "ses_other", location: { directory } }, messages: [] })
  assert.throws(() => measurementExport(bad.info, bad.messages, sessionID, directory, Date.now()))
})

test("V2 public client verifies server process before exporting the active session", async () => {
  const originalFetch = globalThis.fetch
  const requests = []
  let serverPid = process.pid + 1
  const ctx = { app: { version: "2.0.19" }, location: { directory },
    options: { serverUrl: "http://127.0.0.1:43219" },
    session: { get: async () => ({ id: sessionID, location: { directory } }) },
  }
  globalThis.fetch = async (url) => {
    requests.push(new URL(url).pathname)
    assert.equal(new URL(url).origin, ctx.options.serverUrl)
    const data = new URL(url).pathname === "/api/info"
      ? { pid: serverPid, version: ctx.app.version, urls: [], paths: { tmp: tmpdir() } }
      : { data: { info: { id: sessionID, location: { directory } }, messages: [nativeMessage()] } }
    return new Response(JSON.stringify(data), { headers: { "content-type": "application/json" } })
  }
  const bridge = nativeUsageBridge(ctx)
  try {
    await assert.rejects(bridge.capture({ sessionID }), /active OpenCode server process/)
    assert.deepEqual(requests, ["/api/info"])
    serverPid = process.pid
    const capture = JSON.parse(await bridge.capture({ sessionID }))
    const measured = JSON.parse(await readFile(capture.usage_export, "utf8"))
    assert.equal(measured.messages[0].info.tokens.reasoning, 5)
    assert.deepEqual(requests, ["/api/info", "/api/info", `/api/experimental/session/${sessionID}/export`])
  } finally { globalThis.fetch = originalFetch; await bridge.dispose() }
})

test("usage maps wildcard binds to loopback without altering port or path", () => {
  for (const [input, expected] of [
    ["http://0.0.0.0:49374", "http://127.0.0.1:49374/"],
    ["http://[::]:49374", "http://[::1]:49374/"],
    ["https://0.0.0.0:43219/base?x=1", "https://127.0.0.1:43219/base?x=1"],
    ["http://127.0.0.1:43219", "http://127.0.0.1:43219/"],
    ["http://localhost:43219", "http://localhost:43219/"],
    ["http://[::1]:43219", "http://[::1]:43219/"],
  ]) assert.equal(localUsageServerUrl(input), expected)
})

test("wildcard support does not permit LAN, remote or non-HTTP endpoints", () => {
  for (const value of ["http://192.168.1.20:49374", "http://10.0.0.10:49374",
    "https://example.com", "http://127.0.0.1.example.com", "http://[2001:db8::1]",
    "ftp://0.0.0.0:49374", "file:///tmp/test", "not a URL"])
    assert.throws(() => localUsageServerUrl(value))
})

test("Pair service discovery connects locally, preserves auth and rejects another process/version", async () => {
  const originalFetch = globalThis.fetch
  const originalDiscover = Service.discover
  let info
  const requests = []
  const ctx = { app: { version: "2.0.19" }, location: { directory },
    session: { get: async () => ({ id: sessionID, location: { directory } }) } }
  const endpoint = { url: "http://0.0.0.0:43219", auth: { type: "basic", username: "opencode", password: "fixture-only" } }
  const originalEndpoint = { ...endpoint }
  const expectedHeaders = Service.headers(endpoint)
  Service.discover = async (options) => {
    assert.equal(options.version, ctx.app.version)
    return endpoint
  }
  globalThis.fetch = async (url, options) => {
    const parsed = new URL(url)
    assert.equal(parsed.origin, "http://127.0.0.1:43219")
    const headers = new Headers(options.headers)
    for (const [key, value] of Object.entries(expectedHeaders)) assert.equal(headers.get(key), value)
    requests.push(parsed.pathname)
    return new Response(JSON.stringify(parsed.pathname === "/api/info" ? info : {
      data: { info: { id: sessionID, location: { directory } }, messages: [nativeMessage()] },
    }), { headers: { "content-type": "application/json" } })
  }
  const bridge = nativeUsageBridge(ctx)
  try {
    info = { pid: process.pid + 1, version: ctx.app.version }
    await assert.rejects(bridge.capture({ sessionID }), /active OpenCode server process/)
    info = { pid: process.pid, version: "2.0.20" }
    await assert.rejects(bridge.capture({ sessionID }), /active OpenCode server process/)
    assert.deepEqual(requests, ["/api/info", "/api/info"])
    info = { pid: process.pid, version: ctx.app.version }
    const capture = JSON.parse(await bridge.capture({ sessionID }))
    const measured = JSON.parse(await readFile(capture.usage_export, "utf8"))
    assert.equal(measured.messages[0].info.tokens.input, 100)
    assert.equal(JSON.stringify(measured).includes("fixture-only"), false)
    assert.deepEqual(endpoint, originalEndpoint)
    assert.deepEqual(requests, ["/api/info", "/api/info", "/api/info", `/api/experimental/session/${sessionID}/export`])
  } finally {
    globalThis.fetch = originalFetch
    Service.discover = originalDiscover
    await bridge.dispose()
  }
})
