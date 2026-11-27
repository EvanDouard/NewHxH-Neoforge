#!/usr/bin/env python3
"""Extrait de la carte « Lac Mobius » (image d'Evan, 5921×3513) le contour du lac et celui du Nouveau Continent,
enregistrés en 2400×1424 dans tools/ref/ (lake_mask.png, new_continent_mask.png) pour tools/gen_world_map.py.
Usage : python3 tools/extract_lake_masks.py chemin/vers/carte_lac_mobius.png"""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi
CW, CH = 2400, 1424
im = Image.open(sys.argv[1]).convert('RGB').resize((CW, CH), Image.LANCZOS)
a = np.array(im).astype(int); r, g, b = a[..., 0], a[..., 1], a[..., 2]
blue = ndi.binary_closing((r < 125) & (g > r + 8) & (b > r + 5), iterations=3)
lab, _ = ndi.label(~blue)
border = (set(lab[0, :]) | set(lab[-1, :]) | set(lab[:, 0]) | set(lab[:, -1])) - {0}
lake = ndi.binary_opening(~np.isin(lab, list(border)), iterations=2)       # lac + terres intérieures
inner, n = ndi.label(lake & ~blue)
nc = np.zeros_like(lake)
for i in range(1, n + 1):
    ys, xs = np.where(inner == i)
    if len(ys) > 10000 and xs.mean() / CW > 0.7: nc = inner == i            # le Nouveau Continent : à l'est
nc = ndi.binary_closing(ndi.binary_fill_holes(nc), iterations=3)
Image.fromarray((lake * 255).astype(np.uint8)).save('tools/ref/lake_mask.png', optimize=True)
Image.fromarray((nc * 255).astype(np.uint8)).save('tools/ref/new_continent_mask.png', optimize=True)
print('lac', lake.mean(), 'nouveau continent', nc.sum())
