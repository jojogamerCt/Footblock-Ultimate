#!/usr/bin/env python3
"""Generate the original assets for Footblock Ultimate's World Cup 2026 update.

The generator intentionally uses only Python's standard library. PNG files are
encoded directly, block/item models are assembled as JSON, and the whistle is
mathematically synthesized to PCM before ffmpeg converts it to mono Vorbis.

No tournament logos, trademarks, flags, mascots, or official artwork are used.
The shared ball design is an original three-ribbon pattern whose red, green,
and blue palette is simply a nod to the tournament's three host nations.
"""

from __future__ import annotations

import hashlib
import json
import math
import shutil
import struct
import subprocess
import sys
import wave
import zlib
from pathlib import Path
from typing import Iterable, Sequence


MOD_ID = "footblockultimate"
ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "common" / "src" / "main" / "resources"
ASSETS = RESOURCES / "assets" / MOD_ID
DATA = RESOURCES / "data" / MOD_ID

RGBA = tuple[int, int, int, int]
RGB = tuple[int, int, int]
GENERATED: list[Path] = []


def write_bytes(path: Path, payload: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(payload)
    GENERATED.append(path)


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(value, indent=2, ensure_ascii=False) + "\n"
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(payload)
    GENERATED.append(path)


class Canvas:
    """A tiny deterministic RGBA raster helper used instead of an image library."""

    def __init__(self, width: int, height: int, background: RGBA = (0, 0, 0, 0)) -> None:
        self.width = width
        self.height = height
        self.pixels = bytearray(background * (width * height))

    def pixel(self, x: int, y: int, color: RGB | RGBA) -> None:
        if not (0 <= x < self.width and 0 <= y < self.height):
            return
        rgba = (*color, 255) if len(color) == 3 else color
        index = (y * self.width + x) * 4
        self.pixels[index : index + 4] = bytes(rgba)

    def rect(self, x: int, y: int, width: int, height: int, color: RGB | RGBA) -> None:
        for py in range(y, y + height):
            for px in range(x, x + width):
                self.pixel(px, py, color)

    def shaded_rect(self, x: int, y: int, width: int, height: int, color: RGB) -> None:
        for py in range(y, y + height):
            for px in range(x, x + width):
                # A fixed arithmetic grain gives metal/fabric depth without RNG.
                delta = ((px * 17 + py * 29 + px * py * 3) % 11) - 5
                shaded = tuple(max(0, min(255, channel + delta)) for channel in color)
                self.pixel(px, py, shaded)

    def png(self) -> bytes:
        scanlines = bytearray()
        stride = self.width * 4
        for y in range(self.height):
            scanlines.append(0)  # PNG filter type: None
            start = y * stride
            scanlines.extend(self.pixels[start : start + stride])

        def chunk(kind: bytes, payload: bytes) -> bytes:
            checksum = zlib.crc32(kind + payload) & 0xFFFFFFFF
            return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", checksum)

        header = struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0)
        return (
            b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", header)
            + chunk(b"IDAT", zlib.compress(bytes(scanlines), level=9))
            + chunk(b"IEND", b"")
        )


def cube_uv_regions(u: int, v: int, x_size: int, y_size: int, z_size: int) -> dict[str, tuple[int, int, int, int]]:
    """Return Minecraft/Blockbench's six unfolded cube regions in texture pixels."""
    return {
        "west": (u, v + z_size, z_size, y_size),
        "north": (u + z_size, v + z_size, x_size, y_size),
        "east": (u + z_size + x_size, v + z_size, z_size, y_size),
        "south": (u + 2 * z_size + x_size, v + z_size, x_size, y_size),
        "up": (u + z_size, v, x_size, z_size),
        "down": (u + z_size + x_size, v, x_size, z_size),
    }


BALL_RIBBONS: tuple[RGB, RGB, RGB] = (
    (202, 45, 66),   # crimson ribbon
    (26, 145, 101),  # emerald ribbon
    (40, 91, 178),   # azure ribbon
)
BALL_IVORY: RGB = (236, 237, 224)
BALL_SEAM: RGB = (34, 43, 57)
BALL_GOLD: RGB = (232, 178, 61)


