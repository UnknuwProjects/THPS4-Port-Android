#!/usr/bin/env python3
"""THPS4 touch-overlay artwork (BOOTSTRAP section 49).

Draws the whole touch skin as transparent PNGs in the house style of the
game itself: smoked near-black discs, a pale double ring with reticle ticks
(the sprite-sheet construction the other recomp ports ship, e.g. Black),
hazard yellow for the pressed state, and skate iconography on the face
buttons - A is the ollie (board + up arrow), B the grab (arrow down onto
the board), X the flip (board under a rotation arrow), Y the grind (board
on a rail). The face letters ride in a small badge straddling the top rim,
coloured as on the Xbox pad (A green, B red, X blue, Y yellow).

Everything is drawn at 2x and downscaled for clean edges. Output goes to
the app module's assets (app/src/main/assets/touch/) so the PNGs ship
inside the APK; the port loads them at startup (native/vulkan.cpp) and
touch.cpp draws them with AddImage, falling back to plain discs if a file
is missing.

Run: python3 make_touch_assets.py [outdir]
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

SS = 2  # supersampling
C = 512 * SS  # canvas
CENTER = C // 2
DISC_R = 218 * SS  # the disc inside the canvas; touch.cpp scales by this fraction
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "touch")

FONT_BOLD = "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"
FONT_ITALIC = "/usr/share/fonts/truetype/liberation/LiberationSans-BoldItalic.ttf"

PALE = (206, 211, 201, 255)
PALE_DIM = (206, 211, 201, 170)
YELLOW = (255, 200, 0, 255)
FILL = (13, 14, 18, 238)
FILL_DOWN = (46, 48, 56, 246)


def font(path, size):
    return ImageFont.truetype(path, size * SS)


def text_center(d, xy, s, fnt, fill):
    d.text(xy, s, font=fnt, fill=fill, anchor="mm")


def ring(d, r, width, color):
    d.ellipse([CENTER - r, CENTER - r, CENTER + r, CENTER + r], outline=color, width=width * SS)


def disc(pressed):
    """The shared button body: fill, double ring, reticle ticks."""
    img = Image.new("RGBA", (C, C), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    main = YELLOW if pressed else PALE
    d.ellipse([CENTER - DISC_R, CENTER - DISC_R, CENTER + DISC_R, CENTER + DISC_R],
              fill=FILL_DOWN if pressed else FILL)
    ring(d, DISC_R - 4 * SS, 5, main)
    ring(d, DISC_R - 26 * SS, 2, YELLOW if pressed else PALE_DIM)
    for a in range(0, 360, 15):
        r0, r1 = DISC_R - 24 * SS, DISC_R - 12 * SS
        x0 = CENTER + r0 * math.cos(math.radians(a))
        y0 = CENTER + r0 * math.sin(math.radians(a))
        x1 = CENTER + r1 * math.cos(math.radians(a))
        y1 = CENTER + r1 * math.sin(math.radians(a))
        d.line([x0, y0, x1, y1], fill=YELLOW if pressed else PALE_DIM, width=3 * SS)
    return img


def badge(img, letter, color):
    """The letter badge straddling the disc's top rim (Xbox colours)."""
    d = ImageDraw.Draw(img)
    cy, r = CENTER - DISC_R + 4 * SS, 40 * SS
    d.ellipse([CENTER - r, cy - r, CENTER + r, cy + r], fill=(10, 10, 13, 255), outline=color,
              width=7 * SS)
    text_center(d, (CENTER, cy + 2 * SS), letter, font(FONT_BOLD, 44), color)


def board(d, cx, cy, w, color):
    """A skateboard in profile: deck with a kicked nose, two wheels."""
    h = 7 * SS
    d.rounded_rectangle([cx - w / 2, cy - h, cx + w / 2, cy + h], radius=h, fill=color)
    d.line([cx + w / 2 - 30 * SS, cy - h, cx + w / 2 + 10 * SS, cy - h - 16 * SS], fill=color,
           width=9 * SS)
    for dx in (-w / 4, w / 4):
        r = 10 * SS
        d.ellipse([cx + dx - r, cy + h + 2 * SS - r, cx + dx + r, cy + h + 2 * SS + r], fill=color)


def icon_color(pressed):
    return (244, 247, 242, 255) if pressed else PALE


