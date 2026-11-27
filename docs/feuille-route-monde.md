# Monde HxH — feuille de route des constructions (25 nov)

Source : liste de propositions collée par Evan (analyse du monde actuel, régions à enrichir, bâtiments transversaux, ordre de priorité). Evan la juge « très pertinente ».

## Décision d'Evan
- **On ne s'attarde pas sur l'Examen Hunter** : l'examen se déroule dans **une autre dimension**. Sont donc écartés du monde réel : Tour des astuces (Trick Tower), centre d'inscription, terrain de la 1re phase, marais Numere, île de Zevil, campement des candidats, poste des examinateurs, infirmerie. (L'hôtel de la 5e phase déjà construit reste en place.)
- Le reste de la liste est retenu.
- Rappel (déjà décidé) : pas de détails sur les structures, Evan repasse dessus à la main ; on pose emplacement, volume simple, borne/PNJ.

## Principes retenus
- **Agglomérations cohérentes** (rues, place, marché, auberge, PNJ) plutôt que monuments isolés.
- **Une identité architecturale par région** : Yorknew moderne, Kakin monumental/royal, NGL rural/naturel, Meteor City délabrée, Greed Island fantastique.
- **Trois niveaux** : 1 lieux emblématiques (détaillés) ; 2 bâtiments fonctionnels (intérieur, PNJ, marchands, coffres) ; 3 décors de remplissage (maisons, entrepôts, garages…).
- **Bibliothèque de bâtiments réutilisables** avec variantes par région : forge, auberge, bibliothèque, guilde des Hunters, marché, tour de garde, maisons, fermes, écuries, moulins, ponts, phares, ports secondaires, camps (chasseurs, bandits), mines, ruines, temples, sanctuaires, entrepôts, tours abandonnées.
- Objectifs à terme : 5-7 grandes villes, 10-15 villages, 15-20 lieux emblématiques, 30-40 bâtiments fonctionnels uniques, 50+ bâtiments résidentiels/commerces réutilisables, 20-30 donjons/ruines/camps.

## À construire par région (hors examen)
- **Padokea** : ville au pied de la montagne Kukuroo avec route jusqu'au domaine ; gare, quartier résidentiel, centre commercial, hôpital, commissariat, hôtel, restaurant, école d'assassins, terrain d'entraînement, prison.
- **Saherta / Yorknew** (déjà : tours, maison de ventes, entrepôt de la Brigade) : hôtel de luxe, banque centrale, QG de la mafia, cimetière, commissariat, hôpital, gare centrale, marché noir, musée, restaurant gastronomique, discothèque, appartements ; sous-sols secrets avec salles de vente clandestines.
- **Kakin** (déjà : port, Black Whale) : palais royal, parlement, caserne, ambassade, prison royale, temple, marché, gare maritime, quartier résidentiel, base militaire. **Black Whale — intérieurs** : cabines des princes, quartiers des gardes, hôpital, restaurants/cuisines, salles de réunion, entrepôts, prison, quartiers de la mafia, 3e classe.
- **Îles de l'Est** : Greed Island — Masadora, Antokiba, Soufrabi, Aiai, Limeiro, Jispa, Pukku, château, ruines ; lieux spéciaux : donjon, arène, temple abandonné, bibliothèque magique, tour de sorcier, sanctuaire, grotte aux trésors. Île de la Baleine — port de pêche, phare, village de pêcheurs, école, épicerie, cabane, campement forestier, sanctuaire du lac.
- **Régions à ajouter** : **Meteor City** (priorité très élevée : montagnes de déchets, bidonvilles, marché de récupération, cimetière de véhicules, usine, centre de tri, repaires de gangs, cachettes de la Brigade, tour de déchets) ; **NGL** (priorité très élevée : village, poste-frontière, ferme, moulin, scierie, centre médical, campement de chercheurs, ruines, nid des fourmis, palais souterrain de la Reine) ; **East Gorteau** (priorité élevée : palais royal, place, quartiers, caserne, hôpital, école, administratif, centre de sélection, souterrains).

## Ordre de priorité proposé (examen retiré)
| # | Construction | Région |
|---|---|---|
| 1 | Palais royal | Kakin |
| 2 | Meteor City | Meteor City |
| 3 | Ville de Masadora | Greed Island |
| 4 | Village NGL | NGL |
| 5 | Quartier général de la mafia | Yorknew |
| 6 | Palais de East Gorteau | East Gorteau |
| 7 | Ville autour de la Tour Céleste | Padokea |
| 8 | Port de l'île de la Baleine | Île de la Baleine |
| 9 | Quartier résidentiel de Yorknew | Yorknew |

(La Trick Tower, n°1 de la liste d'origine, est retirée : examen = autre dimension.)

## Prérequis techniques
- Meteor City, NGL, East Gorteau n'ont pas de destination de dirigeable ni de lieu sur la carte : placement à valider avec Evan (voir `docs/monde-hxh.md` §3), puis destination + zone aplatie.
- Bibliothèque de bâtiments réutilisables : à prévoir dans le système de places (blocs de structure paramétrables / sous-places avec variantes), pour ne pas écrire chaque maison en JSON.

## Emplacements proposés pour les régions sans lieu (25 nov) — à confirmer
Propositions « libre/théorie » (non officielles) sur la carte 8 000 × 5 650, d'après les zones de `cells.txt` ; **à vérifier par Evan** (voir `docs/a-trancher.md`).

| Lieu | Proposition (x, z) | Pourquoi |
|---|---|---|
| Palais royal de Kakin | (6 000, 2 500) — **construit (ébauche)** | centre de l'Empire de Kakin, terre ferme, entre le port et la forêt de Kite |
| Meteor City | ~ (1 700, 3 900) | terres sauvages du continent Yorbian, entre Yorknew et Greed Island |
| NGL | ~ (1 350, 4 950) | pointe sud (Union de Mitene), forêt/ferme isolée |
| East Gorteau | ~ (2 000, 5 000) | côte sud de Mitene, vers Peijin (2 445, 5 085) |

## Demandes d'Evan du 27 nov (ordre de traitement ; détail dans la feuille de route Docs)
1. Peur : effet « crise de panique » (vue qui se rétrécit, bords durs, zoom par à-coups). 2. Organisations en listes + nouvelles (Fourmis-Chimères) + hostilités (Utilisateur de Nen ↔ Fourmis-Chimères). 3. Neutres solidaires si l'un des leurs est attaqué. 4. Mobs plus autonomes (coups de pied, frappes chargées). 5. Succès. 6. Monde HxH sans structures classiques + biomes par lieu (lister d'abord). 7. Techniques qui grossissent avec la volonté : optimiser, niveau max 300. 8. Quêtes : ville/région à côté des coordonnées. 9. Échelle du monde (discussion : carte pré-générée ?). 10. Modèles validés par Evan. 11. Vitesse des personnages seulement en combat.

**Fait (8)** : les objectifs de quête à coordonnées ajoutent la région (`QuestManager.regionTail`, clés `airship.hxh.region.*`) ; GameTest `coordinatesComeWithRegionName`.

**Fait (7, 11)** : éclats de zone plafonnés à 12 victimes (`AreaScaling.MAX_SPLASH_TARGETS`, optimisation ; sommes de niveaux jusqu'à 300 inchangées) ; personnages à vitesse réduite de 60 % hors combat (`CharacterEntity.calmWhenIdle`), GameTest `charactersAreFastOnlyInCombat`.

**Fait (5)** : succès = advancements `data/hxh/advancement/*.json` (générés par `tools/gen_achievements.py`, critère « impossible ») accordés par `achievement/Achievements` (contrôle toutes les 2 s + achat/fabrication immédiats) ; GameTest `achievementsAreGrantedFromPlayerState`, captures 99zzi/99zzj. Pistes à valider avec Evan : liste des succès, ajout de succès par boss/personnage.

**Fait (6, partie 1)** : monde HxH sans structures vanilla (`world/HxHChunkGenerator`, `createStructures` vide, type `hxh:world` dans `dimension/world.json`) ; les chunks déjà générés gardent leurs structures. **Reste (6, partie 2)** : biomes par lieu (voir la liste proposée dans le chat / `docs/monde-hxh.md`).

**Fait (28 nov, carte)** : Padokea colorée/nommée, archipel de Mitene (NGL déplacé), seconde carte « Continent Noir connu » + variable `darkContinentKnown`. À trancher : l'événement qui rend la variable vraie (quête, PNJ, livre ?).
