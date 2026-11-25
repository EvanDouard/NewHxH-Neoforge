"""
Shaiapouf — même style. Grand et mince (≈ 1,95 bloc) ; cheveux blonds ondulés mi-longs, raie sur le côté,
frange qui balaie le front, pointes qui ondulent sur les épaules ; visage fin et pâle, yeux mi-clos ; tenue de
danseur sombre (justaucorps bleu nuit, jabot et poignets blancs en dentelle, ceinture), grandes ailes de papillon
noires aux motifs violets dans le dos.
Expression « froid » : yeux grands ouverts, fanatiques.
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f6e6da')
HAIR = rgb('#f2d77a')
HAIR_SH = rgb('#b48d3a')
HAIR_RIM = rgb('#fff1b8')
LINE = rgb('#3a2a2a')
SUIT = rgb('#1f2440')
SUIT_L = rgb('#323a66')
LACE = rgb('#f3f1ea')
WING = rgb('#1b1626')
WING_L = rgb('#7a4ab0')
WING_EDGE = rgb('#d7c2ff')

BODY = Body(head=(7.8, 8.4, 7.6), head_y=22.8, hip_y=12.6, knee_y=6.4, shoulder_y=21.2, arm_x=4.7, leg_x=1.8,
            torso_w=6.8, torso_d=3.6)
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
        crown_lock(m, f'f_m{i}', c, rr, az + rnd.uniform(-4, 4), 76 + rnd.uniform(-4, 4), 12.0 + rnd.uniform(-1, 1.5) + 3.0 * max(0.0, 1 - min(az, 360 - az) / 90),
                   3.8 + rnd.uniform(-0.3, 0.5), mk(0.14 if 50 < az < 310 else 0.02), flat=0.42,
                   turn=122 + rnd.uniform(-6, 6), tip=0.18, segs=5, lean=rnd.uniform(-6, 6),
                   twist=rnd.uniform(-10, 10), skew=0.14 * side * (1 if i % 2 else -1))
    for i, az in enumerate(range(13, 360, 26)):
        if 140 < az < 220:
            continue
        crown_lock(m, f'f_s{i}', c, rr - 0.3, az, 70, 9.4 + rnd.uniform(-1, 1) + 3.0 * max(0.0, 1 - min(az, 360 - az) / 90), 3.0, mk(-0.02), flat=0.45, turn=126,
                   tip=0.25, segs=4, lean=rnd.uniform(-8, 8), twist=rnd.uniform(-16, 16))
    # frange balayée sur le côté (raie à gauche du perso) : mèches qui partent de la raie et traversent le front
    for j, (az, L, W, lean, k) in enumerate(((128, 9.0, 4.2, 30, 0.34), (148, 9.8, 4.4, 34, 0.4),
                                             (168, 9.4, 4.2, 38, 0.36), (186, 8.0, 3.6, 40, 0.3))):
        crown_lock(m, f'f_f{j}', c, rr, az, 74, L, W, mk(k), flat=0.5, turn=84, tip=0.2, segs=4, lean=lean,
                   skew=-0.12)
    crown_lock(m, 'f_f9', c, rr, 222, 74, 10.4, 3.6, mk(0.28), flat=0.5, turn=96, tip=0.15, segs=4, lean=-8)


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
    suit = cloth(SUIT, SUIT_L, shade(SUIT, 0.7), ao_bottom=0.1, folds=2, seed=180)
    lace = cloth(LACE, shade(LACE, 1.02), shade(LACE, 0.8), folds=3, seed=181)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2 + 0.4, y0, -d2], [b.torso_w - 0.8, 4.2, b.torso_d], suit)
    m.cube('torso', [-b.torso_w / 2, y0 + 4.0, -d2 - 0.1], [b.torso_w, y1 - y0 - 4.0, b.torso_d + 0.2], suit)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.9, 2.4], lace)                                  # col montant
    for k, w in enumerate((2.6, 2.2, 1.6)):                                                         # jabot
        m.cube('torso', [-w / 2, y1 - 1.4 - k * 1.1, -d2 - 0.3 - 0.1 * k], [w, 1.2, 0.4], lace)
    m.cube('torso', [-b.torso_w / 2 - 0.05, y0 - 0.2, -d2 - 0.15], [b.torso_w + 0.1, 0.9, b.torso_d + 0.3],
           cloth(rgb('#5a3a7a'), seed=182))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.9, b.shoulder_y - b.elbow_y + 0.6, 3.0), suit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.4, (2.8, b.elbow_y - b.wrist_y - 0.4, 2.9), suit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y - 0.2, (3.3, 1.0, 3.3), lace)                  # poignets
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.4, (2.4, 2.4, 2.4), skin_mat(SKIN))
    m.cube('body', [-b.torso_w / 2 + 0.6, b.hip_y - 1.2, -1.7], [b.torso_w - 1.2, 1.4, 3.4], suit)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.9, b.hip_y - b.knee_y + 0.2, 3.0), suit)
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.7, b.knee_y - 1.0, 2.8), suit)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.8, 1.1, 3.9), cloth(rgb('#14141a'), seed=183))


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
    """Quatre ailes de papillon (deux grandes en haut, deux plus petites en bas), fines, bord clair."""
    b = BODY
    y = b.shoulder_y - 1.0
    for sx in (-1, 1):
        for k, (w, h, rz, dy) in enumerate(((10.0, 9.0, -28, 0.0), (7.0, 6.0, 18, -5.0))):
            name = f'wing{sx}{k}'
            m.bone(name, [sx * 0.8, y + dy, b.torso_d / 2 + 0.3], 'torso', rotation=[0, -sx * 24, -sx * rz])
            x0 = sx * 0.8 + (0 if sx > 0 else -w)
            m.cube(name, [x0, y + dy - h * 0.4, b.torso_d / 2 + 0.3], [w, h, 0.25], wing_paint(190 + k))


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
