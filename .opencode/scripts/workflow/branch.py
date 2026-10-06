import re
from pathlib import Path

from .common import WorkflowError, run


def current(repo: Path) -> str:
    return run(repo, "git", "branch", "--show-current").strip()


def require_clean(repo: Path, findings: bool = False) -> None:
    # Do not strip leading status spaces: ' M' is significant.
    entries = run(repo, "git", "status", "--porcelain", "--untracked-files=all").splitlines()
    unexpected = [line for line in entries if not (findings and line == "?? findings.md")]
    if unexpected:
        raise WorkflowError("Working tree is not clean: " + "; ".join(unexpected))
    if findings and run(repo, "git", "ls-files", "--", "findings.md").strip():
        raise WorkflowError("findings.md must remain untracked, never committed.")


def validate(repo: Path, name: str) -> str:
    if not isinstance(name, str) or not re.fullmatch(r"[a-z0-9_-]+", name):
        raise WorkflowError("Branch name must match ^[a-z0-9_-]+$.")
    target = "feature/" + name
    if current(repo) != "main":
        raise WorkflowError("New branch and benchmark workflows must start on main.")
    require_clean(repo)
    refs = run(repo, "git", "for-each-ref", "--format=%(refname)",
               "refs/heads", "refs/remotes").splitlines()
    if any(ref == "refs/heads/" + target or
           (ref.startswith("refs/remotes/") and ref.split("/", 3)[-1] == target)
           for ref in refs):
        raise WorkflowError(f"Branch already exists locally or remotely: {target}")
    return target


def create(repo: Path, name: str) -> str:
    target = validate(repo, name)
    run(repo, "git", "switch", "-c", target, "main")
    if current(repo) != target:
        raise WorkflowError(f"Could not verify active branch {target}.")
    return target
