"""
Boîte à outils commune aux personnages au style « Minecraft anime » (référence d'Evan pour Gon) : grosse tête
cubique (~1/3 de la hauteur), corps compact, cheveux en grandes lames triangulaires en escalier, vêtements avec
reliefs, ombres peintes. Voir docs/Analyse_model.md et docs/apparences-personnages.md.
"""
from __future__ import annotations

import random

from geolib import material, mix, rgb, shade

# ---------------------------------------------------------------------------------------------------------------
# Squelette chibi standard (mêmes noms d'os que Gon : les animations sont partagées)

HEAD_Y = 16.8       # base de la tête
HEAD = (9.4, 8.6, 8.6)


def skeleton(m, arm_out=7, leg_out=3, feet_out=8, arm_rot=None, forearm_rot=None):
    m.bone('body', [0, 7.0, 0])
    m.bone('torso', [0, 9.4, 0], 'body')
    m.bone('head', [0, HEAD_Y, 0], 'torso')
    for side, sx in (('right', -1), ('left', 1)):
        rot = list(arm_rot or [0, 0, 0])
        rot[2] += -sx * arm_out
        m.bone(f'{side}_arm', [sx * 4.6, 15.6, 0], 'torso', rotation=rot)
        m.bone(f'{side}_forearm', [sx * 5.5, 11.8, 0], f'{side}_arm', rotation=forearm_rot)
        m.bone(f'{side}_hand', [sx * 5.5, 7.9, 0], f'{side}_forearm')
        m.bone('item_main' if side == 'right' else 'item_off', [sx * 5.5, 6.2, -1.0], f'{side}_hand')
        m.bone(f'{side}_leg', [sx * 2.05, 7.4, 0], 'body', rotation=[0, 0, -sx * leg_out])
        m.bone(f'{side}_shin', [sx * 2.05, 3.8, 0], f'{side}_leg', rotation=[0, -sx * feet_out, 0])


# ---------------------------------------------------------------------------------------------------------------
# Matériaux

def cloth(base, light=None, dark=None, ao_top=0.0, ao_bottom=0.12, folds=0, seed=0):
    """Tissu : dégradé, faces différenciées, occlusion aux jonctions, quelques plis verticaux."""
    base = rgb(base)
    light = rgb(light) if light else shade(base, 1.12)
    dark = rgb(dark) if dark else shade(base, 0.72)

    def paint(ctx):
        rnd = random.Random(seed * 31 + ctx.w * 7 + ctx.h)
        px = ctx.img.load()
        face_k = {'north': 1.0, 'west': 0.94, 'east': 0.86, 'south': 0.84, 'up': 1.08, 'down': 0.7}[ctx.face]
        fold_x = [rnd.randrange(2, max(3, ctx.w - 2)) for _ in range(folds)] if ctx.face not in ('up', 'down') else []
        for y in range(ctx.h):
            t = y / max(1, ctx.h - 1)
            for x in range(ctx.w):
                c = mix(light, base, min(1, t * 1.4)) if ctx.face not in ('up', 'down') else base
                c = shade(c, face_k)
                if ctx.face not in ('up', 'down'):
                    if t < ao_top:
                        c = shade(c, 0.78 + 0.22 * t / ao_top)
                    if t > 1 - ao_bottom:
                        c = shade(c, 0.82 + 0.18 * (1 - t) / ao_bottom)
                    if x in fold_x and 0.25 < t < 0.95:
                        c = mix(c, dark, 0.45)
                    if x == 0 or x == ctx.w - 1:
                        c = shade(c, 0.86)
                d = rnd.randint(-3, 3)
                px[x, y] = (max(0, min(255, c[0] + d)), max(0, min(255, c[1] + d)), max(0, min(255, c[2] + d)), 255)
    return paint


def flat(color, edge=0.9):
    return material(color, grad=0.1, noise=2, edge=edge)


def skin_mat(skin):
    return material(skin, grad=0.12, noise=2, edge=0.86)


# ---------------------------------------------------------------------------------------------------------------
# Cheveux

