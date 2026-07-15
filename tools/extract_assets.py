#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import io
import shutil
import zipfile
from pathlib import Path

from PIL import Image


def decoded_hash(image: Image.Image) -> str:
    return hashlib.sha256(image.convert("RGB").tobytes()).hexdigest()


def extract(zip_path: Path, project_root: Path) -> None:
    work = project_root / "build" / "original-game"
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True)

    with zipfile.ZipFile(zip_path) as archive:
        archive.extractall(work)

    exe_candidates = list(work.rglob("PoleForWindows.exe"))
    base_candidates = list(work.rglob("base.dat"))
    if not exe_candidates or not base_candidates:
        raise SystemExit("PoleForWindows.exe or Gamedata/base.dat was not found")

    exe = exe_candidates[0].read_bytes()
    drawable = project_root / "app" / "src" / "main" / "res" / "drawable-nodpi"
    assets = project_root / "app" / "src" / "main" / "assets"
    drawable.mkdir(parents=True, exist_ok=True)
    assets.mkdir(parents=True, exist_ok=True)
    shutil.copy2(base_candidates[0], assets / "base.dat")

    starts = []
    cursor = 0
    while True:
        offset = exe.find(b"\xff\xd8\xff", cursor)
        if offset < 0:
            break
        starts.append(offset)
        cursor = offset + 1

    unique = []
    seen = set()
    for offset in starts:
        try:
            image = Image.open(io.BytesIO(exe[offset:]))
            image.load()
        except Exception:
            continue
        key = decoded_hash(image)
        if key in seen:
            continue
        seen.add(key)
        unique.append((offset, image.convert("RGB")))

    background = next((im for _, im in unique if im.size == (896, 527)), None)
    idle = next((im for _, im in unique if im.size == (228, 285)), None)
    talk = next((im for _, im in unique if im.size == (230, 287)), None)
    wheels = [im for _, im in unique if im.size == (224, 186)]

    if background is None or idle is None or talk is None or len(wheels) != 16:
        sizes = [im.size for _, im in unique]
        raise SystemExit(f"Unexpected EXE image layout: {sizes}")

    background.save(drawable / "stage_background.png", optimize=True)
    idle.save(drawable / "host_idle.png", optimize=True)
    talk.save(drawable / "host_talk.png", optimize=True)
    for index, image in enumerate(wheels):
        image.save(drawable / f"wheel_{index:02d}.png", optimize=True)

    print(f"Extracted original background, two host frames and {len(wheels)} wheel frames")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("zip_path", type=Path)
    parser.add_argument("project_root", type=Path)
    args = parser.parse_args()
    extract(args.zip_path.resolve(), args.project_root.resolve())


if __name__ == "__main__":
    main()
