"""Gon Freecss — modèle pilote (voir docs/Analyse_model.md et docs/modeles-a-refaire.md, fiche Gon)."""
from __future__ import annotations

from PIL import ImageDraw

from geolib import Model, humanoid_animations, layered, material, only, rgb, shade

SKIN = rgb('#f2c6a0')
HAIR = rgb('#1f1d26')
HAIR_TIP = rgb('#2f7442')
JACKET = rgb('#37a052')
JACKET_D = shade(JACKET, 0.72)
COLLAR = rgb('#f3f0e6')
SHORTS = rgb('#2c6d3e')
BOOT = rgb('#3b3b42')
BOOT_L = rgb('#5a5a63')
SOLE = rgb('#1c1c21')
EYE = rgb('#6b3a1e')

skin = material(SKIN, grad=0.10, noise=3, edge=0.9)
hair = material(HAIR, grad=0.25, noise=4, edge=0.7)
jacket = material(JACKET, grad=0.14, noise=4, edge=0.7)
shorts = material(SHORTS, grad=0.12, noise=4, edge=0.7)
boot = material(BOOT, grad=0.2, noise=4, edge=0.65)
collar = material(COLLAR, grad=0.08, noise=3, edge=0.82)


def spike_paint(ctx):
    """Mèche : noire en bas, reflet au milieu, pointe verte en haut."""
    hair(ctx)
    if ctx.face in ('up', 'down'):
        if ctx.face == 'up':
            ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR_TIP)
        return
    px = ctx.img.load()
    for y in range(ctx.h):
        t = 1 - y / max(1, ctx.h - 1)  # 1 en haut
        for x in range(ctx.w):
            c = px[x, y]
            if t > 0.72:
                k = min(1, (t - 0.72) / 0.28) * 0.85
                c = tuple(int(c[i] + (HAIR_TIP[i] - c[i]) * k) for i in range(3)) + (255,)
            if x == ctx.w // 2 and 0.2 < t < 0.7:
                c = shade(c, 1.35)
            px[x, y] = c


def face_paint(ctx):
    """Visage (face avant de la tête, 7×7 px à 6 texels/px = 42×42)."""
    skin(ctx)
    d = ctx.draw
    w, h = ctx.w, ctx.h
    s = w / 42
    S = lambda *v: [round(a * s) for a in v]
    d.rectangle(S(0, 0, 42, 13), fill=HAIR)                     # front sous la frange
    for x0 in (4, 13, 22, 31):                                   # mèches de frange qui descendent
        d.polygon(S(x0, 12, x0 + 8, 12, x0 + 4, 17), fill=HAIR)
    # sourcils épais, légèrement froncés vers le centre
    d.polygon(S(7, 15, 17, 16, 17, 18, 7, 17), fill=shade(HAIR, 1.1))
    d.polygon(S(25, 16, 35, 15, 35, 17, 25, 18), fill=shade(HAIR, 1.1))
    # yeux : grands, ronds, bruns, avec reflets
    for ex in (8, 25):
        d.ellipse(S(ex, 19, ex + 9, 28), fill=(255, 255, 255, 255), outline=(40, 25, 20, 255))
        d.ellipse(S(ex + 2, 20, ex + 8, 28), fill=EYE)
        d.ellipse(S(ex + 3, 22, ex + 7, 27), fill=shade(EYE, 0.45))
        d.rectangle(S(ex + 3, 21, ex + 5, 23), fill=(255, 255, 255, 255))
        d.line(S(ex - 1, 19, ex + 10, 19), fill=(30, 20, 18, 255), width=max(1, round(s)))  # cils
    # nez (petite ombre) et sourire
    d.line(S(21, 29, 22, 31), fill=shade(SKIN, 0.78), width=max(1, round(s)))
    d.arc(S(16, 29, 27, 36), 20, 160, fill=(120, 55, 45, 255), width=max(1, round(s * 1.2)))
    d.ellipse(S(5, 29, 10, 32), fill=shade(SKIN, 0.93))        # joues
    d.ellipse(S(32, 29, 37, 32), fill=shade(SKIN, 0.93))


