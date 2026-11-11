#!/usr/bin/env python3
"""Install the bundled Lilt JAR into a user-selected Forge 1.12.2 game directory."""
import argparse
from datetime import datetime, timezone
from pathlib import Path
import shutil


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("game_directory", type=Path, help="The game directory of an existing Forge 1.12.2 instance")
    args = parser.parse_args()
    target = args.game_directory.expanduser().resolve(strict=True)
    if not target.is_dir():
        parser.error("game_directory must be an existing directory")
    root = Path(__file__).resolve().parents[1]
    name = "lilt-client-1.12.2-1.0.0.jar"
    candidates = [root / name, root / "build" / "libs" / name]
    source = next((path for path in candidates if path.is_file()), None)
    if source is None:
        parser.error("Lilt JAR not found. Extract the full release or run gradlew build first.")
    mods = target / "mods"
    mods.mkdir(exist_ok=True)
    destination = mods / name
    if destination.exists():
        if destination.read_bytes() == source.read_bytes():
            print(f"Already installed: {destination}")
            return
        backup = mods / (name + ".backup-" + datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%f"))
        shutil.copy2(destination, backup)
        print(f"Backed up previous JAR: {backup}")
    shutil.copy2(source, destination)
    print(f"Installed: {destination}")
    print("Launch this instance with Forge 14.23.5.2860 and Java 8. Open Lilt Studio with Right Shift.")


if __name__ == "__main__":
    main()
