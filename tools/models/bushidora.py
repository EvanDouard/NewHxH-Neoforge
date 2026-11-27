"""
Bushidora Ambitious, Chasseur de la liste noire (Seirin) — même style. Grand et droit (≈ 1,85 bloc) ; longs
cheveux noirs rassemblés en queue haute de samouraï, mèches qui encadrent le visage ; bouc ; regard dur ; haori
noir sur kimono gris, hakama sombre, sabre au côté.
Apparence reconstituée faute de référence précise : à vérifier.
Expression « froid » : regard de juge (sourcils froncés).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#e6c2a2')
HAIR = rgb('#141418')
HAIR_L = rgb('#383c4a')
LINE = rgb('#22181a')
HAORI = rgb('#1a1a20')
KIMONO = rgb('#7a7a82')

BODY = Body(head=(7.8, 8.4, 7.6), head_y=21.6, hip_y=11.8, knee_y=6.0, shoulder_y=20.0, arm_x=4.9, leg_x=1.9,
            torso_w=7.2, torso_d=4.0)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5a6074'))


def build_hair(m):
    b = BODY
    top, hz = b.top, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    c, R = (0.0, top - 3.6, 0.3), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=44, front_phi=40)
    rr = R + 0.22
    for i, x_az in enumerate((-28, -14, 0, 14, 28)):
        crown_lock(m, f'b_p{i}', c, rr, x_az, 140, 6.6, 3.6, mk(0.2), flat=0.3, turn=64, tip=0.5, segs=4)
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 56, 132, 6.4), (sx * 80, 118, 6.0), (sx * 104, 104, 5.4))):
            crown_lock(m, f'b_s{sx}{j}', c, rr - 0.05 * j, az, phi, L, 3.4, mk(0.1), flat=0.32, turn=70, tip=0.5, segs=3)
        lock(m, f'b_t{sx}', [sx * 3.7, b.head_y + 6.2, -hz + 0.4], 6.0, 1.6, 180, -sx * 6, mk(0.18), flat=0.5,
             curve=(-8, sx * 8), segs=3, root=1.1, tip=0.15, taper_pow=0.6, parent='head')
    kx, ky, kz = 0.0, top + 1.0, 1.4
    m.bone('knot', [kx, ky, kz], 'hair')
    m.cube('knot', [kx - 1.3, ky - 1.4, kz - 1.3], [2.6, 3.0, 2.6], mk(0.22)(0.5))
    m.cube('knot', [kx - 1.0, ky - 1.6, kz - 1.0], [2.0, 0.5, 2.0], flat(rgb('#a03030'), 0.9))
    for k, (dx, L, W, rz) in enumerate(((0, 14.0, 3.8, 0), (-0.8, 12.0, 3.0, 12), (0.8, 12.5, 3.0, -12))):
        lock(m, f'b_tail{k}', [kx + dx, ky + 0.2, kz + 1.0], L, W, 145, rz, mk(0.22 - 0.06 * k), flat=0.7,
             curve=(30, rz * 0.6), segs=5, root=1.1, tip=0.08, taper_pow=0.6, parent='knot')


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 6, HAIR)
    f.poly((10, 19 if cold else 21, 28, 22, 28, 25, 10, 24), LINE)
    f.poly((56, 19 if cold else 21, 38, 22, 38, 25, 56, 24), LINE)
    for x0 in (12, 40):
        f.rect(x0, 27, x0 + 14, 31, (240, 236, 228))
        f.rect(x0 + 5, 27, x0 + 10, 31, rgb('#2a2018'))
    f.rect(32, 34, 33, 41, shade(SKIN, 0.82))
    f.rect(27, 48, 39, 48, rgb('#6a3e34'))
    f.poly((30, 54, 36, 54, 34, 60, 32, 60), HAIR)


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.55))], fill=HAIR)


def build_body(m):
    b = BODY
    haori = cloth(HAORI, shade(HAORI, 1.5), shade(HAORI, 0.7), folds=3, seed=780)
    kim = cloth(KIMONO, shade(KIMONO, 1.1), shade(KIMONO, 0.75), folds=3, seed=781)
    hakama = cloth(rgb('#2a2a34'), rgb('#3c3c48'), rgb('#18181e'), ao_top=0.1, folds=5, seed=782)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2 + 0.6, y0, -d2 + 0.2], [b.torso_w - 1.2, y1 - y0, b.torso_d - 0.4], kim)
    for sx in (-1, 1):
        x0 = sx * b.torso_w / 2 - (2.6 if sx > 0 else 0)
        m.cube('torso', [x0, y0 - 2.0, -d2 - 0.3], [2.6, y1 - y0 + 2.0, b.torso_d + 0.6], haori)
    m.cube('torso', [-b.torso_w / 2, y0 - 2.0, d2 - 0.3], [b.torso_w, y1 - y0 + 2.0, 0.6], haori)
    m.cube('torso', [-b.torso_w / 2 - 0.1, y0 - 0.4, -d2 - 0.1], [b.torso_w + 0.2, 1.6, b.torso_d + 0.2], cloth(rgb('#5a2a2a'), seed=783))
    m.bone('katana', [b.torso_w / 2 - 0.6, y0 + 0.4, -d2 - 0.4], 'torso', rotation=[0, 0, 62])
    kx, ky, kz = b.torso_w / 2 - 0.6, y0 + 0.4, -d2 - 0.4
    m.cube('katana', [kx - 0.45, ky - 9.0, kz - 0.45], [0.9, 9.0, 0.9], flat(rgb('#141418'), 0.9))
    m.cube('katana', [kx - 1.0, ky, kz - 1.0], [2.0, 0.4, 2.0], flat(rgb('#8a7a4a'), 0.95))
    m.cube('katana', [kx - 0.4, ky + 0.4, kz - 0.4], [0.8, 3.6, 0.8], cloth(rgb('#5a2a2a'), rgb('#7a3a3a'), folds=5, seed=784))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y - 0.4, (3.4, b.shoulder_y - b.elbow_y + 1.0, 3.4), haori)
        m.box(f'{side}_forearm', (cx, 0.2), b.wrist_y + 0.6, (3.8, b.elbow_y - b.wrist_y - 0.2, 4.2), haori)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.2, (2.5, 2.8, 2.5), skin_mat(SKIN))
    m.cube('body', [-b.torso_w / 2, b.hip_y - 1.6, -d2 - 0.1], [b.torso_w, 1.8, b.torso_d + 0.2], hakama)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.3, (3.8, b.hip_y - b.knee_y + 0.3, 4.2), hakama)
        m.box(f'{side}_shin', (cx, 0), 1.0, (4.0, b.knee_y - 1.0, 4.4), hakama)
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.8, 1.0, 3.8), flat(rgb('#2a2220'), 0.9))


def build():
    m = Model('bushidora', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=4, feet_out=8)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=20, breath=1.2)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
