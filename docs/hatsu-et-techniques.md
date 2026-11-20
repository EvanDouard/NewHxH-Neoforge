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
*Généré depuis le code par `tools/docs/hatsu_doc.py` — 95 techniques enregistrées. Aura = coût d'aura au lancement ; recharge en secondes ; une technique ratée coûte quand même.*

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
|---|---|---|---|---------------|---|
| **Jajanken : Pierre** <br>*Jajanken: Rock* | Renforcement | 10 | 8 s | —             | Gon charge son aura dans le poing, immobile : une boule d'une demi-taille de joueur brille et change de couleur à chaque palier tant qu'on maintient la touche. Relâcher la lance tout droit : elle creuse sa route, emporte la cible et explose. Plus on charge, plus c'est destructeur. Le Ryu tourné vers l'attaque la rend plus grosse. |
| **Jajanken : Ciseaux** <br>*Jajanken: Scissors* | Transformation + Renforcement | 25 | 5 s | Nen 5         | L'aura prend la forme d'une lame qui file tout droit jusqu'à toucher un bloc ou un ennemi. |
| **Jajanken : Papier** <br>*Jajanken: Paper* | Émission + Renforcement | 30 | 6 s | Nen 10        | Gon ouvre la main et libère son aura d'un coup : un cône d'émission qui frappe tout ce qui est devant lui et le projette loin (catégorie Émission : 80 % pour Gon). |
| **Jajanken : Pierre sans retenue** ★ <br>*Jajanken: Rock, all out* | Renforcement | 40 | 20 s | Nen 60        | Gon met tout dans le poing : la boule enfle jusqu'à deux fois sa taille, puis se recondense, minuscule et aveuglante. Ceux qui la regardent sont éblouis. Coûte deux fois plus d'aura, frappe bien plus fort et creuse un cratère énorme. |

#### Killua Zoldyck — Transformation

*aura #7FD3FF, aura calme*  
Héritier des Zoldyck : il transforme son aura en électricité.

- **Passif — Corps Zoldyck** : Aura electrique, possède une nouvelle barre d'electricité, chaque coup et technique consomme un peu d'electricité, possède un taser et doit se recharger en se tazant de temps en temps

| Technique                    | Type                         | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                                       |
|------------------------------|------------------------------|------|----------|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Foudre** <br>*Thunderbolt* | Transformation + Émission    | 10   | 6 s      | — | Décharge électrique qui paralyse presque complètement la cible.                                                                                                                                                       |
| **Godspeed**                 | Transformation + Manipulation | 50   | 30 s     | Nen 10 | Son corps réagit par réflexe électrique : vitesse III le monde autour de Killua se ralentit (sauf lui) (50blocs environs, seul les autres godspeed ne sont pas affectué ou les cible qui ont une speed de 90 ou plus) |
| **Coup de yoyo**             | ?                            | ?    | ? s      | Nen 10 | ?                                                                                                                                                                                                                     |
| **Coup de poing electrique** | Transformation + Émission  | ?    | ? s      | Nen 10 | Grosse quantité d'electricité dans la main qui est donné au prochain coup, paralisie temporaire disponible uniquement en godspeed                                                                                     |

#### Kurapika — Matérialisation

*aura #9AD7FF, aura calme*  
Dernier survivant du clan Kurta : chaînes matérialisées.

- **Passif — Chaîne radiesthésique** : Les monstres à moins de 12 blocs brillent et se voient à travers les murs.
- **Serment — Chaîne-prison : la Brigade seulement** : Comme Kurapika : la Chaîne-prison ×3, mais seulement contre la Brigade Fantôme (joueurs de la Brigade, mobs avec le personnage d'un de ses membres). La lancer sur quelqu'un d'autre : le coup ne porte pas, le serment est rompu et tu meurs.

| Technique                              | Type                           | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                                                             |
|----------------------------------------|--------------------------------|------|----------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Prison de chaînes** <br>*Chain Jail* | Matérialisation + Manipulation | 45   | 20 s     | Nen 5         | La chaîne immobilise la cible (lenteur VII) et l'affaiblit pendant 5 s.                                                                                                                                                                     |
| **Chaîne sacrée** <br>*Holy Chain*     | Matérialisation + Renforcement | 35   | 15 s     | —             | La chaîne du pouce soigne 8 PV.                                                                                                                                                                                                             |
| ** Steal chain**                       | ?                              | ?    | ? s      | ?             | Aspirer l'aura et voler un Hatsu (temporairement avec un drawback dès la fin de l'utilisation de la chaine) j'estimerais 2min de vol, pour avoir cette technique il faut pouvoir être en empror time sinon la technique dure simplement 10s |
| ** Jugement chain**                    | ?                              | ?    | ? s      | ?             | Impose une règle mortelles à l'adversaire (seulement psosible quand la cible à moins de 10-20% de pv)                                                                                                                                       |
| ** Lancer de chaine**                  | ?                              | ?    | ? s      | ?             | Kurapika lance une chaine sur l'adversaire (fouette)                                                                                                                                                                                        |
#### Neferpitou — Spécialisation

*aura #7A2E8C, aura sauvage (flammes même au repos)*  
Garde royal du roi des fourmis-chimères, spécialiste félin.

- **Passif — Garde royal ** : +20% d'aura max, +20 en vitesse dès la selection du Hatsu, régénration d'aura augmenté, taille max de En augmenté, possède un Nen post mortem lors de sa mort Terpsichora reprend le contrôle du corp de Pitou et elle doit être une nouvelle fois être battu (encore plus redoutable +10-20% dans toute les stats au moment de la mort) (aucun Hatsu spécial en plus ne peux être utilisé par Pitou son corp inanimé se combat)

| Technique               | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                      |
|-------------------------|---|---|---|---|------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Terpsichora**         | Spécialisation + Manipulation | 60 | 30 s | Nen 10 | Le corps manipulé comme une marionnette : force II, vitesse II et saut II pendant 10 s.                                                              |
| **Doctor Blythe**       | Spécialisation + Matérialisation | 20 | 20 s | — | La poupée-chirurgienne apparaît au-dessus du blessé devant toi (ou de toi-même) et le recoud : 16 PV en 4 s. Tu restes immobile pendant l'opération. |
| **Bond/attaque éclair** |  |  |  s | — |                                                                                                                                                      |
| **Griffure**            |  |  |  s | — |                                                                                                                                                      |
| **Puppeteering**            |  |  |  s | — | Manipulation de marionnettes, peut manipuler un cadavre qu'il vient de tuer                                                                          |

#### Illumi Zoldyck — Manipulation

*aura #4A3570, aura sauvage (flammes même au repos)*  
Aîné des Zoldyck : aiguilles qui manipulent les corps.

- **Passif — Entraînement Zoldyck** : Immunisé au poison.

| Technique                                                | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                                                                                            |
|----------------------------------------------------------|---|------|----------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Aiguille** <br>*Needle*                                | Manipulation | 25   | 5 s      | —             | Une aiguille qui dérègle le corps : lenteur III et nausée pendant 2 s.                                                                                                                                                                                                     |
| **Aiguille de métamorphose** <br>*Transformation Needle* | Manipulation + Transformation | 50   | 30 s     | Nen 10        | Se plante des aiguilles pour remodeler son corps : résistance et force pendant 15 s, peut choisir l'apparence d'une autre personne (ou mob, il gagne aussi donc les "clan" et la passivité que certain ennemie peuvent avoir avec les membres de leurs clan/espèce/autre). |
| **Les hommes-aiguilles*                    | Manipulation | ?    | ? s      | Nen ?         | Illumi plante une aiguille chargée d'une grande quantité de Nen dans le crâne d'une personne vivante pour en faire un serviteur entièrement contrôlé. (Ne fonctionne pas sur les personne ayant un Willpower superieur ou égal à Illumi)                                   |
| **Needle Barrage*                                        | Manipulation | ?    | ? s      | Nen ?         |   |

#### Leorio Paladiknight — Émission

*aura #F2A33A, aura calme*  
Futur médecin : il fait voyager ses coups de poing à distance.

- **Passif — Médecin** : Récupère 1 PV toutes les 5 secondes.

