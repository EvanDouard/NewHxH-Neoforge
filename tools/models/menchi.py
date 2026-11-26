"""
Menchi — même style. Mince (≈ 1,7 bloc) ; cheveux bleu-vert attachés en cinq petites couettes en pointe qui
partent dans tous les sens (deux de chaque côté, une au sommet arrière), frange en mèches pointues, nuque courte ;
grands yeux verts vifs, air gourmand et colérique ; haut résille sur une brassière noire, mini-short, ceinture à
couteaux de cuisine, bottines.
Expression « froid » : colère (sourcils froncés, bouche ouverte).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f6dcc8')
HAIR = rgb('#2e7a82')
HAIR_L = rgb('#6ab8bc')
LINE = rgb('#1a2a2a')
EYE = rgb('#3ab05a')
MESH = rgb('#4a4a52')
TOP = rgb('#16161a')
SHORTS = rgb('#2a2a4a')
TIE = rgb('#e84a6a')

BODY = Body(head=(7.8, 8.2, 7.6), head_y=19.6, hip_y=10.6, knee_y=5.4, shoulder_y=18.2, arm_x=4.2, leg_x=1.6,
            torso_w=5.8, torso_d=3.2)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#a8e0e0'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(173)
    c, R = (0.0, top - 3.5, 0.3), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=46, front_phi=34)
    rr = R + 0.22
    for i, az in enumerate(range(0, 360, 26)):                    # mèches courtes qui suivent le crâne
        if 140 < az < 220:
            continue
        crown_lock(m, f'm_m{i}', c, rr, az + rnd.uniform(-4, 4), 82 + rnd.uniform(-4, 4), 6.4 + rnd.uniform(-0.4, 0.6),
                   4.0, mk(0.14 + 0.06 * math.cos(math.radians(az - 150))), flat=0.45, turn=104, tip=0.15, segs=4,
                   twist=rnd.uniform(-8, 8))
    for j, (az, L, W, lean) in enumerate(((148, 5.0, 3.4, -14), (164, 5.6, 3.6, -6), (180, 6.0, 3.4, 0),
                                          (196, 5.6, 3.6, 6), (212, 5.0, 3.4, 14))):
        crown_lock(m, f'm_f{j}', c, rr, az, 70, L, W, mk(0.34), flat=0.45, turn=80, tip=0.1, segs=3, lean=lean,
                   skew=rnd.uniform(-0.12, 0.12))
    # cinq petites couettes en pointe : nœud rose + mèche courte qui pointe vers l'extérieur
    for k, (az, phi, out) in enumerate(((66, 58, 74), (118, 40, 82), (-66, 58, 74), (-118, 40, 82), (0, 70, 60))):
        a, p = math.radians(az), math.radians(phi)
        base = [c[0] + (rr + 0.3) * math.cos(p) * math.sin(a), c[1] + (rr + 0.3) * math.sin(p),
                c[2] + (rr + 0.3) * math.cos(p) * math.cos(a)]
        m.cube('hair', [base[0] - 0.8, base[1] - 0.8, base[2] - 0.8], [1.6, 1.6, 1.6], flat(TIE, 0.95))
        rx = out * math.cos(a)
        rz = -out * math.sin(a)
        lock(m, f'm_t{k}', base, 4.0 + rnd.uniform(-0.3, 0.4), 3.4, rx, rz, mk(0.2), flat=0.6,
             curve=(14 * math.cos(a), -14 * math.sin(a)), segs=3, root=1.1, tip=0.06, taper_pow=0.55, belly=1.2)


def face_paint(ctx, mode='calme'):
    angry = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 8, HAIR)
    for x0 in (11, 39):
        f.rect(x0, 24, x0 + 16, 36, (250, 250, 250))
        f.rect(x0 + 4, 24, x0 + 12, 36, EYE)
        f.rect(x0 + 6, 27, x0 + 10, 35, (10, 30, 14))
        f.rect(x0 + 5, 25, x0 + 7, 27, (255, 255, 255))
        f.rect(x0 - 1, 22, x0 + 17, 24, LINE)
        if angry:
            f.poly((x0, 16 if x0 > 30 else 20, x0 + 16, 20 if x0 > 30 else 16, x0 + 16, 22 if x0 > 30 else 18,
                    x0, 18 if x0 > 30 else 22), LINE)
    if angry:
        f.poly((26, 44, 40, 44, 38, 52, 28, 52), rgb('#7a2a30'))
    else:
        f.poly((28, 46, 38, 46, 36, 49, 30, 49), rgb('#c0606a'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.6))], fill=HAIR)


def mesh_paint(ctx):
    material(SKIN, grad=0.06, noise=2, edge=0.9)(ctx)
    f = FaceGrid(ctx)
    for k in range(0, 66, 11):                                     # résille
        f.rect(k, 0, k + 1, 60, MESH)
    for k in range(0, 60, 10):
        f.rect(0, k, 66, k + 1, MESH)
    f.rect(0, 16, 66, 30, TOP)                                     # brassière


def build_body(m):
    b = BODY
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], mesh_paint)
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 - 0.2, -d2 - 0.15], [b.torso_w + 0.3, 1.0, b.torso_d + 0.3],
           cloth(rgb('#5a3a22'), seed=390))
    for k, x in enumerate((-2.2, -1.2, 1.4, 2.4)):                 # couteaux à la ceinture
        m.cube('torso', [x - 0.3, y0 - 2.4, -d2 - 0.35], [0.6, 2.4, 0.2], flat(rgb('#d8dce4'), 1.0))
        m.cube('torso', [x - 0.35, y0 - 0.2, -d2 - 0.4], [0.7, 1.0, 0.3], flat(rgb('#3a2a1e'), 0.9))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (2.5, b.shoulder_y - b.elbow_y + 0.6, 2.5), mesh_paint)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y, (2.3, b.elbow_y - b.wrist_y, 2.3), skin_mat(SKIN))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.1, (2.2, 2.2, 2.2), skin_mat(SKIN))
    shorts = cloth(SHORTS, shade(SHORTS, 1.2), shade(SHORTS, 0.7), folds=2, seed=391)
    m.cube('body', [-b.torso_w / 2 + 0.3, b.hip_y - 1.2, -1.55], [b.torso_w - 0.6, 1.4, 3.1], shorts)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.hip_y - 2.4, (2.7, 2.6, 2.8), shorts)
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.4, b.hip_y - b.knee_y - 2.0, 2.5), skin_mat(SKIN))
        m.box(f'{side}_shin', (cx, 0), 2.4, (2.3, b.knee_y - 2.4, 2.4), skin_mat(SKIN))
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.6, 2.6, 3.3), cloth(rgb('#2a2224'), seed=392))


def build():
    m = Model('menchi', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
