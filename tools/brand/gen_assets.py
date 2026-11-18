#!/usr/bin/env python3
"""Generates every Rash Music logo asset from the three-arc mark.

Run from the repo root:  python3 tools/brand/gen_assets.py
Needs Pillow + numpy.  The arcs are the author's original SVG geometry
(512 x 512 view box, circle r=240, stroke 30, round caps).
"""
import math, os, re, glob
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app/src/main/res")

BLUE = (0x48, 0xB6, 0xFF)
VIOLET = (0x89, 0x55, 0xF6)
INK = (0x05, 0x05, 0x05)

ARCS = [((105, 190), (256, 125), (407, 190)),
        ((125, 256), (256, 205), (387, 256)),
        ((150, 320), (256, 280), (362, 320))]
STROKE = 30.0


# ---------------------------------------------------------------- geometry
def bez(p0, p1, p2, t):
    a, b, c = (1 - t) ** 2, 2 * (1 - t) * t, t * t
    return (a * p0[0] + b * p1[0] + c * p2[0], a * p0[1] + b * p1[1] + c * p2[1])


def bez_d(p0, p1, p2, t):
    dx = 2 * (1 - t) * (p1[0] - p0[0]) + 2 * t * (p2[0] - p1[0])
    dy = 2 * (1 - t) * (p1[1] - p0[1]) + 2 * t * (p2[1] - p1[1])
    n = math.hypot(dx, dy) or 1
    return dx / n, dy / n


def outline(arc, half, n=40, cap=10):
    """Polygon (list of points) for a round-capped stroke along a quadratic."""
    p0, p1, p2 = arc
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        x, y = bez(p0, p1, p2, t)
        dx, dy = bez_d(p0, p1, p2, t)
        nx, ny = -dy, dx
        left.append((x + nx * half, y + ny * half))
        right.append((x - nx * half, y - ny * half))
    pts = list(left)
    # end cap: from left-end around to right-end
    dx, dy = bez_d(p0, p1, p2, 1.0)
    ang0 = math.atan2(dx, -dy)  # angle of left normal (-dy, dx)
    ex, ey = bez(p0, p1, p2, 1.0)
    for k in range(1, cap):
        a = math.atan2(dx, -dy) - math.pi * k / cap
        pts.append((ex + math.cos(a) * half, ey + math.sin(a) * half))
    pts += right[::-1]
    dx, dy = bez_d(p0, p1, p2, 0.0)
    sx, sy = bez(p0, p1, p2, 0.0)
    for k in range(1, cap):
        a = math.atan2(-dx, dy) - math.pi * k / cap
        pts.append((sx + math.cos(a) * half, sy + math.sin(a) * half))
    return pts


def path_data(tx):
    """SVG/Android pathData for all three arcs after transform tx(x, y)."""
    out = []
    for arc in ARCS:
        pts = [tx(*p) for p in outline(arc, STROKE / 2)]
        out.append("M" + " L".join(f"{x:.1f},{y:.1f}" for x, y in pts) + "Z")
    return " ".join(out)


def per_arc_paths(tx):
    res = []
    for arc in ARCS:
        pts = [tx(*p) for p in outline(arc, STROKE / 2)]
        res.append("M" + " L".join(f"{x:.1f},{y:.1f}" for x, y in pts) + "Z")
    return res


# ---------------------------------------------------------------- vectors
def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def vec_foreground():
    # 1024 viewport, arcs scaled into the 66dp safe zone and centred.
    k = 1.18
    tx = lambda x, y: (512 + (x - 256) * k, 512 + (y - 239) * k)
    paths = per_arc_paths(tx)
    shine = per_arc_paths(lambda x, y: (512 + (x - 256) * k, 512 + (y - 239) * k - 7))
    body = "".join(
        f'    <path android:fillColor="#F2050505" android:pathData="{p}" />\n' for p in paths)
    gloss = "".join(
        f'    <path android:fillColor="#26FFFFFF" android:pathData="{p}" />\n' for p in shine[:0])
    return ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="1024"\n    android:viewportHeight="1024">\n' + body + gloss + '</vector>\n')


def vec_mono():
    k = 1.18
    tx = lambda x, y: (512 + (x - 256) * k, 512 + (y - 239) * k)
    return ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="1024"\n    android:viewportHeight="1024">\n'
            f'    <path android:fillColor="#FF000000" android:pathData="{path_data(tx)}" />\n</vector>\n')


