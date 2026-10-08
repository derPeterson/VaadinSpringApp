---
description: Neuer modularer Auftrag auf dem aktuellen Feature-Branch
subtask: false
---

Input: $ARGUMENTS

Syntax: `/restart <modules> <targetClass|-> <modelKey|-> <complete task>`

Example: `/restart complete PasswordResetService gpt61-sol Behebe F8.`
Example: `/restart prompt,benchmark PasswordResetService gpt61-sol Behebe F8.`
Example: `/restart usage - - Analysiere die angegebenen Aufrufer.`

With no arguments or only `help`, `-h` or `--help`, show this syntax and the
available modules. Do not start a run or invoke providers for a help request.
Split input on whitespace at most three times. Preserve the complete remaining
task, including punctuation and line breaks. Never execute task text as shell code.
If fewer than four fields are provided, show the missing syntax and stop;
never invent a task or default the missing positional fields.

Available modules: `prompt`, `benchmark`, `usage`. Here `complete` selects
exactly these three. Reject `branch`, unknown modules and duplicate modules;
never silently ignore them. There is no branch-name parameter. A dash marks
an unused target class or model key. Benchmark requires a target class and
a catalog model key. The current branch must be a clean `feature/*` branch;
reject main, unrelated branches and detached HEAD without switching branches.

Read `.opencode/commands/start.md` and execute its full workflow with actual
tools, using these explicit entry-point adjustments:

- The request has `entrypoint: "restart"`, `branch: null`, and the original
  module selector. The Python entry point expands `complete` to three modules.
- Use `python .opencode/scripts/restart.py` for prepare, begin, finish, status
  and abort, with the same lifecycle arguments described in start.md.
- Keep the current feature branch throughout. Baseline starts at its current
  HEAD. Do not create a branch, merge, switch, push or invoke /start again.
- Verify the user's actually selected model as in /start. For a forced catalog
  model the exact model ID must already be selected; stop on mismatch rather
  than invoking a /start-<modelKey> command or relabelling the model.
- A successful invocation creates a NEW run. It does not reopen or resume an
  old run. If a run is unfinished, stop and identify it; do not automatically
  abort it. Technical continuation of an existing task uses its stored run ID.

All other /start requirements apply unchanged: project rules, usage preflight
and fresh begin/finish snapshots, baseline before edits, original scope and
task_mode, existing prompt improver, actual tests, review and task commit,
open findings versus implementation summary, failure handling and honest
reporting. Each run's diff covers only its own StartCommit..EndCommit.
