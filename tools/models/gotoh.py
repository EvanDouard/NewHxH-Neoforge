"""
Gotoh, majordome en chef des Zoldyck — même style. Grand et droit (≈ 1,85 bloc) ; cheveux noirs courts et
lisses, peignés en arrière avec une raie sur le côté, tempes nettes ; petites lunettes rondes, visage sévère et
digne, fine moustache ; queue-de-pie noire, gilet, chemise blanche, cravate sombre ; une pièce entre les doigts.
Expression « froid » : regard de tireur (yeux plissés derrière les lunettes).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from castkit import suit_body
from chibi import FaceGrid, crown_lock, flat, scalp, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import glasses

SKIN = rgb('#ecceb4')
HAIR = rgb('#141418')
HAIR_L = rgb('#383c4c')
LINE = rgb('#1e1a1c')

BODY = Body(head=(7.6, 8.4, 7.4), head_y=21.6, hip_y=11.8, knee_y=6.0, shoulder_y=20.1, arm_x=4.5, leg_x=1.75,
            torso_w=6.4, torso_d=3.5)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5a6080'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(301)
    c, R = (0.0, top - 3.7, 0.35), 4.3
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=48, front_phi=40)
    rr = R + 0.12
    for i, x_az in enumerate((-34, -20, -6, 8, 22, 36)):          # peigné en arrière, raie à gauche du perso
        crown_lock(m, f'g_t{i}', c, rr, x_az + 6, 140, 11.0, 3.4, mk(0.22 + 0.02 * (2.5 - abs(i - 2.5))), flat=0.22,
                   turn=118, tip=0.5, segs=5, lean=-4)
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 60, 124, 7.0), (sx * 84, 112, 4.2), (sx * 100, 104, 3.0))):
            crown_lock(m, f'g_s{sx}{j}', c, rr - 0.04 * j, az, phi, L, 3.4, mk(0.12), flat=0.22, turn=100, tip=0.5, segs=4)
    crown_lock(m, 'g_part', c, rr + 0.1, 150, 138, 9.0, 3.2, mk(0.3), flat=0.22, turn=116, tip=0.5, segs=4, lean=-10)
    m.cube('hair', [-2.0, top - 0.25, -3.8], [0.25, 0.3, 4.4], flat(shade(HAIR, 1.6), 0.9))          # ligne de la raie


def face_paint(ctx, mode='calme'):
    aim = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 4, HAIR)
    for x0 in (12, 40):
        top = 29 if aim else 28
        f.rect(x0 + 1, top, x0 + 13, 32, (244, 242, 238))
        f.rect(x0 + 5, top, x0 + 9, 32, rgb('#2a2228'))
        f.poly((x0, 23, x0 + 14, 22, x0 + 14, 24, x0, 25), LINE)
    f.rect(32, 35, 33, 42, shade(SKIN, 0.82))
    f.rect(25, 44, 41, 45, HAIR)
    f.rect(28, 49, 38, 49, rgb('#7a4a44'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (0.8 if ctx.face == 'south' else 1 if ctx.face == 'up' else 0.5))],
                           fill=HAIR)


def build():
    m = Model('gotoh', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    glasses(m, BODY, rgb('#3a3036'), rgb('#e0ecf2'), gy=30, w=2.0, h=1.8)
    build_hair(m)
    suit_body(m, BODY, SKIN, rgb('#16161c'), rgb('#2a2a34'), rgb('#f2f0ec'), tie=rgb('#2a2a3a'), tails=True)
    m.cube('right_hand', [-BODY.arm_x - 0.6, BODY.wrist_y - 2.6, -1.6], [1.2, 1.2, 0.3], flat(rgb('#e8c860'), 1.0))
    m.animations = humanoid_animations(arm_swing=10, leg_swing=20, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
