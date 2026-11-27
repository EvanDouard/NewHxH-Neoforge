"""
Illumi Zoldyck — même style. Grand et mince (≈ 1,95 bloc) ; très longs cheveux noirs parfaitement raides, coupés
net : masse lisse sur le crâne, rideau qui tombe jusqu'à la taille dans le dos, deux longues mèches devant les
épaules, frange droite au-dessus des yeux ; grands yeux noirs vides sans reflet, visage impassible ; tunique
sombre vert-noir à col montant, ceinture-écharpe claire, aiguilles plantées sur la poitrine, pantalon large.
Expression « froid » : même visage, yeux plus grands et ombre (inquiétant).
"""
from __future__ import annotations

import math
import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, flat, lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f3e2d4')
HAIR = rgb('#0f0f14')
HAIR_L = rgb('#2c3040')
LINE = rgb('#16141a')
TUNIC = rgb('#1e2622')
TUNIC_L = rgb('#34403a')
SASH = rgb('#b9b2a2')
NEEDLE = rgb('#c8ccd4')
PIN = rgb('#5a2a8a')

BODY = Body(head=(7.8, 8.4, 7.6), head_y=23.0, hip_y=12.6, knee_y=6.4, shoulder_y=21.4, arm_x=4.7, leg_x=1.85,
            torso_w=6.9, torso_d=3.7)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#4a5068'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(53)
    c, R = (0.0, top - 3.7, 0.3), 4.6
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=40, front_phi=20)
    rr = R + 0.25
    # rideau : longues mèches droites qui pendent de la ligne équatoriale du crâne jusqu'à la taille
    for i, az in enumerate(range(-72, 73, 12)):
        a = math.radians(az)
        r = rr + 0.15 + (0.4 if i % 2 else 0.0)          # deux plans : nappes intérieures et extérieures
        x, z = c[0] + r * math.sin(a), c[2] + r * math.cos(a)
        side = abs(az) / 72
        L = 16.5 - 3.0 * side + rnd.uniform(-1.2, 1.0) + (0.8 if i % 2 == 0 else 0.0)
        lock(m, f'i_c{i}', [x, c[1] + 0.4, z], L, 3.6 + (0.8 if i % 2 == 0 else 0.0), 180 + 4 * math.cos(a), rnd.uniform(-1.0, 1.0),
             mk(0.04 + 0.03 * (i % 2)), ry=-az * 0.0, flat=0.26, curve=(-10 * math.cos(a), 0), segs=4, root=1.05,
             tip=0.75 + 0.15 * (i % 2), taper_pow=0.15, belly=1.0, skew=rnd.uniform(-0.03, 0.03))
    # dessus lisse : mèches qui partent du sommet et suivent le crâne jusqu'au rideau
    for i, az in enumerate(range(0, 360, 24)):
        if 140 < az < 220:
            continue
        crown_lock(m, f'i_t{i}', c, rr + 0.1, az, 98, 9.8, 4.0, mk(0.16 + 0.04 * math.cos(math.radians(az - 120))),
                   flat=0.3, turn=96, tip=0.85, segs=4, taper_pow=0.2)
    # côtés : rideau court (jusqu'aux épaules) qui cache le côté de la tête sans recouvrir les bras
    for sx in (-1, 1):
        for j, az in enumerate((86, 100, 112)):
            a = math.radians(sx * az)
            r = rr + 0.2
            lock(m, f'i_cs{sx}{j}', [c[0] + r * math.sin(a), c[1] + 0.4, c[2] + r * math.cos(a)], 7.6 - 0.4 * j, 3.4,
                 180, 0, mk(0.08), flat=0.32, segs=3, root=1.05, tip=0.85, taper_pow=0.25, belly=1.02)
    # deux longues mèches devant les épaules (jusqu'à la poitrine)
    for sx in (-1, 1):
        for j, az in enumerate((120, 130)):
            a = math.radians(sx * az)
            r = rr + 0.1
            lock(m, f'i_s{sx}{j}', [c[0] + r * math.sin(a), c[1] + 0.8, c[2] + r * math.cos(a)], 12.0 - 1.0 * j,
                 3.0, 180, 0, mk(0.1), flat=0.32, curve=(0, 0), segs=4, root=1.05, tip=0.8, taper_pow=0.25,
                 belly=1.02)
        for j, az in enumerate((84, 102, 120, 136)):
            crown_lock(m, f'i_sv{sx}{j}', c, rr + 0.2, sx * az, 92, 9.5 - 0.6 * j, 3.8, mk(0.14 + 0.02 * j), flat=0.3,
                       turn=86, tip=0.85, segs=4, taper_pow=0.2)
    # frange droite coupée net au-dessus des yeux
    for j, az in enumerate((150, 165, 180, 195, 210)):
        crown_lock(m, f'i_f{j}', c, rr + 0.1, az, 92, 6.6, 3.6, mk(0.22), flat=0.28, turn=70, tip=0.95, segs=3,
                   taper_pow=0.15)


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 17, HAIR)
    for x0 in (11, 39):
        top = 23 if cold else 24
        f.rect(x0, top, x0 + 16, 36 if cold else 34, (10, 10, 14))       # grands yeux noirs vides
        f.rect(x0 - 1, top - 2, x0 + 17, top, LINE)
        f.rect(x0 + 2, top + 1, x0 + 14, top + 2, rgb('#24242c'))
    if cold:
        f.rect(6, 18, 60, 22, shade(SKIN, 0.82))
    f.rect(32, 39, 33, 44, shade(SKIN, 0.86))
    f.rect(30, 50, 36, 50, rgb('#9a7068'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, ctx.h], fill=HAIR)


