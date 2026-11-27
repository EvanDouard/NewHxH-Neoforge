"""
Phinks Magcub — même style. Grand et sec (≈ 1,85 bloc) ; cheveux blond cendré courts plaqués vers l'arrière (front
haut, pas de sourcils), nuque rasée ; yeux étroits jaune-brun, air bagarreur ; survêtement bleu marine à bandes
blanches (veste zippée, col montant), pantalon de survêtement, baskets ; bras droit qu'il fait tourner (poing
serré).
Expression « froid » : rictus de colère (dents serrées).
"""
from __future__ import annotations

import random

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, crown_lock, scalp, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#e6c6a8')
HAIR = rgb('#a8946a')
HAIR_L = rgb('#dccca0')
LINE = rgb('#2a2018')
TRACK = rgb('#22305a')
TRACK_L = rgb('#344478')
STRIPE = rgb('#eceae4')

BODY = Body(head=(7.8, 8.4, 7.6), head_y=21.6, hip_y=11.8, knee_y=6.0, shoulder_y=20.0, arm_x=4.8, leg_x=1.85,
            torso_w=7.0, torso_d=3.8)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#f4ead0'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    rnd = random.Random(151)
    c, R = (0.0, top - 3.6, 0.35), 4.3
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=52, front_phi=46,
          rings=((40, 12, -0.08), (58, 9, -0.04), (74, 5, 0.0)))
    rr = R + 0.12
    for i, x_az in enumerate((-30, -18, -6, 6, 18, 30)):           # plaqué vers l'arrière depuis un front haut
        crown_lock(m, f'p_t{i}', c, rr, x_az + rnd.uniform(-2, 2), 132 + rnd.uniform(-3, 3), 9.4, 3.6,
                   mk(0.2 + 0.03 * (2.5 - abs(i - 2.5))), flat=0.22, turn=104, tip=0.4, segs=5,
                   twist=rnd.uniform(-4, 4))
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 48, 128, 6.0),)):
            crown_lock(m, f'p_s{sx}{j}', c, rr, az, phi, L, 3.4, mk(0.1), flat=0.22, turn=96, tip=0.4, segs=3)


def face_paint(ctx, mode='calme'):
    angry = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 4, HAIR)
    f.poly((10, 21, 30, 24, 30, 26, 10, 23), shade(SKIN, 0.78))      # arcades froncées, sans sourcils
    f.poly((56, 21, 36, 24, 36, 26, 56, 23), shade(SKIN, 0.78))
    for x0 in (12, 40):
        f.rect(x0, 27, x0 + 14, 31, (242, 238, 228))
        f.rect(x0 + 5, 27, x0 + 10, 31, rgb('#8a6a2a'))
        f.rect(x0 + 7, 28, x0 + 8, 30, (14, 10, 6))
        f.poly((x0 - 1, 26, x0 + 15, 24 if angry else 25, x0 + 15, 27, x0 - 1, 28), LINE)
    f.rect(32, 33, 33, 41, shade(SKIN, 0.82))
    if angry:
        f.rect(22, 46, 44, 51, (240, 236, 228))
        f.rect(22, 48, 44, 48, LINE)
        f.rect(22, 45, 44, 46, LINE)
        f.rect(22, 51, 44, 52, LINE)
    else:
        f.rect(26, 49, 40, 49, rgb('#7a4a40'))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        h = int(ctx.h * (0.85 if ctx.face == 'south' else 1 if ctx.face == 'up' else 0.62))
        ctx.draw.rectangle([0, 0, ctx.w, int(h * 0.55)], fill=shade(HAIR, 0.8))
        # nuque et côtés rasés : fondu progressif (duvet blond qui s'éclaircit vers la peau)
        import random
        rnd = random.Random(ctx.w * 7 + ctx.h)
        stub = tuple(int(0.55 * a + 0.45 * b) for a, b in zip(HAIR[:3], SKIN[:3]))
        for y in range(int(h * 0.55), h):
            for x in range(ctx.w):
                if rnd.random() < 1.0 - (y - h * 0.55) / (h * 0.45 + 1) * 0.8:
                    ctx.img.putpixel((x, y), stub + (255,))


def track_paint(ctx):
    cloth(TRACK, TRACK_L, shade(TRACK, 0.7), ao_bottom=0.1, folds=2, seed=350)(ctx)
    f = FaceGrid(ctx)
    if ctx.face == 'north':
        f.rect(32, 0, 34, 60, rgb('#8a8a90'))                    # fermeture éclair
    if ctx.face in ('east', 'west'):
        f.rect(26, 0, 32, 60, STRIPE)
        f.rect(34, 0, 40, 60, STRIPE)


def build_body(m):
    b = BODY
    sleeve = track_paint
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2, y0 - 0.4, -d2], [b.torso_w, y1 - y0 + 0.4, b.torso_d], track_paint)
    m.cube('torso', [-2.2, y1 - 0.4, -2.0], [4.4, 1.6, 4.0], cloth(TRACK, TRACK_L, folds=1, seed=351))    # col
    m.cube('torso', [-b.torso_w / 2 - 0.1, y0 - 0.6, -d2 - 0.1], [b.torso_w + 0.2, 1.0, b.torso_d + 0.2],
           cloth(shade(TRACK, 0.85), seed=352))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (3.1, b.shoulder_y - b.elbow_y + 0.6, 3.2), sleeve)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (3.0, b.elbow_y - b.wrist_y - 0.2, 3.1), sleeve)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.4, (2.6, 2.6, 2.6), skin_mat(SKIN))
    pants = track_paint
    m.cube('body', [-b.torso_w / 2 + 0.4, b.hip_y - 1.2, -1.8], [b.torso_w - 0.8, 1.4, 3.6],
           cloth(TRACK, TRACK_L, folds=2, seed=353))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (3.1, b.hip_y - b.knee_y + 0.2, 3.2), pants)
        m.box(f'{side}_shin', (cx, 0), 1.0, (3.0, b.knee_y - 1.0, 3.1), pants)
        m.box(f'{side}_shin', (cx, -0.4), 0.0, (3.1, 1.2, 4.0), cloth(rgb('#ecebe6'), rgb('#ffffff'), rgb('#3a3a46'), seed=354))


def build():
    m = Model('phinks', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=16, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
