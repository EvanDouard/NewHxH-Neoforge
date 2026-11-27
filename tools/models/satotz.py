"""
Satotz, examinateur de la première épreuve — même style. Très grand et mince (≈ 1,95 bloc) ; cheveux lavande
coiffés en deux grosses boucles enroulées de chaque côté de la tête (dessus lisse et plaqué) ; longue moustache en
guidon, pas de bouche visible, petits yeux calmes ; costume trois pièces violet, chemise blanche, cravate.
Expression « froid » : regard sévère.
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from castkit import suit_body
from chibi import FaceGrid, crown_lock, flat, scalp, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#f0d6c2')
HAIR = rgb('#9a7ac8')
HAIR_L = rgb('#d0b8f0')
LINE = rgb('#2a2234')

BODY = Body(head=(7.4, 8.6, 7.2), head_y=22.8, hip_y=12.6, knee_y=6.4, shoulder_y=21.2, arm_x=4.4, leg_x=1.7,
            torso_w=6.2, torso_d=3.4)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#ece0ff'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    c, R = (0.0, top - 3.4, 0.35), 4.7
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=48, front_phi=40)
    rr = R + 0.18
    for i, x_az in enumerate((-24, -8, 8, 24)):                       # dessus lisse plaqué
        crown_lock(m, f's_t{i}', c, rr, x_az, 136, 8.4, 3.8, mk(0.22), flat=0.28, turn=100, tip=0.4, segs=5)
    for i, az in enumerate((-40, -14, 14, 40)):
        crown_lock(m, f's_b{i}', c, rr, az, 70, 6.4, 3.8, mk(0.08), flat=0.3, turn=50, tip=0.4, segs=3)
    # deux grosses boucles enroulées au-dessus des oreilles : rouleaux (anneau de mèches autour d'un axe gauche-droite)
    import math
    for sx in (-1, 1):
        cx, cy, cz = sx * (b.head[0] / 2 + 1.2), b.head_y + 6.0, 0.6
        m.cube('head', [cx - 1.3, cy - 1.6, cz - 1.6], [2.6, 3.2, 3.2], mk(0.12)(0.5))
        for k in range(10):
            a = k * 36
            r = math.radians(a)
            y, z = cy + 2.0 * math.cos(r), cz + 2.0 * math.sin(r)
            m.cube('head', [cx - 1.5, y - 0.7, z - 1.0], [3.0, 1.4, 2.0], mk(0.1 + 0.18 * (0.5 + 0.5 * math.cos(r)))(0.5),
                   pivot=[cx, y, z], rotation=[-a, 0, 0])


def face_paint(ctx, mode='calme'):
    stern = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 4, HAIR)
    for x0 in (14, 40):
        f.rect(x0, 28, x0 + 12, 31, (244, 242, 246))
        f.rect(x0 + 4, 28, x0 + 8, 31, rgb('#3a2a4a'))
        f.poly((x0 - 1, 25 if stern else 26, x0 + 13, 23 if stern else 25, x0 + 13, 27, x0 - 1, 28), LINE)
    f.rect(32, 33, 33, 41, shade(SKIN, 0.84))
    f.poly((12, 46, 30, 42, 33, 44, 36, 42, 54, 46, 52, 48, 36, 45, 33, 47, 30, 45, 14, 48), HAIR)   # moustache en guidon


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (0.85 if ctx.face == 'south' else 1 if ctx.face == 'up' else 0.5))],
                           fill=HAIR)


def build():
    m = Model('satotz', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    suit_body(m, BODY, SKIN, rgb('#5a3a7a'), rgb('#7a58a0'), rgb('#f4f2ee'), tie=rgb('#2a1a3a'))
    m.animations = humanoid_animations(arm_swing=14, leg_swing=26, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
