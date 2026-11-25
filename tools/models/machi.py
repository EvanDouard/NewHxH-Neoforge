"""
Machi Komacine — même style. Mince (≈ 1,75 bloc) ; cheveux rose framboise tirés en une queue de cheval haute
(nœud au sommet arrière, longue queue épaisse qui retombe entre les omoplates), frange balayée sur le côté et deux
longues mèches qui encadrent le visage ; yeux bleu-violet en amande, air blasé ; haut de kimono indigo croisé à
manches larges, obi violet clair, pantalon sombre resserré, bottines ; fils de Nen enroulés aux doigts.
Expression « froid » : regard tranchant (yeux plissés).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f6dfcf')
HAIR = rgb('#c23a72')
HAIR_L = rgb('#f07aa8')
LINE = rgb('#3a1a26')
EYE = rgb('#5a6ad0')
TOP = rgb('#2c3266')
TOP_L = rgb('#424a8a')
OBI = rgb('#9a8ad0')

BODY = Body(head=(7.6, 8.2, 7.4), head_y=20.2, hip_y=11.0, knee_y=5.6, shoulder_y=18.8, arm_x=4.3, leg_x=1.65,
            torso_w=6.0, torso_d=3.4)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#ffb4d0'))


def build_hair(m):
    b = BODY
    top, hz = b.top, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(103)
    c, R = (0.0, top - 3.5, 0.3), 4.45
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=44, front_phi=40)
    rr = R + 0.22
    # cheveux tirés vers la queue : mèches plaquées des tempes vers le sommet arrière
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 60, 130, 6.4), (sx * 82, 116, 6.0), (sx * 104, 102, 5.4))):
            crown_lock(m, f'm_s{sx}{j}', c, rr - 0.05 * j, az, phi, L, 3.4, mk(0.16 if sx > 0 else 0.08), flat=0.32,
                       turn=70, tip=0.5, segs=3, lean=sx * 4)
    # frange balayée vers la gauche du perso, mèches pointues
    for j, (az, L, W, lean, k) in enumerate(((150, 5.4, 3.6, 18, 0.3), (166, 6.0, 3.8, 22, 0.36),
                                             (182, 6.4, 3.8, 26, 0.4), (198, 5.6, 3.4, 30, 0.34))):
        crown_lock(m, f'm_f{j}', c, rr + 0.05, az, 74, L, W, mk(k), flat=0.42, turn=84, tip=0.1, segs=3, lean=lean,
                   skew=0.12)
    # longues mèches qui encadrent le visage
    for sx in (-1, 1):
        lock(m, f'm_c{sx}', [sx * 3.9, b.head_y + 6.8, -hz + 0.6], 8.4, 2.2, 180, -sx * 4, mk(0.24), flat=0.5,
             curve=(-6, sx * 6), segs=4, root=1.15, tip=0.12, taper_pow=0.6, parent='head', skew=-0.1 * sx)
    # nœud au sommet arrière et longue queue de cheval
    kx, ky, kz = 0.0, top + 0.8, 1.6
    m.bone('knot', [kx, ky, kz], 'hair')
    m.cube('knot', [kx - 1.3, ky - 1.2, kz - 1.3], [2.6, 2.6, 2.6], mk(0.22)(0.5))
    m.cube('knot', [kx - 1.0, ky - 1.4, kz - 1.0], [2.0, 0.5, 2.0], flat(rgb('#2a2a3a'), 0.9))
    for k, (dx, L, W, rz, cz) in enumerate(((0, 15.0, 4.2, 0, 0), (-0.8, 12.5, 3.2, 12, -8), (0.8, 13.0, 3.2, -12, 8))):
        lock(m, f'm_tail{k}', [kx + dx, ky + 0.4, kz + 0.8], L, W, 122, rz, mk(0.24 - 0.06 * k), flat=0.7,
             curve=(58, cz), segs=5, root=1.2, tip=0.08, taper_pow=0.6, parent='knot', skew=0.06 * (k - 1))


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 8, HAIR)
    for x0 in (11, 39):
        top = 27 if cold else 25
        f.poly((x0, top + 2, x0 + 16, top, x0 + 16, top + 8, x0 + 2, top + 8), (248, 248, 250))
        f.rect(x0 + 5, top, x0 + 11, top + 8, EYE)
        f.rect(x0 + 7, top + 2, x0 + 9, top + 7, (16, 16, 40))
        f.poly((x0 - 1, top + 1, x0 + 17, top - 2, x0 + 17, top, x0 - 1, top + 3), LINE)
    f.rect(32, 37, 33, 42, shade(SKIN, 0.86))
    f.rect(29, 49, 37, 49, rgb('#b05a6a'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1 if ctx.face in ('up', 'south') else 0.55))], fill=HAIR)


def top_paint(ctx):
    cloth(TOP, TOP_L, shade(TOP, 0.7), ao_bottom=0.08, folds=3, seed=280)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.poly((18, 0, 26, 0, 40, 40, 34, 46), shade(TOP_L, 1.1))
        f.poly((26, 0, 40, 0, 33, 22), SKIN)


def build_body(m):
    b = BODY
    sleeve = cloth(TOP, TOP_L, shade(TOP, 0.7), folds=4, seed=281)
    pants = cloth(rgb('#22222c'), rgb('#34343e'), rgb('#141418'), folds=3, seed=282)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0, -d2], [b.torso_w, y1 - y0, b.torso_d], top_paint)
    m.cube('torso', [-1.1, y1 - 0.2, -1.1], [2.2, 0.6, 2.2], skin_mat(shade(SKIN, 0.92)))
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 - 0.2, -d2 - 0.15], [b.torso_w + 0.3, 1.8, b.torso_d + 0.3],
           cloth(OBI, seed=283))
    m.cube('torso', [-1.0, y0 - 0.4, d2 + 0.1], [2.0, 2.2, 0.8], cloth(shade(OBI, 0.9), seed=284))      # nœud du obi
    for name, sz in (('top_f', -1), ('top_b', 1)):                     # pans du kimono (mi-cuisse)
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 4, 0, 0])
        m.cube(name, [-b.torso_w / 2 - 0.05, b.hip_y - 2.6, sz * d2 - 0.3], [b.torso_w + 0.1, y0 - b.hip_y + 2.6, 0.6],
               sleeve)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y - 0.4, (3.0, b.shoulder_y - b.elbow_y + 1.0, 3.0), sleeve)
        m.box(f'{side}_forearm', (cx, 0.2), b.wrist_y + 0.8, (3.4, b.elbow_y - b.wrist_y - 0.4, 3.8), sleeve)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.0, (2.2, 2.4, 2.2), skin_mat(SKIN))
        for k in range(2):                                             # fils de Nen aux doigts
            m.cube(f'{side}_hand', [cx - 1.15, b.wrist_y - 1.6 + 0.7 * k, -1.15], [2.3, 0.15, 2.3],
                   flat(rgb('#e8e4ff'), 1.0))
    m.cube('body', [-b.torso_w / 2 + 0.4, b.hip_y - 1.2, -1.6], [b.torso_w - 0.8, 1.4, 3.2], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (2.8, b.hip_y - b.knee_y + 0.2, 2.9), pants)
        m.box(f'{side}_shin', (cx, 0), 2.2, (2.6, b.knee_y - 2.2, 2.7), pants)
        m.box(f'{side}_shin', (cx, -0.3), 0.0, (2.8, 2.4, 3.4), cloth(rgb('#3a2a3a'), seed=285))      # bottines


def build():
    m = Model('machi', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
