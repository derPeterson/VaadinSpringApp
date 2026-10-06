import { tool } from "@opencode-ai/plugin"
import { usageBridge } from "../scripts/workflow_usage_bridge.mjs"

export const WorkflowUsage = async ({ client }) => {
  const bridge = usageBridge(client)
  return {
    dispose: () => bridge.dispose(),
    tool: {
      workflow_usage_snapshot: tool({
        description: "Export usage metrics for THIS active OpenCode session. Call immediately before workflow begin and finish. Returns session_id and usage_export; never accepts a guessed session ID.",
        args: {},
        execute: (_args, context) => bridge.capture(context),
      }),
    },
  }
}