| Technique | Type        | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|-------------|------|----------|---------------|---|
| **Coup à distance** <br>*Remote Punch* | Émission    | 30   | 6 s      | —             | Le poing frappe le sol et ressort sous la cible, jusqu'à 12 blocs. |
| **Onde de choc** <br>*Shockwave* | Émission    | 40   | 10 s     | Nen 5         | Frappe le sol : onde qui blesse et repousse tout autour. |
| **Détection par ultrasons* | Émission    | 40   | 10 s     | Nen 5         | De détecter des personnes derrière un mur. |
| **Percussion interne : Frappe ciblée* | Émission + Manipulation | ?    | ? s      | Nen ?         | Une frappe de Nen atteint une zone précise du corps de l'adversaire. Elle pourrait infliger un effet ciblé : ralentissement, désorientation ou interruption d'une action. |

#### Isaac Netero — Renforcement

*aura #FFD966, aura calme*  
Président de l'Association, maître absolu du Renforcement. Sa force vient de la Prière.

- **Passif — Prière** : Une jauge de Prière propre à Netero. « Méditer » devient « Prière » : immobile, mains jointes, la jauge se remplit (pleine en 20 s). Chaque technique de Kannon en consomme une partie.

| Technique                                                              | Type                                   | Aura           | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                                                                                                                                                                                                                                                   |
|------------------------------------------------------------------------|----------------------------------------|----------------|----------|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Bodhisattva aux mille mains** <br>*Hundred-Type Guanyin Bodhisattva* | Renforcement + Émission                | 30 + 10 Prière | 10 s     | Nen 10        | L'immense statue de Kannon surgit derrière Netero et frappe d'une paume géante tout ce qui est devant (12 blocs). Coûte 30 de Prière.                                                                                                                                                                                                                                                                                             |
| **Prière et attaque**                                                  | Émission + Manipulation + Renforcement | 30 + 0 Prière  | 10 s     | Nen 70        | Attaque haut niveau de netero, Netero dans cet ordre pris, puise attaque la statut attaquera devant netero avec une de ces main (random) cela doit ce faire extrèement rapidement (animation prière +attaque), si l'ennemie en face est très rapide (>=90) alors netero ralentit le temps autour de lui et l'attaque ce fait beaucoup plus lenttement on voit la statue apparaitre (netero ne bouge pas pendant ce laps de temps) |
| **Hyakushiki Kannon : Première main**                                  | Émission + Manipulation + Renforcement | 30 + 3 Prière  | ? s      | Nen 30        | Fait apparaître une main géante qui frappe instantanément la cible. Dégâts importants et faible temps de recharge.                                                                                                                                                                                                                                                                                                                |
| **Hyakushiki Kannon : Troisième main**                                 | ?                                      | +50 Prière     | ? s      | Nen 30        | Donne +50 de prière                                                                                                                                                                                                                                                                                                                                                                                                               |
| **Hyakushiki Kannon : 99e main**                                       | ?                                      | +50 Prière     | ? s      | Nen 30        | Deux mains apparaissent autour de la cible pour l'écraser et l'immobiliser brièvement.                                                                                                                                                                                                                                                                                                                                            |
| **Prière rapide**                                                      | ?                                      | +50 Prière     | ? s      | Nen 30        | Donne +50 de prière et recharge les autres technique                                                                                                                                                                                                                                                                                                                                                                              |
| **Main zéro** ★ <br>*Zero Hand* | Renforcement + Émission | 60 + 100 Prière | 60 s | Nen 40 | Prière pleine : toute l'aura est dépensé. Un rayon d'aura pure jaillit de la statue détruit les blocs autours (24 blocs, dégâts en fonction de l'aura dépensé).                                                                                                                                                                                                                                                                   |

#### Hisoka Morow — Transformation

*aura #FF4FA3, aura sauvage (flammes même au repos)*  
Magicien imprévisible, amoureux des combats.

- **Passif — Frisson du combat** : Plus l'adversaire est robuste (vie max), plus tes coups font mal : +1 % par PV au-delà de 20, jusqu'à +30 %.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                                                                                                                         |
|---|---|---|---|---|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Bungee Gum** | Transformation | 20 | 1,5 s | — | Colle une aura élastique sur la cible devant toi. Tant que le lien tient (10 s), elle est ramenée vers toi dès qu'elle s'éloigne. Relance la technique : l'élastique se contracte d'un coup et la projette contre toi.                                                                                  |
| **Cartes renforcées** <br>*Reinforced cards* | Transformation + Émission | 10 | 1,5 s | — | Une carte à jouer renforcée par le Nen, lancée comme une lame. Elle tranche tout ce qu'elle touche.                                                                                                                                                                                                     |
| **Texture surprise** <br>*Texture Surprise* | Transformation | 40 | 25 s | Nen 10 | Tu recouvres ton corps d'une fausse texture : tu disparais 8 s dans une gerbe de cartes, et les monstres te perdent de vue.                                                                                                                                                                             |
| *Poing élastique* | Transformation | 40 | 25 s | Nen 10 |    Il attache son aura à son poing ou à une autre partie de son corps, l'étire puis la rétracte pour accélérer son mouvement ou modifier sa trajectoire.                                                                                                                                                                                                                                                                                                    |
| *Piège élastique* | Transformation | 40 | 25 s | Nen 10 | Il installe des fils d'aura sur le terrain ou sur un adversaire. Il peut ensuite immobiliser, déséquilibrer ou attirer sa cible.                                                                                                                                                                                                                                                                                                        |
| *Renvoi de projectile* | Transformation | 40 | 25 s | Nen 10 | Il utilise l'élasticité et l'adhérence de son aura pour arrêter ou retenir un projectile, puis le renvoyer en utilisant la tension accumulée. (je verrais un genre de "bouclier" d'aura devant lui qui renverrais les projectiles "faibles" je ne le vois pas renvoyer un truc comme un jajanken ultime |

### Association Hunter et examinateurs

#### Pariston Hill — Spécialisation (très peu d'information dans le manga)

*aura #FF9FC8, aura calme*  
Vice-président de l'Association, manipulateur souriant.

- **Passif — Charisme** : Un monstre qui te prend pour cible a une chance sur trois de t'oublier (toutes les 3 s). Un débiteur (Dette d'amour) se blesse de la moitié des coups qu'il te porte.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Dette d'amour** <br>*Love Debt* | Spécialisation | 25 | 15 s | — | Marque une cible (10 blocs) pendant 10 s : la moitié des coups qu'elle te porte lui revient. Un cœur rose la désigne. |

#### Ging Freecss — Émission

*aura #E4E4E4, aura calme*  
Le père de Gon, Hunter de légende et archéologue.

- **Passif — Archéologue** : permet d'imiter les techniques de frappe qu'il a déjà reçues. (attaque physique basés sur le Nen, technique simple pas de technique complexe qui peuvent être copié, ces technique copié auront un petit malus par rapport à l'original)

| Technique                                                                                                                                                                                                                 | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---|---|---|---|---|
| Aucune technique de base, tout sera des technique copié de manière permanente dans son kit il lui faudra peut-être un menu pour retirer les techniques dont-il ne souhaite pas (ou pouvoir les mettre dans une blacklist) |

#### Cheadle Yorkshire — Manipulation (très peu d'information dans le manga)

*aura #9BC4FF, aura calme*  
Secrétaire puis présidente de l'Association.

- **Passif — Secrétaire** : Recharges des techniques −10 %.

| Technique                   | Type         | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                 |
|-----------------------------|--------------|------|----------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Protocole de quarantaine* | ?            | ?    | ? s      | Nen ?         | Permet de créer une zone ou la cible est enfermé avec Cheadle, elle ne peut pas se téléporter ailleurs ni casser facilement les parois de Nen du "domaine" Cheadle dans son bloc est plus forte |
| **Soins augmenté*           | ?            | ?    | ? s      | Nen ?         | Permet de ce soigner et de se redonner un peu d'aura                                                                                                                                            |
| Projection d'objet          | ? | ?    | ? s      | Nen ?         | Cheadle envoie sur l'ennemie des outils de Nen de chirugie                                                                                                                                      |

#### Botobai Gigante — Renforcement (très peu d'information dans le manga)

*aura #B8B8B8, aura calme*  
Membre du Zodiaque, vétéran imposant.

- **Passif — Vétéran** : Résiste au recul ; bonus de résistance et de puissance lorsque ses points de vie sont élevés.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Pression** <br>*Pressure* | Renforcement | 30 | 12 s | — | Une onde de 6 blocs écrase les ennemis : lenteur et faiblesse. |
Souffle draconique : projection d'une vague d'aura.
Écailles du dragon : renforce temporairement sa défense.
Morsure du dragon : attaque frontale très puissante.

#### Biscuit Krueger — Transformation

*aura #FFB6E1, aura calme*  
Maîtresse du Shingen-ryu, experte en pierres précieuses.

- **Passif — Deux formes** : Biscuit alterne entre deux formes : la grande (2,5 blocs, très musclée) frappe ×1,5 mais est plus lente (lègère slowness) et l'aura remonte moins vite ; la petite (1,5 bloc) frappe ×0,8 mais son aura remonte plus vite. Peut enseigner les bases du Nen à quelqu'un

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                              |
|---|---|---|---|---|------------------------------------------------------------------------------------------------------------------------------|
| **Changer de forme** <br>*Switch Form* | Transformation | 0 | 2 s | — | Passe de la petite à la grande forme, ou l'inverse.                                                                          |
| **Cookie, la masseuse** <br>*Cookie, the Masseuse* | Transformation + Matérialisation | 30 | 20 s | — | Cookie soigne Biscuit et ses alliés à 3 blocs, régénération et un peu de satiété donne un petit bonus de dégats aux alliées. |
Magical Esthetician Permet de récupéré totalement tout, vie, faim, fatigue, aura vers la cible ou elle même
Gros coup de poing (consomme pas de mana mais est seulement accécible en forme musclé)
Plusieurs attaques a distances en forme petit (seulement accécible en forme petit) par contre elle consomme de l'aura

#### Menchi — Transformation (très peu d'information dans le manga)

*aura #FF8AA8, aura calme*  
Hunter gastronome, examinatrice de l'Examen Hunter.

- **Passif — Gastronome** : La nourriture te rassasie deux fois plus.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Repas de gourmet** <br>*Gourmet Meal* | Transformation | 30 | 30 s | — | Un plat partagé avec les alliés à 6 blocs : soin, régénération, Force. |

#### Bushidora Ambitious — Renforcement (très peu d'information dans le manga)

*aura #8FBF6F, aura calme*  
Membre du Zodiaque, sabreur.

- **Passif — Bretteur** : +20 % de dégâts à l'épée.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Lame d'aura** <br>*Aura Blade* | Renforcement + Transformation | 25 | 6 s | — | La lame s'allonge et tranche en arc tout ce qui est devant (6 blocs). |

#### Tsezguerra — Renforcement

*aura #E0A040, aura calme*  
Hunter chasseur de primes, chef d'équipe à Greed Island.

- **Passif — Meneur** : Permet a Tsezguerra de faire des doubles saut (plusieurs en fonction de son niveau de willpower max : 5)

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Frappe chargée** <br>*Charged Strike* | Renforcement | 25 | 8 s | — | Un coup de poing très puissant au corps à corps. |
Défense instantanée - Concentre automatiquement une grande quantité d'aura sur la partie du corps qui va recevoir une attaque. Réduit fortement les dégâts si le timing est bon.
Réflexes du vétéran Augmente temporairement la vitesse de réaction et permet de redistribuer rapidement son aura vers une zone menacée (=plus de défense) effet qui s'active pendant une certaine durée.
### Famille Zoldyck

#### Silva Zoldyck — Émission

*aura #8E6CFF, aura calme*  
Chef de la famille d'assassins Zoldyck.

- **Passif — Corps Zoldyck** : Immunisé à la foudre et au poison, dégats bonus sur une cible isolé.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|------|----------|---|---|
| **Grande boule** <br>*Great Orb* | Émission + Transformation | 10   | 8 s      | — | Silva forme en une seconde une grosse boule d'aura et la propulse : elle emporte la cible et explose,  creuse le terrain. |
| **Orbes explosifs** <br>*Explosive Orbs* | Émission + Transformation | 35   | 10 s     | — | Trois sphères d'aura violettes, l'une après l'autre, vers la cible (16 blocs). |
| **Frappe du patriarche** * | Renforcement | ?    | ? s      | — | Concentre son aura dans son poing pour effectuer une frappe extrêmement puissante, capable de projeter les ennemis et de détruire certains blocs. |

#### Zeno Zoldyck — Émission

*aura #FFC34D, aura calme*  
Grand-père de Killua, assassin légendaire.

- **Passif — Corps Zoldyck** : Immunisé à la foudre et au poison.

| Technique | Type                      | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                             |
|---|---------------------------|------|----------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Tête de dragon** <br>*Dragon Head* | Émission + Transformation | 30   | 8 s      | —             | Invoque un dragon d'aura devant le joueur. Il peut mordre les ennemis, les repousser ou les immobiliser temporairement.                                                                     |
| **Dragon Lance – Charge du dragon*** | ?                         | ?    | ? s      | Nen ?         | Envoie le dragon vers une cible. Le joueur peut diriger sa trajectoire pour poursuivre un ennemi et le capturer avec ses mâchoires.                                                         |
| ***Dragon Dive – Pluie d'étoiles** | ?                         | ?    | ? s      | Nen ?         | Fait apparaître un immense dragon au-dessus d'une zone, qui se fragmente en centaines de projectiles d'aura. Très gros dégâts de zone (creuse les blocs) (ils decendent très profondément). |
| ***Dragon Rider – Monture du dragon** | ?                         | ?    | ? s      | Nen ?         | Crée un dragon suffisamment grand pour transporter le joueur et éventuellement des alliés. Permet de voler et de se déplacer rapidement. |
Aura Blast – Décharge | Projette une puissante vague d'aura depuis la paume, repoussant les ennemis et détruisant les blocs fragiles.
#### Kalluto Zoldyck — Manipulation

*aura #D9C9FF, aura calme*  
Le plus jeune Zoldyck, danse des papiers.

- **Passif — Corps Zoldyck** : Immunisé à la foudre et au poison.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Danse des papiers** <br>*Paper Dance* | Manipulation | 35 | 15 s | — | Un tourbillon de papiers coupants balaie l'avant pendant 2 s. |
Dance of the Serpent's Bite Déploie des confettis autour d'un ennemi. Lorsqu'un morceau le touche, il devient une marque. Les autres confettis convergent alors vers cette marque pour infliger des dégâts répétés.
Surveillance Paper Dolls - Place discrètement une marque de papier sur un joueur ou un PNJ. Permet de suivre sa position et d'entendre les conversations à proximité (voir qui l'entour).
Paper Storm - Projette une multitude de confettis dans une zone. Les papiers gênent la vision, touchent plusieurs ennemis et peuvent préparer Dance of the Serpent's Bite.
Paper Cocoon- Enveloppe un adversaire dans une masse de papier pour ralentir ses déplacements et réduire sa capacité à attaquer.
Razor Fan - Recouvre son éventail de Nen avec Shu pour effectuer des attaques au corps à corps capables de couper les ennemis.
### Brigade Fantôme

#### Chrollo Lucilfer — Spécialisation

*aura #5B4B8A, aura calme*  
Chef de la Brigade fantôme.

- **Passif — Tête** : La tête de l'araignée : +5 % de dégâts par membre de la Brigade à 16 blocs (40 % au plus).

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                          |
|---|---|---|---|---|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Chasseur de compétences** <br>*Skill Hunter* | Spécialisation + Matérialisation | 30 | 10 s | Nen 20 | Ajoute de manière permanente une technique si la compétences est déclenché dans la minute après la mort de la cible, comme Ging il peut gérer ce qu'il veut garder ou non dans le menu O |
Indoor Fish : poissons squelettiques qui dévorent les ennemis dans une pièce fermée.
Fun Fun Cloth : capture et réduction de taille des ennemis ou objets.
Order Stamp : création et contrôle de marionnettes.
Convert Hands : changement d'apparence et échange d'identité visuelle.
The Sun and Moon : marquage explosif de deux cibles.
✍Lovely Ghostwriter : prédictions sur le futur.
Téléportation : déplacement instantané.
Love Dial 6700 : recherche d'une capacité correspondant à des critères.

#### Feitan Portor — Transformation

*aura #FF4500, aura sauvage (flammes même au repos)*  
Membre de la Brigade, tortionnaire.

- **Passif — Douleur** : Chaque PV perdu remplit la jauge de Douleur ; elle redescend hors combat. Jusqu'à +50 % de dégâts quand elle est pleine.
- **Serment — Soleil levant : seulement blessé** : Comme Feitan : le Soleil levant ×1,8, mais il ne se déclenche que sous la moitié de ta vie (refusé sinon). Le serment ne se rompt pas.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                   |
|---|---|---|---|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Soleil levant** <br>*Rising Sun* | Transformation + Matérialisation | 40 + 100 Douleur | 30 s | Nen 20 | Déclenche un soleil miniature au-dessus du joueur. Inflige des dégâts thermiques massifs dans un rayon important. Sa puissance dépend des dégâts reçus depuis le début du combat. |
Pain Packer – Armure Fait apparaître une armure de Nen qui protège le joueur contre les effets de sa propre attaque Rising Sun.
Heat Wave – Vague thermique - Variante inventée : libère une vague de chaleur autour du joueur, repoussant et brûlant les ennemis proches.
Solar Burst – Explosion solaire Variante inventée : concentre la chaleur dans une petite sphère avant de provoquer une explosion thermique extrêmement puissante.
Phantom Step – Pas fantôme - Variante inventée : effectue une accélération brutale dans une direction, laissant une image rémanente derrière lui.
#### Nobunaga Hazama — Renforcement

*aura #E8E8FF, aura calme*  
Membre de la Brigade, samouraï.

- **Passif — En** : La technique permet d'utiliser le En, le En ne peut pas aller plus loins que 4 blocs, par contre la taille supplémentaire du En est convertit en dégats pour l'attaque Iai

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                    |
|---|---|---|---|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Iaijutsu – Dégainage mortel** | Renforcement | 25 | 7 s | — | Le joueur rengaine son katana et entre en posture et déploie son En. Si un ennemi entre dans sa portée, il effectue automatiquement une frappe extrêmement rapide. |
Coupe instantanée - Effectue un dégainage rapide dans une direction. Inflige de lourds dégâts à la première cible rencontrée.
Posture du samouraï - Le joueur adopte une posture défensive, concentre son aura et améliore sa résistance. Une attaque au corps à corps déclenche une contre-attaque.
Pas du sabreur - Effectue un déplacement extrêmement rapide vers une cible proche, suivi d'une attaque au katana.
#### Phinks Magcub — Renforcement

*aura #D9A441, aura sauvage (flammes même au repos)*  
Membre de la Brigade, bagarreur.

- **Passif — Bagarreur** : Bonus de dégâts à mains nues. Meilleure résistance au recul.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                                                                                                                                                     |
|---|---|---|---|---|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Ripper Cyclotron** | Renforcement | 4 | 0,3 s | — | Maintenir la touche pour faire tourner le bras. Chaque rotation augmente la puissance du prochain coup, chaque rotation consomme de l'aura, si le joueur consomme toute son aura alors le coup est annulé, l'aura consommé augmente à chaque tour (demandant ainsi au joueur de jauger comme phinks combien de tour il peut faire) Afficher un compteur de rotations. |
Coup dévastateur - Libère toute la puissance accumulée. Dégâts proportionnels au nombre de rotations. Repousse violemment les ennemis. Peut briser certains blocs avec suffisamment de charge. Un coup chargé au maximum peut provoquer une très grosse onde de choc.
#### Uvogin — Renforcement

*aura #FF6A3D, aura sauvage (flammes même au repos)*  
Membre de la Brigade, force brute pure.

- **Passif — Corps d'acier** : Dégâts physiques reçus −25 %.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Big Bang Impact** | Renforcement | 30 | 20 s | Nen 20 | Maintenir la touche pour charger le poing.Consommer de l'aura progressivement.Plus la charge est élevée, plus les dégâts augmentent.Créer un cratère à l'impact.Détruire certains blocs selon la puissance.Infliger des dégâts de zone autour du point d'impact. |
. Cri destructeur - Cri puissant dans un cône devant le joueur.Dégâts aux ennemis proches. Repoussement important. Peut interrompre les attaques adverses. Temps de recharge assez long.
Force monstrueuse -  +25 en force pendant X temps augmente les dégats à mains nue 
Projeter - Tien un ennemie en main (il ne peut plus lancer quoi que ce soit) puis le projète 
#### Franklin Bordeau — Émission

*aura #D0703A, aura calme*  
Membre de la Brigade, colosse.

- **Passif — Robuste** : Tous les dégâts reçus −15 %.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                    |
|---|---|---|---|---|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Double mitrailleuse** <br>*Double Machine Gun* | Émission | 40 | 15 s | — | Tire des balles d'aura depuis les dix doigts. Maintenir la touche pour tirer en continu. Consommation d'aura par projectile.Cadence de tir améliorable (en fonction du willpower). |
Tir concentré - Concentre les tirs sur le curseur. Réduit la dispersion. Augmente les dégâts sur une cible. Consomme davantage d'aura.
Déluge de balles-Déclenche une rafale très rapide. Augmente temporairement la cadence de tir. Permet de couvrir une zone entière. Très forte consommation d'aura. Temps de recharge important.(peut casser des blocs à niveau moyen de willpower (50))
Tir perforant - Tire un projectile plus concentré. Traverse plusieurs ennemis alignés. Peut traverser certaines protections. Coût d'aura élevé. (peut casser des blocs à niveau moyen de willpower (50))
#### Shalnark — Manipulation

*aura #7FE0C0, aura calme*  
Membre de la Brigade, manipulateur par antenne.

- **Passif — Technicien** : Un mob vaincu lâche parfois un objet de plus (une chance sur quatre).

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Voix noire** <br>*Black Voice* | Manipulation | 35 | 20 s | Nen 10 | Une antenne plantée dans un mob (8 blocs) : pendant 30 s il attaque tes ennemis. Échoue si la cible a plus de 10 niveaux de Nen de plus que toi. |
| **Pilote automatique** ★ <br>*Autopilot* | Manipulation | 45 | 45 s | Nen 30 | Il se contrôle lui-même : 15 s surpuissant, puis 10 s épuisé. |

#### Machi Komacine — Transformation

*aura #FF7FD0, aura calme*  
Membre de la Brigade, maîtresse des fils.

- **Passif — Suture** : Sens du fil - Détecte les vibrations dans ses fils. Permet de connaître la position des ennemis reliés à ses fils. Améliore les réflexes lorsque quelqu'un touche sa toile, possède un petite avantage quand quelqu'un se fait piéger dans sa toile 
- 
| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
Nen Threads – Fils de Nen - Crée des fils d'aura depuis les doigts.Permet de les allonger et de les raccourcir.Plus le fil est court, plus il est résistant.Peut maintenir plusieurs fils simultanément. Les fils perdent de leur résistance lorsqu'ils sont lâchés.
Marionnettiste - Attache des fils aux ennemis. Immobilise leurs membres. Peut attirer les ennemis vers elle. Peut ralentir ou perturber leurs déplacements. Possibilité de maintenir plusieurs cibles.
Toile invisible - Installe des fils dans une zone.Détecte les ennemis qui les touchent. Immobilise les cibles qui traversent la toile. Peut déclencher une attaque lorsque le piège est activé.Utilisation d'In pour rendre les fils invisibles.
Fil étrangleur- Enroule un fil autour de l'ennemi.Inflige des dégâts continus.Peut étrangler une cible immobilisée.Dégâts augmentés lorsque le fil est raccourci.
Nen Stitches – Suture de Nen - - Soigne les blessures. Permet de recoudre les membres sectionnés.Peut réparer les blessures graves.Demande au patient de rester immobile.Soins plus efficaces si le patient utilise Zetsu.
#### Shizuku Murasaki — Matérialisation

*aura #B6A8FF, aura calme*  
Membre de la Brigade, distraite.

- **Passif — Ménage** : Attire les objets au sol à 5 blocs.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                      |
|---|---|---|---|---|--------------------------------------------------------------------------------------------------------------------------------------|
| **Blinky** | Matérialisation | 30 | 12 s | — | Matérialise son aspirateur. Peut le faire disparaître à volonté. Peut le tenir comme une arme. Peut aspirer les objets non vivants. Capacité de stockage pratiquement illimitée. |
Aspiration massive - Aspire les objets dans un rayon défini.Nettoie les blocs et les objets au sol.Aspire les cadavres et les ressources.Peut aspirer les projectiles physiques.
Extraction sanguine - Aspire le sang des ennemis blessés. Inflige des dégâts continus. Plus la cible saigne, plus l'aspiration est efficace.
Frappe de Blinky - Utilise l'aspirateur comme une massue. Inflige des dégâts physiques. Peut repousser les adversaires. Peut provoquer des blessures ouvertes.
Extraction médicale - Retire le poison du joueur.Élimine certains effets négatifs.Retire les gaz ou matières étrangères.Peut être utilisée sur un allié.
Restitution - Recrache le dernier objet aspiré.Peut servir à récupérer un objet (sneak clic + utilisation de la technqiue) sinon utilisation classique = peut également projeter un objet comme projectile.


Ne peut pas aspirer les êtres vivants ni les constructions de Nen.
#### Pakunoda — Spécialisation

*aura #9F8FFF, aura calme*  
Membre de la Brigade, lit les souvenirs.

- **Passif — Liseuse de pensées** : Un coup au corps à corps révèle la vie, le Nen et l'aura de la cible.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                                                                    |
|---|---|---|---|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Bombe de mémoire** <br>*Memory Bomb* | Spécialisation | 30 | 12 s | — | Une balle d'aura (16 blocs) : la cible est désorientée, ralentie, aveuglée un instant. (6balle disponible avec un cooldown de 0) puis rechargement de la capacité, ou rechargement si pas utilisé pendant plus de 60s (et qu'il manque des balles) |
Lecture mémorielle - Identifier les capacités Nen déjà utilisées par la cible. Détecter certaines informations cachées dans les souvenirs.

