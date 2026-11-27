"""
Menthuthuyoupi (anime 2011) — refonte d'après les références : le moins humain des trois gardes royaux.
Colosse (≈ 2,4 blocs) à la musculature énorme et à la tête petite ; peau rouge sombre ; cheveux noirs courts
plaqués vers l'arrière ; oreilles de forme irrégulière ; os du visage saillants (arcades, pommettes), yeux
toujours plissés, dents acérées ; jambes animales (cuisses massives, tibias inclinés vers l'arrière, pieds à deux
gros orteils ronds) ; il ne porte qu'un pantalon noir moulant.
Expression « froid » : rage (bouche ouverte sur les crocs).
"""
from __future__ import annotations

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, flat, lock, skin_mat, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#8e2f2a')
SKIN_L = rgb('#b04a40')
SKIN_D = rgb('#5c1a18')
HAIR = rgb('#1c1a1e')
HAIR_L = rgb('#3c3a44')
LINE = rgb('#220a0a')
EYE = rgb('#f2d24a')
PANTS = rgb('#141418')
PANTS_L = rgb('#2c2c36')

BODY = Body(head=(7.2, 7.6, 7.2), head_y=29.4, hip_y=15.4, knee_y=7.6, shoulder_y=27.6, arm_x=8.8, leg_x=2.7,
            torso_w=13.8, torso_d=8.4)
FACE_D = 7


def skin(k=1.0):
    return material(shade(SKIN, k), grad=0.12, noise=2, edge=0.86)


def mkh(lit):
    return lambda t: strand_paint(HAIR, HAIR_L, t, lit=lit, rim=rgb('#55525e'))


def build_crest(m):
    """Cheveux noirs courts plaqués vers l'arrière, oreilles irrégulières, arcades saillantes."""
    b = BODY
    top, hx, hz = b.top, b.head[0] / 2, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    hair = cloth(HAIR, HAIR_L, shade(HAIR, 0.6), seed=190)
    m.cube('hair', [-hx - 0.2, top - 1.0, -hz + 0.6], [b.head[0] + 0.4, 1.4, b.head[2] - 0.2], hair)   # calotte
    m.cube('hair', [-hx - 0.2, top - 3.6, hz - 0.8], [b.head[0] + 0.4, 3.0, 1.0], hair)                 # nuque
    for i, x in enumerate((-2.4, -0.8, 0.8, 2.4)):                 # mèches plaquées vers l'arrière
        lock(m, f'y_h{i}', [x, top - 0.1, -hz + 0.8], 6.2, 2.2, 96 + 4 * (i % 2), (x / 2.4) * 6, mkh(0.2),
             flat=0.4, curve=(40, 0), segs=3, root=1.1, tip=0.3, taper_pow=0.5)
    m.cube('head', [-hx - 0.15, b.head_y + 4.7, -hz - 0.45], [b.head[0] + 0.3, 1.0, 0.6], skin(0.85))  # arcade
    for sx in (-1, 1):
        m.cube('head', [sx * (hx - 1.6) - 0.9, b.head_y + 2.4, -hz - 0.35], [1.8, 1.0, 0.4], skin(1.08))  # pommettes
        # oreilles irrégulières : trois lobes déchiquetés de tailles différentes
        for k, (dy, L, w, rz) in enumerate(((3.4, 2.4, 2.4, 28), (1.6, 2.0, 2.0, -18))):
            m.cube('head', [sx * (hx + 0.1) - (L if sx < 0 else 0), b.head_y + dy, -0.4], [L, w, 0.7], skin(0.92 - 0.06 * k),
                   pivot=[sx * hx, b.head_y + dy + w / 2, 0], rotation=[0, 0, sx * rz])


