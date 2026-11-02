# NewHxH

Mod Minecraft Hunter x Hunter orienté combat pour **NeoForge 1.21.1** : Nen, techniques (Hatsu),
mobs et personnages, Examen Hunter, Tour Céleste, structures.

Projet de fan, non commercial. Tous les assets sont originaux.

## Développement
- Java 21
- `./gradlew runClient` : lance le jeu avec le mod
- `./gradlew runGameTestServer` : lance les tests en jeu
- `./gradlew runCiClient` : client automatique qui joue `ci/scenario.txt` et prend des captures

La CI (GitHub Actions) fait tout ça à chaque push et publie les résultats sur la branche `ci-results`.
