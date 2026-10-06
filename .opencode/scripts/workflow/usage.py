"""OpenCode session snapshots; never scrape the global stats screen."""
import math
import os
import re
import shutil
import time
from pathlib import Path
from datetime import datetime, timezone

from .common import WorkflowError, read_json, run

TOKEN_FIELDS = ("InputTokens", "OutputTokens", "ReasoningTokens", "CacheReadTokens",
                "CacheWriteTokens")
VALUE_FIELDS = (*TOKEN_FIELDS, "EstimatedCostUSD", "Requests", "RetryEvents",
                "ReasoningSeconds", "MessageElapsedSeconds")


def timestamp(captured_ms):
    return datetime.fromtimestamp(captured_ms / 1000, timezone.utc).isoformat()


def cost_status(value):
    if value is None:
        return "unavailable"
    return "reported-zero-actual-unknown" if value == 0 else "reported-estimate-actual-unknown"


def cost_description(value):
    if value is None:
        return "Keine Kostenschätzung gemeldet; tatsächliche Kosten unbekannt."
    return f"OpenCode meldet {value:g} USD; tatsächliche Kosten unbekannt."


def overlap(start, end, lower, upper):
    start, end = number(start), number(end)
    if start is None or end is None or end < start:
        return None
    return max(0, min(end, upper) - max(start, lower)) / 1000


def number(value):
    if isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value) and value >= 0:
        return value
    return None


def total(values):
    values = list(values)
    return sum(values) if all(value is not None for value in values) else None


def token_values(unit: dict) -> dict:
    tokens = unit.get("tokens") or {}
    cache = tokens.get("cache") or {}
    return {"InputTokens": number(tokens.get("input")),
            "OutputTokens": number(tokens.get("output")),
            "ReasoningTokens": number(tokens.get("reasoning")),
            "CacheReadTokens": number(cache.get("read")),
            "CacheWriteTokens": number(cache.get("write")),
            "EstimatedCostUSD": number(unit.get("cost"))}


def elapsed(start, end):
    start, end = number(start), number(end)
    if start is None or end is None or end < start:
        return None
    return (end - start) / 1000


def snapshot(data: dict, repo: Path, session_id: str | None = None,
             captured_ms: float | None = None) -> dict:
    bridge = data.get("workflowUsage")
    if bridge is not None:
        if (not isinstance(bridge, dict) or bridge.get("version") != 1
                or bridge.get("source") != "OpenCode SDK"
                or bridge.get("sessionId") != data.get("info", {}).get("id")
                or number(bridge.get("capturedMs")) is None):
            raise WorkflowError("Invalid active-server usage snapshot.")
        if captured_ms is None:
            captured_ms = bridge["capturedMs"]
    captured_ms = captured_ms if captured_ms is not None else time.time() * 1000
    info = data.get("info", {})
    if not isinstance(data.get("messages"), list) or not info.get("id"):
        raise WorkflowError("Unsupported OpenCode export: expected info and messages.")
    if session_id and info["id"] != session_id:
        raise WorkflowError("Usage export belongs to a different session.")
    directory = info.get("directory")
    if not directory or Path(directory).resolve() != repo.resolve():
        raise WorkflowError("Usage session belongs to a different project directory.")
    messages = {}
    for message in data["messages"]:
        metadata = message.get("info", {})
        if metadata.get("role") != "assistant":
            continue
        message_id = metadata.get("id")
        if not message_id or message_id in messages:
            raise WorkflowError("Missing or duplicate assistant message ID in usage export.")
        parts = message.get("parts", [])
        steps = [part for part in parts if part.get("type") == "step-finish"]
        completed = metadata.get("completed", metadata.get("time", {}).get("completed") is not None)
        units = steps or ([metadata] if completed else [])
        values = [token_values(unit) for unit in units]
        result = {key: total(unit[key] for unit in values)
                  for key in (*TOKEN_FIELDS, "EstimatedCostUSD")}
        result["Requests"] = len(units)
        result["RetryEvents"] = (number(metadata["retryEvents"]) if "retryEvents" in metadata
                                 else sum(part.get("type") == "retry" for part in parts))
        reasoning = [part.get("time", {}) for part in parts if part.get("type") == "reasoning"]
        # Measure partial intervals at the snapshot boundary so a later completion
        # does not charge pre-begin reasoning time to this run.
        result["ReasoningSeconds"] = total(elapsed(t.get("start"), t.get("end", captured_ms)) for t in reasoning)
        if not reasoning and result["Requests"] and result["ReasoningTokens"] != 0:
            result["ReasoningSeconds"] = None
        timing = metadata.get("time", {})
        result["MessageElapsedSeconds"] = elapsed(timing.get("created"),
                                             timing.get("completed", None if completed else captured_ms))
        result["messageStartedMs"] = number(timing.get("created"))
        result["messageEndedMs"] = number(timing.get("completed"))
        result["reasoningIntervals"] = [{"start": number(t.get("start")), "end": number(t.get("end", captured_ms))}
                                        for t in reasoning]
        result["pending"] = not completed
        result["model"] = "/".join(str(metadata.get(key, "unknown")) for key in ("providerID", "modelID"))
        messages[message_id] = result
    # No transcript, tool arguments, code, prompts or credentials are persisted.
    result = {"sessionId": info["id"], "capturedMs": captured_ms, "messages": messages}
    if bridge is not None:
        result["source"] = bridge["source"]
    return result


