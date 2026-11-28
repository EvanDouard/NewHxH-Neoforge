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

LETTERS = 'mksKobprhwen'  # six régions + République de Padokea (p) + États de l'Union de Mitene (n NGL, r Rokario, h Hass, w Gorteau-Ouest, e Gorteau-Est)
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

# HXH_NO_CLEAN=1 : cells.txt est déjà la grille finale (retouchée à la main), on saute le nettoyage automatique (étapes 0 à 4)
import os
if not os.environ.get('HXH_NO_CLEAN'):
    # 0. Un trou de 1 à 2 cases de terre libre ou de mer entre deux cases de la même région est comblé
    for _ in range(2):
        for z in range(H):
            for x in range(W):
                if g[z][x] in LETTERS: continue
                for gap in (1, 2):
                    for dx, dz in ((1, 0), (0, 1)):
                        ax, az, bx, bz = x - dx, z - dz, x + dx * gap, z + dz * gap
                        if 0 <= ax < W and 0 <= bx < W and 0 <= az < H and 0 <= bz < H:
                            a, b = g[az][ax], g[bz][bx]
                            if a == b and a in LETTERS and all(g[z + dz * i][x + dx * i] in '.~' for i in range(gap)):
                                for i in range(gap): g[z + dz * i][x + dx * i] = a
    # 1. Les petits morceaux isolés d'une lettre redeviennent de la terre libre
    for letter in 'mksKobp':
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
TINT = {'m': (132, 178, 206), 'k': (160, 130, 184), 's': (196, 104, 92), 'K': (222, 190, 96), 'o': (110, 170, 108), 'b': (222, 148, 84),
        'p': (88, 176, 168), 'n': (124, 196, 122), 'r': (224, 138, 200), 'h': (200, 216, 96), 'w': (154, 106, 208), 'e': (232, 154, 80)}
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