def paint_ball_face(canvas: Canvas, region: tuple[int, int, int, int], face_index: int) -> None:
    """Paint a small leather panel with three interleaved, original color ribbons."""
    x, y, width, height = region
    primary = BALL_RIBBONS[face_index % 3]
    secondary = BALL_RIBBONS[(face_index + 1) % 3]
    tertiary = BALL_RIBBONS[(face_index + 2) % 3]

    for local_y in range(height):
        for local_x in range(width):
            grain = ((x + local_x) * 13 + (y + local_y) * 7 + face_index * 5) % 9 - 4
            color: RGB = tuple(max(0, min(255, c + grain)) for c in BALL_IVORY)

            if min(width, height) > 1 and (
                local_x in (0, width - 1) or local_y in (0, height - 1)
            ):
                color = BALL_SEAM
            elif (local_x + local_y + face_index) % max(3, min(width, height)) == 0:
                color = primary
            elif (local_x - local_y - face_index) % max(4, min(width, height) + 1) == 0:
                color = secondary
            elif (2 * local_x + local_y + face_index) % max(5, width + height) == 0:
                color = tertiary

            canvas.pixel(x + local_x, y + local_y, color)

    if width >= 3 and height >= 3:
        canvas.pixel(x + width // 2, y + height // 2, BALL_GOLD)


def paint_cap_badge(canvas: Canvas, region: tuple[int, int, int, int], color: RGB) -> None:
    """Give each opposing cap pair one of the three host-ribbon colors."""
    x, y, width, height = region
    canvas.shaded_rect(x, y, width, height, color)
    if width >= 3 and height >= 3:
        for px, py in (
            (x + width // 2, y + 1),
            (x + 1, y + height // 2),
            (x + width - 2, y + height // 2),
            (x + width // 2, y + height - 2),
        ):
            canvas.pixel(px, py, BALL_IVORY)
        canvas.pixel(x + width // 2, y + height // 2, BALL_GOLD)


BALL_CUBES: tuple[tuple[int, int, int, int, int], ...] = (
    (0, 0, 6, 6, 6),
    (0, 20, 4, 1, 4),
    (0, 25, 4, 1, 4),
    (16, 20, 4, 4, 1),
    (16, 25, 4, 4, 1),
    (32, 20, 1, 4, 4),
    (32, 25, 1, 4, 4),
)


def make_world_cup_ball_texture() -> bytes:
    canvas = Canvas(64, 64)
    face_index = 0
    regions_by_cube: list[dict[str, tuple[int, int, int, int]]] = []
    for u, v, sx, sy, sz in BALL_CUBES:
        regions = cube_uv_regions(u, v, sx, sy, sz)
        regions_by_cube.append(regions)
        for direction in ("west", "north", "east", "south", "up", "down"):
            paint_ball_face(canvas, regions[direction], face_index)
            face_index += 1

    # The six outer panels form three opposing color pairs. This is not a flag
    # or tournament emblem, just a readable motif while the entity is rolling.
    outward_faces = (
        (1, "up", BALL_RIBBONS[0]),
        (2, "down", BALL_RIBBONS[0]),
        (3, "north", BALL_RIBBONS[1]),
        (4, "south", BALL_RIBBONS[1]),
        (5, "west", BALL_RIBBONS[2]),
        (6, "east", BALL_RIBBONS[2]),
    )
    for cube_index, direction, color in outward_faces:
        paint_cap_badge(canvas, regions_by_cube[cube_index][direction], color)

    return canvas.png()


def make_cube_faces(u: int, v: int, x_size: int, y_size: int, z_size: int, texture: str = "#ball") -> dict[str, object]:
    """Convert 64px entity UV coordinates to the item model's 0..16 space."""
    regions = cube_uv_regions(u, v, x_size, y_size, z_size)
    faces: dict[str, object] = {}
    for direction in ("down", "up", "north", "south", "west", "east"):
        x, y, width, height = regions[direction]
        faces[direction] = {
            "uv": [x / 4.0, y / 4.0, (x + width) / 4.0, (y + height) / 4.0],
            "texture": texture,
        }
    return faces


def world_cup_ball_model() -> dict[str, object]:
    elements = (
        ([5, 5, 5], [11, 11, 11]),
        ([6, 11, 6], [10, 12, 10]),
        ([6, 4, 6], [10, 5, 10]),
        ([6, 6, 4], [10, 10, 5]),
        ([6, 6, 11], [10, 10, 12]),
        ([4, 6, 6], [5, 10, 10]),
        ([11, 6, 6], [12, 10, 10]),
    )
    return {
        "texture_size": [64, 64],
        "ambientocclusion": False,
        "textures": {"ball": f"{MOD_ID}:item/world_cup_2026_ball"},
        "elements": [
            {
                "from": start,
                "to": end,
                "faces": make_cube_faces(*BALL_CUBES[index]),
            }
            for index, (start, end) in enumerate(elements)
        ],
        "display": {
            "thirdperson_righthand": {
                "rotation": [0, 0, 0],
                "translation": [0, 3, 1],
                "scale": [0.85, 0.85, 0.85],
            },
            "thirdperson_lefthand": {
                "rotation": [0, 0, 0],
                "translation": [0, 3, 1],
                "scale": [0.85, 0.85, 0.85],
            },
            "firstperson_righthand": {
                "rotation": [0, 0, 0],
                "translation": [1.13, 3.2, 1.13],
                "scale": [0.75, 0.75, 0.75],
            },
            "firstperson_lefthand": {
                "rotation": [0, 0, 0],
                "translation": [1.13, 3.2, 1.13],
                "scale": [0.75, 0.75, 0.75],
            },
            "ground": {"translation": [0, 2, 0], "scale": [0.8, 0.8, 0.8]},
            "gui": {"rotation": [30, 45, 0], "scale": [0.85, 0.85, 0.85]},
            "fixed": {"scale": [0.8, 0.8, 0.8]},
        },
    }


def face_set(texture: str, uv: Sequence[float] = (0, 0, 16, 16)) -> dict[str, object]:
    return {
        direction: {"uv": list(uv), "texture": texture}
        for direction in ("down", "up", "north", "south", "west", "east")
    }


def cuboid(start: Sequence[float], end: Sequence[float], texture: str, uv: Sequence[float]) -> dict[str, object]:
    return {"from": list(start), "to": list(end), "faces": face_set(texture, uv)}


def make_whistle_texture() -> bytes:
    canvas = Canvas(32, 32, (48, 38, 28, 255))
    # Four reusable material zones. Model UVs select these quadrants.
    canvas.shaded_rect(0, 0, 16, 16, (218, 177, 68))       # brushed brass
    canvas.shaded_rect(16, 0, 16, 16, (249, 216, 121))     # lit brass
    canvas.shaded_rect(0, 16, 16, 16, (91, 68, 45))        # shadowed metal
    canvas.shaded_rect(16, 16, 16, 16, (31, 39, 50))       # air channel

    for y in range(0, 16, 4):
        for x in range(16):
            canvas.pixel(x, y, (244, 204, 105))
    for y in range(16):
        canvas.pixel(16 + y, y, (255, 234, 154))
    for y in range(16, 32):
        canvas.pixel(16, y, (61, 75, 92))
        canvas.pixel(31, y, (17, 22, 29))
    return canvas.png()


def whistle_model() -> dict[str, object]:
    gold = (0, 0, 8, 8)
    light = (8, 0, 16, 8)
    bronze = (0, 8, 8, 16)
    dark = (8, 8, 16, 16)
    elements = [
        # Resonating chamber and raised spine.
        cuboid((4, 5, 5), (11, 11, 11), "#metal", gold),
        cuboid((5, 11, 6), (11, 13, 10), "#metal", light),
        cuboid((5, 4, 6), (10, 5, 10), "#metal", bronze),
        # Tapered-looking mouthpiece assembled from three genuine cuboids.
        cuboid((10, 7, 6), (13, 10, 10), "#metal", light),
        cuboid((13, 7.5, 6.5), (15, 9.5, 9.5), "#metal", gold),
        cuboid((15, 8, 7), (16, 9, 9), "#metal", bronze),
        # Dark air slot makes the chamber read from the inventory camera.
        cuboid((8, 11.75, 6.75), (12.5, 13.25, 9.25), "#metal", dark),
        # Four bars form a real open attachment loop instead of a flat sprite.
        cuboid((1, 4, 6), (5, 5, 10), "#metal", bronze),
        cuboid((1, 11, 6), (5, 12, 10), "#metal", light),
        cuboid((1, 5, 6), (2, 11, 10), "#metal", gold),
        cuboid((4, 5, 6), (5, 11, 10), "#metal", gold),
    ]
    return {
        "ambientocclusion": False,
        "textures": {"metal": f"{MOD_ID}:item/referee_whistle"},
        "elements": elements,
        "display": {
            "thirdperson_righthand": {
                "rotation": [0, -90, 35],
                "translation": [0, 2.5, 1],
                "scale": [0.72, 0.72, 0.72],
            },
            "thirdperson_lefthand": {
                "rotation": [0, 90, -35],
                "translation": [0, 2.5, 1],
                "scale": [0.72, 0.72, 0.72],
            },
            "firstperson_righthand": {
                "rotation": [0, -90, 20],
                "translation": [1.5, 3.2, 1.2],
                "scale": [0.78, 0.78, 0.78],
            },
            "firstperson_lefthand": {
                "rotation": [0, 90, -20],
                "translation": [-1.5, 3.2, 1.2],
                "scale": [0.78, 0.78, 0.78],
            },
            "ground": {"translation": [0, 2.2, 0], "scale": [0.65, 0.65, 0.65]},
            "gui": {"rotation": [30, 135, 0], "translation": [0, 0.5, 0], "scale": [0.9, 0.9, 0.9]},
            "fixed": {"rotation": [0, 90, 0], "scale": [0.75, 0.75, 0.75]},
        },
    }


def make_goal_line_texture(team: str) -> bytes:
    if team == "red":
        base, edge, glow = (111, 18, 34), (214, 43, 65), (255, 171, 104)
    elif team == "blue":
        base, edge, glow = (20, 45, 112), (42, 106, 208), (105, 215, 255)
    else:
        raise ValueError(f"Unknown goal-line team: {team}")

    canvas = Canvas(16, 16)
    canvas.shaded_rect(0, 0, 16, 16, base)
    for index in range(16):
        canvas.pixel(index, 0, edge)
        canvas.pixel(index, 15, edge)
        canvas.pixel(0, index, edge)
        canvas.pixel(15, index, edge)

    # The pale center lane reads as a field sensor. The gold arrow points north;
    # blockstate rotations keep it aligned with the block's scoring direction.
    for y in (7, 8):
        for x in range(1, 15):
            canvas.pixel(x, y, glow if (x + y) % 3 else (245, 225, 155))
    arrow = (235, 189, 67)
    for y in range(3, 13):
        canvas.pixel(7, y, arrow)
        canvas.pixel(8, y, arrow)
    for offset in range(4):
        y = 2 + offset
        canvas.pixel(7 - offset, y, arrow)
        canvas.pixel(8 + offset, y, arrow)
    return canvas.png()


def goal_line_model(team: str) -> dict[str, object]:
    texture = f"{MOD_ID}:block/{team}_goal_line"
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {"particle": texture, "line": texture},
        "elements": [
            {
                "from": [0, 0, 0],
                "to": [16, 1, 16],
                "faces": {
                    "down": {"uv": [0, 0, 16, 16], "texture": "#line", "cullface": "down"},
                    "up": {"uv": [0, 0, 16, 16], "texture": "#line"},
                    "north": {"uv": [0, 0, 16, 1], "texture": "#line"},
                    "south": {"uv": [0, 0, 16, 1], "texture": "#line"},
                    "west": {"uv": [0, 0, 16, 1], "texture": "#line"},
                    "east": {"uv": [0, 0, 16, 1], "texture": "#line"},
                },
            }
        ],
        "display": {
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2, 0], "scale": [0.55, 0.55, 0.55]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2, 0], "scale": [0.55, 0.55, 0.55]},
            "firstperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 3, 0], "scale": [0.65, 0.65, 0.65]},
            "firstperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 3, 0], "scale": [0.65, 0.65, 0.65]},
            "ground": {"translation": [0, 2.5, 0], "scale": [0.65, 0.65, 0.65]},
            "gui": {"rotation": [30, 45, 0], "translation": [0, 3.5, 0], "scale": [0.9, 0.9, 0.9]},
            "fixed": {"rotation": [90, 0, 0], "translation": [0, 0, -7.2], "scale": [0.8, 0.8, 0.8]},
        },
    }


def make_trophy_texture() -> bytes:
    canvas = Canvas(32, 32)
    canvas.shaded_rect(0, 0, 16, 16, (220, 169, 49))       # warm gold
    canvas.shaded_rect(16, 0, 16, 16, (255, 218, 101))     # highlight gold
    canvas.shaded_rect(0, 16, 16, 16, (65, 47, 39))        # walnut-bronze base
    canvas.shaded_rect(16, 16, 16, 16, (34, 40, 51))       # inset shadow

    # Three abstract ribbons live in the accent quadrant, with no flag shapes.
    for y in range(16, 32):
        for x in range(16, 32):
            local = x - 16
            if (local + y) % 9 < 3:
                color = BALL_RIBBONS[0]
            elif (local + y) % 9 < 6:
                color = BALL_RIBBONS[1]
            else:
                color = BALL_RIBBONS[2]
            canvas.pixel(x, y, color)

    for y in range(16):
        canvas.pixel(y, y, (255, 236, 145))
        canvas.pixel(31 - y, y, (241, 191, 65))
    return canvas.png()


def trophy_model() -> dict[str, object]:
    gold = (0, 0, 8, 8)
    light = (8, 0, 16, 8)
    base = (0, 8, 8, 16)
    accent = (8, 8, 16, 16)
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {
            "particle": f"{MOD_ID}:block/world_cup_trophy",
            "trophy": f"{MOD_ID}:block/world_cup_trophy",
        },
        "elements": [
            cuboid((3, 0, 3), (13, 2, 13), "#trophy", base),
            cuboid((4, 2, 4), (12, 3, 12), "#trophy", gold),
            cuboid((5, 3, 5), (11, 4, 11), "#trophy", light),
            cuboid((7, 4, 7), (9, 9, 9), "#trophy", gold),
            cuboid((6, 7, 6), (10, 8, 10), "#trophy", accent),
            cuboid((6, 8, 6), (10, 10, 10), "#trophy", light),
            cuboid((5, 10, 5), (11, 13, 11), "#trophy", gold),
            cuboid((4, 13, 4), (12, 14, 12), "#trophy", light),
            # Handles remain inside the block's custom collision silhouette.
            cuboid((4, 10, 6), (5, 13, 10), "#trophy", light),
            cuboid((11, 10, 6), (12, 13, 10), "#trophy", gold),
            # Three raised ribbon stones crown the original trophy design.
            cuboid((5, 14, 5), (7, 16, 7), "#trophy", accent),
            cuboid((9, 14, 5), (11, 16, 7), "#trophy", accent),
            cuboid((7, 14, 9), (9, 16, 11), "#trophy", accent),
        ],
        "display": {
            "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1.5, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_lefthand": {"rotation": [0, 45, 0], "translation": [0, 1.5, 0], "scale": [0.5, 0.5, 0.5]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.58, 0.58, 0.58]},
            "firstperson_lefthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.58, 0.58, 0.58]},
            "ground": {"translation": [0, 1.5, 0], "scale": [0.5, 0.5, 0.5]},
            "gui": {"rotation": [30, 225, 0], "scale": [0.78, 0.78, 0.78]},
            "fixed": {"scale": [0.65, 0.65, 0.65]},
        },
    }


