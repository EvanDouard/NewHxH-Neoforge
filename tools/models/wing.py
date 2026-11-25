"""
Wing — même style. Mince (≈ 1,8 bloc) ; cheveux brun foncé en bataille, mèches qui tombent dans tous les sens,
grand épi rebelle sur le sommet ; lunettes rondes (verres clairs, monture fine) ; air doux de professeur ; chemise
blanche à moitié rentrée (un pan qui dépasse), pantalon sombre, chaussures.
Expression « froid » : sévère (sourcils froncés, regard dur derrière les lunettes).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#eed2ba')
HAIR = rgb('#2a1e18')
HAIR_L = rgb('#5a4232')
LINE = rgb('#24181a')
FRAME = rgb('#3a3036')
LENS = rgb('#cfe0ea')
SHIRT = rgb('#f2f0ea')

BODY = Body(head=(7.8, 8.4, 7.6), head_y=21.0, hip_y=11.5, knee_y=5.8, shoulder_y=19.5, arm_x=4.5, leg_x=1.75,
            torso_w=6.4, torso_d=3.5)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#8a6a50'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(163)
    c, R = (0.0, top - 3.4, 0.3), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=34)
    rr = R + 0.25
    for i, az in enumerate(range(0, 360, 24)):                    # en bataille : mèches qui partent dans tous les sens
        if 140 < az < 220:
            continue
        crown_lock(m, f'w_m{i}', c, rr, az + rnd.uniform(-8, 8), 80 + rnd.uniform(-8, 8), 7.0 + rnd.uniform(-0.8, 1.0),
                   4.0 + rnd.uniform(-0.3, 0.4), mk(0.12 + 0.05 * (i % 2)), flat=0.5, turn=92 + rnd.uniform(-20, 16),
                   tip=0.12, segs=4, lean=rnd.uniform(-18, 18), twist=rnd.uniform(-18, 18),
                   skew=rnd.uniform(-0.16, 0.16))
    crown_lock(m, 'w_epi', c, rr + 0.3, 30, 106, 5.2, 3.2, mk(0.3), flat=0.5, turn=-30, tip=0.06, segs=3, twist=20)
    crown_lock(m, 'w_epi2', c, rr + 0.2, -10, 100, 4.0, 2.8, mk(0.26), flat=0.5, turn=-14, tip=0.06, segs=2,
               twist=-16)
    for j, (az, L, W, lean) in enumerate(((148, 5.6, 3.6, -16), (164, 6.2, 3.6, -6), (182, 5.8, 3.4, 6),
                                          (198, 6.4, 3.6, 12), (214, 5.2, 3.4, 18))):
        crown_lock(m, f'w_f{j}', c, rr, az, 70, L, W, mk(0.3), flat=0.45, turn=80, tip=0.1, segs=3, lean=lean,
                   skew=rnd.uniform(-0.15, 0.15), twist=rnd.uniform(-10, 10))


def face_paint(ctx, mode='calme'):
    severe = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 8, HAIR)
    for x0 in (12, 40):
        if severe:
            f.poly((x0 - 1, 21, x0 + 15, 25, x0 + 15, 27, x0 - 1, 23), LINE)
        f.rect(x0 + 1, 28, x0 + 13, 33, (246, 244, 240))
        f.rect(x0 + 4, 28, x0 + 10, 33, rgb('#4a3426'))
        f.rect(x0 + 6, 29, x0 + 8, 32, (10, 8, 6))
        f.rect(x0, 26, x0 + 14, 28, LINE)
    f.rect(32, 36, 33, 42, shade(SKIN, 0.84))
    if severe:
        f.rect(28, 50, 38, 50, rgb('#7a4a44'))
    else:
        f.poly((27, 49, 39, 49, 37, 51, 29, 51), rgb('#9a5a54'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.55))], fill=HAIR)


def build_glasses(m):
    b = BODY
    hx, hz = b.head[0] / 2, b.head[2] / 2
    yl = b.head_y + b.head[1] * (1 - 30.5 / 60)
    fr = flat(FRAME, 0.9)
    for sx in (-1, 1):
        cx = sx * 1.75
        m.cube('head', [cx - 1.1, yl - 1.0, -hz - 0.36], [2.2, 2.0, 0.1], flat(LENS, 0.95))
        m.cube('head', [cx - 1.2, yl + 0.95, -hz - 0.42], [2.4, 0.18, 0.1], fr)
        m.cube('head', [cx - 1.2, yl - 1.15, -hz - 0.42], [2.4, 0.18, 0.1], fr)
        m.cube('head', [cx - 1.3, yl - 1.0, -hz - 0.42], [0.18, 2.0, 0.1], fr)
        m.cube('head', [cx + 1.12, yl - 1.0, -hz - 0.42], [0.18, 2.0, 0.1], fr)
        m.cube('head', [sx * (hx + 0.05) - 0.06, yl, -hz - 0.2], [0.12, 0.2, hz + 0.4], fr)
    m.cube('head', [-0.6, yl + 0.2, -hz - 0.42], [1.2, 0.18, 0.1], fr)


def build_body(m):
    b = BODY
    shirt = cloth(SHIRT, shade(SHIRT, 1.03), shade(SHIRT, 0.8), folds=3, seed=370)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], shirt)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.6, 2.4], skin_mat(shade(SKIN, 0.92)))
    m.cube('torso', [-1.8, y1 - 0.8, -d2 - 0.2], [3.6, 1.0, b.torso_d + 0.4], shirt)                     # col
    m.cube('torso', [0.4, y0 - 2.2, -d2 - 0.15], [2.6, 2.6, 0.5], shirt)                                # pan qui dépasse
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.9, b.shoulder_y - b.elbow_y + 0.6, 3.0), shirt)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.1, (2.8, b.elbow_y - b.wrist_y - 0.1, 2.9), shirt)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.3, (2.3, 2.3, 2.3), skin_mat(SKIN))
    pants = cloth(rgb('#2c2a30'), rgb('#3e3c44'), rgb('#18171c'), folds=2, seed=371)
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.2, -1.75], [b.torso_w - 0.6, 1.5, 3.5], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.9, b.hip_y - b.knee_y + 0.2, 3.0), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.8, b.knee_y - 1.0, 2.9), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.9, 1.1, 3.9), cloth(rgb('#3a2a20'), seed=372))


def build():
    m = Model('wing', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_glasses(m)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
