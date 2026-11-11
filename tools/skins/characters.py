"""
Apparences des personnages (voir skinlib.py). Chaque fonction dessine un personnage et renvoie
(skin, apparence, cubes en plus). Lancer ce fichier régénère les textures, les JSON et les aperçus.
"""
from __future__ import annotations

import json
import os
import sys

from skinlib import dump_look, Extras, Skin, SIDES, mix, rgb, shade, style, preview_sheet

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hxh')
PREVIEWS = os.path.join(ROOT, 'build', 'skin-previews')

CHARACTERS = {}


def character(fn):
    CHARACTERS[fn.__name__] = fn
    return fn


# ---------------------------------------------------------------------------------------------------------------
# Morceaux réutilisables


def skin_tone(base):
    return {'S': base, 's': shade(base, 0.86), 'z': shade(base, 0.74), 'l': shade(base, 1.08)}


def limb_skin(skin, part, tone, layer='base'):
    skin.fill(part, tone['S'], layer, noise=3)
    skin.paint(part, lambda f, x, y, w, h, gy: tone['s'] if f in ('back', 'right' if 'right' in part else 'left') else None,
               layer, faces=SIDES, noise=3)


def shoes(skin, color, height=2, sole=None):
    sole = sole or shade(color, 0.6)
    for leg in ('right_leg', 'left_leg'):
        skin.bands(leg, [(32 - height, 31, color), (31, 32, sole)], noise=3)
        skin.fill(leg, sole, faces=('bottom',))


def clothes(skin, part, color, gy0=0, gy1=99, layer='base', noise=4, faces=SIDES, dark_back=0.9):
    def fn(f, x, y, w, h, gy):
        if gy is None or not (gy0 <= gy < gy1):
            return None
        c = rgb(color)
        if f == 'back':
            c = shade(c, dark_back)
        return c
    skin.paint(part, fn, layer, faces, noise)


def top_fill(skin, part, color, layer='base', noise=3):
    skin.fill(part, color, layer, faces=('top',), noise=noise)


def save_character(cid, skin, look, extras):
    skin.save(os.path.join(ASSETS, 'textures', 'entity', 'npc', cid + '.png'))
    extras_img = None
    if extras is not None and extras.cubes:
        rel = 'textures/entity/npc/' + cid + '_extra.png'
        extras.img.save(os.path.join(ASSETS, rel))
        look['extras'] = extras.json('hxh:' + rel)
        extras_img = extras.img
    look.setdefault('skin', 'hxh:textures/entity/npc/' + cid + '.png')
    os.makedirs(os.path.join(ASSETS, 'looks'), exist_ok=True)
    with open(os.path.join(ASSETS, 'looks', cid + '.json'), 'w', encoding='utf-8') as f:
        f.write(dump_look(look))
    preview_sheet(skin.img, look, extras_img, os.path.join(PREVIEWS, cid + '.png'), cid)


# ---------------------------------------------------------------------------------------------------------------
# Netero : vieux maître, crâne blanc, chignon, longue barbe, sourcils tombants, chemise blanche « 心 »


