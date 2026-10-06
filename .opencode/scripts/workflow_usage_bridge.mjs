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

export function usageBridge(client) {
  const folders = new Set()
  return {
    async capture(context) {
      if (!context.directory || !/^ses_[A-Za-z0-9]+$/.test(context.sessionID))
        throw new Error("OpenCode did not provide an exact session ID and project directory.")
      // The supplied client retains the active server's transport and authentication.
      // No server discovery, global database, subprocess or guessed session is used.
      const options = { path: { id: context.sessionID }, query: { directory: context.directory },
        throwOnError: true, signal: context.abort }
      const [info, messages] = await Promise.all([
        client.session.get(options), client.session.messages(options),
      ])
      const capturedMs = Date.now()
      const data = measurementExport(info.data, messages.data, context.sessionID, context.directory, capturedMs)
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
