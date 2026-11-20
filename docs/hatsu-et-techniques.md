# Hatsu et techniques

Document unique sur les techniques du mod : tout ce qui est **dans le jeu** (principes du Nen, coups de base,
familles et coups secrets, les personnages jouables avec leurs passifs, techniques et serments, serments communs,
Hatsu des mobs, combattants de la Tour Céleste), puis les **pistes pas encore codées**.

- La partie « Dans le jeu » est régénérée depuis le code : `python3 tools/docs/hatsu_doc.py` (noms et descriptions
  des fichiers de langue, coûts et recharges du code). Ne pas la modifier à la main.
- La partie « Pas encore dans le jeu » s'écrit à la main : Evan y dit ce qu'on fait ou non.

Remplace les anciens `kits-personnages.md` (fiche de conception des 36 personnages, tous jouables aujourd'hui :
leurs descriptions à jour sont ci-dessous) et `propositions-techniques.md` (repris en partie 8).

# Dans le jeu

<!-- DEBUT-GENERE -->
*Généré depuis le code par `tools/docs/hatsu_doc.py` — 95 techniques enregistrées. Aura = coût d'aura au lancement, tel qu'affiché en jeu (×10 par rapport aux unités du code) ; recharge en secondes ; une technique ratée coûte quand même.*

## 1. Principes du Nen

