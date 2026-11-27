"""
Kite — même style. Grand et mince (≈ 1,85 bloc) ; très longs cheveux blancs, raides avec un léger mouvement, qui
tombent jusqu'à la taille, mèches pointues devant les épaules, quelques mèches de frange sous la casquette ;
grande casquette souple bleu nuit à visière ; yeux sombres calmes ; long manteau bleu nuit ouvert sur une
chemise claire, pantalon gris-bleu, bottes.
Expression « froid » : regard de chasseur (yeux plissés, ombre).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#ecd2bc')
WHITE = rgb('#f2f2f6')
WHITE_SH = rgb('#a2aabe')
LINE = rgb('#24202a')
HAT = rgb('#283050')
HAT_L = rgb('#3c4670')
COAT = rgb('#242a40')
COAT_L = rgb('#363e5c')
SHIRT = rgb('#e6e4dc')

BODY = Body(head=(7.8, 8.4, 7.6), head_y=21.6, hip_y=11.8, knee_y=6.0, shoulder_y=20.0, arm_x=4.7, leg_x=1.85,
            torso_w=6.8, torso_d=3.7)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(WHITE_SH, WHITE, t, lit=lit, rim=rgb('#ffffff'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(127)
    c, R = (0.0, top - 3.7, 0.3), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.12)(0.5), skip_front=46, front_phi=34)
    rr = R + 0.25
    for i, az in enumerate(range(-76, 77, 12)):                   # rideau jusqu'à la taille
        a = math.radians(az)
        r = rr + 0.15 + (0.45 if i % 2 else 0.0)                  # deux plans de mèches
        x, z = c[0] + r * math.sin(a), c[2] + r * math.cos(a)
        side = abs(az) / 76
        L = 22.5 - 3.5 * side + rnd.uniform(-2.6, 1.6)
        lock(m, f'k_c{i}', [x, c[1] + 2.0, z], L + 1.6, 3.8 + (0.0 if i % 2 else 0.6), 178, az * 0.05, mk(0.05 + 0.04 * (i % 2)),
             ry=rnd.uniform(-6, 6), flat=0.38, curve=(-16 * math.cos(a), az * 0.04), segs=5, root=1.05, tip=0.12,
             taper_pow=0.45, belly=1.08, skew=rnd.uniform(-0.12, 0.12))
    for sx in (-1, 1):                                            # côtés courts (cachent la tête) + devant épaules
        for j, az in enumerate((88, 102)):
            a = math.radians(sx * az)
            r = rr + 0.2
            lock(m, f'k_cs{sx}{j}', [c[0] + r * math.sin(a), c[1] + 2.0, c[2] + r * math.cos(a)], 9.0, 3.4, 180, 0,
                 mk(0.1), flat=0.38, segs=3, root=1.05, tip=0.2, taper_pow=0.4)
        for j, az in enumerate((118, 130)):
            a = math.radians(sx * az)
            r = rr + 0.15
            lock(m, f'k_s{sx}{j}', [c[0] + r * math.sin(a), c[1] + 0.6, c[2] + r * math.cos(a)], 12.5 - 1.2 * j, 3.2,
                 180, -sx * 4, mk(0.14), flat=0.4, curve=(-6, sx * 6), segs=4, root=1.05, tip=0.1, taper_pow=0.5)
    for j, (az, L, lean) in enumerate(((154, 3.4, -8), (170, 3.8, -2), (192, 3.6, 4), (208, 3.2, 10))):   # frange
        crown_lock(m, f'k_f{j}', c, rr + 0.1, az, 46, L, 3.0, mk(0.3), flat=0.42, turn=40, tip=0.1, segs=3, lean=lean)
    # casquette souple : calotte bouffante, bandeau, visière
    hx, hz = b.head[0] / 2, b.head[2] / 2
    hat = cloth(HAT, HAT_L, shade(HAT, 0.7), folds=3, seed=300)
    m.bone('hat', [0, top, 0], 'head', rotation=[-6, 0, 4])
    m.cube('hat', [-hx - 0.35, top - 1.5, -hz - 0.35], [b.head[0] + 0.7, 1.6, b.head[2] + 0.7], cloth(shade(HAT, 0.85), seed=301))
    for k, (dy, g, h) in enumerate(((0.0, 0.7, 1.5), (1.5, 0.15, 0.6))):    # calotte souple
        m.cube('hat', [-hx - g, top + dy, -hz - g + 0.5], [b.head[0] + 2 * g, h, b.head[2] + 2 * g], hat)
    m.cube('hat', [-hx + 0.2, top - 1.3, -hz - 3.2], [b.head[0] - 0.4, 0.45, 2.9], cloth(shade(HAT, 0.8), seed=302),
           pivot=[0, top - 1.3, -hz - 0.3], rotation=[-6, 0, 0])


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 10, WHITE_SH)
    for x0 in (12, 40):
        top = 28 if cold else 26
        f.rect(x0, top, x0 + 14, 32, (244, 244, 248))
        f.rect(x0 + 4, top, x0 + 10, 32, rgb('#2a2a36'))
        f.rect(x0 + 6, top + 1, x0 + 8, 32, (8, 8, 12))
        f.rect(x0 - 1, top - 2, x0 + 15, top, LINE)
    if cold:
        f.rect(8, 20, 58, 25, shade(SKIN, 0.84))
    f.rect(32, 35, 33, 42, shade(SKIN, 0.84))
    f.rect(28, 50, 38, 50, rgb('#8a5a50'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=WHITE_SH)


def shirt_paint(ctx):
    cloth(COAT, COAT_L, shade(COAT, 0.7), ao_bottom=0.1, folds=3, seed=303)(ctx)
    if ctx.face == 'north':
        f = FaceGrid(ctx)
        f.rect(24, 0, 42, 60, SHIRT)                              # chemise visible entre les pans ouverts
        f.rect(32, 4, 34, 60, shade(SHIRT, 0.8))


def build_body(m):
    b = BODY
    coat = cloth(COAT, COAT_L, shade(COAT, 0.7), ao_bottom=0.1, folds=3, seed=304)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.2, -d2], [b.torso_w, y1 - y0 + 0.2, b.torso_d], shirt_paint)
    m.cube('torso', [-1.2, y1 - 0.2, -1.2], [2.4, 0.6, 2.4], skin_mat(shade(SKIN, 0.92)))
    for sx in (-1, 1):                                            # revers du col
        m.cube('torso', [sx * 1.8 - 0.8, y1 - 3.6, -d2 - 0.3], [1.6, 3.8, 0.4], coat,
               pivot=[sx * 1.8, y1, -d2], rotation=[0, 0, -sx * 14])
    for name, sz in (('coat_f', -1), ('coat_b', 1)):              # pans longs du manteau
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 5, 0, 0])
        if sz < 0:
            for sx in (-1, 1):
                m.cube(name, [sx * b.torso_w / 2 - (2.6 if sx > 0 else 0), b.knee_y - 1.4, sz * d2 - 0.35],
                       [2.6, y0 - b.knee_y + 1.4, 0.7], coat)
        else:
            m.cube(name, [-b.torso_w / 2 - 0.1, b.knee_y - 1.4, sz * d2 - 0.35], [b.torso_w + 0.2, y0 - b.knee_y + 1.4, 0.7],
                   coat)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (3.0, b.shoulder_y - b.elbow_y + 0.6, 3.1), coat)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.1, (3.1, b.elbow_y - b.wrist_y - 0.1, 3.2), coat)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.4, (2.4, 2.4, 2.4), skin_mat(SKIN))
    pants = cloth(rgb('#5a6478'), rgb('#6e7a90'), rgb('#3c4456'), folds=2, seed=305)
    m.cube('body', [-b.torso_w / 2 + 0.6, b.hip_y - 1.2, -1.8], [b.torso_w - 1.2, 1.4, 3.6], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (3.0, b.hip_y - b.knee_y + 0.2, 3.1), pants)
        m.box(f'{side}_shin', (cx, 0), 2.0, (2.9, b.knee_y - 2.0, 3.0), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.1, 2.6, 4.0), cloth(rgb('#2a2220'), seed=306))


def build():
    m = Model('kite', density=4, tex_width=512)
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
