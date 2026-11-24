#!/usr/bin/env python3
"""Skin checker for VivecraftBedrockBridge pack registration.

Usage: python3 check_skin.py <skin.png>

Reports (for recording in roster.json):
  - format: modern 64x64 / legacy 64x32 (legacy needs conversion)
  - overlay_used: whether any overlay region (hat/jacket/sleeves/pants)
    contains non-transparent pixels. The current geometry has no overlay
    boxes, so a skin with overlay_used=true is only partially supported.
"""
import json
import struct
import sys
import zlib

OVERLAY_REGIONS = {
    "head": (32, 0, 64, 16),
    "body": (16, 32, 40, 48),
    "right_arm": (40, 32, 56, 48),
    "right_leg": (4, 32, 20, 48),
    "left_leg": (0, 48, 16, 64),
    "left_arm": (48, 48, 64, 64),
}


def read_png_rgba(path):
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a PNG"
    pos = 8
    w = h = None
    idat = b""
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos + 4])[0]
        typ = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + ln]
        if typ == b"IHDR":
            w, h = struct.unpack(">II", chunk[:8])
        elif typ == b"IDAT":
            idat += chunk
        pos += 12 + ln
    raw = zlib.decompress(idat)
    stride = w * 4
    px = bytearray(w * h * 4)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]
        p += 1
        line = bytearray(raw[p:p + stride])
        p += stride
        if f == 1:
            for i in range(4, stride):
                line[i] = (line[i] + line[i - 4]) & 0xFF
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif f == 3:
            for i in range(stride):
                a = line[i - 4] if i >= 4 else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif f == 4:
            for i in range(stride):
                a = line[i - 4] if i >= 4 else 0
                b = prev[i]
                c = prev[i - 4] if i >= 4 else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        px[y * stride:(y + 1) * stride] = line
        prev = line
    return w, h, px


def main():
    w, h, px = read_png_rgba(sys.argv[1])
    result = {"size": f"{w}x{h}", "format": "modern" if (w, h) == (64, 64) else ("legacy" if (w, h) == (64, 32) else "unknown")}
    overlay = {}
    if result["format"] == "modern":
        for name, (x0, y0, x1, y1) in OVERLAY_REGIONS.items():
            n = 0
            for y in range(y0, y1):
                for x in range(x0, x1):
                    if px[(y * w + x) * 4 + 3] > 0:
                        n += 1
            overlay[name] = n
    result["overlay_opaque_px"] = overlay
    result["overlay_used"] = any(v > 0 for v in overlay.values())
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