def make_score_manager_texture() -> bytes:
    canvas = Canvas(32, 32)
    canvas.shaded_rect(0, 0, 16, 16, (24, 31, 47))
    canvas.shaded_rect(16, 0, 16, 16, (102, 118, 132))
    canvas.shaded_rect(0, 16, 16, 16, (173, 31, 45))
    canvas.shaded_rect(16, 16, 16, 16, (74, 211, 221))
    for index in range(3, 13):
        canvas.pixel(index, 3, (235, 189, 67))
        canvas.pixel(index, 12, (235, 189, 67))
        canvas.pixel(3, index, (235, 189, 67))
        canvas.pixel(12, index, (235, 189, 67))
    for y in range(19, 29):
        for x in range(3, 13):
            if (x - 8) ** 2 + (y - 24) ** 2 <= 18:
                canvas.pixel(x, y, (235, 62, 73))
    return canvas.png()


def score_manager_model() -> dict[str, object]:
    dark = (0, 0, 8, 8)
    steel = (8, 0, 16, 8)
    reset = (0, 8, 8, 16)
    glow = (8, 8, 16, 16)
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {
            "particle": f"{MOD_ID}:block/score_manager_console",
            "console": f"{MOD_ID}:block/score_manager_console",
        },
        "elements": [
            cuboid((1, 0, 1), (15, 3, 15), "#console", dark),
            cuboid((2, 2, 2), (14, 4, 14), "#console", steel),
            cuboid((3, 4, 3), (13, 10, 13), "#console", dark),
            cuboid((2, 10, 2), (14, 13, 14), "#console", steel),
            cuboid((5, 13, 5), (11, 14, 11), "#console", dark),
            cuboid((6, 14, 6), (10, 15, 10), "#console", reset),
            cuboid((3, 12.5, 3), (5, 13.5, 5), "#console", glow),
            cuboid((11, 12.5, 3), (13, 13.5, 5), "#console", glow),
            cuboid((3, 12.5, 11), (5, 13.5, 13), "#console", glow),
            cuboid((11, 12.5, 11), (13, 13.5, 13), "#console", glow),
        ],
        "display": {
            "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1.5, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_lefthand": {"rotation": [0, 45, 0], "translation": [0, 1.5, 0], "scale": [0.5, 0.5, 0.5]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.58, 0.58, 0.58]},
            "firstperson_lefthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.58, 0.58, 0.58]},
            "ground": {"translation": [0, 1.5, 0], "scale": [0.5, 0.5, 0.5]},
            "gui": {"rotation": [30, 225, 0], "scale": [0.78, 0.78, 0.78]},
            "fixed": {"scale": [0.65, 0.65, 0.65]},
        },
    }

