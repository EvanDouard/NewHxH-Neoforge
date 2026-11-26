# Greed Island — conception des deux entrées (décision d'Evan, 25 nov)

L'île (3366, 4110) est un lieu du monde réel HxH avec **deux manières d'y entrer**.

## 1) Par la console (entrée « jeu »)
- Le joueur utilise une **console + la cartouche du jeu**, puis entre dans le jeu avec le **Nen** (comme dans l'anime).
- On lance alors les bases de Greed Island : règles du jeu, anneau, cartes (Livre, Cartes de restriction, emplacements…).
- À faire : objets `console` et `cartouche_greed_island`, condition Nen (aura minimale), téléportation vers l'île, état « joueur du jeu » (anneau), mécanique de base du jeu.

## 2) À la nage / en bateau (entrée « sauvage »)
- Approcher l'île par la mer **déclenche un combat contre Razor** (gardien).
- **Razor battu** : le joueur accède à l'île **sans anneau**, avec le **système classique** (pas les règles du jeu).
- **Razor gagne** : il **épargne** le joueur et utilise la **carte Ruler** pour le **téléporter à un endroit** (lieu à choisir/aléatoire parmi des points de l'île).
- À faire : zone de déclenchement autour de l'île (détection joueur arrivant par la mer, hors console), entité/PNJ combattant **Razor** (kit de données, applicable à n'importe quelle LivingEntity), état « Razor vaincu » par joueur, effet Ruler (téléportation), cas du joueur déjà dans le jeu.

## Questions ouvertes pour Evan
- Points de téléportation de la carte Ruler (liste de lieux de l'île) ?
- Quel kit/stats pour Razor (difficulté) et que gagne-t-on en le battant (récompense, accès permanent) ?
- Le joueur entré par la console est-il enfermé dans le jeu (sortie par la Carte « Départ ») ?