def face(letter, color, kind, pressed):
    img = disc(pressed)
    d = ImageDraw.Draw(img)
    col = icon_color(pressed)
    icy = CENTER + 16 * SS  # icons sit a touch low; the badge owns the top
    if kind == "a":  # ollie: board below, arrow bursting up
        d.polygon([(CENTER - 42 * SS, icy - 62 * SS), (CENTER + 42 * SS, icy - 62 * SS),
                   (CENTER, icy - 112 * SS)], fill=col)
        d.rectangle([CENTER - 17 * SS, icy - 62 * SS, CENTER + 17 * SS, icy - 8 * SS], fill=col)
        board(d, CENTER, icy + 52 * SS, 176 * SS, col)
    elif kind == "b":  # grab: arrow pressing down onto the board
        d.rectangle([CENTER - 17 * SS, icy - 108 * SS, CENTER + 17 * SS, icy - 54 * SS], fill=col)
        d.polygon([(CENTER - 42 * SS, icy - 58 * SS), (CENTER + 42 * SS, icy - 58 * SS),
                   (CENTER, icy - 8 * SS)], fill=col)
        board(d, CENTER, icy + 52 * SS, 176 * SS, col)
    elif kind == "x":  # flip: board tilted under a rotation arrow
        layer = Image.new("RGBA", (360 * SS, 200 * SS), (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        board(ld, 180 * SS, 92 * SS, 190 * SS, col)
        layer = layer.rotate(26, resample=Image.BICUBIC, center=(180 * SS, 100 * SS))
        img.alpha_composite(layer, (CENTER - 180 * SS, icy - 108 * SS))
        d = ImageDraw.Draw(img)
        d.arc([CENTER - 100 * SS, icy - 108 * SS, CENTER + 100 * SS, icy + 92 * SS], start=115,
              end=335, fill=col, width=11 * SS)
        ax = CENTER + 100 * SS * math.cos(math.radians(115))
        ay = icy - 8 * SS + 100 * SS * math.sin(math.radians(115))
        d.polygon([(ax, ay), (ax - 34 * SS, ay - 6 * SS), (ax - 8 * SS, ay + 30 * SS)], fill=col)
    elif kind == "y":  # grind: board riding a rail
        board(d, CENTER, icy - 6 * SS, 168 * SS, col)
        d.rectangle([CENTER - 108 * SS, icy + 34 * SS, CENTER + 108 * SS, icy + 46 * SS], fill=col)
        for dx in (-76 * SS, 76 * SS):
            d.rectangle([CENTER + dx - 6 * SS, icy + 46 * SS, CENTER + dx + 6 * SS, icy + 74 * SS],
                        fill=col)
    badge(img, letter, color)
    return img


def text_disc(text, size, pressed):
    img = disc(pressed)
    d = ImageDraw.Draw(img)
    text_center(d, (CENTER, CENTER + 2 * SS), text, font(FONT_ITALIC, size),
                YELLOW if pressed else PALE)
    return img


def dpad(direction, pressed):
    img = disc(pressed)
    d = ImageDraw.Draw(img)
    col = YELLOW if pressed else icon_color(pressed)
    s, t = 56 * SS, 46 * SS  # half length, half width of the triangle
    shapes = {
        "up": [(CENTER, CENTER - s), (CENTER - t, CENTER + s), (CENTER + t, CENTER + s)],
        "down": [(CENTER, CENTER + s), (CENTER - t, CENTER - s), (CENTER + t, CENTER - s)],
        "left": [(CENTER - s, CENTER), (CENTER + s, CENTER - t), (CENTER + s, CENTER + t)],
        "right": [(CENTER + s, CENTER), (CENTER - s, CENTER - t), (CENTER - s, CENTER + t)],
    }
    d.polygon(shapes[direction], fill=col)
    return img


def stick_base():
    img = Image.new("RGBA", (C, C), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ring(d, DISC_R + 14 * SS, 6, (206, 211, 201, 170))
    ring(d, 150 * SS, 3, (206, 211, 201, 100))
    for a in range(0, 360, 30):
        r0, r1 = DISC_R - 8 * SS, DISC_R + 8 * SS
        d.line([CENTER + r0 * math.cos(math.radians(a)), CENTER + r0 * math.sin(math.radians(a)),
                CENTER + r1 * math.cos(math.radians(a)), CENTER + r1 * math.sin(math.radians(a))],
               fill=(206, 211, 201, 140), width=5 * SS)
    return img


def knob(pressed):
    img = Image.new("RGBA", (C, C), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([CENTER - DISC_R, CENTER - DISC_R, CENTER + DISC_R, CENTER + DISC_R],
              fill=FILL_DOWN if pressed else (16, 17, 21, 242))
    ring(d, DISC_R - 4 * SS, 6, YELLOW if pressed else PALE)
    ring(d, 76 * SS, 15, YELLOW)  # the wheel's hub
    r = 24 * SS
    d.ellipse([CENTER - r, CENTER - r, CENTER + r, CENTER + r], fill=PALE)
    return img


def save(img, name):
    img = img.resize((256, 256), Image.LANCZOS)
    path = os.path.join(OUT, name)
    img.save(path)
    print("wrote", path, img.size)


def main():
    os.makedirs(OUT, exist_ok=True)
    faces = [("a", "A", (80, 200, 120, 255)), ("b", "B", (235, 95, 85, 255)),
             ("x", "X", (95, 145, 245, 255)), ("y", "Y", (245, 205, 75, 255))]
    for name, letter, color in faces:
        save(face(letter, color, name, False), f"{name}.png")
        save(face(letter, color, name, True), f"{name}_p.png")
    for name, text, size in [("lt", "LT", 96), ("rt", "RT", 96), ("white", "WHITE", 62),
                             ("black", "BLACK", 62), ("back", "BACK", 74), ("start", "START", 74)]:
        save(text_disc(text, size, False), f"{name}.png")
        save(text_disc(text, size, True), f"{name}_p.png")
    for direction in ("up", "down", "left", "right"):
        save(dpad(direction, False), f"dpad_{direction}.png")
        save(dpad(direction, True), f"dpad_{direction}_p.png")
    save(stick_base(), "stick_base.png")
    save(knob(False), "knob.png")
    save(knob(True), "knob_p.png")


if __name__ == "__main__":
    main()
