# Analyse des modèles de référence et lignes directrices pour nos personnages

Référence étudiée : les modèles du mod One Piece « Minepiece » (Kaido, Barbe Noire, Barbe Blanche), à partir de
5 captures en jeu et des fichiers fournis par Evan :

| Fichier | Contenu |
|---|---|
| `edward_newgate.geo.json` | Barbe Blanche, modèle d'entité GeckoLib (géométrie Bedrock 1.12), UV 128×128 |
| `edward_newgate_2.geo.json` | Variante de Barbe Blanche (cheveux et couvre-chef en plus), UV 64×64 |
| `edward_newgate.png` | Texture de Barbe Blanche, 1024×1024 |
| `naginata.json` | Le bisento de Barbe Blanche, modèle d'objet Java (Blockbench) |
| `clothes_kaidou_*.json` | Icônes des vêtements de Kaido : chapeau, plastron, jambières, bottes |

Ces fichiers servent à **comprendre la méthode**. On ne copie ni leur géométrie ni leurs textures dans le mod :
tout ce qu'on fera sera original.

---

## 1. Ce que montrent les fichiers (mesuré)

### 1.1 Technique

- **GeckoLib**. Le format `minecraft:geometry` 1.12, les os `armorHead`, `armorBody`, `armorLeftLeg`,
  `armorLeftBoot`… et les os d'objet `itemMainHand3` et `itemOffHand` sont les conventions de GeckoLib. Les
  personnages sont donc des entités GeckoLib, animées par des fichiers d'animation Blockbench, et non des modèles
  de joueur.
- **Les vêtements sont des armures.** `clothes_kaidou_chestplate`, `_leggings`, `_boots` sont des objets
  d'armure : les tenues se portent comme des armures GeckoLib (d'où les os `armor*` dans le squelette).
- **Les armes sont des modèles d'objet Java** (format bloc/objet de Blockbench, pas GeckoLib). Elles sont donc
  soumises aux contraintes de ce format : rotations uniquement de 0, ±22,5 ou ±45°, un seul axe par cube,
  texture 64×64.

### 1.2 Échelle et proportions (Barbe Blanche)

| Mesure | Barbe Blanche | Joueur Minecraft |
|---|---|---|
| Hauteur totale | ~75 px ≈ **4,7 blocs** | 32 px = 2 blocs (modèle) |
| Tête | 8×8×8 px (même taille que Steve) | 8×8×8 |
| Tête / hauteur | **1/9,4** | 1/4 |
| Torse | 22 de large × 31 de haut × 12 de profondeur | 8 × 12 × 4 |
| Largeur aux épaules (bras compris) | ~36 px | 16 px |
| Largeur aux hanches | 20 px | 8 px |
| Bras | ~29 px, en 2 segments, la main arrive à mi-cuisse | 12 px, 1 segment |
| Jambes | 30 px, 10×10 de section, en 2 segments | 12 px, 4×4 |

C'est la taille qui grandit, pas la tête : on garde une tête de 8 px et on étire le corps. La tête minuscule sur un
torse immense donne l'impression de gigantisme du manga.

**Pose de repos intégrée** au modèle : torse penché de 2,5°, bras écartés de 2,5°, jambes ouvertes de 7,5° avec
une légère rotation, avant-bras pliés de 7,5°. Même immobile, le personnage a une attitude.

### 1.3 Squelette

26 os (28 dans la variante), contre 6 pour un joueur :

```
body
├─ head ─ head2 / head3 (deux visages), hat, hair, armorHead
├─ torso ─ armorBody
│   └─ Body2 (épaulettes) ─ MantMain ─ Mant1 ─ Mant2 ─ Mant3   (manteau en 3 segments)
├─ right_arm ─ right_arm2 (coude) ─ itemMainHand3
├─ left_arm  ─ left_arm2  (coude) ─ itemOffHand
├─ right_leg ─ right_leg2 (genou) ─ armorRightBoot ; armorRightLeg
└─ left_leg  ─ left_leg2  (genou) ─ armorLeftBoot  ; armorLeftLeg
```

