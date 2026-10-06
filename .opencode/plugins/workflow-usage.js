import { usageBridge, nativeUsageBridge } from "../scripts/workflow_usage_bridge.mjs"

const description = "Export usage metrics for THIS active OpenCode session. Call immediately before workflow begin and finish. Returns session_id and usage_export; never accepts a guessed session ID."

async function server({ client }) {
  const bridge = usageBridge(client)
  return {
    dispose: () => bridge.dispose(),
    tool: {
      workflow_usage_snapshot: {
        description,
        args: {},
        execute: (_args, context) => bridge.capture(context),
      },
    },
  }
}

// Plugin.define is an identity helper. This definition needs no V2 runtime
// import until V2 setup runs; the documented server entry also supports V1.
export default {
  id: "workflow-usage",
  server,
  async setup(ctx) {
    const bridge = nativeUsageBridge(ctx)
    await ctx.tool.transform((editor) => editor.add({
      name: "workflow_usage_snapshot",
      description,
      input: { type: "object", properties: {}, additionalProperties: false },
      options: { codemode: false },
      execute: async (_input, context) => ({ content: await bridge.capture(context) }),
    }))
    return () => bridge.dispose()
  },
}
