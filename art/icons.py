"""
Draws the two icons of the home screen, each in two layers as the phone
wants them: a round face of warm grey paper with a fine grain, and on it
the thing itself, in ink.

  launcher  an ellipse in one stroke of ink, two red points sitting on it
            and a larger gold one slipped off it, running out past the edge
  settings  the head of a spanner in the same ink, off the middle, its
            handle running out past the edge, its jaws touched with gold,
            and the ellipse drawn round it

Drawn large and brought down for smooth edges. Run from this folder;
writes the layers into the resources and round previews here.
"""
import math
import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

S = 432            # 108dp at four pixels a dp
SS = 4             # drawn four times larger, then brought down
N = S * SS
C = N / 2
U = 0.6 * 4 * SS   # one unit of the sketch: the sketch's circle of 62 becomes 37dp
RES = "../res/drawable-nodpi/"


def px(x, y):
    return (C + x * U, C + y * U)


def disc(draw, x, y, r, fill):
    a, b = px(x - r, y - r)
    c, d = px(x + r, y + r)
    draw.ellipse((a, b, c, d), fill=fill)


def paper(seed, base):
    """Warm grey with a fine grain and a few long fibres, a touch darker at the edges."""
    rng = np.random.default_rng(seed)
    img = np.ones((S, S, 3), dtype=np.float32) * np.array(base, dtype=np.float32)
    img += rng.normal(0, 3.0, (S, S, 1))
    yy, xx = np.mgrid[0:S, 0:S]
    r = np.hypot(xx - S * 0.46, yy - S * 0.44) / (S * 0.7)
    img -= (r ** 2)[..., None] * np.array([16, 16, 17])
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB")
    d = ImageDraw.Draw(out)
    fibre = tuple(int(v - 9) for v in base)
    for _ in range(22):
        x, y = rng.uniform(0, S), rng.uniform(0, S)
        a, length = rng.uniform(-0.3, 0.3), rng.uniform(20, 60)
        d.line([(x, y), (x + length * math.cos(a), y + length * math.sin(a))], fill=fibre, width=1)
    return out


def shadow_of(mask, dx, dy, blur, strength):
    moved = ImageChops.offset(mask, int(dx * U), int(dy * U))
    soft = moved.filter(ImageFilter.GaussianBlur(blur * U))
    layer = Image.new("RGBA", (N, N), (30, 28, 26, 0))
    layer.putalpha(soft.point(lambda v: int(v * strength)))
    return layer


def solid(mask, colour):
    layer = Image.new("RGBA", (N, N), colour)
    layer.putalpha(mask)
    return layer


def save(layer, name):
    layer.resize((S, S), Image.LANCZOS).save(RES + name)


def preview(back, front, name):
    whole = Image.alpha_composite(back.convert("RGBA"), front.resize((S, S), Image.LANCZOS))
    cut = Image.new("L", (S, S), 0)
    ImageDraw.Draw(cut).ellipse((S / 2 - 148, S / 2 - 148, S / 2 + 148, S / 2 + 148), fill=255)
    canvas = Image.new("RGBA", (S, S), (20, 20, 20, 255))
    canvas.paste(whole, (0, 0), cut)
    canvas.crop((S / 2 - 160, S / 2 - 160, S / 2 + 160, S / 2 + 160)).save(name)


GREY = (201, 198, 193)

# ------------------------------------------------------------- the pen
#
# Everything is ink on paper: points set down by a pen, the edge of each
# wandering a little, thicker at the rim where it dried, the grain of the
# paper showing through; and lines drawn in one stroke, fine at the ends
# where the pen came down and lifted, never quite closed.

