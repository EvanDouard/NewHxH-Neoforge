"""
Cheadle Yorkshire — même style. Taille moyenne (≈ 1,7 bloc) ; cheveux vert tendre en carré bouffant : mèches du
sommet qui suivent le crâne, côtés qui s'arrondissent et rebiquent vers l'extérieur sous les oreilles, frange
droite légère ; grandes lunettes rondes, air sérieux et méticuleux ; tailleur crème à col, jupe droite, collants
sombres, escarpins ; un bloc-notes sous le bras.
Expression « froid » : sévère (sourcils froncés derrière les lunettes).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import curtain, glasses

SKIN = rgb('#f4dcc8')
HAIR = rgb('#3e7e36')
HAIR_L = rgb('#86c06a')
LINE = rgb('#22261a')
FRAME = rgb('#3a2e2a')
LENS = rgb('#dcecf0')
SUIT = rgb('#e6dcc4')
SUIT_SH = rgb('#b8ac92')

BODY = Body(head=(7.8, 8.2, 7.6), head_y=19.8, hip_y=10.8, knee_y=5.5, shoulder_y=18.4, arm_x=4.2, leg_x=1.6,
            torso_w=5.8, torso_d=3.2)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#c0e8a8'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(179)
    c, R = (0.0, top - 3.6, 0.3), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=36)
    rr = R + 0.25
    for i, az in enumerate(range(-150, 151, 22)):
        crown_lock(m, f'c_m{i}', c, rr, az + rnd.uniform(-5, 5), 72 + rnd.uniform(-3, 3), 11.4 + rnd.uniform(-0.6, 0.6),
                   3.8 + rnd.uniform(-0.3, 0.9), mk(0.16 + 0.06 * math.cos(math.radians(az))), flat=0.4,
                   turn=76 + rnd.uniform(-3, 3), tip=0.4, segs=5, twist=rnd.uniform(-5, 5))
    for j, az in enumerate((150, 165, 180, 195, 210)):           # frange droite légère
        crown_lock(m, f'c_f{j}', c, rr + 0.05, az, 82, 3.6, 2.8, mk(0.28), flat=0.28, turn=74, tip=0.4, segs=3,
                   lean=(j - 2) * 6, side=(j - 2) * 8)


def face_paint(ctx, mode='calme'):
    severe = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 14, HAIR)
    for x0 in (12, 40):
        f.rect(x0 + 1, 27, x0 + 13, 33, (248, 248, 250))
        f.rect(x0 + 4, 27, x0 + 10, 33, rgb('#5a4a3a'))
        f.rect(x0 + 6, 28, x0 + 8, 32, (10, 8, 6))
        f.rect(x0, 25, x0 + 14, 27, LINE)
        if severe:
            f.poly((x0, 19, x0 + 14, 23, x0 + 14, 24, x0, 21), LINE)
    f.rect(32, 36, 33, 41, shade(SKIN, 0.86))
    f.rect(29, 48, 37, 48, rgb('#b0606a'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def suit_paint(ctx):
    cloth(SUIT, shade(SUIT, 1.04), SUIT_SH, ao_bottom=0.08, folds=2, seed=400)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.poly((24, 0, 42, 0, 36, 24, 30, 24), rgb('#f6f4ee'))
        f.poly((24, 0, 29, 0, 33, 22), SUIT_SH)
        f.poly((42, 0, 37, 0, 33, 22), SUIT_SH)
        for y in (30, 40, 50):
            f.rect(32, y, 34, y + 2, rgb('#8a7a5a'))


def build_body(m):
    b = BODY
    suit = cloth(SUIT, shade(SUIT, 1.04), SUIT_SH, folds=2, seed=401)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], suit_paint)
    m.cube('torso', [-1.1, y1 - 0.2, -1.1], [2.2, 0.6, 2.2], skin_mat(shade(SKIN, 0.92)))
    m.bone('skirt', [0, b.hip_y + 0.4, 0], 'body')
    m.cube('skirt', [-b.torso_w / 2 - 0.1, b.knee_y + 0.4, -d2 - 0.1], [b.torso_w + 0.2, b.hip_y - b.knee_y, b.torso_d + 0.2], suit)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.5, b.shoulder_y - b.elbow_y + 0.6, 2.6), suit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (2.4, b.elbow_y - b.wrist_y - 0.2, 2.5), suit)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.1, (2.0, 2.1, 2.0), skin_mat(SKIN))
    m.cube('left_arm', [b.arm_x + 1.2, b.elbow_y - 1.0, -2.0], [0.5, 4.0, 3.0], cloth(rgb('#3a5a8a'), seed=402))  # bloc-notes
    tights = cloth(rgb('#2a2a32'), rgb('#3a3a44'), rgb('#18181e'), seed=403)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.3, b.hip_y - b.knee_y + 0.2, 2.4), tights)
        m.box(f'{side}_shin', (cx, 0), 0.8, (2.2, b.knee_y - 0.8, 2.3), tights)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.3, 0.9, 3.2), flat(rgb('#5a3a2a'), 0.9))


def build():
    m = Model('cheadle', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    glasses(m, BODY, FRAME, LENS, gy=30, w=2.6, h=2.4)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=12, leg_swing=20, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
