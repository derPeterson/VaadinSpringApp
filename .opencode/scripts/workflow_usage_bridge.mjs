/** Active-server adapter for Python usage. Only measurement fields leave memory. */
import { mkdtemp, writeFile, rm } from "node:fs/promises"
import { tmpdir } from "node:os"
import path from "node:path"

const pick = (value, keys) => Object.fromEntries(
  keys.filter((key) => value?.[key] !== undefined).map((key) => [key, value[key]]),
)
const numeric = (value, keys) => Object.fromEntries(Object.entries(pick(value, keys)).map(
  ([key, count]) => [key, typeof count === "number" && Number.isFinite(count) && count >= 0 ? count : null],
))
const strings = (value, keys) => Object.fromEntries(Object.entries(pick(value, keys)).filter(
  ([_key, content]) => typeof content === "string",
))
const timing = (value) => numeric(value, ["created", "completed", "start", "end"])
const normalizedDirectory = (value) => process.platform === "win32"
  ? path.resolve(value).toLowerCase() : path.resolve(value)
const counters = (value) => ({
  ...numeric(value, ["cost"]),
  ...(value?.tokens ? { tokens: {
    ...numeric(value.tokens, ["input", "output", "reasoning"]),
    cache: numeric(value.tokens.cache, ["read", "write"]),
  } } : {}),
})

export function measurementExport(info, messages, sessionID, directory, capturedMs) {
  if (!/^ses_[A-Za-z0-9]+$/.test(sessionID) || info?.id !== sessionID)
    throw new Error("Active-server export belongs to a different or invalid session.")
  if (!info.directory || normalizedDirectory(info.directory) !== normalizedDirectory(directory))
    throw new Error("Active-server session belongs to a different project directory.")
  if (!Array.isArray(messages)) throw new Error("Active server returned no complete message list.")
  const seen = new Set()
  const measured = messages.filter((message) => message.info?.role === "assistant").map((message) => {
    if (typeof message.info.id !== "string" || !message.info.id || seen.has(message.info.id) || !Array.isArray(message.parts))
      throw new Error("Invalid or duplicate assistant message in active-server export.")
    seen.add(message.info.id)
    return {
      info: {
        ...strings(message.info, ["id", "role", "providerID", "modelID"]),
        ...(typeof message.info.completed === "boolean" ? { completed: message.info.completed } : {}),
        ...(message.info.retryEvents !== undefined ? numeric(message.info, ["retryEvents"]) : {}),
        time: timing(message.info.time),
        ...counters(message.info),
      },
      parts: message.parts.flatMap((part) => {
        if (part.type === "step-finish") return [{ type: part.type, ...counters(part) }]
        if (part.type === "reasoning") return [{ type: part.type, time: timing(part.time) }]
        if (part.type === "retry") return [{ type: part.type }]
        return []
      }),
    }
  })
  return {
    info: { id: info.id, directory: info.directory },
    messages: measured,
    workflowUsage: { version: 1, source: "OpenCode SDK", sessionId: sessionID, capturedMs },
  }
}

function exportWriter(readSession) {
  const folders = new Set()
  return {
    async capture(context) {
      if (!/^ses_[A-Za-z0-9]+$/.test(context.sessionID))
        throw new Error("OpenCode did not provide an exact session ID and project directory.")
      const { info, messages, directory } = await readSession(context)
      const capturedMs = Date.now()
      const data = measurementExport(info, messages, context.sessionID, directory, capturedMs)
      const folder = await mkdtemp(path.join(tmpdir(), "opencode-workflow-usage-"))
      folders.add(folder)
      const filename = path.join(folder, "usage.json")
      await writeFile(filename, JSON.stringify(data), { encoding: "utf8", mode: 0o600, flag: "wx" })
      return JSON.stringify({ session_id: context.sessionID, usage_export: filename, captured_ms: capturedMs })
    },
    async dispose() {
      for (const folder of folders) {
        // Delete only directories this bridge created directly in the system temp directory.
        if (path.dirname(folder) !== path.resolve(tmpdir()) || !path.basename(folder).startsWith("opencode-workflow-usage-"))
          throw new Error("Unexpected usage temp directory.")
        await rm(folder, { recursive: true, force: true })
        folders.delete(folder)
      }
    },
  }
}

export function usageBridge(client) {
  return exportWriter(async (context) => {
    if (!context.directory) throw new Error("OpenCode did not provide a project directory.")
    const options = { path: { id: context.sessionID }, query: { directory: context.directory },
      throwOnError: true, signal: context.abort }
    const [info, messages] = await Promise.all([client.session.get(options), client.session.messages(options)])
    return { info: info.data, messages: messages.data, directory: context.directory }
  })
}

export function nativeMeasurements(data) {
  if (!Array.isArray(data?.messages)) throw new Error("No complete V2 session export.")
  return {
    info: { id: data.info?.id, directory: data.info?.location?.directory },
    messages: data.messages.filter((m) => m.type === "assistant" || m.type === "compaction").map((m) => {
      if (m.type === "assistant" && !Array.isArray(m.content)) throw new Error("Invalid V2 assistant content.")
      const compaction = m.type === "compaction"
      return { info: {
        id: m.id, role: "assistant", providerID: m.model?.providerID, modelID: m.model?.id,
        time: timing(m.time), ...counters(m),
        ...(compaction ? { completed: m.status === "completed" } : {}),
        // Only the currently exposed retry is available, not a durable retry history.
        retryEvents: null,
      }, parts: compaction ? [] : m.content.filter((p) => p.type === "reasoning").map((p) => ({
        type: "reasoning", time: {
          ...(p.time?.created !== undefined ? { start: p.time.created } : {}),
          ...(p.time?.completed !== undefined ? { end: p.time.completed } : {}),
        },
      })) }
    }),
  }
}

export function nativeUsageBridge(ctx, connect = connectNativeServer) {
  return exportWriter(async (context) => {
    const active = await ctx.session.get({ sessionID: context.sessionID })
    const directory = active.location?.directory
    if (!directory || normalizedDirectory(directory) !== normalizedDirectory(ctx.location.directory))
      throw new Error("Active session belongs to a different plugin location.")
    const client = await connect(ctx, context.signal)
    const exported = await client.session.export({ sessionID: context.sessionID }, { signal: context.signal })
    const measured = nativeMeasurements(exported)
    return { ...measured, directory }
  })
}

async function connectNativeServer(ctx, signal) {
  // V2's plugin context omits session.export; context() loses pre-compaction
  // history. Use the public client, and verify that it reaches THIS process.
  const [{ OpenCode }, { Service }] = await Promise.all([
    import("@opencode/client"), import("@opencode/client/service"),
  ])
  const configured = ctx.options?.serverUrl
  const endpoint = configured ? { url: configured } : await Service.discover({ version: ctx.app.version })
  if (!endpoint) throw new Error("No active V2 service endpoint. For standalone serve, set workflow-usage plugin option serverUrl.")
  const url = new URL(endpoint.url)
  if (!["http:", "https:"].includes(url.protocol) || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname))
    throw new Error("Usage endpoint must be the local OpenCode server process.")
  const client = OpenCode.make({ baseUrl: endpoint.url, headers: Service.headers(endpoint) })
  const info = await client.server.info({ signal })
  if (info.pid !== process.pid || info.version !== ctx.app.version)
    throw new Error("Usage endpoint is not the active OpenCode server process.")
  return client
}
