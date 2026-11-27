"""
Neon Nostrad — même style. Menue (≈ 1,6 bloc) ; longs cheveux rose saumon qui tombent jusqu'au bas du dos en
mèches souples, frange épaisse en pointes sur le front, deux longues mèches qui encadrent le visage ; grands yeux
bleus brillants, joues roses, air capricieux ; robe blanche courte à gros nœud rose, collants, ballerines.
Expression « froid » : moue boudeuse (sourcils froncés, joues gonflées).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import curtain

SKIN = rgb('#fbe3d4')
HAIR = rgb('#e07a7a')
HAIR_L = rgb('#ffb8a8')
LINE = rgb('#4a2228')
EYE = rgb('#4aa0e8')
DRESS = rgb('#f6f2f0')
BOW = rgb('#f07aa8')

BODY = Body(head=(8.0, 8.2, 7.6), head_y=17.6, hip_y=9.4, knee_y=4.8, shoulder_y=16.3, arm_x=3.9, leg_x=1.45,
            torso_w=5.2, torso_d=3.0)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#ffe0d8'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(193)
    c, R = (0.0, top - 3.6, 0.3), 4.7
    scalp(m, c, R, lambda k: mk(k)(0.5), skip_front=46, front_phi=34)
    rr = R + 0.25
    for i, az in enumerate(range(0, 360, 22)):
        if 140 < az < 220:
            continue
        crown_lock(m, f'n_m{i}', c, rr, az + rnd.uniform(-3, 3), 94, 8.6, 4.2,
                   mk(0.16 + 0.06 * math.cos(math.radians(az - 150))), flat=0.4, turn=98, tip=0.3, segs=4,
                   twist=rnd.uniform(-5, 5))
    curtain(m, 'n_c', c, rr - 0.5, range(-96, 97, 12), 1.6, lambda az: 15.5 - 3.0 * abs(az) / 96, 4.0,
            lambda i: mk(0.08 + 0.05 * (i % 2)), inward=14, flare=0.15, wave=8, rnd=rnd, vary=0.2, layer=0.55, tip=0.15, belly=1.12)
    for sx in (-1, 1):
        curtain(m, f'n_s{sx}', c, rr - 0.1, [sx * 112, sx * 124], 1.2, 10.5, 3.4, lambda i: mk(0.14), inward=0,
                rnd=rnd, tip=0.12, belly=1.1)
    for j, (az, L, lean) in enumerate(((146, 5.0, -12), (160, 5.6, -5), (174, 6.0, 0), (188, 6.0, 3), (202, 5.6, 7),
                                       (216, 5.0, 12))):
        crown_lock(m, f'n_f{j}', c, rr, az, 70, L, 3.4, mk(0.34), flat=0.42, turn=80, tip=0.12, segs=3, lean=lean,
                   skew=rnd.uniform(-0.1, 0.1))


def face_paint(ctx, mode='calme'):
    pout = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 10, HAIR)
    for x0 in (10, 38):
        f.rect(x0, 23, x0 + 18, 37, (250, 250, 252))
        f.rect(x0 + 3, 23, x0 + 15, 37, EYE)
        f.rect(x0 + 3, 31, x0 + 15, 37, shade(EYE, 0.75))
        f.rect(x0 + 6, 26, x0 + 12, 34, (14, 30, 60))
        f.rect(x0 + 4, 24, x0 + 7, 27, (255, 255, 255))
        f.rect(x0 - 1, 21, x0 + 19, 23, LINE)
        if pout:
            f.poly((x0, 16 if x0 > 30 else 19, x0 + 18, 19 if x0 > 30 else 16, x0 + 18, 21 if x0 > 30 else 18,
                    x0, 18 if x0 > 30 else 21), LINE)
    f.rect(6, 39, 14, 42, rgb('#f6a0a8'))
    f.rect(52, 39, 60, 42, rgb('#f6a0a8'))
    if pout:
        f.poly((29, 48, 37, 48, 35, 46, 31, 46), rgb('#c0505a'))
    else:
        f.poly((28, 46, 38, 46, 36, 49, 30, 49), rgb('#d0606a'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def build_body(m):
    b = BODY
    dress = cloth(DRESS, DRESS, shade(DRESS, 0.82), ao_bottom=0.06, folds=3, seed=430)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], dress)
    m.cube('torso', [-1.0, y1 - 0.2, -1.0], [2.0, 0.6, 2.0], skin_mat(shade(SKIN, 0.92)))
    m.cube('torso', [-1.4, y1 - 2.4, -d2 - 0.6], [2.8, 1.8, 0.6], flat(BOW, 0.95))
    for sx in (-1, 1):
        m.cube('torso', [sx * 1.7 - 1.0, y1 - 2.8, -d2 - 0.5], [2.0, 2.6, 0.5], flat(shade(BOW, 1.08), 0.9),
               pivot=[sx * 0.9, y1 - 1.5, -d2], rotation=[0, 0, -sx * 22])
    m.bone('skirt', [0, b.hip_y + 0.4, 0], 'body')
    for k, (y, w, d) in enumerate(((b.hip_y - 0.8, 6.4, 4.2), (b.hip_y - 2.2, 7.4, 5.0))):
        m.cube('skirt', [-w / 2, y, -d / 2], [w, 1.8, d], dress)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.shoulder_y - 2.4, (2.8, 2.6, 2.8), dress)
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.1, b.shoulder_y - b.elbow_y - 2.0, 2.1), skin_mat(SKIN))
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (2.0, b.elbow_y - b.wrist_y, 2.0), skin_mat(SKIN))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 1.9, (2.0, 1.9, 2.0), skin_mat(SKIN))
    tights = cloth(rgb('#f2ece8'), seed=431)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.2, b.hip_y - b.knee_y + 0.2, 2.2), tights)
        m.box(f'{side}_shin', (cx, 0), 0.7, (2.1, b.knee_y - 0.7, 2.1), tights)
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.3, 0.8, 3.0), flat(BOW, 0.9))


def build():
    m = Model('neon', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=4)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
