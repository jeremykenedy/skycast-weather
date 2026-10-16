"""Require all lines and branches in the explicit host-logic report to pass."""

import sys
import xml.etree.ElementTree as ET

report = ET.parse(sys.argv[1]).getroot()
for metric in ("LINE", "BRANCH"):
    counter = next(item for item in report.findall("counter") if item.get("type") == metric)
    missed, covered = int(counter.get("missed")), int(counter.get("covered"))
    if missed or not covered:
        raise SystemExit(f"{metric}: {covered} covered, {missed} missed; 100% required")
    print(f"{metric}: {covered}/{covered}, 100%")
