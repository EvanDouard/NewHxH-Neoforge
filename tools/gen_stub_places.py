#!/usr/bin/env python3
"""Génère les lieux « stub » (strict minimum) : un bâtiment creux avec porte, destiné à être remplacé par un fichier exporté
(`/hunter place export`). Relancer ne touche pas aux fichiers déjà modifiés à la main (option --force pour réécrire)."""
import json, os, sys
OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/data/hxh/world_places')
# id: (x, z, largeur, profondeur, hauteur, bloc de mur)
STUBS = {
    'greed_antokiba': (67500, 80800, 15, 15, 8, 'stone_bricks'),
    'greed_soufrabi': (68300, 80800, 15, 15, 8, 'stone_bricks'),
    'greed_aiai': (69100, 80800, 15, 15, 8, 'stone_bricks'),
    'greed_limeiro': (67500, 82200, 15, 15, 8, 'stone_bricks'),
    'greed_jispa': (68300, 82200, 15, 15, 8, 'stone_bricks'),
    'greed_pukku': (69100, 82200, 15, 15, 8, 'stone_bricks'),
    'yorknew_residential': (13120, 77690, 31, 21, 20, 'bricks'),
    'padokea_gare': (52320, 28000, 21, 13, 8, 'stone_bricks'),
    'padokea_hopital': (52320, 28030, 21, 13, 10, 'white_concrete'),
    'padokea_commissariat': (52320, 28060, 17, 13, 8, 'blue_terracotta'),
    'padokea_ecole_assassins': (52320, 28090, 25, 17, 12, 'deepslate_bricks'),
    'padokea_prison': (52320, 28125, 25, 17, 8, 'stone_bricks'),
    'gorteau_caserne': (52200, 100140, 25, 15, 8, 'stone_bricks'),
    'gorteau_centre_selection': (52200, 100175, 21, 15, 10, 'quartz_bricks'),
}
force = '--force' in sys.argv
for name, (x, z, w, d, h, wall) in STUBS.items():
    path = os.path.join(OUT, name + '.json')
    if os.path.exists(path) and not force:
        continue
    cx = w // 2
    j = {
        'x': x, 'z': z,
        'clear': [-1, 0, -3, w, h + 2, d],
        'foundation': [-1, -3, w, d, 'minecraft:stone_bricks'],
        'fills': [
            {'box': [0, -1, 0, w - 1, -1, d - 1], 'block': 'minecraft:stone_bricks'},
            {'box': [0, 0, 0, w - 1, h - 1, d - 1], 'block': 'minecraft:' + wall, 'mode': 'walls'},
            {'box': [0, h, 0, w - 1, h, d - 1], 'block': 'minecraft:stone_brick_slab'},
            {'box': [cx - 1, 0, 0, cx + 1, 2, 0], 'block': 'minecraft:air'},
            {'box': [cx - 1, -1, -3, cx + 1, -1, -1], 'block': 'minecraft:dirt_path'},
        ],
        'blocks': [], 'entities': [],
    }
    json.dump(j, open(path, 'w'), indent=1)
    print('écrit', name)