def capture(repo: Path, session_id: str | None, export_path: Path | None = None) -> dict:
    if export_path:
        data = read_json(export_path)
        if "workflowUsage" in data:
            captured = (data["workflowUsage"] or {}).get("capturedMs") if isinstance(data["workflowUsage"], dict) else None
            if number(captured) is None or not -30000 <= time.time() * 1000 - captured <= 300000:
                raise WorkflowError("Active-server usage snapshot is stale or invalid. Call workflow_usage_snapshot again.")
    else:
        if not session_id or not re.fullmatch(r"ses_[A-Za-z0-9]+", session_id):
            raise WorkflowError("usage requires the exact OpenCode session_id (ses_...) or an export file.")
        executable = shutil.which("opencode.cmd" if os.name == "nt" else "opencode")
        if not executable:
            raise WorkflowError("OpenCode CLI not found. Supply an explicit session export file.")
        import json
        data = json.loads(run(repo, executable, "export", session_id))
    return snapshot(data, repo, session_id)


def difference(before: dict, after: dict) -> dict:
    if before["sessionId"] != after["sessionId"]:
        raise WorkflowError("Cannot compare different usage sessions.")
    if before.get("source") == "OpenCode SDK":
        if after.get("source") != "OpenCode SDK" or after["capturedMs"] <= before["capturedMs"]:
            raise WorkflowError("Finish requires a fresh workflow_usage_snapshot from the same active session.")
    old, new = before["messages"], after["messages"]
    if not old.keys() <= new.keys():
        raise WorkflowError("Usage messages disappeared; session was reverted or export is incomplete.")
    if after["capturedMs"] < before["capturedMs"]:
        raise WorkflowError("Usage snapshot time went backwards.")
    deltas = []
    models = set()
    pending = 0
    for message_id, values in new.items():
        prior = old.get(message_id, {})
        delta = {}
        for key in VALUE_FIELDS:
            # Historical completed runs retain their original snapshot files.
            previous = prior.get(key, prior.get("InferenceSeconds", 0) if key == "MessageElapsedSeconds" else 0)
            latest = values.get(key, values.get("InferenceSeconds"))
            if message_id in old and latest == previous and (latest is not None or values == prior):
                delta[key] = 0
            elif previous is None or latest is None:
                delta[key] = None
            elif latest < previous - 1e-9:
                raise WorkflowError(f"Usage counter went backwards: {message_id} / {key}")
            else:
                delta[key] = max(0, latest - previous)
        if values != prior and "messageStartedMs" in values:
            lower, upper = before["capturedMs"], after["capturedMs"]
            delta["MessageElapsedSeconds"] = overlap(values["messageStartedMs"],
                values["messageEndedMs"] if not values["pending"] else upper, lower, upper)
            intervals = values["reasoningIntervals"]
            if intervals:
                delta["ReasoningSeconds"] = total(overlap(t["start"], t["end"],
                                                          lower, upper) for t in intervals)
            elif values["ReasoningSeconds"] is None:
                delta["ReasoningSeconds"] = None
        if delta["Requests"] or delta["RetryEvents"] or (values["pending"] and values != prior):
            models.add(values["model"])
        pending += bool(values["pending"])
        deltas.append(delta)
    result = {key: total(delta[key] for delta in deltas) for key in VALUE_FIELDS}
    result.update(SessionId=after["sessionId"], ActualModels=sorted(models),
                  PendingMessages=pending, CostSource="OpenCode estimate (USD), not an invoice",
                  CostStatus=cost_status(result["EstimatedCostUSD"]),
                  UsageStartUTC=timestamp(before["capturedMs"]), UsageEndUTC=timestamp(after["capturedMs"]),
                  UsageWindowSeconds=round((after["capturedMs"] - before["capturedMs"]) / 1000, 3),
                  MessageTimeDefinition="sum of clipped message intervals; includes tool/wait time, not pure model compute; overlapping messages may exceed wall time",
                  RequestDefinition="completed inference steps/messages including V2 compactions; transport retries may not be exposed",
                  Scope="session delta between begin and finish; excludes final response and external prompt provider")
    return result
