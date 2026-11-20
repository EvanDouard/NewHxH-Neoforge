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
| Satotz | Joueur + coiffure, moustache | Geo : grand et maigre, longs membres, moustache, pas de bouche | P2 |
| Menchi | Joueur + couettes | Geo : cinq couettes, résille, short | P2 |
| Buhara | Joueur ×1,5 + ventre | Geo : colosse (≈ 2,5 blocs), ventre énorme, gros appétit (animation) | P2 |
| Beans | Joueur ×0,7 + dôme | Geo : petit, tête en haricot, costume | P2 |
| Les 36 autres personnages jouables (Pitou, Illumi, Silva, Zeno, Chrollo, Uvogin, Meruem…) | Aucun modèle : le joueur garde son skin, un mob garde son apparence | Geo pour chacun, en commençant par les antagonistes visibles (Brigade Fantôme, Fourmis-Chimères, Zoldyck) | P1 à P3 |
| Combattants nommés de la Tour : Gido, Sadaso, Riehlvelt, Kastro | Combattant générique (4 skins de lutteur) | Geo : Gido (jambe de bois, toupies), Sadaso (bras fantôme), Riehlvelt (fauteuil roulant, fouet), Kastro (tenue de combat, double) | P2 |

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
| Marionnette (`PUPPET`), fils | Pitou (Terpsichora), Shalnark | Fils d'aura reliés à une silhouette de marionnettiste | P2 |
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
| Statue de Kannon et mains de Kannon | Netero (Bodhisattva, Main zéro) | Geo-Fx : grande statue (≈ 8 blocs), mille mains, animation de frappe | P1 |
| Dragons de Zeno (tête de dragon, descente du dragon) | Zeno | Geo-Fx : dragon d'aura serpentin, animé | P1 |
| Poupée Doctor Blythe | Neferpitou | Geo-Fx : poupée-chirurgienne (déjà « poupée 3D » par le code) | P1 |
| Démons de Razor (14) | Razor | Geo-Fx : démons humanoïdes numérotés | P2 |
| Soldats de fumée | Morel (Deep Purple) | Geo-Fx : soldats de fumée, texture translucide | P2 |
| Cookie la masseuse | Biscuit | Geo-Fx : petite masseuse | P2 |
| Blinky | Shizuku | Geo-Fx (ou objet) : aspirateur avec son visage | P2 |
| Soleil levant | Feitan | Fx : soleil de feu texturé, chaleur | P2 |
| Clown et armes de la roulette | Kite (Crazy Slots) | Geo-Fx : clown parlant + les armes en objets (2.3) | P2 |
| Pot Clean (compteur) | Knuckle | Geo-Fx : petite créature compteur avec ses chiffres | P2 |
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

Ordre conseillé :
1. Les fondations : GeckoLib, le squelette standard, un modèle humain de base, le personnage pilote.
2. Les effets génériques (3.2), qui améliorent toutes les techniques d'un coup.
3. Le casting principal et les bêtes qui sont encore des animaux de Minecraft (Kiriko, Fourmi-Chimère).
4. Le reste par ordre de priorité.
