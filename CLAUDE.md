# NewHxH-Neoforge

Mod Minecraft Hunter x Hunter, NeoForge 1.21.1 (Java 21, ModDevGradle, Mojmap + Parchment).
Mod id `hxh`, package `com.evandouard.hxh`. Feuille de route : doc « HxH Mod — Feuille de route ».

## Règles de conception
- Les capacités (Nen, techniques) s'appliquent à n'importe quelle `LivingEntity`, jamais au seul joueur :
  un mob Gon utilise le même Jajanken que le joueur. Seul le déclenchement diffère (touche vs IA).
- Le serveur fait autorité. Le client n'envoie que des intentions (touche pressée) ; le serveur valide
  et renvoie uniquement les champs modifiés.
- Les personnages, kits et techniques sont décrits en données quand c'est possible.
- Textes en français et anglais (`fr_fr.json` et `en_us.json`).

## Vérification (obligatoire pour chaque ajout)
L'environnement de Claude n'a pas accès à Maven/Mojang : on ne compile pas en local, on passe par la CI.
1. Pousser sur `main` → `.github/workflows/ci.yml` compile, lance `runGameTestServer`, puis `runCiClient`
   sous Xvfb qui joue `ci/scenario.txt` et prend des captures.
2. Lire les résultats : `git fetch origin ci-results` puis `git show origin/ci-results:SUMMARY.md`,
   `ci-report.txt`, logs et `screenshots/*.png`.
3. Tout ajout testable a un GameTest (`gametest/`) ; tout ajout visible a une ligne `shot` dans le scénario.