def simple_blockstate(block: str) -> dict[str, object]:
    return {"variants": {"": {"model": f"{MOD_ID}:block/{block}"}}}


def directional_blockstate(block: str) -> dict[str, object]:
    model = f"{MOD_ID}:block/{block}"
    return {
        "variants": {
            "facing=north": {"model": model},
            "facing=east": {"model": model, "y": 90},
            "facing=south": {"model": model, "y": 180},
            "facing=west": {"model": model, "y": 270},
        }
    }


def block_item_model(block: str) -> dict[str, str]:
    return {"parent": f"{MOD_ID}:block/{block}"}


def shaped_recipe(pattern: Sequence[str], key: dict[str, str], result: str, count: int = 1, category: str = "misc") -> dict[str, object]:
    return {
        "type": "minecraft:crafting_shaped",
        "category": category,
        "pattern": list(pattern),
        "key": {symbol: {"item": item} for symbol, item in key.items()},
        "result": {"id": result, "count": count},
    }


def shapeless_recipe(ingredients: Iterable[str], result: str, count: int = 1, category: str = "misc") -> dict[str, object]:
    return {
        "type": "minecraft:crafting_shapeless",
        "category": category,
        "ingredients": [{"item": item} for item in ingredients],
        "result": {"id": result, "count": count},
    }


