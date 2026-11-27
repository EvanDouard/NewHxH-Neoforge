"""
Canary, servante des Zoldyck — même style. Jeune fille (≈ 1,55 bloc), peau mate ; cheveux noirs bouclés mi-courts
qui gonflent autour de la tête jusqu'aux épaules, frange en boucles ; grands yeux sombres sérieux ; tenue de
servante noire à col blanc, tablier blanc, collants, souliers ; long bâton de garde.
Expression « froid » : regard déterminé (sourcils froncés).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import curtain

SKIN = rgb('#8a5a3c')
HAIR = rgb('#141216')
HAIR_L = rgb('#383440')
LINE = rgb('#1a1012')
DRESS = rgb('#1a1a20')
WHITE = rgb('#f6f4ee')

BODY = Body(head=(8.0, 8.2, 7.6), head_y=17.6, hip_y=9.4, knee_y=4.8, shoulder_y=16.3, arm_x=3.9, leg_x=1.45,
            torso_w=5.2, torso_d=3.0)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5a5468'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(311)
    c, R = (0.0, top - 3.5, 0.3), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=34)
    rr = R + 0.3
    for i, az in enumerate(range(0, 360, 20)):                      # boucles : mèches courtes très enroulées
        if 140 < az < 220:
            continue
        crown_lock(m, f'c_m{i}', c, rr, az + rnd.uniform(-5, 5), 80 + rnd.uniform(-8, 8), 5.6 + rnd.uniform(-0.6, 0.6),
                   4.0, mk(0.1 + 0.06 * (i % 2)), flat=0.6, turn=170, tip=0.4, segs=5, twist=rnd.uniform(-14, 14),
                   side=rnd.choice((-1, 1)) * rnd.uniform(20, 45))
    # deuxième couche de boucles plus basses (autour des oreilles et de la nuque) au lieu d'un rideau
    for i, az in enumerate(range(10, 360, 22)):
        if 130 < az < 230:
            continue
        crown_lock(m, f'c_l{i}', c, rr - 0.2, az + rnd.uniform(-6, 6), 50 + rnd.uniform(-8, 6), 5.0 + rnd.uniform(-0.6, 0.8),
                   3.6, mk(0.04 + 0.06 * (i % 2)), flat=0.65, turn=180 + rnd.uniform(-20, 20), tip=0.45, segs=5,
                   twist=rnd.uniform(-20, 20), side=rnd.choice((-1, 1)) * rnd.uniform(20, 50))
    for j, az in enumerate((152, 170, 190, 208)):
        crown_lock(m, f'c_f{j}', c, rr, az, 72, 4.0, 3.6, mk(0.22), flat=0.6, turn=160, tip=0.4, segs=4)


def face_paint(ctx, mode='calme'):
    det = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 10, HAIR)
    for x0 in (11, 39):
        f.rect(x0, 25, x0 + 16, 36, (246, 244, 240))
        f.rect(x0 + 4, 25, x0 + 12, 36, rgb('#2a1c14'))
        f.rect(x0 + 6, 27, x0 + 10, 35, (8, 4, 2))
        f.rect(x0 + 5, 26, x0 + 7, 28, (255, 255, 255))
        f.rect(x0 - 1, 23, x0 + 17, 25, LINE)
        if det:
            f.poly((x0, 16 if x0 > 30 else 20, x0 + 16, 20 if x0 > 30 else 16, x0 + 16, 22 if x0 > 30 else 18,
                    x0, 18 if x0 > 30 else 22), LINE)
    f.rect(29, 48, 37, 48, rgb('#5a2a28'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def build_body(m):
    b = BODY
    dress = cloth(DRESS, shade(DRESS, 1.5), shade(DRESS, 0.7), ao_bottom=0.08, folds=3, seed=710)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], dress)
    m.cube('torso', [-b.torso_w / 2 - 0.2, y1 - 1.0, -d2 - 0.2], [b.torso_w + 0.4, 1.2, b.torso_d + 0.4], cloth(WHITE, seed=711))
    m.cube('torso', [-1.8, y0, -d2 - 0.2], [3.6, y1 - y0 - 1.6, 0.3], cloth(WHITE, seed=712))
    m.bone('skirt', [0, b.hip_y + 0.4, 0], 'body')
    m.cube('skirt', [-b.torso_w / 2 - 0.6, b.knee_y + 0.4, -d2 - 0.6], [b.torso_w + 1.2, b.hip_y - b.knee_y, b.torso_d + 1.2], dress)
    m.cube('skirt', [-2.2, b.knee_y + 0.8, -d2 - 0.75], [4.4, b.hip_y - b.knee_y - 0.6, 0.2], cloth(WHITE, seed=713))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.3, b.shoulder_y - b.elbow_y + 0.6, 2.4), dress)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.1, (2.2, b.elbow_y - b.wrist_y - 0.1, 2.3), dress)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (2.4, 0.6, 2.5), cloth(WHITE, seed=714))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.0, (2.0, 2.0, 2.0), skin_mat(SKIN))
    cx = -b.arm_x                                                      # long bâton
    m.cube('right_hand', [cx - 0.4, b.wrist_y - 14, -1.4], [0.8, 26, 0.8], cloth(rgb('#5a3a22'), seed=715))
    tights = cloth(rgb('#1e1e24'), seed=716)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.2, b.hip_y - b.knee_y + 0.2, 2.2), tights)
        m.box(f'{side}_shin', (cx, 0), 0.8, (2.1, b.knee_y - 0.8, 2.1), tights)
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.3, 0.9, 3.0), flat(rgb('#0e0e10'), 0.95))


def build():
    m = Model('canary', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=4)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=12, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
