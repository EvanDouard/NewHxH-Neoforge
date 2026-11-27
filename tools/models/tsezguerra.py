"""
Tsezguerra — même style. Grand et solide (≈ 1,85 bloc) ; cheveux noirs plaqués en arrière, sourcils blancs très
marqués, bouc noir au menton, regard perçant ; costume sombre trois pièces, chemise claire, cravate.
Expression « froid » : regard de chasseur de primes (sourcils froncés).
"""
from __future__ import annotations

from adult import Body, head_box, skeleton, write_expression
from castkit import suit_body
from chibi import FaceGrid, crown_lock, flat, scalp, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#e6c4a6')
HAIR = rgb('#141418')
HAIR_L = rgb('#383c4c')
LINE = rgb('#1e1a1c')
BROW = rgb('#f4f2ec')

BODY = Body(head=(7.8, 8.6, 7.6), head_y=21.6, hip_y=11.8, knee_y=6.0, shoulder_y=20.1, arm_x=4.7, leg_x=1.8,
            torso_w=6.8, torso_d=3.7)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#5a6080'))


def build_hair(m):
    b = BODY
    top = b.top
    m.bone('hair', [0, top, 0], 'head')
    c, R = (0.0, top - 3.4, 0.4), 4.85
    scalp(m, c, R, lambda k: mk(k - 0.05)(0.5), skip_front=48, front_phi=40)
    rr = R + 0.18
    for i, x_az in enumerate((-30, -18, -6, 6, 18, 30)):
        crown_lock(m, f't_t{i}', c, rr, x_az, 136, 9.4, 3.6, mk(0.22), flat=0.28, turn=104, tip=0.35, segs=5)
    for sx in (-1, 1):
        for j, (az, phi, L) in enumerate(((sx * 58, 124, 8.4), (sx * 82, 108, 7.2), (sx * 104, 94, 6.0))):
            crown_lock(m, f't_s{sx}{j}', c, rr - 0.04 * j, az, phi, L, 3.4, mk(0.12), flat=0.3, turn=100, tip=0.35, segs=4)


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 4, HAIR)
    f.poly((8, 18 if cold else 20, 28, 22, 28, 26, 8, 24), BROW)              # sourcils blancs
    f.poly((58, 18 if cold else 20, 38, 22, 38, 26, 58, 24), BROW)
    for x0 in (12, 40):
        f.rect(x0, 28, x0 + 14, 32, (244, 242, 238))
        f.rect(x0 + 5, 28, x0 + 10, 32, rgb('#2a2228'))
        f.rect(x0 + 6, 29, x0 + 8, 31, (8, 6, 6))
        f.rect(x0 - 1, 26, x0 + 15, 28, LINE)
    f.rect(32, 34, 33, 42, shade(SKIN, 0.82))
    f.rect(27, 48, 39, 48, rgb('#7a4a44'))
    f.poly((29, 54, 37, 54, 35, 60, 31, 60), HAIR)                            # bouc


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (0.8 if ctx.face == 'south' else 1 if ctx.face == 'up' else 0.5))], fill=HAIR)


def build():
    m = Model('tsezguerra', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN)
    build_hair(m)
    suit_body(m, BODY, SKIN, rgb('#2a2a32'), rgb('#3c3c48'), rgb('#e8e6e0'), tie=rgb('#6a2a2a'))
    m.animations = humanoid_animations(arm_swing=14, leg_swing=22, breath=1.0)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
