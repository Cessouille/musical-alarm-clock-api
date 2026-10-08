#!/usr/bin/env python3
"""CI gate: fail the build if a non-whitelisted license shows up in the
license-maven-plugin scan (target/generated-resources/licenses.xml).

A dependency that lists a non-whitelisted license is allowed through only if
it has an explicit, reviewed entry in REVIEWED_EXCEPTIONS (documented in
licenses.md) — e.g. a dependency dual-licensed under a whitelisted license
where that branch was deliberately elected.
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

LICENSES_XML = Path("target/generated-resources/licenses.xml")

# Project-approved license families (substring match, case-insensitive).
WHITELIST = [
    "Apache",       # Apache-2.0 / "Apache License, Version 2.0" / "The Apache Software License..."
    "MIT",
    "BSD",
    "EPL",          # Eclipse Public License - permissive-enough, weak copyleft at file level, elected branch
    "Eclipse Public License",        # long spelling of EPL used by e.g. JUnit 5 (EPL-2.0, test scope only)
    "Eclipse Distribution License",  # EDL 1.0 == BSD-3-Clause (Jakarta XML Bind / Activation, test scope only)
    "EDL",
]

# (groupId:artifactId, license name as reported by the scanner) -> justification.
# Only add an entry here after writing the corresponding decision sheet in licenses.md.
REVIEWED_EXCEPTIONS = {
    ("ch.qos.logback:logback-classic", "LGPL-2.1-only"):
        "Dual-licensed EPL-2.0/LGPL-2.1 by upstream; EPL-2.0 branch is elected. See licenses.md.",
    ("ch.qos.logback:logback-core", "LGPL-2.1-only"):
        "Dual-licensed EPL-2.0/LGPL-2.1 by upstream; EPL-2.0 branch is elected. See licenses.md.",
    ("jakarta.annotation:jakarta.annotation-api", "GPL2 w/ CPE"):
        "Dual-licensed EPL-2.0/GPL2+Classpath-Exception by upstream; EPL-2.0 branch is elected "
        "(and CPE removes copyleft propagation on the GPL branch anyway). See licenses.md.",
}

NS = {"m": "https://mojo.codehaus.org/license-maven-plugin"}


def is_whitelisted(license_name: str) -> bool:
    return any(term.lower() in license_name.lower() for term in WHITELIST)


def main() -> int:
    if not LICENSES_XML.exists():
        print(f"ERROR: {LICENSES_XML} not found. Run `mvn license:download-licenses` first.")
        return 2

    root = ET.parse(LICENSES_XML).getroot()
    violations = []

    for dep in root.findall(".//dependency"):
        group_id = dep.findtext("groupId")
        artifact_id = dep.findtext("artifactId")
        key = f"{group_id}:{artifact_id}"
        license_names = [lic.findtext("name") or "UNKNOWN" for lic in dep.findall("./licenses/license")]

        if not license_names:
            violations.append((key, "UNKNOWN", "no license metadata returned by scanner"))
            continue

        for name in license_names:
            if is_whitelisted(name):
                continue
            exception = REVIEWED_EXCEPTIONS.get((key, name))
            if exception:
                continue
            violations.append((key, name, "not in whitelist and no reviewed exception"))

    if violations:
        print("License whitelist check FAILED:\n")
        for key, name, reason in violations:
            print(f"  - {key}: '{name}' ({reason})")
        print(f"\n{len(violations)} violation(s). Add a reviewed exception in scripts/check_license_whitelist.py")
        print("(with a matching decision sheet in licenses.md) or remove/replace the dependency.")
        return 1

    print("License whitelist check PASSED: no unreviewed non-whitelisted licenses found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
