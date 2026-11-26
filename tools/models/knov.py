"""
Knov — même style. Grand et élancé (≈ 1,85 bloc) ; cheveux noirs gominés plaqués en arrière, impeccables, longs
favoris en pointe ; visage anguleux, yeux étroits sérieux, fine moustache ; costume trois pièces bleu nuit,
chemise blanche, cravate fine, chaussures vernies.
Expression « froid » : terreur contenue (yeux écarquillés, sueur) — clin d'œil à son passage à NGL.
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#ecd0b8')
HAIR = rgb('#141418')
HAIR_L = rgb('#383c4c')
LINE = rgb('#1e1a1c')
SUIT = rgb('#1e2840')
SUIT_L = rgb('#303c5c')
SHIRT = rgb('#f2f0ec')
TIE = rgb('#5a2a3a')

BODY = Body(head=(7.6, 8.6, 7.4), head_y=21.6, hip_y=11.8, knee_y=6.0, shoulder_y=20.1, arm_x=4.5, leg_x=1.75,
            torso_w=6.4, torso_d=3.5)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5a6080'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(199)
    c, R = (0.0, top - 3.4, 0.4), 4.85
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=48, front_phi=40)
    rr = R + 0.18
    for i, x_az in enumerate((-30, -18, -6, 6, 18, 30)):           # gominé plaqué en arrière
        crown_lock(m, f'k_t{i}', c, rr, x_az, 136, 9.6, 3.6, mk(0.24 + 0.02 * (2.5 - abs(i - 2.5))), flat=0.28,
                   turn=104, tip=0.35, segs=5)
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 58, 124, 8.6), (sx * 82, 108, 7.4), (sx * 104, 94, 6.2))):
            crown_lock(m, f'k_s{sx}{j}', c, rr - 0.04 * j, az, phi, L, 3.4, mk(0.12), flat=0.3, turn=100, tip=0.35,
                       segs=4)
        lock(m, f'k_fav{sx}', [sx * 3.85, b.head_y + 5.4, -0.9], 3.6, 1.4, 180, 0, mk(0.1), flat=0.4, segs=2,
             root=1.0, tip=0.15, taper_pow=0.6, parent='head')


def face_paint(ctx, mode='calme'):
    scared = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 4, HAIR)
    f.poly((9, 21, 28, 20, 28, 22, 9, 23), LINE)
    f.poly((57, 21, 38, 20, 38, 22, 57, 23), LINE)
    for x0 in (12, 40):
        if scared:
            f.rect(x0, 25, x0 + 14, 33, (250, 250, 250))
            f.rect(x0 + 6, 28, x0 + 8, 30, (10, 8, 8))
        else:
            f.rect(x0, 27, x0 + 14, 31, (244, 242, 238))
            f.rect(x0 + 5, 27, x0 + 10, 31, rgb('#2a2228'))
            f.rect(x0 + 6, 28, x0 + 8, 30, (8, 6, 6))
            f.rect(x0 - 1, 25, x0 + 15, 27, LINE)
    if scared:
        f.rect(56, 14, 59, 22, rgb('#bcd8f0'))
    f.rect(32, 34, 33, 42, shade(SKIN, 0.82))
    f.rect(24, 44, 42, 45, HAIR)                                  # fine moustache
    f.rect(28, 49, 38, 49, rgb('#7a4a44'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (0.8 if ctx.face == 'south' else 1 if ctx.face == 'up' else 0.5))],
                           fill=HAIR)


def suit_paint(ctx):
    cloth(SUIT, SUIT_L, shade(SUIT, 0.72), ao_bottom=0.1, folds=2, seed=450)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.poly((22, 0, 44, 0, 36, 34, 30, 34), SHIRT)
        f.poly((32, 2, 34, 2, 35, 32, 33, 34, 31, 32), TIE)
        f.poly((22, 0, 28, 0, 33, 34), SUIT_L)
        f.poly((44, 0, 38, 0, 33, 34), SUIT_L)


def build_body(m):
    b = BODY
    suit = cloth(SUIT, SUIT_L, shade(SUIT, 0.72), folds=2, seed=451)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 1.0, -d2], [b.torso_w, y1 - y0 + 1.0, b.torso_d], suit_paint)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.6, 2.4], skin_mat(shade(SKIN, 0.92)))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.9, b.shoulder_y - b.elbow_y + 0.6, 3.0), suit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (2.8, b.elbow_y - b.wrist_y - 0.2, 2.9), suit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.1, (2.9, 0.5, 3.0), cloth(SHIRT, seed=452))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.3, (2.3, 2.3, 2.3), skin_mat(SKIN))
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.2, -1.75], [b.torso_w - 0.6, 1.5, 3.5], suit)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.9, b.hip_y - b.knee_y + 0.2, 3.0), suit)
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.8, b.knee_y - 1.0, 2.9), suit)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.9, 1.1, 3.9), flat(rgb('#0e0e12'), 0.95))


def build():
    m = Model('knov', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=12, leg_swing=20, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