def face_paint(ctx, mode='calme'):
    rage = mode == 'froid'
    material(SKIN, grad=0.06, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 8, HAIR)
    f.rect(6, 16, 60, 22, SKIN_D)                               # arcade sourcilière lourde
    for x0 in (12, 41):                                         # yeux toujours plissés : fentes étroites
        f.rect(x0, 25, x0 + 13, 28, (30, 6, 6))
        f.rect(x0 + 4, 26, x0 + 9, 27, EYE)
    for x0 in (8, 48):                                          # pommettes saillantes
        f.poly((x0, 32, x0 + 10, 36, x0 + 2, 40), SKIN_D)
    f.rect(30, 30, 36, 40, SKIN_D)                              # nez épaté
    f.rect(27, 38, 39, 41, SKIN_D)
    if rage:
        f.rect(16, 45, 50, 56, rgb('#3a0a10'))
        for x in range(18, 48, 5):
            f.poly((x, 45, x + 3, 45, x + 1.5, 49), (240, 236, 220))
            f.poly((x, 56, x + 3, 56, x + 1.5, 52), (240, 236, 220))
    else:                                                       # bouche fermée, crocs qui dépassent
        f.rect(14, 49, 52, 50, LINE)
        for x in (20, 26, 40, 46):
            f.poly((x, 50, x + 3, 50, x + 1.5, 54), (240, 236, 220))


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    material(SKIN, grad=0.08, noise=2, edge=0.9)(ctx)
    if ctx.face in ('east', 'west', 'south', 'up'):
        ctx.draw.rectangle([0, 0, ctx.w, int(ctx.h * (1.0 if ctx.face == 'up' else 0.45))], fill=HAIR)


def build_body(m):
    b = BODY
    pants = cloth(PANTS, PANTS_L, shade(PANTS, 0.7), ao_top=0.1, folds=1, seed=201)
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    # torse musclé (taille plus fine), pectoraux, abdominaux en relief
    m.cube('torso', [-b.torso_w / 2 + 1.4, y0, -d2 + 0.4], [b.torso_w - 2.8, 5.0, b.torso_d - 0.8], skin(0.96))
    m.cube('torso', [-b.torso_w / 2, y0 + 4.8, -d2], [b.torso_w, y1 - y0 - 4.8, b.torso_d], skin())
    for sx in (-1, 1):
        m.cube('torso', [sx * 2.6 - 2.4, y1 - 5.2, -d2 - 0.5], [4.8, 3.4, 0.6], skin(1.05))     # pectoraux
        for k in range(3):
            m.cube('torso', [sx * 1.2 - 1.0, y0 + 0.8 + k * 1.7, -d2 + 0.1], [2.0, 1.4, 0.4], skin(1.02))
    m.cube('torso', [-2.0, y1 - 0.2, -2.0], [4.0, 1.0, 4.0], skin(0.9))                           # cou épais
    for sx in (-1, 1):
        m.cube('torso', [sx * 3.4 - 2.2, y1 - 0.8, -2.4], [4.4, 1.8, 4.8], skin(0.95),
               pivot=[sx * 3.4, y1, 0], rotation=[0, 0, -sx * 22])                                 # trapèzes
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.shoulder_y - 3.8, (6.4, 5.0, 6.2), skin(0.98))           # deltoïde
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (5.6, b.shoulder_y - b.elbow_y - 3.2, 5.6), skin())
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (5.4, b.elbow_y - b.wrist_y - 0.2, 5.4), skin(0.97))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 3.6, (4.4, 3.8, 4.4), skin(0.95))
    # pantalon noir moulant (seul vêtement) ; jambes animales : cuisses massives, tibias inclinés vers l'arrière,
    # talon haut, pieds à deux gros orteils ronds
    m.cube('body', [-b.torso_w / 2 + 1.0, b.hip_y - 1.8, -2.9], [b.torso_w - 2.0, 2.4, 5.8], pants)
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, -0.4), b.knee_y - 0.4, (5.6, b.hip_y - b.knee_y + 0.4, 6.0), pants)
        m.box(f'{side}_leg', (cx, -1.6), b.knee_y - 0.6, (4.6, 2.4, 2.4), pants)                  # genou
        m.cube(f'{side}_shin', [cx - 2.0, 1.8, -0.8], [4.0, b.knee_y - 1.4, 4.0], pants,
               pivot=[cx, b.knee_y, 0], rotation=[-22, 0, 0])                                      # tibia incliné
        m.cube(f'{side}_shin', [cx - 1.7, 0.6, 1.0], [3.4, 2.6, 2.6], skin(0.88))                  # talon haut
        for k, ox in enumerate((-1.1, 1.1)):                                                       # deux orteils ronds
            m.cube(f'{side}_shin', [cx + ox - 1.2, 0.0, -2.6], [2.4, 1.8, 3.4], skin(0.92 + 0.04 * k))


def build():
    m = Model('youpi', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=8, leg_out=3)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_crest(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=14, leg_swing=20, breath=1.6)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