### York Shin et Greed Island

#### Neon Nostrade — Spécialisation (pas trop de possibilité)

*aura #FFB0E0, aura calme*  
Fille de chef mafieux, voyante.

- **Passif — Divination** : Une petite chance en permanence (Chance I).

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Lovely Ghostwriter** | Spécialisation | 20 | 30 s | — | Un poème annonce les dangers à 32 blocs (nombre, direction) et les fait briller 10 s. |

#### Razor — Émission

*aura #FF7A3C, aura calme*  
Maître du jeu à Greed Island.

- **Passif — Maître du jeu** : +10 % de dégâts au corps à corps.

| Technique                                               | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                   |
|---------------------------------------------------------|---|---|---|---|-----------------------------------------------------------------------------------|
| **Balle d'aura** <br>*Aura Ball*                        | Émission | 25 | 7 s | — | Un ballon d'aura frappé très fort vers la cible (18 blocs), gros recul.           |
Invcation des Quatorze démons - Invocation de combattants de Nen. Ordres simples : attaquer, défendre, suivre, se regrouper. Possibilité de fusionner les diables. Puissance individuelle réduite lorsque plusieurs sont actifs. (Invocations actives	Puissance individuelle
1	Très élevée
2–4	Élevée
5–8	Moyenne
9–14	Faible)
| **Attaque des Quatorze démons** ★ <br>*Fourteen Devils* | Émission + Matérialisation | 60 | 30 s | Nen 30 | les Quatorze démons d'aura bombardent en même temps la zone autour du point visé. |
- Smash de Razor - Charge une balle d'aura. La frappe violemment dans la direction visée. Très gros dégâts, recul et destruction des blocs.Peut être renforcée par une invocation qui effectue une passe.