def vec_background():
    return f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="1024"
    android:viewportHeight="1024">
    <path android:pathData="M0,0h1024v1024h-1024z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="0"
                android:startY="0"
                android:endX="1024"
                android:endY="1024"
                android:type="linear">
                <item android:color="#48B6FF" android:offset="0.0" />
                <item android:color="#8955F6" android:offset="1.0" />
            </gradient>
        </aapt:attr>
    </path>
    <path android:pathData="M0,0h1024v1024h-1024z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:centerX="300"
                android:centerY="180"
                android:gradientRadius="620"
                android:type="radial">
                <item android:color="#40FFFFFF" android:offset="0.0" />
                <item android:color="#00FFFFFF" android:offset="1.0" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
'''


def vec_logo(w, h):
    """White monochrome mark for in-app use (tinted by callers)."""
    # fit arcs bounding box (105..407 x 142..335) into w x h viewport
    bx0, bx1, by0, by1 = 90, 422, 127, 350
    s = min(w / (bx1 - bx0), h / (by1 - by0)) * 0.98
    cx, cy = (bx0 + bx1) / 2, (by0 + by1) / 2
    tx = lambda x, y: (w / 2 + (x - cx) * s, h / 2 + (y - cy) * s)
    return (f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{round(w * 28 / 730)}dp"\n    android:height="{round(h * 28 / 730)}dp"\n'
            f'    android:viewportWidth="{w}"\n    android:viewportHeight="{h}">\n'
            f'  <path\n      android:fillColor="#FFFFFFFF"\n      android:pathData="{path_data(tx)}"/>\n</vector>\n')


def vec_notification():
    k = 0.062
    tx = lambda x, y: (12 + (x - 256) * k * 1.3, 12 + (y - 239) * k * 1.3)
    return ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp"\n    android:height="24dp"\n'
            '    android:viewportWidth="24"\n    android:viewportHeight="24">\n'
            f'    <path android:fillColor="#FFFFFFFF" android:pathData="{path_data(tx)}" />\n</vector>\n')


def vec_splash_icon():
    """Static splash icon: brand circle + arcs (used by the system splash)."""
    k = 1.0
    tx = lambda x, y: (x, y)
    return f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="288dp"
    android:height="288dp"
    android:viewportWidth="768"
    android:viewportHeight="768">
  <group android:translateX="128" android:translateY="128">
    <path android:pathData="M256,16 A240,240 0 1 1 255.99,16 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="0"
                android:startY="0"
                android:endX="512"
                android:endY="512"
                android:type="linear">
                <item android:color="#48B6FF" android:offset="0.0" />
                <item android:color="#8955F6" android:offset="1.0" />
            </gradient>
        </aapt:attr>
    </path>
    <path android:fillColor="#050505" android:pathData="{path_data(tx)}" />
  </group>
</vector>
'''


# ---------------------------------------------------------------- rasters
SS = 3
S = 432


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def grad_img(size, c0, c1, diag=True):
    w, h = size
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    t = ((xs / (w - 1)) + (ys / (h - 1))) / 2 if diag else ys / (h - 1)
    t = t[..., None]
    a = np.array(c0, np.float32)
    b = np.array(c1, np.float32)
    rgb = a + (b - a) * t
    return Image.fromarray(np.dstack([rgb, np.full((h, w, 1), 255, np.float32)]).astype(np.uint8), "RGBA")


def hgrad_img(size, c0, c1):
    w, h = size
    xs = np.linspace(0, 1, w, dtype=np.float32)[None, :, None]
    a, b = np.array(c0, np.float32), np.array(c1, np.float32)
    rgb = np.broadcast_to(a + (b - a) * xs, (h, w, 3))
    return Image.fromarray(np.dstack([rgb, np.full((h, w, 1), 255, np.float32)]).astype(np.uint8), "RGBA")


def squircle_mask(size, half, n):
    w, h = size
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    x = np.abs(xs - w / 2 + 0.5) / half
    y = np.abs(ys - h / 2 + 0.5) / half
    m = (x ** n + y ** n) <= 1.0
    return Image.fromarray((m * 255).astype(np.uint8), "L")


