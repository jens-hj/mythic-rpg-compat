"""Draws the mod icon (original art, Ametrine colours): python3 scripts/gen_icon.py <icon.png> <preview.png>. Needs Pillow."""
import sys
from PIL import Image, ImageDraw, ImageFilter
# Gem palette, dark -> light (Ametrine)
PAL = [(21,11,47),(45,17,76),(69,21,113),(100,31,154),(127,33,177),(164,49,194),(208,64,193),(239,109,171),(249,160,139),(253,203,179)]
GOLD = {"a": (140, 82, 20), "b": (214, 150, 30), "c": (252, 214, 70), "d": (255, 244, 170)}
# 16x16 silhouette: '#' body, 'g' gold trim, '.' empty. Left 8 columns, mirrored.
HALF = [
"........",
".ggg....",
"gggg#...",
"ggg##g..",
"#g###g..",
"######gg",
"########",
".#######",
"..######",
"..######",
"..######",
"..######",
"..######",
"..gggggg",
"..gggggg",
"........",
]
rows = [h + h[::-1] for h in HALF]
H, W = len(rows), 16
def filled(x, y):
    return 0 <= x < W and 0 <= y < H and rows[y][x] != "."
spr = Image.new("RGBA", (W, H), (0, 0, 0, 0))
for y in range(H):
    for x in range(W):
        c = rows[y][x]
        if c == ".":
            continue
        edge_tl = not filled(x - 1, y) or not filled(x, y - 1)
        edge_br = not filled(x + 1, y) or not filled(x, y + 1)
        if c == "g":
            col = GOLD["a"] if edge_br else GOLD["d"] if edge_tl and x < 8 else GOLD["c"] if x < 8 else GOLD["b"]
        else:
            # light from the top-left
            s = 8.3 - 0.33 * x - 0.30 * y
            if x in (7, 8):          # centre seam
                s -= 1.2
            if edge_br:
                s = 1 if y > 4 or x > 8 else 2
            elif edge_tl:
                s = min(s + 1.5, 7)
            col = PAL[max(1, min(9, round(s)))]
        spr.putpixel((x, y), col + (255,))
# Gem set in the chest, in a gold setting
for (x, y) in [(6, 7), (9, 7), (5, 8), (10, 8), (5, 9), (10, 9), (6, 10), (9, 10), (7, 11), (8, 11)]:
    spr.putpixel((x, y), (GOLD["c"] if x < 8 and y < 10 else GOLD["a"]) + (255,))
for (x, y), c in {(7, 7): 9, (8, 7): 8, (6, 8): 9, (7, 8): 8, (8, 8): 7, (9, 8): 6,
                  (6, 9): 7, (7, 9): 6, (8, 9): 5, (9, 9): 4, (7, 10): 4, (8, 10): 3}.items():
    spr.putpixel((x, y), PAL[c] + (255,))

N, scale = 128, 7
icon = Image.new("RGBA", (N, N), (0, 0, 0, 0))
glow = Image.new("RGBA", (N, N), (0, 0, 0, 0))
ImageDraw.Draw(glow).ellipse((22, 22, N - 22, N - 22), fill=(190, 90, 255, 110))
icon.alpha_composite(glow.filter(ImageFilter.GaussianBlur(16)))
big = spr.resize((16 * scale, 16 * scale), Image.NEAREST)
x0 = (N - big.size[0]) // 2; y0 = (N - big.size[1]) // 2
shadow = Image.new("RGBA", big.size, (8, 4, 16, 0)); shadow.putalpha(big.split()[3].point(lambda v: 140 if v else 0))
icon.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(3)), (x0 + 2, y0 + 4))
icon.alpha_composite(big, (x0, y0))
icon.save(sys.argv[1])
prev = Image.new("RGBA", (N * 2 + 30, N + 20), (30, 30, 30, 255))
prev.alpha_composite(icon, (10, 10))
light = Image.new("RGBA", (N, N), (200, 200, 200, 255)); light.alpha_composite(icon)
prev.alpha_composite(light, (N + 20, 10))
prev.resize((prev.size[0] * 3, prev.size[1] * 3), Image.NEAREST).save(sys.argv[2])

# Optional: the Modrinth project icon, the same sprite on a dark rounded tile
if len(sys.argv) > 3:
    T = 512
    tile = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    bg = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    ImageDraw.Draw(bg).rounded_rectangle((0, 0, T - 1, T - 1), radius=100, fill=(26, 22, 32, 255))
    glow = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((96, 96, T - 96, T - 96), fill=(190, 90, 255, 150))
    tile.alpha_composite(bg)
    tile.alpha_composite(Image.composite(glow.filter(ImageFilter.GaussianBlur(80)), Image.new("RGBA", (T, T)), bg.split()[3]))
    big = spr.resize((16 * 22, 16 * 22), Image.NEAREST)
    x0 = (T - big.size[0]) // 2; y0 = (T - big.size[1]) // 2 + 6
    shadow = Image.new("RGBA", big.size, (0, 0, 0, 0)); shadow.putalpha(big.split()[3].point(lambda v: 150 if v else 0))
    tile.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(10)), (x0 + 6, y0 + 14))
    tile.alpha_composite(big, (x0, y0))
    tile.save(sys.argv[3])
