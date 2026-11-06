# Tour Céleste : construire et importer ses halls

Ce guide explique comment remplacer les halls générés de la Tour Céleste par tes propres constructions :
le hall d'entrée, le hall de chaque tranche d'étages (avec son ring central) et la chambre des joueurs.

## Comment la Tour est organisée

| Lieu | Ce qu'on y trouve | Structure à fournir (optionnelle) |
| --- | --- | --- |
| Hall d'entrée (étage 0) | Arrivée dans la Tour, hôtesse d'inscription, ascenseur, sortie | `hxh:heavens_arena/lobby` |
| Hall d'une tranche (1–50, 51–100…) | Ring central visible des spectateurs, hôtesse, arbitre, ascenseur | Celle que tu indiques dans le fichier de la tranche |
| Chambre d'un joueur | Lit, coffre, ascenseur ; le joueur peut la décorer | `hxh:heavens_arena/room` |
| Tour extérieure (Overworld) | La tour visible dans le monde, avec ses portails d'entrée | `hxh:heavens_arena/tour` |

Actuellement, les 15 tranches utilisent ton arène `hxh:heavens_arena/arene` (ring de 21 × 21,
`"ring_radius": 10`). La tour extérieure apparaît une fois par monde (près de l'origine, dans une plaine, une
savane, une prairie ou une forêt) ; dans un monde existant, `/place structure hxh:heavens_arena_tower` la pose.

Tous les joueurs dont l'étage est dans la même tranche partagent le même hall et le même ring.
Un seul combat à la fois par ring : les autres joueurs attendent dans une file et regardent.

Les 15 tranches sont définies dans `src/main/resources/data/hxh/arena_brackets/`, un fichier par tranche
(`001_050.json`, `051_100.json`… `251_251.json`).

## 1. Construire en créatif

Construis où tu veux dans un monde créatif. Les blocs utiles sont dans l'onglet créatif du mod :

