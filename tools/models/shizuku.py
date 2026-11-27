"""
Shizuku Murasaki — même style. Mince (≈ 1,65 bloc) ; cheveux noirs en carré court lisse coupé au menton, frange
balayée sur un côté ; grandes lunettes à monture noire, yeux mi-clos, air absent ; pull noir moulant à col roulé,
pantalon noir, bottines.
Expression « froid » : regard vide (yeux noirs sans reflet).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade
from hairkit import curtain, glasses

SKIN = rgb('#f4e0d2')
HAIR = rgb('#141218')
HAIR_L = rgb('#34323e')
LINE = rgb('#1a1418')
BLACK = rgb('#18181c')

BODY = Body(head=(7.8, 8.2, 7.6), head_y=19.0, hip_y=10.2, knee_y=5.2, shoulder_y=17.6, arm_x=4.1, leg_x=1.55,
            torso_w=5.6, torso_d=3.1)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#565268'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(401)
    c, R = (0.0, top - 3.7, 0.3), 4.4
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=36)
    rr = R + 0.25
    for i, az in enumerate(range(0, 360, 20)):
        if 136 < az < 224:
            continue
        crown_lock(m, f's_m{i}', c, rr, az + rnd.uniform(-4, 4), 70 + rnd.uniform(-2, 2), 11.6 + rnd.uniform(-0.3, 0.3),
                   3.6 + rnd.uniform(-0.4, 0.8), mk(0.14 + 0.06 * math.cos(math.radians(az - 140))),
                   flat=0.3, turn=78 + rnd.uniform(-3, 3), tip=0.9, segs=5, taper_pow=0.12,
                   twist=rnd.uniform(-3, 3))
    for j, (az, L, lean) in enumerate(((146, 4.6, -6), (163, 4.6, -2), (180, 4.6, 0), (197, 4.6, 2), (214, 4.6, 6))):
        crown_lock(m, f's_f{j}', c, rr + 0.05, az, 74, L + 1.6, 4.0, mk(0.24), flat=0.28, turn=80, tip=0.95, segs=3, lean=lean)


def face_paint(ctx, mode='calme'):
    blank = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 14, HAIR)
    for x0 in (12, 40):
        f.rect(x0 + 1, 28, x0 + 13, 33, (248, 246, 244))
        f.rect(x0 + 4, 28, x0 + 10, 33, (10, 8, 12) if blank else rgb('#2a2232'))
        if not blank:
            f.rect(x0 + 5, 29, x0 + 6, 30, (255, 255, 255))
        f.rect(x0, 26, x0 + 14, 28, LINE)
    f.rect(30, 48, 36, 48, rgb('#b07070'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def build_body(m):
    b = BODY
    knit = cloth(BLACK, shade(BLACK, 1.5), shade(BLACK, 0.7), folds=5, seed=750)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.4, -d2], [b.torso_w, y1 - y0 + 0.4, b.torso_d], knit)
    m.cube('torso', [-1.6, y1 - 0.4, -1.6], [3.2, 1.6, 3.2], knit)                    # col roulé
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.3, b.shoulder_y - b.elbow_y + 0.6, 2.4), knit)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (2.2, b.elbow_y - b.wrist_y, 2.3), knit)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.0, (2.0, 2.0, 2.0), skin_mat(SKIN))
    pants = cloth(rgb('#202026'), rgb('#30303a'), rgb('#121216'), folds=2, seed=751)
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.2, -1.5], [b.torso_w - 0.6, 1.4, 3.0], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.5, b.hip_y - b.knee_y + 0.2, 2.6), pants)
        m.box(f'{side}_shin', (cx, 0), 1.8, (2.4, b.knee_y - 1.8, 2.5), pants)
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.6, 2.2, 3.3), cloth(rgb('#101012'), seed=752))


def build():
    m = Model('shizuku', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    glasses(m, BODY, rgb('#141418'), rgb('#e4eef2'), gy=30.5, w=2.7, h=2.2)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=12, leg_swing=20, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
