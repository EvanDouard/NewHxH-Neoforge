"""
Poses tenues communes aux personnages humanoïdes (squelettes `adult.skeleton` et `chibi.skeleton`) :

- `meditate` : assis en tailleur, cuisses écartées à l'horizontale, tibias croisés devant, mains posées sur les
  genoux, dos droit, respiration lente (4 s) ;
- `pray` (prière de gratitude de Netero) : seiza — à genoux assis sur les talons, paumes jointes devant la
  poitrine (gasshō) ; toutes les 6 s, une inclinaison lente du buste et de la tête, puis le retour.

Les angles sont calculés pour chaque modèle (recherche numérique sur ses vrais pivots) : les genoux touchent le
sol, les tibias se croisent, les mains se rejoignent, quelles que soient les proportions (chibi ou adulte).
Rotations dans la convention d'aperçu de geolib (converties à l'enregistrement comme les autres animations).
"""
from __future__ import annotations

import itertools
import math

import numpy as np

from geolib import _affine

LEG = ('right_leg', 'right_shin', 'left_leg', 'left_shin')


def _chain(model, pose, names):
    """Matrices monde des os `names` (parents compris) pour une pose {os: rotation ajoutée}."""
    cache = {}

    def m(bn):
        if bn in cache:
            return cache[bn]
        b = model.bones[bn]
        rot = list(b.rotation or [0, 0, 0])
        if bn in pose:
            rot = [rot[i] + pose[bn][i] for i in range(3)]
        M = _affine(rot, b.pivot)
        if b.parent:
            M = m(b.parent) @ M
        cache[bn] = M
        return M
    return {n: m(n) for n in names}


def _pt(M, p):
    return (M @ np.array([p[0], p[1], p[2], 1.0]))[:3]


def _search(f, ranges, start=None):
    """Minimise f par descente par coordonnées à pas décroissants (rapide, déterministe)."""
    x = list(start) if start else [(lo + hi) / 2 for lo, hi in ranges]
    best = f(x)
    for step in (24, 12, 6, 3, 1.5, 0.5):
        improved = True
        while improved:
            improved = False
            for i, (lo, hi) in enumerate(ranges):
                for d in (-step, step):
                    y = list(x)
                    y[i] = min(hi, max(lo, y[i] + d))
                    v = f(y)
                    if v < best - 1e-9:
                        best, x, improved = v, y, True
    return [float(round(v, 1)) for v in x]


def _bone(model, n):
    return model.bones[n]