| Bloc | Lettre | Rôle |
| --- | --- | --- |
| Repère : arrivée de l'ascenseur | E (vert) | Où le joueur apparaît en arrivant dans ce lieu |
| Repère : hôtesse | H (jaune) | Où se tient l'hôtesse |
| Repère : arbitre | R (gris) | Où se tient l'arbitre qui lance les combats (halls de tranche) |
| Repère : départ du joueur | J (bleu) | Où le joueur se place au début du combat |
| Repère : départ de l'adversaire | A (rouge) | Où l'adversaire se place |
| Ascenseur | — | Le vrai bloc d'ascenseur (pose-le sur 2 de haut) |
| Sortie | — | Le vrai bloc de sortie (hall d'entrée seulement) |

Règles :

- Pose chaque repère **posé sur le sol, à hauteur des pieds**. Il est remplacé par de l'air quand le hall est construit.
- Le **ring** est un carré centré entre le repère J et le repère A. Sa taille se règle avec `ring_radius`
  (demi-côté, 9 par défaut, donc un ring de 19 × 19). **Sortir de ce carré = sortie de ring.**
  Dessine le ring à cette taille pour que ce soit clair pour les joueurs.
- Les spectateurs qui entrent dans le ring pendant un combat sont renvoyés au repère E :
  place E **hors du ring**.
- Un repère manquant prend une place par défaut. Sans repère H ou R, pas d'hôtesse ou pas d'arbitre.
- Tu peux poser **plusieurs repères H et R** : une hôtesse ou un arbitre apparaît sur chacun.
  N'importe lequel des arbitres lance le combat. Un seul repère J, A et E compte : le premier trouvé.
- Taille maximale conseillée : environ 300 × 300 blocs pour un hall.

## 2. Sauvegarder

**Le plus simple, quelle que soit la taille** : vise deux coins opposés de ta construction (F3 donne leurs
coordonnées dans « Targeted Block »), puis :

```
/hunter structure save hxh:heavens_arena/<nom> X1 Y1 Z1 X2 Y2 Z2
```

Le fichier arrive au même endroit qu'avec un bloc de structure (voir plus bas).

**Avec un bloc de structure** (limité à 48 × 48 × 48) :

1. `/give @s minecraft:structure_block`, pose-le dans un coin de ta construction.
2. Mode **Save** (Sauvegarde). Nom de la structure : `hxh:heavens_arena/<nom>`, par exemple
   `hxh:heavens_arena/tranche_1_50`, ou `hxh:heavens_arena/lobby` pour le hall d'entrée.
3. Règle la taille et la position relative pour englober toute la construction (le bouton **Detect**
   aide si tu as posé un bloc de structure « Corner » au coin opposé), puis **Save**.

Le fichier arrive dans ton monde :
`saves/<monde>/generated/hxh/structures/heavens_arena/<nom>.nbt`

Sur un serveur : `<monde>/generated/hxh/structures/heavens_arena/<nom>.nbt`.

## 3. Dire au mod où l'utiliser

- **Hall d'entrée** : rien à faire, la structure `hxh:heavens_arena/lobby` est prise automatiquement.
- **Chambre** : rien à faire, `hxh:heavens_arena/room` est prise automatiquement pour les nouvelles chambres
  (les chambres déjà créées ne sont pas écrasées, pour ne pas effacer la déco des joueurs).
- **Hall d'une tranche** : ajoute `"structure"` dans le fichier de la tranche, par exemple `001_050.json` :

```json
{
  "from": 1,
  "to": 50,
  "structure": "hxh:heavens_arena/tranche_1_50",
  "ring_radius": 9,
  "y_offset": 0
}
```

La structure est centrée sur le hall et sa couche du bas devient le sol (on marche juste au-dessus).
Si ta construction a des fondations sous le sol, mets `"y_offset": -N` pour la descendre de N blocs.

Les autres champs (`floor`, `border`, `accent`, `lobby_floor`, `pillar`, `light`…) ne servent qu'au hall
généré, quand il n'y a pas de structure.

## 4. Tester dans ton monde, sans recompiler le mod

1. Le `.nbt` sauvegardé dans ton monde est trouvé directement.
2. Pour le fichier de tranche, crée un datapack dans ton monde :

```
saves/<monde>/datapacks/tour/
├── pack.mcmeta
└── data/hxh/arena_brackets/001_050.json
```

`pack.mcmeta` :

```json
{ "pack": { "pack_format": 48, "description": "Mes halls de la Tour Céleste" } }
```

Ton `001_050.json` remplace celui du mod (même nom = même tranche).

3. En jeu : `/reload`, puis `/hunter arena rebuild`, puis va dans le hall :
   `/hunter arena floor 25` puis `/hunter arena goto 25`.

Un hall n'est reconstruit que si son fichier change ou après `/hunter arena rebuild`. Après avoir
re-sauvegardé une structure, refais `/hunter arena rebuild` puis retourne dans le hall.

Commandes utiles (admin) : `/hunter arena floor <n>` (régler son étage), `/hunter arena register`
(s'inscrire d'office à sa tranche), `/hunter arena goto <étage>` (aller au hall de la tranche de cet étage),
`/hunter arena room` (sa chambre), `/hunter arena fight` (lancer son combat).

## 5. Intégrer au mod

Copie les fichiers dans le dépôt :

- la structure : `src/main/resources/data/hxh/structure/heavens_arena/<nom>.nbt`
- le fichier de tranche modifié : `src/main/resources/data/hxh/arena_brackets/<tranche>.json`

Tu peux aussi les envoyer à Claude, qui les ajoute, pousse et vérifie en CI.

## Partir de l'exemple

Le mod contient un petit hall d'exemple, `hxh:heavens_arena/exemple`, avec tous les repères.
Pour le charger : bloc de structure en mode **Load**, nom `hxh:heavens_arena/exemple`, **Load**.
Modifie-le, puis sauvegarde-le sous un nouveau nom.

## Importer depuis une map existante

Si ta construction est dans une autre map (téléchargée ou ancienne sauvegarde) :

1. Ouvre cette map en créatif (copie-la dans `saves/` si besoin).
2. Pose les repères (onglet du mod) aux bons endroits.
3. Sauvegarde avec un bloc de structure comme à l'étape 2 : le `.nbt` est créé dans **cette** map,
   sous `generated/hxh/structures/`.
4. Copie ce `.nbt` dans ton monde de test (même chemin) ou directement dans le mod (étape 5).

Pour une construction plus grande que 48 × 48 × 48, utilise `/hunter structure save` (étape 2) : pas de
découpage nécessaire.
