# CivilisationUltimate 1.0.2

Correction du build :
- suppression de l'import `org.bukkit.ban.BanList` incompatible avec l'API utilisée ;
- correction du système de freeze : `Map.remove()` renvoie un booléen dans le code précédent, remplacé par `containsKey/remove`.

Le workflow reste Java 25 + Gradle 9.1.
