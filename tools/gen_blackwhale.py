#!/usr/bin/env python3
"""Génère data/hxh/world_places/black_whale_port.json : port de départ du Black Whale (navire noir géant)."""
import json
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
# quai
F([0, -1, 0, 80, -1, 60], "minecraft:stone_bricks")
F([0, -1, 40, 80, -1, 60], "minecraft:smooth_stone")
F([70, -1, 0, 84, -1, 60], "minecraft:dark_oak_planks")        # ponton le long de la coque
for z in range(1, 60, 6): F([70, -2, z, 84, -2, z], "minecraft:stripped_dark_oak_log[axis=x]")
# place du dirigeable : borne en (45, 0, 50)
Bk(45, 0, 50, "hxh:airship_terminal[facing=north]")
# entrepôts
for i, (x0, z0) in enumerate(((4, 4), (26, 4), (4, 24))):
    F([x0, 0, z0, x0 + 16, 8, z0 + 14], "minecraft:gray_concrete", "walls")
    F([x0, 9, z0, x0 + 16, 9, z0 + 14], "minecraft:smooth_stone_slab")
    F([x0 + 7, 0, z0 + 14, x0 + 9, 4, z0 + 14], "minecraft:air")
    F([x0 + 2, 5, z0 + 14, x0 + 3, 6, z0 + 14], "minecraft:glass_pane"); F([x0 + 13, 5, z0 + 14, x0 + 14, 6, z0 + 14], "minecraft:glass_pane")
    for k in range(4):
        Bk(x0 + 3 + k * 3, 0, z0 + 4, "minecraft:barrel[facing=up]"); Bk(x0 + 3 + k * 3, 1, z0 + 4, "minecraft:barrel[facing=up]")
        Bk(x0 + 3 + k * 3, 0, z0 + 9, "minecraft:chest[facing=south]")
    Bk(x0 + 8, 7, z0 + 7, "minecraft:lantern[hanging=true]")
# conteneurs empilés
cols = ["minecraft:red_concrete", "minecraft:blue_concrete", "minecraft:orange_concrete", "minecraft:green_concrete"]
for k in range(8):
    x = 48 + (k % 4) * 6; z = 6 + (k // 4) * 5
    F([x, 0, z, x + 4, 2, z + 2], cols[k % 4], "walls") ; F([x, 3, z, x + 4, 3, z + 2], cols[(k + 1) % 4])
# grues
for (gx, gz) in ((64, 14), (64, 36)):
    F([gx, 0, gz, gx, 18, gz], "minecraft:iron_bars"); F([gx + 1, 0, gz, gx + 1, 18, gz], "minecraft:iron_bars")
    F([gx - 12, 19, gz, gx + 14, 19, gz], "minecraft:yellow_concrete")
    F([gx + 12, 12, gz, gx + 12, 18, gz], "minecraft:chain[axis=y]"); Bk(gx + 12, 11, gz, "minecraft:hopper[facing=down]")
# bureau du capitaine / billetterie
F([30, 0, 44, 42, 5, 54], "minecraft:spruce_planks", "walls"); F([30, 6, 44, 42, 6, 54], "minecraft:dark_oak_slab")
F([35, 0, 44, 36, 2, 44], "minecraft:air"); F([31, 3, 44, 33, 4, 44], "minecraft:glass_pane")
Bk(36, 0, 50, "minecraft:lectern[facing=south]"); Bk(32, 0, 52, "minecraft:red_bed[facing=south,part=foot]"); Bk(32, 0, 51, "minecraft:red_bed[facing=south,part=head]")
# le Black Whale : grande coque noire le long du ponton
F([84, -4, 4, 98, 4, 58], "minecraft:black_concrete", "walls")
F([84, -4, 4, 98, -4, 58], "minecraft:black_concrete")
F([86, -4, 2, 96, 4, 3], "minecraft:black_concrete"); F([88, -4, 0, 94, 4, 1], "minecraft:black_concrete")
F([86, -4, 59, 96, 4, 60], "minecraft:black_concrete")
F([84, 5, 4, 98, 5, 58], "minecraft:dark_oak_planks"); F([85, 5, 3, 97, 5, 3], "minecraft:dark_oak_planks")
F([83, 6, 4, 83, 6, 58], "minecraft:iron_bars"); F([99, 6, 4, 99, 6, 58], "minecraft:iron_bars")
F([86, 5, 20, 96, 12, 44], "minecraft:gray_concrete", "walls"); F([86, 13, 20, 96, 13, 44], "minecraft:smooth_stone_slab")
for y in (7, 10): 
    for z in range(22, 44, 3): F([86, y, z, 86, y, z + 1], "minecraft:glass_pane"); F([96, y, z, 96, y, z + 1], "minecraft:glass_pane")
for z in (14, 52):
    F([90, 6, z, 92, 14, z + 2], "minecraft:black_concrete"); F([90, 15, z, 92, 15, z + 2], "minecraft:red_concrete")
F([84, 5, 31, 84, 8, 33], "minecraft:air")                          # passerelle
F([84, -1, 4, 84, 3, 58], "minecraft:black_concrete")
F([75, 5, 30, 83, 5, 34], "minecraft:dark_oak_planks"); F([75, 6, 30, 83, 6, 30], "minecraft:dark_oak_fence"); F([75, 6, 34, 83, 6, 34], "minecraft:dark_oak_fence")
F([86, 6, 21, 95, 10, 43], "minecraft:air")
F([86, 5, 31, 86, 8, 33], "minecraft:air")
for z in range(8, 58, 8): Bk(85, 6, z, "minecraft:lantern[hanging=false]")
# lampadaires du quai
for x in range(6, 70, 8):
    for z in (2, 42): Bk(x, 0, z, "minecraft:stone_brick_wall"); Bk(x, 1, z, "minecraft:stone_brick_wall"); Bk(x, 2, z, "minecraft:lantern[hanging=false]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("capitaine_baleine_noire", 36, 0, 49, 0); npc("docker_baleine_noire", 56, 0, 20, 90)
json.dump({"x": 5385, "z": 2130, "clear": [0, 0, 0, 82, 30, 62], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/black_whale_port.json", "w"), indent=1)
print(len(fills), len(blocks))
