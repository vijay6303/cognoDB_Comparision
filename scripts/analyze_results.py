"""Normalize benchmark CSVs and create lightweight SVG charts without dependencies."""

from __future__ import annotations

import csv
from pathlib import Path

RESULTS = Path(__file__).resolve().parents[1] / "results"


def read_rows(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def normalize() -> None:
    rows: list[dict[str, str]] = []
    for path in sorted(RESULTS.glob("*.csv")):
        if path.name == "normalized.csv":
            continue
        for row in read_rows(path):
            row["source_file"] = path.name
            rows.append(row)

    if not rows:
        raise SystemExit("No result CSV files found")

    columns = ["source_file"]
    for row in rows:
        for key in row:
            if key not in columns:
                columns.append(key)
    with (RESULTS / "normalized.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        writer.writerows({column: row.get(column, "") for column in columns} for row in rows)
    print(f"Wrote {len(rows)} normalized rows to {RESULTS / 'normalized.csv'}")


def chart() -> None:
    rows = []
    path = RESULTS / "normalized.csv"
    for row in read_rows(path):
        try:
            rows.append((row["source_file"], float(row["p95Micros"])))
        except (KeyError, ValueError):
            continue
    if not rows:
        return

    width, height, margin = 1000, 520, 80
    maximum = max(value for _, value in rows) or 1
    bar_width = max(8, (width - 2 * margin) // len(rows) - 4)
    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}">',
        '<rect width="100%" height="100%" fill="#f7f4ee"/>',
        '<text x="80" y="42" font-family="sans-serif" font-size="24" fill="#17202a">p95 latency by result row</text>',
        f'<line x1="{margin}" y1="{height-margin}" x2="{width-margin}" y2="{height-margin}" stroke="#17202a"/>',
    ]
    for index, (label, value) in enumerate(rows):
        x = margin + index * (bar_width + 4)
        bar_height = (height - 2 * margin) * value / maximum
        y = height - margin - bar_height
        svg.append(f'<rect x="{x}" y="{y:.1f}" width="{bar_width}" height="{bar_height:.1f}" fill="#d45d3d"/>')
        svg.append(f'<text x="{x + bar_width / 2:.1f}" y="{height-margin+18}" text-anchor="middle" font-family="sans-serif" font-size="10" transform="rotate(45 {x + bar_width / 2:.1f},{height-margin+18})">{label}</text>')
    svg.append('</svg>')
    (RESULTS / "p95_latency.svg").write_text("\n".join(svg), encoding="utf-8")
    print(f"Wrote {RESULTS / 'p95_latency.svg'}")


if __name__ == "__main__":
    normalize()
    chart()
