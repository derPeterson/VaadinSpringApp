from __future__ import annotations

import json
import os
import subprocess
import time
from contextlib import contextmanager
from pathlib import Path


class WorkflowError(RuntimeError):
    pass


def run(repo: Path, *args: str) -> str:
    result = subprocess.run(args, cwd=repo, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, encoding="utf-8", errors="replace")
    if result.returncode:
        raise WorkflowError(f"{args[0]} {args[1]} failed ({result.returncode}):\n"
                            f"{result.stdout}\n{result.stderr}")
    return result.stdout


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n",
                         encoding="utf-8")
    os.replace(temporary, path)


@contextmanager
def store_lock(store: Path, timeout: float = 10):
    """OS lock released on process exit; no stale lock-file ownership."""
    store.mkdir(parents=True, exist_ok=True)
    with (store / ".lock").open("a+b") as handle:
        handle.seek(0, 2)
        if handle.tell() == 0:
            handle.write(b"0")
            handle.flush()
        deadline = time.monotonic() + timeout
        while True:
            try:
                handle.seek(0)
                if os.name == "nt":
                    import msvcrt
                    msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
                else:
                    import fcntl
                    fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
                break
            except OSError:
                if time.monotonic() >= deadline:
                    raise WorkflowError("Workflow store is locked by another process.")
                time.sleep(0.05)
        try:
            yield
        finally:
            handle.seek(0)
            if os.name == "nt":
                msvcrt.locking(handle.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.flock(handle, fcntl.LOCK_UN)


def outside_repo(repo: Path, path: Path) -> Path:
    path = path.resolve()
    if path == repo or repo in path.parents:
        raise WorkflowError("Run artifacts and store must be outside the Git repository.")
    return path