#### Genthru (le Bombardier) — Matérialisation

*aura #FF5A2A, aura sauvage (flammes même au repos)*  
Le Bombardier de Greed Island.

- **Passif — Bombardier** : Dégâts d'explosion reçus −50 %.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                                                              |
|---|---|---|---|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Petite fleur** <br>*Little Flower* | Matérialisation | 30 | 10 s | — | Explosion au contact d'un ennemi.. Possibilité de saisir un joueur ou un PNJ.  Dégâts importants et recul Consommation d'aura proportionnelle à la puissance. reçoit des dégats si la défense n'est pas assez bonne par rapport àl'attaque . |
| **Compte à rebours** <br>*Countdown* | Matérialisation + Émission | 40 | 30 s | Nen 20 | Pose une bombe sur la cible Le compte à rebours dépend du rythme cardiaque (random entre 1-100). elle explose (extrèement puissant). Désamorçage si trop de contact (même les attaque in/out) avec  Genthru                                  |

### Hunters de l'arc des Fourmis

#### Kite — Matérialisation

*aura #E6E6FA, aura calme*  
Disciple de Ging, Hunter des espèces.

- **Passif — Chasseur** : +15 % de dégâts contre les bêtes et les monstres.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                                                                                                                                                                                                           |
|---|---|---|----------|---|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Crazy Slot** <br>*Crazy Slots* | Matérialisation + Spécialisation | 35 | 60 s     | — | La roulette tire une arme au hasard : faux, fusil, bâton ou clown. les armes sont materialisé et donné au joueur, il en peut pas relancer ou jeter l'arme, Il doit l'avoir utiliser pour pouvoir relancer la technique (Le clown possède également une personnalité et peut parler à Kite. Il commente notamment ses choix et ses réactions, ce qui agace régulièrement son utilisateur.)<br/> |
La faux géante - Kite matérialise une immense faux à la lame particulièrement tranchante. - Cette arme possède une restriction très particulière : il ne peut pas simplement la ranger ou changer d'arme. Il doit utiliser Silent Waltz pour s'en débarrasser.
-> Silent Waltz — Valse silencieuse - Kite effectue une rotation complète sur lui-même en faisant tournoyer sa faux.La lame produit alors une attaque circulaire extrêmement meurtrière qui tranche les ennemis autour de lui.
Numéro 3 — Le bâton mystérieux - pour l emoment un simple baton rien de spécial (quelque dégats sur l'arme de abse genre un +4 mais pas plus)
Numéro 4 — La carabine - Kite peut matérialiser une carabine capable de tirer des projectiles extrêmement puissants. (Tir à distance.Cadence de tir relativement rapide. Puissance suffisante pour pénétrer les corps résistants des fourmis-chimères. Fonctionnement apparemment silencieux ou fortement atténué.))
Pour les autres slot propose des autres trucs il faut qu'il y ait un total de 10 armes au total dont une qui est la "spécial qui permet àKite de ne pas mourir"
#### Knov — Émission

