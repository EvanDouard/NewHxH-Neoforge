"""
Pariston Hill — même style. Mince (≈ 1,8 bloc) ; cheveux blonds ondulés et bouffants : grosses boucles du sommet
qui gonflent vers l'avant et les côtés, mèches qui ondulent sur les tempes et la nuque ; yeux mi-clos, grand
sourire permanent ; costume gris-bleu clair trois pièces, chemise blanche, cravate rouge, chaussures marron.
Expression « froid » : sourire en coin et regard calculateur (yeux ouverts, pupilles fines).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f4dac4')
HAIR = rgb('#d8a84a')
HAIR_L = rgb('#f8e08a')
LINE = rgb('#3a2a1e')
SUIT = rgb('#8ea0b8')
SUIT_L = rgb('#a8b8cc')
SHIRT = rgb('#f4f2ee')
TIE = rgb('#c0303a')

BODY = Body(head=(7.8, 8.4, 7.6), head_y=21.0, hip_y=11.5, knee_y=5.8, shoulder_y=19.5, arm_x=4.5, leg_x=1.75,
            torso_w=6.4, torso_d=3.5)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#fff4c8'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(197)
    c, R = (0.0, top - 3.4, 0.3), 4.8
    scalp(m, c, R, lambda k: mk(k)(0.5), skip_front=48, front_phi=40)
    rr = R + 0.3
    # boucles bouffantes : mèches larges et courtes, très courbées (elles s'enroulent), sur deux couches
    for layer, (phi, L, turn, W) in enumerate(((60, 5.6, 150, 4.6), (92, 6.2, 160, 5.0))):
        for i, az in enumerate(range(0 + layer * 12, 360, 24)):
            if layer == 0 and 140 < az < 220:
                continue
            crown_lock(m, f'p_l{layer}{i}', c, rr + 0.25 * layer, az + rnd.uniform(-5, 5), phi + rnd.uniform(-5, 5),
                       L + rnd.uniform(-0.4, 0.5), W, mk(0.14 + 0.08 * layer + 0.04 * (i % 2)), flat=0.6, turn=turn,
                       tip=0.3, segs=5, twist=rnd.uniform(-12, 12), skew=rnd.uniform(-0.1, 0.1),
                       side=rnd.choice((-1, 1)) * rnd.uniform(25, 55))                    # boucles distinctes
    for i, az in enumerate(range(6, 360, 30)):                    # petites boucles serrées sur le dessus et les tempes
        crown_lock(m, f'p_c{i}', c, rr + 0.5, az + rnd.uniform(-6, 6), 108 + rnd.uniform(-10, 10), 3.4, 2.8,
                   mk(0.3), flat=0.65, turn=200, tip=0.4, segs=5, twist=rnd.uniform(-30, 30),
                   side=rnd.choice((-1, 1)) * rnd.uniform(30, 60))
    for j, (az, lean) in enumerate(((150, -10), (170, -2), (192, 6), (212, 12))):     # boucles sur le front
        crown_lock(m, f'p_f{j}', c, rr + 0.2, az, 76, 5.0, 4.0, mk(0.32), flat=0.6, turn=180, tip=0.3, segs=5,
                   lean=lean, twist=rnd.uniform(-10, 10), side=(1 if j % 2 else -1) * 35)


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 8, HAIR)
    for x0 in (12, 40):
        if cold:
            f.rect(x0 + 1, 27, x0 + 13, 32, (248, 246, 240))
            f.rect(x0 + 5, 27, x0 + 9, 32, rgb('#6a5a3a'))
            f.rect(x0 + 6, 28, x0 + 8, 31, (10, 8, 6))
            f.rect(x0, 25, x0 + 14, 27, LINE)
        else:
            f.poly((x0, 31, x0 + 7, 27, x0 + 14, 31, x0 + 14, 33, x0 + 7, 29, x0, 33), LINE)   # yeux plissés ^^
    f.rect(32, 35, 33, 41, shade(SKIN, 0.86))
    if cold:
        f.poly((26, 49, 40, 46, 40, 48, 26, 50), rgb('#7a4a44'))
    else:
        f.poly((20, 45, 46, 45, 42, 51, 24, 51), rgb('#7a3a3a'))   # grand sourire
        f.rect(22, 45, 44, 47, (250, 250, 250))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.6))], fill=HAIR)


def suit_paint(ctx):
    cloth(SUIT, SUIT_L, shade(SUIT, 0.72), ao_bottom=0.1, folds=2, seed=440)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.poly((22, 0, 44, 0, 36, 34, 30, 34), SHIRT)
        f.poly((31, 2, 35, 2, 36, 30, 33, 34, 30, 30), TIE)
        f.poly((22, 0, 28, 0, 33, 34), SUIT_L)
        f.poly((44, 0, 38, 0, 33, 34), SUIT_L)


def build_body(m):
    b = BODY
    suit = cloth(SUIT, SUIT_L, shade(SUIT, 0.72), folds=2, seed=441)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 1.0, -d2], [b.torso_w, y1 - y0 + 1.0, b.torso_d], suit_paint)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.6, 2.4], skin_mat(shade(SKIN, 0.92)))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.9, b.shoulder_y - b.elbow_y + 0.6, 3.0), suit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (2.8, b.elbow_y - b.wrist_y - 0.2, 2.9), suit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.1, (2.9, 0.5, 3.0), cloth(SHIRT, seed=442))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.3, (2.3, 2.3, 2.3), skin_mat(SKIN))
    pants = cloth(SUIT, SUIT_L, shade(SUIT, 0.72), folds=2, seed=443)
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.2, -1.75], [b.torso_w - 0.6, 1.5, 3.5], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.9, b.hip_y - b.knee_y + 0.2, 3.0), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (2.8, b.knee_y - 1.0, 2.9), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.9, 1.1, 3.9), cloth(rgb('#6a4228'), seed=444))


def build():
    m = Model('pariston', density=4, tex_width=512)
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
