"""Current-feature-branch entry point; all lifecycle work uses the /start engine."""
from workflow.cli import main

if __name__ == "__main__":
    raise SystemExit(main(restart=True))