*aura #7AA0D0, aura calme*  
Hunter stratège de l'équipe d'extermination.

- **Passif — Stratège** : Prévenu quand un monstre le prend pour cible. Au Gyo, il voit ses portes marquées.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait                                                                                                                                                                                                    |
|---|---|---|---|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Marquer une porte** <br>*Mark a Door* | Émission + Matérialisation | 10 | 2 s | — | Marque la porte visée (5 blocs). Nombre de marques : 1, plus 1 par tranche de 20 niveaux de Nen (6 au plus) ; la plus ancienne s'efface. les portails sont visible avec le Gyo et sont destructible par les autres |
| **Cache-cache** <br>*Hide and Seek* | Émission + Matérialisation | 30 | 10 s | Nen 10 | Un portail vers la pièce-refuge (abri, régénération). Dans le refuge : choisir par quelle porte marquée ressortir.                                                                                                 |

#### Knuckle Bine — Matérialisation

*aura #F0E060, aura calme*

Hunter au grand cœur, disciple de Morel. Spécialiste des combats d'usure et de la neutralisation des utilisateurs de Nen.

- **Passif — Hakoware (Bankruptcy Chapter Seven)** : après avoir frappé un adversaire, Knuckle lui prête une quantité d'aura proportionnelle à la puissance du coup. Potclean apparaît et suit la cible en affichant sa dette. Les intérêts augmentent de 10 % toutes les 10 secondes (intérêts composés). Chaque attaque Nen de la cible contre Knuckle rembourse une partie de la dette au lieu d'infliger ses dégâts normaux. Lorsque la dette dépasse le potentiel total d'aura de la cible, elle fait banqueroute : Potclean devient Toritaten et impose un Zetsu forcé.
  | **Toritaten — Banqueroute** (*Bankruptcy*) | Matérialisation + Émission + Manipulation | 0 | Automatique | — | Lorsque la dette dépasse le potentiel total d'aura de la cible, Potclean se transforme en Toritaten et impose un Zetsu forcé. |

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---:|---:|---|---|
| **Activer Hakoware** (*Activate Hakoware*) | Matérialisation + Émission + Manipulation | 15 | 3 s | — | Frappe une cible à moins de 3 blocs. Lui prête de l'aura et fait apparaître Potclean. Le montant prêté dépend de la puissance du coup. |
| **Annuler Hakoware** (*Cancel Hakoware*) | Matérialisation + Manipulation | 0 | 1 s | — | Rappelle Potclean et efface la dette. Impossible après une banqueroute. |