@character
def netero():
    s = Skin(seed=11)
    t = skin_tone('#e2b48c')
    hair, hair_s, hair_d = rgb('#f4f3ee'), rgb('#cfd2d8'), rgb('#a9adb6')
    pal = {**t, 'W': hair, 'w': hair_s, 'g': hair_d, 'k': '#2a1c14', 'e': '#5b3a24', 'm': '#a86a55', 'r': '#e09a86'}
    s.fill('head', t['S'], noise=3)
    s.grid('head', 'front', [
        'llllllll',
        'SlllllSS',
        'WWWSSWWW',
        'wkkSSkkw',
        'SSSzzSSS',
        'SrWzzWrS',
        'SWWWWWWS',
        'SSWWWWSS',
    ], pal, noise=2)
    side = ['WWSSSSSS', 'WWSSSSSS', 'WWWSSzSS', 'WWWSzzSS', 'WWWSSzSS', 'WWWWSSSS', 'WWWWWSSW', 'WWWWWWSS']
    s.grid('head', 'right', side, pal, noise=2)
    s.grid('head', 'left', [r[::-1] for r in side], pal, noise=2)
    s.grid('head', 'back', ['SSSSSSSS', 'SSSSSSSS', 'WWWWWWWW', 'WWWWWWWW', 'WwWWWwWW', 'WWwWWWwW', 'WWWWWWWW', 'wWWwwWWw'], pal, noise=3)
    s.grid('head', 'top', ['wwwwwwww', 'WSSSSSSW', 'WSllllSW', 'WSllllSW', 'WSlllSSW', 'WSSSSSSW', 'SSSSSSSS', 'SSSSSSSS'], pal, noise=2)
    s.grid('head', 'bottom', ['SSSSSSSS'] * 2 + ['SSWWWWSS'] * 3 + ['SSSSSSSS'] * 3, pal)
    # couche 2 : bouts des sourcils qui tombent sur les côtés
    s.grid('head', 'front', ['........', '........', 'W......W', 'W......W', '........', '........', '........', '........'], pal, layer='overlay')

    shirt, shirt_s, ink = rgb('#f3efe4'), rgb('#d8d2c3'), rgb('#1e1a18')
    pants, pants_s = rgb('#3a3f52'), rgb('#2c3040')
    clothes(s, 'body', shirt, 8, 18)
    clothes(s, 'body', pants, 18, 20)
    top_fill(s, 'body', shirt)
    s.grid('body', 'front', [
        'sSSSSSSs',
        'oSssSSSo',
        'oSSSSSSo',
        'oSSSSSSo',
        'oSSKSSSo',
        'oSSSKSSo',
        'oKSSSSKo',
        'KSKSSSKo',
        'oSSKKKSo',
        'oSSSSSSo',
        'bbbbbbbb',
        'PPPPPPPP',
    ], {'S': shirt, 's': t['s'], 'o': shirt_s, 'K': ink, 'b': '#4b3a2a', 'P': pants}, noise=3)
    s.grid('body', 'front', ['.SS.', '..S.'], {'S': t['s']}, x0=2)  # col ouvert
    for arm in ('right_arm', 'left_arm'):
        limb_skin(s, arm, t)
        clothes(s, arm, shirt, 8, 13)
        top_fill(s, arm, shirt)
        clothes(s, arm, shirt_s, 13, 14)
        clothes(s, arm, shirt, 8, 14, layer='overlay')
    for leg in ('right_leg', 'left_leg'):
        clothes(s, leg, pants, 20, 32, noise=4)
        clothes(s, leg, pants_s, 20, 21)
        clothes(s, leg, pants, 27, 30, layer='overlay')  # bas du pantalon large
    shoes(s, '#6b4a2e', 1, sole='#3a2616')

    ex = Extras()
    beard = style(hair, outline=0.8, grad=0.25)
    # barbe longue sous le menton, qui s'affine en descendant sur la poitrine
    ex.cube('head', [-2, 0, -4.4], [4, 3, 2], beard)
    ex.cube('head', [-1.5, 2.6, -4.2], [3, 3, 2], beard, rot=(10, 0, 0), pivot=[0, 2.6, -3.5])
    ex.cube('head', [-1, 5.2, -4.0], [2, 2, 1], style(hair_s), rot=(18, 0, 0), pivot=[0, 2.6, -3.5])
    # chignon (mèche nouée sur le sommet arrière)
    ex.cube('head', [-1, -9.5, 0.5], [2, 2, 3], style(hair), rot=(-25, 0, 0), pivot=[0, -8, 1])
    ex.cube('head', [-0.5, -10.5, 2.2], [1, 2, 2], style(hair_s), rot=(-50, 0, 0), pivot=[0, -9, 2])
    # longs lobes d'oreille
    lobe = style(t['S'], outline=0.85, grad=0.1)
    ex.cube('head', [-4.6, -4, -0.5], [1, 4, 1], lobe)
    ex.cube('head', [3.6, -4, -0.5], [1, 4, 1], lobe)
    return s, {'slim': False, 'scale': [0.95, 0.93, 0.95]}, ex


