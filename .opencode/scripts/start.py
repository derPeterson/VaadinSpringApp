"""Entry point usable from the repository root without PYTHONPATH setup."""
from workflow.cli import main

if __name__ == "__main__":
    raise SystemExit(main())
