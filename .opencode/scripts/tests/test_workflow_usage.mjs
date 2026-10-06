import test from "node:test"
import assert from "node:assert/strict"
import { readFile, access } from "node:fs/promises"
import { tmpdir } from "node:os"
import path from "node:path"
import { createOpencodeClient } from "@opencode-ai/sdk/client"
import { WorkflowUsage } from "../../plugins/workflow-usage.js"
import { measurementExport } from "../workflow_usage_bridge.mjs"

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
