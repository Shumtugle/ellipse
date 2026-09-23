"""
Draws the face of the door to the settings: a clock's winding key of dark
steel lying on warm paper, one wing worn down to the brass where the thumb
turns it. Drawn large and brought down for smooth edges; the paper gets a
fine grain and a few long fibres. Run from this folder; writes the two
layers of the icon into the resources, and a round preview here.
"""
import math, numpy as np
from PIL import Image, ImageDraw, ImageFilter

S = 432          # 108dp at four pixels a dp
SS = 4           # drawn four times larger, then brought down: smooth edges
N = S * SS
C = N / 2
U = 0.68 * 4 * SS  # one unit of the sketch in pixels (the sketch's circle of 62 becomes 37dp)
ROT = math.radians(-18)

def tp(x, y, dx=0, dy=0):
    x, y = x + dx, y + dy
    xr = x * math.cos(ROT) - y * math.sin(ROT)
    yr = x * math.sin(ROT) + y * math.cos(ROT)
    return (C + xr * U, C + yr * U)

def bez(p0, p1, p2, p3, n=24):
    out = []
    for i in range(n + 1):
        t = i / n
        a = (1 - t) ** 3; b = 3 * (1 - t) ** 2 * t; c = 3 * (1 - t) * t ** 2; d = t ** 3
        out.append((a*p0[0]+b*p1[0]+c*p2[0]+d*p3[0], a*p0[1]+b*p1[1]+c*p2[1]+d*p3[1]))
    return out

def rrect(x0, y0, x1, y1, r, n=6):
    pts = []
    for cx, cy, a0 in ((x1-r, y0+r, -90), (x1-r, y1-r, 0), (x0+r, y1-r, 90), (x0+r, y0+r, 180)):
        for i in range(n + 1):
            a = math.radians(a0 + 90 * i / n)
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts

def circ(cx, cy, r, n=48):
    return [(cx + r*math.cos(2*math.pi*i/n), cy + r*math.sin(2*math.pi*i/n)) for i in range(n)]

left_wing = bez((-7,-10), (-18,-34), (-44,-36), (-42,-16)) + bez((-42,-16), (-40,2), (-18,2), (-7,-4))
right_wing = [(-x, y) for x, y in left_wing]
worn = bez((12,-20), (20,-30), (36,-31), (38,-20)) + bez((38,-20), (39,-12), (30,-8), (22,-10)) + [(16,-14)]
waist = rrect(-9, -15, 9, -1, 3)
shaft = rrect(-6, -4, 6, 34, 2.5)
socket = rrect(-7, 30, 7, 40, 2)
hole = rrect(-3, 33, 3, 38, 0.8)
rivet = circ(0, -8, 2.6)
def about(pts, cx, cy, k, dx, dy):
    return [(cx + (x - cx) * k + dx, cy + (y - cy) * k + dy) for x, y in pts]
inner = about(right_wing, 24, -18, 0.8, -1.6, 1.4)
glint = bez((20,-25), (26,-28), (32,-28), (34,-24))

def poly(draw, pts, fill, dx=0, dy=0):
    draw.polygon([tp(x, y, dx, dy) for x, y in pts], fill=fill)

# the shadow the key throws on the paper, soft and to the lower right
shadow = Image.new("L", (N, N), 0)
d = ImageDraw.Draw(shadow)
for shape in (left_wing, right_wing, waist, shaft, socket):
    poly(d, shape, 255, 3.0, 5.0)
shadow = shadow.filter(ImageFilter.GaussianBlur(5 * U / 2.4))
layer = Image.new("RGBA", (N, N), (0, 0, 0, 0))
black = Image.new("RGBA", (N, N), (40, 26, 8, 255))
layer.paste(black, (0, 0), shadow.point(lambda v: int(v * 0.30)))

key = Image.new("RGBA", (N, N), (0, 0, 0, 0))
d = ImageDraw.Draw(key)
steel, lit, deep = (42, 52, 66, 255), (70, 84, 104, 255), (26, 33, 43, 255)
# a lit edge on the upper left and a deep one on the lower right give the metal its thickness
for shape in (shaft, socket, left_wing, right_wing, waist):
    poly(d, shape, deep, 0.8, 0.9)
    poly(d, shape, lit, -0.6, -0.7)
for shape in (shaft, socket, left_wing, right_wing, waist):
    poly(d, shape, steel)
# the rings of the barrel
for y in (6, 12):
    d.line([tp(-6, y), tp(6, y)], fill=deep, width=int(0.9 * U))
poly(d, hole, (12, 16, 22, 255))
# where the thumb turns it, the dark is worn off the rim of one wing down to the brass
poly(d, right_wing, (209, 142, 31, 255))
poly(d, inner, steel)
d.line([tp(x, y) for x, y in glint], fill=(247, 217, 140, 255), width=int(1.6 * U), joint="curve")
poly(d, rivet, lit)

out = Image.alpha_composite(layer, key).resize((S, S), Image.LANCZOS)
out.save("../res/drawable-nodpi/door_fg.png")

# the paper: warm, with a fine grain and a few long fibres, a little darker at the edges
rng = np.random.default_rng(7)
base = np.array([239, 227, 203], dtype=np.float32)
img = np.ones((S, S, 3), dtype=np.float32) * base
img += rng.normal(0, 3.2, (S, S, 1))
yy, xx = np.mgrid[0:S, 0:S]
r = np.hypot(xx - S * 0.47, yy - S * 0.45) / (S * 0.7)
img -= (r ** 2)[..., None] * np.array([14, 16, 20])
paper = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB")
d = ImageDraw.Draw(paper)
for _ in range(26):
    x = rng.uniform(0, S); y = rng.uniform(0, S); a = rng.uniform(-0.25, 0.25); L = rng.uniform(20, 70)
    d.line([(x, y), (x + L * math.cos(a), y + L * math.sin(a))], fill=(226, 212, 184), width=1)
paper.save("../res/drawable-nodpi/door_back.jpg", quality=88, optimize=True)

# a preview, cut round as the phone cuts it
prev = Image.alpha_composite(paper.convert("RGBA"), out)
mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(mask).ellipse((S*0.5 - 37*4, S*0.5 - 37*4, S*0.5 + 37*4, S*0.5 + 37*4), fill=255)
canvas = Image.new("RGBA", (S, S), (20, 20, 20, 255))
canvas.paste(prev, (0, 0), mask)
canvas.crop((S*0.5-160, S*0.5-160, S*0.5+160, S*0.5+160)).save("preview.png")