def hair_paint(dark, lit_color, rim, lit=0.0, seed=1):
    """Faces éclairées (avant, gauche de l'image, dessus) vers `lit_color`, faces à l'ombre vers `dark`,
    plus clair vers la pointe ; liseré de reflet `rim` ; bords assombris."""
    dark, lit_color, rim = rgb(dark), rgb(lit_color), rgb(rim)
    rnd = random.Random(seed)

    def paint(ctx):
        px = ctx.img.load()
        lit_face = {'north': 0.55, 'west': 0.75, 'up': 0.9, 'east': 0.18, 'south': 0.12, 'down': 0.0}[ctx.face]
        for y in range(ctx.h):
            t = 1 - y / max(1, ctx.h - 1)
            for x in range(ctx.w):
                k = lit_face * (0.35 + 0.65 * t) * (0.75 + lit)
                c = mix(dark, lit_color, min(1, k))
                if ctx.face in ('north', 'west') and x == 1 and t > 0.3:
                    c = mix(c, rim, 0.5 * lit_face)
                if ctx.face not in ('up', 'down') and (x == 0 or x == ctx.w - 1):
                    c = shade(c, 0.8)
                d = rnd.randint(-3, 3)
                px[x, y] = (max(0, min(255, c[0] + d)), max(0, min(255, c[1] + d)), max(0, min(255, c[2] + d)), 255)
    return paint


def blade(m, name, base, length, width, rx, rz, paint_for, ry=0, depth=None, parent='hair', step=1.7,
          taper=1.0, min_w=0.9):
    """Mèche en lame triangulaire « voxel » : tranches empilées qui rétrécissent jusqu'à la pointe (bords en
    escalier), avec une vraie épaisseur, dans un os orienté (rx > 0 : vers l'arrière ; rz > 0 : vers la gauche de
    l'image ; rz < 0 : vers la droite). `paint_for(t)` donne la peinture d'une tranche (t = 0 base, 1 pointe)."""
    m.bone(name, base, parent, rotation=[rx, ry, rz])
    x, y, z = base
    depth = depth if depth is not None else max(1.6, width * 0.42)
    n = max(3, round(length / step))
    h = length / n
    for i in range(n):
        t = i / n
        w = max(min_w, width * (1 - t) ** taper)
        d = max(0.8, depth * (1 - 0.5 * t))
        m.cube(name, [x - w / 2, y + i * h - 0.4, z - d / 2], [w, h + 0.4, d], paint_for(t))


# ---------------------------------------------------------------------------------------------------------------
# Visage : aides de dessin (face avant de la tête, en coordonnées d'une grille 66 × 60)

class FaceGrid:
    def __init__(self, ctx):
        self.ctx, self.d = ctx, ctx.draw
        self.sx, self.sy = ctx.w / 66, ctx.h / 60

    def P(self, *v):
        return [round(v[i] * (self.sx if i % 2 == 0 else self.sy)) for i in range(len(v))]

    def rect(self, x0, y0, x1, y1, color):
        self.d.rectangle(self.P(x0, y0, x1, y1), fill=rgb(color))

    def poly(self, pts, color):
        self.d.polygon(self.P(*pts), fill=rgb(color))


def strand(m, name, base, length, width, rx, rz, paint_for, ry=0, depth=None, bend=(0.0, 0.0), step=0.55,
           taper=0.85, min_w=0.35, parent='hair'):
    """Mèche continue (itération 3) : au lieu de quelques étages bien visibles, la section diminue par tranches
    très fines (≈ 0,5 px) pour que les bords lisent comme des diagonales continues jusqu'à une vraie pointe. La
    mèche peut se courber (`bend` : décalage x, z de la pointe, en px, réparti en t²) et se tordre (`ry`).
    `paint_for(t)` doit peindre sans dégradé propre à la tranche (sinon on revoit les étages)."""
    m.bone(name, base, parent, rotation=[rx, ry, rz])
    x, y, z = base
    depth = depth if depth is not None else width * 0.5
    n = max(4, round(length / step))
    h = length / n
    for i in range(n):
        t = (i + 0.5) / n
        w = max(min_w, width * (1 - t) ** taper)
        d = max(min_w * 0.8, depth * (1 - 0.7 * t) ** 1.0)
        ox, oz = bend[0] * t * t, bend[1] * t * t
        m.cube(name, [x + ox - w / 2, y + i * h - 0.02, z + oz - d / 2], [w, h + 0.04, d], paint_for(t))


_STRAND_PAINTS = {}

# contraste volontairement réduit : sur des cheveux clairs, les petits replis (« up ») ne doivent pas faire de rayures
PALE_LIGHT = {'north': 0.8, 'up': 0.7, 'west': 0.9, 'east': 0.62, 'south': 0.56, 'down': 0.5}


