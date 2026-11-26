# Modèles à revoir ou à refaire

Inventaire de tout ce qui a un rendu dans le mod au 20 novembre 2026 (code et ressources), avec ce qu'il faut
faire pour atteindre le niveau décrit dans [Analyse_model.md](Analyse_model.md).

**Priorité** : **P1** = visible tout le temps ou emblématique (à faire en premier), **P2** = visible souvent,
**P3** = secondaire ou outil de construction.
**Format cible** : *Geo* = modèle d'entité GeckoLib (`.geo.json` + animations + texture HD) ; *Objet* = modèle
d'objet Java Blockbench ; *Bloc* = modèle de bloc Java ; *Fx* = effet dessiné par le code (Mesh3D) avec de vraies
textures ; *Geo-Fx* = créature ou objet invoqué par une technique, en vrai modèle GeckoLib animé.

---

## 1. Entités

### 1.1 Personnages nommés (PNJ, et plus tard mobs-personnages et joueur incarné)

Aujourd'hui, tous utilisent le **modèle du joueur** avec un skin 64×64 et quelques cubes en plus
(`assets/hxh/looks`). C'est le rendu provisoire. Il faut un modèle *Geo* propre à chacun.

| Personnage | Actuel | À faire | Priorité |
|---|---|---|---|
| Gon | Joueur + pointes de cheveux | Geo : enfant (≈ 1,5 bloc), cheveux en pointes, veste verte, canne à pêche | P1 |
| Killua | Joueur + touffes | Geo : enfant, touffe blanche, posture voûtée, yo-yo, mains-griffes | P1 |
| Kurapika | Joueur + tabard | Geo : tabard bleu et or, chaînes à la main droite, yeux écarlates (expression) | P1 |
| Leorio | Joueur + cheveux | Geo : grand (≈ 1,9), costume, lunettes rondes, mallette | P1 |
| Hisoka | Joueur + pointes | Geo : grand, cheveux plaqués, maquillage, cartes, deux expressions | P1 |
| Netero | Joueur + barbe, chignon | Geo : petit vieillard, barbe, chignon, lobes, chemise « 心 », Kannon (voir 3.3) | P1 |
| Satotz | Joueur + coiffure, moustache | Geo : grand et maigre, longs membres, moustache, pas de bouche | P3 |
| Menchi | Joueur + couettes | Geo : cinq couettes, résille, short | P3 |
| Buhara | Joueur ×1,5 + ventre | Geo : colosse (≈ 2,5 blocs), ventre énorme, gros appétit (animation) | P3 |
| Beans | Joueur ×0,7 + dôme | Geo : petit, tête en haricot, costume | P3 |
| Les 36 autres personnages jouables (Pitou, Illumi, Silva, Zeno, Chrollo, Uvogin, Meruem…) | Aucun modèle : le joueur garde son skin, un mob garde son apparence | Geo pour chacun, ordre de priorité en 5.0 — **détail de chaque personnage et de chaque technique en partie 5** | P1 à P3 |
| Personnages avec un Hatsu absents du jeu (Kortopi, Bonolenov, Shoot, Welfin…) | — | *IDÉE* : voir 5.3 | — |
| Combattants nommés de la Tour : Gido, Sadaso, Riehlvelt, Kastro | Combattant générique (4 skins de lutteur) | Geo : Gido (jambe de bois, toupies), Sadaso (bras fantôme), Riehlvelt (fauteuil roulant, fouet), Kastro (tenue de combat, double) | Kastro P2, les autres P3 |

### 1.2 PNJ génériques et figurants

| Entité | Actuel | À faire | Priorité |
|---|---|---|---|
| Cuisinier, gérant d'hôtel, réceptionniste, arbitre, navigateur, hôtesse | Joueur + skin 64×64 sans modèle en plus | Un modèle Geo humain de base + variantes de tenue (uniforme d'arbitre, tablier, costume) | P2 |
| Femme et maris Kiriko déguisés (`kiriko_wife`, `husband_1`, `husband_2`) | Joueur + skin | Geo humain de base + tenues de villageois | P3 |
| Combattants de la Tour (`arena_fighter`, 4 skins) | Joueur + skins de lutteur | Geo humain de base + ~6 tenues de combattant variées (karaté, boxe, lutte, armes) | P2 |
| Candidats de l'Examen (`exam_candidate`) | Joueur + **skins vanilla de Minecraft** | Geo humain de base + candidats reconnaissables de l'arc (Tonpa, Todo, Pokkle, Ponzu, les frères Amori…) et figurants variés | P1 |

### 1.3 Bêtes et monstres

Tous **réutilisent un modèle de Minecraft** avec sa texture d'origine : c'est ce qui casse le plus l'immersion.

| Entité | Actuel | À faire | Priorité |
|---|---|---|---|
| Kiriko | **Perroquet vert** | Geo : grande bête humanoïde ailée (≈ 2,5 blocs), bec, ailes, capable de se déguiser (lien avec les PNJ « mari » et « femme ») ; œuf (voir 2.1) | P1 |
| Fourmi-Chimère | **Araignée venimeuse** | Geo : soldat fourmi bipède (carapace, mandibules, plusieurs variantes animales) | P1 |
| Singe à face humaine (marais de Numele) | **Zombie momifié** | Geo : singe maigre au visage humain imitant Satotz | P2 |
| Aigle-araignée | **Perroquet gris** | Geo : grand rapace aux pattes d'araignée, nid et œuf | P2 |
| Renard-ours | **Ours polaire** | Geo : ours à tête de renard, roux, griffes | P2 |
| Great Stamp | **Hoglin** | Geo : sanglier géant au nez énorme (≈ 3 blocs) | P2 |
| Zombies de la « Horde de Nen » (Hatsu des zombies) | Zombie vanilla | Geo-Fx : zombie d'aura translucide, couleur de l'aura du lanceur | P3 |

### 1.4 Projectiles

| Entité | Actuel | À faire | Priorité |
|---|---|---|---|
| Projectile de Nen — orbe | Boule 3D dessinée par le code | Fx : noyau + enveloppe texturés, traînée | P2 |
| Projectile de Nen — carte (Hisoka) | Un carré texturé | Objet : vraie carte à jouer avec épaisseur, dos et face peints, rotation en vol | P1 |

---

## 2. Objets et blocs

### 2.1 Objets (aujourd'hui des icônes 2D)

