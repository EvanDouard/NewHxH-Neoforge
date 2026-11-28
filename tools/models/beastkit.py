"""
Outils communs aux animaux du bestiaire (modèles GeckoLib `beast_<id>`, rendus par BeastGeoRenderer).

Conventions : pixels Bedrock, pieds à y = 0, l'avant regarde vers -Z, +X = côté gauche de l'animal.
Os attendus par les animations : `body`, `head`, pattes `leg_fl`, `leg_fr`, `leg_bl`, `leg_br` (avant/arrière,
gauche/droite), `tail` (facultatif), ailes `wing_l` / `wing_r` (oiseaux). Animations : `idle`, `walk`, `attack`,
et `fly` pour ceux qui volent.
"""
from __future__ import annotations

import math
import random

from geolib import layered, material, only, rgb, shade


def fur(base, noise=9, seed=0, streaks=0.0, dark=None):
    """Fourrure : la peinture « manga » plus des mèches verticales sombres semées au hasard."""
    base = rgb(base)
    dark = rgb(dark) if dark else shade(base, 0.72)
    mat = material(base, top=1.1, bottom=0.78, edge=0.8, noise=noise, grad=0.18, seed=seed)

    def paint(ctx):
        mat(ctx)
        if streaks <= 0:
            return
        rnd = random.Random(hash((ctx.face, ctx.w, ctx.h, seed, 'fur')) & 0xffffffff)
        px = ctx.img.load()
        n = int(ctx.w * ctx.h * streaks / 6)
        for _ in range(n):
            x, y = rnd.randrange(ctx.w), rnd.randrange(ctx.h)
            L = rnd.randint(2, 4)
            for k in range(L):
                yy = y + k if ctx.face not in ('up', 'down') else y
                xx = x if ctx.face not in ('up', 'down') else min(ctx.w - 1, x + k)
                if 0 <= yy < ctx.h and px[xx, yy][3]:
                    px[xx, yy] = dark[:3] + (255,)
    return paint


def belly(base, light, faces=('down',)):
    """Ventre / poitrail plus clair sur certaines faces."""
    return layered(fur(base, streaks=0.5), only(faces, fur(light, noise=5)))


def on_face(face, base_painter, draw):
    """Peint `draw(ctx)` par-dessus une face donnée (yeux, museau…)."""
    def paint(ctx):
        base_painter(ctx)
        if ctx.face == face:
            draw(ctx)
    return paint