#### Morel Mackernasey — Manipulation

*aura #8A5AB0, aura calme*

Hunter professionnel, membre de l'équipe d'extermination des fourmis-chimères et maître de Knuckle et Shoot. Combattant tactique spécialisé dans la manipulation de fumée.

- **Passif — Maître de la fumée** : Morel peut récupérer une partie de l'aura investie dans ses constructions lorsqu'il les dissipe. Ses créations peuvent être contrôlées à distance. Sa pipe géante est nécessaire pour produire de nouvelles constructions complexes ; sans elle, il peut encore manipuler la fumée déjà présente.

- **Passif — Capacité pulmonaire** : réserve respiratoire exceptionnelle. Permet de maintenir ses techniques de fumée dans des conditions difficiles et d'utiliser la fumée dans des environnements inhabituels.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---:|---:|---|---|
| **Deep Purple — Soldats de fumée** | Manipulation + Émission | 45 | 20 s | Nen 20 | Crée des soldats humanoïdes de fumée qui combattent automatiquement ou suivent des ordres. Jusqu'à 216 soldats simples ou 50 soldats complexes. |
| **Deep Purple — Éclaireur** | Manipulation + Émission | 15 | 8 s | Nen 15 | Crée un petit animal de fumée qui explore une zone et repère les ennemis. |
| **Deep Purple — Clone** | Manipulation + Émission | 35 | 15 s | Nen 25 | Crée une copie de Morel ou d'une cible observée. Peut servir de leurre, se déplacer et reproduire des comportements simples. |
| **Deep Purple — Constructions** | Manipulation + Transformation possible | 25 | 10 s | Nen 20 | Façonne la fumée en cordes, murs, plateformes, radeaux ou autres structures utilitaires. |
| **Smoky Jail — Prison de fumée** | Manipulation + Transformation probable | 70 | 60 s | Nen 35 | Enferme une zone dans une barrière de fumée extrêmement résistante. Empêche les entités de traverser normalement. |
| **Nuage aveuglant** | Manipulation + Transformation possible | 20 | 12 s | Nen 10 | Répand un épais nuage de fumée qui réduit fortement la visibilité des ennemis. Morel conserve une capacité de perception à travers sa fumée. |


#### Palm Siberia — Renforcement

*aura #6A3A7A, aura calme*  
Élève de Knov, voyante, devenue fourmi-chimère.

- **Passif — Wink Blue** : Permet de suivre jusqu'à 3 ennemis préalablement observés avec l'œil droit. En couvrant cet œil, Palm peut voir leur position à distance. Une nouvelle cible remplace la plus ancienne.  
  *Type : Inconnu (clairvoyance).*

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---:|---:|---:|---|
| **Veuve noire** (*Black Widow*) | Renforcement (+ Manipulation possible) | 50 | 30 s | Nen 20 | Palm enveloppe son corps de ses cheveux pour former une armure noire. Lui confère Résistance et lui permet de concentrer son aura sur ses attaques avec Ko. Force II pendant 20 s. |
### Fourmis-Chimères

#### Meruem — Émission

*aura #6A2C91, aura sauvage (flammes même au repos)*  
Le roi des fourmis-chimères.

**Passifs**

- **Roi — Synthèse d'aura** : En consommant un utilisateur du Nen, Meruem absorbe une partie de son aura et augmente ses propres réserves. Après avoir consommé une grande partie de Pouf et Youpi, il récupère également certaines de leurs capacités.  
  *Type : inconnu / capacité innée. Ne pas attribuer automatiquement à la Spécialisation.*

- **Corps du Roi** : Très grande résistance, force physique, vitesse et régénération naturelle. Réduit les dégâts physiques et augmente les dégâts au corps-à-corps.  
  *Type : Renforcement probable pour les applications de Nen ; capacités biologiques de fourmi-chimère également.*

- **Génie du combat** : Meruem analyse rapidement les habitudes et les mouvements de ses adversaires. Après avoir affronté plusieurs fois un même ennemi, ses attaques ont davantage de chances de toucher.  
  *Aucun type de Nen : intelligence et capacité d'analyse.*

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---:|---:|---:|---|
| **Rafale de rage** (*Rage Blast*) | Émission + Renforcement | 40 | 15 s | — | Meruem concentre une immense quantité d'aura dans son bras et projette une décharge explosive vers l'avant. Explose à l'impact et inflige des dégâts dans un rayon de 7 blocs. |
| **Photon** ★ | Transformation + Émission (+ Manipulation possible) | 50 | 20 s | Nen 30 | Meruem transforme son aura en particules lumineuses et les disperse dans une zone de 20 blocs. Révèle les ennemis, leurs positions et leurs mouvements. Permet également de percevoir certaines informations sur leur état et leurs émotions. Plus il concentre de particules sur une cible, plus les informations obtenues sont précises. |
| **En du Roi** | Émission + Transformation probable | 60 | 45 s | Nen 35 | Meruem déploie son En dans une immense zone autour de lui. Détecte les êtres vivants, leurs déplacements et les changements dans l'environnement. Avec Photon, permet une perception beaucoup plus précise. |
| **Métamorphose** (*Metamorphosis*) ★ | Type incertain : capacité héritée de Youpi | 50 | 30 s | Nen 30 | Meruem modifie son corps grâce aux capacités absorbées : fait apparaître des ailes et développe une forme adaptée au combat aérien. Lui permet de voler à très grande vitesse. |
| **Accélération royale** | Renforcement probable | 30 | 12 s | Nen 20 | Meruem concentre son aura dans son corps pour effectuer une accélération fulgurante. Augmente fortement sa vitesse de déplacement et d'attaque pendant quelques secondes. |
| **Frappe du Roi** | Renforcement | 25 | 5 s | — | Un coup physique extrêmement puissant, porté avec sa force naturelle et son aura. Peut projeter les ennemis et détruire certains blocs. |
| **Queue du Roi** | Renforcement | 20 | 4 s | — | Attaque rapide avec sa queue. Inflige des dégâts importants et peut repousser ou déséquilibrer la cible. |
| **Pression royale** | Renforcement (Ren) | 40 | 30 s | Nen 15 | Meruem libère un Ren extrêmement puissant. Les ennemis proches subissent un effet de peur et peuvent être ralentis ou désorientés. |

**Capacités conditionnelles — Meruem post-Rose**

- **Vol royal** : Débloqué après la Métamorphose. Permet de voler librement et d'atteindre une vitesse extrême.
- **Photon avancé** : Améliore la portée et la précision de Photon. Peut repérer les ennemis dissimulés et détecter certaines intentions hostiles.
- **Synthèse supérieure** : Après consommation d'un utilisateur du Nen, augmente temporairement ou définitivement les réserves d'aura selon l'équilibrage du mod.
#### Shaiapouf — Manipulation

*aura #C0A0FF, aura calme*  
Garde royal du roi, manipulateur.

**Passifs**

