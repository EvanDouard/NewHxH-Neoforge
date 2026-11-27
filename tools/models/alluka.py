"""
Alluka Zoldyck — même style. Enfant menue (≈ 1,45 bloc), tête plus grosse ; longs cheveux noirs raides qui
tombent jusqu'au bas du dos, frange droite épaisse, mèches qui encadrent le visage ; grands yeux noirs doux,
sourire ; kimono court rose clair à motifs de fleurs, obi rouge, chaussettes blanches et sandales.
Expression « froid » : Nanika (yeux et bouche en trous noirs sans fond).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import curtain

SKIN = rgb('#f6e2d6')
HAIR = rgb('#141218')
HAIR_L = rgb('#34323e')
LINE = rgb('#1a1418')
KIMONO = rgb('#f6c6d6')
KIMONO_L = rgb('#fde0ea')
OBI = rgb('#d03040')

BODY = Body(head=(8.2, 8.2, 7.8), head_y=14.4, hip_y=7.2, knee_y=3.6, shoulder_y=13.2, arm_x=3.8, leg_x=1.35,
            torso_w=5.0, torso_d=3.0)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#565268'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(313)
    c, R = (0.0, top - 3.6, 0.3), 4.8
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=36)
    rr = R + 0.25
    for i, az in enumerate(range(0, 360, 20)):
        if 136 < az < 224:
            continue
        crown_lock(m, f'a_m{i}', c, rr, az, 96, 9.8, 3.8, mk(0.14 + 0.06 * math.cos(math.radians(az - 140))),
                   flat=0.3, turn=96, tip=0.85, segs=4, taper_pow=0.15)
    curtain(m, 'a_c', c, rr - 0.4, range(-84, 85, 12), 1.6, lambda az: 13.5 - 2.5 * abs(az) / 84, 3.8,
            lambda i: mk(0.06 + 0.04 * (i % 2)), inward=10, rnd=rnd, vary=0.18, layer=0.5, tip=0.7, segs=4, belly=1.02)
    for sx in (-1, 1):
        curtain(m, f'a_s{sx}', c, rr - 0.2, [sx * 100, sx * 114, sx * 128], 1.0, 8.6, 3.4, lambda i: mk(0.12),
                inward=0, rnd=rnd, tip=0.75, segs=4)
    for j, az in enumerate((144, 158, 172, 188, 202, 216)):
        crown_lock(m, f'a_f{j}', c, rr + 0.05, az, 92, 6.8, 3.4, mk(0.24), flat=0.28, turn=74, tip=0.95, segs=3,
                   taper_pow=0.1)


def face_paint(ctx, mode='calme'):
    nanika = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 18, HAIR)
    for x0 in (10, 38):
        if nanika:
            f.rect(x0 + 2, 24, x0 + 16, 38, (4, 4, 6))                  # trous noirs sans fond
        else:
            f.rect(x0, 23, x0 + 18, 37, (250, 248, 248))
            f.rect(x0 + 4, 23, x0 + 14, 37, rgb('#2a2230'))
            f.rect(x0 + 6, 26, x0 + 12, 35, (8, 6, 10))
            f.rect(x0 + 5, 24, x0 + 8, 27, (255, 255, 255))
            f.rect(x0 - 1, 21, x0 + 19, 23, LINE)
    if nanika:
        f.rect(26, 44, 40, 54, (4, 4, 6))
    else:
        f.rect(7, 39, 14, 42, rgb('#f6a8b0'))
        f.rect(52, 39, 59, 42, rgb('#f6a8b0'))
        f.poly((28, 46, 38, 46, 36, 49, 30, 49), rgb('#c0606a'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def kimono_paint(ctx):
    cloth(KIMONO, KIMONO_L, shade(KIMONO, 0.78), ao_bottom=0.08, folds=3, seed=720)(ctx)
    f = FaceGrid(ctx)
    rnd = random.Random(ctx.w * 5 + ctx.h)
    for _ in range(4):
        x, y = rnd.uniform(6, 56), rnd.uniform(6, 50)
        f.rect(x, y + 2, x + 6, y + 4, rgb('#e86a8a'))
        f.rect(x + 2, y, x + 4, y + 6, rgb('#e86a8a'))
    if ctx.face == 'north':
        f.poly((20, 0, 27, 0, 40, 40, 34, 46), shade(KIMONO, 0.85))
        f.poly((27, 0, 39, 0, 33, 20), SKIN)


def build_body(m):
    b = BODY
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], kimono_paint)
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 - 0.2, -d2 - 0.15], [b.torso_w + 0.3, 1.8, b.torso_d + 0.3], cloth(OBI, seed=721))
    m.cube('torso', [-1.4, y0 - 0.6, d2 + 0.1], [2.8, 2.6, 1.0], cloth(shade(OBI, 0.9), seed=722))
    m.bone('kimono_b', [0, y0, 0], 'body')
    m.cube('kimono_b', [-b.torso_w / 2 - 0.25, b.knee_y - 0.2, -d2 - 0.25], [b.torso_w + 0.5, y0 - b.knee_y + 0.2, b.torso_d + 0.5], kimono_paint)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y - 0.2, (2.6, b.shoulder_y - b.elbow_y + 0.8, 2.6), kimono_paint)
        m.box(f'{side}_forearm', (cx, 0.3), b.wrist_y + 0.6, (3.0, b.elbow_y - b.wrist_y - 0.4, 3.6), kimono_paint)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 1.8, (2.0, 2.2, 2.0), skin_mat(SKIN))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.1, b.hip_y - b.knee_y + 0.2, 2.1), skin_mat(SKIN))
        m.box(f'{side}_shin', (cx, 0), 0.6, (2.1, b.knee_y - 0.6, 2.1), cloth(rgb('#f4f2ee'), seed=723))
        m.box(f'{side}_shin', (cx, -0.2), 0.0, (2.4, 0.6, 3.0), flat(rgb('#8a5a3a'), 0.9))


def build():
    m = Model('alluka', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=4, leg_out=1, feet_out=6)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=20, breath=0.8)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
