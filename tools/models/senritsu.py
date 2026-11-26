"""
Senritsu — même style. Petite (≈ 1,4 bloc), tête ronde un peu grosse ; cheveux noirs courts et ondulés en casque
arrondi (mèches qui suivent le crâne et se recourbent sur la nuque et les joues), petite frange ; grands yeux ronds
saillants, grande bouche aux lèvres épaisses, sourire doux ; costume noir de musicienne (veste, nœud papillon
blanc), pantalon, chaussures vernies ; flûte traversière dans la main.
Expression « froid » : écoute attentive (yeux plissés, bouche serrée).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import curtain

SKIN = rgb('#e6c2a4')
HAIR = rgb('#18161c')
HAIR_L = rgb('#3a3848')
LINE = rgb('#24181a')
SUIT = rgb('#1a1a22')
SUIT_L = rgb('#2c2c38')

BODY = Body(head=(8.4, 8.4, 8.0), head_y=13.6, hip_y=6.8, knee_y=3.4, shoulder_y=12.4, arm_x=3.8, leg_x=1.35,
            torso_w=5.2, torso_d=3.2)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5a586e'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(227)
    c, R = (0.0, top - 3.3, 0.35), 5.25
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=36)
    rr = R + 0.25
    for i, az in enumerate(range(-150, 151, 20)):                # casque ondulé : mèches qui se recourbent en dedans
        crown_lock(m, f's_m{i}', c, rr, az + rnd.uniform(-3, 3), 88, 8.4 + rnd.uniform(-0.4, 0.5), 4.2,
                   mk(0.14 + 0.06 * math.cos(math.radians(az))), flat=0.45, turn=140, tip=0.35, segs=5,
                   twist=rnd.uniform(-8, 8))
    # bas du casque : mèches qui couvrent les côtés et la nuque jusqu'à la mâchoire et se recourbent en dedans
    curtain(m, 's_c', c, rr - 0.45, range(-118, 119, 14), 1.0, 5.4, 3.8, lambda i: mk(0.06 + 0.05 * (i % 2)),
            inward=0, flip=-16, rnd=rnd, tip=0.3, segs=4, belly=1.1)
    for j, az in enumerate((150, 168, 192, 210)):
        crown_lock(m, f's_f{j}', c, rr, az, 76, 4.6, 3.6, mk(0.28), flat=0.45, turn=110, tip=0.3, segs=3)


def face_paint(ctx, mode='calme'):
    listen = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 12, HAIR)
    for x0 in (9, 37):
        if listen:
            f.rect(x0 + 1, 28, x0 + 19, 31, (246, 244, 240))
            f.rect(x0 + 7, 28, x0 + 12, 31, (14, 10, 8))
            f.rect(x0, 26, x0 + 20, 28, LINE)
        else:
            f.rect(x0, 21, x0 + 20, 37, (248, 246, 242))         # grands yeux ronds saillants
            f.rect(x0 + 7, 25, x0 + 13, 33, (14, 10, 8))
            f.rect(x0 + 8, 26, x0 + 10, 28, (255, 255, 255))
            f.rect(x0 - 1, 19, x0 + 21, 21, LINE)
    if listen:
        f.rect(22, 47, 44, 50, rgb('#8a4a44'))
    else:
        f.poly((18, 45, 48, 45, 44, 52, 22, 52), rgb('#9a4e48'))  # grande bouche aux lèvres épaisses
        f.rect(22, 47, 44, 49, rgb('#5a2424'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def suit_paint(ctx):
    cloth(SUIT, SUIT_L, shade(SUIT, 0.7), ao_bottom=0.1, folds=2, seed=480)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.poly((24, 0, 42, 0, 36, 26, 30, 26), rgb('#f2f0ea'))
        f.rect(26, 3, 40, 9, rgb('#ffffff'))                      # nœud papillon
        f.rect(31, 2, 35, 10, rgb('#d8d6d0'))


def build_body(m):
    b = BODY
    suit = cloth(SUIT, SUIT_L, shade(SUIT, 0.7), folds=2, seed=481)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.6, -d2], [b.torso_w, y1 - y0 + 0.6, b.torso_d], suit_paint)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.4, b.shoulder_y - b.elbow_y + 0.6, 2.5), suit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.1, (2.3, b.elbow_y - b.wrist_y - 0.1, 2.4), suit)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.0, (2.0, 2.0, 2.0), skin_mat(SKIN))
    cx = -b.arm_x                                                 # flûte traversière
    m.cube('right_hand', [cx - 0.35, b.wrist_y - 1.4, -4.0], [0.7, 0.7, 7.0], flat(rgb('#d8dce4'), 1.0))
    for k in range(4):
        m.cube('right_hand', [cx - 0.2, b.wrist_y - 0.75, -3.4 + k * 1.2], [0.4, 0.15, 0.4], flat(rgb('#8a8e96'), 0.9))
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.1, -1.55], [b.torso_w - 0.6, 1.3, 3.1], suit)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.3, b.hip_y - b.knee_y + 0.2, 2.4), suit)
        m.box(f'{side}_shin', (cx, 0), 0.8, (2.2, b.knee_y - 0.8, 2.3), suit)
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.3, 0.9, 3.1), flat(rgb('#0c0c10'), 0.95))


def build():
    m = Model('senritsu', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=5, leg_out=2, feet_out=6)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