Wing enseigne les bases (Ten → In → Ren → Hatsu). Le reste se débloque **tout seul** quand les maîtrises requises sont atteintes (onglet « Graphe du Nen » du carnet). In et Zetsu partagent la même maîtrise (le Zetsu est un palier de l'In), de même que Ten et Ken.

| Technique | Déblocage (réglages par défaut) | Ce qu'elle fait |
|---|---|---|
| **Ten** | enseigné par Wing | Garder son aura autour du corps : résistance de Nen contre chaque coup reçu. Au début, le tenir coûte beaucoup d'aura ; maîtrisé (niveau 5), il ne coûte plus rien. S'entraîne en le tenant. |
| **In** | enseigné par Wing | Cacher son aura. C'est la même maîtrise que le Zetsu. Au début, tenir l'In coûte beaucoup d'aura. S'entraîne en le tenant. |
| **Zetsu** | In 5 | Fermer les pores : plus de défense de Nen, mais l'aura se régénère bien plus vite. Même maîtrise que l'In. |
| **Ren** | enseigné par Wing | Faire gonfler l'aura : le plafond d'aura monte un moment et les coups font plus mal, mais l'aura se vide. S'entraîne en le tenant (donne aussi de la Volonté). |
| **Ken** | Ten 5 + Ren 3 | Aura de combat constante : bonne résistance et petit bonus de dégâts, mais coût soutenu. S'entraîne en le tenant (donne aussi de la Volonté). |
| **Gyo** | Ren 5 | Concentrer l'aura dans les yeux pour voir ce qui est caché. Plus la maîtrise monte, plus tu perces. |
| **Shu** | Ten 8 | Envelopper une arme ou un outil d'aura : plus de dégâts, minage plus rapide, usure réduite. |
| **En** | Ten 10 + Ren 8 | Étendre l'aura en bulle pour sentir ce qui s'y trouve. Le rayon grandit avec la maîtrise ; la forme se personnalise dès le niveau 50. |
| **Ko** | Ten 10 + In 10 + Ren 10 + Gyo 5 + Hatsu appris | Concentrer toute l'aura dans les poings : un coup dévastateur, mais le corps est découvert. Dure plus longtemps avec la maîtrise. |
| **Ryu** | Gyo 15 + Ko 10 + Ken 10 (lien Ken incertain) | Répartir l'aura entre attaque et défense. La maîtrise amplifie l'effet de la répartition. |
| **Hatsu** | enseigné par Wing (divination puis choix du personnage) | La technique propre à ton type de Nen. Son coût en aura baisse avec le niveau de Nen. |
| **Soin par le Nen** | quête de Netero | Convertit de l'aura en vie tant que la touche est maintenue. Plus le niveau monte, plus c'est rapide et économe. Monte en soignant des PV. |

## 2. Coups de base (sans Nen)

Disponibles dès le début, gratuits, à la fin de la liste des techniques. Type Physique : 100 % pour tout le monde. La recharge baisse avec l'Endurance.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Attaque basique** <br>*Basic attack* | Physique | 0 | 0,6 s | — | Un coup de poing, droit puis gauche. Sans Nen. L'Endurance réduit la recharge. |
| **Coup de pied** <br>*Kick* | Physique | 0 | 1,5 s | — | Projette la cible selon ta Force. Sans Nen. L'Endurance réduit la recharge. |
| **Frappe chargée** <br>*Charged strike* | Physique | 0 | 2 s | — | Maintiens la touche : la barre monte et redescend ; relâche dans le vert pour une frappe parfaite. L'Endurance réduit la recharge. |

## 3. Familles (clans) et coups secrets

La famille est tirée à la création du personnage (poids ci-dessous). Chaque famille a ses bonus et un **coup secret** sans Nen, placé juste avant les coups de base : il se règle sur les stats, ne peut être ni copié par Ging ni volé par Chrollo. Le niveau de clan monte avec l'entraînement et les coups secrets (+30 XP par coup) et renforce les bonus.

| Famille | Tirage | Bonus | Coup secret |
|---|---|---|---|
| **Freecss** | poids 6 | Le sang de Ging et de Gon : Force, Vitesse et Vitalité s'entraînent 20 % plus vite. Coup secret : Instinct du chasseur. | Instinct du chasseur |
| **Kurta** | poids 8 | Clan aux yeux écarlates : sous 40 % de vie, les techniques gagnent 30 % de puissance ; la Volonté s'entraîne plus vite. Coup secret : Colère écarlate, qui dure plus longtemps avec le niveau de clan (15 s → 5 min). | Colère écarlate |
| **Sans famille** | poids 70 |  | — |
| **Paladiknight** | poids 8 | Famille de Leorio, tournée vers la médecine : Endurance et Vitalité s'entraînent plus vite. Coup secret : Premiers soins. | Premiers soins |
| **Zoldyck** | poids 8 | Famille d'assassins de la montagne Kukuro : immunisé au poison, la foudre ne fait que la moitié des dégâts, Vitesse et Endurance s'entraînent plus vite. Coup secret : Main-griffe. | Main-griffe |

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Instinct du chasseur** <br>*Hunter's Instinct* | Physique | 0 | 40 s | — | Coup secret des Freecss : l'odorat et l'ouïe de Gon. Toutes les créatures à 32 blocs sont repérées 12 s, à travers les murs, et tu gagnes Vitesse I. Sans Nen. |
| **Colère écarlate** <br>*Scarlet Wrath* | Physique | 0 | 90 s | — | Coup secret des Kurta : tes yeux virent à l'écarlate. Ton Nen devient Spécialisation et toutes les catégories passent à 100 % d'efficacité. Dure 15 s au niveau de clan 0, jusqu'à 5 min au niveau 100 ; la recharge part après. Sans Nen. |
| **Premiers soins** <br>*First Aid* | Physique | 0 | 45 s | — | Coup secret des Paladiknight : soigne l'allié devant toi (4 blocs) ou toi-même, 4 PV + un peu selon la Vitalité, et retire poison et faiblesse. Sans Nen. |
| **Main-griffe** <br>*Claw Hand* | Physique | 0 | 15 s | — | Coup secret des Zoldyck : la main se raidit en griffe, tu fonds sur la cible (8 blocs) et la lacères. Dégâts selon la Force et la Vitesse, saignement 3 s. Sans Nen. |

## 4. Personnages jouables (43)

Choisis une fois pour toutes au Hatsu, parmi les personnages de son type (pastilles de tri). Toutes les techniques marchent aussi pour un mob qui incarne le personnage (déclenchées par son IA). ★ = ultime (Nen 30 et plus) : jamais pour une copie portée par un mob de Minecraft.

### Premier lot (un par type, puis Netero et Hisoka)

#### Gon Freecss — Renforcement

*aura #F5D547, aura calme*  
Garçon de l'île de la Baleine, pur Renforcement. Son Jajanken décline la pierre-feuille-ciseaux.

- **Passif — Instinct sauvage** : +10 % de dégâts au corps à corps.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Jajanken : Pierre** <br>*Jajanken: Rock* | Renforcement | 100 | 8 s | — | Gon charge son aura dans le poing, immobile : une boule d'une demi-taille de joueur brille et change de couleur à chaque palier tant qu'on maintient la touche. Relâcher la lance tout droit : elle creuse sa route, emporte la cible et explose. Plus on charge, plus c'est destructeur. Le Ryu tourné vers l'attaque la rend plus grosse. |
| **Jajanken : Ciseaux** <br>*Jajanken: Scissors* | Transformation + Renforcement | 250 | 5 s | Nen 5 | L'aura prend la forme d'une lame qui file tout droit jusqu'à toucher un bloc ou un ennemi. |
| **Jajanken : Papier** <br>*Jajanken: Paper* | Émission + Renforcement | 300 | 6 s | Nen 10 | Gon ouvre la main et libère son aura d'un coup : un cône d'émission qui frappe tout ce qui est devant lui et le projette loin (catégorie Émission : 80 % pour Gon). |
| **Jajanken : Pierre sans retenue** ★ <br>*Jajanken: Rock, all out* | Renforcement | 400 | 20 s | Nen 30 | Gon met tout dans le poing : la boule enfle jusqu'à deux fois sa taille, puis se recondense, minuscule et aveuglante. Ceux qui la regardent sont éblouis. Coûte deux fois plus d'aura, frappe bien plus fort et creuse un cratère énorme. |

#### Killua Zoldyck — Transformation

*aura #7FD3FF, aura calme*  
Héritier des Zoldyck : il transforme son aura en électricité.

- **Passif — Corps Zoldyck** : Immunisé à la foudre et au poison.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Foudre** <br>*Thunderbolt* | Transformation + Émission | 100 | 6 s | — | Décharge électrique qui paralyse presque complètement la cible. |
| **Godspeed** | Transformation + Manipulation | 500 | 30 s | Nen 10 | Son corps réagit par réflexe électrique : vitesse III et minage rapide pendant 10 s. |

#### Kurapika — Matérialisation

*aura #9AD7FF, aura calme*  
Dernier survivant du clan Kurta : chaînes matérialisées.

- **Passif — Chaîne radiesthésique** : Les monstres à moins de 12 blocs brillent et se voient à travers les murs.
- **Serment — Chaîne-prison : la Brigade seulement** : Comme Kurapika : la Chaîne-prison ×3, mais seulement contre la Brigade Fantôme (joueurs de la Brigade, mobs avec le personnage d'un de ses membres). La lancer sur quelqu'un d'autre : le coup ne porte pas, le serment est rompu et tu meurs.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Prison de chaînes** <br>*Chain Jail* | Matérialisation + Manipulation | 450 | 20 s | Nen 5 | La chaîne immobilise la cible (lenteur VII) et l'affaiblit pendant 5 s. |
| **Chaîne sacrée** <br>*Holy Chain* | Matérialisation + Renforcement | 350 | 15 s | — | La chaîne du pouce soigne 8 PV. |

#### Neferpitou — Spécialisation

*aura #7A2E8C, aura sauvage (flammes même au repos)*  
Garde royal du roi des fourmis-chimères, spécialiste félin.

- **Passif — Agilité féline** : Aucun dégât de chute.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Terpsichora** | Spécialisation + Manipulation | 600 | 30 s | Nen 10 | Le corps manipulé comme une marionnette : force II, vitesse II et saut II pendant 10 s. |
| **Doctor Blythe** | Spécialisation + Matérialisation | 200 | 20 s | — | La poupée-chirurgienne apparaît au-dessus du blessé devant toi (ou de toi-même) et le recoud : 16 PV en 4 s. Tu restes immobile pendant l'opération. |

#### Illumi Zoldyck — Manipulation

*aura #4A3570, aura sauvage (flammes même au repos)*  
Aîné des Zoldyck : aiguilles qui manipulent les corps.

- **Passif — Entraînement Zoldyck** : Immunisé au poison.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Aiguille** <br>*Needle* | Manipulation | 250 | 5 s | — | Une aiguille qui dérègle le corps : lenteur III et nausée pendant 5 s. |
| **Aiguille de métamorphose** <br>*Transformation Needle* | Manipulation + Transformation | 500 | 30 s | Nen 10 | Se plante des aiguilles pour remodeler son corps : résistance et force pendant 15 s. |

#### Leorio Paladiknight — Émission

*aura #F2A33A, aura calme*  
Futur médecin : il fait voyager ses coups de poing à distance.

- **Passif — Médecin** : Récupère 1 PV toutes les 5 secondes.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Coup à distance** <br>*Remote Punch* | Émission | 300 | 6 s | — | Le poing frappe le sol et ressort sous la cible, jusqu'à 12 blocs. |
| **Onde de choc** <br>*Shockwave* | Émission | 400 | 10 s | Nen 5 | Frappe le sol : onde qui blesse et repousse tout autour. |

#### Isaac Netero — Renforcement

*aura #FFD966, aura calme*  
Président de l'Association, maître absolu du Renforcement. Sa force vient de la Prière.

- **Passif — Prière** : Une jauge de Prière propre à Netero. « Méditer » devient « Prière » : immobile, mains jointes, la jauge se remplit (pleine en 20 s). Chaque technique de Kannon en consomme une partie.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Poings de Kannon** <br>*Kannon's Fists* | Renforcement | 150 + 15 Prière | 15 s | — | Kannon prête ses poings à Netero : pendant 10 s, ses coups sont renforcés (×1,6), mains dorées sur les poings. Coûte 15 de Prière. |
| **Bodhisattva aux mille mains** <br>*Hundred-Type Guanyin Bodhisattva* | Renforcement + Émission | 300 + 30 Prière | 10 s | Nen 10 | L'immense statue de Kannon surgit derrière Netero et frappe d'une paume géante tout ce qui est devant (12 blocs). Coûte 30 de Prière. |
| **Main zéro** ★ <br>*Zero Hand* | Renforcement + Émission | 600 + 100 Prière | 60 s | Nen 40 | Prière pleine : tout est dépensé. Un rayon d'aura pure jaillit de la statue (24 blocs, 40 dégâts). |

#### Hisoka Morow — Transformation

*aura #FF4FA3, aura sauvage (flammes même au repos)*  
Magicien imprévisible, amoureux des combats.

- **Passif — Frisson du combat** : Plus l'adversaire est robuste (vie max), plus tes coups font mal : +1 % par PV au-delà de 20, jusqu'à +30 %.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Bungee Gum** | Transformation | 200 | 1,5 s | — | Colle une aura élastique sur la cible devant toi. Tant que le lien tient (10 s), elle est ramenée vers toi dès qu'elle s'éloigne. Relance la technique : l'élastique se contracte d'un coup et la projette contre toi. |
| **Cartes renforcées** <br>*Reinforced cards* | Transformation + Émission | 100 | 1,5 s | — | Une carte à jouer renforcée par le Nen, lancée comme une lame. Elle tranche tout ce qu'elle touche. |
| **Texture surprise** <br>*Texture Surprise* | Transformation | 400 | 25 s | Nen 10 | Tu recouvres ton corps d'une fausse texture : tu disparais 8 s dans une gerbe de cartes, et les monstres te perdent de vue. |

### Association Hunter et examinateurs

#### Pariston Hill — Spécialisation

*aura #FF9FC8, aura calme*  
Vice-président de l'Association, manipulateur souriant.

- **Passif — Charisme** : Un monstre qui te prend pour cible a une chance sur trois de t'oublier (toutes les 3 s). Un débiteur (Dette d'amour) se blesse de la moitié des coups qu'il te porte.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Dette d'amour** <br>*Love Debt* | Spécialisation | 250 | 15 s | — | Marque une cible (10 blocs) pendant 10 s : la moitié des coups qu'elle te porte lui revient. Un cœur rose la désigne. |

#### Ging Freecss — Émission

*aura #E4E4E4, aura calme*  
Le père de Gon, Hunter de légende et archéologue.

- **Passif — Archéologue** : Toutes les 10 s, indique le coffre le plus proche (24 blocs).

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Poing à distance** <br>*Remote Punch* | Émission | 200 | 4 s | — | Un poing d'aura jaillit d'un portail à côté de la cible (10 blocs). |
| **Copie** <br>*Copy* | Émission + Spécialisation | 200 | 10 s | Nen 20 | Reproduit, à 70 % de sa puissance, la dernière technique lancée près de toi (16 blocs). |

#### Cheadle Yorkshire — Manipulation

*aura #9BC4FF, aura calme*  
Secrétaire puis présidente de l'Association.

- **Passif — Secrétaire** : Recharges des techniques −10 %.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Règlement** <br>*Rules* | Manipulation | 350 | 25 s | Nen 10 | Zone de 8 blocs pendant 8 s : les ennemis sont ralentis et ne peuvent plus se téléporter. |

#### Botobai Gigante — Renforcement

*aura #B8B8B8, aura calme*  
Membre du Zodiaque, vétéran imposant.

- **Passif — Vétéran** : Résiste au recul ; faiblesse et lenteur durent moitié moins.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Pression** <br>*Pressure* | Renforcement | 300 | 12 s | — | Une onde de 6 blocs écrase les ennemis : lenteur et faiblesse. |

#### Biscuit Krueger — Transformation

*aura #FFB6E1, aura calme*  
Maîtresse du Shingen-ryu, experte en pierres précieuses.

- **Passif — Deux formes** : Biscuit alterne entre deux formes : la grande (2,5 blocs, très musclée) frappe ×1,5 mais use son aura ; la petite (1,5 bloc) frappe ×0,8 mais son aura remonte plus vite.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Changer de forme** <br>*Switch Form* | Transformation | 0 | 2 s | — | Passe de la petite à la grande forme, ou l'inverse. |
| **Cookie, la masseuse** <br>*Cookie, the Masseuse* | Transformation + Matérialisation | 300 | 20 s | — | Cookie soigne Biscuit et ses alliés à 3 blocs, régénération et un peu de satiété. |

#### Menchi — Transformation

*aura #FF8AA8, aura calme*  
Hunter gastronome, examinatrice de l'Examen Hunter.

- **Passif — Gastronome** : La nourriture te rassasie deux fois plus.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Repas de gourmet** <br>*Gourmet Meal* | Transformation | 300 | 30 s | — | Un plat partagé avec les alliés à 6 blocs : soin, régénération, Force. |

#### Bushidora Ambitious — Renforcement

*aura #8FBF6F, aura calme*  
Membre du Zodiaque, sabreur.

- **Passif — Bretteur** : +20 % de dégâts à l'épée.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Lame d'aura** <br>*Aura Blade* | Renforcement + Transformation | 250 | 6 s | — | La lame s'allonge et tranche en arc tout ce qui est devant (6 blocs). |

#### Tsezguerra — Renforcement

*aura #E0A040, aura calme*  
Hunter chasseur de primes, chef d'équipe à Greed Island.

- **Passif — Meneur** : Les alliés à 8 blocs gagnent Force I.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Frappe chargée** <br>*Charged Strike* | Renforcement | 250 | 8 s | — | Un coup de poing très puissant au corps à corps. |

### Famille Zoldyck

#### Silva Zoldyck — Émission

*aura #8E6CFF, aura calme*  
Chef de la famille d'assassins Zoldyck.

- **Passif — Corps Zoldyck** : Immunisé à la foudre et au poison.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Grande boule** <br>*Great Orb* | Émission + Transformation | 100 | 8 s | — | Silva forme en une seconde une grosse boule d'aura et la propulse : elle emporte la cible et explose, sans creuser le terrain. |
| **Orbes explosifs** <br>*Explosive Orbs* | Émission + Transformation | 350 | 10 s | — | Trois sphères d'aura violettes, l'une après l'autre, vers la cible (16 blocs). |

#### Zeno Zoldyck — Émission

*aura #FFC34D, aura calme*  
Grand-père de Killua, assassin légendaire.

- **Passif — Corps Zoldyck** : Immunisé à la foudre et au poison.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Tête de dragon** <br>*Dragon Head* | Émission + Transformation | 300 | 8 s | — | Un dragon d'aura doré jaillit de la main et frappe la cible (14 blocs). |
| **Plongée du dragon** ★ <br>*Dragon Dive* | Émission + Transformation | 600 | 30 s | Nen 30 | Le dragon se scinde en dizaines de petits dragons qui pleuvent autour du point visé. |

#### Kalluto Zoldyck — Manipulation

*aura #D9C9FF, aura calme*  
Le plus jeune Zoldyck, danse des papiers.

- **Passif — Corps Zoldyck** : Immunisé à la foudre et au poison.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Danse des papiers** <br>*Paper Dance* | Manipulation | 350 | 15 s | — | Un tourbillon de papiers coupants balaie l'avant pendant 2 s. |

### Brigade Fantôme

#### Chrollo Lucilfer — Spécialisation

*aura #5B4B8A, aura calme*  
Chef de la Brigade fantôme.

- **Passif — Tête** : La tête de l'araignée : +5 % de dégâts par membre de la Brigade à 16 blocs (40 % au plus).

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Chasseur de compétences** <br>*Skill Hunter* | Spécialisation + Matérialisation | 300 | 10 s | Nen 20 | Livre vide : vole la dernière technique lancée par la cible (10 blocs). Page pleine : la lance une fois, puis la page s'efface. |

#### Feitan Portor — Transformation

*aura #FF4500, aura sauvage (flammes même au repos)*  
Membre de la Brigade, tortionnaire.

- **Passif — Douleur** : Chaque PV perdu remplit la jauge de Douleur ; elle redescend hors combat. Jusqu'à +50 % de dégâts quand elle est pleine.
- **Serment — Soleil levant : seulement blessé** : Comme Feitan : le Soleil levant ×1,8, mais il ne se déclenche que sous la moitié de ta vie (refusé sinon). Le serment ne se rompt pas.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Soleil levant** <br>*Rising Sun* | Transformation + Matérialisation | 400 + 100 Douleur | 30 s | Nen 20 | Douleur pleine : un soleil de feu surgit au-dessus de Feitan et brûle tout à 8 blocs. |

#### Nobunaga Hazama — Renforcement

*aura #E8E8FF, aura calme*  
Membre de la Brigade, samouraï.

- **Passif — En** : Un petit En permanent : les ennemis à 4 blocs brillent.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Iai** | Renforcement | 250 | 7 s | — | Une coupe instantanée sur tout ce qui est dans ton En (4 blocs). |

#### Phinks Magcub — Renforcement

*aura #D9A441, aura sauvage (flammes même au repos)*  
Membre de la Brigade, bagarreur.

- **Passif — Bagarreur** : Le prochain coup de poing libère les tours de bras accumulés : +2,5 dégâts par tour.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Ripper Cyclotron** | Renforcement | 40 | 0,3 s | — | Un tour de bras de plus. Maximum : 1 tour par tranche de 10 niveaux de Nen (10 au plus). |

#### Uvogin — Renforcement

*aura #FF6A3D, aura sauvage (flammes même au repos)*  
Membre de la Brigade, force brute pure.

- **Passif — Corps d'acier** : Dégâts physiques reçus −25 %.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Big Bang Impact** | Renforcement | 300 | 20 s | Nen 20 | Uvogin charge son poing (maintenir jusqu'à 2 s) puis frappe le sol devant lui : plus la charge est pleine, plus le cratère est grand (1 à 6 blocs), plus il fait mal et projette loin. |

