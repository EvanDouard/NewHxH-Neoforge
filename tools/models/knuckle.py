"""
Knuckle Bine — même style. Grand et costaud (≈ 1,9 bloc) ; cheveux noirs gominés en grande banane (pompadour) :
houppe épaisse qui s'avance au-dessus du front et s'enroule vers l'arrière, côtés plaqués, longs favoris ;
sourcils épais froncés, yeux sombres, air de voyou au grand cœur ; veste grise ouverte sur un t-shirt blanc,
pantalon sombre, chaussures.
Expression « froid » : colère (dents serrées, sourcils très froncés).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#e2bc9a')
HAIR = rgb('#121216')
HAIR_L = rgb('#3a4058')
LINE = rgb('#22181a')
JACKET = rgb('#6a6a72')
JACKET_L = rgb('#86868e')
TEE = rgb('#f0eee8')

BODY = Body(head=(8.0, 8.4, 7.8), head_y=22.6, hip_y=12.2, knee_y=6.2, shoulder_y=21.0, arm_x=5.4, leg_x=2.0,
            torso_w=8.2, torso_d=4.4)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#6a7090'))


def build_hair(m):
    b = BODY
    top, hz = b.top, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(157)
    c, R = (0.0, top - 3.5, 0.4), 4.7
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=50, front_phi=40)
    rr = R + 0.2
    # côtés et arrière plaqués (gominés) vers l'arrière
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 62, 120, 7.6), (sx * 84, 104, 6.8), (sx * 106, 90, 5.8))):
            crown_lock(m, f'k_s{sx}{j}', c, rr, az, phi, L, 3.4, mk(0.12), flat=0.3, turn=82, tip=0.4, segs=3)
    for i, az in enumerate((-40, -14, 14, 40)):
        crown_lock(m, f'k_b{i}', c, rr, az, 70, 6.4, 3.8, mk(0.06), flat=0.3, turn=50, tip=0.4, segs=3)
    # banane : grosses mèches qui partent du dessus, s'avancent au-dessus du front et s'enroulent vers l'arrière
    for i, (x, L, W, k) in enumerate(((-2.2, 8.4, 3.2, 0.2), (-0.8, 9.6, 3.6, 0.3), (0.8, 9.8, 3.6, 0.34),
                                      (2.2, 8.6, 3.2, 0.24), (0.0, 10.4, 3.4, 0.4))):
        lock(m, f'k_q{i}', [x, top - 0.8, -0.6], L * 1.05, W * 1.1, -40 + rnd.uniform(-4, 4), -x * 4, mk(k), flat=0.75,
             curve=(150, x * 6), segs=5, root=1.15, tip=0.3, taper_pow=0.4, belly=1.25,
             skew=rnd.uniform(-0.08, 0.08))
    for sx in (-1, 1):                                            # longs favoris
        lock(m, f'k_fav{sx}', [sx * 4.05, b.head_y + 5.2, -0.6], 3.4, 1.6, 180, 0, mk(0.1), flat=0.4, segs=2,
             root=1.0, tip=0.6, taper_pow=0.4, parent='head')


def face_paint(ctx, mode='calme'):
    angry = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 5, HAIR)
    f.poly((8, 18 if angry else 20, 28, 23, 28, 26, 8, 23), LINE)
    f.poly((58, 18 if angry else 20, 38, 23, 38, 26, 58, 23), LINE)
    for x0 in (12, 40):
        f.rect(x0, 27, x0 + 14, 32, (242, 238, 230))
        f.rect(x0 + 5, 27, x0 + 10, 32, rgb('#2a2018'))
        f.rect(x0 + 6, 28, x0 + 8, 31, (8, 6, 4))
    f.rect(31, 34, 34, 42, shade(SKIN, 0.82))
    if angry:
        f.rect(20, 46, 46, 52, (240, 236, 228))
        f.rect(20, 49, 46, 49, LINE)
        f.rect(20, 45, 46, 46, LINE)
        f.rect(20, 52, 46, 53, LINE)
    else:
        f.rect(25, 49, 41, 50, rgb('#6a3e34'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.55))], fill=HAIR)


def jacket_paint(ctx):
    cloth(JACKET, JACKET_L, shade(JACKET, 0.7), ao_bottom=0.1, folds=3, seed=360)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.rect(22, 0, 44, 60, TEE)


def build_body(m):
    b = BODY
    jacket = cloth(JACKET, JACKET_L, shade(JACKET, 0.7), folds=3, seed=361)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.8, -d2], [b.torso_w, y1 - y0 + 0.8, b.torso_d], jacket_paint)
    m.cube('torso', [-1.5, y1 - 0.2, -1.5], [3.0, 0.7, 3.0], skin_mat(shade(SKIN, 0.92)))
    for sx in (-1, 1):
        m.cube('torso', [sx * 2.4 - 1.0, y1 - 4.0, -d2 - 0.3], [2.0, 4.2, 0.4], jacket,
               pivot=[sx * 2.4, y1, -d2], rotation=[0, 0, -sx * 12])
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (3.6, b.shoulder_y - b.elbow_y + 0.6, 3.7), jacket)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (3.5, b.elbow_y - b.wrist_y - 0.2, 3.6), jacket)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.8, (3.0, 3.0, 3.0), skin_mat(SKIN))
    pants = cloth(rgb('#24242c'), rgb('#363640'), rgb('#141418'), folds=2, seed=362)
    m.cube('body', [-b.torso_w / 2 + 0.5, b.hip_y - 1.3, -2.0], [b.torso_w - 1.0, 1.5, 4.0], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (3.6, b.hip_y - b.knee_y + 0.2, 3.7), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (3.5, b.knee_y - 1.0, 3.6), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.6, 1.2, 4.6), cloth(rgb('#2a1e18'), seed=363))


def build():
    m = Model('knuckle', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=4)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=20, breath=1.2)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