def eyes_drawer(white, iris, pupil, y0=0.25, y1=0.55, x0=0.08, x1=0.32, glint=True, brow=None):
    """Deux yeux symétriques sur une face (coordonnées relatives 0..1)."""
    white, iris, pupil = rgb(white), rgb(iris), rgb(pupil)

    def draw(ctx):
        d, W, H = ctx.draw, ctx.w, ctx.h
        for (a, b) in ((x0, x1), (1 - x1, 1 - x0)):
            box = [round(a * W), round(y0 * H), round(b * W) - 1, round(y1 * H) - 1]
            d.ellipse(box, fill=white)
            cx, cy = (box[0] + box[2]) / 2, (box[1] + box[3]) / 2
            rx, ry = (box[2] - box[0]) * 0.32, (box[3] - box[1]) * 0.38
            d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=iris)
            d.ellipse([cx - rx * 0.5, cy - ry * 0.55, cx + rx * 0.5, cy + ry * 0.55], fill=pupil)
            if glint:
                d.rectangle([round(cx - rx * 0.55), round(cy - ry * 0.7), round(cx - rx * 0.1), round(cy - ry * 0.3)],
                            fill=(255, 255, 255, 255))
            if brow:
                d.line([box[0], box[1] - 1, box[2], box[1] + (box[3] - box[1]) * 0.15], fill=rgb(brow), width=max(1, H // 14))
    return draw


def quad_leg(m, name, x, y_top, z, w, h, d, paint, paw_paint=None, parent='root', claws=None, paw_len=None):
    """Patte droite : os au sommet, cylindre + pied un peu plus large ; griffes facultatives."""
    m.bone(name, [x, y_top, z], parent)
    m.cube(name, [x - w / 2, 2.0, z - d / 2], [w, y_top - 2.0, d], paint)
    pl = paw_len or d + 1.0
    m.cube(name, [x - w / 2 - 0.3, 0, z - pl + d / 2], [w + 0.6, 2.2, pl], paw_paint or paint)
    if claws:
        for i in range(3):
            cx = x - w / 2 + (i + 0.5) * w / 3
            m.cube(name, [cx - 0.3, 0, z - pl + d / 2 - 0.9], [0.6, 0.9, 1.0], claws)


def quad_animations(legs=('leg_fl', 'leg_fr', 'leg_bl', 'leg_br'), swing=26, walk_len=0.9, head_bob=3,
                    tail_wag=10, attack='bite', extra_idle=None, extra_walk=None):
    fl, fr, bl, br = legs
    half = walk_len / 2

    def sw(a):
        return {'0.0': [a, 0, 0], f'{half:.2f}': [-a, 0, 0], f'{walk_len:.2f}': [a, 0, 0]}

    idle = {
        'body': {'rotation': {'0.0': [0, 0, 0], '1.5': [-1.2, 0, 0], '3.0': [0, 0, 0]},
                 'scale': {'0.0': [1, 1, 1], '1.5': [1.01, 1.02, 1.0], '3.0': [1, 1, 1]}},
        'tail': {'rotation': {'0.0': [0, -tail_wag * 0.5, 0], '1.5': [0, tail_wag * 0.5, 0], '3.0': [0, -tail_wag * 0.5, 0]}},
    }
    if extra_idle:
        idle.update(extra_idle)
    walk = {
        fl: {'rotation': sw(swing)}, br: {'rotation': sw(swing)},
        fr: {'rotation': sw(-swing)}, bl: {'rotation': sw(-swing)},
        'body': {'position': {'0.0': [0, 0, 0], f'{walk_len / 4:.2f}': [0, 0.6, 0], f'{half:.2f}': [0, 0, 0],
                              f'{walk_len * 0.75:.2f}': [0, 0.6, 0], f'{walk_len:.2f}': [0, 0, 0]}},
        'tail': {'rotation': {'0.0': [0, -tail_wag, 0], f'{half:.2f}': [0, tail_wag, 0], f'{walk_len:.2f}': [0, -tail_wag, 0]}},
    }
    if extra_walk:
        walk.update(extra_walk)
    if attack == 'bite':
        atk = {'head': {'rotation': {'0.0': [0, 0, 0], '0.12': [-18, 0, 0], '0.25': [16, 0, 0], '0.5': [0, 0, 0]}},
               'jaw': {'rotation': {'0.0': [0, 0, 0], '0.12': [30, 0, 0], '0.25': [0, 0, 0], '0.5': [0, 0, 0]}},
               'body': {'position': {'0.0': [0, 0, 0], '0.2': [0, 0, -2], '0.5': [0, 0, 0]}}}
    elif attack == 'swipe':
        atk = {fr: {'rotation': {'0.0': [0, 0, 0], '0.15': [-70, 0, 10], '0.3': [20, 0, 0], '0.55': [0, 0, 0]}},
               'body': {'rotation': {'0.0': [0, 0, 0], '0.15': [-12, 0, 0], '0.3': [4, 0, 0], '0.55': [0, 0, 0]}},
               'jaw': {'rotation': {'0.0': [0, 0, 0], '0.15': [28, 0, 0], '0.55': [0, 0, 0]}}}
    else:  # charge (tête baissée)
        atk = {'head': {'rotation': {'0.0': [0, 0, 0], '0.15': [26, 0, 0], '0.3': [-22, 0, 0], '0.55': [0, 0, 0]}},
               'body': {'position': {'0.0': [0, 0, 0], '0.15': [0, 0, 1], '0.3': [0, 0, -3], '0.55': [0, 0, 0]}}}
    return {
        'idle': {'loop': True, 'animation_length': 3.0, 'bones': idle},
        'walk': {'loop': True, 'animation_length': walk_len, 'bones': walk},
        'attack': {'loop': False, 'animation_length': 0.55 if attack != 'bite' else 0.5, 'bones': atk},
    }


def arc(n, a0, a1):
    return [a0 + (a1 - a0) * i / max(1, n - 1) for i in range(n)]


def deg(x):
    return math.degrees(x)


def biped_animations(flap=70, walk_len=0.7, step=24, attack='peck', arms=False, wings=('wing_l', 'wing_r'), fly=True):
    """Oiseaux et bipèdes : os `leg_l`, `leg_r`, `wing_l`, `wing_r` (ou bras), `body`, `head`, `tail`."""
    h = walk_len / 2
    sw = lambda a: {'0.0': [a, 0, 0], f'{h:.2f}': [-a, 0, 0], f'{walk_len:.2f}': [a, 0, 0]}
    idle = {
        'body': {'rotation': {'0.0': [0, 0, 0], '1.5': [-1.5, 0, 0], '3.0': [0, 0, 0]}},
        'head': {'rotation': {'0.0': [0, 0, 0], '1.0': [0, 8, 0], '1.6': [0, 8, 0], '2.2': [0, -6, 0], '3.0': [0, 0, 0]}},
        'tail': {'rotation': {'0.0': [0, 0, 0], '1.5': [4, 0, 0], '3.0': [0, 0, 0]}},
    }
    walk = {
        'leg_l': {'rotation': sw(step)}, 'leg_r': {'rotation': sw(-step)},
        'body': {'rotation': {'0.0': [0, 0, -3], f'{h:.2f}': [0, 0, 3], f'{walk_len:.2f}': [0, 0, -3]},
                 'position': {'0.0': [0, 0, 0], f'{h / 2:.2f}': [0, 0.5, 0], f'{h:.2f}': [0, 0, 0],
                              f'{h * 1.5:.2f}': [0, 0.5, 0], f'{walk_len:.2f}': [0, 0, 0]}},
        'head': {'position': {'0.0': [0, 0, 0], f'{h:.2f}': [0, 0, -0.8], f'{walk_len:.2f}': [0, 0, 0]}},
    }
    if arms:
        walk[wings[0]] = {'rotation': sw(-step * 0.8)}
        walk[wings[1]] = {'rotation': sw(step * 0.8)}
    lo, hi = 70, 70 + flap
    fly_bones = {
        'wing_l': {'rotation': {'0.0': [0, 0, -lo], '0.25': [0, 0, -hi], '0.5': [0, 0, -lo]}},
        'wing_r': {'rotation': {'0.0': [0, 0, lo], '0.25': [0, 0, hi], '0.5': [0, 0, lo]}},
        'body': {'rotation': {'0.0': [-50, 0, 0], '0.5': [-50, 0, 0]},
                 'position': {'0.0': [0, 0, 0], '0.25': [0, 1, 0], '0.5': [0, 0, 0]}},
        'head': {'rotation': {'0.0': [40, 0, 0], '0.5': [40, 0, 0]}},
        'tail': {'rotation': {'0.0': [-25, 0, 0], '0.5': [-25, 0, 0]}},
        'leg_l': {'rotation': {'0.0': [-60, 0, 0], '0.5': [-60, 0, 0]}},
        'leg_r': {'rotation': {'0.0': [-60, 0, 0], '0.5': [-60, 0, 0]}},
    }
    if attack == 'peck':
        atk = {'head': {'rotation': {'0.0': [0, 0, 0], '0.12': [-25, 0, 0], '0.25': [40, 0, 0], '0.5': [0, 0, 0]}},
               'body': {'rotation': {'0.0': [0, 0, 0], '0.25': [12, 0, 0], '0.5': [0, 0, 0]}},
               'wing_l': {'rotation': {'0.0': [0, 0, 0], '0.2': [0, 0, -35], '0.5': [0, 0, 0]}},
               'wing_r': {'rotation': {'0.0': [0, 0, 0], '0.2': [0, 0, 35], '0.5': [0, 0, 0]}}}
    else:  # griffes / coup de bras
        atk = {wings[1]: {'rotation': {'0.0': [0, 0, 0], '0.15': [-120, 0, -10], '0.3': [20, 0, 0], '0.5': [0, 0, 0]}},
               'body': {'rotation': {'0.0': [0, 0, 0], '0.15': [-6, -12, 0], '0.3': [8, 10, 0], '0.5': [0, 0, 0]}}}
    out = {
        'idle': {'loop': True, 'animation_length': 3.0, 'bones': idle},
        'walk': {'loop': True, 'animation_length': walk_len, 'bones': walk},
        'attack': {'loop': False, 'animation_length': 0.5, 'bones': atk},
    }
    if fly:
        out['fly'] = {'loop': True, 'animation_length': 0.5, 'bones': fly_bones}
    return out


def seg(m, bone, x, p0, p1, w, d, paint, over=0.6):
    """Pavé tendu entre deux points (y, z) du plan de profil, centré en x (cou, membres articulés sans jour)."""
    (y0, z0), (y1, z1) = p0, p1
    L = math.hypot(y1 - y0, z1 - z0) + over
    th = math.degrees(math.atan2(-(z1 - z0), (y0 - y1)))
    m.cube(bone, [x - w / 2, y0 - L + over / 2, z0 - d / 2], [w, L, d], paint, pivot=[x, y0, z0], rotation=[th, 0, 0])