def suit(s, jacket, shirt, tie, pants=None, shoe='#1b1b20', lapel=None, tie_len=7, open_jacket=True):
    """Costume : veste, chemise, cravate, pantalon, chaussures."""
    pants = pants or jacket
    lapel = lapel or shade(jacket, 0.78)
    clothes(s, 'body', jacket, 8, 20)
    top_fill(s, 'body', jacket)
    rows = []
    for y in range(12):
        r = list('JJJJJJJJ')
        if open_jacket and y < 9:
            half = max(0, 2 - y // 3)
            for x in range(3 - half, 5 + half):
                r[x] = 'W'
            r[2 - half] = 'L'
            r[5 + half] = 'L'
        if 1 <= y <= tie_len:
            r[3] = r[4] = 'T'
            if y == 1:
                r[3] = 't'
            if y == tie_len:
                r[3], r[4] = 'W' if open_jacket else 'J', 't'
        if y == 9:
            r[3] = r[4] = 'B'
        rows.append(''.join(r))
    rows[0] = rows[0][:3] + 'WW' + rows[0][5:]
    s.grid('body', 'front', rows, {'J': jacket, 'W': shirt, 'L': lapel, 'T': tie, 't': shade(tie, 0.8), 'B': shade(jacket, 0.6)}, noise=3)
    for arm in ('right_arm', 'left_arm'):
        clothes(s, arm, jacket, 8, 19)
        top_fill(s, arm, jacket)
        clothes(s, arm, shirt, 19, 20)
        clothes(s, arm, jacket, 17, 19, layer='overlay')
    for leg in ('right_leg', 'left_leg'):
        clothes(s, leg, pants, 20, 32)
        clothes(s, leg, shade(pants, 0.85), 20, 21)
    shoes(s, shoe, 2)


def hands(s, tone, gy0=19):
    for arm in ('right_arm', 'left_arm'):
        s.paint(arm, lambda f, x, y, w, h, gy: (tone['s'] if f == 'back' else tone['S']) if gy is not None and gy >= gy0 else None, faces=SIDES, noise=2)
        s.fill(arm, tone['S'], faces=('bottom',))


# ---------------------------------------------------------------------------------------------------------------
# Satotz : grand, maigre, cheveux lavande coiffés vers le haut, grosse moustache bouclée, pas de bouche


@character
def satotz():
    s = Skin(seed=12)
    t = skin_tone('#f0d2b4')
    hair = rgb('#9b7fc4')
    pal = {**t, 'H': hair, 'h': shade(hair, 0.8), 'd': shade(hair, 0.62), 'L': shade(hair, 1.15),
           'k': '#1d1726', 'w': '#ffffff', 'b': shade(hair, 0.55)}
    s.fill('head', t['S'], noise=3)
    s.grid('head', 'front', [
        'HHLLLLHH',
        'hHHHHHHh',
        'SbbSSbbS',
        'SkwSSwkS',
        'SSSzzSSS',
        'SSSSSSSS',
        'SSSSSSSS',
        'sSSSSSSs',
    ], pal, noise=2)
    side = ['HHHHHHHH', 'HHHHHHhh', 'HHHHSSSS', 'hHHSSzSS', 'hHHSzzSS', 'hhHSSSSH', 'dhSSSSSS', 'dSSSSSSS']
    s.grid('head', 'right', side, pal, noise=3)
    s.grid('head', 'left', [r[::-1] for r in side], pal, noise=3)
    s.grid('head', 'back', ['HHHHHHHH', 'HHHHHHHH', 'HhHHHHhH', 'HHHHHHHH', 'hHHhhHHh', 'hhHHHHhh', 'dhhhhhhd', 'SddddddS'], pal, noise=3)
    s.fill('head', hair, faces=('top',), noise=4)
    s.grid('head', 'right', ['H.......', 'H.......', 'h.......'], pal, layer='overlay', y0=2)
    s.grid('head', 'left', ['.......H', '.......H', '.......h'], pal, layer='overlay', y0=2)
    s.grid('head', 'back', ['HHHHHHHH', 'HHHHHHHH', 'hHHHHHHh', 'hhHHHHhh'], pal, layer='overlay', y0=2)
    suit(s, '#262838', '#f2f2f4', '#8467b5', shoe='#141418')
    hands(s, t)
    s.grid('body', 'front', ['.WW.'], {'W': '#f2f2f4'}, x0=2)

    ex = Extras()
    hs = style(hair, outline=0.7)
    # coiffure : une vague qui remonte au-dessus du front et retombe vers l'arrière
    ex.cube('head', [-4.1, -9.0, -4.3], [8.2, 1.5, 5], hs, rot=(-14, 0, 0), pivot=[0, -8, -4])
    ex.cube('head', [-3.4, -10.0, -4.0], [6.8, 1.2, 2], style(shade(hair, 1.1), outline=0.7), rot=(-24, 0, 0), pivot=[0, -9, -4])
    # moustache : barre sous le nez et pointes bouclées vers le haut (on ne voit pas de bouche)
    ms = style(hair, outline=0.7, grad=0.1)
    ex.cube('head', [-3, -3.3, -4.6], [6, 1.5, 1], ms)
    ex.cube('head', [-4.6, -4.2, -4.55], [1.6, 1.4, 1], ms, rot=(0, 0, 30), pivot=[-3, -2.8, -4.3])
    ex.cube('head', [3.0, -4.2, -4.55], [1.6, 1.4, 1], ms, rot=(0, 0, -30), pivot=[3, -2.8, -4.3])
    return s, {'slim': True, 'scale': [0.9, 1.12, 0.9]}, ex


# ---------------------------------------------------------------------------------------------------------------
# Menchi : cheveux vert émeraude noués en cinq petites couettes, résille noire sur haut de bikini, short en jean


@character
def menchi():
    s = Skin(slim=True, seed=13)
    t = skin_tone('#f6d6bd')
    hair = rgb('#2fae72')
    pal = {**t, 'H': hair, 'h': shade(hair, 0.78), 'd': shade(hair, 0.58), 'L': shade(hair, 1.2),
           'k': '#1a1420', 'i': '#7a4fa0', 'w': '#ffffff', 'm': '#d0566c', 'r': '#f2a5a0'}
    s.fill('head', t['S'], noise=2)
    s.grid('head', 'front', [
        'HHLHHLHH',
        'HHhHHhHH',
        'HhSHHShH',
        'HkiSSikH',
        'hwkSSkwh',
        'hrSSSSrh',
        'dSSmmSSd',
        'dSSSSSSd',
    ], pal, noise=2)
    side = ['HHHHHHHH', 'HHHHHHHH', 'HHHHhHHH', 'HHHHSSHH', 'hHHHSSSh', 'hhHHSSSh', 'dhhSSSSd', 'ddSSSSSd']
    s.grid('head', 'right', side, pal, noise=3)
    s.grid('head', 'left', [r[::-1] for r in side], pal, noise=3)
    s.grid('head', 'back', ['HHHHHHHH', 'HLHHHHLH', 'HHHHHHHH', 'hHHhhHHh', 'hhHHHHhh', 'hhhHHhhh', 'dhhhhhhd', 'SddddddS'], pal, noise=3)
    s.fill('head', hair, faces=('top',), noise=4)
    s.grid('head', 'front', ['HHHHHHHH', 'H.H..H.H', 'h......h'], pal, layer='overlay')
    s.grid('head', 'right', ['HHHHHHHH', 'HHHHHHHH', 'hHHHH...', 'hhH.....'], pal, layer='overlay')
    s.grid('head', 'left', ['HHHHHHHH', 'HHHHHHHH', '...HHHHh', '.....Hhh'], pal, layer='overlay')
    s.grid('head', 'back', ['HHHHHHHH', 'HHHHHHHH', 'hHHHHHHh', 'hhHHHHhh', 'dhhhhhhd'], pal, layer='overlay')
    s.fill('head', hair, layer='overlay', faces=('top',), noise=4)

    bikini, mesh, denim = rgb('#e8559a'), rgb('#18161c'), rgb('#4f79b8')
    # peau partout, puis maillot, short, et la résille noire par-dessus (couche 2)
    for p in ('body', 'right_arm', 'left_arm', 'right_leg', 'left_leg'):
        limb_skin(s, p, t)
    s.grid('body', 'front', [
        'SSSSSSSS', 'SSSSSSSS', 'PPPSSPPP', 'pPPSSPPp', 'SppSSppS', 'SSSSSSSS',
        'SSSzzSSS', 'SSSSSSSS', 'bbbbbbbb', 'DDDDDDDD', 'DDDdDDDD', 'DDDdDDDD',
    ], {**t, 'P': bikini, 'p': shade(bikini, 0.8), 'b': '#3c2a20', 'D': denim, 'd': shade(denim, 0.75)}, noise=2)
    s.grid('body', 'back', ['SSSSSSSS', 'SSSSSSSS', 'PPPPPPPP', 'SSSSSSSS'] + ['SSSSSSSS'] * 4 + ['bbbbbbbb'] + ['DDDDDDDD'] * 3,
           {**t, 'P': bikini, 'b': '#3c2a20', 'D': denim}, noise=2)
    for f in ('right', 'left'):
        s.grid('body', f, ['SSSS', 'SSSS', 'PPPP', 'SSSS', 'SSSS', 'SSSS', 'SSSS', 'SSSS', 'bbbb', 'DDDD', 'DDDD', 'DDDD'],
               {**t, 'P': bikini, 'b': '#3c2a20', 'D': denim}, noise=2)
    for leg in ('right_leg', 'left_leg'):
        clothes(s, leg, denim, 20, 23, noise=4, dark_back=0.85)
        clothes(s, leg, shade(denim, 1.25), 23, 24)
    shoes(s, '#1d1b22', 3, sole='#e8e4dc')

    # résille : la peau sous le filet noir est assombrie un point sur deux (moins agressif qu'un filet opaque)
    net = mix(t['S'], mesh, 0.55)

    def mesh_fn(part, gmax):
        def fn(f, x, y, w, h, gy):
            if gy is None or gy >= gmax:
                return None
            fx, fy, fw, fh = s.rect(part, f)
            px = s.img.getpixel((fx + x, fy + y))
            if min(sum(abs(px[i] - rgb(t[k])[i]) for i in range(3)) for k in ('S', 's')) > 40:
                return None  # maillot : pas de filet
            return net if (x + y) % 2 == 0 else None
        return fn
    s.paint('body', mesh_fn('body', 16), faces=SIDES)
    s.grid('body', 'front', ['MMMMMMMM'], {'M': mesh})
    for arm in ('right_arm', 'left_arm'):
        s.paint(arm, mesh_fn(arm, 18), faces=SIDES)
        clothes(s, arm, mesh, 8, 9)
        clothes(s, arm, mesh, 17, 18, layer='overlay')
        s.fill(arm, mesh, faces=('top',))

    ex = Extras()
    knot = style(hair, outline=0.65)
    # cinq couettes nouées en éventail sur l'arrière du crâne
    for i, (x, rz, rx) in enumerate(((-3.6, 40, -20), (-1.9, 18, -32), (-0.5, 0, -40), (0.9, -18, -32), (2.6, -40, -20))):
        ex.cube('head', [x, -10.6, 1.0], [1, 3, 1], knot, rot=(rx, 0, rz), pivot=[x + 0.5, -8, 1.5])
        ex.cube('head', [x - 0.25, -12.0, 0.75], [1.5, 1.6, 1.5], style(shade(hair, 1.12), outline=0.7), rot=(rx, 0, rz), pivot=[x + 0.5, -8, 1.5])
    # frange en volume
    ex.cube('head', [-4.3, -8.4, -4.6], [8.6, 2, 1], style(hair, outline=0.7, grad=0.3))
    return s, {'slim': True, 'scale': [0.94, 0.95, 0.94]}, ex


# ---------------------------------------------------------------------------------------------------------------
# Buhara : colosse ventru, très large, tête ronde, petits yeux, gros appétit


@character
def buhara():
    s = Skin(seed=14)
    t = skin_tone('#d9a072')
    hair = rgb('#3a2a22')
    pal = {**t, 'H': hair, 'h': shade(hair, 0.8), 'k': '#1d140f', 'r': '#e48a76', 'm': '#8c4a3a', 'n': shade('#d9a072', 0.8)}
    s.fill('head', t['S'], noise=3)
    s.grid('head', 'front', [
        'HHhHHhHH',
        'SSHHHHSS',
        'SHHSSHHS',
        'SkSSSSkS',
        'rSSnnSSr',
        'rSSSSSSr',
        'SSmmmmSS',
        'zSSSSSSz',
    ], pal, noise=2)
    side = ['HHHHHHHH', 'HHHHHSSS', 'HHHSSSSS', 'hHHSnnSS', 'hHSSnSSS', 'hSSSSSSS', 'SSSSSSSS', 'zzSSSSSS']
    s.grid('head', 'right', side, pal, noise=3)
    s.grid('head', 'left', [r[::-1] for r in side], pal, noise=3)
    s.grid('head', 'back', ['HHHHHHHH', 'HHHHHHHH', 'HHhHHhHH', 'hHHHHHHh', 'hhHHHHhh', 'SSSSSSSS', 'zSSSSSSz', 'zzzzzzzz'], pal, noise=3)
    s.fill('head', hair, faces=('top',), noise=4)

    shirt, pants, belt = rgb('#e88c3a'), rgb('#6b4b34'), rgb('#3b2a1d')
    for p in ('right_arm', 'left_arm'):
        limb_skin(s, p, t)
        clothes(s, p, shirt, 8, 12)
        top_fill(s, p, shirt)
        clothes(s, p, shirt, 8, 12, layer='overlay')
    clothes(s, 'body', shirt, 8, 18)
    top_fill(s, 'body', shirt)
    s.grid('body', 'front', ['SzzzzzzS', 'SSzzzzSS', 'SSSzzSSS', 'SSSSSSSS'], {'S': shirt, 'z': t['s']}, noise=3)
    clothes(s, 'body', belt, 18, 19)
    clothes(s, 'body', pants, 19, 20)
    for leg in ('right_leg', 'left_leg'):
        clothes(s, leg, pants, 20, 32)
        clothes(s, leg, shade(pants, 0.85), 26, 27)
    shoes(s, '#2a1d14', 1)

    ex = Extras()
    sh = style(shirt, outline=0.75, grad=0.12)
    # gros ventre : un bloc arrondi devant le torse (deux cubes pour l'arrondi)
    ex.cube('body', [-4.4, 3, -5.2], [8.8, 8, 3.6], sh)
    ex.cube('body', [-3.6, 4, -6.4], [7.2, 6, 1.4], style(shade(shirt, 1.04), outline=0.75, grad=0.1))
    ex.cube('body', [-4.6, 10.5, -4.4], [9.2, 1, 6.6], style(belt, outline=0.0))
    # double menton
    ex.cube('head', [-3, -1.2, -4.6], [6, 2, 2], style(t['S'], outline=0.88, grad=0.2))
    return s, {'slim': False, 'scale': [1.5, 1.32, 1.5]}, ex


# ---------------------------------------------------------------------------------------------------------------
# Beans : secrétaire du président, petite taille, tête verte en forme de haricot, costume noir


@character
def beans():
    s = Skin(seed=15)
    g = rgb('#7fbf5a')
    t = {'S': g, 's': shade(g, 0.85), 'z': shade(g, 0.72), 'l': shade(g, 1.12)}
    pal = {**t, 'k': '#16130f', 'w': '#ffffff', 'm': '#3f6b2c'}
    s.fill('head', g, noise=3)
    s.grid('head', 'front', [
        'llllllll',
        'SSSllSSS',
        'SSSSSSSS',
        'SkwSSkwS',
        'SkkSSkkS',
        'SSSSSSSS',
        'SSSmmSSS',
        'sSSSSSSs',
    ], pal, noise=2)
    for f in ('right', 'left', 'back'):
        s.grid('head', f, ['SSSSSSSS'] * 6 + ['ssssssss', 'zzzzzzzz'], pal, noise=3)
    suit(s, '#1f2027', '#f4f4f4', '#3d6cc4', shoe='#101014', tie_len=8)
    hands(s, {'S': '#f4f4f4', 's': '#d8d8d8'}, gy0=18)
    ex = Extras()
    # haut du crâne en forme de haricot : un dôme qui penche un peu vers l'arrière
    ex.cube('head', [-3.5, -11, -3.8], [7, 3, 7], style(shade(g, 1.06), outline=0.8, grad=0.2), rot=(-8, 0, 0), pivot=[0, -8, 0])
    ex.cube('head', [-2.5, -12.4, -2.6], [5, 2, 5], style(shade(g, 1.12), outline=0.8, grad=0.2), rot=(-14, 0, 0), pivot=[0, -8, 0])
    return s, {'slim': False, 'scale': [0.74, 0.7, 0.74]}, ex


def hair_head(s, pal, front, side, back, top='H', over_front=None, over_side=None, over_back=None):
    s.fill('head', pal['S'], noise=2)
    s.grid('head', 'front', front, pal, noise=2)
    s.grid('head', 'right', side, pal, noise=3)
    s.grid('head', 'left', [r[::-1] for r in side], pal, noise=3)
    s.grid('head', 'back', back, pal, noise=3)
    s.fill('head', pal[top], faces=('top',), noise=4)
    if over_front:
        s.grid('head', 'front', over_front, pal, layer='overlay')
    if over_side:
        s.grid('head', 'right', over_side, pal, layer='overlay')
        s.grid('head', 'left', [r[::-1] for r in over_side], pal, layer='overlay')
    if over_back:
        s.grid('head', 'back', over_back, pal, layer='overlay')
        s.fill('head', pal[top], layer='overlay', faces=('top',), noise=4)


def spikes(ex, specs, color, tip=None):
    """specs : (x, z, hauteur, rot x, rot z) — pointes plantées dans le haut du crâne."""
    for x, z, h, rx, rz in specs:
        rz = -rz  # (signe : positif = la pointe part vers l'extérieur du côté de x)
        ex.cube('head', [x - 1, -8 - h, z - 1], [2, h + 1, 2], style(color, outline=0.7, grad=0.25,
                faces={f: (lambda f, X, Y, W, H, tip=tip: tip if tip and Y <= max(0, H // 3 - 1) and f != 'bottom' else None) for f in ('front', 'back', 'left', 'right', 'top')}),
                rot=(rx, 0, rz), pivot=[x, -8, z])


# ---------------------------------------------------------------------------------------------------------------
# Gon : cheveux noirs en grandes pointes (bouts verts), veste verte, short, bottes


@character
def gon():
    s = Skin(seed=21)
    t = skin_tone('#e6b48c')
    hair = rgb('#1c1a1f')
    pal = {**t, 'H': hair, 'h': shade('#2e5c3a', 0.8), 'k': '#2a160c', 'i': '#6b3a1c', 'w': '#ffffff', 'm': '#a2504a', 'r': '#eb9c86'}
    hair_head(s, pal,
              ['HHHHHHHH', 'HHHHHHHH', 'HSHSSHSH', 'SkiSSikS', 'SkwSSwkS', 'SrSSSSrS', 'SSSmmSSS', 'sSSSSSSs'],
              ['HHHHHHHH', 'HHHHHHHH', 'HHHHHSSS', 'HHHSSSSS', 'HHSSzSSS', 'HHSSSSSS', 'HSSSSSSS', 'SSSSSSSS'],
              ['HHHHHHHH'] * 5 + ['HHhHHhHH', 'SHHHHHHS', 'SSSSSSSS'],
              over_front=['HHHHHHHH', 'H.H..H.H'], over_side=['HHHHHHHH', 'HHHHH...'], over_back=['HHHHHHHH'] * 5)
    jacket, shorts, boot = rgb('#2f9a54'), rgb('#2c4e2f'), rgb('#2a2a2e')
    for p in ('right_arm', 'left_arm', 'right_leg', 'left_leg'):
        limb_skin(s, p, t)
    clothes(s, 'body', jacket, 8, 20)
    top_fill(s, 'body', jacket)
    s.grid('body', 'front', ['GGWWWWGG', 'GGgWWgGG', 'GGgWWgGG', 'GGGgGGGG', 'GGGgGGGG', 'GGGgGGGG', 'GGGgGGGG', 'GGGgGGGG',
                            'GGGgGGGG', 'GGGgGGGG', 'GGGgGGGG', 'KKKKKKKK'],
           {'G': jacket, 'g': shade(jacket, 0.72), 'W': '#f2efe6', 'K': shade(jacket, 0.65)}, noise=3)
    for arm in ('right_arm', 'left_arm'):
        clothes(s, arm, jacket, 8, 13)
        top_fill(s, arm, jacket)
        clothes(s, arm, shade(jacket, 0.75), 12, 13)
        clothes(s, arm, jacket, 8, 13, layer='overlay')
    for leg in ('right_leg', 'left_leg'):
        clothes(s, leg, shorts, 20, 24)
        clothes(s, leg, boot, 27, 32)
        clothes(s, leg, shade(boot, 1.4), 27, 28)
        s.fill(leg, shade(boot, 0.6), faces=('bottom',))
    ex = Extras()
    spikes(ex, [(-3.2, -2.5, 6, -18, 48), (-1.6, -2.8, 8, -16, 22), (0, -3.0, 9, -18, 0), (1.6, -2.8, 8, -16, -22), (3.2, -2.5, 6, -18, -48),
                (-3.0, 1.5, 6, 30, 42), (-1.2, 1.2, 8, 26, 16), (1.2, 1.2, 8, 26, -16), (3.0, 1.5, 6, 30, -42),
                (0, -0.5, 9, 4, 0)], hair, tip=rgb('#2e6a3d'))
    return s, {'slim': False, 'scale': [0.86, 0.84, 0.86]}, ex


# ---------------------------------------------------------------------------------------------------------------
# Killua : cheveux blancs ébouriffés, yeux bleus, haut blanc sur manches longues bleu nuit, short, chaussures violettes


@character
def killua():
    s = Skin(seed=22)
    t = skin_tone('#f7dcc6')
    hair = rgb('#eef0f6')
    pal = {**t, 'H': hair, 'h': rgb('#c7ccdb'), 'd': rgb('#a3a9bd'), 'k': '#1c2a4a', 'i': '#3d7fd6', 'w': '#ffffff', 'm': '#b86a6a'}
    hair_head(s, pal,
              ['HHHHHHHH', 'HHHHHHHH', 'HhSHHShH', 'SkiSSikS', 'SkwSSwkS', 'SSSSSSSS', 'SSSmmSSS', 'sSSSSSSs'],
              ['HHHHHHHH', 'HHHHHHHH', 'HHHHhHHH', 'hHHHSSHH', 'hHHSSSSh', 'hhHSSSSS', 'dhSSSSSS', 'dSSSSSSS'],
              ['HHHHHHHH', 'HHHHHHHH', 'HhHHHHhH', 'HHHHHHHH', 'hHHhhHHh', 'hhHHHHhh', 'dhHhhHhd', 'SddddddS'],
              over_front=['HHHHHHHH', 'HHH.HHHH', 'h.H..H.h', 'h......h'], over_side=['HHHHHHHH', 'HHHHHHHH', 'hHHHHHH.', 'hhH.....'],
              over_back=['HHHHHHHH', 'HHHHHHHH', 'hHHHHHHh', 'hhHHHHhh', 'dhhhhhhd'])
    top, under, shorts, shoe = rgb('#f1f1f4'), rgb('#26315a'), rgb('#3b4570'), rgb('#7d55b8')
    for p in ('right_leg', 'left_leg'):
        limb_skin(s, p, t)
    clothes(s, 'body', top, 8, 20)
    top_fill(s, 'body', top)
    s.grid('body', 'front', ['UUUUUUUU', 'TUUUUUUT', 'TTTUUTTT', 'TTTTTTTT'], {'U': under, 'T': top}, noise=2)
    for arm in ('right_arm', 'left_arm'):
        clothes(s, arm, under, 8, 19)
        top_fill(s, arm, under)
        clothes(s, arm, t['S'], 19, 20)
        s.fill(arm, t['S'], faces=('bottom',))
        clothes(s, arm, top, 8, 9, layer='overlay')
    for leg in ('right_leg', 'left_leg'):
        clothes(s, leg, shorts, 20, 25)
        clothes(s, leg, shade(shorts, 1.2), 24, 25)
    shoes(s, shoe, 3, sole='#f0f0f0')
    ex = Extras()
    fluff = style(hair, outline=0.78, grad=0.3)
    # touffes blanches en tous sens (le volume de ses cheveux)
    for frm, size, rot, piv in (
            ([-4.6, -10.4, -4.6], [9.2, 3, 9.2], (0, 0, 0), None),
            ([-5.6, -9.6, -3.0], [2, 3, 6], (0, 0, 28), [-4, -8, 0]), ([3.6, -9.6, -3.0], [2, 3, 6], (0, 0, -28), [4, -8, 0]),
            ([-3, -12.4, -3.4], [3, 3, 3], (-20, 0, 25), [-1.5, -10, -2]), ([0.5, -12.6, -2.5], [3, 3, 3], (-15, 0, -20), [2, -10, -1]),
            ([-1.5, -12, 1.2], [3, 3, 3], (25, 0, 0), [0, -10, 2]), ([-4.4, -9.2, -5.6], [2, 2, 2], (-35, 0, 20), [-3.4, -8.2, -4.6]),
            ([2.4, -9.2, -5.6], [2, 2, 2], (-35, 0, -20), [3.4, -8.2, -4.6]), ([-1, -9.6, -5.8], [2, 2, 2], (-40, 0, 0), [0, -8.6, -4.8])):
        ex.cube('head', frm, size, fluff, rot=rot, pivot=piv)
    return s, {'slim': True, 'scale': [0.86, 0.85, 0.86]}, ex


# ---------------------------------------------------------------------------------------------------------------
# Kurapika : carré blond, yeux gris, tunique bleue brodée d'or sur vêtements blancs


@character
def kurapika():
    s = Skin(seed=23)
    t = skin_tone('#f6dcc4')
    hair = rgb('#f1cf6a')
    pal = {**t, 'H': hair, 'h': shade(hair, 0.82), 'd': shade(hair, 0.66), 'k': '#2b2b38', 'i': '#6c7a8e', 'w': '#ffffff', 'm': '#b9706a'}
    hair_head(s, pal,
              ['HHHHHHHH', 'HHHhHHHH', 'HhSHHShH', 'HkiSSikH', 'hwkSSkwh', 'hSSSSSSh', 'dSSmmSSd', 'dSSSSSSd'],
              ['HHHHHHHH', 'HHHHHHHH', 'HHHHHHHH', 'HHHHHHHH', 'hHHHHHHh', 'hhHHHHhh', 'dhhhhhhd', 'ddddddSS'],
              ['HHHHHHHH', 'HHHHHHHH', 'HHHHHHHH', 'hHHHHHHh', 'hhHHHHhh', 'hhhhhhhh', 'dhhhhhhd', 'dddddddd'],
              over_front=['HHHHHHHH', 'HH.HH.HH', 'h......h', 'h......h', 'd......d', 'd......d'],
              over_side=['HHHHHHHH', 'HHHHHHHH', 'HHHHHHHH', 'hHHHHHHh', 'hhHHHHhh', 'dhhhhhhd', 'dddddddd'],
              over_back=['HHHHHHHH', 'HHHHHHHH', 'HHHHHHHH', 'hHHHHHHh', 'hhHHHHhh', 'dhhhhhhd', 'dddddddd'])
    white, blue, gold = rgb('#f3f3f0'), rgb('#2f63b8'), rgb('#e1b84a')
    for p in ('body', 'right_arm', 'left_arm', 'right_leg', 'left_leg'):
        clothes(s, p, white, 0, 99)
        top_fill(s, p, white)
    s.paint('body', lambda f, x, y, w, h, gy: (blue if f in ('front', 'back') and 1 <= x <= 6 else None), faces=SIDES, noise=3)
    s.grid('body', 'front', ['.YBBBBY.', '.BYBBYB.', '.BBYYBB.', '.BBBBBB.', '.BYBBYB.', '.YYYYYY.', '.BBBBBB.', '.BBYYBB.',
                            '.BYBBYB.', '.BBBBBB.', '.YBBBBY.', '.BBBBBB.'], {'B': blue, 'Y': gold}, noise=2)
    for arm in ('right_arm', 'left_arm'):
        s.fill(arm, t['S'], faces=('bottom',))
        clothes(s, arm, t['S'], 19, 20)
        clothes(s, arm, white, 15, 19, layer='overlay')
    shoes(s, '#5a4632', 2)
    ex = Extras()
    ex.cube('body', [-3.6, 11, -2.6], [7.2, 6, 0.6], style(blue, outline=0.7, faces={'front': lambda f, x, y, w, h: gold if y == h - 1 or x in (0, w - 1) else None}))
    ex.cube('body', [-3.6, 11, 2.0], [7.2, 6, 0.6], style(blue, outline=0.7))
    ex.cube('head', [-4.8, -4, -1], [0.6, 1.5, 0.6], style('#c4373a', outline=0))  # boucle d'oreille
    return s, {'slim': True, 'scale': [0.9, 0.9, 0.9]}, ex


# ---------------------------------------------------------------------------------------------------------------
# Leorio : grand, costume bleu nuit, lunettes rondes, cheveux noirs courts


@character
def leorio():
    s = Skin(seed=24)
    t = skin_tone('#e8bf9a')
    hair = rgb('#1f1c22')
    pal = {**t, 'H': hair, 'h': shade(hair, 1.4), 'k': '#141418', 'g': '#3a3442', 'm': '#9c5a50'}
    hair_head(s, pal,
              ['HHHHHHHH', 'HHHSSHHH', 'SSSSSSSS', 'gkkggkkg', 'SkkSSkkS', 'SSSzzSSS', 'SSmmmmSS', 'sSSSSSSs'],
              ['HHHHHHHH', 'HHHHHHSS', 'HHHHSSSS', 'HHHgggSS', 'HHSSzSSS', 'HSSSSSSS', 'SSSSSSSS', 'SSSSSSSS'],
              ['HHHHHHHH'] * 4 + ['hHHHHHHh', 'SHHHHHHS', 'SSSSSSSS', 'SSSSSSSS'],
              over_front=['HHHHHHHH', 'HH....HH'], over_side=['HHHHHHHH', 'HHHH....'], over_back=['HHHHHHHH'] * 4)
    suit(s, '#2a3148', '#f2f2f4', '#3d72c9', shoe='#1a1410')
    hands(s, t)
    ex = Extras()
    ex.cube('head', [-4.2, -9, -4.4], [8.4, 1.5, 8.8], style(hair, outline=0.7))
    ex.cube('head', [-3.6, -10.2, -3.6], [2.4, 2, 2.4], style(hair), rot=(-20, 0, 20), pivot=[-2.4, -9, -2.4])
    ex.cube('head', [1.2, -10.2, -3.6], [2.4, 2, 2.4], style(hair), rot=(-20, 0, -20), pivot=[2.4, -9, -2.4])
    return s, {'slim': False, 'scale': [1.0, 1.1, 1.0]}, ex


# ---------------------------------------------------------------------------------------------------------------
# Hisoka : cheveux rouges plaqués en pointes, étoile et larme maquillées, yeux dorés, tenue de bouffon


@character
def hisoka():
    s = Skin(seed=25)
    t = skin_tone('#f5e1d2')
    hair = rgb('#d8435a')
    pal = {**t, 'H': hair, 'h': shade(hair, 0.78), 'k': '#1b1214', 'i': '#e2b33a', 'b': '#3a7bd5', 'p': '#d64a8e', 'm': '#8e3a48'}
    hair_head(s, pal,
              ['HHHHHHHH', 'HhSSSShH', 'SkkSSkkS', 'SkiSSikS', 'pSSzzSSb', 'SSSSSSbS', 'SSmmmmSS', 'sSSSSSSs'],
              ['HHHHHHHH', 'HHHHHHHH', 'HHHHHSSS', 'hHHSSSSS', 'hHSSzSSS', 'hSSSSSSS', 'SSSSSSSS', 'SSSSSSSS'],
              ['HHHHHHHH', 'HHHHHHHH', 'HHHHHHHH', 'hHHHHHHh', 'hhHHHHhh', 'SSSSSSSS', 'SSSSSSSS', 'SSSSSSSS'])
    white, sash, pants = rgb('#f2ecef'), rgb('#4d9d6a'), rgb('#e8e2ea')
    for p in ('body', 'right_arm', 'left_arm'):
        clothes(s, p, white, 0, 99)
        top_fill(s, p, white)
    s.grid('body', 'front', ['WSSSSSSW', 'WWSSSSWW', 'WWWWWWWW', 'WCWWWWdW', 'WWWWWWWW', 'WWWWWWWW', 'WWHWWsWW', 'WWWWWWWW',
                            'GGGGGGGG', 'gGGGGGGg', 'PPPPPPPP', 'PPPPPPPP'],
           {'W': white, 'S': t['S'], 'C': '#2b2b8a', 'd': '#d64a6e', 'H': '#d64a6e', 's': '#2b2b2b', 'G': sash, 'g': shade(sash, 0.8), 'P': pants}, noise=2)
    clothes(s, 'body', sash, 16, 18, faces=('right', 'left', 'back'))
    for arm in ('right_arm', 'left_arm'):
        clothes(s, arm, t['S'], 18, 20)
        s.fill(arm, t['S'], faces=('bottom',))
        clothes(s, arm, white, 8, 12, layer='overlay')
    for leg in ('right_leg', 'left_leg'):
        clothes(s, leg, pants, 20, 32)
        clothes(s, leg, shade(pants, 0.88), 25, 26)
    shoes(s, '#4a2d5c', 2)
    ex = Extras()
    hs = style(hair, outline=0.7, grad=0.3)
    # cheveux plaqués vers l'arrière en pointes dressées
    ex.cube('head', [-4.2, -8.8, -4.3], [8.4, 1, 8.6], hs)
    for x, rz, h in ((-3.2, 14, 4), (-1.2, 5, 6), (1.2, -5, 6), (3.2, -14, 4)):
        ex.cube('head', [x - 1, -8 - h, -1], [2, h + 1, 2], hs, rot=(58, 0, -rz), pivot=[x, -8, 0])
    return s, {'slim': True, 'scale': [0.97, 1.1, 0.97]}, ex


def run(names):
    for n in names:
        skin, look, ex = CHARACTERS[n]()
        save_character(n, skin, look, ex)
        print('ok', n)


if __name__ == '__main__':
    run(sys.argv[1:] or list(CHARACTERS))