def arcs_mask(size, scale, dy=0.0, cx=None, cy=None, extra=0.0):
    w, h = size
    im = Image.new("L", size, 0)
    d = ImageDraw.Draw(im)
    ox = (w / 2) if cx is None else cx
    oy = (h / 2) if cy is None else cy
    tx = lambda x, y: (ox + (x - 256) * scale, oy + (y - 239) * scale + dy)
    for arc in ARCS:
        pts = [tx(*p) for p in outline(arc, STROKE / 2 + extra, n=80, cap=24)]
        d.polygon(pts, fill=255)
    return im


def over(base, layer):
    return Image.alpha_composite(base, layer)


def solid(size, rgb, mask, alpha=1.0):
    im = Image.new("RGBA", size, rgb + (0,))
    a = mask.point(lambda v: int(v * alpha))
    im.putalpha(a)
    return im


def draw_icon(style, n, with_shape=True):
    big = (S * SS, S * SS)
    k_arc = 0.62 * SS  # arcs scale on the shape
    canvas = Image.new("RGBA", big, (0, 0, 0, 0))
    half = 143 * SS
    shape = squircle_mask(big, half, n)

    palette = {
        "light":      dict(fill=(BLUE, VIOLET), arcs=("solid", INK), gloss=0.27, rim=0.35, shadow=(255, 255, 255)),
        "dark":       dict(fill=((0x1B, 0x1B, 0x2B), (0x08, 0x08, 0x0E)), arcs=("grad", None), gloss=0.16, rim=0.22, shadow=(0, 0, 0)),
        "clear":      dict(fill=((0xF7, 0xF8, 0xFF), (0xD5, 0xD9, 0xEE)), arcs=("grad", None), gloss=0.55, rim=0.6, shadow=(0x6B, 0x5B, 0xC8)),
        "clear_dark": dict(fill=((0x52, 0x54, 0x63), (0x2A, 0x2B, 0x35)), arcs=("solid", (0xF4, 0xF6, 0xFF)), gloss=0.22, rim=0.3, shadow=(0, 0, 0)),
        "neon_silver": dict(fill=((0x0D, 0x0D, 0x14), (0x04, 0x04, 0x07)), arcs=("solid", (0xE8, 0xEA, 0xF6)), glow=(0xC8, 0xCC, 0xE8), gloss=0.1, rim=0.5),
        "neon_cyan":   dict(fill=((0x0A, 0x10, 0x16), (0x03, 0x06, 0x09)), arcs=("solid", (0x4F, 0xE0, 0xFF)), glow=(0x20, 0xC8, 0xFF), gloss=0.1, rim=0.5),
        "neon_violet": dict(fill=((0x10, 0x0B, 0x1A), (0x06, 0x04, 0x0B)), arcs=("solid", (0xB4, 0x84, 0xFF)), glow=(0x93, 0x55, 0xFF), gloss=0.1, rim=0.5),
    }[style]

    # body
    body = grad_img(big, palette["fill"][0], palette["fill"][1])
    body.putalpha(shape)
    canvas = over(canvas, body)

    # soft top gloss (clipped to the shape)
    gloss = Image.new("L", big, 0)
    gd = ImageDraw.Draw(gloss)
    gd.ellipse((big[0] * 0.5 - half * 1.05, big[1] * 0.5 - half * 1.55, big[0] * 0.5 + half * 1.05, big[1] * 0.5 - half * 0.05), fill=255)
    gloss = gloss.filter(ImageFilter.GaussianBlur(14 * SS))
    gloss = Image.fromarray((np.minimum(np.array(gloss), np.array(shape)) * palette["gloss"]).astype(np.uint8), "L")
    canvas = over(canvas, solid(big, (255, 255, 255), gloss))

    # rim light
    inner = squircle_mask(big, half - 2.2 * SS, n)
    rim = Image.fromarray(np.clip(np.array(shape, np.int16) - np.array(inner, np.int16), 0, 255).astype(np.uint8), "L")
    rim_col = palette.get("glow", (255, 255, 255))
    canvas = over(canvas, solid(big, rim_col, rim, palette["rim"]))

    # arcs
    am = arcs_mask(big, k_arc, dy=0)
    if "glow" in palette:
        halo = arcs_mask(big, k_arc, extra=9).filter(ImageFilter.GaussianBlur(11 * SS))
        canvas = over(canvas, solid(big, palette["glow"], halo, 0.75))
    else:
        sh = arcs_mask(big, k_arc, dy=5 * SS).filter(ImageFilter.GaussianBlur(5 * SS))
        canvas = over(canvas, solid(big, palette["shadow"], sh, 0.30))

    kind, col = palette["arcs"]
    if kind == "grad":
        fillimg = hgrad_img(big, BLUE, VIOLET)
    else:
        fillimg = Image.new("RGBA", big, tuple(col) + (255,))
    fillimg.putalpha(am)
    canvas = over(canvas, fillimg)

    # fine specular line on the upper edge of each arc
    spec = arcs_mask(big, k_arc, dy=0)
    spec_in = arcs_mask(big, k_arc, dy=3.2 * SS)
    edge = Image.fromarray(np.clip(np.array(spec, np.int16) - np.array(spec_in, np.int16), 0, 255).astype(np.uint8), "L")
    canvas = over(canvas, solid(big, (255, 255, 255), edge, 0.28 if style != "light" else 0.22))
    return canvas


