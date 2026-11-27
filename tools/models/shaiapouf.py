"""
Shaiapouf (anime 2011) — refonte d'après les références : jeune adulte grand et élancé, presque androgyne ; peau
mauve (chardon) ; cheveux blond foncé courts et souples qui couvrent le front et les oreilles, frange balayée ;
deux antennes fines ; chemise blanche à jabot et poignets à volants, pantalon noir, chaussures noires ; quatre
grandes ailes de papillon multicolores (deux supérieures, deux inférieures) qui partent du dos, avec nervures.
Expression « froid » : yeux grands ouverts, fanatiques.
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#c7a6d4')
HAIR = rgb('#d4ad52')
HAIR_SH = rgb('#8e6a2a')
HAIR_RIM = rgb('#f2d891')
LINE = rgb('#3a2a2a')
SUIT = rgb('#1a1a20')
SUIT_L = rgb('#34343e')
SHIRT = rgb('#f2f0f4')
SHIRT_SH = rgb('#c9c4d2')
LACE = rgb('#f3f1ea')
WING = rgb('#3a2a6a')
WING_L = rgb('#4fa0d8')
WING_EDGE = rgb('#f2d14a')
WING_SPOT = rgb('#e8743a')

BODY = Body(head=(7.4, 8.2, 7.4), head_y=23.6, hip_y=13.6, knee_y=6.9, shoulder_y=22.0, arm_x=4.4, leg_x=1.7,
            torso_w=6.2, torso_d=3.4)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR_SH, HAIR, t, lit=lit, rim=HAIR_RIM)


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(19)
    c, R = (0.0, top - 3.4, 0.4), 4.5
    scalp(m, c, R, lambda k: mk(k - 0.1)(0.5))
    rr = R + 0.3
    # mèches mi-longues qui descendent jusqu'aux épaules en ondulant (courbure + déjettement alternés)
    for i, az in enumerate(range(0, 360, 26)):
        if 140 < az < 220:
            continue
        side = 1 if az < 180 else -1
        crown_lock(m, f'f_m{i}', c, rr, az + rnd.uniform(-4, 4), 80 + rnd.uniform(-5, 5), 8.6 + rnd.uniform(-0.8, 1.2) + 1.2 * max(0.0, 1 - min(az, 360 - az) / 90),
                   3.8 + rnd.uniform(-0.3, 0.5), mk(0.14 if 50 < az < 310 else 0.02), flat=0.42,
                   turn=122 + rnd.uniform(-6, 6), tip=0.18, segs=5, lean=rnd.uniform(-6, 6),
                   twist=rnd.uniform(-10, 10), skew=0.14 * side * (1 if i % 2 else -1), side=rnd.uniform(-24, 24))
    for i, az in enumerate(range(13, 360, 26)):
        if 140 < az < 220:
            continue
        crown_lock(m, f'f_s{i}', c, rr - 0.3, az, 70, 6.8 + rnd.uniform(-0.8, 0.8) + 1.0 * max(0.0, 1 - min(az, 360 - az) / 90), 3.0, mk(-0.02), flat=0.45, turn=120,
                   tip=0.25, segs=4, lean=rnd.uniform(-8, 8), twist=rnd.uniform(-16, 16))
    # frange balayée sur le côté (raie à gauche du perso) : mèches qui partent de la raie et traversent le front
    for j, (az, L, W, lean, k) in enumerate(((128, 7.4, 4.2, 30, 0.34), (148, 7.8, 4.4, 34, 0.4),
                                             (168, 7.4, 4.2, 38, 0.36), (186, 6.6, 3.6, 40, 0.3))):
        crown_lock(m, f'f_f{j}', c, rr, az, 74, L, W, mk(k), flat=0.5, turn=84, tip=0.2, segs=4, lean=lean,
                   skew=-0.12)
    crown_lock(m, 'f_f9', c, rr, 222, 74, 7.8, 3.6, mk(0.28), flat=0.5, turn=96, tip=0.15, segs=4, lean=-8)
    # deux antennes fines qui sortent du haut du front et se courbent vers l'arrière
    for sx in (-1, 1):
        lock(m, f'f_ant{sx}', [sx * 1.2, top + 0.4, -2.2], 6.0, 0.5, -18, -sx * 14,
             lambda t: strand_paint(rgb('#2a1a3a'), rgb('#5a4a6a'), t, lit=0.2), flat=1.0, curve=(-60, -sx * 10),
             segs=3, root=1.0, tip=0.8, taper_pow=0.3)


def face_paint(ctx, mode='calme'):
    wide = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 8, HAIR_SH)
    f.poly((10, 18, 27, 19, 27, 20, 10, 20), shade(HAIR_SH, 0.8))
    f.poly((56, 18, 39, 19, 39, 20, 56, 20), shade(HAIR_SH, 0.8))
    for x0 in (12, 40):
        if wide:
            f.rect(x0, 24, x0 + 14, 33, (250, 250, 250))
            f.rect(x0 + 4, 24, x0 + 10, 33, rgb('#3a6aa0'))
            f.rect(x0 + 6, 26, x0 + 8, 31, rgb('#0a0a14'))
            f.rect(x0 - 1, 23, x0 + 15, 24, LINE)
        else:                                                     # yeux mi-clos, longs cils
            f.rect(x0, 28, x0 + 14, 31, (250, 250, 250))
            f.rect(x0 + 4, 28, x0 + 10, 31, rgb('#3a6aa0'))
            f.rect(x0 - 1, 26, x0 + 15, 28, LINE)
            f.rect(x0 + 14, 25, x0 + 16, 27, LINE)
    f.rect(32, 38, 33, 43, shade(SKIN, 0.84))
    f.rect(29, 49, 37, 49, rgb('#9a5a5a'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR_SH)


def build_body(m):
    b = BODY
    shirt = cloth(SHIRT, shade(SHIRT, 1.02), SHIRT_SH, ao_bottom=0.1, folds=3, seed=180)
    pants = cloth(SUIT, SUIT_L, shade(SUIT, 0.7), folds=2, seed=181)
    lace = cloth(LACE, shade(LACE, 1.02), shade(LACE, 0.78), folds=3, seed=182)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    # torse long et mince : chemise blanche ample, jabot à volants en cascade, col montant
    m.cube('torso', [-b.torso_w / 2 + 0.4, y0, -d2], [b.torso_w - 0.8, 4.2, b.torso_d], shirt)
    m.cube('torso', [-b.torso_w / 2, y0 + 4.0, -d2 - 0.1], [b.torso_w, y1 - y0 - 4.0, b.torso_d + 0.2], shirt)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 1.0, 2.4], lace)
    for k, w in enumerate((2.8, 2.4, 2.0, 1.5)):
        m.cube('torso', [-w / 2, y1 - 1.3 - k * 1.0, -d2 - 0.3 - 0.12 * k], [w, 1.1, 0.4], lace,
               pivot=[0, y1 - 1.3 - k * 1.0, -d2], rotation=[12, 0, 0])
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.6, b.shoulder_y - b.elbow_y + 0.6, 2.7), shirt)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.4, (2.5, b.elbow_y - b.wrist_y - 0.4, 2.6), shirt)
        for k in range(2):                                                                      # poignets à volants
            m.box(f'{side}_forearm', (cx, 0), b.wrist_y - 0.3 + k * 0.6, (3.2 - 0.3 * k, 0.6, 3.2 - 0.3 * k), lace)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.4, (2.2, 2.4, 2.2), skin_mat(SKIN))
    m.cube('body', [-b.torso_w / 2 + 0.6, b.hip_y - 1.2, -1.6], [b.torso_w - 1.2, 1.6, 3.2], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.6, b.hip_y - b.knee_y + 0.2, 2.7), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.5, b.knee_y - 1.0, 2.6), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.6, 1.1, 3.8), cloth(rgb('#101014'), seed=183))


def wing_paint(seed):
    def paint(ctx):
        cloth(WING, shade(WING, 1.4), shade(WING, 0.7), folds=0, seed=seed)(ctx)
        if ctx.face not in ('north', 'south'):
            return
        f = FaceGrid(ctx)
        f.poly((10, 10, 40, 18, 30, 40, 8, 30), WING_L)
        f.poly((40, 30, 58, 34, 52, 52, 38, 48), shade(WING_L, 0.8))
        f.rect(0, 0, 66, 3, WING_EDGE)
        for x in range(4, 66, 10):
            f.rect(x, 54, x + 4, 58, WING_EDGE)
    return paint


def build_wings(m):
    """Quatre ailes de papillon distinctes (deux grandes en haut, deux plus petites en bas) : contour arrondi fait de
    bandes de largeur variable (pas un panneau rectangulaire), bord clair, nervures sombres en relief qui partent de
    l'attache ; elles sortent du dos sur une petite tige et s'écartent du costume."""
    import math
    b = BODY
    y = b.shoulder_y - 1.0
    z0 = b.torso_d / 2 + 0.9
    vein = flat(shade(WING, 0.45), 0.9)
    for sx in (-1, 1):
        m.cube('torso', [sx * 0.6 - 0.35, y - 4.0, b.torso_d / 2 - 0.1], [0.7, 4.6, 1.2], flat(shade(WING, 0.6), 0.9))
        for k, (w, h, rz, dy, up) in enumerate(((13.5, 12.0, -24, 0.6, 0.66), (9.0, 8.0, 26, -4.8, 0.32))):
            name = f'wing{sx}{k}'
            m.bone(name, [sx * 0.8, y + dy, z0], 'torso', rotation=[6, -sx * 20, -sx * rz])
            n = 9
            for i in range(n):                      # bandes du bas (attache) vers le haut de l'aile
                t = (i + 0.5) / n
                prof = math.sin(math.pi * (0.12 + 0.82 * t)) ** 0.7       # aile arrondie, plus large au milieu
                ww = w * (0.35 + 0.65 * prof)
                hy = y + dy - h * (1 - up) + i * h / n
                x0 = sx * 0.8 + (0 if sx > 0 else -ww)
                col = tuple(int(WING[c] + (WING_L[c] - WING[c]) * (0.15 + 0.75 * t)) for c in range(3))
                m.cube(name, [x0, hy, z0], [ww, h / n + 0.05, 0.22], cloth(col, shade(col, 1.15), shade(col, 0.8), folds=0, seed=190 + k * 10 + i))
                m.cube(name, [x0 + (ww - 0.35 if sx > 0 else 0), hy, z0 - 0.04], [0.35, h / n + 0.05, 0.3],
                       flat(WING_EDGE, 0.95))                                      # bord jaune
                if i in (n // 2, n // 2 + 1):                                      # ocelle orange
                    m.cube(name, [x0 + ww * (0.55 if sx > 0 else 0.25), hy, z0 - 0.08], [ww * 0.2, h / n + 0.05, 0.12],
                           flat(WING_SPOT, 0.9))
            for j, ang in enumerate((-35, 0, 30)):                                 # nervures
                L = w * (0.85 if j == 1 else 0.7)
                m.cube(name, [sx * 0.8 + (0 if sx > 0 else -L), y + dy - 0.1, z0 - 0.08], [L, 0.2, 0.36], vein,
                       pivot=[sx * 0.8, y + dy, z0], rotation=[0, 0, sx * ang])


def build():
    m = Model('shaiapouf', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=4)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    build_wings(m)
    m.animations = humanoid_animations(arm_swing=16, leg_swing=24, breath=1.2)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
