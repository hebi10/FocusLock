#!/usr/bin/env python3
"""STORE-02-A: premium brochure concepts from unaltered FocusLock Android captures.

No synthetic usage figures or edits to in-app UI. Review artifacts only; not Play uploads.
"""
import hashlib
import json
import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path.cwd()
CAP = Path(os.environ.get("FOCUSLOCK_CAPTURE_DIR", "captures/qa-captures"))
OUT = Path(os.environ.get("FOCUSLOCK_OUTPUT_DIR", "generated/store-02-A"))
OUT.mkdir(parents=True, exist_ok=True)

W, H = 1350, 2400
BG = "#F6F7F8"
INK = "#111316"
MUTED = "#5E646C"
BLUE = "#09548D"
LINE = "#D8DEE5"
NAVY = "#12171C"


def font(size, bold=False):
    name = "NotoSansCJK-Bold.ttc" if bold else "NotoSansCJK-Regular.ttc"
    return ImageFont.truetype("/usr/share/fonts/opentype/noto/" + name, size)


def text(draw, location, value, size, color, bold=False, anchor=None):
    draw.text(location, value, font=font(size, bold), fill=color, anchor=anchor)


def rule(draw, points, color, width=2):
    draw.line(points, fill=color, width=width)


def place_screen(dest, screenshot, rectangle, radius=46, border=8, stroke=INK):
    x, y, width, height = rectangle
    src = Image.open(CAP / screenshot).convert("RGB")
    if src.size != (1080, 2400):
        raise ValueError("Only actual 1080x2400 Android QA captures are permitted")
    src = src.resize((width, height), Image.Resampling.LANCZOS)
    mask = Image.new("L", (width, height), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, width - 1, height - 1), radius=radius, fill=255
    )
    dest.paste(src, (x, y), mask)
    ImageDraw.Draw(dest).rounded_rectangle(
        (x - border, y - border, x + width + border, y + height + border),
        radius=radius + border, outline=stroke, width=border
    )


icon = Image.open(ROOT / "assets/store/FocusLock_Play_Icon_512.png").convert("RGB")


def header(img, draw, number, light):
    img.paste(icon.resize((78, 78), Image.Resampling.LANCZOS), (76, 80))
    text(draw, (175, 99), "FocusLock", 44, INK if light else "#FFFFFF", True)
    text(draw, (1278, 111), f"{number} / 08", 32,
         MUTED if light else "#A7BBCB", anchor="ra")
    rule(draw, (76, 188, 1276, 188), LINE if light else "#39444F")


def render_home():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    draw.rectangle((1143, 730, 1350, 2400), fill=BLUE)
    draw.rectangle((77, 265, 87, 478), fill=BLUE)
    header(img, draw, "01", True)
    text(draw, (119, 270), "나의 스마트폰 사용,", 91, INK, True)
    text(draw, (119, 382), "이제는 알고 쓰세요.", 91, BLUE, True)
    text(draw, (121, 540), "오늘의 사용 시간부터 자주 쓰는 앱까지", 36, MUTED)
    text(draw, (121, 590), "한 화면에서 확인하세요.", 36, MUTED)
    sw = 805
    sh = round(2400 * sw / 1080)
    place_screen(img, "14-home-granted.png", (486, 742, sw, sh),
                 radius=52, border=8, stroke=INK)
    draw = ImageDraw.Draw(img)
    features = [
        ("01", "오늘의 사용 시간", "하루 이용 현황"),
        ("02", "자주 쓰는 앱", "실제 앱 사용 기록"),
        ("03", "집중으로 연결", "타이머 바로 시작"),
    ]
    for i, (index, title, description) in enumerate(features):
        top = 955 + i * 316
        rule(draw, (82, top, 402, top), LINE, 3)
        text(draw, (86, top + 22), index, 29, BLUE, True)
        text(draw, (86, top + 83), title, 36, INK, True)
        text(draw, (86, top + 145), description, 27, MUTED)
    draw.rectangle((0, 2260, 469, 2400), fill="#EAF0F5")
    text(draw, (82, 2310), "내 습관을 기록하는 시작", 26, INK, True)
    text(draw, (82, 2350), "ACTUAL APP SCREEN", 20, MUTED)
    return img


