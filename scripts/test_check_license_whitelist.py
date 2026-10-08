import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

import check_license_whitelist as gate


def scan(*dependencies):
    """dependencies: (groupId, artifactId, [license names])"""
    root = ET.Element("licenseSummary")
    deps = ET.SubElement(root, "dependencies")
    for group_id, artifact_id, licenses in dependencies:
        dep = ET.SubElement(deps, "dependency")
        ET.SubElement(dep, "groupId").text = group_id
        ET.SubElement(dep, "artifactId").text = artifact_id
        container = ET.SubElement(dep, "licenses")
        for name in licenses:
            ET.SubElement(ET.SubElement(container, "license"), "name").text = name
    return root


class LicenseGateTest(unittest.TestCase):
    def test_permissive_licenses_pass(self):
        root = scan(("a", "apache", ["The Apache Software License, Version 2.0"]),
                    ("a", "mit", ["The MIT License"]),
                    ("a", "bsd", ["BSD-3-Clause"]),
                    ("a", "edl", ["Eclipse Distribution License - v 1.0"]))
        self.assertEqual([], gate.find_violations(root))

    def test_strong_copyleft_is_rejected(self):
        for name in ("GNU Affero General Public License v3", "GPL-3.0", "AGPL-3.0-only"):
            violations = gate.find_violations(scan(("com.itextpdf", "itextpdf", [name])))
            self.assertEqual(1, len(violations), name)

    def test_unknown_or_missing_license_is_rejected(self):
        self.assertEqual(1, len(gate.find_violations(scan(("a", "nolicense", [])))))

    def test_epl_is_not_whitelisted_wholesale(self):
        violations = gate.find_violations(scan(("com.example", "new-epl-lib", ["Eclipse Public License v2.0"])))
        self.assertEqual([("com.example:new-epl-lib", "Eclipse Public License v2.0",
                           "not in whitelist and no reviewed exception")], violations)

    def test_reviewed_exception_passes_only_for_that_artifact_and_license(self):
        reviewed = scan(("ch.qos.logback", "logback-classic", ["EPL-2.0", "LGPL-2.1-only"]))
        self.assertEqual([], gate.find_violations(reviewed))

        other_license = scan(("ch.qos.logback", "logback-classic", ["GPL-3.0"]))
        self.assertEqual(1, len(gate.find_violations(other_license)))

        other_artifact = scan(("ch.qos.logback", "logback-other", ["EPL-2.0"]))
        self.assertEqual(1, len(gate.find_violations(other_artifact)))

    def test_whitelist_matches_whole_words_only(self):
        self.assertFalse(gate.is_whitelisted("Permit Public License"))
        self.assertFalse(gate.is_whitelisted("Seattle Public License"))
        self.assertTrue(gate.is_whitelisted("Apache-2.0"))

    def test_main_returns_error_code_when_scan_is_missing(self):
        self.assertEqual(2, gate.main(Path("does-not-exist.xml")))

    def test_main_exit_codes_follow_the_scan(self):
        with tempfile.TemporaryDirectory() as tmp:
            good, bad = Path(tmp) / "good.xml", Path(tmp) / "bad.xml"
            ET.ElementTree(scan(("a", "apache", ["Apache-2.0"]))).write(good)
            ET.ElementTree(scan(("a", "agpl", ["AGPL-3.0-only"]))).write(bad)
            self.assertEqual(0, gate.main(good))
            self.assertEqual(1, gate.main(bad))


if __name__ == "__main__":
    unittest.main()
