"""
Shalnark — même style. Mince (≈ 1,75 bloc), visage juvénile ; cheveux blonds courts et légers : mèches pointues
du sommet qui retombent en coupe arrondie, frange en pointes sur le front, nuque courte ; grands yeux verts,
sourire aimable ; veste kaki ouverte sur un t-shirt clair, jean bleu, baskets ; téléphone à antenne dans la main.
Expression « froid » : sourire figé et yeux plissés (mode automatique).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f4dcc8')
HAIR = rgb('#d6b25a')
HAIR_L = rgb('#f6e2a0')
LINE = rgb('#3a2a1a')
EYE = rgb('#3a9a5a')
JACKET = rgb('#7a7448')
JACKET_L = rgb('#9a9462')
TEE = rgb('#ecebe4')
JEANS = rgb('#3a5a8a')

BODY = Body(head=(7.8, 8.2, 7.6), head_y=20.0, hip_y=10.9, knee_y=5.5, shoulder_y=18.6, arm_x=4.4, leg_x=1.7,
            torso_w=6.2, torso_d=3.4)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#fff0c0'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(137)
    c, R = (0.0, top - 3.5, 0.3), 4.35
    scalp(m, c, R, lambda k: mk(k)(0.5), skip_front=46, front_phi=32)
    rr = R + 0.25
    for i, az in enumerate(range(0, 360, 24)):
        if 140 < az < 220:
            continue
        crown_lock(m, f's_m{i}', c, rr, az + rnd.uniform(-5, 5), 82 + rnd.uniform(-5, 5), 7.4 + rnd.uniform(-0.6, 0.8),
                   4.0, mk(0.14 + 0.08 * math.cos(math.radians(az - 150))), flat=0.48, turn=108 + rnd.uniform(-8, 8),
                   tip=0.12, segs=4, lean=rnd.uniform(-8, 8), twist=rnd.uniform(-10, 10), skew=rnd.uniform(-0.12, 0.12))
    for i, az in enumerate((-40, 40, 120, 240)):                  # quelques épis légers sur le dessus
        crown_lock(m, f's_k{i}', c, rr + 0.3, az, 100, 3.8, 3.0, mk(0.3), flat=0.5, turn=30, tip=0.08, segs=2,
                   twist=rnd.uniform(-20, 20))
    for j, (az, L, W, lean) in enumerate(((146, 5.2, 3.4, -12), (162, 5.8, 3.6, -5), (178, 6.2, 3.4, 0),
                                          (194, 5.8, 3.6, 5), (210, 5.2, 3.4, 12))):
        crown_lock(m, f's_f{j}', c, rr, az, 70, L, W, mk(0.36), flat=0.45, turn=84, tip=0.1, segs=3, lean=lean,
                   skew=rnd.uniform(-0.12, 0.12))


def face_paint(ctx, mode='calme'):
    auto = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 9, HAIR)
    for x0 in (11, 39):
        if auto:
            f.poly((x0, 30, x0 + 16, 30, x0 + 14, 33, x0 + 2, 33), LINE)
        else:
            f.rect(x0, 24, x0 + 16, 36, (250, 250, 250))
            f.rect(x0 + 3, 24, x0 + 13, 36, EYE)
            f.rect(x0 + 5, 27, x0 + 11, 35, (12, 30, 16))
            f.rect(x0 + 4, 25, x0 + 7, 28, (255, 255, 255))
            f.rect(x0 - 1, 22, x0 + 17, 24, LINE)
    f.poly((28, 46, 38, 46, 36, 49, 30, 49), rgb('#b8706a'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.6))], fill=HAIR)


def jacket_paint(ctx):
    cloth(JACKET, JACKET_L, shade(JACKET, 0.7), ao_bottom=0.1, folds=3, seed=320)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.rect(22, 0, 44, 60, TEE)


def build_body(m):
    b = BODY
    jacket = cloth(JACKET, JACKET_L, shade(JACKET, 0.7), folds=3, seed=321)
    jeans = cloth(JEANS, shade(JEANS, 1.2), shade(JEANS, 0.7), ao_top=0.1, folds=2, seed=322)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.6, -d2], [b.torso_w, y1 - y0 + 0.6, b.torso_d], jacket_paint)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.6, 2.4], skin_mat(shade(SKIN, 0.92)))
    for sx in (-1, 1):                                             # col de la veste
        m.cube('torso', [sx * 2.0 - 0.9, y1 - 1.0, -d2 - 0.2], [1.8, 1.4, b.torso_d + 0.4], jacket)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.9, b.shoulder_y - b.elbow_y + 0.6, 3.0), jacket)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (2.8, b.elbow_y - b.wrist_y - 0.2, 2.9), jacket)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.2, (2.3, 2.2, 2.3), skin_mat(SKIN))
    cx = -b.arm_x                                                  # téléphone à antenne
    m.cube('right_hand', [cx - 0.6, b.wrist_y - 2.6, -2.0], [1.2, 2.0, 0.5], flat(rgb('#2a2a34'), 0.9))
    m.cube('right_hand', [cx - 0.15, b.wrist_y - 0.6, -1.9], [0.3, 1.8, 0.3], flat(rgb('#c84a4a'), 0.9))
    m.cube('body', [-b.torso_w / 2 + 0.4, b.hip_y - 1.2, -1.6], [b.torso_w - 0.8, 1.4, 3.2], jeans)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.9, b.hip_y - b.knee_y + 0.2, 3.0), jeans)
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.8, b.knee_y - 1.0, 2.9), jeans)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.0, 1.2, 3.9), cloth(rgb('#e8e6e0'), rgb('#ffffff'), rgb('#9a3a3a'), seed=323))


def build():
    m = Model('shalnark', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
