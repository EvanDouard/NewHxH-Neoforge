#!/usr/bin/env python3
"""Génère la carte du dirigeable : texture propre (assets/hxh/textures/gui/world_map.png) et grille des régions nettoyée
(assets/hxh/world/regions.txt, lue par le client pour le survol). Source : data/hxh/world/cells.txt (non modifiée).
Usage : python3 tools/gen_world_map.py"""
import numpy as np
from collections import deque
from PIL import Image, ImageFilter

CELLS = 'src/main/resources/data/hxh/world/cells.txt'
OUT_TXT = 'src/main/resources/assets/hxh/world/regions.txt'
OUT_PNG = 'src/main/resources/assets/hxh/textures/gui/world_map.png'
PX = 6          # pixels par case dans la texture finale
SS = 3          # sur-échantillonnage pour des côtes lisses

rows = [r for r in open(CELLS).read().split('\n') if r]
H, W = len(rows), max(len(r) for r in rows)
g = [list(r.ljust(W, '~')) for r in rows]

def comps(pred):
    seen = np.zeros((H, W), bool); out = []
    for z in range(H):
        for x in range(W):
            if seen[z, x] or not pred(g[z][x]): continue
            q = deque([(x, z)]); seen[z, x] = True; cs = []
            while q:
                cx, cz = q.popleft(); cs.append((cx, cz))
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
                    nx, nz = cx + dx, cz + dz
                    if 0 <= nx < W and 0 <= nz < H and not seen[nz, nx] and pred(g[nz][nx]):
                        seen[nz, nx] = True; q.append((nx, nz))
            out.append(cs)
    return out

# 0. Un trou de 1 à 2 cases de terre libre ou de mer entre deux cases de la même région est comblé
for _ in range(2):
    for z in range(H):
        for x in range(W):
            if g[z][x] in 'mksKob' and g[z][x] != '': continue
            for gap in (1, 2):
                for dx, dz in ((1, 0), (0, 1)):
                    ax, az, bx, bz = x - dx, z - dz, x + dx * gap, z + dz * gap
                    if 0 <= ax < W and 0 <= bx < W and 0 <= az < H and 0 <= bz < H:
                        a, b = g[az][ax], g[bz][bx]
                        if a == b and a in 'mksKob' and all(g[z + dz * i][x + dx * i] in '.~' for i in range(gap)):
                            for i in range(gap): g[z + dz * i][x + dx * i] = a
# 1. Les petits morceaux isolés d'une lettre redeviennent de la terre libre
for letter in 'mksKob':
    for small in comps(lambda c, L=letter: c == L):
        if len(small) < (25 if letter == 'o' else 100):  # Ochima est un archipel : ses îles restent
            for x, z in small: g[z][x] = '.'

# 2. Les poches de terre libre enclavées dans une région (ou petites) sont absorbées par la région voisine majoritaire
for cs in comps(lambda c: c == '.'):
    border = {}
    sea = False
    for x, z in cs:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                nx, nz = x + dx, z + dz
                if 0 <= nx < W and 0 <= nz < H:
                    c = g[nz][nx]
                    if c == '~': sea = True
                    elif c != '.': border[c] = border.get(c, 0) + 1
    if border and (len(cs) <= 60 or not sea):
        best = max(border, key=border.get)
        for x, z in cs: g[z][x] = best

# 3. Les îlots de moins de 3 cases disparaissent
for cs in comps(lambda c: c != '~'):
    if len(cs) < 3:
        for x, z in cs: g[z][x] = '~'

# 4. Lissage : une case prend la lettre majoritaire de ses 8 voisins si elle est en minorité écrasante
for _ in range(2):
    new = [r[:] for r in g]
    for z in range(H):
        for x in range(W):
            cnt = {}
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    if dx == 0 and dz == 0: continue
                    nx, nz = x + dx, z + dz
                    c = g[nz][nx] if 0 <= nx < W and 0 <= nz < H else '~'
                    cnt[c] = cnt.get(c, 0) + 1
            best = max(cnt, key=cnt.get)
            if cnt[best] >= 6 and best != g[z][x]: new[z][x] = best
    g = new

open(OUT_TXT, 'w').write('\n'.join(''.join(r) for r in g) + '\n')

# 5. Texture
SEA = (38, 78, 140); SEA_DEEP = (28, 62, 118)
LAND = (196, 184, 140)
TINT = {'m': (132, 178, 206), 'k': (160, 130, 184), 's': (196, 104, 92), 'K': (222, 190, 96), 'o': (110, 170, 108), 'b': (222, 148, 84)}
big = np.zeros((H * PX * SS, W * PX * SS, 3), np.uint8)
mask = np.zeros((H, W), np.uint8)
for z in range(H):
    for x in range(W): mask[z, x] = 255 if g[z][x] != '~' else 0
m = Image.fromarray(mask).resize((W * PX * SS, H * PX * SS), Image.NEAREST).filter(ImageFilter.GaussianBlur(PX * SS * 0.55))
land = np.array(m) > 127
# dégradé de mer : plus sombre loin des côtes
dist = np.array(Image.fromarray((land * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(PX * SS * 6))).astype(float) / 255
sea_img = np.zeros_like(big, float)
for i in range(3): sea_img[..., i] = SEA[i] + (SEA_DEEP[i] - SEA[i]) * (1 - np.clip(dist * 3, 0, 1))
big[:] = sea_img.astype(np.uint8)
big[land] = LAND
for letter, col in TINT.items():
    rm = np.zeros((H, W), np.uint8)
    for z in range(H):
        for x in range(W):
            if g[z][x] == letter: rm[z, x] = 255
    rimg = Image.fromarray(rm).resize((W * PX * SS, H * PX * SS), Image.NEAREST).filter(ImageFilter.GaussianBlur(PX * SS * 0.55))
    sel = (np.array(rimg) > 127) & land
    big[sel] = col
# liseré côtier
edge = land & ~(np.array(Image.fromarray((land * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(2 * SS + 1))) > 127)
big[edge] = (120, 108, 78)
img = Image.fromarray(big).resize((W * PX, H * PX), Image.LANCZOS)
img.save(OUT_PNG, optimize=True)
print('ok', img.size)