- **Coudes et genoux articulés** : chaque membre a deux os, ce qui permet de plier un bras pour frapper ou une
  jambe pour courir.
- **Le manteau est une chaîne de 3 os** (pivots en haut du dos, puis environ 22 px et 36 px plus bas, inclinés de
  +10°, 0° et −15°). On peut l'animer pour qu'il flotte et suive les mouvements, au lieu d'une plaque rigide.
- **Deux visages** : `head2` et `head3` sont deux têtes complètes (texture : un visage qui sourit, un visage
  crispé). L'animation affiche l'une ou l'autre, ce qui change l'expression sans changer le modèle.
- **Os vides de fixation** : `itemMainHand3` et `itemOffHand` placent l'arme dans la main ; les os `armor*`
  servent de support aux vêtements.

### 1.4 Le corps est construit en volumes

Sur les 27 à 32 cubes du torse :

- **pectoraux** : deux blocs de 9,4 × 8 × 2,1 px, tournés de ±5° pour bomber la poitrine ;
- **abdominaux** : six plaques de 7,5 × 2,7 × 1 px (trois rangées de chaque côté), tournées de ±5° — un vrai
  « six-pack » en relief ;
- **dorsaux** : blocs de 8 × 10 × 7 px inclinés de ±5° sur les flancs ;
- **trapèzes** : deux volumes inclinés de ±25° qui relient le cou aux épaules, et un cou de 7,6 × 6,3 × 5 px ;
- **épaules** : deltoïde de 7×9×7 px gonflé de 0,5, plus large que le haut du bras (6,4×9×6) ;
- **avant-bras** : manchette de 7,4 × 8 × 7 px plus large que le bras, puis la main (6,6 × 5,6 × 6,6) — d'où la
  démarcation nette entre le poignet et le poing.

### 1.5 Vêtements et accessoires

- **Pans de manteau** : des plaques de **0,64 px d'épaisseur**, gonflées de 0,25 à 0,28 (donc environ 1,1 px en
  tout), disposées en éventail autour du corps (rotations de 0, ±10, ±40, ±90, ±140°). La doublure rouge est une
  deuxième plaque collée derrière.
- **Boutons** : des cubes de 0,77 px, gonflés de 0,3.
- **Épaulettes** : un plateau de 7,7 × 0,9 × 7,7 px et, tout autour, des **plaques de franges de 0,13 px
  d'épaisseur** inclinées de 22,5°.
- **Pantalon bouffant** : des cubes gonflés de 0,4 puis de 0,7 au genou et en haut de la botte, pour imiter le
  tissu qui s'accumule.
- **Botte** : tige, semelle, talon et **pointe relevée** (un cube de 12,5 px de long incliné de 18°).
- **Moustache en croissant** : 7 cubes à des **angles libres** (par exemple −23,58° / 11,28° / 24,14°). Les
  modèles d'entité acceptent n'importe quel angle, pas seulement des multiples de 22,5°.
- **Bandana** : une bande de 14 × 6 × 8 px gonflée de 0,5 autour de la tête, et un dessus de 8 × 2,6 × 8.

### 1.6 Texture

- Les UV sont exprimées sur 128×128, mais **le PNG fait 1024×1024** : la texture est **8 fois plus fine** que ce
  que suggèrent les UV. Le visage (8×8 px de modèle) dispose de **64×64 pixels peints**, d'où le rendu « peinture
  numérique » de Kaido et les visages très expressifs.
- **Densité médiane : environ 3,4 pixels de texture par pixel de modèle**. Les écarts sont énormes : jusqu'à 18
  sur le visage, à peine 0,5 sur le manteau.
