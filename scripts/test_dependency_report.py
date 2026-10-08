import unittest
import json
import tempfile
from pathlib import Path

from dependency_report import classify, is_stable, latest_stable, replace_between_markers, runtime_artifacts, scope_of


class DependencyReportTest(unittest.TestCase):
    def test_is_stable_rejects_prerelease_tokens(self):
        for version in ["4.2.0-M2", "2.0.0-beta1", "1.0.0-RC1", "3.0.0-SNAPSHOT", "1.0-alpha", "5.0.0.CR2"]:
            self.assertFalse(is_stable(version), version)

    def test_is_stable_accepts_release_versions(self):
        for version in ["1.5.38", "9.1.3.Final", "7.0.9", "2.0.18"]:
            self.assertTrue(is_stable(version), version)

    def test_latest_stable_ignores_prereleases_and_compares_numerically(self):
        versions = ["1.9.0", "1.10.0", "2.0.0-M1", "1.10.0-rc1"]
        self.assertEqual("1.10.0", latest_stable(versions))

    def test_latest_stable_is_none_without_stable_version(self):
        self.assertIsNone(latest_stable(["1.0.0-alpha"]))

    def test_classify(self):
        self.assertEqual("à jour", classify("1.5.38", "1.5.38"))
        self.assertEqual("à jour", classify("1.5.39", "1.5.38"))
        self.assertEqual("mineure/patch en retard", classify("1.5.1", "1.5.38"))
        self.assertEqual("majeure en retard", classify("1.5.1", "2.0.0"))
        self.assertEqual("inconnue", classify("1.0.0", None))

    def test_replace_between_markers_replaces_only_the_block(self):
        text = "avant\n<!-- DEPS:START -->\nancien\n<!-- DEPS:END -->\naprès\n"
        self.assertEqual("avant\n<!-- DEPS:START -->\nnouveau\n<!-- DEPS:END -->\naprès\n",
                         replace_between_markers(text, "nouveau"))

    def test_replace_between_markers_fails_without_markers(self):
        with self.assertRaises(ValueError):
            replace_between_markers("rien", "x")


if __name__ == "__main__":
    unittest.main()

    def test_scope_is_derived_from_the_sbom(self):
        with tempfile.TemporaryDirectory() as tmp:
            sbom = Path(tmp) / "bom.json"
            sbom.write_text(json.dumps({"components": [{"group": "org.springframework", "name": "spring-core"}]}),
                            encoding="utf-8")
            runtime = runtime_artifacts(sbom)
        self.assertEqual("compile/runtime", scope_of("org.springframework:spring-core", runtime))
        self.assertEqual("test", scope_of("org.mockito:mockito-core", runtime))

    def test_scope_is_unknown_without_sbom(self):
        self.assertIsNone(runtime_artifacts(Path("does-not-exist.json")))
        self.assertEqual("?", scope_of("a:b", None))
