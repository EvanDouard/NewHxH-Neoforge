"""
Nobunaga Hazama — même style. Grand samouraï (≈ 1,85 bloc) ; cheveux noirs longs rassemblés en haut du crâne en
un chignon d'où retombe une queue épaisse jusqu'aux omoplates, mèches libres sur les tempes ; barbe de quelques
jours, yeux sombres mi-clos ; kimono jaune pâle croisé, hakama gris-brun, ceinture bleue où passe le katana,
sandales.
Expression « froid » : regard de duel (yeux grands ouverts, sourcils froncés).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#e8c4a2')
HAIR = rgb('#141418')
HAIR_L = rgb('#383c4a')
LINE = rgb('#22181a')
KIMONO = rgb('#e6dcb0')
KIMONO_SH = rgb('#b8ac80')
HAKAMA = rgb('#6a6258')
OBI = rgb('#3a5a8a')

BODY = Body(head=(7.8, 8.4, 7.6), head_y=21.6, hip_y=11.8, knee_y=6.0, shoulder_y=20.0, arm_x=4.9, leg_x=1.9,
            torso_w=7.2, torso_d=4.0)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5a6074'))


def build_hair(m):
    b = BODY
    top, hz = b.top, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(97)
    c, R = (0.0, top - 3.6, 0.3), 4.5
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=44, front_phi=40)
    rr = R + 0.22
    # cheveux tirés vers le chignon : mèches plaquées du front et des tempes vers le sommet arrière
    for i, x_az in enumerate((-28, -14, 0, 14, 28)):
        crown_lock(m, f'n_p{i}', c, rr, x_az + rnd.uniform(-2, 2), 140, 6.6, 3.6, mk(0.2 + 0.03 * (2 - abs(i - 2))),
                   flat=0.3, turn=64, tip=0.5, segs=4, taper_pow=0.35, twist=rnd.uniform(-4, 4))
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 56, 132, 6.4), (sx * 80, 118, 6.0), (sx * 104, 104, 5.4))):
            crown_lock(m, f'n_s{sx}{j}', c, rr - 0.05 * j, az, phi, L, 3.4, mk(0.12 if sx > 0 else 0.04), flat=0.32,
                       turn=70, tip=0.5, segs=3, lean=sx * 4)
    # chignon au sommet arrière et queue épaisse qui retombe dans le dos
    kx, ky, kz = 0.0, top + 1.0, 1.4
    m.bone('knot', [kx, ky, kz], 'hair')
    m.cube('knot', [kx - 1.4, ky - 1.4, kz - 1.4], [2.8, 3.0, 2.8], mk(0.22)(0.5))
    m.cube('knot', [kx - 1.0, ky - 1.1, kz - 1.0], [2.0, 0.5, 2.0], flat(OBI, 0.9))                     # lien
    for k, (dx, L, W, rz, cz) in enumerate(((0, 12.0, 3.8, 0, 0), (-0.8, 10.0, 3.0, 14, -10), (0.8, 10.5, 3.0, -14, 10))):
        lock(m, f'n_tail{k}', [kx + dx, ky + 0.2, kz + 1.0], L + 0.5, W, 118, rz, mk(0.22 - 0.06 * k), flat=0.7,
             curve=(66, cz), segs=5, root=1.1, tip=0.08, taper_pow=0.6, parent='knot', skew=0.06 * (k - 1))
    for sx in (-1, 1):                                            # mèches libres sur les tempes
        lock(m, f'n_t{sx}', [sx * 3.7, b.head_y + 6.2, -hz + 0.4], 4.6, 1.5, 180, -sx * 6, mk(0.18), flat=0.5,
             curve=(-8, sx * 8), segs=3, root=1.1, tip=0.15, taper_pow=0.6, parent='head')


def face_paint(ctx, mode='calme'):
    duel = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 6, HAIR)
    f.poly((10, 21, 28, 19, 28, 22, 10, 24), LINE) if not duel else f.poly((10, 18, 28, 22, 28, 25, 10, 21), LINE)
    f.poly((56, 21, 38, 19, 38, 22, 56, 24), LINE) if not duel else f.poly((56, 18, 38, 22, 38, 25, 56, 21), LINE)
    for x0 in (12, 40):
        top = 26 if duel else 28
        f.rect(x0, top, x0 + 14, 32, (240, 236, 228))
        f.rect(x0 + 5, top, x0 + 10, 32, rgb('#2a2018'))
        f.rect(x0 - 1, top - 2, x0 + 15, top, LINE)
    f.rect(32, 34, 33, 41, shade(SKIN, 0.82))
    stub = shade(SKIN, 0.8)                                       # barbe de quelques jours
    f.rect(8, 44, 58, 58, stub)
    f.rect(16, 42, 50, 44, stub)
    f.rect(26, 49, 40, 50, rgb('#6a3e34'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.55))], fill=HAIR)


def kimono_paint(ctx):
    cloth(KIMONO, shade(KIMONO, 1.05), KIMONO_SH, ao_bottom=0.08, folds=3, seed=270)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)                                          # col croisé (gauche sur droite)
        f.poly((18, 0, 26, 0, 40, 40, 34, 46), KIMONO_SH)
        f.poly((48, 0, 40, 0, 30, 26, 34, 30), shade(KIMONO_SH, 0.9))
        f.poly((26, 0, 40, 0, 33, 22), SKIN)


def build_body(m):
    b = BODY
    sleeve = cloth(KIMONO, shade(KIMONO, 1.05), KIMONO_SH, folds=4, seed=271)
    hakama = cloth(HAKAMA, shade(HAKAMA, 1.2), shade(HAKAMA, 0.7), ao_top=0.1, folds=5, seed=272)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], kimono_paint)
    m.cube('torso', [-1.3, y1 - 0.2, -1.3], [2.6, 0.6, 2.6], skin_mat(shade(SKIN, 0.92)))
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 - 0.4, -d2 - 0.15], [b.torso_w + 0.3, 2.0, b.torso_d + 0.3],
           cloth(OBI, seed=273))
    # katana passé dans la ceinture, côté gauche (fourreau noir, garde, poignée tressée)
    m.bone('katana', [b.torso_w / 2 - 0.6, y0 + 0.6, -d2 - 0.4], 'torso', rotation=[0, 0, 62])
    kx, ky, kz = b.torso_w / 2 - 0.6, y0 + 0.6, -d2 - 0.4
    m.cube('katana', [kx - 0.45, ky - 9.0, kz - 0.45], [0.9, 9.0, 0.9], flat(rgb('#1a1418'), 0.9))
    m.cube('katana', [kx - 1.0, ky, kz - 1.0], [2.0, 0.4, 2.0], flat(rgb('#8a7a4a'), 0.95))
    m.cube('katana', [kx - 0.4, ky + 0.4, kz - 0.4], [0.8, 3.6, 0.8], cloth(rgb('#2a2a3a'), rgb('#4a4a62'), folds=5, seed=274))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y - 0.4, (3.4, b.shoulder_y - b.elbow_y + 1.0, 3.4), sleeve)
        m.box(f'{side}_forearm', (cx, 0.2), b.wrist_y + 0.6, (3.8, b.elbow_y - b.wrist_y - 0.2, 4.2), sleeve)  # manche
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.2, (2.5, 2.8, 2.5), skin_mat(SKIN))
    m.cube('body', [-b.torso_w / 2, b.hip_y - 1.6, -d2 - 0.1], [b.torso_w, 1.8, b.torso_d + 0.2], hakama)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.3, (3.8, b.hip_y - b.knee_y + 0.3, 4.2), hakama)
        m.box(f'{side}_shin', (cx, 0), 1.0, (4.0, b.knee_y - 1.0, 4.4), hakama)                    # hakama large
        m.box(f'{side}_shin', (cx, -0.2), 0.0, (2.5, 1.0, 3.2), skin_mat(shade(SKIN, 0.95)))
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.8, 0.35, 3.8), flat(rgb('#8a6a3a'), 0.9))


def build():
    m = Model('nobunaga', density=4, tex_width=512)
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