- **Ailes de papillon** : Pouf vole naturellement grâce à ses ailes. Peut voler librement, même en Zetsu. Aucun dégât de chute. Sa vitesse aérienne peut être augmentée avec son aura.
- **Esprit calculateur** : Pouf analyse les comportements et les émotions de ses adversaires. Après avoir observé un ennemi, augmente la précision de ses prédictions et réduit les chances de subir ses attaques surprises.
- **Régénération cellulaire** : Grâce à sa capacité à contrôler ses cellules, Pouf peut reconstituer les parties de son corps détruites, tant que ses cellules ne sont pas complètement endommagées.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---:|---:|---:|---|
| **Message spirituel** (*Spiritual Message*) ★ | Manipulation (+ Émission possible) | 30 | 15 s | — | Pouf disperse ses écailles dorées dans un rayon de 8 blocs. Les ennemis exposés deviennent visibles à travers leurs flux d'aura. Pouf peut analyser leurs émotions, leur état mental et anticiper leurs intentions. |
| **Hypnose des écailles** | Manipulation | 40 | 25 s | Nen 15 | Les ennemis ayant inhalé les écailles de Pouf deviennent hypnotisables. Les cibles faibles peuvent être désorientées, ralenties ou contraintes de suivre des ordres simples. Les utilisateurs du Nen peuvent résister selon leur puissance et leur volonté. |
| **Belzébuth** (*Beelzebub*) ★ | Manipulation | 45 | 30 s | Nen 20 | Pouf divise son corps en milliers de fragments miniatures autonomes. Il peut les disperser pour espionner, fuir ou harceler ses ennemis. Plus les fragments sont petits, plus ils sont difficiles à repérer, mais plus ils sont fragiles. Un corps central de la taille d'une abeille doit être conservé pour commander les fragments. |
| **Reconstruction corporelle** | Manipulation | 35 | 20 s | Nen 25 | Pouf réorganise ses cellules pour modifier son apparence : visage, corps et voix. Peut prendre l'apparence d'une autre personne. Peut également utiliser ses cellules pour réparer les blessures d'un autre individu. |
| **Cocon de renaissance** (*Cocoon*) ★ | Manipulation | 70 | 90 s | Nen 30 | Pouf enferme une cible dans un cocon. Après une période d'incubation, la cible peut être éveillée au Nen et développer une aptitude adaptée à sa nature. Peut également transformer un humain en hybride humain-fourmi. |
| **Nuée espionne** | Manipulation | 20 | 15 s | Nen 15 | Pouf disperse quelques fragments miniatures pour surveiller une zone. Il partage leurs perceptions et peut détecter les déplacements des ennemis. |
| **En du papillon** | En (technique générale de Nen) | 35 | 30 s | Nen 20 | Pouf étend son aura autour de lui afin de détecter les présences et mouvements dans une zone. Moins vaste que celui de Pitou, mais peut être maintenu longtemps. |

**Interactions et restrictions**

- **Message spirituel + Hypnose** : les écailles servent à la fois à analyser les émotions et à influencer les personnes qui les inhalent.
- **Belzébuth** : les fragments miniatures sont beaucoup plus fragiles que le corps original. Les fragments microscopiques perdent certaines capacités sensorielles.
- **Belzébuth + Message spirituel** : l'utilisation simultanée de ces deux capacités est à éviter dans le gameplay ; la division complète du corps compromet la dispersion et l'utilisation efficace des écailles.
- **Cocon de renaissance** : la création d'une nouvelle aptitude nécessite du temps. Le processus n'est pas une simple guérison instantanée.

#### Menthuthuyoupi — Transformation

*aura #FF3B3B, aura sauvage (flammes même au repos)*  
Garde royal du roi, monstre de puissance.

**Passifs**

- **Vitalité** : +20 PV max.
- **Corps monstrueux** : Résistance naturelle accrue aux dégâts physiques et aux explosions.
- **Colère grandissante** : Plus Youpi reçoit de dégâts, plus sa jauge de rage augmente. À certains seuils, ses attaques deviennent plus puissantes. La rage peut être consommée pour déclencher ses techniques explosives.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---:|---:|---:|---|
| **Métamorphose** (*Metamorphosis*) ★ | Transformation | 40 | 25 s | — | Youpi modifie son anatomie : bras supplémentaires, tentacules, yeux sur son corps et lames. Force II et Résistance pendant 15 s. Permet d'augmenter la portée et le nombre de ses attaques. |
| **Bras tentaculaires** | Transformation | 25 | 8 s | Nen 10 | Fait pousser quatre bras supplémentaires sous forme de tentacules. Permet d'attaquer plusieurs ennemis simultanément et d'augmenter considérablement la portée au corps-à-corps. |
| **Lames organiques** | Transformation | 20 | 6 s | Nen 10 | Transforme ses bras ou tentacules en lames tranchantes. Les attaques infligent des dégâts supplémentaires et peuvent provoquer un saignement. |
| **Pointes dorsales** | Transformation | 35 | 18 s | Nen 20 | Fait pousser une multitude de pointes sur son dos et les projette brutalement autour de lui. Attaque en zone, particulièrement efficace contre les ennemis qui l'entourent. |
| **Rafale de rage** (*Rage Blast*) ★ | Transformation + Émission | 45 | 20 s | Nen 20 | Youpi accumule sa colère et gonfle son corps avant de libérer une gigantesque explosion d'aura autour de lui (6 blocs). Plus sa jauge de rage est élevée, plus l'explosion est puissante. Pendant la charge, il est plus vulnérable. |
| **Rage incarnée** (*Rage Incarnate*) ★ | Transformation | 60 | 45 s | Nen 30 | Youpi adopte une forme de centaure monstrueux. Ses jambes deviennent celles d'un quadrupède, son bras droit devient une énorme faux et son bras gauche se transforme en une poche contenant sa rage. Augmente sa vitesse, sa portée et ses dégâts au corps-à-corps. |
| **Faux du Roi** | Transformation + Renforcement possible | 30 | 7 s | Nen 25 | Dans sa forme Rage incarnée, Youpi utilise sa faux pour effectuer une large attaque circulaire dévastatrice. Peut toucher plusieurs ennemis. |
| **Ailes monstrueuses** | Transformation | 25 | 15 s | Nen 20 | Fait pousser des ailes dans son dos. Permet de voler temporairement et de réaliser des attaques aériennes. |
| **Poing colossal** | Transformation + Renforcement possible | 30 | 10 s | Nen 15 | Agrandit considérablement un de ses bras pour porter un coup extrêmement puissant. Repousse les ennemis et peut détruire les blocs résistants. |
| **Explosion concentrée** | Transformation + Émission | 55 | 30 s | Nen 35 | Dans sa forme Rage incarnée, concentre sa rage et la projette sous la forme d'un puissant rayon destructeur vers l'avant. Très coûteux en aura. |

**Capacités spéciales**

- **Contrôle de la rage** : Youpi peut interrompre la préparation de Rage Blast avant l'explosion. Il conserve alors son aura et peut surprendre les adversaires qui s'approchent en pensant qu'il est vulnérable.
- **Adaptation anatomique** : Certaines transformations peuvent être combinées : bras supplémentaires + lames, yeux supplémentaires + tentacules, ailes + forme quadrupède.
- **Corps sacrificiel** : Youpi peut liquéfier et céder une partie de ses cellules à une autre personne. Cette capacité lui permet notamment de nourrir Meruem après l'explosion de la Rose.
### Autres

#### Zushi — Manipulation

*aura #9FD4FF, aura calme*  
Élève de Wing à la Tour Céleste.

- **Passif — Assidu** : +25 % d'expérience à l'entraînement.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Poing de Ren** <br>*Ren Punch* | Manipulation | 15 | 4 s | — | Un coup renforcé par le Ren au corps à corps. |

#### Senritsu — Émission

*aura #C9A6FF, aura calme*  
Musicienne, entend les battements de cœur.

- **Passif — Battement de cœur** : Elle entend les ennemis cachés (Zetsu, invisibles) à 20 blocs : ils brillent.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Mélodie** <br>*Melody* | Émission + Manipulation | 35 | 20 s | — | Sa flûte soigne les alliés à 8 blocs, ralentit et affaiblit les ennemis. |

#### Wing — Renforcement

*aura #9FD0FF, aura calme*  
Maître de Gon et Killua, Shingen-ryu.

