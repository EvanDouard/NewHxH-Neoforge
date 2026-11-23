# Apparences des personnages

Chaque personnage a une **apparence** : `assets/hxh/looks/<id>.json` (même id que le PNJ, ex. `npc:"netero"`).
Elle donne le skin (format joueur 64x64, deux couches), les bras fins ou non, la taille (largeur, hauteur,
profondeur) et des **cubes en plus** accrochés aux membres — cheveux en pointes, barbe, chignon, ventre… — avec
leur propre texture `<id>_extra.png`. Ces cubes suivent la tête, les bras et les jambes, animations de combat comprises.

Personnages faits : Netero, Satotz, Menchi, Buhara, Beans, Gon, Killua, Kurapika, Leorio, Hisoka.

## Modifier ou ajouter un personnage

Tout est généré par `tools/skins/characters.py` (Python 3 + Pillow) :

```
python3 tools/skins/characters.py            # régénère tout
python3 tools/skins/characters.py netero     # un seul
```

Le script écrit les PNG, le JSON et un **aperçu 3D** dans `build/skin-previews/<id>.png` (face, trois-quarts,
profil, dos + les textures), pour vérifier sans lancer le jeu.

- Le visage et les vêtements se dessinent en lettres (`grid`) : une lettre = une couleur de la palette.
- Les cubes en plus se déclarent avec `ex.cube(membre, coin, taille, peinture, pivot=…, rot=(x, y, z))`, en pixels,
  comme dans Blockbench (Y vers le bas, l'avant regarde vers -Z, la droite du personnage est en -X).
- On peut aussi retoucher les PNG à la main (ou dans Blockbench) : le jeu ne lit que les PNG et le JSON.

Sans fichier d'apparence, un PNJ utilise `textures/entity/npc/<id>.png`, sinon Steve. F3+T recharge les apparences.

## Modèles sur mesure (GeckoLib)

Les personnages refaits au niveau décrit dans `docs/Analyse_model.md` ont un vrai modèle GeckoLib, qui remplace le
rendu ci-dessus dès qu'il existe (`NpcRenderer` délègue à `NpcGeoRenderer`). Personnages faits : **Gon** (pilote) et **Killua**.

```
python3 tools/models/gon.py
python3 tools/models/killua.py
```

`tools/models/chibi.py` regroupe ce qui est commun au style « Minecraft anime » : squelette chibi (mêmes os,
donc mêmes animations), tissus, peau, mèches en lames triangulaires en escalier (`blade`) ou continues (`strand` : tranches de ≈ 0,5 px, courbure et
torsion, pointe effilée ; utilisée par Gon) ou en ailettes croisées (`spire` : deux triangles pleins croisés à angle droit, flancs
formés par des plaques inclinées, donc sans marches, effilés de face comme de profil ; utilisée par Killua), peinture des cheveux
(couleurs ombre / lumière / reflet au choix) et grille de dessin du visage. **Mèches organiques (`lock`, méthode actuelle de Gon et Killua).** Une mèche = 2 ou 3 tronçons enchaînés
(un os chacun) qui changent un peu de direction à chaque jonction (courbure) ; la racine est plus large et sort
de la masse en s'évasant ; chaque tronçon = deux ailettes en trapèze croisées (flancs droits, pas de marches),
aplatissement réglable par mèche ; la pointe s'affine en largeur et en épaisseur. `arched` pose une liste de
mèches (racine qui penche vers l'extérieur puis se redresse). Hiérarchie : grandes, intermédiaires, petites.

Chaque nouveau personnage part de là.

**Expressions.** Une texture `npc_geo/<id>_<expression>.png` (mêmes UV, seul le visage change) remplace la
texture habituelle quand le PNJ a cette expression : imposée en NBT (`{expression:"froid"}`), sinon `froid`
automatiquement quand il combat. Killua a `killua_froid.png` (regard d'assassin : paupières baissées, iris
sombre, pupille réduite à un point).

Le script (`tools/models/geolib.py` + un fichier par personnage) écrit :

- `assets/hxh/geo/entity/npc/<id>.geo.json` — os (corps, torse, tête, cheveux, bras, avant-bras, mains, os
  d'objet, jambes, mollets) et cubes ;
- `assets/hxh/animations/entity/npc/<id>.animation.json` — animations `idle` et `walk` (communes) ;
- `assets/hxh/textures/entity/npc_geo/<id>.png` — chaque face peinte à densité fixe (4 pixels de texture par
  pixel de modèle, 6 pour le visage) et rangée automatiquement ;
- `build/model-previews/<id>.png` — aperçu 3D (face, trois-quarts, profil, dos et texture).

Les fichiers s'ouvrent aussi dans Blockbench (format « GeckoLib animated model ») pour les retoucher à la main.
Repère : Y vers le haut, pieds à 0, l'avant vers -Z, le côté gauche du personnage en +X. GeckoLib inverse les
rotations X et Z par rapport à l'aperçu : le script les inverse à l'écriture (`to_geckolib`).


## Personnages en mob (Gon, Killua)

Types d'entité `hxh:gon` et `hxh:killua` (classe `mob/CharacterEntity`), avec un œuf d'apparition chacun dans
l'onglet du mod. Même modèle GeckoLib que le PNJ du même nom, plus une animation `attack` (coup de poing) jouée
quand ils frappent ; en combat, Killua prend sa texture `killua_froid`. Stats, Nen et Hatsu viennent du mob
d'exemple `data/hxh/mob_presets/<id>.json` (kit `gon` : Jajanken ; kit `killua` : Kanmuru, Shippu Jinrai, yo-yo,
poing électrique) et l'IA lance les techniques comme pour tout mob au Nen (`NenMobAI`). Neutres pour l'instant :
ils se promènent, regardent les joueurs, ne ripostent qu'à qui les frappe, et les autres personnages proches
viennent aider. Pour ajouter un personnage : un type dans `ModEntities.character(id)`, son œuf, son mob
d'exemple, ses traductions et son modèle `tools/models/<id>.py`.

### Gon ultime et Gon adulte

`hxh:gon_ultime` (œuf « Gon (ultime) », classe `mob/GonUltimeEntity`) se bat comme Gon enfant (Nen 50 : pas de
Jajanken ultime, qui demande le niveau 60). Sous 30 % de vie, il passe une fois pour toutes en **mode adulte** :
stats ×3 (plafonnées à 100), Nen 100 et maîtrises 80 (Jajanken ultime accessible, et c'est sa prochaine technique),
aura et vie pleines, modèle `gon_adulte` (`tools/models/gon_adulte.py` : torse nu musclé, épaules très larges,
short déchiré, chevelure qui monte très haut et retombe dans le dos), boîte de collision 0,95 × 2,5, explosion
d'aura, rugissement et message aux joueurs proches. NBT `HxhAdult:1b` pour le faire apparaître déjà adulte.
