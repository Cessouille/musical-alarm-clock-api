#!/usr/bin/env python3
"""CI gate: fail the build if a non-permissive license shows up in the
license-maven-plugin scan (target/generated-resources/licenses.xml).

Only permissive families are whitelisted wholesale. Anything else (EPL, LGPL,
GPL...) is allowed through only if it has an explicit, reviewed entry in
REVIEWED_EXCEPTIONS (documented in README.md): the exception is keyed on the
exact artifact AND license name, so a new EPL dependency still fails the gate.
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

LICENSES_XML = Path("target/generated-resources/licenses.xml")

# Permissive license families (whole-word match, case-insensitive).
WHITELIST = [
    "Apache",       # Apache-2.0 / "Apache License, Version 2.0" / "The Apache Software License..."
    "MIT",
    "BSD",
    "Eclipse Distribution License",  # EDL 1.0 == BSD-3-Clause (Jakarta XML Bind / Activation)
    "EDL",
]

# (groupId:artifactId, license name as reported by the scanner) -> justification.
# Only add an entry here after writing the corresponding decision sheet in README.md.
REVIEWED_EXCEPTIONS = {
    ("ch.qos.logback:logback-classic", "EPL-2.0"):
        "Dual-licensed EPL-2.0/LGPL-2.1 by upstream; EPL-2.0 branch is elected. See README.md.",
    ("ch.qos.logback:logback-classic", "LGPL-2.1-only"):
        "Dual-licensed EPL-2.0/LGPL-2.1 by upstream; EPL-2.0 branch is elected. See README.md.",
    ("ch.qos.logback:logback-core", "EPL-2.0"):
        "Dual-licensed EPL-2.0/LGPL-2.1 by upstream; EPL-2.0 branch is elected. See README.md.",
    ("ch.qos.logback:logback-core", "LGPL-2.1-only"):
        "Dual-licensed EPL-2.0/LGPL-2.1 by upstream; EPL-2.0 branch is elected. See README.md.",
    ("jakarta.annotation:jakarta.annotation-api", "EPL 2.0"):
        "Dual-licensed EPL-2.0/GPL2+Classpath-Exception by upstream; EPL-2.0 branch is elected. See README.md.",
    ("jakarta.annotation:jakarta.annotation-api", "GPL2 w/ CPE"):
        "Dual-licensed EPL-2.0/GPL2+Classpath-Exception by upstream; EPL-2.0 branch is elected "
        "(and CPE removes copyleft propagation on the GPL branch anyway). See README.md.",
}
for _artifact in ("org.junit.jupiter:junit-jupiter", "org.junit.jupiter:junit-jupiter-api",
                  "org.junit.jupiter:junit-jupiter-engine", "org.junit.jupiter:junit-jupiter-params",
                  "org.junit.platform:junit-platform-commons", "org.junit.platform:junit-platform-engine"):
    REVIEWED_EXCEPTIONS[(_artifact, "Eclipse Public License v2.0")] = (
        "EPL-2.0, weak file-level copyleft; test scope only (never in the shipped artifact). See README.md.")


def is_whitelisted(license_name: str) -> bool:
    return any(re.search(rf"\b{re.escape(term)}\b", license_name, re.IGNORECASE) for term in WHITELIST)


def find_violations(root) -> list:
    violations = []
    for dep in root.findall(".//dependency"):
        key = f"{dep.findtext('groupId')}:{dep.findtext('artifactId')}"
        license_names = [lic.findtext("name") or "UNKNOWN" for lic in dep.findall("./licenses/license")]

        if not license_names:
            violations.append((key, "UNKNOWN", "no license metadata returned by scanner"))
            continue

        for name in license_names:
            if is_whitelisted(name) or (key, name) in REVIEWED_EXCEPTIONS:
                continue
            violations.append((key, name, "not in whitelist and no reviewed exception"))
    return violations


def main(licenses_xml: Path = LICENSES_XML) -> int:
    if not licenses_xml.exists():
        print(f"ERROR: {licenses_xml} not found. Run `mvn license:download-licenses` first.")
        return 2

    violations = find_violations(ET.parse(licenses_xml).getroot())

    if violations:
        print("License whitelist check FAILED:\n")
        for key, name, reason in violations:
            print(f"  - {key}: '{name}' ({reason})")
        print(f"\n{len(violations)} violation(s). Add a reviewed exception in scripts/check_license_whitelist.py")
        print("(with a matching decision sheet in README.md) or remove/replace the dependency.")
        return 1

    print("License whitelist check PASSED: no unreviewed non-whitelisted licenses found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