def self_drop_loot_table(block: str) -> dict[str, object]:
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "bonus_rolls": 0.0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"{MOD_ID}:{block}"}],
                "rolls": 1.0,
            }
        ],
    }


def synthesize_whistle() -> Path:
    """Create a deterministic mono referee blast, then encode and remove PCM."""
    ffmpeg = shutil.which("ffmpeg")
    if ffmpeg is None:
        raise RuntimeError("ffmpeg is required to encode referee_whistle.ogg")

    sample_rate = 44_100
    duration = 1.18
    frame_count = round(sample_rate * duration)
    wav_path = ASSETS / "sounds" / "referee_whistle.tmp.wav"
    ogg_path = ASSETS / "sounds" / "referee_whistle.ogg"
    wav_path.parent.mkdir(parents=True, exist_ok=True)

    phase = 0.0
    samples = bytearray()
    for frame in range(frame_count):
        t = frame / sample_rate

        # Smooth one-blast envelope with a tiny mid-blast referee trill.
        attack = min(1.0, t / 0.012)
        release = min(1.0, max(0.0, (duration - t) / 0.085))
        gate = attack * release
        gate *= 0.90 + 0.10 * math.sin(2.0 * math.pi * 17.0 * t) ** 2

        frequency = 3050.0 + 82.0 * math.sin(2.0 * math.pi * 22.0 * t)
        phase += 2.0 * math.pi * frequency / sample_rate
        tone = (
            0.72 * math.sin(phase)
            + 0.21 * math.sin(2.0 * phase + 0.31)
            + 0.07 * math.sin(3.0 * phase + 1.07)
        )

        # Deterministic breath texture; no random module or external sample.
        breath = (((frame * 1103515245 + 12345) >> 16) & 0x7FFF) / 16383.5 - 1.0
        value = gate * (0.73 * tone + 0.018 * breath)
        pcm = round(max(-1.0, min(1.0, value)) * 32767)
        samples.extend(struct.pack("<h", pcm))

    try:
        with wave.open(str(wav_path), "wb") as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(sample_rate)
            wav.writeframes(bytes(samples))

        command = [
            ffmpeg,
            "-y",
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            str(wav_path),
            # Apply deterministic flags to the output muxer as well as the
            # encoder. In particular this fixes the Ogg logical-stream serial.
            "-bitexact",
            "-fflags",
            "+bitexact",
            "-map_metadata",
            "-1",
            "-ac",
            "1",
            "-ar",
            str(sample_rate),
            "-c:a",
            "libvorbis",
            "-q:a",
            "5",
            "-flags:a",
            "+bitexact",
            "-f",
            "ogg",
            "-serial_offset",
            "0",
            str(ogg_path),
        ]
        subprocess.run(command, check=True)
    finally:
        wav_path.unlink(missing_ok=True)

    GENERATED.append(ogg_path)
    return ogg_path