def mono_icon(rgb):
    big = (S * SS, S * SS)
    m = arcs_mask(big, 0.86 * SS)
    return solid(big, rgb, m, 1.0)


def bg_icon(style):
    big = (S * SS, S * SS)
    if style == "light":
        return grad_img(big, BLUE, VIOLET)
    if style == "dark":
        return grad_img(big, (0x14, 0x14, 0x22), (0x07, 0x07, 0x0C))
    if style == "clear":
        return grad_img(big, (0xEC, 0xEE, 0xFA), (0xC6, 0xCA, 0xE2))
    return grad_img(big, (0x45, 0x47, 0x55), (0x24, 0x25, 0x2E))


DENS = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}


def save_all(img, name):
    for d, px in DENS.items():
        p = os.path.join(RES, f"mipmap-{d}", name)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        img.resize((px, px), Image.LANCZOS).save(p)


def classify(fname):
    base = fname[:-len("_foreground.png")]
    n = 5
    if base.startswith("ic_samsung"):
        n = 4
    if base.startswith("ic_pixel"):
        n = 2
    if "neon_silver" in base:
        st = "neon_silver"
    elif "neon_cyan" in base:
        st = "neon_cyan"
    elif "neon_violet" in base:
        st = "neon_violet"
    elif "glass_clear_dark" in base:
        st = "clear_dark"
    elif "glass_clear" in base:
        st = "clear"
    elif "glass_dark" in base:
        st = "dark"
    else:
        st = "light"
    return st, n


def legacy_icons():
    """ic_launcher.webp / round for app + wear (unused on API 26+, kept in sync)."""
    for mod, sizes in (("app", {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}),
                       ("wearos", {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144})):
        base = Image.new("RGBA", (S * SS, S * SS), (0, 0, 0, 0))
        full = draw_icon("light", 5)
        circ = draw_icon("light", 2)
        for d, px in sizes.items():
            folder = os.path.join(ROOT, mod, "src/main/res", f"mipmap-{d}")
            if not os.path.isdir(folder):
                continue
            crop = (S * SS) // 2 - 150 * SS
            box = (crop, crop, (S * SS) - crop, (S * SS) - crop)
            full.crop(box).resize((px, px), Image.LANCZOS).save(os.path.join(folder, "ic_launcher.webp"), quality=95)
            circ.crop(box).resize((px, px), Image.LANCZOS).save(os.path.join(folder, "ic_launcher_round.webp"), quality=95)


