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
