#!/usr/bin/env python3
"""Génère le tableau « package / licence / version / fraîcheur » du README.

Entrée : target/generated-resources/licenses.xml (mvn license:download-licenses).
Fraîcheur : dernière version stable publiée sur Maven Central (maven-metadata.xml).
Usage : python scripts/dependency_report.py [--readme README.md]
"""
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

LICENSES_XML = Path("target/generated-resources/licenses.xml")
CENTRAL = "https://repo1.maven.org/maven2"
START_MARKER = "<!-- DEPS:START -->"
END_MARKER = "<!-- DEPS:END -->"
UNSTABLE_TOKEN = re.compile(r"^(alpha|beta|rc|cr|m|b|preview|ea|dev)\d*$|^snapshot$", re.IGNORECASE)


def is_stable(version: str) -> bool:
    return not any(UNSTABLE_TOKEN.match(token) for token in re.split(r"[.\-_]", version))


def numeric_key(version: str) -> tuple:
    numbers = [int(n) for n in re.findall(r"\d+", version)]
    return tuple(numbers + [0] * (6 - len(numbers)))


def latest_stable(versions):
    stable = [v for v in versions if is_stable(v)]
    return max(stable, key=numeric_key) if stable else None


def classify(installed: str, latest) -> str:
    if latest is None:
        return "inconnue"
    installed_key, latest_key = numeric_key(installed), numeric_key(latest)
    if installed_key >= latest_key:
        return "à jour"
    if installed_key[0] < latest_key[0]:
        return "majeure en retard"
    return "mineure/patch en retard"


def fetch_versions(group_id: str, artifact_id: str):
    url = f"{CENTRAL}/{group_id.replace('.', '/')}/{artifact_id}/maven-metadata.xml"
    request = urllib.request.Request(url, headers={"User-Agent": "MusicalAlarmClockTP-license-audit/1.0"})
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            root = ET.fromstring(response.read())
    except Exception as error:  # network or parse failure: report "inconnue", never crash the report
        print(f"WARN: cannot read metadata for {group_id}:{artifact_id}: {error}", file=sys.stderr)
        return []
    return [v.text for v in root.findall(".//version") if v.text]


def build_table() -> str:
    root = ET.parse(LICENSES_XML).getroot()
    rows = []
    for dep in root.findall(".//dependency"):
        group_id, artifact_id, version = (dep.findtext(t) for t in ("groupId", "artifactId", "version"))
        licenses = " / ".join(l.findtext("name") or "UNKNOWN" for l in dep.findall("./licenses/license")) or "UNKNOWN"
        latest = latest_stable(fetch_versions(group_id, artifact_id))
        rows.append((f"{group_id}:{artifact_id}", version, latest or "?", classify(version, latest), licenses))
    rows.sort()
    lines = ["| Package | Version installée | Dernière stable | Fraîcheur | Licence(s) |",
             "|---|---|---|---|---|"]
    lines += [f"| {p} | {v} | {l} | {f} | {lic} |" for p, v, l, f, lic in rows]
    return "\n".join(lines)


def replace_between_markers(text: str, table: str) -> str:
    start, end = text.find(START_MARKER), text.find(END_MARKER)
    if start == -1 or end == -1 or end < start:
        raise ValueError(f"README must contain {START_MARKER} and {END_MARKER}")
    return text[:start + len(START_MARKER)] + "\n" + table + "\n" + text[end:]


def main(argv) -> int:
    if not LICENSES_XML.exists():
        print(f"ERROR: {LICENSES_XML} not found. Run `mvn license:download-licenses` first.")
        return 2
    table = build_table()
    if len(argv) == 3 and argv[1] == "--readme":
        readme = Path(argv[2])
        readme.write_text(replace_between_markers(readme.read_text(encoding="utf-8"), table), encoding="utf-8")
        print(f"{argv[2]} updated.")
    else:
        print(table)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