def head_side_paint(ctx):
    """Côtés et arrière de la tête : cheveux en haut (sous la calotte), peau en bas."""
    skin(ctx)
    cut = int(ctx.h * (0.55 if ctx.face == 'south' else 0.4))
    ctx.draw.rectangle([0, 0, ctx.w, cut], fill=HAIR)
    if ctx.face in ('east', 'west'):
        # oreille dessinée (ombre) au milieu du côté
        cx = ctx.w // 2
        ctx.draw.ellipse([cx - ctx.w // 8, cut + 2, cx + ctx.w // 8, cut + ctx.h // 4], fill=shade(SKIN, 0.85))


def jacket_front(ctx):
    jacket(ctx)
    d, w, h = ctx.draw, ctx.w, ctx.h
    cx = w // 2
    d.line([cx, 0, cx, h], fill=JACKET_D, width=2)                       # fermeture
    for y in range(2, h, 4):
        d.point([cx - 1, y], fill=shade(JACKET, 1.3))
    d.rectangle([3, int(h * 0.6), cx - 5, int(h * 0.62)], fill=JACKET_D)   # poches
    d.rectangle([cx + 5, int(h * 0.6), w - 4, int(h * 0.62)], fill=JACKET_D)
    d.rectangle([0, h - 3, w, h], fill=JACKET_D)                          # ourlet


def boot_paint(ctx):
    boot(ctx)
    d, w, h = ctx.draw, ctx.w, ctx.h
    d.rectangle([0, h - max(2, h // 8), w, h], fill=SOLE)
    if ctx.face == 'north':
        for y in range(3, h - 4, 3):                                       # lacets
            d.line([w * 0.3, y, w * 0.7, y + 1], fill=BOOT_L, width=1)


def build():
    m = Model('gon', density=4, tex_width=512)
    m.bone('body', [0, 11, 0])
    m.bone('torso', [0, 11, 0], 'body')
    m.bone('head', [0, 19.6, 0], 'torso')
    m.bone('hair', [0, 24, 0], 'head')
    for side, sx in (('right', -1), ('left', 1)):
        m.bone(f'{side}_arm', [sx * 4.55, 18.6, 0], 'torso')
        m.bone(f'{side}_forearm', [sx * 4.55, 14.3, 0], f'{side}_arm')
        m.bone(f'{side}_hand', [sx * 4.55, 10.4, 0], f'{side}_forearm')
        m.bone('item_main' if side == 'right' else 'item_off', [sx * 4.55, 9.0, -1.0], f'{side}_hand')
        m.bone(f'{side}_leg', [sx * 1.9, 11, 0], 'body')
        m.bone(f'{side}_shin', [sx * 1.9, 5.6, 0], f'{side}_leg')

    # ---- torse : veste verte à col blanc
    m.box('torso', (0, 0), 11.0, (7.4, 8.0, 3.8),
          layered(jacket, only(('north',), jacket_front)))
    m.box('torso', (0, 0), 18.2, (5.6, 1.3, 3.0), collar)                           # col (tour du cou)
    m.cube('torso', [-3.0, 16.6, -2.35], [2.4, 2.4, 0.5], collar, pivot=[-0.6, 18.8, -2.1], rotation=[0, 0, -18])
    m.cube('torso', [0.6, 16.6, -2.35], [2.4, 2.4, 0.5], collar, pivot=[0.6, 18.8, -2.1], rotation=[0, 0, 18])
    m.box('torso', (0, 0), 18.9, (2.4, 1.0, 2.4), skin)                             # cou

    # ---- tête (densité 6 pour le visage)
    def head_paint(ctx):
        (face_paint if ctx.face == 'north' else head_side_paint if ctx.face != 'up' else hair)(ctx)
    m.box('head', (0, 0), 19.6, (7, 7, 7), head_paint, density=6)
    for sx in (-1, 1):                                                              # oreilles
        m.box('head', (sx * 3.75, -0.3), 21.6, (0.6, 1.6, 1.1), skin, density=6)

    # ---- cheveux : calotte, nuque, pattes, frange en volume, grandes pointes
    m.cube('hair', [-3.8, 24.3, -3.8], [7.6, 2.9, 7.6], hair)
    m.cube('hair', [-3.8, 20.6, 2.6], [7.6, 3.8, 1.3], hair)                        # nuque
    m.cube('hair', [-3.4, 26.6, -3.2], [6.8, 2.2, 6.6], hair)                       # masse sous les pointes
    for sx in (-1, 1):
        m.cube('hair', [sx * 3.75 - 0.4, 21.8, -3.6], [0.8, 2.6, 2.4], hair)        # pattes
    for x, rz in ((-2.4, 12), (0.0, 0), (2.4, -12)):                                # frange
        m.cube('hair', [x - 1.0, 23.0, -4.05], [2.0, 2.0, 0.8], spike_paint, pivot=[x, 25, -3.7], rotation=[-12, 0, rz])
    spikes = [  # (x, z, hauteur, inclinaison avant/arrière, inclinaison sur le côté)
        (0.0, -2.2, 7.5, -16, 0), (-2.2, -2.0, 6.5, -14, 22), (2.2, -2.0, 6.5, -14, -22),
        (-3.2, 0.2, 5.5, 0, 42), (3.2, 0.2, 5.5, 0, -42),
        (-1.4, 0.4, 7.5, 6, 12), (1.4, 0.4, 7.5, 6, -12),
        (0.0, 2.3, 6.5, 24, 0), (-2.4, 2.2, 5.5, 26, 26), (2.4, 2.2, 5.5, 26, -26),
    ]
    for x, z, h, rx, rz in spikes:
        piv = [x, 26.4, z]
        m.cube('hair', [x - 1.6, 25.8, z - 1.6], [3.2, h * 0.55, 3.2], hair, pivot=piv, rotation=[rx, 0, rz])
        m.cube('hair', [x - 0.9, 25.8 + h * 0.55 - 0.3, z - 0.9], [1.8, h * 0.5, 1.8], spike_paint,
               pivot=piv, rotation=[rx, 0, rz])

    # ---- bras : manche courte de veste, avant-bras et main séparés
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 4.55
        m.box(f'{side}_arm', (cx, 0), 14.3, (2.8, 4.4, 2.8), skin)
        m.box(f'{side}_arm', (cx, 0), 16.2, (3.0, 2.8, 3.0), jacket, inflate=0.3)    # manche
        m.box(f'{side}_forearm', (cx, 0), 10.4, (2.7, 3.95, 2.7), skin)
        m.box(f'{side}_hand', (cx, 0), 8.1, (2.9, 2.3, 2.9), material(shade(SKIN, 0.97), grad=0.1, noise=3, edge=0.82))

    # ---- jambes : short, mollet, botte avec revers et pointe
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * 1.9
        m.box(f'{side}_leg', (cx, 0), 5.6, (3.4, 5.4, 3.4), skin)
        m.box(f'{side}_leg', (cx, 0), 8.0, (3.6, 3.0, 3.6), shorts, inflate=0.3)
        m.box(f'{side}_shin', (cx, 0), 0.0, (3.3, 5.6, 3.3), skin)
        m.box(f'{side}_shin', (cx, 0), 0.0, (3.4, 4.0, 3.4), boot_paint, inflate=0.2)
        m.box(f'{side}_shin', (cx, 0), 3.3, (3.5, 0.9, 3.5), material(BOOT_L, grad=0.1, noise=3, edge=0.7), inflate=0.3)
        m.box(f'{side}_shin', (cx, -2.2), 0.0, (3.2, 1.6, 1.4), boot_paint, inflate=0.15)  # pointe du pied

    m.animations = humanoid_animations()
    return m


if __name__ == '__main__':
    build().save()
