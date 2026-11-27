"""
Meruem, le Roi — même style. Grand (≈ 2 blocs), corps d'insecte humanoïde : carapace vert sombre aux plaques
plus claires, crête de la tête qui s'étire vers l'arrière en deux grands lobes (sa « chevelure », construite en
mèches organiques épaisses), deux antennes, visage pâle aux marques sombres sous les yeux, yeux fendus ; épaules à
plaques, longue queue segmentée qui se termine en dard ; cape courte sombre.
Expression « froid » : regard de Roi (sourcils froncés, pupilles fines).
"""
from __future__ import annotations

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, flat, lock, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#cfd9cf')
CARA = rgb('#35514a')
CARA_LIGHT = rgb('#577a6c')
CARA_DARK = rgb('#1c2e2a')
PLATE = rgb('#8fa69a')
MARK = rgb('#2c3a2a')
EYE = rgb('#c23a6a')
CAPE = rgb('#2a2440')
CAPE_LIGHT = rgb('#3e3660')

BODY = Body(head=(7.8, 8.6, 7.8), head_y=23.6, hip_y=13.0, knee_y=6.6, shoulder_y=22.0, arm_x=5.1, leg_x=2.0,
            torso_w=7.6, torso_d=4.0)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(CARA_DARK, CARA_LIGHT, t, lit=lit, rim=PLATE)


def build_crest(m):
    b = BODY
    top, hx, hz = b.top, b.head[0] / 2, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    cara = cloth(CARA, CARA_LIGHT, CARA_DARK, seed=130)
    m.cube('hair', [-hx - 0.25, top - 1.6, -hz + 0.9], [b.head[0] + 0.5, 2.0, b.head[2] - 0.5], cara)     # casque, front dégagé
    m.cube('hair', [-hx + 0.6, top + 0.4, -hz + 1.0], [b.head[0] - 1.2, 0.8, b.head[2] - 1.4], cara)
    for sx in (-1, 1):                                                      # joues de carapace
        m.cube('hair', [sx * (hx + 0.25) - 0.35, b.head_y + 2.4, -hz + 0.6], [0.7, 4.4, b.head[2] - 0.6], cara)
    # deux grands lobes lisses qui s'étirent vers l'arrière et un peu vers le haut : tronçons arrondis qui
    # s'affinent (arêtes adoucies par des pans), séparés par un sillon
    for sx in (-1, 1):
        m.bone(f'm_lobe{sx}', [sx * 1.9, top - 1.4, 0.6], 'hair', rotation=[-8, sx * 11, sx * 4])
        x0, y0, z0 = sx * 1.9, top - 1.4, 0.6
        for i in range(6):
            w = 3.4 * (1 - 0.12 * i)
            h = 3.8 * (1 - 0.14 * i)
            L = 2.3
            z = z0 + i * (L - 0.3)
            y = y0 - h / 2 + 0.22 * i + 0.03 * i * i      # le lobe se relève en s'effilant
            paint = cloth(shade(CARA, 1.08 - 0.04 * i), CARA_LIGHT, CARA_DARK, seed=140 + i)
            m.cube(f'm_lobe{sx}', [x0 - w / 2, y, z], [w, h, L], paint)
            s2 = min(w, h) * 0.42
            for ex in (-1, 1):                                    # pans supérieurs : arrondi
                m.cube(f'm_lobe{sx}', [x0 + ex * w / 2 - s2 / 2, y + h - s2 / 2, z], [s2, s2, L],
                       cloth(CARA_LIGHT, seed=150 + i), pivot=[x0 + ex * w / 2, y + h, z + L / 2], rotation=[0, 0, 45])
        m.cube(f'm_lobe{sx}', [x0 - 0.5, y0 + 0.6, z0 + 6 * 2.0 - 0.6], [1.0, 1.0, 1.4], cloth(CARA_DARK, seed=160))
        # deux antennes courtes au-dessus du front
        lock(m, f'm_ant{sx}', [sx * 1.3, top + 0.2, -2.6], 3.6, 0.6, -12, -sx * 18, mk(0.2), flat=1.0,
             curve=(-20, -sx * 8), segs=2, root=1.0, tip=0.4, taper_pow=0.4)


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.05, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 10, CARA)
    f.poly((0, 10, 8, 10, 0, 36), CARA)
    f.poly((66, 10, 58, 10, 66, 36), CARA)
    if cold:
        f.poly((12, 18, 28, 22, 28, 24, 12, 20), MARK)
        f.poly((54, 18, 38, 22, 38, 24, 54, 20), MARK)
    for x0, left in ((12, True), (40, False)):
        x1 = x0 + 14
        if left:
            f.poly((x0, 26, x1, 28, x1, 32, x0 + 2, 31), (240, 236, 220))
        else:
            f.poly((x0, 28, x1, 26, x1 - 2, 31, x0, 32), (240, 236, 220))
        ix = x0 + (5 if left else 4)
        f.rect(ix, 27, ix + 6, 32, EYE)
        f.rect(ix + 2 + (1 if cold else 0), 27, ix + 4 - (1 if cold else 0), 32, rgb('#1a0a12'))
        f.rect(x0 - 1, 25, x1 + 1, 26, MARK)
        f.poly((x0 + 3, 34, x0 + 6, 34, x0 + 4, 44, x0 + 2, 44), MARK)   # marques sous les yeux
    f.rect(32, 38, 33, 43, shade(SKIN, 0.85))
    f.rect(27, 49, 39, 49, MARK)


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    cloth(CARA, CARA_LIGHT, CARA_DARK, seed=131)(ctx)