| Objet | À faire | Format | Priorité |
|---|---|---|---|
| Licence Hunter (`hunter_card`) | Carte emblématique : en main en 3D, icône redessinée | Objet + icône | P1 |
| Balle de Netero (`netero_ball`) | Balle en 3D (épreuve de Netero) | Objet | P2 |
| Plaque et badge d'Examen (`exam_plate`, `exam_badge`) | Plaque numérotée en 3D, numéro lisible | Objet | P2 |
| Œuf d'aigle-araignée (`spider_eagle_egg`) | Œuf en 3D | Objet | P2 |
| Divination par l'eau (`water_divination`, aussi un bloc) | Verre, eau et feuille en 3D, avec les 6 réactions déjà animées | Bloc + objet | P1 |
| Pièce engloutie (`sunken_coin`), plaque cible (`target_plate`) | Redessiner en 3D | Objet | P3 |
| Recette gourmet (`gourmet_recipe`) | Parchemin redessiné | Icône | P3 |
| Plats nutritifs I à IV, viande de renard-ours crue et cuite | Icônes redessinées dans le style du mod | Icône | P3 |
| Haltères I à IV (`dumbbell_*`) | Haltères en 3D, taille et matériau par niveau | Objet | P2 |
| Œufs d'apparition (6) | Couleurs accordées aux nouvelles créatures | Icône | P3 |

### 2.2 Blocs

| Bloc | Actuel | À faire | Priorité |
|---|---|---|---|
| Tapis de course I à IV (vitesse et endurance) | Bloc texturé | Bloc 3D : tapis, montants, console, matériaux selon le niveau | P2 |
| Pierres de méditation 1 à 5 | Bloc | Bloc 3D : coussin, pierre gravée, encens selon le niveau | P2 |
| Bain glacé | Bloc | Bloc 3D : baquet en bois, glaçons, eau | P3 |
| Mannequin de frappe | Bloc (corps, visage, bois) | Bloc 3D ou Geo animé (il encaisse les coups) | P2 |
| Ordinateur Hunter | Bloc (boîtier, écran, clavier) | Bloc 3D : écran, clavier, logo Hunter | P2 |
| Porte de la Tour Céleste, ascenseur, sortie | Blocs | Blocs 3D cohérents avec l'architecture de la Tour | P3 |
| Mur d'illusion (Tour de Trick) | Bloc | Garder l'aspect pierre ; effet de dissolution au passage | P3 |
| Marqueurs (arène, PNJ, proie, étape) | Blocs d'outil | Rien : outils de construction invisibles en jeu normal | — |

### 2.3 Armes et accessoires à créer (n'existent aujourd'hui que comme effets)

Ces objets apparaissent dans les techniques sous forme d'effets plats ou n'existent pas. Il leur faut un vrai
modèle tenu en main (os `item_main`), utilisable aussi par les mobs-personnages.

| Objet | Personnage | Priorité |
|---|---|---|
| Canne à pêche | Gon | P1 |
| Yo-yos | Killua | P1 |
| Chaînes (cinq, une par doigt, avec leurs embouts) | Kurapika | P1 |
| Jeu de cartes | Hisoka | P1 |
| Aiguilles | Illumi | P2 |
| Les armes de Kite (bâton, faux, fusil…) | Kite | P2 |
| Katana | Nobunaga | P2 |
| Parapluie-épée | Feitan | P2 |
| Éventail | Kalluto | P2 |
| Livre (Skill Hunter) | Chrollo | P2 |
| Aspirateur Blinky | Shizuku (voir aussi 3.3) | P2 |
| Pipe | Morel | P3 |
| Seringues et mallette de médecin | Leorio, Cheadle | P3 |

---

## 3. Effets et particules

