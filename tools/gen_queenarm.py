#!/usr/bin/env python3
"""Ébauche rapide du site du Bras de la Reine des Fourmis-Chimères (Evan repasse dessus)."""
import json, math
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
W = D = 56
F([0, -1, 0, W, -1, D], "minecraft:coarse_dirt")
# cratère d'atterrissage (anneaux concentriques, centre 28,28)
for r, blk in ((18, "minecraft:blackstone"), (14, "minecraft:coarse_dirt"), (10, "minecraft:soul_soil"), (6, "minecraft:magma_block")):
    for x in range(W + 1):
        for z in range(D + 1):
            if (x - 28) ** 2 + (z - 28) ** 2 <= r * r:
                Bk(x, -2 if r <= 10 else -1, z, blk) if (x + z) % 2 == 0 or r > 10 else None
# "bras" : grande excroissance d'os et de chair émergeant du cratère
F([24, 0, 24, 32, 3, 32], "minecraft:bone_block[axis=y]"); F([25, 4, 25, 31, 9, 31], "minecraft:bone_block[axis=y]")
F([26, 10, 26, 30, 16, 30], "minecraft:red_nether_bricks"); F([27, 17, 27, 29, 22, 29], "minecraft:nether_wart_block")
for k, (dx, dz) in enumerate(((31, 27), (31, 29), (30, 31), (29, 31))):
    F([dx, 17, dz, dx + 2, 19 + k, dz + 1], "minecraft:red_nether_bricks")      # doigts
# débris (épaves)
for (x, z, h) in ((8, 10, 4), (44, 12, 5), (10, 44, 3), (46, 46, 6)):
    F([x, 0, z, x + 2, h, z + 2], "minecraft:cracked_deepslate_bricks"); Bk(x + 1, h + 1, z + 1, "minecraft:soul_lantern[hanging=false]")
# arbres morts
for (x, z) in ((4, 28), (52, 24), (28, 4), (24, 52), (14, 18), (40, 38)):
    F([x, 0, z, x, 6, z], "minecraft:dark_oak_log[axis=y]"); F([x - 1, 5, z, x + 1, 5, z], "minecraft:dark_oak_log[axis=x]")
Bk(28, 0, 52, "hxh:airship_terminal[facing=north]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("survivant_atterrissage", 30, 0, 50, 180)
json.dump({"x": 772, "z": 4557, "clear": [0, 0, 0, W, 30, D], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/chimera_ant_queen_arm.json", "w"), indent=1)
print(len(fills), len(blocks))
