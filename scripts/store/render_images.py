#!/usr/bin/env python3
"""STORE-01: compose authentic Android captures into Play-ready Korean listing images."""
import argparse
import hashlib
import json
import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

PACKAGE = "com.haebi.focuslock"
WIDTH, HEIGHT = 1350, 2400
BG, INK, MUTED, ACCENT, LINE = "#F7F7F7", "#151515", "#656565", "#373737", "#DEDEDE"
CAPTURES = [
    ("01-home.png", "14-home-granted.png", "스마트폰 사용 습관", "오늘의 사용 시간 한눈에"),
    ("02-analysis.png", "15-analysis-today.png", "앱 사용 패턴 분석", "오늘 · 7일 · 30일 사용 기록"),
    ("03-app-detail.png", "18-app-detail.png", "앱별 사용 기록", "사용 시간과 관측된 이벤트 확인"),
    ("04-goals.png", "19-goal-sheet.png", "선택형 앱 사용 목표", "시간과 요일별 알림 직접 설정"),
    ("05-focus.png", "09-focus-running.png", "필요할 때 집중", "선택형 타이머로 시간 기록"),
    ("06-result.png", "11-focus-result.png", "집중 성과 기록", "완료 시간과 실제 보상 확인"),
    ("07-garden.png", "12-garden.png", "기록으로 키우는 정원", "경험치로 아이템 해금"),
    ("08-report.png", "20-report.png", "사용 변화 리포트", "7일·30일 사용 시간 비교"),
]

def font(weight, size):
    choices = [
        f"/usr/share/fonts/opentype/noto/NotoSansCJK-{weight}.ttc",
        f"/usr/share/fonts/truetype/nanum/NanumGothic{'Bold' if weight == 'Bold' else ''}.ttf",
    ]
    for candidate in choices:
        if Path(candidate).is_file():
            return ImageFont.truetype(candidate, size)
    raise RuntimeError("Korean font unavailable. Install fonts-noto-cjk")

def rgb_image(source):
    im = Image.open(source)
    if im.size != (1080, 2400):
        raise ValueError(f"Expected verified 1080x2400 Android screenshot: {source}")
    if im.format != "PNG":
        raise ValueError(f"Not a PNG screenshot: {source}")
    return im.convert("RGB")

def render_screen(source, index, title, subtitle):
    native = rgb_image(source)
    # Crop Android system bars only; never synthesize or edit in-app UI.
    native = native.crop((0, 130, 1080, 2270))
    frame = Image.new("RGB", (WIDTH, HEIGHT), BG)
    draw = ImageDraw.Draw(frame)
    draw.rectangle((0, 0, WIDTH, 22), fill=INK)
    draw.text((110, 104), "FOCUSLOCK  /  DIGITAL WELLBEING", font=font("Regular", 32), fill=ACCENT)
    draw.text((1240, 104), f"{index:02d} / 08", font=font("Regular", 32), fill=MUTED, anchor="ra")
    draw.text((110, 186), title, font=font("Bold", 93), fill=INK)
    draw.text((112, 326), subtitle, font=font("Regular", 46), fill=MUTED)
    screenshot_width = 938
    screenshot_height = round(native.height * screenshot_width / native.width)
    native = native.resize((screenshot_width, screenshot_height), Image.Resampling.LANCZOS)
    x, y = (WIDTH - screenshot_width) // 2, 468
    draw.rectangle((x - 2, y - 2, x + screenshot_width + 1, y + screenshot_height + 1), outline=LINE, width=2)
    frame.paste(native, (x, y))
    return frame