#### Franklin Bordeau — Émission

*aura #D0703A, aura calme*  
Membre de la Brigade, colosse.

- **Passif — Robuste** : Tous les dégâts reçus −15 %.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Double mitrailleuse** <br>*Double Machine Gun* | Émission | 400 | 15 s | — | Ses dix doigts tirent une rafale d'aura sur la cible (18 blocs). |

#### Shalnark — Manipulation

*aura #7FE0C0, aura calme*  
Membre de la Brigade, manipulateur par antenne.

- **Passif — Technicien** : Un mob vaincu lâche parfois un objet de plus (une chance sur quatre).

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Voix noire** <br>*Black Voice* | Manipulation | 350 | 20 s | Nen 10 | Une antenne plantée dans un mob (8 blocs) : pendant 30 s il attaque tes ennemis. Échoue si la cible a plus de 10 niveaux de Nen de plus que toi. |
| **Pilote automatique** ★ <br>*Autopilot* | Manipulation | 450 | 45 s | Nen 30 | Il se contrôle lui-même : 15 s surpuissant, puis 10 s épuisé. |

#### Machi Komacine — Transformation

*aura #FF7FD0, aura calme*  
Membre de la Brigade, maîtresse des fils.

- **Passif — Suture** : Se soigne d'1 PV toutes les 4 s, et ses alliés à 3 blocs aussi.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Fils de Nen** <br>*Nen Threads* | Transformation + Manipulation | 250 | 10 s | — | Des fils fins enserrent la cible (10 blocs) : très ralentie, ne peut plus sauter. |

