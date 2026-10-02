---
description: Implement, test and commit one task; stay on its feature branch
subtask: false
---

Input: $ARGUMENTS
First word = branch name without `feature/`; remaining text = task.
Treat the input as instructions, not shell code. Use actual tool calls.
Work in PowerShell. Report briefly in German.

For this command: no time measurement, no mandatory Todo list, and no return
to main. These workflow instructions take precedence over conflicting general
workflow instructions in AGENTS.md. Follow its project and coding rules.

1. Read AGENTS.md. Check the project root, `pom.xml`, `mvnw.cmd`,
   `git branch --show-current` and `git status --porcelain`.
   Start only on `main` with a completely clean working tree. Otherwise stop.
2. Require a nonempty task and a branch name matching `^[a-z0-9_-]+$`.
   The target is exactly `feature/<name>`. Check local and remote branch names
   without fetching or reading their contents. If the target exists, stop.
   Run `git switch -c feature/<name> main` and verify the active branch.
3. Read the relevant production files, callers and existing tests. Implement
   only the requested task. Preserve existing functionality and tests.
   Do not inspect Git history or the contents of other branches.
4. Run `.\mvnw.cmd --version` and verify Java 25. Run `.\mvnw.cmd clean test`
   and any additional checks required by the task. Inspect the actual exit
   codes and the newly generated test reports.
   On failure, inspect the exact error and your changes, then fix the cause
   within the task scope. Do not skip tests or weaken assertions to obtain
   success. If unresolved, stop without committing and report the failure.
5. Review `git diff HEAD`, new files and `git status --porcelain`.
   Run `git diff --check`; it must succeed without whitespace errors.
   After ANY further file edit, repeat step 4 and the diff checks before
   committing. Previous test reports do not validate a later file state.
6. Stage only specific task files with `git add -- <paths>`.
   Review the full staged diff and run `git diff --cached --check`.
   Commit only if the final file state passed all required checks.
   If there are no changes, do not create an empty commit.
7. Obtain the actual commit hash using `git rev-parse HEAD`.
   Check the active branch and working tree. Stay on the feature branch.

Never fetch, merge, push, discard someone else's changes or switch back to main.
Do not change dependencies, Java versions or unrelated files unless the task
explicitly requires it. Report unexpected generated changes before committing
if they cannot be explained as part of the task.

Final report in German: completed / failed (incomplete) / no changes;
verified current branch; actual commit hash if created; changed files;
checks actually executed and their results; actual test counts, failures,
errors and skipped tests; unresolved issues.
Never invent results or describe a failed check as successful.