rng = np.random.default_rng(5)
grain = Image.fromarray(np.clip(rng.normal(200, 40, (N // 4, N // 4)), 0, 255).astype(np.uint8), "L")
grain = grain.resize((N, N), Image.BILINEAR).filter(ImageFilter.GaussianBlur(1.5))
INK = (43, 49, 64)


def turned(x, y, angle):
    a = math.radians(angle)
    return (x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a))


def wander(pts, seed, amount):
    """An outline that wanders around its true shape: a few slow waves and a little jitter."""
    g = np.random.default_rng(seed)
    waves = [(k, g.uniform(0.3, 1.0) / k ** 0.6, g.uniform(0, 2 * math.pi)) for k in (2, 3, 5)]
    out = []
    n = len(pts)
    cx = sum(p[0] for p in pts) / n
    cy = sum(p[1] for p in pts) / n
    for i, (x, y) in enumerate(pts):
        a = 2 * math.pi * i / n
        k = amount * (sum(amp * math.sin(m * a + ph) for m, amp, ph in waves) + g.normal(0, 0.12))
        dx, dy = x - cx, y - cy
        d = math.hypot(dx, dy) or 1
        out.append((x + dx / d * k, y + dy / d * k))
    return out


def inked(front, shape, colour, rim=0.72):
    """A shape filled with ink: uneven with the paper's grain, darker where it dried at the rim."""
    body = ImageChops.multiply(shape, grain.point(lambda v: 205 + v * 50 // 255))
    front = Image.alpha_composite(front, solid(body, colour + (255,)))
    edge = ImageChops.subtract(shape, shape.filter(ImageFilter.MinFilter(int(1.6 * U) | 1)))
    edge = edge.filter(ImageFilter.GaussianBlur(0.5 * U))
    darker = tuple(int(c * rim) for c in colour)
    return Image.alpha_composite(front, solid(edge.point(lambda v: int(v * 0.7)), darker + (255,)))


def point(x, y, r, seed):
    pts = [(x + r * math.cos(2 * math.pi * i / 180), y + r * math.sin(2 * math.pi * i / 180)) for i in range(180)]
    shape = Image.new("L", (N, N), 0)
    ImageDraw.Draw(shape).polygon([px(a, b) for a, b in wander(pts, seed, r * 0.035)], fill=255)
    return shape.filter(ImageFilter.GaussianBlur(0.35 * U))


def stroke(cx, cy, rx, ry, angle, start, sweep, width, seed):
    """One stroke of the pen along an ellipse: it swells in the middle and thins where the pen lands and lifts."""
    g = np.random.default_rng(seed)
    ph1, ph2 = g.uniform(0, 2 * math.pi, 2)
    shape = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(shape)
    steps = int(abs(sweep) * 3)
    for i in range(steps + 1):
        s = i / steps
        t = math.radians(start + sweep * s)
        wob = 1 + 0.012 * math.sin(3 * t + ph1) + 0.008 * math.sin(7 * t + ph2)
        x, y = turned(rx * wob * math.cos(t), ry * wob * math.sin(t), angle)
        w = width * (0.25 + 0.75 * math.sin(math.pi * s) ** 0.5)
        a, b = px(cx + x, cy + y)
        rr = w * U / 2
        d.ellipse((a - rr, b - rr, a + rr, b + rr), fill=255)
    return shape.filter(ImageFilter.GaussianBlur(0.3 * U))


def orbit_point(cx, cy, rx, ry, angle, t):
    x, y = turned(rx * math.cos(math.radians(t)), ry * math.sin(math.radians(t)), angle)
    return cx + x, cy + y


# ------------------------------------------------------------- launcher
#
# The ellipse is the line of the thought: drawn in one stroke of dark ink,
# tilted, not quite closed. Two red points sit on it; the third, larger,
# in gold ink with fine flakes of metal, has slipped off it and runs out
# past the edge of the icon.

OX, OY, RX, RY, TILT = -2, 0, 42, 17, -14
front = Image.new("RGBA", (N, N), (0, 0, 0, 0))
front = inked(front, stroke(OX, OY, RX, RY, TILT, 10, 334, 2.2, 3), INK, rim=0.9)
reds = [orbit_point(OX, OY, RX, RY, TILT, t) for t in (222, 262)]
for i, (x, y) in enumerate(reds):
    front = inked(front, point(x, y, 7.6, 40 + i), (150, 38, 30))
gx, gy, gr = 40, 22, 15.5
gold = point(gx, gy, gr, 42)
front = inked(front, gold, (178, 128, 40))
flakes = Image.new("L", (N, N), 0)
dfl = ImageDraw.Draw(flakes)
for _ in range(36):
    a, t = rng.uniform(0, 2 * math.pi), math.sqrt(rng.uniform(0, 1)) * gr * 0.92
    fx, fy = px(gx + math.cos(a) * t, gy + math.sin(a) * t)
    q = rng.uniform(0.2, 0.42) * U
    dfl.ellipse((fx - q, fy - q, fx + q, fy + q), fill=int(rng.uniform(60, 140)))
front = Image.alpha_composite(front, solid(ImageChops.multiply(flakes, gold), (240, 208, 130, 255)))
back = paper(11, GREY)
back.save(RES + "ic_back.jpg", quality=88, optimize=True)
save(front, "ic_fg.png")
preview(back, front, "launcher.png")
LAUNCHER_POINTS = reds + [(gx, gy)]

# ------------------------------------------------------------- settings
#
# The head of a spanner in the same dark ink, set off the middle, its
# handle running out past the edge, the jaws touched with gold where bolts
# have turned. The ellipse goes round the head: behind it, the stroke is
# hidden; where it crosses in front, the pen has left a fine line of bare
# paper through the ink.

ANGLE = -32
HX, HY = 14, -10


def spanner(x, y):
    return turned(x, y, ANGLE)


outline = []
for i in range(0, 361, 2):
    a = math.radians(i)
    outline.append((HX + spanner(27 * math.cos(a), 27 * math.sin(a))[0],
                    HY + spanner(27 * math.cos(a), 27 * math.sin(a))[1]))
tool = Image.new("L", (N, N), 0)
d = ImageDraw.Draw(tool)
d.polygon([px(a, b) for a, b in wander(outline, 9, 0.9)], fill=255)
handle = [spanner(x, y) for x, y in [(-10, -8.5), (-120, -9.5), (-120, 9.5), (-10, 8.5)]]
d.polygon([px(HX + a, HY + b) for a, b in handle], fill=255)
lean = math.tan(math.radians(15))
jaw = [spanner(x, y) for x, y in [(4, -11.5), (46, -11.5 + 42 * lean), (46, 11.5 + 42 * lean), (4, 11.5)]]
slot = Image.new("L", (N, N), 0)
ds = ImageDraw.Draw(slot)
ds.polygon([px(HX + a, HY + b) for a, b in jaw], fill=255)
tx, ty = spanner(4, 0)
a, b = px(HX + tx, HY + ty)
ds.ellipse((a - 11.5 * U, b - 11.5 * U, a + 11.5 * U, b + 11.5 * U), fill=255)
tool = ImageChops.subtract(tool, slot).filter(ImageFilter.GaussianBlur(0.35 * U))
worn = ImageChops.multiply(ImageChops.subtract(slot.filter(ImageFilter.MaxFilter(int(2.2 * U) | 1)), slot), tool)

SX, SY, SRX, SRY, STILT = HX - 2, HY + 2, 40, 14, 18
front = Image.new("RGBA", (N, N), (0, 0, 0, 0))
front = inked(front, stroke(SX, SY, SRX, SRY, STILT, 200, 334, 2.0, 8), INK, rim=0.9)
front = inked(front, tool, INK)
front = inked(front, worn, (178, 128, 40))
near = stroke(SX, SY, SRX, SRY, STILT, 12, 150, 1.4, 8)
front = Image.alpha_composite(front, solid(ImageChops.multiply(near, tool), GREY + (255,)))
back = paper(23, GREY)
back.save(RES + "door_back.jpg", quality=88, optimize=True)
save(front, "door_fg.png")
preview(back, front, "settings.png")
print("points", [(round(x, 1), round(y, 1)) for x, y in LAUNCHER_POINTS])
