"""
Meruem, le Roi des Fourmis chimères (anime 2011) — même style Minecraft anime. Refonte d'après les références :

- corps humanoïde musclé et athlétique (pas d'armure) : peau vert-bleu pâle, zones pigmentées sombres (épaules,
  flancs, avant-bras, bas des jambes, lignes sur la poitrine) ; pieds nus à quatre orteils, mains à quatre doigts ;
- tête : grande carapace lisse qui épouse le crâne (front, dessus, arrière) et se prolonge vers l'arrière en deux
  grands lobes arrondis qui se relèvent ; deux longues antennes qui pendent des oreilles comme de longs lobes ;
- visage fin et pâle, yeux violets étroits, marques sombres sous les yeux, expression froide ;
- longue queue de scorpion segmentée qui part du bas du dos, descend puis remonte, dard au bout ;
- cape royale violet sombre attachée aux épaules, qui tombe jusqu'aux mollets.
Expression « froid » : sourcils froncés, pupilles fines.
"""
from __future__ import annotations

from adult import Body, head_box, skeleton, write_expression
from chibi import FaceGrid, cloth, flat, lock, strand_paint
from geolib import Model, humanoid_animations, material, rgb, shade

SKIN = rgb('#b9ccc2')
SKIN_D = rgb('#93ab9f')
PIG = rgb('#3a5448')          # zones pigmentées
PIG_D = rgb('#26392f')
SHELL = rgb('#4f6e60')        # carapace de la tête
SHELL_L = rgb('#729383')
SHELL_D = rgb('#2c4238')
EYE = rgb('#7a3c9a')
CAPE = rgb('#2e2347')
CAPE_L = rgb('#45386a')
CAPE_IN = rgb('#5a2a3a')

BODY = Body(head=(7.4, 8.4, 7.4), head_y=24.2, hip_y=13.2, knee_y=6.6, shoulder_y=22.6, arm_x=5.6, leg_x=2.0,
            torso_w=8.6, torso_d=4.2)
FACE_D = 7


def mk(lit):
    return lambda t: strand_paint(SHELL_D, SHELL_L, t, lit=lit, rim=shade(SHELL_L, 1.15))


def skin_paint(seed=0, k=1.0):
    return material(shade(SKIN, k), top=1.08, bottom=0.86, edge=0.86, noise=3, grad=0.12, seed=seed)


def pig_paint(seed=0):
    return material(PIG, top=1.12, bottom=0.82, edge=0.8, noise=4, grad=0.14, seed=seed)