- **Les grandes surfaces réutilisent de petits morceaux de texture** : le manteau blanc étire une zone d'environ
  8×8 sur des pans de 19×22 px. C'est **la cause directe des aplats** relevés sur les captures (manteau en
  « plastique », cape de Kaido unie).
- 33 faces ont une taille UV nulle : on ne les voit jamais (tranches des plaques), donc elles n'occupent aucune
  place dans la texture.
- Contenu du PNG : deux visages, les oreilles, le bandana, la moustache, le torse découpé en panneaux (pectoraux,
  abdos, flancs avec cicatrices), l'or des épaulettes, le manteau blanc avec « 正義 » dans le dos, la doublure
  rouge à motif, le pantalon beige avec ses plis peints, l'emblème de l'équipage.
- L'ombre et la lumière sont **peintes** (dégradés doux sur les muscles, creux sombres), comme un rendu 3D figé.

### 1.7 L'arme (bisento)

- 95 éléments, texture 64×64, environ 1,8 bloc de long, **affichée ×1,75 en main** (≈ 3,1 blocs à l'écran).
- Organisation par groupes : manche (`motite`), lame (`blade`, avec les cubes du tranchant blanc ondulé), trois
  **anneaux** (`ring`) faits chacun de 16 petits cubes tournés de 0, ±22,5 et ±45° pour approcher un cercle.
- Des cubes minuscules (jusqu'à 0,05 px) pour les détails fins.

### 1.8 Comparaison avec nos personnages actuels

| | Barbe Blanche (Minepiece) | Notre Netero |
|---|---|---|
| Base | Entité GeckoLib sur mesure | Modèle du joueur + cubes en plus |
| Os | 26 | 6 (ceux du joueur) |
| Cubes | 192 à 247 | 12 + 7 en plus |
| Coudes / genoux | Oui | Non |
| Texture | 1024² (UV 128) | 64² + 64² |
| Densité | ≈ 3,4 texels/px | 1 texel/px |
| Taille | 4,7 blocs, tête 1/9 | Mise à l'échelle globale, tête 1/4 |
| Expressions | 2 visages | 1 |
| Vêtements animables | Manteau en 3 segments | Non |
| Peinture | À la main | Générée par script |

---

## 2. Ce qui change par rapport à ma première analyse (captures seules)

- **Densité de texture** : je pensais qu'elle restait de 1 pixel par 1/16 de bloc. En réalité la texture est en
  HD (×8 sur Barbe Blanche) : c'est ce qui donne les visages détaillés et le rendu peint.
- **Le manteau n'est pas rigide** : il est découpé en 3 os, donc prévu pour être animé.
- **Les aplats ont une cause précise** : de petites zones de texture étirées sur de grandes plaques, pas un manque
  de géométrie.
- **Les abdos et les pectoraux sont de vrais volumes**, pas seulement de la peinture.
- **Les « plaques » ne sont pas d'épaisseur nulle** : 0,13 à 0,64 px plus le gonflement. Elles restent fines au
  point de disparaître de profil.
- **Ce sont des entités GeckoLib**, avec des tenues portées comme des armures et des armes en modèle d'objet Java.

---

## 3. Lignes directrices pour atteindre ce niveau

Elles valent pour tous nos personnages (joueur incarné, PNJ, mobs-personnages). Chaque point est vérifiable.

### 3.1 Technique

1. **Un modèle d'entité sur mesure par personnage**, fait dans Blockbench (modèle « GeckoLib animated »),
   livré en trois fichiers : `<id>.geo.json`, `<id>.animation.json`, `<id>.png`.
2. **Moteur proposé : GeckoLib** (version NeoForge 1.21.1). C'est le standard pour ce niveau de détail et il gère
   nativement les angles libres, les chaînes d'os, les animations et les armures. *Décision à prendre par Evan :
   c'est une nouvelle dépendance, et nos animations de combat actuelles (sur le modèle humanoïde) devront être
   refaites en animations GeckoLib ou pilotées os par os.*
3. **Armes** : modèle GeckoLib quand elles s'animent (chaîne de Kurapika, yo-yo de Killua), sinon modèle d'objet
   Java (rotations de 22,5° seulement), accroché à l'os `itemMainHand`.
4. **Vêtements interchangeables** (si on en veut) : armures GeckoLib accrochées aux os `armor*`. Sinon, ils font
   partie du modèle.

### 3.2 Échelle

5. **Hauteur fidèle à l'œuvre**, avec la règle de base **1 bloc ≈ 1 m** (Steve fait 1,8 bloc). Exemples :
   Gon et Killua environ 1,5 bloc, Kurapika 1,7, Leorio et Hisoka environ 1,9, Uvogin et Buhara 2,5 ou plus.
   Plafond pratique : environ 5 blocs. *À valider par Evan, personnage par personnage.*
6. **Hitbox adaptée** à la taille (largeur et hauteur de l'entité), et position du nom réglée au-dessus de la tête.

### 3.3 Proportions

7. **La tête reste à 8×8×8 px** (ou 7 à 9 selon le personnage) : c'est le corps qui change de taille.
   Rapport tête / hauteur visé : 1/5 à 1/6 pour les enfants (Gon, Killua), 1/7 à 1/8 pour les adultes, jusqu'à
   1/9 pour les colosses.
8. **Épaules plus larges que les hanches** pour les combattants ; deltoïdes en volume séparé, plus larges que le
   haut du bras.
9. **Bras longs** : la main arrive à mi-cuisse. Avant-bras légèrement plus fin que le haut du bras, **poignet ou
   manchette marqué**, main en cube séparé.
10. **Une pose de repos** propre au personnage (penché, bras écartés, jambes ouvertes), intégrée au modèle.

### 3.4 Squelette standard

11. Noms d'os communs à tous les personnages, pour pouvoir partager les animations :
    `body` › `torso`, `head` (+ `face_*` pour les expressions), `right_arm` › `right_forearm` › `right_hand` ›
    `item_main`, `left_arm` › `left_forearm` › `left_hand` › `item_off`, `right_leg` › `right_shin`,
    `left_leg` › `left_shin`.
12. **Coudes et genoux obligatoires** (deux os par membre).
13. **Chaque élément qui bouge a sa chaîne d'os** : cape et manteau (3 segments minimum), cheveux longs, nattes,
    queue (Pitou), ailes (Pouf), oreilles. Pivot de chaque segment en haut de celui-ci.
14. **Au moins 2 expressions** (`face_neutral`, `face_combat`) ; plus si le personnage en a une célèbre (Gon en
    colère, sourire d'Hisoka).

### 3.5 Volume ou peinture ?

15. **Tout relief de 1 px ou plus est un cube** : pectoraux, abdos, épaules, nez proéminent, mèches, plis épais,
    poches, ceintures, boucles. En dessous de 1 px, c'est de la peinture (cicatrices, veines, coutures).
16. **Les plaques ont toujours une épaisseur** d'au moins 0,5 px (gonflement compris), jamais une face unique : il
    faut qu'elles restent visibles de profil.
17. **Couches de vêtements** : la peau, puis les vêtements avec un gonflement croissant (0,25 → 0,5 → 0,7) ; les
    plis là où le tissu s'accumule (taille, genoux, chevilles, coudes).
18. **Angles libres** pour les formes courbes (moustaches, cheveux, cornes) : une courbe = une suite de segments
    tournés progressivement.

### 3.6 Texture

19. **Densité cible uniforme : 4 pixels de texture par pixel de modèle** sur tout le personnage (texture de 512
    ou 1024 de côté selon la taille). Le visage peut monter à 8. **Jamais une petite zone étirée sur une grande
    surface** : c'est ce qui crée les aplats de la référence.
20. **Ombres et lumières peintes** : dégradé du haut vers le bas, creux plus sombres (aisselles, sous les
    pectoraux, plis), reflets sur les volumes saillants.
21. **Pas de grand aplat sans variation** : au moins un dégradé et quelques nuances sur tout pan de plus de 8×8 px
    de modèle. Le blanc n'est jamais du blanc pur (gris bleuté dans l'ombre), le noir n'est jamais du noir pur
    (bleu-noir ou brun-noir avec reflets).
22. **Trait manga** : contour sombre de 1 texel autour des yeux, des sourcils, des mèches et des bords de
    vêtements (cohérent avec l'interface du mod, police Bangers et traits noirs).
23. **Faces jamais visibles** (tranches de plaques collées, dessous recouverts) : UV de taille nulle, pour ne pas
    gaspiller de texture.

### 3.7 Silhouette et fidélité

24. **3 traits iconiques par personnage**, exagérés et lisibles de loin. Exemples : Gon (cheveux en pointes,
    veste verte, canne à pêche), Killua (touffe blanche, yo-yo, posture voûtée), Kurapika (tabard bleu, chaînes,
    yeux écarlates), Hisoka (cheveux plaqués, étoile et larme, cartes), Netero (barbe, chignon, Kannon derrière
    lui), Meruem (queue, casque crânien, tête verte).
25. **Test de silhouette** : rendu en noir uni, de face et de profil ; on doit reconnaître le personnage.
26. **Planche de référence avant de modéliser** : face, profil et dos (images de l'anime 2011), avec les couleurs
    relevées.

### 3.8 Animations (au minimum)

27. Attente (respiration), marche, course, coup de base, chaque technique du personnage, dégât reçu, mort.
28. **Mouvement secondaire** : la cape, les cheveux et les franges suivent avec un léger retard.
29. Les expressions changent avec l'état (repos ↔ combat, Ren, technique).

### 3.9 Budget et performance

30. **300 cubes maximum** par personnage (Barbe Blanche en a 192 à 247), texture **1024² maximum**, zone de rendu
    (`visible_bounds`) ajustée à la taille réelle pour éviter que le personnage disparaisse en bord d'écran.
31. Les petits détails (boutons, franges) sont regroupés sous un même os pour ne pas multiplier les os.

### 3.10 Contrôle avant d'intégrer un personnage

32. Captures CI de face, de trois-quarts, de profil et de dos, à côté d'un joueur pour juger l'échelle.
33. Vérifier : la silhouette (point 25), la densité de texture (point 19, à mesurer par script sur le
    `.geo.json`), aucune plaque invisible de profil, des animations sans traversée (cape dans les jambes, bras dans
    le torse).
34. Validation d'Evan sur la planche de référence, puis sur les captures.

---

## 4. Ce que ça implique pour le mod

- Le système actuel (`assets/hxh/looks` : skin de joueur 64×64, mise à l'échelle et quelques cubes en plus) ne
  peut pas atteindre ce niveau : il garde 6 os, des membres d'un seul tenant et une texture basse résolution. Il
  reste utile comme **solution provisoire** tant qu'un personnage n'a pas son vrai modèle.
- Pour passer au niveau de la référence :
  1. ajouter GeckoLib (décision d'Evan) ;
  2. un rendu GeckoLib commun pour les PNJ et les mobs-personnages, avec le squelette standard (point 11) ;
  3. un modèle pilote complet (proposition : **Netero ou Uvogin**, faciles à juger sur la musculature, l'échelle
     et les accessoires), validé avec Evan avant de passer aux autres ;
  4. réécrire les animations de combat en animations GeckoLib.
- **Répartition possible du travail** : Claude peut générer les `.geo.json` (géométrie, os, UV) et des textures de
  départ par script, et vérifier les mesures. Pour le niveau de peinture de la référence, une **retouche à la main
  dans Blockbench** (par Evan ou un graphiste) reste nécessaire.
