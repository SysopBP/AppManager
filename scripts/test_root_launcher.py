"""Host regression checks for the real native launcher; no Android paths are written."""
import ctypes
import pathlib
import subprocess
import tempfile
import unittest

SOURCE = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/cpp/run_server.c"


class LauncherTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.root = pathlib.Path(cls.temp.name)
        cls.binary = cls.root / "launcher"
        subprocess.run(["cc", "-Wall", "-Wextra", "-Werror", "-Wno-sign-compare", str(SOURCE), "-o", str(cls.binary)], check=True)
        library = cls.root / "launcher.so"
        subprocess.run(["cc", "-shared", "-fPIC", str(SOURCE), "-o", str(library)], check=True)
        cls.lib = ctypes.CDLL(str(library))
        cls.lib.copy_file.argtypes = [ctypes.c_char_p, ctypes.c_char_p]

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def run_launcher(self, am, main, app="io.github.sysopbp.sphere.debug"):
        return subprocess.run([str(self.binary), "60001", "test-token", am, main, app, "0", "1"], capture_output=True, text=True)

    def test_private_paths_pass_validation(self):
        base = "/data/user/0/io.github.sysopbp.sphere.debug/files/server/"
        result = self.run_launcher(base + "am.jar", base + "main.jar")
        self.assertNotIn("Invalid", result.stderr)
        self.assertIn("Starting am_local_server", result.stdout)

    def test_legacy_names_pass_validation(self):
        self.assertNotIn("Invalid", self.run_launcher("am.jar", "main.jar").stderr)

    def test_arbitrary_and_traversal_paths_rejected(self):
        for path in ("/tmp/am.jar", "../am.jar", "/data/user/0/other/files/server/am.jar"):
            self.assertIn("Invalid server asset paths", self.run_launcher(path, "main.jar").stderr)

    def test_copy_replaces_symlink_and_is_read_only(self):
        source, target, victim = [self.root / name for name in ("source", "target", "victim")]
        source.write_bytes(b"dex-asset")
        victim.write_bytes(b"unchanged")
        target.symlink_to(victim)
        self.assertEqual(0, self.lib.copy_file(bytes(source), bytes(target)))
        self.assertEqual(b"dex-asset", target.read_bytes())
        self.assertEqual(b"unchanged", victim.read_bytes())
        self.assertEqual(0o400, target.stat().st_mode & 0o777)


if __name__ == "__main__":
    unittest.main()
