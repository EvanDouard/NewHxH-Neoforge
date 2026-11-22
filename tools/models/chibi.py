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
