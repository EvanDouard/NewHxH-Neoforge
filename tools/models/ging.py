"""
Ging Freecss — même style. Taille moyenne (≈ 1,8 bloc) ; cheveux noirs en pointes sauvages (air de famille avec
Gon) qui jaillissent vers le haut et l'arrière au-dessus d'un turban de tissu clair noué sur le front (pans qui
pendent derrière) ; barbe de quelques jours, sourire en coin, yeux sombres malicieux ; chemise ample écrue
ouverte au col, écharpe-ceinture, pantalon large brun, sandales.
Expression « froid » : sérieux (yeux grands ouverts, sourcils froncés).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#e8c2a0')
HAIR = rgb('#121214')
HAIR_L = rgb('#2e3436')
LINE = rgb('#22181a')
WRAP = rgb('#d8ccb0')
WRAP_SH = rgb('#a89a7c')
SHIRT = rgb('#e8e0cc')
PANTS = rgb('#6a5236')
SASH = rgb('#4a6a5a')

BODY = Body(head=(7.8, 8.4, 7.6), head_y=20.8, hip_y=11.4, knee_y=5.8, shoulder_y=19.2, arm_x=4.7, leg_x=1.85,
            torso_w=6.8, torso_d=3.7)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#4a5a54'))


def build_hair(m):
    b = BODY
    top, hx, hz = b.top, b.head[0] / 2, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(149)
    c, R = (0.0, top - 3.4, 0.4), 4.5
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=50, front_phi=50)
    rr = R + 0.2
    # pointes sauvages qui jaillissent du haut du crâne vers le haut et l'arrière (au-dessus du turban)
    for ring, (rad, T, n, L0) in enumerate(((3.0, 52, 10, 6.8), (1.4, 26, 5, 7.6))):
        for i in range(n):
            az = i * 360 / n + ring * 18 + rnd.uniform(-8, 8)
            if ring == 0 and abs(((az + 180) % 360) - 180) > 150:
                continue                                         # pas de pointe droit devant
            a = math.radians(az)
            back = (1 + math.cos(a)) / 2
            x, z = rad * math.sin(a), 0.4 + rad * math.cos(a)
            t = T + 12 * back
            lock(m, f'g_u{ring}{i}', [x, top - 0.4, z], L0 + 1.6 * back + rnd.uniform(-0.6, 0.8), 4.0 + rnd.uniform(-0.3, 0.4),
                 t * math.cos(a) + 14, -t * math.sin(a), mk(0.12 + 0.08 * ring + 0.04 * (i % 2)), flat=0.6,
                 curve=(16, 0), segs=3, root=1.2, tip=0.06, taper_pow=0.6, ry=rnd.uniform(-15, 15),
                 skew=rnd.uniform(-0.14, 0.14))
    # turban : bandes de tissu enroulées, nœud et pans derrière
    wrap = cloth(WRAP, shade(WRAP, 1.05), WRAP_SH, folds=4, seed=340)
    y = b.head_y + 5.2
    m.cube('hair', [-hx - 0.45, y, -hz - 0.45], [b.head[0] + 0.9, 1.5, b.head[2] + 0.9], wrap)
    m.cube('hair', [-hx - 0.35, y + 1.2, -hz - 0.35], [b.head[0] + 0.7, 1.1, b.head[2] + 0.7], wrap,
           pivot=[0, y + 1.6, 0], rotation=[0, 0, -4])
    m.cube('hair', [-1.2, y + 0.2, hz + 0.3], [2.4, 1.8, 1.0], wrap)                           # nœud
    for k, (dx, L, rz) in enumerate(((-0.7, 7.8, 14), (0.7, 6.6, -16))):                     # pans qui flottent
        lock(m, f'g_w{k}', [dx, y + 0.8, hz + 0.9], L, 2.2, 140, rz,
             lambda t: strand_paint(WRAP_SH, WRAP, t, lit=0.2), flat=0.25, curve=(-36, rz * 1.6), segs=3, root=1.0,
             tip=0.6, taper_pow=0.4)


def face_paint(ctx, mode='calme'):
    serious = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 6, HAIR)
    if serious:
        f.poly((10, 18, 28, 22, 28, 25, 10, 21), LINE)
        f.poly((56, 18, 38, 22, 38, 25, 56, 21), LINE)
    else:
        f.poly((10, 21, 28, 20, 28, 23, 10, 24), LINE)
        f.poly((56, 21, 38, 20, 38, 23, 56, 24), LINE)
    for x0 in (12, 40):
        f.rect(x0, 26, x0 + 14, 33, (244, 240, 232))
        f.rect(x0 + 4, 26, x0 + 11, 33, rgb('#3a2a1e'))
        f.rect(x0 + 6, 27, x0 + 9, 32, (10, 8, 6))
        f.rect(x0 + 5, 27, x0 + 6, 28, (255, 255, 255))
        f.rect(x0 - 1, 24, x0 + 15, 26, LINE)
    f.rect(32, 35, 33, 41, shade(SKIN, 0.82))
    stub = shade(SKIN, 0.74)
    f.rect(10, 44, 56, 58, stub)
    import random as _r
    rr_ = _r.Random(91)
    for _ in range(160):                                          # barbe de quelques jours : poils piquetés
        x, y = rr_.randint(10, 55), rr_.randint(42, 59)
        f.rect(x, y, x, y, shade(SKIN, 0.55))
    if serious:
        f.rect(27, 49, 39, 49, rgb('#6a3e34'))
    else:
        f.poly((26, 48, 40, 46, 40, 48, 26, 50), rgb('#6a3e34'))                # sourire en coin


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.5))], fill=HAIR)


def shirt_paint(ctx):
    cloth(SHIRT, shade(SHIRT, 1.04), shade(SHIRT, 0.78), ao_bottom=0.1, folds=4, seed=341)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.poly((26, 0, 40, 0, 33, 18), SKIN)


def build_body(m):
    b = BODY
    sleeve = cloth(SHIRT, shade(SHIRT, 1.04), shade(SHIRT, 0.78), folds=4, seed=342)
    pants = cloth(PANTS, shade(PANTS, 1.2), shade(PANTS, 0.7), ao_top=0.1, folds=4, seed=343)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], shirt_paint)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.6, 2.4], skin_mat(shade(SKIN, 0.92)))
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 - 0.4, -d2 - 0.15], [b.torso_w + 0.3, 1.6, b.torso_d + 0.3],
           cloth(SASH, seed=344))
    m.cube('torso', [b.torso_w / 2 - 1.6, y0 - 3.2, -d2 - 0.3], [1.4, 3.4, 0.4], cloth(SASH, seed=345))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y - 0.4, (3.2, b.shoulder_y - b.elbow_y + 1.0, 3.2), sleeve)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 1.6, (3.0, b.elbow_y - b.wrist_y - 1.4, 3.1), sleeve)   # retroussées
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (2.5, 1.8, 2.5), skin_mat(SKIN))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.2, (2.4, 2.4, 2.4), skin_mat(SKIN))
    m.cube('body', [-b.torso_w / 2 + 0.2, b.hip_y - 1.4, -1.9], [b.torso_w - 0.4, 1.6, 3.8], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.3, (3.4, b.hip_y - b.knee_y + 0.3, 3.6), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (3.5, b.knee_y - 1.0, 3.6), pants)
        m.box(f'{side}_shin', (cx, -0.2), 0.0, (2.5, 1.0, 3.2), skin_mat(shade(SKIN, 0.95)))
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.8, 0.35, 3.8), flat(rgb('#5a4028'), 0.9))


def build():
    m = Model('ging', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3, feet_out=8)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