def update_sounds_json() -> None:
    path = ASSETS / "sounds.json"
    sounds = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    sounds["referee_whistle"] = {
        "category": "player",
        "subtitle": f"subtitles.{MOD_ID}.referee_whistle",
        "sounds": [
            {
                "name": f"{MOD_ID}:referee_whistle",
                "stream": False,
            }
        ],
    }
    write_json(path, sounds)


def generate_resources() -> None:
    ball_png = make_world_cup_ball_texture()
    write_bytes(ASSETS / "textures" / "item" / "world_cup_2026_ball.png", ball_png)
    write_bytes(ASSETS / "textures" / "entity" / "world_cup_2026_ball.png", ball_png)
    write_json(ASSETS / "models" / "item" / "world_cup_2026_ball.json", world_cup_ball_model())

    write_bytes(ASSETS / "textures" / "item" / "referee_whistle.png", make_whistle_texture())
    write_json(ASSETS / "models" / "item" / "referee_whistle.json", whistle_model())

    for team in ("red", "blue"):
        block = f"{team}_goal_line"
        write_bytes(ASSETS / "textures" / "block" / f"{block}.png", make_goal_line_texture(team))
        write_json(ASSETS / "models" / "block" / f"{block}.json", goal_line_model(team))
        write_json(ASSETS / "models" / "item" / f"{block}.json", block_item_model(block))
        write_json(ASSETS / "blockstates" / f"{block}.json", directional_blockstate(block))

    write_bytes(ASSETS / "textures" / "block" / "world_cup_trophy.png", make_trophy_texture())
    write_json(ASSETS / "models" / "block" / "world_cup_trophy.json", trophy_model())
    write_json(ASSETS / "models" / "item" / "world_cup_trophy.json", block_item_model("world_cup_trophy"))
    write_json(ASSETS / "blockstates" / "world_cup_trophy.json", simple_blockstate("world_cup_trophy"))

    write_bytes(ASSETS / "textures" / "block" / "score_manager_console.png", make_score_manager_texture())
    write_json(ASSETS / "models" / "block" / "score_manager_console.json", score_manager_model())
    write_json(ASSETS / "models" / "item" / "score_manager_console.json", block_item_model("score_manager_console"))
    write_json(ASSETS / "blockstates" / "score_manager_console.json", simple_blockstate("score_manager_console"))

    recipes = {
        "football": shaped_recipe(
            (" L ", "LWL", " L "),
            {"L": "minecraft:leather", "W": "minecraft:white_wool"},
            f"{MOD_ID}:football",
            category="equipment",
        ),
        "world_cup_2026_ball": shapeless_recipe(
            (
                f"{MOD_ID}:football",
                "minecraft:red_dye",
                "minecraft:green_dye",
                "minecraft:blue_dye",
            ),
            f"{MOD_ID}:world_cup_2026_ball",
            category="equipment",
        ),
        "referee_whistle": shaped_recipe(
            (" II", "IG ", " S "),
            {
                "I": "minecraft:iron_nugget",
                "G": "minecraft:gold_nugget",
                "S": "minecraft:string",
            },
            f"{MOD_ID}:referee_whistle",
            category="equipment",
        ),
        "red_goal_line": shaped_recipe(
            ("CCC", "RRR"),
            {"C": "minecraft:red_carpet", "R": "minecraft:redstone"},
            f"{MOD_ID}:red_goal_line",
            count=6,
            category="redstone",
        ),
        "blue_goal_line": shaped_recipe(
            ("CCC", "RRR"),
            {"C": "minecraft:blue_carpet", "R": "minecraft:redstone"},
            f"{MOD_ID}:blue_goal_line",
            count=6,
            category="redstone",
        ),
        "world_cup_trophy": shaped_recipe(
            ("GGG", "GEG", " G "),
            {"G": "minecraft:gold_ingot", "E": "minecraft:emerald"},
            f"{MOD_ID}:world_cup_trophy",
            category="building",
        ),
        "score_manager_console": shaped_recipe(
            ("IRI", "RBR", "IGI"),
            {
                "I": "minecraft:iron_ingot",
                "R": "minecraft:redstone",
                "B": "minecraft:smooth_stone",
                "G": "minecraft:gold_ingot",
            },
            f"{MOD_ID}:score_manager_console",
            category="redstone",
        ),
    }
    for name, recipe in recipes.items():
        write_json(DATA / "recipe" / f"{name}.json", recipe)

    for block in ("red_goal_line", "blue_goal_line", "world_cup_trophy", "score_manager_console"):
        write_json(DATA / "loot_table" / "blocks" / f"{block}.json", self_drop_loot_table(block))

    synthesize_whistle()
    update_sounds_json()