#### Shizuku Murasaki — Matérialisation

*aura #B6A8FF, aura calme*  
Membre de la Brigade, distraite.

- **Passif — Ménage** : Attire les objets au sol à 5 blocs.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Blinky** | Matérialisation | 300 | 12 s | — | L'aspirateur aspire devant toi : projectiles détruits, objets ramassés, ennemis attirés. |

#### Pakunoda — Spécialisation

*aura #9F8FFF, aura calme*  
Membre de la Brigade, lit les souvenirs.

- **Passif — Liseuse de pensées** : Un coup au corps à corps révèle la vie, le Nen et l'aura de la cible.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Bombe de mémoire** <br>*Memory Bomb* | Spécialisation | 300 | 12 s | — | Une balle d'aura (16 blocs) : la cible est désorientée, ralentie, aveuglée un instant. |

### York Shin et Greed Island

#### Neon Nostrade — Spécialisation

*aura #FFB0E0, aura calme*  
Fille de chef mafieux, voyante.

- **Passif — Divination** : Une petite chance en permanence (Chance I).

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Lovely Ghostwriter** | Spécialisation | 200 | 30 s | — | Un poème annonce les dangers à 32 blocs (nombre, direction) et les fait briller 10 s. |

#### Razor — Émission

*aura #FF7A3C, aura calme*  
Maître du jeu à Greed Island.