def render_analysis():
    img = Image.new("RGB", (W, H), NAVY)
    draw = ImageDraw.Draw(img)
    header(img, draw, "02", False)
    text(draw, (82, 284), "오늘 · 7일 · 30일", 92, "#FFFFFF", True)
    text(draw, (82, 402), "사용 패턴을 기간별로", 82, "#FFFFFF", True)
    text(draw, (82, 508), "확인하세요.", 86, "#80B9EB", True)
    text(draw, (86, 638), "기기에서 확인 가능한 사용 기록을", 34, "#CAD3DB")
    text(draw, (86, 692), "기간에 따라 다르게 분석합니다.", 34, "#CAD3DB")
    draw.rectangle((795, 805, 1350, 2400), fill="#1C334A")
    rw = 685
    rh = round(2400 * rw / 1080)
    place_screen(img, "15-analysis-today.png", (608, 898, rw, rh),
                 radius=44, border=7, stroke="#D3E0EB")
    lw = 505
    lh = round(2400 * lw / 1080)
    place_screen(img, "16-analysis-week.png", (83, 1100, lw, lh),
                 radius=40, border=7, stroke="#D3E0EB")
    draw = ImageDraw.Draw(img)
    draw.rectangle((78, 1005, 288, 1075), fill=BLUE)
    text(draw, (184, 1035), "7일 분석", 31, "#FFFFFF", True, anchor="mm")
    draw.rectangle((999, 814, 1288, 880), fill="#E9F1F9")
    text(draw, (1143, 846), "오늘 분석", 31, BLUE, True, anchor="mm")
    draw.rectangle((0, 2205, 607, 2400), fill="#263B4E")
    text(draw, (85, 2245), "기간별 그래프", 33, "#FFFFFF", True)
    text(draw, (85, 2308), "앱 사용 기록을 다르게 보는 방법", 24, "#CBD9E4")
    text(draw, (85, 2350), "실제 Android 화면 사용", 19, "#B6C6D4")
    return img


def generate():
    items = [
        ("01-home-concept.png", render_home, ["14-home-granted.png"]),
        ("02-analysis-concept.png", render_analysis,
         ["15-analysis-today.png", "16-analysis-week.png"]),
    ]
    outputs = []
    for file, renderer, captures in items:
        image = renderer()
        path = OUT / file
        image.save(path, format="PNG", optimize=True)
        if image.mode != "RGB" or image.size != (W, H):
            raise ValueError("Invalid image dimensions or mode")
        outputs.append({
            "file": file,
            "pixels": [W, H],
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "source_captures": captures
        })
    sheet = Image.new("RGB", (1250, 1230), "#F2F3F5")
    for file, x in [(items[0][0], 38), (items[1][0], 652)]:
        snapshot = Image.open(OUT / file).resize(
            (561, 996), Image.Resampling.LANCZOS
        )
        sheet.paste(snapshot, (x, 68))
    draw = ImageDraw.Draw(sheet)
    text(draw, (38, 1100), "01  사용 습관 관리", 35, INK, True)
    text(draw, (652, 1100), "02  기간별 사용 분석", 35, INK, True)
    text(draw, (40, 1176), "STORE-02-A  /  시안 검수용 · Google Play 미업로드",
         25, MUTED)
    sheet.save(OUT / "FocusLock_STORE-02-A_preview.jpg",
               quality=92, optimize=True)
    manifest = {
        "app": "FocusLock", "store_task": "STORE-02-A",
        "status": "review_only_not_uploaded",
        "capture_source_workflow_run": os.environ.get(
            "FOCUSLOCK_SOURCE_RUN", "42945780305"
        ),
        "capture_source_commit": os.environ.get(
            "FOCUSLOCK_SOURCE_SHA",
            "c485fecff504f40ad7555cce58443a49f0d844e9"
        ),
        "native_app_ui": "unaltered screenshots from Android API36",
        "screenshots": outputs,
    }
    (OUT / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8"
    )
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    generate()
