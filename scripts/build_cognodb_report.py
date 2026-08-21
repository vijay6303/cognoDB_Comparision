"""Build CognoDB-only normalized metrics and SVG charts from results/*.csv."""

from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "results"
REPORT = ROOT / "reports"


def read(name: str) -> list[dict[str, str]]:
    with (RESULTS / name).open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def main() -> None:
    REPORT.mkdir(exist_ok=True)
    rows: list[dict[str, str]] = []
    for row in read("traversal.csv") + read("point_lookup.csv") + read("aggregation.csv"):
        rows.append({
            "platform": "CognoDB",
            "workload": row["workload"],
            "hops": row["hops"],
            "iterations": row["successfulQueries"],
            "failures": row["failedQueries"],
            "p50_ms": f'{float(row["p50Micros"]) / 1000:.3f}',
            "p95_ms": f'{float(row["p95Micros"]) / 1000:.3f}',
        })

    concurrent = read("concurrent_read_write.csv")[0]
    rows.append({
        "platform": "CognoDB",
        "workload": "concurrent_read_write",
        "hops": "",
        "iterations": concurrent["successfulOperations"],
        "failures": concurrent["failedOperations"],
        "p50_ms": f'{float(concurrent["p50Micros"]) / 1000:.3f}',
        "p95_ms": f'{float(concurrent["p95Micros"]) / 1000:.3f}',
    })

    with (REPORT / "cognodb_normalized.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)

    load_seconds = 11449.31
    nodes = 91489
    relationships = 200000
    with (REPORT / "cognodb_summary.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(["metric", "value", "unit"])
        writer.writerow(["nodes", nodes, "count"])
        writer.writerow(["relationships", relationships, "count"])
        writer.writerow(["load_time", load_seconds, "seconds"])
        writer.writerow(["node_throughput", f'{nodes / load_seconds:.3f}', "nodes/sec"])
        writer.writerow(["relationship_throughput", f'{relationships / load_seconds:.3f}', "relationships/sec"])
        writer.writerow(["load_batch_size", 500, "relationships/batch"])

    chart_rows = [(row["workload"], float(row["p95_ms"])) for row in rows]
    maximum = max(value for _, value in chart_rows) or 1
    width, height, margin = 1100, 560, 90
    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}">',
        '<rect width="100%" height="100%" fill="#f7f4ee"/>',
        '<text x="90" y="42" font-family="sans-serif" font-size="24" fill="#17202a">CognoDB p95 latency</text>',
        f'<line x1="{margin}" y1="{height-margin}" x2="{width-margin}" y2="{height-margin}" stroke="#17202a"/>',
    ]
    bar_width = max(20, (width - 2 * margin) // len(chart_rows) - 8)
    for index, (label, value) in enumerate(chart_rows):
        x = margin + index * (bar_width + 8)
        bar_height = (height - 2 * margin) * value / maximum
        y = height - margin - bar_height
        svg.append(f'<rect x="{x}" y="{y:.1f}" width="{bar_width}" height="{bar_height:.1f}" fill="#d45d3d"/>')
        svg.append(f'<text x="{x + bar_width / 2:.1f}" y="{height-margin+18}" text-anchor="middle" font-family="sans-serif" font-size="10" transform="rotate(45 {x + bar_width / 2:.1f},{height-margin+18})">{label}</text>')
    svg.append('</svg>')
    (REPORT / "cognodb_p95_latency.svg").write_text("\n".join(svg), encoding="utf-8")
    print(f"Wrote CognoDB report artifacts to {REPORT}")


if __name__ == "__main__":
    main()
