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
for x, z in seen:
    if g[z][x] in '.sp': g[z][x] = 'p'

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