def torso_paint(ctx):
    """Torse nu : peau pâle, pectoraux et abdominaux dessinés, flancs et lignes pigmentés."""
    skin_paint(seed=11)(ctx)
    w, h = ctx.w, ctx.h
    d = ctx.draw
    if ctx.face == 'north':
        # flancs pigmentés qui remontent vers les épaules
        d.polygon([(0, h), (int(w * 0.16), h), (int(w * 0.24), int(h * 0.35)), (int(w * 0.12), 0), (0, 0)], fill=PIG)
        d.polygon([(w, h), (int(w * 0.84), h), (int(w * 0.76), int(h * 0.35)), (int(w * 0.88), 0), (w, 0)], fill=PIG)
        # pectoraux
        for x0, x1 in ((0.22, 0.49), (0.51, 0.78)):
            d.line([(int(w * x0), int(h * 0.36)), (int(w * x1), int(h * 0.38))], fill=SKIN_D, width=max(1, h // 40))
        d.line([(w // 2, int(h * 0.12)), (w // 2, int(h * 0.92))], fill=SKIN_D, width=max(1, w // 60))
        # abdominaux
        for k in range(3):
            y = int(h * (0.52 + 0.13 * k))
            d.line([(int(w * 0.34), y), (int(w * 0.66), y)], fill=SKIN_D, width=max(1, h // 50))
        # lignes pigmentées sous les clavicules
        for sx in (-1, 1):
            cx = w // 2 + sx * int(w * 0.17)
            d.line([(cx, int(h * 0.08)), (cx + sx * int(w * 0.12), int(h * 0.26))], fill=PIG, width=max(2, w // 30))
    elif ctx.face == 'south':
        d.polygon([(0, 0), (w, 0), (int(w * 0.8), int(h * 0.3)), (int(w * 0.2), int(h * 0.3))], fill=PIG)
        d.line([(w // 2, int(h * 0.3)), (w // 2, h)], fill=PIG_D, width=max(2, w // 40))
    else:
        d.rectangle([0, 0, w, h], fill=PIG)
        d.rectangle([int(w * 0.3), int(h * 0.15), int(w * 0.7), h], fill=shade(PIG, 1.15))


def build_head(m):
    b = BODY
    top, hx, hz = b.top, b.head[0] / 2, b.head[2] / 2
    m.bone('hair', [0, top, 0], 'head')
    shell = cloth(SHELL, SHELL_L, SHELL_D, folds=0, seed=130)
    # carapace qui épouse le crâne : bandeau frontal en arc, dessus bombé, arrière qui descend jusqu'à la nuque
    m.cube('hair', [-hx - 0.3, top - 2.2, -hz - 0.3], [b.head[0] + 0.6, 2.0, b.head[2] + 0.6], shell)
    m.cube('hair', [-hx + 0.2, top - 0.3, -hz + 0.2], [b.head[0] - 0.4, 1.0, b.head[2] - 0.4], shell)
    m.cube('hair', [-hx + 1.0, top + 0.6, -hz + 1.0], [b.head[0] - 2.0, 0.6, b.head[2] - 1.6], shell)
    m.cube('hair', [-hx - 0.3, b.head_y + 0.8, hz - 0.6], [b.head[0] + 0.6, b.head[1] - 2.8, 1.0], shell)  # arrière
    for sx in (-1, 1):                                            # côtés jusqu'aux tempes
        m.cube('hair', [sx * (hx + 0.15) - 0.3, b.head_y + 4.0, -hz + 1.6], [0.6, 2.6, b.head[2] - 1.4], shell)
    m.cube('hair', [-2.2, top - 2.6, -hz - 0.45], [4.4, 0.6, 0.4], cloth(SHELL_D, seed=131))        # arête frontale
    # deux grands lobes arrondis : ils naissent du dessus du casque, s'étirent vers l'arrière et se relèvent
    for sx in (-1, 1):
        bn = f'm_lobe{sx}'
        x0, y0, z0 = sx * 1.75, top - 0.6, -1.0
        m.bone(bn, [x0, y0, z0], 'hair', rotation=[6, sx * 10, sx * 3])
        n = 8
        for i in range(n):
            t = i / (n - 1)
            w = 3.3 * (1.0 - 0.55 * t ** 1.3)
            h = 3.4 * (1.0 - 0.5 * t ** 1.2)
            L = 1.9
            z = z0 + i * (L - 0.2)
            y = y0 - h / 2 + 0.28 * i * t
            k = 1.06 - 0.12 * t
            m.cube(bn, [x0 - w / 2, y, z], [w, h, L], cloth(shade(SHELL, k), SHELL_L, SHELL_D, seed=140 + i))
            s2 = min(w, h) * 0.45                                 # arêtes adoucies
            for ex in (-1, 1):
                m.cube(bn, [x0 + ex * w / 2 - s2 / 2, y + h - s2 / 2, z], [s2, s2, L],
                       cloth(shade(SHELL_L, k), seed=150 + i), pivot=[x0 + ex * w / 2, y + h, z + L / 2],
                       rotation=[0, 0, 45])
        m.cube(bn, [x0 - 0.45, y0 + 0.28 * (n - 1) - 0.4, z0 + n * 1.7], [0.9, 0.9, 1.0], cloth(SHELL_D, seed=160))
    # antennes : longues, fines, elles pendent des oreilles le long des joues jusqu'aux épaules
    for sx in (-1, 1):
        lock(m, f'm_ant{sx}', [sx * (hx + 0.35), b.head_y + 3.6, 0.2], 7.4, 1.3, 176, -sx * 6, mk(0.25), flat=0.9,
             curve=(14, sx * 10), segs=4, root=1.4, tip=0.5, taper_pow=0.5, parent='head')


def face_paint(ctx, mode='calme'):
    cold = mode == 'froid'
    material(SKIN, grad=0.04, noise=2, outline=False)(ctx)
    f = FaceGrid(ctx)
    f.rect(0, 0, 66, 9, SHELL)                                     # bord du casque sur le front
    f.poly((0, 9, 6, 9, 0, 22), SHELL)
    f.poly((66, 9, 60, 9, 66, 22), SHELL)
    f.poly((2, 48, 10, 66, 0, 66), shade(SKIN, 0.9))               # joues creusées : visage plus fin
    f.poly((64, 48, 56, 66, 66, 66), shade(SKIN, 0.9))
    brow = PIG_D
    if cold:
        f.poly((12, 20, 28, 24, 28, 26, 12, 22), brow)
        f.poly((54, 20, 38, 24, 38, 26, 54, 22), brow)
    else:
        f.rect(12, 22, 27, 23, brow)
        f.rect(39, 22, 54, 23, brow)
    for x0, left in ((12, True), (40, False)):
        x1 = x0 + 14
        if left:                                                   # yeux étroits en amande
            f.poly((x0, 28, x1, 27, x1, 31, x0 + 3, 31), (236, 232, 222))
        else:
            f.poly((x0, 27, x1, 28, x1 - 3, 31, x0, 31), (236, 232, 222))
        ix = x0 + (6 if left else 3)
        f.rect(ix, 27, ix + 5, 31, EYE)
        f.rect(ix + 2, 27, ix + 3 if cold else ix + 4, 31, rgb('#1a0a1a'))
        f.rect(x0 - 1, 26, x1 + 1, 26, PIG_D)
        f.poly((x0 + 4, 33, x0 + 7, 33, x0 + 5, 45, x0 + 3, 45), PIG)  # marques sous les yeux
    f.rect(32, 36, 33, 42, shade(SKIN, 0.82))
    f.rect(28, 50, 38, 50, PIG_D)


def head_paint(ctx):
    if ctx.face == 'north':
        face_paint(ctx)
        return
    if ctx.face == 'up':
        cloth(SHELL, SHELL_L, SHELL_D, seed=131)(ctx)
        return
    skin_paint(seed=12)(ctx)


def build_body(m):
    b = BODY
    y0, y1 = b.hip_y + 0.2, b.shoulder_y + 0.6
    d2 = b.torso_d / 2
    # torse en V : taille plus étroite, cage thoracique et épaules larges
    m.cube('torso', [-b.torso_w / 2 + 1.0, y0, -d2 + 0.2], [b.torso_w - 2.0, 4.0, b.torso_d - 0.4], torso_paint)
    m.cube('torso', [-b.torso_w / 2, y0 + 3.8, -d2], [b.torso_w, y1 - y0 - 3.8, b.torso_d], torso_paint)
    for sx in (-1, 1):                                              # pectoraux en léger relief
        m.cube('torso', [sx * 1.95 - 1.75, y1 - 4.4, -d2 - 0.3], [3.5, 2.6, 0.35], skin_paint(20, 1.03))
        m.cube('torso', [sx * (b.torso_w / 2 + 0.2) - 1.6, y1 - 1.6, -2.1], [3.2, 2.0, 4.2], pig_paint(21),
               pivot=[sx * b.torso_w / 2, y1 - 0.6, 0], rotation=[0, 0, -sx * 18])                # épaules pigmentées
    m.cube('torso', [-1.5, y1 - 0.2, -1.4], [3.0, 1.0, 2.8], skin_paint(22, 0.95))                 # cou
    # cape royale attachée aux épaules (fermoirs dorés), qui tombe jusqu'aux mollets en deux pans
    for half, hx in (('r', -1), ('l', 1)):
        bn = f'cape_{half}'
        m.bone(bn, [hx * 0.1, y1, d2 + 0.3], 'torso', rotation=[7, hx * 4, hx * 2])
        x0 = -b.torso_w / 2 - 0.9 if hx < 0 else 0.0
        L = y1 - 4.0
        m.cube(bn, [x0, y1 - L, d2 + 0.3], [b.torso_w / 2 + 0.9, L, 0.5], cloth(CAPE, CAPE_L, shade(CAPE, 0.7), folds=3, seed=134 + (hx > 0)))
        m.cube(bn, [x0 + 0.1, y1 - L + 0.1, d2 + 0.25], [b.torso_w / 2 + 0.7, L - 0.2, 0.06], flat(CAPE_IN, 0.9))
    for sx in (-1, 1):
        m.cube('torso', [sx * (b.torso_w / 2 - 0.6) - 0.6, y1 - 0.4, -0.6], [1.2, 1.2, 1.2], flat(rgb('#c9a640'), 0.85))
        m.cube('torso', [sx * (b.torso_w / 2 + 0.2) - 0.7, y1 - 0.9, 0.2], [1.4, 1.2, d2 + 0.6], cloth(CAPE, CAPE_L, seed=137))
    # bras musclés : épaules pigmentées, bras nus pâles, avant-bras pigmentés, mains à quatre doigts
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.arm_x
        m.box(f'{side}_arm', (cx, 0), b.elbow_y, (3.3, b.shoulder_y - b.elbow_y + 0.4, 3.3), skin_paint(30))
        m.box(f'{side}_arm', (cx + sx * 0.1, 0), b.shoulder_y - 3.0, (3.6, 3.2, 3.6), pig_paint(31))
        m.box(f'{side}_forearm', (cx, 0), b.wrist_y + 0.2, (3.1, b.elbow_y - b.wrist_y - 0.2, 3.1), pig_paint(32))
        m.box(f'{side}_hand', (cx, 0), b.wrist_y - 2.1, (2.6, 2.3, 2.6), pig_paint(33))
        for k in range(4):                                                                   # quatre doigts
            m.cube(f'{side}_hand', [cx - 1.15 + k * 0.62, b.wrist_y - 3.0, -1.1], [0.5, 1.0, 0.6], flat(PIG_D, 0.85))
    # bassin : pagne court sombre, jambes nues, bas des jambes pigmenté, pieds nus à quatre orteils
    m.cube('body', [-b.torso_w / 2 + 0.9, b.hip_y - 1.6, -2.0], [b.torso_w - 1.8, 1.8, 4.0], cloth(PIG_D, PIG, seed=40))
    for side, sx in (('right', -1), ('left', 1)):
        cx = sx * b.leg_x
        m.box(f'{side}_leg', (cx, 0), b.knee_y - 0.2, (3.4, b.hip_y - b.knee_y + 0.2, 3.5), skin_paint(41))
        m.box(f'{side}_shin', (cx, 0), 1.0, (3.1, b.knee_y - 1.0, 3.2), pig_paint(42))
        m.box(f'{side}_shin', (cx, -0.5), 0.0, (3.0, 1.1, 4.2), pig_paint(43))
        for k in range(4):
            m.cube(f'{side}_shin', [cx - 1.3 + k * 0.7, 0.0, -2.9], [0.55, 0.8, 0.6], flat(PIG_D, 0.85))
    # queue de scorpion : part du bas du dos, descend, puis remonte en s'affinant, dard recourbé au bout
    parent, py, pz = 'body', b.hip_y + 0.4, 1.9
    pitches = [22, -4, -12, -14, -16, -16, -14, -12, -10]
    for i, rx in enumerate(pitches):
        bn = f'tail_{i}'
        m.bone(bn, [0, py, pz], parent, rotation=[rx, 0, 0])
        w = 2.5 * (1 - 0.07 * i)
        L = 2.2
        m.cube(bn, [-w / 2, py - w / 2, pz], [w, w, L], pig_paint(170 + i) if i % 2 else cloth(SHELL, SHELL_L, SHELL_D, seed=170 + i))
        m.cube(bn, [-w / 2 - 0.12, py - w / 2 - 0.12, pz + L - 0.4], [w + 0.24, w + 0.24, 0.4], flat(PIG_D, 0.85))
        parent, pz = bn, pz + L - 0.1
    m.bone('tail_sting', [0, py, pz], parent, rotation=[-40, 0, 0])
    m.cube('tail_sting', [-0.7, py - 0.7, pz], [1.4, 1.4, 1.4], cloth(SHELL_L, seed=180))
    m.cube('tail_sting', [-0.3, py - 0.3, pz + 1.2], [0.6, 0.6, 1.8], flat(rgb('#d8d0b8'), 0.9))


def build():
    m = Model('meruem', density=4, tex_width=512)
    skeleton(m, BODY, arm_out=5)
    head_box(m, BODY, head_paint, FACE_D, skin=SKIN, ears=False)
    build_head(m)
    build_body(m)
    m.animations = humanoid_animations(arm_swing=16, leg_swing=24, breath=1.2)
    return m


if __name__ == '__main__':
    model = build()
    model.save()
    write_expression(model, face_paint, FACE_D)