Les effets sont aujourd'hui dessinés par le code (`NenFxClient`, `Sprites3D`, `AuraClient`, avec la boîte à
outils `Mesh3D`) et quelques textures (`textures/fx`). Règle proposée : **l'énergie reste dessinée par le code**
(elle doit grandir, onduler et suivre la couleur de l'aura), mais avec de vraies textures et des formes plus
fines. **Tout ce qui est une créature ou un objet** devient un modèle *Geo-Fx* animé.

### 3.1 Aura et techniques de base du Nen

| Effet | Actuel | À faire | Priorité |
|---|---|---|---|
| Aura au repos (vapeur) et aura agressive (flammes) | Langues de flammes 3D et vapeur dessinées par le code | Fx : flammes plus organiques (textures de flamme dessinées, ondulation), vapeur plus douce, épaisseur selon le Ren et le Ken | P1 |
| Ryu (poings d'aura, répartition visible) | Poings dessinés par le code | Fx : aura concentrée qui suit les os (bras, jambes) du nouveau modèle | P2 |
| Gyo (yeux) | Yeux collés au visage + vignette | Fx : yeux d'aura qui suivent l'os de la tête | P2 |
| En (bulle et formes) | Dôme ou boîte en lignes | Fx : membrane d'aura texturée qui ondule, réglage des formes conservé | P2 |
| Ko | Aura dans le poing | Fx : poing d'aura incandescent qui suit l'os de la main | P2 |
| Zetsu / In | Disparition de l'aura | Fx : fondu de l'aura plutôt que coupure nette | P3 |
| Soin par le Nen | Particules | Fx : aura qui se resserre sur la blessure | P3 |

### 3.2 Effets génériques des techniques

Ils couvrent la plupart des 95 techniques ; les refaire améliore beaucoup de personnages d'un coup.

| Effet | Techniques concernées (exemples) | À faire | Priorité |
|---|---|---|---|
| Impact (`IMPACT`) | Botobai, Leorio, Meruem, Razor, Silva, Tsezguerra, Youpi, Zushi… (11) | Onde de choc texturée, éclats, trait de vitesse manga | P1 |
| Coupure (`SLASH`) | Kalluto, Meruem (queue), Nobunaga ×2, Pitou, Youpi, Kastro (7) | Arc de lame net, traînée qui s'efface | P1 |
| Onde de choc (`SHOCKWAVE`) | Feitan, Leorio, Meruem ×2, Morel, Youpi, Zeno, Gido… (9) | Anneau au sol + déformation de l'air | P1 |
| Orbe (`ORB`) | Franklin, Kite, Pakunoda, Razor, Silva, Riehlvelt (6) | Noyau, enveloppe qui ondule, traînée | P1 |
| Poing à distance (`WARP_FIST`) | Leorio, Ging, Hisoka, Sadaso (4) | Poing d'aura en 3D qui suit le bras du lanceur | P1 |
| Éclair (`BOLT`, `CRACKLE`) | Killua (yo-yo, poing électrique, Godspeed), Riehlvelt (4) | Tubes lumineux ramifiés, plus fins, scintillement | P1 |
| Chaîne (`CHAIN`, `HOLY_CHAIN`) | Kurapika ×2, Machi ×2 (4) | Vraie chaîne en maillons 3D (fil d'aura pour Machi) | P1 |
| Jajanken (boule chargée, ciseaux, papier) | Gon (4) | Boule qui grossit, lame, cône : déjà en 3D, à affiner avec de vraies textures | P1 |
| Aiguilles (`NEEDLE`, `NEEDLE_CROWN`) | Illumi ×3 | Aiguilles en 3D (objet) + couronne d'aiguilles | P2 |
| Marionnette (`PUPPET`), fils | Pitou (Terpsichora), Shalnark | Pitou **fait (26/11)** : `summon_terpsichora` derrière elle, fils depuis ses doigts ; Shalnark à faire | P2 |
| Embrasement (`FLARE`) | 15 techniques de renforcement sur soi (Nobunaga, Feitan, Youpi, Meruem…) | Explosion d'aura autour du corps, plus marquée que l'aura normale | P2 |
| Séisme (`QUAKE`), débris | Uvogin, Netero, mobs | Fissures au sol, blocs projetés plus réalistes | P2 |
| Marque (`MARK`) | Pariston, Neon, Freecss (instinct) | Symbole qui flotte sur la cible | P3 |
| Mort en giclée d'encre (`DISSOLVE`) | Toutes les morts de mobs | Taches d'encre manga plus lisibles | P3 |
| Cadre d'impact (`impactFrame`) | Main zéro de Netero | Cadre manga noir et blanc plein écran | P3 |
| Chiffres de dégâts | Tous les coups | Police Bangers, contour noir (déjà proche du style voulu) | P3 |
| Projection de cartes (`CARD_BURST`) | Frappe chargée parfaite | Cartes en 3D (objet carte) | P3 |

### 3.3 Créatures et objets invoqués (aujourd'hui des « sprites » faits de cubes par le code)

| Invocation | Technique | À faire | Priorité |
|---|---|---|---|
| Statue de Kannon et mains de Kannon | Netero (Bodhisattva, Main zéro) | **Fait (26/11)** : `summon_kannon` (9 à 12 blocs, 22 bras en éventail, gestes `strike` et `zero`) ; reste les mains dorées de la quête de Netero | P1 |
| Dragons de Zeno (tête de dragon, descente du dragon) | Zeno | **Fait (26/11)** : `summon_dragon`, dragon serpentin qui vole jusqu'à la cible ou pique du ciel | P1 |
| Poupée Doctor Blythe | Neferpitou | **Fait (26/11)** : `summon_doctor_blythe` au-dessus du patient, quatre bras qui cousent ; l'effet garde le cordon et les fils | P1 |
| Démons de Razor (14) | Razor | Geo-Fx : démons humanoïdes numérotés | P2 |
| Soldats de fumée | Morel (Deep Purple) | Geo-Fx : soldats de fumée, texture translucide | P2 |
| Cookie la masseuse | Biscuit | Geo-Fx : petite masseuse | P2 |
| Blinky | Shizuku | Geo-Fx (ou objet) : aspirateur avec son visage | P2 |
| Soleil levant | Feitan | Fx : soleil de feu texturé, chaleur | P2 |
| Clown et armes de la roulette | Kite (Crazy Slots) | Geo-Fx : clown parlant + les armes en objets (2.3) | P2 |
| Pot Clean (compteur) | Knuckle | **Fait (26/11)** : `summon_pot_clean` au-dessus de la cible, dette affichée sur sa pancarte, geste `collect` à chaque coup | P2 |
| Ailes | Shaiapouf, Youpi | Geo : ailes intégrées au modèle du personnage, animées | P2 |
| Bombe | Genthru | Objet/Fx : petite bombe accrochée à la cible | P3 |
| Livre | Chrollo | Objet (2.3) | P3 |
| Papier, porte marquée | Kalluto, Knov | Fx : papiers qui tourbillonnent, porte d'aura | P3 |
| Cœur, notes de musique, goutte de peur | Pariston, Senritsu, peur | Fx : sprites redessinés dans le style manga | P3 |

---

## 4. Résumé

| Catégorie | Nombre | Dont P1 |
|---|---|---|
| Personnages nommés (PNJ + jouables + combattants nommés) | 50 | ~15 |
| PNJ génériques et figurants | 5 familles | 1 (candidats de l'Examen) |
| Bêtes et monstres | 7 | 2 (Kiriko, Fourmi-Chimère) |
| Projectiles | 2 | 1 |
| Objets | ~30 (dont 13 armes à créer) | ~6 |
| Blocs | ~15 | 1 (divination) |
| Effets et invocations | ~45 | ~13 |
| Personnages avec un Hatsu absents du jeu (*IDÉE*, partie 5.3) | 10 | — |
| Techniques absentes de personnages déjà jouables (*IDÉE*, partie 5.1) | ~20 | — |

Ordre conseillé :
1. Les fondations : GeckoLib, le squelette standard, un modèle humain de base, le personnage pilote.
2. Les effets génériques (3.2), qui améliorent toutes les techniques d'un coup.
3. Le casting principal et les bêtes qui sont encore des animaux de Minecraft (Kiriko, Fourmi-Chimère).
4. Le reste par ordre de priorité.

---

## 5. Fiches détaillées par personnage

Pour chaque personnage : son **modèle**, ses **accessoires**, puis **chaque technique** avec ce qu'elle contient à
l'écran (dans l'œuvre et dans le jeu), les **modèles à faire** et les **liens** (ce qui est partagé avec d'autres
éléments ou ce qui relie le lanceur à l'effet).

Repères :
- **En jeu** : la technique existe déjà (le rendu est à refaire) ;
- **Absente** : technique connue de l'œuvre qui n'est pas dans le jeu — *IDÉE*, à valider par Evan avant de la
  coder ;
- **Inventée** : technique créée pour le mod (l'œuvre ne montre pas de Hatsu pour ce personnage) ;
- formats : *Geo*, *Objet*, *Fx*, *Geo-Fx* (voir le haut du document).

### 5.0 Ordre de priorité des personnages

Critères (décidés avec Evan) : **importance dans le manga** et **quantité de choses connues sur le personnage et
son Hatsu**. Un personnage secondaire mais très détaillé (les Gardes royaux) passe avant un personnage important
dont on ne sait presque rien. Les personnages mineurs ou au Hatsu inconnu passent en dernier.

| Priorité | Personnages | Pourquoi |
|---|---|---|
| **P1** — en premier | Gon, Killua, Kurapika, Leorio, Hisoka · Netero · Meruem, Neferpitou, Shaiapouf, Youpi · Chrollo, Uvogin · Illumi, Zeno · Biscuit | Personnages principaux, ou Hatsu montrés en détail dans de grands combats (Gardes royaux, Netero contre Meruem, Uvogin contre Kurapika, Zeno et Silva contre Chrollo, Biscuit à Greed Island) |
| **P2** — ensuite | Silva, Ging · Feitan, Nobunaga, Phinks, Franklin, Machi, Shizuku, Pakunoda, Shalnark (Brigade) · Kite, Knuckle, Morel, Knov · Razor, Genthru · Kastro · Alluka/Nanika (absente) | Rôle important dans un arc, Hatsu connu mais moins central |
| **P3** — plus tard | Palm, Kalluto, Neon, Senritsu, Wing, Zushi · Pariston, Cheadle, Botobai, Menchi, Bushidora, Tsezguerra (Hatsu jamais montré) · Satotz, Buhara, Beans (pas de Hatsu) · Gido, Sadaso, Riehlvelt · tous les absents de 5.3 sauf Alluka | Rôle secondaire, ou Hatsu inconnu ou à peine montré |

Dans chaque fiche, les **techniques absentes** suivent la priorité de leur personnage, avec deux exceptions
remontées en P1 parce qu'elles sont emblématiques : **Gon adulte** et la **Chaîne du jugement** de Kurapika.

### 5.1 Personnages jouables du jeu (43)

#### Gon Freecss — Renforcement · **P1**
- **Modèle** : enfant (~1,5 bloc), cheveux noirs en grandes pointes aux bouts verts, veste verte à col blanc,
  short vert, bottes. Deux expressions (sourire, colère). **Variante « Gon adulte »** (voir Absente).
- **Accessoires** : canne à pêche (*Objet*, P1), sac à dos.
- **Jajanken : Pierre** (en jeu) — poing serré, l'aura se concentre en boule qui grossit et change de couleur ;
  Gon accroupi, en position de charge, vent autour. À faire : *Fx* boule (noyau, enveloppe, anneaux) attachée à
  l'**os de la main droite**, animation de charge et de frappe. Lien : même boule que « Pierre sans retenue ».
- **Jajanken : Ciseaux** (en jeu) — index et majeur tendus : l'aura devient une lame. *Fx* lame d'aura qui part
  des doigts. Lien : os de la main.
- **Jajanken : Papier** (en jeu) — main ouverte, l'aura est projetée en boule ou en cône. *Fx* cône d'émission.
- **Pierre sans retenue** (en jeu) — la boule enfle puis se recondense, éblouissante. *Fx* + flash d'écran.
- **Gon adulte** (absente, *IDÉE*) — transformation par le serment : grand corps musclé (~2,5 blocs), cheveux
  jusqu'aux pieds, aura noire immense. À faire : *Geo* variante complète + animation de transformation. Lien :
  serment « Tout sacrifier ».

#### Killua Zoldyck — Transformation · **P1**
- **Modèle** : enfant (~1,5 bloc), touffe de cheveux blancs ébouriffés, yeux bleus, haut blanc sur manches longues
  bleu nuit, short, chaussures violettes, posture voûtée, mains en poches au repos. Expression « assassin »
  (yeux vides).
- **Accessoires** : deux yo-yos en acier (*Objet*, P1), skateboard (P3).
- **Foudre / Thunderbolt** (en jeu) — décharge en éclairs bleutés depuis la main. *Fx* éclairs ramifiés (tubes
  lumineux) de la main à la cible ; la cible reste parcourue d'arcs.
- **Godspeed** (en jeu) — tout le corps crépite, cheveux dressés, aura électrique. *Fx* arcs autour du corps
  attachés aux os + traînées. Absentes : ses deux modes (*Whirlwind* : réflexe automatique ; *Speed of
  Lightning* : déplacement éclair), à distinguer par l'animation.
- **Coup de yoyo** (en jeu) — le yo-yo (50 kg chacun) part au bout de son fil. *Objet* yo-yo + fil (*Fx* tube
  fin) relié à l'os de la main.
- **Poing électrique** (en jeu) — poing chargé d'électricité. *Fx* sur l'os de la main.
- **Narukami** (absente, *IDÉE*) — éclair lancé en ligne droite depuis la paume. *Fx*.
- **Mains-griffes Zoldyck** — coup secret de famille (voir Famille Zoldyck, *Fx* griffures).

#### Kurapika — Matérialisation (Spécialisation aux yeux écarlates) · **P1**
- **Modèle** : ~1,7 bloc, carré blond, tabard bleu brodé d'or sur vêtements blancs, puis costume noir (arc de
  York Shin) — deux tenues possibles. **Yeux écarlates** (expression qui s'allume).
- **Accessoires** : les **cinq chaînes** de la main droite, une par doigt (*Objet* + *Fx*), boucle d'oreille.
- **Prison de chaînes / Chain Jail** (en jeu) — la chaîne du petit doigt s'enroule autour de la cible et la
  serre. *Fx* chaîne en maillons 3D (texture `chain_link`), **départ du petit doigt**, enroulement autour de la
  cible. Lien : objet « chaînes ».
- **Chaîne sacrée / Holy Chain** (en jeu) — chaîne du pouce, embout en croix, qui soigne. *Fx* chaîne + croix
  lumineuse sur la blessure.
- **Chaîne radiesthésique / Dowsing Chain** (passif en jeu) — chaîne de l'annulaire avec un pendule. *Objet* pendule.
- **Chaîne du jugement / Judgment Chain** (absente, *IDÉE*) — chaîne du majeur, crochet planté dans le cœur de la
  cible, qui la tue si elle rompt la règle imposée. *Fx* chaîne + crochet en forme de lame dans la poitrine.
- **Chaîne voleuse / Steal Chain** (absente, *IDÉE*) — chaîne de l'index, aux yeux écarlates : vole un Hatsu.
- **Temps de l'Empereur / Emperor Time** (absente, *IDÉE*) — yeux écarlates, toutes les catégories à 100 %
  (déjà en partie via le coup secret « Colère écarlate »).

#### Neferpitou — Spécialisation · **P1**
- **Modèle** : Garde royal, silhouette fine et souple (~1,8 bloc), oreilles et **queue de chat**, cheveux blancs
  bouclés, yeux rouges fendus, griffes, genoux d'animal (jambes digitigrades). Chaînes d'os pour la queue (≥ 4) et
  les oreilles.
- **Terpsichora** (en jeu) — une **marionnette d'aura** apparaît derrière Pitou, accrochée à elle par des **fils
  d'aura plantés dans le dos et les articulations** ; elle manipule son corps (posture voûtée de prédateur,
  muscles gonflés, bonds énormes). À faire : *Geo-Fx* marionnette (corps de pantin articulé, visage de poupée
  inexpressif, translucide violet) + *Fx* fils tendus. **Liens** : chaque fil relie une articulation de la
  marionnette à l'os correspondant de Pitou (épaules, coudes, poignets, hanches, genoux, chevilles, nuque) ; les
  fils suivent les animations des deux modèles.
- **Doctor Blythe** (en jeu) — grande **poupée-infirmière** à tête de clown recousue, bras multiples terminés par
  des instruments (aiguilles, pinces), reliée à la tête de Pitou par un **cordon** ; opère le blessé allongé.
  À faire : *Geo-Fx* poupée (~3 blocs) avec animation de couture + *Fx* cordon. **Lien** : cordon entre la nuque
  de Pitou et la poupée ; Pitou ne peut pas bouger pendant l'opération.
- **Griffure** (en jeu, inventée en partie) — trois coups de griffes rapides. *Fx* griffures rouges (texture
  `claw`).
- **Puppet Master** (absente, *IDÉE*) — un **pantin-clown** manipule les cadavres (Kite) ; fils et aiguilles
  plantés dans le corps contrôlé. *Geo-Fx* pantin + *Fx* fils vers le corps contrôlé.

#### Illumi Zoldyck — Manipulation · **P1**
- **Modèle** : ~1,85 bloc, longs cheveux noirs raides jusqu'aux hanches (chaîne d'os), visage inexpressif, yeux
  noirs vides, tenue vert sombre à larges manches, ceinture à motifs. **Variante « Gittarackur »** : visage
  hérissé d'aiguilles, peau grise, cheveux en pointes (déguisement de l'Examen).
- **Accessoires** : aiguilles à tête ronde (*Objet*, P2).
- **Aiguille** (en jeu) — aiguille lancée et plantée. *Objet* aiguille en vol.
- **Aiguille de métamorphose** (en jeu) — aiguilles plantées dans son visage : le corps se déforme (Gittarackur).
  À faire : bascule vers la variante *Geo* + couronne d'aiguilles (*Fx*).
- **Pluie d'aiguilles** (en jeu) — salve d'aiguilles. *Objet* répété.
- **Pantins à aiguilles** (absente, *IDÉE*) — des humains contrôlés par une aiguille dans la tête (zombies
  d'Illumi). *Fx* aiguille sur la tête des mobs contrôlés.

#### Leorio Paladiknight — Émission · **P1**
- **Modèle** : grand (~1,9 bloc), costume bleu nuit, cravate, lunettes rondes, cheveux noirs courts. Variante
  « blouse de médecin » (P3).
- **Accessoires** : mallette (*Objet*, P3), couteau de poche (P3).
- **Coup à distance** (en jeu) — il frappe le sol, son **poing d'aura ressort** sous ou à côté de la cible via un
  portail (comme contre Ging). *Fx* portail rond au sol + poing d'aura 3D. **Lien** : le poing reprend la forme et
  le mouvement de son bras.
- **Onde de choc** (en jeu, inventée en partie) — *Fx* anneau au sol.
- **Percussion interne** (en jeu, inventée) — *Fx* impact discret.

#### Isaac Netero — Renforcement · **P1**
- **Modèle** : petit vieillard (~1,6 bloc) mais musclé dans son vrai combat, crâne dégarni, chignon, longue barbe
  blanche, sourcils tombants, longs lobes, chemise « 心 », pantalon, sandales. Variante « dôgi » (combat contre
  Meruem, torse nu musclé). Pose de prière (mains jointes).
- **Prière** (passif en jeu) — mains jointes, vitesse surhumaine. Animation de prière.
- **Bodhisattva aux mille mains / Hyakushiki Kannon** (en jeu) — une **statue colossale de Kannon** dorée
  (dizaines de mètres), mille bras, mains en mudras, surgit derrière Netero ; une **paume géante** écrase la cible.
  À faire : *Geo-Fx* statue (~8 à 12 blocs) + mains détachables pour les frappes. **Liens** : la statue copie les
  gestes de Netero (sa prière déclenche le coup) ; les mains numérotées partent de la statue.
- **Main zéro / Zero Hand** (en jeu) — la statue enlace la cible, la bouche de Kannon libère un **rayon d'aura
  pure**. *Geo-Fx* étreinte + *Fx* rayon + cadre d'impact. Lien : même statue.
- **Mains numérotées** (absentes, *IDÉE*) — Main n° 3 (paume du dessus qui écrase), n° 9 (paumes latérales), n° 33
  (bouche de Kannon)… *Animations* de la même statue.

#### Hisoka Morow — Transformation · **P1**
- **Modèle** : grand et élancé (~1,9 bloc), cheveux rouges plaqués en pointes, étoile et larme maquillées, yeux
  dorés, tenue de bouffon (haut blanc à symboles de cartes, pantalon), musculature sèche. Expressions : sourire
  pervers, excitation.
- **Accessoires** : jeu de cartes (*Objet*, P1).
- **Bungee Gum** (en jeu) — un **fil d'aura rose, élastique et collant** relie son doigt à la cible ; il la tire
  vers lui. *Fx* tube élastique qui s'étire. **Lien** : du bout du doigt d'Hisoka à la cible, la tension se voit.
- **Cartes renforcées** (en jeu) — cartes lancées comme des lames. *Objet* carte en vol (projectile 1.4).
- **Texture surprise** (en jeu) — fausse texture (papier, carte) collée sur une surface ou son visage. *Fx*
  surface qui se décolle + gerbe de cartes.
- **Poing élastique** (en jeu, inventée) — poing au bout d'un fil de Bungee Gum. *Fx* poing + fil.

#### Pariston Hill — Spécialisation (Hatsu jamais montré dans l'œuvre) · **P3**
- **Modèle** : ~1,8 bloc, cheveux blonds bouclés, costume clair, sourire permanent.
- **Dette d'amour** (en jeu, inventée) — cœur rose au-dessus de la cible. *Fx* cœur (sprite redessiné).
- *Note* : dans l'œuvre, il crée des **Fourmis-Chimères** artificielles (mutants) — piste de technique *IDÉE*.

#### Ging Freecss — Émission · **P2**
- **Modèle** : ~1,7 bloc, barbe de trois jours, bandeau sur la tête, cape-turban, tenue d'explorateur.
- **Poing à distance** (en jeu) — un poing d'aura sort d'un portail près de la cible (frappe de Leorio qu'il a
  copiée). *Fx* portail + poing. Lien : même effet que Leorio.
- **Copie** (en jeu) — il reproduit la technique vue. Lien : réutilise l'effet de la technique copiée, teinté de
  son aura.

#### Cheadle Yorkshire — Manipulation (Hatsu non montré) · **P3**
- **Modèle** : ~1,7 bloc, lunettes, cheveux verts, tenue de secrétaire stricte, chien (Zodiaque du Chien).
- **Règlement** (en jeu, inventée) — *Fx* zone au sol aux bords tracés.

#### Botobai Gigante — Renforcement (Hatsu non montré) · **P3**
- **Modèle** : très grand (~2,3 blocs), crâne rasé, barbe, carrure énorme, uniforme de Zodiaque (Tigre).
- **Pression** (en jeu, inventée) — *Fx* onde écrasante.

#### Menchi — Transformation (Hatsu non montré) · **P3**
- **Modèle** : cheveux verts en cinq couettes, résille noire, haut de bikini rose, short en jean.
- **Accessoires** : couteaux de cuisine (*Objet*, P3).
- **Repas de gourmet** (en jeu, inventée) — un plat posé devant les alliés. *Objet* plat fumant + *Fx* vapeur.

#### Bushidora Ambitious — Renforcement (Hatsu non montré) · **P3**
- **Modèle** : ~1,8 bloc, épéiste, cheveux mi-longs, tenue de voyageur (Greed Island).
- **Accessoires** : épée (*Objet*, P2).
- **Lame d'aura** (en jeu, inventée) — *Fx* lame allongée sur l'objet épée.

#### Tsezguerra — Renforcement (Hatsu non montré) · **P3**
- **Modèle** : ~1,8 bloc, lunettes, tenue de mercenaire (Greed Island).
- **Accessoires** : livre de cartes de Greed Island (*Objet*, P3).
- **Frappe chargée**, **Défense instantanée** (en jeu, inventées) — *Fx* génériques.

#### Silva Zoldyck — Émission · **P2**
- **Modèle** : très grand (~2 blocs), longue crinière argentée, yeux bleus, carrure de fauve, tenue sombre.
- **Grande boule** (en jeu) — grosse sphère d'aura lancée à deux mains (contre Chrollo). *Fx* orbe géant.
- **Orbes explosifs** (en jeu) — sphères d'aura violettes en salve. *Fx* orbes.
- **Frappe du patriarche** (en jeu, inventée) — *Fx* impact.

#### Zeno Zoldyck — Émission (Transformation) · **P1**
- **Modèle** : vieillard petit et vif (~1,5 bloc), moustache, longue tresse, tenue traditionnelle avec
  l'inscription « 一日一殺 » (un meurtre par jour).
- **Tête de dragon** (en jeu) — un **dragon d'aura doré** jaillit de sa main, mord et emporte la cible. *Geo-Fx*
  dragon serpentin (tête + corps en chaîne d'os) qui sort de l'os de la main. **Lien** : la queue du dragon reste
  attachée à la main tant que la morsure dure.
- **Plongée du dragon / Dragon Dive** (en jeu) — le dragon monte au ciel et se scinde en une **pluie de petits
  dragons**. *Geo-Fx* mini-dragons. Lien : même modèle de dragon à petite échelle.
- **Aura Blast** (en jeu, inventée) — *Fx* explosion.
- **Lance du dragon / Dragon Lance** (absente, *IDÉE*) — dragon lancé comme une lance.

#### Kalluto Zoldyck — Manipulation · **P3**
- **Modèle** : enfant (~1,3 bloc), carré noir, **kimono**, visage androgyne, éventail à la main.
- **Accessoires** : éventail (*Objet*, P2).
- **Danse des papiers** (en jeu) — tourbillon de **feuilles de papier coupantes** (et papier qui anime les
  cadavres). *Fx* nuée de papiers 3D. Lien : l'éventail les dirige.
- **Éventail de papier** (en jeu) — *Fx* gerbe de papiers.

#### Nobunaga Hazama — Renforcement · **P2**
- **Modèle** : ~1,8 bloc, samouraï, chignon, barbe naissante, kimono ouvert, sandales.
- **Accessoires** : katana et fourreau à la ceinture (*Objet*, P2).
- **Iai** (en jeu) — posture accroupie, la main sur la poignée ; tout ce qui entre dans son **En** (4 m) est
  tranché. *Fx* bulle d'En discrète + éclair de coupe. Lien : animation de dégainage du katana.
- **Coupe instantanée**, **Posture du samouraï**, **Pas du sabreur** (en jeu, inventées en partie) — animations
  de sabre.

#### Uvogin — Renforcement · **P1**
- **Modèle** : colosse (~2,6 blocs), crinière sauvage, torse nu couvert de fourrures, bracelets, muscles énormes.
  Expression : rugissement.
- **Big Bang Impact** (en jeu) — poing énorme qui pulvérise le sol (cratère). *Fx* aura sur le poing + cratère +
  débris. Lien : os de la main droite.
- **Cri** (absente, *IDÉE*) — cri assourdissant (onde de son visible).

#### Franklin Bordeau — Émission · **P2**
- **Modèle** : ~2,2 blocs, visage couturé, oreilles étirées, crâne rasé, long manteau.
- **Double mitrailleuse** (en jeu) — il **coupe le bout de ses doigts** : dix canons qui tirent des balles d'aura.
  À faire : bouts de doigts visibles (modèle de main dédié) + *Fx* rafale. Lien : dix points de départ sur les
  doigts.
- **Tir concentré**, **Tir perforant** (en jeu, inventés) — *Fx* tir.

#### Machi Komacine — Transformation · **P2**
- **Modèle** : ~1,6 bloc, queue de cheval violette, tenue de ninja (haut ample, ceinture), regard froid.
- **Fils de Nen** (en jeu) — fils d'aura très fins, invisibles sans Gyo, qui ligotent. *Fx* fils fins (visibles
  au Gyo). Lien : partent de ses doigts.
- **Fil étrangleur** (en jeu) — *Fx* fil.
- **Suture** (passif) — recoud les membres. Animation de couture.

#### Shizuku Murasaki — Matérialisation · **P2**
- **Modèle** : ~1,6 bloc, carré noir, lunettes, pull col roulé, air distrait.
- **Blinky** (en jeu) — un **aspirateur au visage** (yeux, bouche) qui aspire tout. *Geo-Fx* ou *Objet* animé
  (bouche qui s'ouvre). Lien : tenu par sa main.

#### Pakunoda — Spécialisation · **P2**
- **Modèle** : grande (~1,85 bloc), cheveux blonds, costume noir, chemise.
- **Accessoires** : revolver (*Objet*, P2).
- **Bombe de mémoire** (en jeu) — balles de souvenirs tirées au revolver. *Objet* revolver + *Fx* balles.
- **Lecture de souvenirs** (passif) — main posée sur la cible.

#### Neon Nostrade — Spécialisation · **P3**
- **Modèle** : ~1,6 bloc, cheveux bleu-violet, tenue de jeune héritière.
- **Lovely Ghostwriter** (en jeu) — sa main tient un stylo qui **écrit seul** un poème de prédiction. *Objet*
  stylo + carnet ; *Fx* texte qui s'écrit. Lien : un « esprit » invisible guide sa main (animation).

#### Razor — Émission · **P2**
- **Modèle** : grand (~2,1 blocs), forte carrure, cheveux longs, tenue de maître du jeu (Greed Island).
- **Accessoires** : ballon (*Objet*).
- **Balle d'aura** (en jeu) — ballon de dodgeball frappé avec une puissance monstrueuse. *Fx* ballon enflammé.
- **Quatorze démons** (en jeu) — **14 démons numérotés** (dont certains fusionnent en un démon géant). *Geo-Fx*
  démon (numéro peint, variantes). Lien : apparaissent autour de lui.
- **Smash** (en jeu, inventée) — *Fx* impact.

#### Genthru — Matérialisation · **P2**
- **Modèle** : ~1,8 bloc, chef des Bombers, tenue de Greed Island ; visage abîmé après l'explosion (variante).
- **Petite fleur / Little Flower** (en jeu) — il saisit la tête de la cible et la fait exploser. *Fx* explosion
  dans la main. Lien : sa main encaisse.
- **Compte à rebours / Countdown** (en jeu) — un **démon bombardier** apparaît pour expliquer la règle, une bombe
  marquée d'un compte à rebours se colle à la cible. *Geo-Fx* démon + *Objet* bombe (chiffres qui défilent,
  texture `sprite_digits`). Lien : la bombe suit l'os de la poitrine de la cible.

#### Kite — Matérialisation · **P2**
- **Modèle** : grand (~1,9 bloc), très longs cheveux blancs, chapeau, long manteau, allure de chasseur.
- **Crazy Slot** (en jeu) — un **clown** de la roulette parle et attribue une arme au hasard : faux (**Silent
  Waltz**), fusil, bâton… (neuf emplacements). À faire : *Geo-Fx* clown (bouche qui parle) + *Objet* chaque arme.
  Lien : l'arme tirée remplace l'objet en main pendant la technique.

#### Morel Mackernasey — Manipulation · **P2**
- **Modèle** : grand et massif (~2 blocs), longs cheveux et barbe, **pipe géante** (calebasse).
- **Accessoires** : pipe (*Objet*, P2).
- **Deep Purple** (en jeu) — de la fumée de sa pipe naissent des **soldats de fumée** numérotés. *Geo-Fx*
  soldat de fumée. Lien : la fumée sort de la pipe.
- **Nuage aveuglant** (en jeu) — *Fx* fumée.
- **Prison de fumée / Smoky Jail** (absente, *IDÉE*) — une cage de fumée dôme qui enferme. *Fx* dôme de fumée.

#### Palm Siberia — Renforcement (Manipulation) · **P3**
- **Modèle** : ~1,7 bloc, longs cheveux noirs, yeux fous ; **variante « Veuve noire »** : corps de Chimère,
  armure noire, cheveux-lames.
- **Wink Blue** (passif en jeu) — **un œil de cristal** (et un sirène dans une boule de cristal) qui montre la
  cible à distance. *Objet* boule de cristal.
- **Veuve noire** (en jeu) — bascule vers la variante armure. *Geo* variante + animation.

#### Meruem — Émission · **P1**
- **Modèle** : le Roi (~1,9 bloc), peau verte, **casque crânien** sur la tête, yeux fendus, **queue** articulée
  (chaîne d'os) terminée en pointe, corps musclé, cape (plus tard). Expressions : neutre, rage.
- **Rafale de rage** (en jeu) — explosion d'aura sombre. *Fx*.
- **Photon** (en jeu) — rayon d'aura blanc. *Fx* rayon.
- **Frappe du Roi**, **Pression royale**, **Accélération royale** (en jeu, en partie inventées).
- **Queue du Roi** (en jeu) — coup de queue. Lien : animation de la queue (os).
- **Ailes de Pouf / Beelzebub** (absentes, *IDÉE*) — après avoir absorbé ses Gardes, il gagne des ailes. *Geo*
  ailes.

#### Menthuthuyoupi (Youpi) — Transformation · **P1**
- **Modèle** : énorme (~2,8 blocs), crâne, peau sombre, corps massif.
- **Métamorphose** (en jeu) — il transforme ses bras en **armes** (lames, masses, ailes, gueules). *Geo* bras
  variantes (lame, poing géant). Lien : remplacent les os du bras.
- **Rafale de rage** (en jeu) — *Fx*.
- **Poing colossal**, **Lames organiques**, **Ailes monstrueuses** (en jeu) — variantes du bras et des ailes.

#### Shaiapouf (Pouf) — Manipulation · **P1**
- **Modèle** : ~1,8 bloc, cheveux blonds en mèches, **ailes de papillon**, costume élégant.
- **Message spirituel** (en jeu) — **écailles dorées** de ses ailes qui lisent les émotions. *Fx* poudre dorée.
- **Belzébuth** (en jeu) — il se divise en **dizaines de mini-Pouf** volants. *Geo-Fx* mini-Pouf. Lien : même
  modèle à petite échelle.
- **Cocon** (absente, *IDÉE*) — il enveloppe le Roi dans un cocon de soie. *Fx* cocon.

#### Zushi — Manipulation · **P3**
- **Modèle** : enfant (~1,3 bloc), dôgi de Shingen-ryu, crâne rasé.
- **Poing de Ren** (en jeu) — *Fx* impact.

#### Senritsu — Émission · **P3**
- **Modèle** : petite (~1,4 bloc), apparence transformée par la Mélodie démoniaque, costume noir.
- **Accessoires** : flûte (*Objet*, P2).
- **Mélodie** (en jeu) — sa flûte joue la « Mélodie de l'amour ». *Fx* notes de musique + ondes. Lien : sortent de
  la flûte.

#### Biscuit Krueger — Transformation · **P1**
- **Modèle** : deux formes — **petite** (~1,4 bloc, fillette blonde à couettes, robe rose) et **grande**
  (~2,5 blocs, très musclée). Deux *Geo* complets.
- **Changer de forme** (en jeu) — animation de transformation.
- **Cookie, la masseuse / Magical Esthetician** (en jeu) — **Cookie**, petite esthéticienne en tenue, apparaît et
  masse. *Geo-Fx* Cookie + table de massage (P3). Lien : apparaît à côté de Biscuit.

#### Chrollo Lucilfer — Spécialisation · **P1**
- **Modèle** : ~1,8 bloc, cheveux noirs plaqués, **croix tatouée sur le front**, long manteau à col de fourrure.
  Variante : cheveux libres, bandage sur le front.
- **Chasseur de compétences / Skill Hunter** (en jeu) — un **livre** avec une **empreinte de main** sur la
  couverture. *Objet* livre + pages qui se tournent. Lien : la page de la technique volée s'illumine.
- **Techniques volées** (absentes, *IDÉE*) : *Indoor Fish* (poisson carnivore), *Sun and Moon* (tampons-bombes),
  *Convert Hands*, *Fun Fun Cloth* (tissu qui rétrécit). *Geo-Fx* chacun.

#### Feitan Portor — Transformation · **P2**
- **Modèle** : petit (~1,55 bloc), cheveux noirs, **masque-col** remontant sur la bouche, longue tunique sombre.
- **Accessoires** : **parapluie-épée** (*Objet*, P2).
- **Soleil levant / Rising Sun** (en jeu) — il revêt une **armure de flammes** et fait naître un **soleil**
  au-dessus de lui qui brûle tout. *Fx* soleil + *Geo* armure de feu (variante). Lien : la puissance suit la
  jauge de Douleur.
- **Vague de chaleur**, **Éclat solaire**, **Pas fantôme** (en jeu, en partie inventées).

#### Phinks Magcub — Renforcement · **P2**
- **Modèle** : ~1,85 bloc, crâne rasé, pas de sourcils, survêtement.
- **Ripper Cyclotron** (en jeu) — il **fait tourner son bras** comme une hélice : chaque tour renforce le coup.
  Animation de moulinets + *Fx* traînée circulaire.

#### Shalnark — Manipulation · **P2**
- **Modèle** : ~1,75 bloc, cheveux blonds, visage enfantin, tenue décontractée.
- **Accessoires** : **téléphone** et **antennes** (*Objet*, P2).
- **Voix noire / Black Voice** (en jeu) — il plante une **antenne** dans la cible et la contrôle par son
  téléphone. *Objet* antenne plantée (suit l'os de la cible). Lien : téléphone ↔ antenne.
- **Pilote automatique** (en jeu) — antenne plantée dans sa propre tête : aura déchaînée. *Fx* + antenne.

#### Knov — Émission · **P2**
- **Modèle** : grand (~1,9 bloc), cheveux noirs, costume, lunettes ; variante après le traumatisme (cheveux
  blancs).
- **Marquer une porte / Scream** (en jeu) — marque d'aura sur un mur ou une porte. *Fx* marque (porte d'aura).
- **Cache-cache / Hide and Seek** (en jeu) — portail vers un **appartement** dans une autre dimension. *Fx*
  portail + pièce construite (structure).

#### Knuckle Bine — Matérialisation · **P2**
- **Modèle** : ~1,8 bloc, cheveux en arrière, veste sans manches, attitude de voyou.
- **Hakoware (passif) / Activer Pot-Clean** (en jeu) — **Pot-Clean**, petite créature au visage d'enfant qui se
  colle à la cible et **affiche la dette** (chiffres qui augmentent). *Geo-Fx* Pot-Clean + *Fx* compteur. Lien :
  suit la cible jusqu'au remboursement.
- **Annuler Pot-Clean** (en jeu) — Pot-Clean disparaît.

#### Wing — Renforcement · **P3**
- **Modèle** : ~1,75 bloc, lunettes, chemise mal rentrée, allure de professeur.
- **Shingen-ryû** (en jeu, inventée) — posture de garde. Animation.

### 5.2 Personnages hors liste jouable (déjà en PNJ ou combattants)

#### Satotz, Buhara, Beans · **P3**
Pas de Hatsu montré : uniquement le modèle (voir 1.1). Satotz : longs membres pour la course de l'Examen. Buhara :
animation de dégustation (porc entier).

#### Gido — Manipulation (combattant de la Tour) · **P3**
- **Modèle** : petit, **jambe de bois**, chapeau. Animation de rotation.
- **Toupies tourbillon** (en jeu) — nuée de **toupies** lancées. *Objet* toupie.
- **Danse de la tornade** (en jeu) — il tourne sur sa jambe de bois. Animation.

#### Sadaso — Émission · **P3**
- **Modèle** : grand, visage creusé, **bras gauche manquant** (manche vide).
- **Main fantôme** (en jeu) — un **bras gauche d'aura invisible** agrippe la cible. *Fx* bras fantôme (visible au
  Gyo). Lien : part de l'épaule gauche.

#### Riehlvelt — Transformation · **P3**
- **Modèle** : en **fauteuil roulant** (*Objet* intégré au modèle), cheveux mi-longs.
- **Fouet électrique** (en jeu) — fouet chargé d'électricité. *Objet* fouet + *Fx* arcs.
- **Canon à air** (en jeu) — air comprimé tiré par le fauteuil. *Fx* bouffée d'air.

#### Kastro — Renforcement · **P2**
- **Modèle** : ~1,85 bloc, cheveux longs attachés, tenue de combat chinoise.
- **Morsure du tigre** (en jeu) — mains en gueule de tigre. *Fx* gueule de tigre sur les mains.
- **Double** (en jeu) — un **double** de lui-même (Matérialisation, hors de son type). *Geo* même modèle,
  translucide. Lien : copie l'animation du vrai Kastro.

### 5.3 Personnages avec un Hatsu, absents du jeu (*IDÉE*, à valider)

Rien n'est codé. Chaque entrée = un modèle *Geo* + les effets de son Hatsu.

| Personnage | Hatsu (type quand il est connu) | Ce que contient la technique | Modèles à faire | Priorité |
|---|---|---|---|---|
| Kortopi (Brigade) | **Gallery Fake** — Matérialisation | Copie parfaite d'un objet (ou de corps) touché de la main droite, qui disparaît au bout de 24 h | *Geo* petit personnage couvert de cheveux (un œil visible) ; *Fx* copie translucide qui se solidifie | P3 |
| Bonolenov Ndongo (Brigade) | **Battle Cantabile** — Matérialisation | Ses trous dans la peau jouent une musique en dansant ; « Jupiter » : armure planétaire | *Geo* corps bandé ; *Geo* armure « Jupiter » ; *Fx* notes | P3 |
| Shoot McMahon | **Hotel Rafflesia** et mains flottantes | Trois mains d'aura flottantes qui frappent ; une **cage à oiseau** enferme les parties du corps arrachées | *Geo* Shoot (bras gauche manquant) ; *Geo-Fx* 3 mains ; *Objet* cage | P3 |
| Ikalgo | Tireur, **Living Dead Dolls** | Pieuvre (Chimère) ; contrôle des cadavres grâce à des sangsues | *Geo* Ikalgo (pieuvre, chapeau) ; *Fx* sangsues | P3 |
| Welfin | **Missile Man** — Matérialisation | Un canon lance des missiles d'aura dès qu'on lui ment | *Geo* Welfin (Chimère loup) ; *Geo-Fx* canon et missiles | P3 |
| Zazan | **Queen Shot** | Le dard de sa queue transforme ses victimes en serviteurs ; seconde forme monstrueuse | *Geo* Zazan (deux formes) ; *Fx* dard | P3 |
| Goreinu | **Gorille blanc / Gorille noir** — Matérialisation | Deux gorilles d'aura ; le blanc permet d'échanger sa place avec une cible | *Geo* Goreinu ; *Geo-Fx* deux gorilles | P3 |
| Abengane | Exorciste | Une bête de Nen qui dévore le Nen d'un autre (lien avec l'exorcisme déjà dans le jeu) | *Geo* Abengane ; *Geo-Fx* bête de Nen | P3 |
| Hanzo | **Hanzo Skwad** | Un double astral qui agit à sa place | *Geo* ninja chauve ; *Geo* double translucide | P3 |
| Alluka / Nanika | Souhaits (pouvoir hors norme) | Nanika exauce les vœux ; yeux noirs, bouche béante | *Geo* Alluka, expression « Nanika » ; *Fx* aura noire | P2 |

*D'autres personnages ont un Hatsu (prétendants de la succession, Zodiaques…), mais leurs capacités sont trop
peu détaillées dans l'œuvre pour être listées sans inventer. À compléter si Evan le souhaite.*
