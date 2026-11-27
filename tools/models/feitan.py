"""
Feitan Portor — même style. Petit et sec (≈ 1,55 bloc) ; cheveux noirs en bataille : mèches pointues qui partent
du sommet et retombent dans tous les sens, longue frange effilée qui tombe entre les yeux, côtés jusqu'à la
mâchoire ; petits yeux noirs tombants ; grand col montant noir qui couvre le bas du visage, orné d'une mâchoire
de crâne blanche ; long manteau sombre bleu nuit, pantalon et bottes noirs.
Expression « froid » : regard de tortionnaire (yeux plissés, pupilles rouges).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#efd6c2')
HAIR = rgb('#101016')
HAIR_L = rgb('#2e3448')
LINE = rgb('#141218')
COAT = rgb('#1c2030')
COAT_L = rgb('#2c3246')
COLLAR = rgb('#121218')
BONE = rgb('#ecebe4')

BODY = Body(head=(7.8, 8.2, 7.6), head_y=16.2, hip_y=8.4, knee_y=4.2, shoulder_y=14.8, arm_x=4.4, leg_x=1.7,
            torso_w=6.4, torso_d=3.6)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#4c5470'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(83)
    c, R = (0.0, top - 3.4, 0.3), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=30)
    rr = R + 0.25
    # mèches pointues en bataille autour du crâne
    for i, az in enumerate(range(0, 360, 24)):
        if 140 < az < 220:
            continue
        side = 1 - abs(((az + 180) % 360) - 180) / 180          # 1 devant, 0 derrière
        crown_lock(m, f'f_m{i}', c, rr, az + rnd.uniform(-6, 6), 78 + rnd.uniform(-8, 8),
                   7.0 + 1.6 * side + rnd.uniform(-0.8, 0.8), 4.0 + rnd.uniform(-0.3, 0.5),
                   mk(0.1 + 0.08 * side + 0.04 * (i % 2)), flat=0.5, turn=96 + rnd.uniform(-14, 14), tip=0.08,
                   segs=4, lean=rnd.uniform(-14, 14), twist=rnd.uniform(-16, 16), skew=rnd.uniform(-0.16, 0.16))
    # pointes du sommet qui rebiquent
    for i, az in enumerate((-60, 20, 100, 200, 280)):
        crown_lock(m, f'f_k{i}', c, rr + 0.3, az, 104, 4.6 + rnd.uniform(-0.4, 0.8), 3.4, mk(0.24), flat=0.5,
                   turn=20, tip=0.06, segs=3, twist=rnd.uniform(-20, 20))
    # longue frange effilée : une mèche centrale qui tombe entre les yeux, d'autres plus courtes
    for j, (az, L, W, lean, k) in enumerate(((146, 5.4, 3.2, -14, 0.24), (162, 6.4, 3.4, -6, 0.3),
                                             (178, 8.8, 2.8, 2, 0.36), (194, 6.2, 3.4, 8, 0.3),
                                             (212, 5.2, 3.2, 14, 0.24))):
        crown_lock(m, f'f_f{j}', c, rr, az, 64, L, W, mk(k), flat=0.45, turn=70, tip=0.06, segs=3, lean=lean,
                   skew=rnd.uniform(-0.14, 0.14))
    for sx in (-1, 1):                                          # côtés jusqu'à la mâchoire
        crown_lock(m, f'f_s{sx}', c, rr, sx * 124, 60, 7.4, 3.6, mk(0.12), flat=0.45, turn=66, tip=0.08, segs=3,
                   lean=sx * 4)


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 10, HAIR)
    for x0 in (12, 40):
        f.poly((x0 - 1, 26, x0 + 15, 28, x0 + 15, 30, x0 - 1, 28), LINE)
        f.rect(x0 + 1, 29, x0 + 13, 33 if not cold else 32, (244, 244, 246))
        f.rect(x0 + 5, 29, x0 + 10, 33, rgb('#c01c28') if cold else rgb('#1a1a20'))
        f.rect(x0 + 7, 30, x0 + 8, 32, (8, 8, 10))
    f.rect(32, 36, 33, 40, shade(SKIN, 0.84))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.6))], fill=HAIR)


def collar_paint(ctx):
    cloth(COLLAR, shade(COLLAR, 1.4), shade(COLLAR, 0.7), folds=2, seed=260)(ctx)
    if ctx.face != 'north':
        return
    f = FaceGrid(ctx)                                            # mâchoire de crâne : deux rangées de dents
    for x in range(9, 57, 8):
        f.rect(x, 20, x + 5, 31, BONE)
        f.rect(x, 35, x + 5, 46, BONE)
    f.rect(6, 31, 60, 35, shade(BONE, 0.7))


def build_body(m):
    b = BODY
    coat = cloth(COAT, COAT_L, shade(COAT, 0.7), ao_bottom=0.1, folds=3, seed=261)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.2, -d2], [b.torso_w, y1 - y0 + 0.2, b.torso_d], coat)
    # grand col montant, fixé au torse, qui monte jusque sous les yeux
    m.cube('torso', [-b.head[0] / 2 - 0.3, y1 - 0.4, -b.head[2] / 2 - 0.5], [b.head[0] + 0.6, 4.6, 1.0], collar_paint)
    for sx in (-1, 1):
        m.cube('torso', [sx * (b.head[0] / 2 + 0.3) - (1.0 if sx > 0 else 0), y1 - 0.4, -b.head[2] / 2 - 0.5],
               [1.0, 4.6, b.head[2] + 0.6], collar_paint)
    m.cube('torso', [-b.head[0] / 2 - 0.3, y1 - 0.4, b.head[2] / 2 - 0.4], [b.head[0] + 0.6, 3.8, 1.0], collar_paint)
    # pans longs du manteau (sous les genoux)
    for name, sz in (('coat_f', -1), ('coat_b', 1)):
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 5, 0, 0])
        if sz < 0:
            for sx in (-1, 1):
                m.cube(name, [sx * b.torso_w / 2 - (2.6 if sx > 0 else 0), b.knee_y - 1.6, sz * d2 - 0.35],
                       [2.6, y0 - b.knee_y + 1.6, 0.7], coat)
        else:
            m.cube(name, [-b.torso_w / 2 - 0.1, b.knee_y - 1.6, sz * d2 - 0.35], [b.torso_w + 0.2, y0 - b.knee_y + 1.6, 0.7],
                   coat)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.9, b.shoulder_y - b.elbow_y + 0.6, 3.0), coat)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y - 0.4, (3.1, b.elbow_y - b.wrist_y + 0.4, 3.2), coat)  # manches longues
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.2, (2.3, 2.2, 2.3), skin_mat(SKIN))
    pants = cloth(rgb('#141418'), rgb('#22222a'), rgb('#0a0a0c'), folds=2, seed=262)
    m.cube('body', [-b.torso_w / 2 + 0.4, b.hip_y - 1.2, -1.7], [b.torso_w - 0.8, 1.4, 3.4], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.9, b.hip_y - b.knee_y + 0.2, 3.0), pants)
        m.box(f'{side}_shin', (cx, 0), 0.0, (3.0, b.knee_y, 3.1), cloth(rgb('#0e0e12'), seed=263))   # bottes
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.0, 1.0, 3.9), cloth(rgb('#0e0e12'), seed=264))


def build():
    m = Model('feitan', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
