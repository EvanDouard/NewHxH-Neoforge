#!/usr/bin/env python3
"""Retouche data/hxh/world/cells.txt (idempotent) d'après la carte officielle du monde humain :
- p = République de Padokea : toute la terre libre du continent nord autour de Minbo ;
- archipel de l'Union de Mitene, du plus à l'ouest au plus à l'est : n NGL, r Rokario, h Hass, w Gorteau-Ouest, e Gorteau-Est.
Usage : python3 tools/paint_regions.py && python3 tools/gen_world_map.py"""
from collections import deque
CELLS = 'src/main/resources/data/hxh/world/cells.txt'
rows = [r for r in open(CELLS).read().split('\n') if r]
H, W = len(rows), max(len(r) for r in rows)
g = [list(r.ljust(W, '~')) for r in rows]

# 0. Régions d'après la carte officielle (tools/ref/human_world_map.png, 1080×764 ↔ 249×176 cases) : la couleur de chaque case décide
#    de sa région ; la teinte brune = terre libre. Les cases de Mitene ne sont pas sur l'image.
import colorsys
from PIL import Image
REF = Image.open('tools/ref/human_world_map.png').convert('RGB')
def tint(px):
    r, gg, b = px
    h, sat, v = colorsys.rgb_to_hsv(r / 255, gg / 255, b / 255); h *= 360
    if v < 0.3 or sat < 0.1: return None                    # texte, contour, blanc
    if 185 <= h <= 212 and sat < 0.36: return None          # mer
    if 20 <= h <= 50 and 0.25 <= sat <= 0.65 and 0.45 <= v <= 0.8: return '.'   # terre libre (brun)
    if 215 <= h <= 268: return 'm'
    if 270 <= h <= 335: return 'k'
    if (h >= 340 or h <= 12) and sat > 0.3: return 's'
    if 50 <= h <= 78: return 'K'
    if 85 <= h <= 165: return 'o'
    if 12 < h < 45 and sat >= 0.3: return 'b'
    return None
sx, sz = REF.size[0] / W, REF.size[1] / H
for z in range(H):
    for x in range(W):
        if g[z][x] in '~nrhwe': continue
        votes = {}
        for py in range(int(z * sz), max(int(z * sz) + 1, int((z + 1) * sz))):
            for px_ in range(int(x * sx), max(int(x * sx) + 1, int((x + 1) * sx))):
                t = tint(REF.getpixel((min(px_, REF.size[0] - 1), min(py, REF.size[1] - 1))))
                if t: votes[t] = votes.get(t, 0) + 1
        tot = sum(votes.values())
        if not tot: continue
        best = max(votes, key=votes.get)
        if best != '.' and votes[best] >= 0.3 * tot: g[z][x] = best
        elif best == '.' and (g[z][x] in 'mksKobp'): g[z][x] = '.'
# 0b. Nettoyage : texte coloré de l'image (« AZIAN CONTINENT »…) et fins traits : on ne garde que la plus grande plage de chaque région
for _ in range(2):
    for z in range(1, H - 1):
        for x in range(1, W - 1):
            L = g[z][x]
            if L in 'mksKob' and sum(g[z + dz][x + dx] == L for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) < 2: g[z][x] = '.'
def components(letter):
    seen_, out_ = set(), []
    for z in range(H):
        for x in range(W):
            if g[z][x] != letter or (x, z) in seen_: continue
            qq = deque([(x, z)]); seen_.add((x, z)); cs = []
            while qq:
                cx, cz = qq.popleft(); cs.append((cx, cz))
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        n = (cx + dx, cz + dz)
                        if 0 <= n[0] < W and 0 <= n[1] < H and n not in seen_ and g[n[1]][n[0]] == letter:
                            seen_.add(n); qq.append(n)
            out_.append(cs)
    return out_
for letter in 'mksKb':
    for cs in components(letter):
        if len(cs) < 60:   # taches de texte coloré de l'image
            for x, z in cs: g[z][x] = '.'
for cs in components('o'):
    if len(cs) < 25:
        for x, z in cs: g[z][x] = '.'

# 1. Padokea : la composante du continent nord (celle qui contient Minbo)
mz = next(z for z in range(H) for x in range(W) if g[z][x] == 'm')
mx = next(x for x in range(W) if g[mz][x] == 'm')
seen = {(mx, mz)}; q = deque([(mx, mz)])
while q:
    cx, cz = q.popleft()
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            n = (cx + dx, cz + dz)
            if 0 <= n[0] < W and 0 <= n[1] < H and n not in seen and g[n[1]][n[0]] != '~':
                seen.add(n); q.append(n)
XB = [(185, 490), (230, 440), (262, 390), (295, 385), (332, 350)]   # frontière (y, x) dans l'image recadrée (130,0)×3
def border_x(cy):
    if cy <= XB[0][0]: return XB[0][1]
    if cy >= XB[-1][0]: return XB[-1][1]
    for (y0, x0), (y1, x1) in zip(XB, XB[1:]):
        if y0 <= cy <= y1: return x0 + (x1 - x0) * (cy - y0) / (y1 - y0)
for x, z in seen:
    if g[z][x] in '.sp':
        cx_, cy_ = ((x + 0.5) * sx - 130) * 3, (z + 0.5) * sz * 3
        g[z][x] = 'p' if cx_ < border_x(cy_) else '.'

# 2. Mitene : on efface l'ancienne tache puis on dessine cinq îles en chaîne
for z in range(155, 170):
    for x in range(58, 90):
        if g[z][x] in '.oksnrhwe' and not (z <= 158 and x <= 64):  # ne touche pas la terre du nord-ouest
            g[z][x] = '~'
ISLES = [('n', 63.5, 164.5, 4.5, 3.5), ('r', 70.0, 161.5, 3.0, 2.5), ('h', 74.0, 160.0, 2.8, 2.3),
         ('w', 77.5, 158.5, 2.8, 2.3), ('e', 81.5, 157.0, 3.0, 2.3)]
for z in range(150, 172):
    for x in range(56, 90):
        for letter, cx, cz, rx, rz in ISLES:
            if ((x + 0.5 - cx) / rx) ** 2 + ((z + 0.5 - cz) / rz) ** 2 <= 1:
                g[z][x] = letter
open(CELLS, 'w').write('\n'.join(''.join(r).rstrip('~') if False else ''.join(r) for r in g) + '\n')
print('ok')
