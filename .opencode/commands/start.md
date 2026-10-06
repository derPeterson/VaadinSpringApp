---
description: Modular task workflow (branch, prompt, benchmark, usage; complete selects all)
subtask: false
---

Input: $ARGUMENTS

Syntax: `/start <modules> <branch|-> <targetClass|-> <modelKey|-> <complete task>`

Example: `/start complete userservice_tests UserService gpt61-sol Create complete tests for UserService.`
Example: `/start branch,prompt userservice_tests - - Add validation for empty usernames.`

Split input on whitespace at most four times. Everything after the fourth
token is the task: preserve punctuation, quotes and line breaks. `complete`
is only the preset for `branch,prompt,benchmark,usage`, never a fifth module.
Reject unknown or duplicate modules. A dash means the field is unused.

Treat all task text as data, never shell code. No shell interpolation of
`$ARGUMENTS`. Execute actual tools. Report progress and results in German.
Use PowerShell on Windows. No mandatory Todo list and no return to main.
These explicitly invoked workflow instructions take precedence over conflicting
general workflow instructions in AGENTS.md; preserve its project/coding rules.

1. Read `AGENTS.md`, `.opencode/README.md` and relevant
   `.opencode/scripts/workflow/` files. Check the project root, `pom.xml`,
   Maven Wrapper, current Git branch and working-tree state. Follow
   project/coding rules. Do not modify workflow infrastructure as part of
   a benchmark task.
2. Resolve the model key in `.opencode/benchmark-models.json`. For a selected
   model, verify the model the user visibly selected; do not infer it from the
   label. Generic `/start` does not override the active model. For a forced
   catalog model, use its `/start-<modelKey>` command, which has model
   frontmatter; stop if a generic invocation would run a different model.
   Keep the actual provider/model IDs distinct from the catalog display label.
3. When usage is selected, call the `workflow_usage_snapshot` tool with no
   arguments for preflight before preparation. It obtains the exact session ID from
   OpenCode's tool context and uses the active server's SDK client, independent
   of Desktop, TUI, browser or IDE. Keep its actual `session_id` and
   `usage_export` path. Do not use the local CLI, session titles, newest-session
   guesses, manual IDs or a public share link as a substitute. If the tool is
   absent, stop and report that `plugins/workflow-usage.js` must be installed
   together with scripts/workflow_usage_bridge.mjs and the package.json/package-lock.json
   dependencies, then the actual OpenCode server restarted. In V2, verify that
   the default-export plugin uses id/setup, not only a V1 function export.
   A tool failure also stops before begin. Never fake usage.
