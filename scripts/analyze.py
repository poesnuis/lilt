#!/usr/bin/env python3
"""Summarize Lilt or baseline-probe CSVs with identical statistics. Python 3.8+."""
import argparse
import csv
import json
import math
from pathlib import Path
import statistics


def summarize(path, include_menus=False):
    values = []
    excluded = 0
    with Path(path).open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream)
        if not {"frame_time_ns", "in_world", "menu_open"}.issubset(reader.fieldnames or []):
            raise ValueError(f"{path}: not a Lilt frame CSV")
        for row in reader:
            value = int(row["frame_time_ns"])
            if value <= 0:
                raise ValueError(f"{path}: non-positive frame interval")
            if include_menus or (row["in_world"] == "true" and row["menu_open"] == "false"):
                values.append(value)
            else:
                excluded += 1
    if not values:
        return {"frames": 0, "excluded": excluded, "note": "No qualifying frames"}
    values.sort()
    count = len(values)
    tail = max(1, math.ceil(count * .01))
    return {
        "frames": count,
        "excluded": excluded,
        "measuredSeconds": sum(values) / 1e9,
        "averageFps": count * 1e9 / sum(values),
        "onePercentLowFps": tail * 1e9 / sum(values[-tail:]),
        "meanMs": statistics.mean(values) / 1e6,
        "p50Ms": values[math.ceil(count * .50) - 1] / 1e6,
        "p95Ms": values[math.ceil(count * .95) - 1] / 1e6,
        "p99Ms": values[math.ceil(count * .99) - 1] / 1e6,
        "maxMs": values[-1] / 1e6,
        "standardDeviationMs": statistics.pstdev(values) / 1e6,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("captures", nargs="+", type=Path)
    parser.add_argument("--all-frames", action="store_true", help="Include menus and frames outside a world")
    parser.add_argument("--output", type=Path, help="Write full results as JSON")
    args = parser.parse_args()
    results = {str(path): summarize(path, args.all_frames) for path in args.captures}
    print("| Capture | Frames | Average FPS | 1% low FPS | P99 ms |")
    print("|---|---:|---:|---:|---:|")
    for path, stats in results.items():
        if stats["frames"]:
            print(f'| {Path(path).stem} | {stats["frames"]} | {stats["averageFps"]:.2f} | {stats["onePercentLowFps"]:.2f} | {stats["p99Ms"]:.2f} |')
        else:
            print(f'| {Path(path).stem} | 0 | — | — | — |')
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(results, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
