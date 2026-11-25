"""
Biscuit Krueger (apparence de petite fille) — même style. Petite (≈ 1,4 bloc), tête plus grosse ; cheveux blonds
dorés : masse arrondie sur le crâne, frange en mèches pointues, deux grandes couettes hautes ondulées qui tombent
jusqu'au bas du dos, nouées de rubans rouges ; grands yeux bleu clair brillants, joues roses ; robe rose à
froufrous (col blanc, gros nœud rouge, jupe évasée à volants), chaussettes blanches, souliers rouges.
Expression « froid » : sourire carnassier de combattante (yeux plissés, sourcils froncés).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#fbe3d2')
HAIR = rgb('#d9a63a')
HAIR_L = rgb('#f8dc7a')
LINE = rgb('#5a3a20')
EYE = rgb('#5ab4ea')
DRESS = rgb('#f0a0bc')
DRESS_L = rgb('#fbc6d8')
WHITE = rgb('#fbf8f4')
RED = rgb('#d8303c')

BODY = Body(head=(8.4, 8.4, 8.0), head_y=13.8, hip_y=6.8, knee_y=3.4, shoulder_y=12.6, arm_x=3.8, leg_x=1.35,
            torso_w=5.2, torso_d=3.2)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#fff2b8'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(71)
    c, R = (0.0, top - 3.6, 0.3), 4.85
    scalp(m, c, R, lambda k: mk(k)(0.5), skip_front=46, front_phi=30)
    rr = R + 0.25
    # masse arrondie : mèches courtes du sommet qui suivent le crâne (carré court derrière les couettes)
    for i, az in enumerate(range(0, 360, 26)):
        if 136 < az < 224:
            continue
        crown_lock(m, f'b_m{i}', c, rr, az + rnd.uniform(-4, 4), 84 + rnd.uniform(-4, 4), 7.6 + rnd.uniform(-0.5, 0.6),
                   4.2, mk(0.18 + 0.06 * math.cos(math.radians(az - 150))), flat=0.45, turn=110, tip=0.3, segs=4,
                   twist=rnd.uniform(-8, 8), skew=rnd.uniform(-0.1, 0.1))
    # nuque : seconde couche qui couvre l'arrière de la tête jusqu'au cou
    for i, az in enumerate(range(-100, 101, 20)):
        crown_lock(m, f'b_n{i}', c, rr + 0.1, az + rnd.uniform(-4, 4), 44 + rnd.uniform(-4, 4), 6.4 + rnd.uniform(-0.4, 0.6),
                   4.0, mk(0.06 + 0.04 * (i % 2)), flat=0.45, turn=58, tip=0.25, segs=3, twist=rnd.uniform(-8, 8),
                   skew=rnd.uniform(-0.1, 0.1))
    # frange : mèches pointues sur le front
    for j, (az, L, W, lean) in enumerate(((144, 5.0, 3.4, -10), (160, 5.8, 3.6, -4), (176, 6.2, 3.6, 0),
                                          (192, 5.8, 3.6, 5), (208, 5.4, 3.4, 10), (222, 4.6, 3.2, 14))):
        crown_lock(m, f'b_f{j}', c, rr, az, 58, L - 0.6, W, mk(0.36), flat=0.45, turn=66, tip=0.1, segs=3, lean=lean,
                   skew=rnd.uniform(-0.12, 0.12))
    # couettes hautes : nœud rouge sur le côté du crâne, longue mèche ondulée qui tombe dans le dos
    for sx in (-1, 1):
        a = math.radians(sx * 70)
        r = rr + 0.4
        base = [c[0] + r * math.sin(a), c[1] + r * math.sin(math.radians(48)), c[2] + r * math.cos(a) * 0.7]
        m.bone(f'tail{sx}', base, 'hair', rotation=[0, 0, 0])
        m.cube(f'tail{sx}', [base[0] - 1.2, base[1] - 1.2, base[2] - 1.2], [2.4, 2.4, 2.4], flat(RED, 0.95))
        for k, dz in enumerate((-1, 1)):                                                 # boucles du ruban
            m.cube(f'tail{sx}', [base[0] - 0.4, base[1] - 0.2 + 0.0, base[2] + dz * 1.0 - 0.9], [0.8, 2.0, 1.8],
                   flat(shade(RED, 1.1), 0.9), pivot=base, rotation=[dz * 30, 0, 0])
        for k in range(3):                                                               # trois mèches par couette
            lock(m, f'b_t{sx}{k}', [base[0] + sx * 0.4, base[1] - 0.4, base[2] + 0.4 * (k - 1)],
                 15.0 - 1.4 * k, 4.6 - 0.5 * k, 166 + 4 * k, -sx * (4 + 6 * k), mk(0.2 - 0.06 * k), flat=0.55,
                 curve=(10 + 4 * k, sx * (14 - 6 * k)), segs=5, root=1.05, tip=0.08, taper_pow=0.55, belly=1.2,
                 parent=f'tail{sx}', skew=0.1 * sx)


def face_paint(ctx, mode='calme'):
    fight = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 8, HAIR)
    for x0 in (10, 38):
        if fight:
            f.poly((x0, 26, x0 + 18, 22, x0 + 18, 25, x0, 29), LINE)
            f.rect(x0 + 2, 28, x0 + 16, 34, (250, 250, 252))
            f.rect(x0 + 6, 28, x0 + 12, 34, EYE)
            f.rect(x0 + 8, 29, x0 + 10, 33, (14, 20, 40))
        else:
            f.rect(x0, 23, x0 + 18, 37, (250, 250, 252))                 # grands yeux
            f.rect(x0 + 3, 23, x0 + 15, 37, EYE)
            f.rect(x0 + 3, 31, x0 + 15, 37, shade(EYE, 0.75))
            f.rect(x0 + 6, 26, x0 + 12, 34, (14, 26, 52))
            f.rect(x0 + 4, 24, x0 + 7, 27, (255, 255, 255))
            f.rect(x0 - 1, 21, x0 + 19, 23, LINE)
    f.rect(7, 39, 15, 42, rgb('#f6a8b0'))
    f.rect(51, 39, 59, 42, rgb('#f6a8b0'))
    if fight:
        f.poly((24, 45, 42, 45, 40, 50, 26, 50), rgb('#7a2a30'))
        f.rect(26, 45, 40, 46, (255, 255, 255))
    else:
        f.poly((28, 46, 38, 46, 36, 49, 30, 49), rgb('#c8505a'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.7))], fill=HAIR)


def build_body(m):
    b = BODY
    dress = cloth(DRESS, DRESS_L, shade(DRESS, 0.75), ao_bottom=0.08, folds=3, seed=250)
    white = cloth(WHITE, WHITE, shade(WHITE, 0.82), folds=4, seed=251)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], dress)
    m.cube('torso', [-b.torso_w / 2 - 0.3, y1 - 1.2, -d2 - 0.3], [b.torso_w + 0.6, 1.4, b.torso_d + 0.6], white)  # col
    m.cube('torso', [-1.4, y1 - 2.6, -d2 - 0.6], [2.8, 1.8, 0.6], flat(RED, 0.95))                           # nœud
    for sx in (-1, 1):
        m.cube('torso', [sx * 1.6 - 0.9, y1 - 2.9, -d2 - 0.5], [1.8, 2.4, 0.5], flat(shade(RED, 1.08), 0.9),
               pivot=[sx * 0.8, y1 - 1.7, -d2], rotation=[0, 0, -sx * 20])
    # jupe évasée (trois volants) — os de la jupe qui suit le bassin
    m.bone('skirt', [0, b.hip_y + 0.4, 0], 'body')
    for k, (y, w, d) in enumerate(((b.hip_y - 0.6, 6.4, 4.4), (b.hip_y - 1.9, 7.4, 5.2), (b.hip_y - 3.0, 8.2, 5.8))):
        m.cube('skirt', [-w / 2, y, -d / 2], [w, 1.5, d], dress if k < 2 else white)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.shoulder_y - 2.4, (3.0, 2.8, 3.0), white)                 # manche bouffante
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.2, b.shoulder_y - b.elbow_y - 2.0, 2.2), skin_mat(SKIN))
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (2.1, b.elbow_y - b.wrist_y, 2.1), skin_mat(SKIN))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.0, (2.1, 2.0, 2.1), skin_mat(SKIN))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.3, b.hip_y - b.knee_y + 0.2, 2.3), skin_mat(SKIN))
        m.box(f'{side}_shin', (cx, 0), 0.9, (2.4, b.knee_y - 0.9, 2.4), white)                     # chaussettes
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.5, 1.0, 3.2), cloth(RED, seed=252))              # souliers


def build():
    m = Model('biscuit', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=5, leg_out=2, feet_out=6)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=16, leg_swing=24, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