def validate_outputs() -> None:
    json_paths = [path for path in GENERATED if path.suffix == ".json"]
    for path in json_paths:
        with path.open("r", encoding="utf-8") as handle:
            json.load(handle)

    png_paths = [path for path in GENERATED if path.suffix == ".png"]
    for path in png_paths:
        payload = path.read_bytes()
        if payload[:8] != b"\x89PNG\r\n\x1a\n":
            raise ValueError(f"Invalid PNG signature: {path}")
        width, height = struct.unpack(">II", payload[16:24])
        expected = (64, 64) if "world_cup_2026_ball" in path.name else None
        if expected is not None and (width, height) != expected:
            raise ValueError(f"Unexpected ball texture size {(width, height)}: {path}")

    ball_model = json.loads((ASSETS / "models" / "item" / "world_cup_2026_ball.json").read_text(encoding="utf-8"))
    if len(ball_model.get("elements", [])) != 7:
        raise ValueError("The World Cup ball item must preserve the entity's seven-cuboid layout")

    whistle_model_data = json.loads((ASSETS / "models" / "item" / "referee_whistle.json").read_text(encoding="utf-8"))
    if not whistle_model_data.get("elements"):
        raise ValueError("The referee whistle must be a cuboid model, not a generated sprite")

    for path in DATA.glob("recipes/*.json"):
        raise ValueError(f"Legacy plural recipe path generated unexpectedly: {path}")
    for path in DATA.glob("loot_tables/**/*.json"):
        raise ValueError(f"Legacy plural loot-table path generated unexpectedly: {path}")

    ogg_path = ASSETS / "sounds" / "referee_whistle.ogg"
    if ogg_path.read_bytes()[:4] != b"OggS":
        raise ValueError(f"Invalid Ogg stream: {ogg_path}")
    if (ASSETS / "sounds" / "referee_whistle.tmp.wav").exists():
        raise ValueError("Temporary whistle WAV was not removed")

    print(f"Validated {len(json_paths)} JSON files, {len(png_paths)} PNG files, and 1 OGG stream.")


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def main() -> int:
    generate_resources()
    validate_outputs()
    print("Generated World Cup 2026 resources:")
    for path in sorted(set(GENERATED), key=relative):
        digest = hashlib.sha256(path.read_bytes()).hexdigest()[:12]
        print(f"  {relative(path)}  sha256:{digest}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, subprocess.CalledProcessError, ValueError, RuntimeError) as error:
        print(f"Asset generation failed: {error}", file=sys.stderr)
        raise SystemExit(1) from error
