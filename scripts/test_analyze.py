import tempfile
import unittest
from pathlib import Path
from analyze import summarize


class AnalysisTest(unittest.TestCase):
    def test_filters_menu_boundaries_and_uses_slowest_tail_mean(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "frames.csv"
            rows = ["frame,frame_time_ns,in_world,menu_open"]
            for i in range(198):
                rows.append(f"{i},10000000,true,false")
            rows += ["198,30000000,true,false", "199,50000000,true,false", "200,1000000000,true,true"]
            path.write_text("\n".join(rows), encoding="utf-8")
            stats = summarize(path)
            self.assertEqual(stats["frames"], 200)
            self.assertEqual(stats["excluded"], 1)
            self.assertAlmostEqual(stats["averageFps"], 200 / 2.06)
            self.assertEqual(stats["onePercentLowFps"], 25)
            self.assertEqual(stats["p99Ms"], 10)
            self.assertEqual(summarize(path, True)["frames"], 201)


if __name__ == "__main__":
    unittest.main()
