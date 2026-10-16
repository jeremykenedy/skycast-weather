"""Check local documentation links and the eight responsive banner variants."""

import hashlib
import re
import xml.etree.ElementTree as ET
from pathlib import Path

root = Path(__file__).resolve().parent.parent
readme = (root / "README.md").read_text()
for document in [root / "README.md", *sorted((root / "docs").glob("*.md"))]:
    text = document.read_text()
    for path in re.findall(r"\]\(([^)#]+)(?:#[^)]*)?\)", text):
        if "://" not in path and not (document.parent / path).exists():
            raise SystemExit(f"Broken local link in {document.name}: {path}")
for document in (root / "docs").glob("*.md"):
    if f"docs/{document.name}" not in readme:
        raise SystemExit(f"Maintained guide missing from README: {document.name}")
references = set(re.findall(r'(?:src|srcset)="(art/[^"#]+)', readme))
for reference in references:
    if not (root / reference).is_file():
        raise SystemExit("Missing README image: " + reference)
for banner in (root / "art").glob("banner-*.svg"):
    ET.parse(banner)
    match = re.fullmatch(r"(banner-[a-z-]+)\.([a-f0-9]{12})\.svg", banner.name)
    if match:
        if hashlib.sha256(banner.read_bytes()).hexdigest()[:12] != match[2]:
            raise SystemExit("Incorrect banner content hash: " + banner.name)
        if "art/" + banner.name not in references:
            raise SystemExit("Unreferenced hashed banner: " + banner.name)
        if banner.read_bytes() != (root / "art" / (match[1] + ".svg")).read_bytes():
            raise SystemExit("Hashed banner differs from source: " + banner.name)
if len([name for name in references if name.endswith(".svg")]) != 8:
    raise SystemExit("README must reference all eight current banner variants")
print("Local documentation, screenshot links and all banner XML/hash references pass.")