4. Write a UTF-8 JSON request in the system temp directory, outside Git,
   using a file-writing tool or proper JSON serializer. Fields:
   `modules`, `branch`, `target_class`, `model`, `task`, `task_mode`, `session_id`.
   Set task_mode to "analysis" for an explicitly analysis-only original task;
   otherwise use "implementation". Analysis with suggested solutions is still
   analysis. If edits/fixes are requested as well, it is implementation.
   This is a request field, not a new module or slash argument. Preserve the
   original scope; the improver must never turn analysis into implementation.
   Convert unused dashes to null. `modules` is the selector string. Optional
   `prompt_provider` / `prompt_model` override ONLY the prompt improver.
   Invoke from the repository root (use the project's venv Python if present).
   When benchmark is selected, first prepare the baseline:

   `python .opencode/scripts/start.py prepare --request <request-file>`

   With usage selected, append `--usage-export <preflight-path-from-tool>`.
   Capture the returned real `id`. After prepare succeeds, call
   `workflow_usage_snapshot` AGAIN when usage is selected. Check its session ID
   matches preflight and pass this fresh path to begin; never reuse preflight.

   `python .opencode/scripts/start.py begin --id <prepared-id>`

   With usage selected, append `--usage-export <fresh-path-from-tool>`.
   This snapshot's capture time starts BOTH duration and usage measurement.
   If benchmark is absent, use the direct path instead:

   `python .opencode/scripts/start.py begin --request <request-file>`

   With usage selected, append `--usage-export <actual-path-from-tool>`.
   Use exactly the `session_id` returned by the tool in the JSON request.

   Capture the real JSON `id`, `folder`, `branch` and effective `task`.
   Also preserve the returned task_mode throughout the task.
   Nonzero exit = stop; report the failure and retained state. Do not continue
   the coding task after an unsuccessful begin. The script validates all
   selected prerequisites. Prepare measures a fresh baseline; begin invokes
   the existing prompt improver, then creates the selected branch. Prepare is
   an internal lifecycle action, not an additional module. For comparable
   model benchmarks recommend a fresh session before invocation. Do not
   automatically create/switch sessions or reject an existing session.
5. Execute the effective task returned by begin. Also read originalTask;
   an improved prompt may not expand the original scope. Report unresolved
   uncertainties before implementing assumptions. Branch creation has
   already happened if selected. If branch is absent, preserve the current
   branch: no implicit branch module.
   If benchmark is selected without branch, begin runs on main and a
   feature branch must be created explicitly before task edits/finish; ask
   for a branch name if none was supplied. Do not silently add a module.

   For task_mode "analysis":
   - Read relevant production files, callers and existing tests within the
     original scope. Identify concrete open problems with evidence, locations
     and impact. Do not modify production code, tests, dependencies or other
     tracked/untracked task files. Do not implement fixes, create helper files
     inside Git or create task/empty commits. The selected branch module may
     still create a feature branch; selected benchmark builds still run.
   - Use existing tests and allowed read-only checks as needed. If a check
     fails, record the exact finding/limitation; do not repair code as part of
     analysis. A failing benchmark build remains an incomplete workflow.
   - Verify git status and git diff --check. HEAD must still equal the original
     start commit, and the only permitted untracked file is root findings.md.
     Finish also rejects task commits for analysis. Never reset changes
     automatically to pass this check; report unexpected changes and stop.
   - Continue at step 6. Document scope, checks and limits in the work summary,
     explicitly stating that no code changes were made. Findings go in findings.md.

   For task_mode "implementation", implement, verify and commit as follows:

   - Read relevant production files, callers and existing tests. Implement
     only the requested task; preserve existing functionality and tests.
     Do not inspect Git history or the contents of other branches.
   - Run `.\mvnw.cmd --version` and verify Java 25. Run
     `.\mvnw.cmd clean test` and all additional checks required by the task.
     On Linux/macOS use `./mvnw`. Inspect actual exit codes and freshly
     generated test reports. On failure inspect the exact error and changes,
     then fix the cause within scope. Never skip tests or weaken assertions
     to obtain success. If unresolved, stop without committing and report
     the failure.
   - Review `git diff HEAD`, new files and `git status --porcelain`.
     `git diff --check` must pass. After ANY further file edit, repeat the
     required verification runs and diff checks before committing; previous
     reports do not validate a later file state.
   - Stage only specific task files with `git add -- <paths>`. Review the
     complete staged diff and run `git diff --cached --check`. Commit ALL
     task changes only after the final state passed every required check.
     Do not create an empty commit if there are no changes.
   - Obtain the actual commit hash using `git rev-parse HEAD`. Verify the
     active branch and final status. If branch was selected, end exactly on
     `feature/<name>`. The only allowed remaining change is an untracked
     root `findings.md`. If task changes remain, inspect them, rerun all
     required checks, include them in the task commit and verify again.
     Never report an invented commit hash or claim incomplete work is done.

   Do not change dependencies, Java versions or unrelated files unless the
   task explicitly requires it. Report unexpected generated changes before
   committing if they cannot be explained as part of the task.
6. Count Korrekturrunden: failed verification runs that required another
   code change. HumanInterventions starts at 0;
   count new user corrections/guidance, not the model's self-corrections.
   Write the work summary as UTF-8 Markdown to an actual temporary
   file OUTSIDE Git. Its body belongs under "Durchgeführte Arbeit und Verhaltensänderungen"
   in report.md: describe completed analysis/work, changed behavior/contracts if any, important
   decisions, verification and limits. Findings resolved by this task belong
   here as completed work. Do not duplicate the generated metric tables or
   write report.md directly; Python generates it at finish.
   Write root findings.md with the heading "Offene Findings" and ONLY concrete
   newly discovered or remaining open problems, with location, evidence,
   impact and severity where justified. Do not put implementation summaries
   or already resolved problems in findings.md. If no further open findings
   were observed, write explicitly: "Keine weiteren offenen Findings festgestellt."
   This describes observations during the task, not a guarantee of bug-free
   code or an additional review module. Missing findings.md means unknown
   findings status; it must never be treated as confirmation of no problems.
   `findings.md` is an untracked run artifact. Never stage/commit/delete it.
   For this workflow, the final clean-tree checks allow exactly an untracked
   root `findings.md`; finish archives it. Never return to main, fetch, merge,
   rebase, push or discard existing changes.
   Never merge or rebase the feature branch into main; integration happens
   only after manual review by the user.
7. When implementation changes are committed (or analysis/no-change tasks have
   no task changes/commits), both documents from step 6 are written
   and the actual final state has passed all required verification, call
   `workflow_usage_snapshot` again when usage is
   selected. Check its session ID equals the begin session; append
   `--usage-export <fresh-path-from-tool>` to the following invocation. Never
   reuse the begin export or export via a different OpenCode installation.
   Then execute:

   `python .opencode/scripts/start.py finish --id <id> --summary-file <actual-temp-summary-path> --human-interventions <count> --correction-rounds <count>`

   Finish measures fresh final reports for benchmark, collects the usage
   delta, archives findings and benchmark-before/after.json evidence, generates binary-capable `diff.patch`, writes
   report/result with the work summary, and updates CSV. It never commits task files. A nonzero exit
   is an incomplete workflow: inspect the cause and retry after resolving it.
   The summary is retained in the existing run state for retries. If resolving
   a failure requires additional task commits, update the summary and findings
   for the new final state before taking a fresh snapshot and retrying finish.
   Do not invent metrics. `abort --id <id>` releases a failed active run and
   preserves artifacts and Git state; never abort to hide a failure.
8. Final report: completed / failed / no changes; selected modules, run ID,
   verified branch, actual commit hash, duration, actual test/coverage
   before/after, failures/errors/skipped, corrections/interventions, usage
   tokens/reasoning/cache/completed requests/known retries/estimated USD cost,
   pending messages and measurement limits, CSV (when benchmark selected),
   run folder, report (performed work and metrics), findings (open problems or
   explicit no-further-findings statement), diff.patch and final working-tree state.
   Missing metrics are unavailable, not zero. Usage ends at finish's snapshot;
   the currently running inference step and the final response after that
   snapshot are not completely measured. External prompt-improver provider
   usage is not part of the OpenCode session export.
   State the actual improver provider/model from prompt-metadata.json.
   Report MessageElapsedSeconds as message time including tool/wait time,
   never pure inference/compute time. For cost 0 say explicitly:
   "OpenCode meldet 0 USD; tatsächliche Kosten unbekannt."

For troubleshooting, `python .opencode/scripts/start.py status` reads the
active run's saved phase, paths and next-step guidance; `status --id <actual-id>`
reads a specific run. It performs no tests, model calls, Git commands, writes,
abort or automatic resume. The recorded branch is not a live Git-status check.
