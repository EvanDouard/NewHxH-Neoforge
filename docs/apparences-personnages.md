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
rendu ci-dessus dès qu'il existe (`NpcRenderer` délègue à `NpcGeoRenderer`). Personnages faits : **Gon** (pilote), **Killua**, **Kurapika**, **Leorio**, **Hisoka**, puis **Netero**, **Meruem**, **Neferpitou**, **Shaiapouf**, **Youpi**, **Chrollo**, **Uvogin**, **Illumi**, **Zeno**, **Biscuit**, **Feitan**, **Nobunaga**, **Machi**, **Silva**, **Kite**, **Kalluto**, **Shalnark** et **Pakunoda**.

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
short déchiré, chevelure qui « coule » vers le ciel comme une flamme noire : 37 mèches ascendantes (`flow` dans
`gon_adulte.py` : inclinaison vers l'extérieur à la racine puis virage continu vers l'axe), en couronne de base
large, anneau intermédiaire, cœur très haut, mèches courtes de remplissage et 3 mèches fines qui dépassent ;
base épaisse, pointe effilée, trois plans de profondeur), boîte de collision 0,95 × 2,5, explosion
d'aura, rugissement et message aux joueurs proches. NBT `HxhAdult:1b` pour le faire apparaître déjà adulte.

### Animations des cheveux (Gon adulte)

Rig généré par `tools/models/gon_adulte.py` (il n'y a pas de fichier Blockbench : le modèle est produit par le
script) :

```
head
└ hair_root            base fixe : calotte, frange (ab_*)
  └ hair_growth_root   déploiement / croissance (réservé à hair_transform)
    ├ hair_back        couche arrière
    ├ hair_core        cœur : masse centrale + mèches du milieu
    └ hair_front       couche avant
       └ <mèche>_0 → <mèche>_1 → … → <mèche>_n   base → milieu → pointe (pivots à l'origine de chaque tronçon)
```

Contrôleur `hair_motion_controller` (GeckoLib 4.6.6, dans `GonUltimeEntity`, actif seulement en adulte) :
`animation.gon.hair_idle`, boucle de 4 s, n'anime que les tronçons `<mèche>_1…n` (85 os) : racines immobiles,
amplitude croissante vers la pointe (rotations cumulées le long de la chaîne), phase et axe propres à chaque
mèche, 2 à 3° au plus pour garder la silhouette verticale.

