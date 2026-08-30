# Correction du build

L'ancien build échouait car le pom.xml demandait :
`io.papermc.paper:paper-api:26.2-R0.1-SNAPSHOT`

Cette version utilise :
`io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`

Java 25 reste utilisé par Maven et GitHub Actions.

Si ton serveur Paper 26.2 utilise une API différente et que Maven échoue encore, envoie le nouveau log : on adaptera le POM à l'API réellement publiée par Paper.