- **Passif — Professeur** : Les alliés à 8 blocs gagnent un peu de Nen avec le temps.

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Shingen-ryû** <br>*Shingen-ryu* | Renforcement | 25 | 15 s | — | 10 s en garde : coups reçus divisés par deux, chaque assaillant reçoit 4 dégâts. |

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
| **Squelette au Nen** | squelette, vagabond, embourbé | Émission | Flèches de Nen | 20 | 4,5 s | Trois flèches d'aura en éventail qui filent droit, sans retomber. |
| **Creeper au Nen** | creeper | Émission | Mines d'aura | 30 | 6 s | Quatre mines autour de la cible éclatent un instant plus tard ; le creeper, lui, n'explose plus. |
| **Zombie au Nen** | zombie, zombie momifié, noyé, zombie villageois | Matérialisation | Horde de Nen | 40 | 20 s | Deux zombies d'aura sortent du sol et attaquent ; ils se dissipent au bout de 15 s. |
| **Araignée au Nen** | araignée, araignée venimeuse | Manipulation | Toile d'aura | 20 | 7 s | Un fil d'aura colle la cible au sol : très ralentie, elle ne peut plus sauter. |
| **Enderman au Nen** | enderman | Spécialisation | Pas éclair | 25 | 6 s | Apparaît dans le dos de la cible et frappe. |
| **Cochon au Nen** | cochon, hoglin | Renforcement | Charge de sanglier | 15 | 5 s | Fonce sur la cible et la renverse. |
| **Vache au Nen** | vache, champimeuh, chèvre | Renforcement | Lait d'aura | 25 | 15 s | Soigne ses alliés autour de lui. |
| **Mouton au Nen** | mouton, lama | Matérialisation | Laine-bouclier | 20 | 15 s | Une laine d'aura protège ses alliés. |
| **Poule au Nen** | poule | Émission | Œufs explosifs | 15 | 6 s | Une volée d'œufs d'aura qui éclatent autour de la cible. |
| **Loup au Nen** | loup, renard, chat, ocelot | Renforcement | Croc d'aura | 15 | 4,5 s | Bond et morsure qui fait saigner. |
| **Slime au Nen** | slime, cube de magma, grenouille | Renforcement | Rebond écrasant | 20 | 6 s | Saute très haut et retombe en onde de choc. |
| **Sorcière au Nen** | sorcière, évocateur | Transformation | Potion de Nen | 25 | 8 s | Affaiblit et ralentit tout ce qui l'entoure. |
| **Golem au Nen** | golem de fer, ravageur | Renforcement | Séisme | 25 | 7 s | Frappe le sol : tout autour est projeté. |
| **Blaze au Nen** | blaze, ghast | Transformation | Pluie de feu | 30 | 8 s | Des boules de feu d'aura tombent autour de la cible. |
| **Gardien au Nen** | gardien | Émission | Rayon abyssal | 30 | 7 s | Charge un rayon d'aura qui poursuit lentement la cible avant de la frapper. |
| **Grand gardien au Nen** | grand gardien | Émission | Rayon abyssal renforcé | 45 | 10 s | Charge un puissant rayon d'aura qui traverse les cibles alignées et inflige de lourds dégâts. |
| **Shulker au Nen** | shulker | Manipulation | Capsule gravitationnelle | 25 | 8 s | Tire un projectile qui enferme temporairement la cible dans une sphère d'aura et la fait léviter. |
| **Phantom au Nen** | phantom | Transformation | Ailes spectrales | 20 | 6 s | Traverse rapidement la cible en piqué, puis laisse derrière lui une traînée d'aura blessante. |
| **Silverfish au Nen** | poisson d'argent, endermite | Matérialisation | Fissure rampante | 15 | 5 s | Disparaît dans le sol et ressort sous les pieds de sa cible pour la mordre. |
| **Abeille au Nen** | abeille | Renforcement | Dard venimeux | 15 | 6 s | Effectue une charge rapide et pique la cible. Inflige des dégâts et un poison temporaire. |
| **Pilleur au Nen** | pillard, vindicateur | Émission | Tir perforant | 20 | 5 s | Tire un projectile d'aura rapide qui traverse les ennemis sur sa trajectoire. |
| **Piglin au Nen** | piglin, piglin barbare | Renforcement | Frénésie dorée | 25 | 12 s | Entre en rage pendant 8 s : vitesse et dégâts augmentés, mais défense réduite. |
| **Vex au Nen** | vex | Transformation | Traversée spectrale | 20 | 7 s | Devient intangible pendant 2 s, traverse les obstacles et attaque à la sortie. |
| **Warden au Nen** | warden | Émission | Rugissement dévastateur | 50 | 15 s | Libère une onde sonore d'aura en ligne droite qui traverse les obstacles et repousse violemment les cibles. |
| **Warden au Nen** | warden | Renforcement | Cœur du séisme | 40 | 12 s | Frappe le sol avec ses deux bras : provoque une onde de choc circulaire qui fait trembler et projette les ennemis. |
| **Wither au Nen** | wither | Émission | Bombardement maudit | 45 | 10 s | Tire trois crânes d'aura qui poursuivent leur cible et explosent à l'impact. |
| **Ender Dragon au Nen** | ender dragon | Émission | Souffle du Néant | 60 | 18 s | Crache un puissant souffle d'aura qui laisse une zone persistante et dangereuse au sol. |
| **Tortue au Nen** | tortue | Renforcement | Carapace impénétrable | 20 | 15 s | Se replie dans sa carapace et réduit considérablement les dégâts reçus pendant 5 s. |
| **Dauphin au Nen** | dauphin | Émission | Onde sonar | 15 | 10 s | Émet une onde d'aura qui révèle les créatures sous-marines dans une large zone. |
| **Panda au Nen** | panda | Renforcement | Étreinte écrasante | 20 | 7 s | Attrape une cible proche et la serre violemment avant de la projeter. |
| **Araignée des cavernes au Nen** | araignée venimeuse | Manipulation | Venin paralysant | 25 | 12 s | Projette une toile empoisonnée qui immobilise brièvement la cible et lui applique un poison. |
| **Chauve-souris au Nen** | chauve-souris | Émission | Écho révélateur | 15 | 12 s | Émet une onde sonore qui révèle les ennemis proches, même derrière les murs. |
| **Poulpe au Nen** | poulpe, poulpe lumineux | Transformation | Encre obscure | 20 | 10 s | Libère un nuage d'aura sombre qui aveugle les ennemis et lui permet de s'échapper. |
## 7. Combattants nommés de la Tour Céleste

Au-delà du 200e étage, selon l'étage du combat. Leurs Hatsu ne sont pas proposés au joueur.


**Gido** — Manipulation, étages 200–209

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Toupies tourbillon** <br>*Whirlwind Tops* | Manipulation | 25 | 6 s | — | Une nuée de toupies d'aura balaie tout autour (3,5 blocs) et repousse. |
| **Danse de la tornade** <br>*Tornado Dance* | Manipulation | 30 | 20 s | — | Gido tourne sur sa jambe de bois : Résistance et Vitesse 6 s. |

**Sadaso** — Émission, étages 210–219

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Main fantôme** <br>*Phantom Hand* | Émission | 25 | 7 s | — | Son bras gauche invisible agrippe la cible à 9 blocs et l'immobilise 3 s. |

**Riehlvelt** — Transformation, étages 220–224

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Fouet électrique** <br>*Electric Whip* | Transformation | 20 | 5 s | — | Fouet chargé d'électricité (7 blocs) : dégâts et lenteur. |
| **Canon à air** <br>*Air Cannon* | Transformation + Émission | 30 | 8 s | — | Air comprimé tiré depuis le fauteuil (14 blocs) : gros recul. |

**Kastro** — Renforcement, étages 225–234

| Technique | Type | Aura | Recharge | Niveau requis | Ce qu'elle fait |
|---|---|---|---|---|---|
| **Morsure du tigre** <br>*Tiger Bite* | Renforcement | 20 | 4,5 s | — | Mains en gueule de tigre, au corps à corps : très gros dégâts. |
| **Double** | Renforcement | 40 | 25 s | — | Son double attire les coups pendant que le vrai Kastro disparaît : Invisibilité et Force 4 s. |
- **Hisoka Morow** (étages 235–240) : son vrai kit de personnage (voir sa fiche).
<!-- FIN-GENERE -->

# Pas encore dans le jeu

## 8. Propositions de nouvelles techniques (19 nov) — à valider par Evan

Texte seulement, rien n'est codé. Pour chacune : nom, effet voulu, type de Nen. Avant de coder une proposition
acceptée, vérifier qu'elle ne fait pas doublon avec une technique déjà listée plus haut.
Les doublons éventuels avec les kits existants (docs/hatsu-et-techniques.md) seront vérifiés avant de coder celles qui sont acceptées.

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