def build_body(m):
    b = BODY
    cara = cloth(CARA, CARA_LIGHT, CARA_DARK, ao_bottom=0.1, folds=0, seed=132)
    plate = cloth(PLATE, shade(PLATE, 1.08), shade(PLATE, 0.75), seed=133)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    m.cube('torso', [-b.torso_w / 2 + 0.5, y0, -d2], [b.torso_w - 1.0, 4.4, b.torso_d], cara)
    m.cube('torso', [-b.torso_w / 2, y0 + 4.2, -d2 - 0.1], [b.torso_w, y1 - y0 - 4.2, b.torso_d + 0.2], cara)
    m.cube('torso', [-1.3, y1 - 0.2, -1.3], [2.6, 0.8, 2.6], cara)
    # plaques ventrales et pectorales en relief
    for k in range(3):
        m.cube('torso', [-1.6, y0 + 0.6 + k * 1.5, -d2 - 0.25], [3.2, 1.2, 0.3], plate)
    for sx in (-1, 1):
        m.cube('torso', [sx * 1.9 - 1.6, y1 - 4.2, -d2 - 0.3], [3.2, 3.0, 0.35], plate)
        m.cube('torso', [sx * (b.torso_w / 2 + 0.3) - 1.5, y1 - 1.2, -2.2], [3.0, 1.4, 4.4], cara,
               pivot=[sx * (b.torso_w / 2), y1 - 0.5, 0], rotation=[0, 0, -sx * 14])          # épaulières
    # cape courte sur le dos
    m.bone('cape', [0, y1, d2], 'torso', rotation=[6, 0, 0])
    m.cube('cape', [-b.torso_w / 2 - 0.4, y0 - 2.0, d2 + 0.1], [b.torso_w + 0.8, y1 - y0 + 1.6, 0.5],
           cloth(CAPE, CAPE_LIGHT, shade(CAPE, 0.7), folds=3, seed=134))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (3.1, b.shoulder_y - b.elbow_y + 0.4, 3.2), cara)
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (3.0, b.elbow_y - b.wrist_y - 0.2, 3.1), cara)
        m.box(f'{side}_forearm', (cx, -0.2), b.elbow_y - 3.0, (3.2, 2.0, 2.0), plate)
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.4, (2.6, 2.6, 2.6), cloth(CARA_DARK, seed=135))
        for k in range(3):                                                                   # griffes
            m.cube(f'{side}_hand', [cx - 1.0 + k * 0.8, b.wrist_y - 3.4, -1.2], [0.3, 1.0, 0.3], flat(CARA_DARK, 0.8))
    m.cube('body', [-b.torso_w / 2 + 0.5, b.hip_y - 1.2, -1.9], [b.torso_w - 1.0, 1.4, 3.8], cara)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (3.3, b.hip_y - b.knee_y + 0.2, 3.4), cara)
        m.box(f'{side}_leg', (cx, -0.3), b.knee_y + 1.2, (3.0, 3.0, 3.0), plate)
        m.box(f'{side}_shin', (cx, 0), 1.0, (3.0, b.knee_y - 1.0, 3.1), cara)
        m.box(f'{side}_shin', (cx, -0.6), 0.0, (3.1, 1.2, 4.4), cloth(CARA_DARK, seed=136))
    # queue segmentée : part du bas du dos, descend puis remonte, dard au bout
    # queue segmentée : anneaux de carapace qui s'affinent, chacun légèrement plus incliné, dard au bout
    parent, py, pz = 'body', b.hip_y + 0.6, 1.9
    for i in range(7):
        bn = f'tail_{i}'
        m.bone(bn, [0, py, pz], parent, rotation=[24 if i == 0 else 9, 0, 0])
        w = 2.4 * (1 - 0.1 * i)
        L = 2.1
        m.cube(bn, [-w / 2, py - w / 2, pz], [w, w, L], cloth(CARA if i % 2 == 0 else CARA_LIGHT, seed=170 + i))
        m.cube(bn, [-w / 2 - 0.12, py - w / 2 - 0.12, pz + L - 0.45], [w + 0.24, w + 0.24, 0.45], flat(CARA_DARK, 0.85))
        parent, pz = bn, pz + L - 0.1
    m.cube(parent, [-0.4, py - 0.4, pz], [0.8, 0.8, 1.6], flat(PLATE, 0.9))



def build():
    m = Model('meruem', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=4)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_crest(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=16, leg_swing=24, breath=1.2)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