def strand_paint(dark, light, t, lit=0.0, lit_map=PALE_LIGHT, edge=0.9, rim=None, rim_k=0.35, up_k=0.72):
    """Peinture d'une tranche de `strand` : couleur uniforme sur chaque face (aucun dégradé propre à la tranche),
    qui ne dépend que de l'orientation de la face, de t (un peu plus clair vers la pointe) et de `lit`. Bords
    longs légèrement assombris ; liseré `rim` sur les faces éclairées. Mise en cache."""
    key = (tuple(rgb(dark)), tuple(rgb(light)), round(t, 2), round(lit, 2), edge, tuple(rgb(rim)) if rim else None,
           up_k)
    if key in _STRAND_PAINTS:
        return _STRAND_PAINTS[key]
    dark, light = rgb(dark), rgb(light)
    rim = rgb(rim) if rim else None
    rnd = random.Random(hash(key) & 0xffff)

    def paint(ctx):
        px = ctx.img.load()
        k = min(1.0, max(0.0, lit_map[ctx.face] * (0.85 + 0.25 * t) + lit))
        c = mix(dark, light, k)
        if ctx.face == 'up':
            # le moteur éclaire les faces du dessus bien plus que les côtés : on assombrit les petits replis pour
            # qu'ils se fondent dans les flancs au lieu de dessiner des rayures claires (effet « escalier »)
            c = shade(c, up_k)
        side = ctx.face in ('north', 'south', 'east', 'west')
        for yy in range(ctx.h):
            for xx in range(ctx.w):
                cc = c
                if side and ctx.w > 2 and (xx == 0 or xx == ctx.w - 1):
                    cc = shade(c, edge)
                elif rim and side and ctx.face in ('north', 'west') and xx == 1:
                    cc = mix(c, rim, rim_k)
                d = rnd.randint(-2, 2)
                px[xx, yy] = (max(0, min(255, cc[0] + d)), max(0, min(255, cc[1] + d)),
                              max(0, min(255, cc[2] + d)), 255)
    _STRAND_PAINTS[key] = paint
    return paint


def _fin(m, bone, x, y, z, L, W, d, paint_for, segments):
    """Triangle plein vu de face (largeur W à la base, pointe à L), d'épaisseur d qui s'affine par tronçons.
    Les flancs sont deux longues plaques inclinées : des droites, pas des marches. Noyaux en retrait derrière."""
    import math
    half = W / 2
    theta = math.degrees(math.atan2(half, L))
    ls = math.hypot(half, L)
    tip = [x, y + L, z]
    for i, (h0, h1, t_seg, dk) in enumerate(segments):
        a_, b_ = (1 - h1) * ls, (1 - h0) * ls + (0.3 if i == 0 else 0.0)
        ds = d * dk
        side = paint_for(0.4 + 0.5 * h1)
        m.cube(bone, [x - t_seg, y + L - b_, z - ds / 2], [t_seg, b_ - a_, ds], side, pivot=tip, rotation=[0, 0, theta])
        m.cube(bone, [x, y + L - b_, z - ds / 2], [t_seg, b_ - a_, ds], side, pivot=tip, rotation=[0, 0, -theta])
        step = min(0.2, 1.8 * t_seg / W)
        hh = h0
        while hh < h1 - 1e-6:
            hh = min(h1, hh + step)
            if hh >= 0.97:
                break
            w = W * (1 - hh)
            dc = ds * 0.94
            m.cube(bone, [x - w / 2, y - 0.3, z - dc / 2], [w, hh * L + 0.3, dc], paint_for(hh * 0.8))


def spire(m, name, base, length, width, rx, rz, paint_for, ry=0, depth=None, bend=(0.0, 0.0), parent='hair',
          **_ignored):
    """Mèche continue sans escalier (itération 4) : deux « ailettes » triangulaires croisées à angle droit. Vue de
    face on voit un triangle de largeur `width`, de profil un triangle de largeur `depth` : la mèche s'effile dans
    les deux sens et se lit comme un volume pointu facetté sous tous les angles. Chaque ailette a des flancs
    droits (plaques inclinées, voir _fin). `bend` incline légèrement toute la mèche (degrés : x, z)."""
    m.bone(name, base, parent, rotation=[rx + bend[1] * 4, ry, rz - bend[0] * 4])
    x, y, z = base
    L, W = length, width
    D = depth if depth is not None else W * 0.5
    t_a = max(0.4, 0.1 * W + 0.05)
    th_a = max(0.9, 0.4 * W)                 # épaisseur de l'ailette principale (cachée de profil par l'autre)
    _fin(m, name, x, y, z, L, W, th_a, paint_for,
         ((0.0, 0.45, t_a, 1.0), (0.45, 0.75, 0.3, 0.55), (0.75, 1.0, 0.2, 0.3)))
    m.bone(name + '_b', base, name, rotation=[0, 90, 0])
    t_b = max(0.35, 0.1 * D + 0.05)
    th_b = max(0.8, 0.6 * th_a)
    _fin(m, name + '_b', x, y, z, L * 0.97, D, th_b, paint_for,
         ((0.0, 0.45, t_b, 1.0), (0.45, 0.75, 0.25, 0.55), (0.75, 1.0, 0.18, 0.3)))
