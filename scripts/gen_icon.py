"""Generates the mod icon: python3 scripts/gen_icon.py src/main/resources/assets/mythic_rpg_compat/icon.png preview.png (needs Pillow)."""
import sys
from PIL import Image
# Left half (16 cols) of a 32x32 chestplate, mirrored. O outline, t trim, b body, '.' empty.
HALF = [
"................",
"................",
"................",
"....OOOOOO......",
"...OttttttO.....",
"..OtttttttO.....",
".OttbbbbbttOOOOO",
".Otbbbbbbbtttttt",
".Otbbbbbbbbbbbbb",
".Otbbbbbbbbbbbbb",
".OOObbbbbbbbbbbb",
"...Obbbbbbbbbbbb",
"....Obbbbbbbbbbb",
"....Obbbbbbbbbbb",
"....Obbbbbbbbbbb",
"....Obbbbbbbbbbb",
"....Obbbbbbbbbbb",
"....Obbbbbbbbbbb",
"....Obbbbbbbbbbb",
"....Obbbbbbbbbbb",
"....Obbbbbbbbbbb",
"....Obbbbbbbbbbb",
"....Otttttttttbb",
"....Ottttttttttt",
"....OOOOOOOOOOOO",
"................",
]
ROWS = [r + r[::-1] for r in HALF]
OUT = (24, 18, 22)
BODY = [(78, 70, 74), (66, 59, 63), (55, 49, 53), (45, 40, 44)]       # netherite-ish, light -> dark
RUBY = [(255, 120, 120), (220, 52, 62), (160, 24, 40), (105, 12, 28)]
AETER = [(150, 255, 230), (60, 210, 190), (30, 145, 140), (18, 90, 95)]
GEM = {"h": (255, 245, 255), "l": (235, 150, 255), "m": (180, 80, 230), "d": (110, 40, 170), "e": (60, 18, 100)}
# Cut gem, centred on the chest
GEM_ROWS = [
"...ee...",
"..ehle..",
".ehllme.",
"elllmmde",
"emmmmdde",
".edmdde.",
"..edde..",
"...ee...",
]
W, H = 32, len(ROWS)
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
top = (32 - H) // 2 + 1
for y, row in enumerate(ROWS):
    for x, c in enumerate(row):
        if c == ".":
            continue
        if c == "O":
            col = OUT
        elif c == "t":
            pal = RUBY if x < 16 else AETER
            # light from the top-left
            shade = 0 if y < 6 else 1 if y < 12 else 2
            if x in (15, 16):
                shade = min(3, shade + 1)
            col = pal[shade]
        else:
            # body: lighter near top-left, darker towards bottom-right, a centre seam
            v = (x * 0.6 + y) / (32 * 0.6 + H)
            shade = min(3, int(v * 4.2))
            if x in (15, 16):
                shade = min(3, shade + 1)
            col = BODY[shade]
            if row[x - 1] == "O" and x < 16:
                col = BODY[0] if y < 18 else BODY[1]
        img.putpixel((x, y + top), col + (255,))
gx, gy = 12, 12 + top
for y, row in enumerate(GEM_ROWS):
    for x, c in enumerate(row):
        if c != ".":
            img.putpixel((gx + x, gy + y), GEM[c] + (255,))
box = img.getbbox()
centred = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
centred.paste(img.crop(box), ((32 - (box[2] - box[0])) // 2, (32 - (box[3] - box[1])) // 2))
img = centred
img.resize((128, 128), Image.NEAREST).save(sys.argv[1])
img.resize((512, 512), Image.NEAREST).save(sys.argv[2])
