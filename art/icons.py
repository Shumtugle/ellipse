"""
Draws the two icons of the home screen, each in two layers as the phone
wants them: a round face of warm grey paper with a fine grain, and on it
the thing itself with its own soft shadow.

  launcher  three points, two red and a larger gold one, going on
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

POINTS = [(-36, -7, 9.8, (196, 52, 42)), (-10, -1, 9.8, (196, 52, 42)), (24, 9, 16.5, (214, 154, 42))]

mask = Image.new("L", (N, N), 0)
d = ImageDraw.Draw(mask)
for x, y, r, _ in POINTS:
    disc(d, x, y, r, 255)
front = Image.new("RGBA", (N, N), (0, 0, 0, 0))
front = Image.alpha_composite(front, shadow_of(mask, 2.2, 3.4, 2.6, 0.34))
for x, y, r, colour in POINTS:
    one = Image.new("L", (N, N), 0)
    disc(ImageDraw.Draw(one), x, y, r, 255)
    front = Image.alpha_composite(front, solid(one, colour + (255,)))
    # the lower edge a little deeper, the upper left caught by light: a bead, not a stamp
    dark = Image.new("L", (N, N), 0)
    disc(ImageDraw.Draw(dark), x + r * 0.16, y + r * 0.2, r * 0.95, 255)
    dark = ImageChops.subtract(one, dark).filter(ImageFilter.GaussianBlur(r * 0.08 * U))
    front = Image.alpha_composite(front, solid(ImageChops.multiply(dark, one).point(lambda v: int(v * 0.5)),
                                               (60, 20, 10, 255)))
    glint = Image.new("L", (N, N), 0)
    disc(ImageDraw.Draw(glint), x - r * 0.38, y - r * 0.4, r * 0.26, 255)
    glint = glint.filter(ImageFilter.GaussianBlur(r * 0.1 * U))
    front = Image.alpha_composite(front, solid(glint.point(lambda v: int(v * 0.55)), (255, 244, 220, 255)))
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