Répartition prévue pour éviter les conflits (dans GeckoLib 4, deux contrôleurs qui animent le même os ne
s'additionnent pas : le dernier gagne) : `hair_transform` sur `hair_growth_root`, `hair_aura` et `hair_burst` sur
les couches et les racines `<mèche>_0`, dans un second contrôleur `hair_fx_controller`. Le mouvement passif ne
touche jamais ces os, donc les effets se composent par la hiérarchie.

### Kurapika

`tools/models/kurapika.py`, deux versions : `kurapika` (avec le tabard) et `kurapika_sans_tunique`.
- Proportions propres, plus élancées que Gon et Killua : tête 8,6 × 8 × 8, jambes plus longues (hanches à 10 px),
  épaules plus étroites ; ≈ 1,85 bloc (boîte de collision 0,6 × 1,85).
- Cheveux (refonte, silhouette d'abord) : volume arrondi fait de plaques tangentes à une sphère qui épouse le
  crâne (anneaux d'élévation décalés, `_shell_panel` : os de lacet puis os d'élévation, donc aucune marche
  horizontale), plus plein à l'arrière ; carré en mèches larges sur deux couches qui sortent de ce volume (côtés
  qui encadrent le visage, arrière qui suit l'arrondi jusqu'à la nuque), frange de 4 mèches principales et 2 fines,
  mèches des tempes. Aperçus par étape : `build_hair_stage1`, `build_hair_stage2`, `build_head_preview`.
- Version A : tabard bleu épais par-dessus la tunique (panneaux qui s'écartent du corps en descendant, côtés
  ouverts, épaules qui enveloppent, grand col relevé ouvert devant, bordures et filets dorés en relief, broderie,
  doublure, plis), manches blanches amples avec revers bleu et or.
- Version B : tunique blanche longue, col montant bordé de bleu, bande croisée et attaches dorées, ceinture bleue
  nouée sur le côté, pans qui s'évasent fendus sur les côtés.
- Yeux écarlates en combat (`<id>_froid.png`), boucle d'oreille, cinq chaînes à la main droite.
- Mobs `hxh:kurapika` et `hxh:kurapika_sans_tunique` (un œuf chacun, kit `kurapika`, neutres, alliés de Gon et
  Killua) ; PNJ `kurapika` et `kurapika_sans_tunique` avec les mêmes modèles.

### Leorio

`tools/models/leorio.py` : grand (≈ 1,95 bloc, boîte 0,65 × 1,95), longues jambes, épaules larges ; cheveux noirs
courts hérissés vers le haut et l'arrière (houppe devant, mèches organiques courtes et épaisses) ; visage allongé,
mâchoire carrée, sourcils épais, grand sourire, petites lunettes rondes aux verres bleutés en relief (monture,
pont, branches) ; costume bleu marine (veste ouverte à revers en relief, épaules carrées, pans et fente au dos qui
suivent le bassin, gilet, chemise, cravate, pantalon à pli, chaussures de ville) ; mallette de médecin (os
`briefcase`, fermoirs dorés). Expression `froid` = colère (il crie). Mob `hxh:leorio` (œuf, kit `leorio`, neutre,
allié des autres personnages) et PNJ `leorio` avec le même modèle.

### Hisoka

`tools/models/hisoka.py` : grand et mince (≈ 1,95 bloc) ; cheveux magenta plaqués vers le haut et l'arrière en
grandes mèches pointues (front dégagé) ; yeux dorés étroits, sourcils fins relevés, sourire en coin, étoile
turquoise et larme rose sur les joues ; haut clair près du corps à col en V bordé de rose, trèfle, carreau et cœur
sur la poitrine, pique dans le dos, bandes roses aux épaules et aux poignets ; écharpe-ceinture magenta nouée sur le
côté ; pantalon clair resserré aux chevilles, chaussures violettes à pointe relevée ; trois cartes en éventail dans
la main droite (os `cards`). Expression `froid` : sourire carnassier. Mob `hxh:hisoka` (œuf, kit `hisoka`, neutre
pour l'instant) et PNJ `hisoka` avec le même modèle.

### Série « même style » : outils communs

Les personnages suivants partagent `tools/models/adult.py` (`Body` : proportions ; `skeleton` : mêmes os que Gon ;
`head_box`, `write_expression`) et `tools/models/integrate.py`, qui les branche en une commande
(`python3 integrate.py <ids…>` : entité, attributs, œuf, onglet créatif, rendu, preset `mob_presets/<id>.json`, textes
fr/en, GameTest `<Id>IsInTheGameWithHisKit`, bloc de captures 6 angles + expression dans `ci/scenario.txt`).
Cheveux : sous-couche arrondie `chibi.scalp` (plaques tangentes qui bouchent les trous) + mèches `chibi.crown_lock`
qui partent d'une sphère autour du crâne, suivent l'arrondi puis retombent (az 0 = arrière, 90 = gauche, 180 =
devant) ; les rideaux de cheveux longs sont des `chibi.lock` qui pendent de l'équateur de la sphère.
Tous sont des mobs neutres (alliés entre eux) avec leur kit Hatsu.

| Personnage | Taille | Cheveux | Tenue / détails | Expression `froid` |
|---|---|---|---|---|
| Netero | 1,6 | crâne chauve, couronne blanche, chignon relevé | longue barbe, sourcils tombants, chemise 心 | regard de combat |
| Meruem | 2,0 | crête de carapace à deux lobes, antennes | cape, queue | regard glacial |
| Neferpitou | 1,8 | mèches ondulées blanches, frange, oreilles de chat | queue de chat | regard de prédateur |
| Shaiapouf | 1,95 | mèches blondes, frange balayée | jabot, quatre ailes de papillon | colère |
| Youpi | 2,4 | crête de cornes recourbées | gilet ouvert, colosse | rage, crocs |
| Chrollo | 1,9 | noir plaqué vers l'arrière du front à la nuque | croix au front, boucles bleues, manteau à col de fourrure, croix inversée dans le dos | regard de chef |
| Uvogin | 2,3 | crinière de lion châtain jusqu'au milieu du dos, front dégagé | torse nu, peau de bête en bandoulière | rugissement |
| Illumi | 1,95 | noir raide jusqu'à la taille, frange droite, mèches devant les épaules | yeux noirs vides, tunique, aiguilles | yeux plus grands |
| Zeno | 1,6 | pointes blanches, longue tresse dans le dos | moustache fine, tunique « 一日一殺 » | regard d'assassin |
| Biscuit | 1,4 | blond, frange pointue, couettes hautes à rubans rouges | robe rose à volants | sourire de combattante |
| Feitan | 1,55 | noir en bataille, longue frange entre les yeux | col montant à mâchoire de crâne, long manteau | pupilles rouges |
| Nobunaga | 1,85 | tirés en chignon au sommet, queue dans le dos | kimono, hakama, katana à la ceinture | regard de duel |
| Machi | 1,75 | queue de cheval haute rose, mèches qui encadrent le visage | haut de kimono indigo, obi, fils de Nen | regard tranchant |
| Silva | 2,1 | immense crinière blanche jusqu'au bas du dos | tunique bleu nuit à col montant | regard du chef |
| Kite | 1,85 | longs cheveux blancs jusqu'à la taille | casquette souple, long manteau | regard de chasseur |
| Kalluto | 1,35 | carré noir lisse au menton, frange droite épaisse | kimono violet à fleurs, obi lavande, éventail | regard vide |
| Shalnark | 1,75 | blond court et léger, frange en pointes | veste kaki ouverte, jean, téléphone à antenne | sourire figé (mode automatique) |
| Pakunoda | 1,85 | blond platine mi-long, raie sur le côté, pointes qui rebiquent | tailleur noir, chemise claire | regard grave |
