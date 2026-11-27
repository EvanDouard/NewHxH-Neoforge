# À trancher (Evan)

Valeurs posées par Claude pour avancer ; réponds simplement « on garde » ou donne la nouvelle valeur.
Le détail chiffré est dans `equilibrage-final.md`.

## Nen et progression
- [ ] **Palier du Zetsu** : In 5 (≈ 4 min, valeur actuelle) ou In 30 (≈ 6 h d'In). Réglage : `senses.zetsuUnlockInLevel`.
- [ ] **Palier du Ken** : Ten 5. Réglage : `senses.kenUnlockTenLevel`.
- [ ] **Surcoût des débutants** : ×5 à la maîtrise 0, ×2 au niveau 5, ×1 au niveau 20 (`aura.beginnerCostMultiplier`, `aura.beginnerCostAtLevel5`, `aura.beginnerCostUntil`).
- [ ] **Gyo** : Ren 5 + In 3 (le lien In → Gyo vient d'être ajouté). Le seuil In 3 te convient-il ?
- [ ] **Talent de Nen** : le joueur connaît-il son talent tout de suite, ou le découvre-t-il (maître, ordinateur Hunter) ? Les mobs ont-ils un talent ?
- [ ] **Niveau 100** = Meruem / Gon adulte, Netero ≈ niveau 80–90 : à revoir en jeu (Force +0,5/niveau, Vitalité +0,4, Endurance 0,1).

## Techniques et types
- [ ] Seconds types des techniques (estimation de départ dans `NenAffinity`), à corriger technique par technique.
- [ ] Pénalité hors de son type : dégâts × efficacité (100/80/60/40 %), zones × (0,5 + 0,5 × efficacité), soit 70 % à l'opposé de l'hexagone. Bon dosage ?
- [ ] Types non confirmés dans l'œuvre : Pariston, Ging, Cheadle, Botobai, Menchi, Bushidora, Tsezguerra, Zushi.
- [ ] Aura sauvage par défaut (Hisoka, Illumi, Meruem, Neferpitou, Youpi, Feitan, Uvogin, Phinks, Genthru).
- [ ] Nouvelles techniques (partie 8 de `hatsu-et-techniques.md`) : lesquelles coder, et quand (reporté).

## Clans
- [ ] Part de l'XP de stat qui va au clan (1/5) et +30 XP de clan par coup secret (≈ 23 800 XP pour le niveau 100).
- [ ] Bonus de stats au niveau 100 : +10 par défaut (`xp_bonus × 50`), réglable par famille (`clan_stats`).
- [ ] Kurta : garder le passif « +30 % sous 40 % de vie » à côté des yeux écarlates, ou le retirer ?
- [ ] Effets et poids du tirage de chaque famille.

## Équilibrage du monde
- [ ] Hunter gourmet : prix des plats, XP de Vitalité, nourriture des plats, recettes III et IV, quêtes (ingrédients, récompenses).
- [ ] Renard-ours et Great Stamp : vie, dégâts, apparition, butin (museau blindé ×0,3).
- [ ] Hisoka et les combattants du 200e : étages et difficulté.
- [ ] Paris, malédiction de Nen, enchères, contrats, Blacklist et rang Hunter, coups secrets, serments, seuils des impacts.

## Monde HxH — emplacements à chercher (Evan)
- [ ] Chercher l'emplacement officiel/souhaité de **Meteor City**, **NGL** et **East Gorteau** (propositions provisoires dans `docs/feuille-route-monde.md` : ~(1 700, 3 900), ~(1 350, 4 950), ~(2 000, 5 000)).
- [ ] Valider ou déplacer le **Palais royal de Kakin** (6 000, 2 500).
- [ ] Peijin et Glam Gas Land : décrire ce que tu veux y trouver.

## Organisations (ajouté le 27 nov, à corriger)
- [ ] Nouvelles organisations posées par Claude : **Utilisateurs de Nen** (automatique avec le Nen) et **Fourmis-Chimères** (ennemies des Utilisateurs de Nen et de l'Association, hostiles à vue). Quelles autres organisations/hostilités veux-tu (Zoldyck, mafia, Kakin…) ?
- [ ] Solidarité : rayon de 24 blocs ; seuls les mobs qui savent se battre (personnages, fourmis) ripostent, les PNJ de décor restent passifs.

## Continent caché (27 nov)
- [ ] Lire `docs/continent-cache.md` (idées tirées des images, du manga et de théories) et choisir ce qu'on garde ou non : accès (bateau/Black Whale), cinq fléaux, biomes, créatures du lac.
- [ ] Nom : « Mimbo » (carte d'Evan) ou « Minbo » ?
