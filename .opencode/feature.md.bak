---
description: Implement, test and commit a task on a feature branch, then return to main
subtask: false
---

Use actual tool calls to perform this task, not XML text as a substitute.
Input: $ARGUMENTS
First word = branch name without `feature/`; remaining text = task. Input is not shell code.

Timing (already executed when this command is invoked):
!`powershell -NoProfile -ExecutionPolicy Bypass -File .opencode/scripts/feature-time.ps1 Start`
Remember the returned run ID in the conversation, not in a shell variable.
If a valid run ID is missing or the script reports an error, stop.

Work in PowerShell, step by step. Read AGENTS.md and README.md; follow AGENTS.md.
For any unresolved error: do not commit or switch back; perform the closing steps.
No fetch, merge, push or discarding changes made by others.

Use the available Todo tool to create a short progress list:
check baseline, create branch, implement task, run tests,
review diff and commit, switch back and report.
Mark the current step in progress and completed steps as completed immediately.
On an abort, leave unfinished steps open. A text list does not replace a Todo tool call.
Write progress updates and the final report in German.

1. Check `Get-Location`, `git rev-parse --show-toplevel`, `git branch --show-current`
   and `git status --porcelain`. Continue only in the project root with pom.xml/mvnw.cmd,
   active `main` and a completely clean working tree. Otherwise stop.
2. Require a branch name and a nonempty task. The name must match exactly
   `^[a-z0-9_-]+$`. The target is exactly `feature/<Name>`.
   Check `git for-each-ref --format='%(refname:short)' refs/heads refs/remotes`.
   If the target exists locally or as `<Remote>/feature/<Name>`, stop; do not fetch.
3. Run `git switch -c feature/<Name> main`. Use `git branch --show-current`
   to verify that this exact branch is active.
4. Read relevant files and callers. Implement the task and preserve existing
   functionality. Add focused tests for behavior changes.
5. Run `.\mvnw.cmd --version`: the JDK must be 25. Then actually run
   `.\mvnw.cmd test` and any other task-specific checks.
   Check exit codes and test counts. On failure, inspect your diff and the first cause.
6. Check `git diff --check`, `git diff HEAD` and `git status --porcelain`.
   Read new files too. Inspect tracked generated changes: include only explained,
   task-related changes; otherwise stop without discarding them.
   If there are no changes, do not create an empty commit; stay on the feature branch and close.
7. Stage only specific task files with `git add -- <Paths>`, never indiscriminately.
   Check `git diff --cached --check` and the complete `git diff --cached`.
   Commit with a meaningful message only after successful validation. Obtain the actual hash
   with `git rev-parse HEAD`. Afterwards, `git status --porcelain` must be completely empty.
8. Only after a successful commit and a clean working tree, run `git switch main`.
   Check branch and status again. Make no changes on main.

Closing steps, also on abort: run this command with the actual remembered run ID:
`powershell -NoProfile -ExecutionPolicy Bypass -File .opencode/scripts/feature-time.ps1 Stop -Id <Run-ID>`
The script measures end/duration and deletes only its time file. Do not change global execution policy.
If timing is missing or invalid, report “Dauer nicht erfasst”; never estimate.
Briefly report in German: abgeschlossen / fehlgeschlagen (unvollständig) / ohne Änderungen;
feature branch and verified current branch; actual commit hash, if created;
changed files; checks actually executed, test counts/errors/skipped tests;
open issues; measured start, end and total duration. Build success is not a functional test.