def tv_banner():
    for d, (w, h) in {"mdpi": (320, 180), "hdpi": (480, 270), "xhdpi": (640, 360)}.items():
        W, H = w * 3, h * 3
        im = grad_img((W, H), (0x0B, 0x0B, 0x16), (0x05, 0x05, 0x08))
        icon = draw_icon("light", 2)
        side = int(H * 0.62)
        cropc = (S * SS) // 2 - 150 * SS
        ic = icon.crop((cropc, cropc, S * SS - cropc, S * SS - cropc)).resize((side, side), Image.LANCZOS)
        d_ = ImageDraw.Draw(im)
        from PIL import ImageFont
        font = None
        for fp in glob.glob(os.path.join(RES, "font", "sf_pro_display_bold.otf")):
            font = ImageFont.truetype(fp, int(H * 0.2))
        if font is None:
            font = ImageFont.load_default()
        tw1 = d_.textlength("Rash ", font=font)
        tw2 = d_.textlength("Music", font=font)
        total = side + int(H * 0.06) + tw1 + tw2
        x0 = (W - total) / 2
        im.alpha_composite(ic, (int(x0), (H - side) // 2))
        tx = x0 + side + H * 0.06
        ty = H / 2 - H * 0.13
        d_.text((tx, ty), "Rash ", font=font, fill=(255, 255, 255, 255))
        # brand-gradient "Music"
        mw, mh = int(tw2) + 4, int(H * 0.28)
        tmp = Image.new("L", (mw, mh), 0)
        ImageDraw.Draw(tmp).text((0, 0), "Music", font=font, fill=255)
        gr = hgrad_img((mw, mh), BLUE, VIOLET)
        gr.putalpha(tmp)
        im.alpha_composite(gr, (int(tx + tw1), int(ty)))
        out = im.convert("RGB").resize((w, h), Image.LANCZOS)
        folder = os.path.join(ROOT, "tv/src/main/res", "drawable" if d == "mdpi" and False else f"drawable-{d}")
        os.makedirs(folder, exist_ok=True)
        out.save(os.path.join(folder, "tv_banner.png"))
        if d == "xhdpi":
            out.save(os.path.join(ROOT, "tv/src/main/res/drawable/tv_banner.png"))


def repo_art():
    icon = draw_icon("light", 2)
    cropc = (S * SS) // 2 - 150 * SS
    ic = icon.crop((cropc, cropc, S * SS - cropc, S * SS - cropc)).resize((512, 512), Image.LANCZOS)
    ic.save(os.path.join(ROOT, "Logo.png"))
    W, H = 1600, 640
    im = grad_img((W, H), (0x0B, 0x0B, 0x16), (0x05, 0x05, 0x08))
    ic2 = ic.resize((360, 360), Image.LANCZOS)
    im.alpha_composite(ic2, (140, 140))
    from PIL import ImageFont
    f = ImageFont.truetype(os.path.join(RES, "font", "sf_pro_display_bold.otf"), 170)
    d = ImageDraw.Draw(im)
    d.text((560, 190), "Rash ", font=f, fill=(255, 255, 255, 255))
    w1 = d.textlength("Rash ", font=f)
    tmp = Image.new("L", (700, 230), 0)
    ImageDraw.Draw(tmp).text((0, 0), "Music", font=f, fill=255)
    gr = hgrad_img((700, 230), BLUE, VIOLET)
    gr.putalpha(tmp)
    im.alpha_composite(gr, (int(560 + w1), 190))
    im.convert("RGB").save(os.path.join(ROOT, "Banner.png"))


def main():
    # vectors ---------------------------------------------------------
    for mod_res in ("app/src/main/res", "wearos/src/main/res"):
        d = os.path.join(ROOT, mod_res, "drawable")
        write(os.path.join(d, "ic_launcher_foreground.xml"), vec_foreground())
        write(os.path.join(d, "ic_launcher_monochrome.xml"), vec_mono())
        write(os.path.join(d, "ic_launcher_background.xml"), vec_background())
        write(os.path.join(d, "ic_logo.xml"), vec_logo(730, 484))
    write(os.path.join(ROOT, "app/src/main/res/drawable/ic_notification_logo.xml"), vec_notification())
    write(os.path.join(ROOT, "app/src/main/res/drawable/ic_splash_icon.xml"), vec_splash_icon())
    cmp_logo = os.path.join(ROOT, "sharedUi/src/commonMain/composeResources/drawable/ic_logo.xml")
    if os.path.exists(cmp_logo):
        write(cmp_logo, vec_logo(730, 484))
    # rasters ---------------------------------------------------------
    fg = sorted(glob.glob(os.path.join(RES, "mipmap-xxxhdpi", "*_foreground.png")))
    for f in fg:
        name = os.path.basename(f)
        if name == "ic_monochrome_foreground.png":
            save_all(mono_icon((0x22, 0x22, 0x22)), name)
            continue
        st, n = classify(name)
        save_all(draw_icon(st, n), name)
    save_all(mono_icon((255, 255, 255)), "ic_glass_monochrome.png")
    for st, nm in (("light", "ic_glass_background.png"), ("dark", "ic_glass_dark_background.png"),
                   ("clear", "ic_glass_clear_background.png"), ("clear_dark", "ic_glass_clear_dark_background.png")):
        save_all(bg_icon(st), nm)
    legacy_icons()
    tv_banner()
    repo_art()
    print("done", len(fg), "foregrounds")


if __name__ == "__main__":
    main()