# 6. Seconde carte (« Continent Noir connu »), d'après la carte du lac Mobius : le Continent Noir (parchemin) entoure tout le lac ;
#    le lac a deux lobes ; le monde connu est au centre dans un cadre en pointillés ; le Nouveau Continent est dans le lobe est ;
#    cinq repères sur les côtes du Continent Noir (Saherta, Begerossé, Kukan'yu, Mimbo, Ochima). Purement visuel : les limites du
#    Continent Noir ne sont pas connues. Mêmes constantes dans AirshipMapScreen (toile, position et taille du monde).
from PIL import ImageDraw
OUT_DARK = 'src/main/resources/assets/hxh/textures/gui/world_map_dark.png'
CW, CH = 2400, 1424
WS = 0.72                                   # échelle du monde connu sur la toile
WW, WH = round(W * PX * WS), round(H * PX * WS)
DX, DY = (CW - WW) // 2, (CH - WH) // 2
rng = np.random.default_rng(27)
def fractal(w, h, octaves=(4, 8, 16, 32, 64)):
    acc = np.zeros((h, w)); amp = 1.0; tot = 0
    for cells_ in octaves:
        n = rng.random((max(2, cells_ * h // w) + 2, cells_ + 2))
        acc += amp * np.array(Image.fromarray((n * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)).astype(float) / 255
        tot += amp; amp *= 0.55
    return acc / tot
yy, xx = np.mgrid[0:CH, 0:CW].astype(float)
u, v = xx / CW, yy / CH
def ell(cx, cy, rx, ry): return 1 - (((u - cx) / rx) ** 2 + ((v - cy) / ry) ** 2)
# contours tracés sur la carte du lac Mobius (tools/extract_lake_masks.py) : lac à deux lobes et Nouveau Continent
lake = np.array(Image.open('tools/ref/lake_mask.png').convert('L').resize((CW, CH), Image.LANCZOS)) > 127
lake = np.array(Image.fromarray((lake * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2))) > 127
# parchemin du Continent Noir
tex = fractal(CW, CH, (3, 6, 12, 24))
vign = np.clip(1 - np.maximum(np.abs(u - 0.5) * 2, np.abs(v - 0.5) * 2) ** 3, 0, 1)
canvas = np.zeros((CH, CW, 3), float)
for i, (a, b) in enumerate(zip((214, 176, 112), (150, 112, 62))):
    canvas[..., i] = b + (a - b) * (0.55 + 0.45 * vign) + (tex - 0.5) * 22
# lac Mobius
lake_f = np.array(Image.fromarray((lake * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(40))).astype(float) / 255
for i, c in enumerate((92, 130, 150)):
    lake_col = c + (tex - 0.5) * 18 - 16 * (1 - np.clip(lake_f * 1.6, 0, 1))
    canvas[..., i][lake] = lake_col[lake]
# rivage : double trait clair + ombre
inner = np.array(Image.fromarray((lake * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(7))) > 127
inner2 = np.array(Image.fromarray((lake * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(15))) > 127
shore1 = lake & ~inner; shore2 = inner & ~inner2
canvas[shore1] = (205, 220, 214); canvas[shore2] = (150, 188, 196)
outside = np.array(Image.fromarray((lake * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(9))) > 127
canvas[outside & ~lake] = canvas[outside & ~lake] * 0.78 + np.array((120, 96, 52)) * 0.22
# Nouveau Continent (lobe est) : contour tracé
nc = np.array(Image.open('tools/ref/new_continent_mask.png').convert('L').resize((CW, CH), Image.LANCZOS)) > 127
nc = np.array(Image.fromarray((nc * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2))) > 127
nc_edge = nc & ~(np.array(Image.fromarray((nc * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(7))) > 127)
nc_shade = np.array(Image.fromarray((nc * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(25))).astype(float) / 255
for i, (a_, b_) in enumerate(zip((214, 166, 104), (186, 134, 80))):
    canvas[..., i][nc] = (a_ + (b_ - a_) * (1 - np.clip(nc_shade * 1.5, 0, 1)) + (tex - 0.5) * 16)[nc]
canvas[nc_edge] = (232, 214, 170)
canvas = np.clip(canvas, 0, 255).astype(np.uint8)
world = Image.fromarray(canvas).convert('RGB')
# monde connu : seules la terre et son liséré sont posés (la mer est celle du lac)
wimg = img.resize((WW, WH), Image.LANCZOS)
wmask = m.resize((WW, WH), Image.LANCZOS).filter(ImageFilter.MaxFilter(3))
wmask = wmask.point(lambda a: 255 if a > 150 else (0 if a < 70 else int((a - 70) * 255 / 80)))
world.paste(wimg, (DX, DY), wmask)
# cadre en pointillés autour du monde connu
d = ImageDraw.Draw(world, 'RGBA')
x0, y0, x1, y1, r = DX - 45, DY - 45, DX + WW + 45, DY + WH + 45, 70
def dashed(pts, dash=22, gap=14, color=(225, 235, 240, 190), wd=3):
    step = dash + gap; carry = 0
    for (ax, ay), (bx, by) in zip(pts, pts[1:]):
        L = max(1, ((bx - ax) ** 2 + (by - ay) ** 2) ** 0.5)
        t = -carry
        while t < L:
            a_, b_ = max(t, 0), min(t + dash, L)
            if b_ > a_: d.line([(ax + (bx - ax) * a_ / L, ay + (by - ay) * a_ / L), (ax + (bx - ax) * b_ / L, ay + (by - ay) * b_ / L)], fill=color, width=wd)
            t += step
        carry = (t - L)
import math
pts = []
for cx_, cy_, a0 in ((x1 - r, y0 + r, -90), (x1 - r, y1 - r, 0), (x0 + r, y1 - r, 90), (x0 + r, y0 + r, 180)):
    for k in range(0, 91, 6):
        pts.append((cx_ + r * math.cos(math.radians(a0 + k)), cy_ + r * math.sin(math.radians(a0 + k))))
pts.append(pts[0])
dashed(pts)
world.save(OUT_DARK, optimize=True)
# repères (petits carrés dans le client) : positions relevées sur la carte du lac Mobius, en pixels de la toile
marks = {'saherta': (0.554, 0.168), 'begerosse': (0.672, 0.104), 'kukanyu': (0.547, 0.857), 'mimbo': (0.751, 0.951), 'ochima': (0.882, 0.876)}
marks = {k: (round(a_ * CW), round(b_ * CH)) for k, (a_, b_) in marks.items()}
print('ok dark', world.size, 'monde', (DX, DY, WW, WH), 'repères', marks)