- **Passif — Maître du jeu** : +10 % de dégâts au corps à corps.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Balle d'aura** <br>*Aura Ball* | Émission | 250 | 7 s | — | Un ballon d'aura frappé très fort vers la cible (18 blocs), gros recul. |
| **Quatorze démons** ★ <br>*Fourteen Devils* | Émission + Matérialisation | 600 | 30 s | Nen 30 | Quatorze démons d'aura bombardent la zone autour du point visé. |

#### Genthru (le Bombardier) — Matérialisation

*aura #FF5A2A, aura sauvage (flammes même au repos)*  
Le Bombardier de Greed Island.

- **Passif — Bombardier** : Dégâts d'explosion reçus −50 %.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Petite fleur** <br>*Little Flower* | Matérialisation | 300 | 10 s | — | Il saisit la cible et lui explose au visage (sa main encaisse un peu). |
| **Compte à rebours** <br>*Countdown* | Matérialisation + Émission | 400 | 30 s | Nen 20 | Pose une bombe sur la cible : 3, 2, 1… elle explose. |

### Hunters de l'arc des Fourmis

#### Kite — Matérialisation

*aura #E6E6FA, aura calme*  
Disciple de Ging, Hunter des espèces.

- **Passif — Chasseur** : +15 % de dégâts contre les bêtes et les monstres.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Crazy Slot** <br>*Crazy Slots* | Matérialisation + Spécialisation | 350 | 12 s | — | La roulette tire une arme au hasard : faux, fusil, bâton ou clown. |

#### Knov — Émission

*aura #7AA0D0, aura calme*  
Hunter stratège de l'équipe d'extermination.

- **Passif — Stratège** : Prévenu quand un monstre le prend pour cible. Au Gyo, il voit ses portes marquées.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Marquer une porte** <br>*Mark a Door* | Émission + Matérialisation | 100 | 2 s | — | Marque la porte visée (5 blocs). Nombre de marques : 1, plus 1 par tranche de 20 niveaux de Nen (6 au plus) ; la plus ancienne s'efface. |
| **Cache-cache** <br>*Hide and Seek* | Émission + Matérialisation | 300 | 10 s | Nen 10 | Un portail vers la pièce-refuge (abri, régénération). Dans le refuge : choisir par quelle porte marquée ressortir. |

#### Knuckle Bine — Matérialisation

*aura #F0E060, aura calme*  
Hunter au grand cœur, élève de Morel.

- **Passif — Hakoware** : Pot-Clean suit une cible à la fois : chaque coup de Knuckle lui prête la moitié des dégâts (12 → +6), intérêts +5 % toutes les 10 s. Dette = aura max de la cible : banqueroute, 20 s de Zetsu forcé, plus aucune protection contre le Nen.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Activer Pot-Clean** <br>*Activate Pot-Clean* | Matérialisation + Renforcement | 150 | 3 s | — | Pot-Clean s'attache à la cible devant toi (10 blocs) ; une seule à la fois. |
| **Annuler Pot-Clean** <br>*Cancel Pot-Clean* | Matérialisation | 0 | 1 s | — | Pot-Clean se retire, la dette est effacée. |

#### Morel Mackernasey — Manipulation

*aura #8A5AB0, aura calme*  
Hunter de l'équipe d'extermination, fumeur de pipe.

- **Passif — Fumée** : Touché, une chance sur cinq de disparaître 2 s dans un nuage.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Deep Purple** | Manipulation + Transformation | 450 | 20 s | Nen 20 | Des soldats de fumée assaillent l'avant : coups et aveuglement. |

#### Palm Siberia — Renforcement

*aura #6A3A7A, aura calme*  
Élève de Knov, voyante, devenue fourmi-chimère.

- **Passif — Wink Blue** : Le dernier ennemi frappé reste visible (il brille 30 s).

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Veuve noire** <br>*Black Widow* | Renforcement + Transformation | 500 | 30 s | Nen 20 | Armure noire, cheveux en lames : Force II, Résistance, vitesse pendant 20 s. |

### Fourmis-Chimères

#### Meruem — Émission

*aura #6A2C91, aura sauvage (flammes même au repos)*  
Le roi des fourmis-chimères.

- **Passif — Roi** : Chaque ennemi vaincu le renforce (+0,5 % de dégâts, 25 % au plus).

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Rafale de rage** <br>*Rage Blast* | Émission + Renforcement | 400 | 15 s | — | Une onde d'aura sombre dévastatrice autour de lui (7 blocs). |
| **Photon** ★ | Émission + Renforcement | 500 | 20 s | Nen 30 | Un rayon d'aura blanc concentré (20 blocs). |

#### Shaiapouf — Manipulation

*aura #C0A0FF, aura calme*  
Garde royal du roi, manipulateur.

- **Passif — Ailes** : Il vole librement avec ses ailes de papillon (pas en Zetsu). Aucun dégât de chute.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Message spirituel** <br>*Spiritual Message* | Manipulation | 300 | 15 s | — | Une poudre dorée à 8 blocs : les ennemis brillent, ralentis et désorientés. |
| **Belzébuth** <br>*Beelzebub* | Manipulation + Transformation | 450 | 30 s | Nen 20 | Il se divise en nuée : invisible et rapide 6 s, les mini-Pouf harcèlent les ennemis proches. |