def render_feature(captures):
    w, h = 1024, 500
    im = Image.new("RGB", (w, h), "#F7F7F7")
    draw = ImageDraw.Draw(im)
    draw.rectangle((0, 0, 13, h), fill=ACCENT)
    draw.text((58, 63), "FOCUSLOCK", font=font("Bold", 28), fill=ACCENT)
    draw.text((56, 119), "앱 사용을 살펴보고", font=font("Bold", 55), fill=INK)
    draw.text((56, 192), "변화를 알아보세요", font=font("Bold", 55), fill=INK)
    draw.text((59, 311), "사용 분석  ·  선택 목표  ·  집중 기록", font=font("Regular", 26), fill=MUTED)
    for filename, x, y, cw, ch in [
        ("14-home-granted.png", 684, 44, 240, 485),
        ("09-focus-running.png", 851, 97, 166, 360),
    ]:
        native = rgb_image(captures / filename)
        native = native.crop((0, 90, 1080, 2280))
        ratio = min(cw / native.width, ch / native.height)
        thumb = native.resize((int(native.width * ratio), int(native.height * ratio)), Image.Resampling.LANCZOS)
        im.paste(thumb, (x, y))
        draw.rectangle((x - 1, y - 1, x + thumb.width, y + thumb.height), outline="#BFBFBF", width=1)
    return im

def sha256(data):
    return hashlib.sha256(data).hexdigest()

def generate(captures, output, commit, run_id):
    output.mkdir(parents=True, exist_ok=True)
    entries = []
    for index, (filename, source, title, subtitle) in enumerate(CAPTURES, start=1):
        source_file = captures / source
        raw_hash = sha256(source_file.read_bytes())
        out = output / filename
        render_screen(source_file, index, title, subtitle).save(out, format="PNG", optimize=True)
        entries.append({"file": filename, "source": source, "source_sha256": raw_hash,
                        "dimensions": [WIDTH, HEIGHT], "sha256": sha256(out.read_bytes()), "title": title})
    feature = output / "feature-graphic.png"
    render_feature(captures).save(feature, format="PNG", optimize=True)
    manifest = {
        "app": "FocusLock", "package": PACKAGE, "locale": "ko-KR",
        "source_commit": commit, "source_workflow_run": str(run_id),
        "provenance": "Actual Android API 36 instrumentation captures; promotional text outside the UI; no simulated data or in-app edits",
        "screenshots": entries,
        "feature_graphic": {"file": feature.name, "dimensions": [1024, 500], "sha256": sha256(feature.read_bytes())},
    }
    (output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    validate(output)
    return manifest

def validate(output):
    manifest = json.loads((output / "manifest.json").read_text(encoding="utf-8"))
    if manifest.get("package") != PACKAGE or manifest.get("locale") != "ko-KR":
        raise ValueError("Wrong store listing target")
    images = manifest["screenshots"]
    if [a["file"] for a in images] != [a[0] for a in CAPTURES]:
        raise ValueError("Unexpected screenshot order")
    allowed = {a[0] for a in CAPTURES} | {"feature-graphic.png", "manifest.json"}
    if {p.name for p in output.iterdir()} != allowed:
        raise ValueError("Unexpected files in image folder")
    for item in images + [manifest["feature_graphic"]]:
        path = output / item["file"]
        data = path.read_bytes()
        with Image.open(path) as im:
            if im.format != "PNG" or im.mode != "RGB" or list(im.size) != item["dimensions"]:
                raise ValueError(f"Bad PNG dimensions/mode: {path}")
        if sha256(data) != item["sha256"] or len(data) > 15 * 1024 * 1024:
            raise ValueError(f"PNG hash or size mismatch: {path}")
    return manifest

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--captures", type=Path)
    parser.add_argument("--output", type=Path, default=Path("assets/play-store/ko-KR"))
    parser.add_argument("--source-sha", default=os.getenv("SOURCE_SHA"))
    parser.add_argument("--source-run", default=os.getenv("SOURCE_RUN"))
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.check:
        validate(args.output)
    else:
        if not args.captures:
            parser.error("--captures required for rendering")
        if not args.source_sha or not args.source_run:
            parser.error("Explicit verified source SHA and workflow run ID are required")
        generate(args.captures, args.output, args.source_sha, args.source_run)
    print(f"STORE-01 verified: {args.output}")

if __name__ == "__main__":
    main()