def pose_animations(model):
    names = model.bones
    need = ('body', 'torso', 'head', 'right_arm', 'right_forearm', 'right_hand', 'left_arm', 'left_forearm',
            'left_hand') + LEG
    if not all(n in names for n in need):
        return {}
    hip = _bone(model, 'right_leg').pivot[1]
    knee = _bone(model, 'right_shin').pivot[1]
    thigh = hip - knee
    lx = abs(_bone(model, 'right_leg').pivot[0])
    sh_y = _bone(model, 'right_arm').pivot[1]
    wrist = _bone(model, 'right_hand').pivot[1]
    elbow = _bone(model, 'right_forearm').pivot[1]
    upper, fore = sh_y - elbow, elbow - wrist
    leg_w = 1.0 + 0.12 * thigh                      # demi-épaisseur de la cuisse, à peu près

    # ---------- tailleur ----------
    tx, ty = 86.0, 38.0                              # cuisse : presque horizontale, écartée
    knee_h = leg_w + 0.2
    m_drop = hip - (knee_h + thigh * math.cos(math.radians(tx)))
    med = {'right_leg': [tx, ty, 0], 'left_leg': [tx, -ty, 0]}
    # tibia : replié vers l'intérieur, à plat, le pied passe devant l'autre jambe
    M = _chain(model, med, ['right_shin'])['right_shin']
    kpt = _pt(M, _bone(model, 'right_shin').pivot)
    target = kpt + np.array([1, 0, 0.32]) / np.linalg.norm([1, 0, 0.32]) * thigh * 0.95
    target[1] -= 0.35                                # le droit passe juste au-dessus du gauche

    def f_r(v):
        p = dict(med, right_shin=[v[0], v[1], 0])
        Ms = _chain(model, p, ['right_shin'])['right_shin']
        foot = _pt(Ms, [kpt_local[0], kpt_local[1] - thigh * 0.95, kpt_local[2]])
        return float(np.sum((foot - target) ** 2))
    kpt_local = _bone(model, 'right_shin').pivot
    rs = _search(f_r, [(-180, 0), (-180, 180)], start=[-60, 60])
    ls = [rs[0] - 4, -rs[1]]                         # symétrique, un peu plus bas : il passe dessous
    med['right_shin'] = [rs[0], rs[1], 0]
    med['left_shin'] = [ls[0], ls[1], 0]

    # mains posées sur les genoux (le genou réel, après la descente)
    Mk = _chain(model, med, ['right_shin'])['right_shin']
    knee_w = _pt(Mk, kpt_local)
    hand_t = knee_w + np.array([0.6, 1.1 + 0.4, -0.2])          # au-dessus du genou, un peu vers l'intérieur

    def f_arm(v, side='right'):
        p = {f'{side}_arm': [v[0], 0, v[1]], f'{side}_forearm': [v[2], v[3], 0]}
        Ms = _chain(model, p, [f'{side}_hand'])[f'{side}_hand']
        hp = _pt(Ms, _bone(model, f'{side}_hand').pivot)
        return float(np.sum((hp - hand_t) ** 2))
    # genoux et mains sont calculés dans le même repère (avant la descente du corps), donc sans correction
    ra = _search(f_arm, [(-10, 80), (-30, 30), (0, 120), (-60, 60)], start=[20, 0, 50, 0])
    med['right_arm'] = [ra[0], 0, ra[1]]
    med['right_forearm'] = [ra[2], ra[3], 0]
    med['left_arm'] = [ra[0], 0, -ra[1]]
    med['left_forearm'] = [ra[2], -ra[3], 0]

    # ---------- seiza + gasshō ----------
    sx_ = 82.0
    p_drop = hip - (leg_w + 0.1 + thigh * math.cos(math.radians(sx_)))
    pray = {'right_leg': [sx_, 4, 0], 'left_leg': [sx_, -4, 0],
            'right_shin': [-(sx_ + 88), 0, 0], 'left_shin': [-(sx_ + 88), 0, 0]}
    chest = np.array([0.0, sh_y - upper * 0.55, -(upper * 0.75 + 0.6)])

    def f_g(v):
        p = {'right_arm': [v[0], v[1], v[2]], 'right_forearm': [v[3], v[4], 0]}
        Ms = _chain(model, p, ['right_hand'])['right_hand']
        hp = _pt(Ms, _bone(model, 'right_hand').pivot)
        tip = _pt(Ms, [hp[0] * 0 + _bone(model, 'right_hand').pivot[0], wrist - fore * 0.6, 0])
        # poignet devant la poitrine, au centre ; la main dressée (doigts plus haut que le poignet)
        want = chest + np.array([-0.7, 0, 0])
        return float(np.sum((hp - want) ** 2)) + 0.4 * max(0.0, hp[1] - tip[1] + fore * 0.3) ** 2
    ga = _search(f_g, [(0, 90), (-40, 40), (-30, 30), (0, 150), (-80, 80)], start=[30, 0, 0, 90, 0])
    pray['right_arm'] = [ga[0], ga[1], ga[2]]
    pray['right_forearm'] = [ga[3], ga[4], 0]
    pray['left_arm'] = [ga[0], -ga[1], -ga[2]]
    pray['left_forearm'] = [ga[3], -ga[4], 0]

    def frames(pose, keys):
        return {bn: {'rotation': {k: [round(a, 2) for a in rot] for k in keys}} for bn, rot in pose.items()}

    mb = frames(med, ('0.0', '4.0'))
    mb['body'] = {'position': {'0.0': [0, -round(m_drop, 2), 0], '4.0': [0, -round(m_drop, 2), 0]}}
    mb['torso'] = {'rotation': {'0.0': [0, 0, 0], '2.0': [-2.0, 0, 0], '4.0': [0, 0, 0]},
                   'scale': {'0.0': [1, 1, 1], '2.0': [1.02, 1.015, 1.03], '4.0': [1, 1, 1]}}
    mb['head'] = {'rotation': {'0.0': [-7, 0, 0], '2.0': [-5, 0, 0], '4.0': [-7, 0, 0]}}

    pb = frames(pray, ('0.0', '6.0'))
    pb['body'] = {'position': {'0.0': [0, -round(p_drop, 2), 0], '6.0': [0, -round(p_drop, 2), 0]}}
    # inclinaison : lente descente (1,2 s), tenue, lente remontée, puis repos et respiration
    pb['torso'] = {'rotation': {'0.0': [0, 0, 0], '0.4': [0, 0, 0], '1.6': [-24, 0, 0], '2.4': [-24, 0, 0],
                                '3.6': [0, 0, 0], '4.8': [-1.5, 0, 0], '6.0': [0, 0, 0]}}
    pb['head'] = {'rotation': {'0.0': [-6, 0, 0], '0.4': [-6, 0, 0], '1.6': [-20, 0, 0], '2.4': [-20, 0, 0],
                               '3.6': [-6, 0, 0], '6.0': [-6, 0, 0]}}
    return {
        'meditate': {'loop': True, 'animation_length': 4.0, 'bones': mb},
        'pray': {'loop': True, 'animation_length': 6.0, 'bones': pb},
    }
