"""
Draws the two icons of the home screen, each in two layers as the phone
wants them: a round face of warm grey paper with a fine grain, and on it
the thing itself with its own soft shadow.

  launcher  three points of ink, two red on a ruled line and a larger gold
            one slid below it and running off at the edge
  settings  the head of a spanner, set off the middle, its handle running
            out past the edge, the jaws worn bright where bolts have turned

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

# ------------------------------------------------------------- launcher
#
# Not beads: ink. Each point is set down by a pen on paper, so its edge is
# not a circle but wanders a little, the ink is thicker at the rim where it
# dried, and the grain of the paper shows through it. Two stand on a faint
# ruled line in red ink; the third, larger, in gold ink, has slid below the
# line and runs off at the edge of the icon.

rng = np.random.default_rng(5)
LINE = 7.5
POINTS = [(-34, LINE - 9.6, 9.6, (150, 38, 30)), (-8, LINE - 9.0, 9.0, (150, 38, 30)),
          (40, 17, 16.5, (178, 128, 40))]


def blot(x, y, r, seed):
    """An outline that wanders around a circle: a few slow waves and a little jitter."""
    g = np.random.default_rng(seed)
    waves = [(k, g.uniform(0.02, 0.06) / k ** 0.6, g.uniform(0, 2 * math.pi)) for k in (2, 3, 5, 7)]
    pts = []
    for i in range(180):
        a = 2 * math.pi * i / 180
        k = 1 + sum(amp * math.sin(n * a + ph) for n, amp, ph in waves) + g.normal(0, 0.006)
        pts.append(px(x + math.cos(a) * r * k, y + math.sin(a) * r * k))
    return pts


grain = Image.fromarray(np.clip(rng.normal(200, 40, (N // 4, N // 4)), 0, 255).astype(np.uint8), "L")
grain = grain.resize((N, N), Image.BILINEAR).filter(ImageFilter.GaussianBlur(1.5))

front = Image.new("RGBA", (N, N), (0, 0, 0, 0))
# the ruled line, in soft pencil, fainter at its ends
rule = Image.new("L", (N, N), 0)
ImageDraw.Draw(rule).line([px(-70, LINE + 0.4), px(70, LINE - 0.2)], fill=255, width=int(0.9 * U))
fade = Image.new("L", (N, N), 0)
ImageDraw.Draw(fade).ellipse((C - 58 * U, C - 58 * U, C + 58 * U, C + 58 * U), fill=255)
rule = ImageChops.multiply(rule, fade.filter(ImageFilter.GaussianBlur(14 * U)))
front = Image.alpha_composite(front, solid(rule.point(lambda v: int(v * 0.32)), (96, 92, 88, 255)))
for i, (x, y, r, colour) in enumerate(POINTS):
    one = Image.new("L", (N, N), 0)
    ImageDraw.Draw(one).polygon(blot(x, y, r, 40 + i), fill=255)
    one = one.filter(ImageFilter.GaussianBlur(0.35 * U))
    # the paper shows through: the ink is a touch uneven, never a flat fill
    body = ImageChops.multiply(one, grain.point(lambda v: 205 + v * 50 // 255))
    front = Image.alpha_composite(front, solid(body, colour + (255,)))
    # where ink dries, it gathers at the rim
    rim = ImageChops.subtract(one, one.filter(ImageFilter.MinFilter(int(1.6 * U) | 1)))
    rim = rim.filter(ImageFilter.GaussianBlur(0.5 * U))
    darker = tuple(int(c * 0.72) for c in colour)
    front = Image.alpha_composite(front, solid(rim.point(lambda v: int(v * 0.7)), darker + (255,)))
    if i == 2:
        # gold ink carries fine flakes of metal
        flakes = Image.new("L", (N, N), 0)
        dfl = ImageDraw.Draw(flakes)
        for _ in range(36):
            a, t = rng.uniform(0, 2 * math.pi), math.sqrt(rng.uniform(0, 1)) * r * 0.92
            fx, fy = px(x + math.cos(a) * t, y + math.sin(a) * t)
            q = rng.uniform(0.2, 0.42) * U
            dfl.ellipse((fx - q, fy - q, fx + q, fy + q), fill=int(rng.uniform(60, 140)))
        front = Image.alpha_composite(front, solid(ImageChops.multiply(flakes, one), (240, 208, 130, 255)))
back = paper(11, GREY)
back.save(RES + "ic_back.jpg", quality=88, optimize=True)
save(front, "ic_fg.png")
preview(back, front, "launcher.png")

# ------------------------------------------------------------- settings

ANGLE = math.radians(-32)


def turn(points, cx, cy):
    out = []
    for x, y in points:
        xr = x * math.cos(ANGLE) - y * math.sin(ANGLE)
        yr = x * math.sin(ANGLE) + y * math.cos(ANGLE)
        out.append(px(cx + xr, cy + yr))
    return out


HX, HY = 16, -12          # the head sits up and to the right
tool = Image.new("L", (N, N), 0)
d = ImageDraw.Draw(tool)
disc(d, HX, HY, 27, 255)
d.polygon(turn([(-10, -8.5), (-120, -8.5), (-120, 8.5), (-10, 8.5)], HX, HY), fill=255)
# the jaws: an opening fifteen degrees off the axis, with a round throat
slot_poly = [(4, -11.5), (44, -11.5 + 40 * math.tan(math.radians(15))), (44, 11.5 + 40 * math.tan(math.radians(15))), (4, 11.5)]
slot = Image.new("L", (N, N), 0)
ds = ImageDraw.Draw(slot)
ds.polygon(turn(slot_poly, HX, HY), fill=255)
cx, cy = turn([(4, 0)], HX, HY)[0]
ds.ellipse((cx - 11.5 * U, cy - 11.5 * U, cx + 11.5 * U, cy + 11.5 * U), fill=255)
wide = slot.filter(ImageFilter.MaxFilter(int(1.6 * U) | 1))
body = ImageChops.subtract(tool, slot)
worn = ImageChops.multiply(ImageChops.subtract(wide, slot), body)

front = Image.new("RGBA", (N, N), (0, 0, 0, 0))
front = Image.alpha_composite(front, shadow_of(body, 2.6, 4.0, 3.0, 0.36))
front = Image.alpha_composite(front, solid(ImageChops.offset(body, int(0.9 * U), int(1.0 * U)), (58, 64, 72, 255)))
front = Image.alpha_composite(front, solid(ImageChops.offset(body, int(-0.7 * U), int(-0.8 * U)), (176, 182, 190, 255)))
front = Image.alpha_composite(front, solid(body, (120, 127, 136, 255)))
front = Image.alpha_composite(front, solid(worn, (214, 154, 42, 255)))
# a long soft light along the handle, as on turned steel
sheen = Image.new("L", (N, N), 0)
ImageDraw.Draw(sheen).polygon(turn([(-10, -5), (-120, -5), (-120, -1.5), (-10, -1.5)], HX, HY), fill=255)
sheen = ImageChops.multiply(sheen.filter(ImageFilter.GaussianBlur(1.2 * U)), body)
front = Image.alpha_composite(front, solid(sheen.point(lambda v: int(v * 0.45)), (235, 238, 242, 255)))
back = paper(23, GREY)
back.save(RES + "door_back.jpg", quality=88, optimize=True)
save(front, "door_fg.png")
preview(back, front, "settings.png")
