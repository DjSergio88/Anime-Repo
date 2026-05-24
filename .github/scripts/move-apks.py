#!/usr/bin/env python3
"""Move APKs from build outputs to repo/apk directory."""

import shutil
from pathlib import Path

def main():
    repo_apk_dir = Path("repo/apk")
    repo_apk_dir.mkdir(parents=True, exist_ok=True)

    # Find all APKs in build directories
    apk_count = 0
    for apk in Path(".").rglob("*.apk"):
        # Skip APKs already in repo/apk
        if "repo/apk" in str(apk):
            continue
        # Skip test APKs
        if "androidTest" in str(apk) or "test" in str(apk).lower():
            continue

        dest = repo_apk_dir / apk.name
        shutil.copy2(apk, dest)
        apk_count += 1
        print(f"Copied: {apk} -> {dest}")

    print(f"\nTotal APKs collected: {apk_count}")

    if apk_count == 0:
        print("WARNING: No APKs found!")
        return 1

    return 0

if __name__ == "__main__":
    exit(main())