#### Menthuthuyoupi — Transformation

*aura #FF3B3B, aura sauvage (flammes même au repos)*  
Garde royal du roi, monstre de puissance.

- **Passif — Vitalité** : +20 PV max.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Métamorphose** <br>*Metamorphosis* | Transformation | 400 | 25 s | — | Bras en armes : Force II et Résistance pendant 15 s. |
| **Rafale de rage** <br>*Rage Blast* | Transformation + Émission | 450 | 20 s | Nen 20 | Une explosion de colère autour de lui (6 blocs). |

### Autres

#### Zushi — Manipulation

*aura #9FD4FF, aura calme*  
Élève de Wing à la Tour Céleste.

- **Passif — Assidu** : +25 % d'expérience à l'entraînement.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Poing de Ren** <br>*Ren Punch* | Manipulation | 150 | 4 s | — | Un coup renforcé par le Ren au corps à corps. |

#### Senritsu — Émission

*aura #C9A6FF, aura calme*  
Musicienne, entend les battements de cœur.

- **Passif — Battement de cœur** : Elle entend les ennemis cachés (Zetsu, invisibles) à 20 blocs : ils brillent.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Mélodie** <br>*Melody* | Émission + Manipulation | 350 | 20 s | — | Sa flûte soigne les alliés à 8 blocs, ralentit et affaiblit les ennemis. |

#### Wing — Renforcement

*aura #9FD0FF, aura calme*  
Maître de Gon et Killua, Shingen-ryu.

- **Passif — Professeur** : Les alliés à 8 blocs gagnent un peu de Nen avec le temps.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Shingen-ryû** <br>*Shingen-ryu* | Renforcement | 250 | 15 s | — | 10 s en garde : coups reçus divisés par deux, chaque assaillant reçoit 4 dégâts. |

## 5. Serments communs

Accessibles à tous dans l'écran des serments. Rompre un serment scelle le Hatsu. Les serments propres à un personnage sont dans sa fiche (Kurapika, Feitan).

| Serment | Effet |
|---|---|
| **Seulement contre les monstres** | Comme Kurapika et sa chaîne : tes techniques ne visent que des monstres. Puissance et stats ×1,5. Un coup sur un humain, un animal ou un PNJ ne porte pas. Peut se mettre en pause. |
| **Payer de sa vie** | Comme le Temps de l'Empereur : chaque technique coûte 2 PV en plus de l'aura. Puissance et stats ×1,4. Refusée sous 2 PV. |
| **Mains nues** | Aucune technique avec un objet en main principale. Puissance et stats ×1,25. Peut se mettre en pause. |
| **Combat loyal** | Jamais dans le dos : la cible doit te faire face. Puissance et stats ×1,3. Un coup dans le dos ne porte pas. Peut se mettre en pause. |
| **Une seule technique** | Tout sur une technique : celle sélectionnée au moment du serment. Puissance ×1,6 pour elle, les autres techniques de Hatsu sont refusées. |
| **Tout sacrifier** | Comme Gon face à Pitou : tout ×3 pendant 60 s (stats, aura, techniques, même au-delà des limites), puis tout ton Nen est scellé une dizaine de jours de jeu. |
| **Serment du personnage** | Un serment propre à ton personnage. |
| **Heure supplémentaire** | Tu travailles la nuit : tout ×1,3 la nuit (stats, aura, techniques), mais tout ×0,7 le jour. Sans cycle jour/nuit (Tour Céleste), c'est le jour. Ne peut pas être pris en même temps que « Service de jour ». |
| **Service de jour** | L'inverse d'« Heure supplémentaire » : tout ×1,3 le jour, mais tout ×0,7 la nuit. Les deux ne peuvent pas être pris ensemble. |

## 6. Hatsu des mobs

Un mob de Minecraft qui s'éveille au Nen sans incarner de personnage reçoit le Hatsu de son espèce. Jamais proposé au joueur. (Un mob qui reçoit une technique de personnage n'en a qu'une copie affaiblie, sans ultime.)

| Hatsu | Espèces | Type | Technique | Aura | Recharge | Ce qu'elle fait |
|---|---|---|---|---|---|---|
| **Squelette au Nen** | squelette, vagabond, embourbé | Émission | Flèches de Nen | 200 | 4,5 s | Trois flèches d'aura en éventail qui filent droit, sans retomber. |
| **Creeper au Nen** | creeper | Émission | Mines d'aura | 300 | 6 s | Quatre mines autour de la cible éclatent un instant plus tard ; le creeper, lui, n'explose plus. |
| **Zombie au Nen** | zombie, zombie momifié, noyé, zombie villageois | Matérialisation | Horde de Nen | 400 | 20 s | Deux zombies d'aura sortent du sol et attaquent ; ils se dissipent au bout de 15 s. |
| **Araignée au Nen** | araignée, araignée venimeuse | Manipulation | Toile d'aura | 200 | 7 s | Un fil d'aura colle la cible au sol : très ralentie, elle ne peut plus sauter. |
| **Enderman au Nen** | enderman | Spécialisation | Pas éclair | 250 | 6 s | Apparaît dans le dos de la cible et frappe. |
| **Cochon au Nen** | cochon, hoglin | Renforcement | Charge de sanglier | 150 | 5 s | Fonce sur la cible et la renverse. |
| **Vache au Nen** | vache, champimeuh, chèvre | Renforcement | Lait d'aura | 250 | 15 s | Soigne ses alliés autour de lui. |
| **Mouton au Nen** | mouton, lama | Matérialisation | Laine-bouclier | 200 | 15 s | Une laine d'aura protège ses alliés. |
| **Poule au Nen** | poule | Émission | Œufs explosifs | 150 | 6 s | Une volée d'œufs d'aura qui éclatent autour de la cible. |
| **Loup au Nen** | loup, renard, chat, ocelot | Renforcement | Croc d'aura | 150 | 4,5 s | Bond et morsure qui fait saigner. |
| **Slime au Nen** | slime, cube de magma, grenouille | Renforcement | Rebond écrasant | 200 | 6 s | Saute très haut et retombe en onde de choc. |
| **Sorcière au Nen** | sorcière, évocateur | Transformation | Potion de Nen | 250 | 8 s | Affaiblit et ralentit tout ce qui l'entoure. |
| **Golem au Nen** | golem de fer, ravageur | Renforcement | Séisme | 250 | 7 s | Frappe le sol : tout autour est projeté. |
| **Blaze au Nen** | blaze, ghast | Transformation | Pluie de feu | 300 | 8 s | Des boules de feu d'aura tombent autour de la cible. |