def build_body(m):
    b = BODY
    tunic = cloth(TUNIC, TUNIC_L, shade(TUNIC, 0.65), ao_bottom=0.1, folds=3, seed=230)
    sash = cloth(SASH, shade(SASH, 1.08), shade(SASH, 0.75), folds=2, seed=231)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.2, -d2], [b.torso_w, y1 - y0 + 0.2, b.torso_d], tunic)
    m.cube('torso', [-2.0, y1 - 0.4, -1.8], [4.0, 1.8, 3.6], tunic)                           # col montant
    m.cube('torso', [-b.torso_w / 2 - 0.15, y0 + 0.6, -d2 - 0.15], [b.torso_w + 0.3, 1.6, b.torso_d + 0.3], sash)
    m.cube('torso', [1.2, y0 - 3.4, -d2 - 0.3], [1.4, 4.4, 0.4], sash)                         # pan de l'écharpe
    for k, (x, y) in enumerate(((-2.2, y1 - 2.4), (-1.2, y1 - 3.6), (1.6, y1 - 2.8), (2.4, y1 - 4.2))):
        m.cube('torso', [x - 0.15, y - 0.85, -d2 - 2.2], [0.3, 0.3, 2.4], flat(NEEDLE, 1.0))   # aiguilles, plus longues
        m.cube('torso', [x - 0.45, y - 1.15, -d2 - 2.6], [0.9, 0.9, 0.5], flat(PIN, 0.9))
    # pans de la tunique (mi-cuisse)
    for name, sz in (('tunic_f', -1), ('tunic_b', 1)):
        m.bone(name, [0, y0, sz * d2], 'body', rotation=[-sz * 4, 0, 0])
        m.cube(name, [-b.torso_w / 2 - 0.1, b.hip_y - 3.4, sz * d2 - 0.3], [b.torso_w + 0.2, y0 - b.hip_y + 3.4, 0.6],
               tunic)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (3.0, b.shoulder_y - b.elbow_y + 0.6, 3.0), tunic)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.1, (3.2, b.elbow_y - b.wrist_y - 0.1, 3.2), tunic)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.4, (2.4, 2.4, 2.4), skin_mat(SKIN))
    pants = cloth(rgb('#2a2e2c'), rgb('#3c4240'), rgb('#181a1a'), folds=3, seed=232)
    m.cube('body', [-b.torso_w / 2 + 0.6, b.hip_y - 1.2, -1.8], [b.torso_w - 1.2, 1.4, 3.6], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (3.2, b.hip_y - b.knee_y + 0.2, 3.3), pants)
        m.box(f'{side}_shin', (cx, 0), 1.2, (3.2, b.knee_y - 1.2, 3.3), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (2.8, 1.2, 4.0), cloth(rgb('#141416'), seed=233))


def build():
    m = Model('illumi', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=12, leg_swing=20, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