## 7. Combattants nommés de la Tour Céleste

Au-delà du 200e étage, selon l'étage du combat. Leurs Hatsu ne sont pas proposés au joueur.


**Gido** — Manipulation, étages 200–209

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Toupies tourbillon** <br>*Whirlwind Tops* | Manipulation | 250 | 6 s | — | Une nuée de toupies d'aura balaie tout autour (3,5 blocs) et repousse. |
| **Danse de la tornade** <br>*Tornado Dance* | Manipulation | 300 | 20 s | — | Gido tourne sur sa jambe de bois : Résistance et Vitesse 6 s. |

**Sadaso** — Émission, étages 210–219

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Main fantôme** <br>*Phantom Hand* | Émission | 250 | 7 s | — | Son bras gauche invisible agrippe la cible à 9 blocs et l'immobilise 3 s. |

**Riehlvelt** — Transformation, étages 220–224

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Fouet électrique** <br>*Electric Whip* | Transformation | 200 | 5 s | — | Fouet chargé d'électricité (7 blocs) : dégâts et lenteur. |
| **Canon à air** <br>*Air Cannon* | Transformation + Émission | 300 | 8 s | — | Air comprimé tiré depuis le fauteuil (14 blocs) : gros recul. |

**Kastro** — Renforcement, étages 225–234

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Morsure du tigre** <br>*Tiger Bite* | Renforcement | 200 | 4,5 s | — | Mains en gueule de tigre, au corps à corps : très gros dégâts. |
| **Double** | Renforcement | 400 | 25 s | — | Son double attire les coups pendant que le vrai Kastro disparaît : Invisibilité et Force 4 s. |
- **Hisoka Morow** (étages 235–240) : son vrai kit de personnage (voir sa fiche).
<!-- FIN-GENERE -->

# Pas encore dans le jeu

## 8. Propositions de nouvelles techniques (19 nov) — à valider par Evan

Texte seulement, rien n'est codé. Pour chacune : nom, effet voulu, type de Nen. Avant de coder une proposition
acceptée, vérifier qu'elle ne fait pas doublon avec une technique déjà listée plus haut.
Les doublons éventuels avec les kits existants (docs/hatsu-et-techniques.md) seront vérifiés avant de coder celles qui sont acceptées.

### Revue par rapport au jeu (20 nov)

Chaque proposition ci-dessous comparée aux techniques déjà en jeu (parties 1 à 7). **Nouveau** = rien d'équivalent,
à coder tel quel ; **Variante** = amélioration d'une technique existante (on la modifie, on n'ajoute pas) ;
**Doublon** = déjà en jeu, à abandonner ou à fusionner. Rien n'est codé : Evan tranche la colonne « Suite ».

| Perso | Proposition | Verdict | En jeu aujourd'hui | Suite proposée |
|---|---|---|---|---|
| Gon | Pierre en rafale | Nouveau | Pierre, Ciseaux, Papier, Pierre sans retenue | Coder |
| Gon | Rage de Gon (adulte) | Nouveau | — | Coder (idéal pour le niveau 80+) |
| Gon | Ren du chasseur (passif) | Nouveau | Passifs de personnage existants | Coder |
| Killua | Foudre fatale | Variante | Foudre (paralysie presque totale) | Fusionner dans Foudre (portée courte, étourdissement) |
| Killua | Godspeed enchaîné | Variante | Godspeed (vitesse III, 10 s) | Ajouter la règle de recharge à Godspeed |
| Killua | Yo-yo d'aura | Nouveau | — | Coder |
| Kurapika | Chaîne scellante | Nouveau | Prison de chaînes, Chaîne sacrée | Coder |
| Kurapika | Détection de la chaîne de l'index | Nouveau | — | Coder |
| Kurapika | Empereur du Temps | Doublon | Colère écarlate (coup secret des Kurta, 6 types à 100 %) | Abandonner ou la renommer |
| Leorio | Poing à distance chargé | Variante | Coup à distance (12 blocs) | Ajouter la charge au Coup à distance |
| Leorio | Sérum d'urgence | Nouveau | Aucun soin sur allié pour Leorio | Coder |
| Leorio | Diagnostic | Variante | Gyo 40 et En 30 montrent déjà la vie des cibles | Ajouter seulement le type de Nen |
| Hisoka | Trampoline | Nouveau | Bungee Gum (lien élastique) | Coder |
| Hisoka | Leurre | Variante | Texture surprise (invisible 8 s) | Ajouter le faux Hisoka à Texture surprise |
| Hisoka | Cartes en éventail | Variante | Cartes renforcées (une carte) | Mode éventail de Cartes renforcées |
| Chrollo | Skill Hunter temporaire | Doublon | Chasseur de compétences (vole la dernière technique) ; Ging a Copie | Abandonner |
| Chrollo | Livre ouvert | Nouveau | — | Coder |
| Netero | Souffle de Kannon | Nouveau | Poings de Kannon, Bodhisattva, Main zéro | Coder |
| Netero | Paumes de feu mille fois | Variante | Bodhisattva aux mille mains | Fusionner avec le Bodhisattva |
| Meruem | Bouclier de membrane | Nouveau | Rafale de rage, Photon | Coder |
| Meruem | Cri royal | Nouveau | — | Coder |
| Feitan | Cage de Pain Packer | Nouveau | Soleil levant | Coder |
| Feitan | Double soleil | Variante | Soleil levant | Option du Soleil levant |
| Illumi | Aiguille de contrôle | Nouveau | Aiguille (lenteur + nausée), Aiguille de métamorphose | Coder |
| Illumi | Visage d'emprunt | Nouveau | — | Coder |
| Biscuit | Pierre massue | Nouveau | Changer de forme, Cookie | Coder |
| Biscuit | Sérénité de Cookie | Doublon | Cookie, la masseuse (soin de zone, régénération) | Abandonner |
| Pitou | Poupée de vie | Doublon | Doctor Blythe maintenu (poupée qui soigne) | Abandonner |
| Pitou | Aura dormante | Variante | Zetsu et In (même maîtrise) | Abandonner ou lier au Zetsu |
| Kite | Lance de chasse | Nouveau | Crazy Slot | Coder |
| Kite | Œil d'aura | Doublon | En et Gyo | Abandonner |
| Zeno / Silva | Dragon Dive | Doublon | Plongée du dragon (déjà là, niveau 30+) | Abandonner (la version en ligne pourrait être un mode) |
| Zeno / Silva | Poids des ancêtres | Nouveau | Tête de dragon, Orbes explosifs | Coder |

Bilan : 18 nouveaux, 9 variantes, 6 doublons (Empereur du Temps, Skill Hunter temporaire, Sérénité de Cookie, Poupée de vie, Œil d'aura, Dragon Dive).
Les montants d'aura écrits dans les propositions sont en unités du jeu telles qu'affichées (×10 depuis le 20 nov) : à ajuster au moment de coder.

### Gon (Renforcement)
1. **Jajanken : Pierre en rafale** — trois petites boules enchaînées sur 1 s, chacune à 40 % des dégâts : bonne contre plusieurs cibles, mauvaise contre un mur.
2. **Rage de Gon (adulte)** — transformation courte (8 s) : Force et Vitesse ×2, aura rouge sombre, ne peut plus se soigner ; épuisement ensuite (Lenteur 10 s).
3. **Ren du chasseur** — passif : après 5 s sans être touché, le prochain coup de poing fait +25 % de dégâts.

### Killua (Transmutation)
1. **Foudre fatale (Narukami)** — coup électrique à courte portée : étourdit 1,5 s la cible touchée, consomme 40 d'aura.
2. **Godspeed enchaîné** — après un Godspeed réussi, la recharge est coupée de moitié si la cible est touchée dans la seconde.
3. **Yo-yo d'aura** — lance un yo-yo qui revient : dégâts à l'aller et au retour, ricoche une fois sur un mur.

### Kurapika (Matérialisation)
1. **Chaîne du jugement scellante** — la cible est marquée : elle ne peut plus utiliser son Hatsu pendant 10 s ; si Kurapika est touché pendant ce temps, la marque tombe.
2. **Chaîne de l'index : détection** — la chaîne pend, puis s'oriente vers l'ennemi de Nen le plus proche dans 40 blocs.
3. **Empereur du Temps (yeux écarlates)** — les 6 types à 100 % pendant 20 s, puis perte de vie progressive (déjà en partie couvert par la Colère écarlate : à fusionner ?).

### Leorio (Émission)
1. **Poing à distance : tir chargé** — maintenir pour charger un poing d'aura (jusqu'à 3 s) : portée et dégâts croissants.
2. **Sérum d'urgence** — projette un soin sur un allié visé (portée 12 blocs) au lieu de soi.
3. **Diagnostic** — révèle PV et type de Nen de la cible visée pendant 20 s.

### Hisoka (Transformation)
1. **Bungee Gum : trampoline** — pose un fil collant au sol ; qui y marche est projeté en l'air.
2. **Texture Surprise : leurre** — invisible 3 s, laisse un faux Hisoka qui attire les mobs.
3. **Cartes tranchantes en éventail** — gerbe de 5 cartes en cône (6 blocs).

### Chrollo (Spécialisation)
1. **Skill Hunter : copie temporaire** — vole un Hatsu à la cible touchée pendant 60 s, à 60 % de puissance ; la cible l'a toujours.
2. **Livre ouvert** — le livre flotte autour de lui et bloque le premier coup reçu toutes les 10 s.

### Netero (Renforcement)
1. **Souffle de Kannon** — onde sacrée devant lui : repousse tout le monde et soigne un peu les alliés (consomme de la Prière).
2. **Paumes de feu mille fois** — enchaînement de 12 frappes en 2 s sur une même cible (consomme de la Prière).

### Meruem (Émission)
1. **Bouclier de membrane** — enveloppe d'aura absorbant 20 PV pendant 6 s ; une seule à la fois.
2. **Cri royal** — onde de peur : étourdit et fait fuir les mobs à 8 blocs.

### Feitan (Transformation)
1. **Pain Packer : cage** — cercle de flammes noires qui retient une cible 3 s (elle peut le briser en frappant).
2. **Rising Sun : double** — deux soleils au lieu d'un, chacun à 60 %.

### Illumi (Manipulation)
1. **Aiguille de contrôle** — fiche une aiguille : pendant 4 s la cible avance seule vers Illumi.
2. **Visage d'emprunt** — change d'apparence (skin d'un joueur ou PNJ proche) pendant 60 s ; l'attaque la rompt.

### Biscuit (Transformation)
1. **Pierre massue** — en grande forme, un coup de poing au sol qui fait trembler la zone (étourdit 1 s).
2. **Sérénité de Cookie** — Cookie dépose une zone de repos : soin lent des alliés pendant 6 s.

### Pitou (Transformation)
1. **Docteur Blythe : poupée de vie** — la poupée suit son maître et le soigne légèrement tant qu'elle vit.
2. **Aura dormante** — Zetsu parfait de 3 s : invisible au Gyo des mobs, mais plus aucun soin.

### Kite (Matérialisation)
1. **Lance de chasse** — matérialise une lance lancée (portée 25 blocs) qui reste plantée et explose au rappel.
2. **Œil d'aura** — voit les mobs dans 30 blocs à travers les murs pendant 10 s.

### Zeno / Silva (Zoldyck)
1. **Dragon Dive** — frappe à distance : le dragon fonce en ligne droite (20 blocs) et traverse les cibles.
2. **Poids des ancêtres** — alourdit la cible visée : Lenteur II 8 s, saut impossible.

## 9. À trancher

- Seconds types des techniques (estimation de départ dans `NenAffinity`, colonne « Type » ci-dessus).
- Clans : part de l'XP de stat qui va au clan (1/5), +30 XP de clan par coup secret, bonus de stats du clan
  (xp_bonus × 50 au niveau 100, réglable par `clan_stats`) ; effets et poids du tirage de chaque famille.
- Aura sauvage par défaut (Hisoka, Illumi, Meruem, Neferpitou, Youpi, Feitan, Uvogin, Phinks, Genthru) : hypothèses
  tirées de l'histoire.
- Types non confirmés dans l'œuvre (hypothèses) : Pariston, Ging, Cheadle, Botobai, Menchi, Bushidora, Tsezguerra, Zushi.
