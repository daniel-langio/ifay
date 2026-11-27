# Rapprochement de paiements

> Une déclaration de paiement et la confirmation d'un vérificateur sont rapprochées par `(type, pspRef)`, puis le paiement passe en vérifié.

## En bref

- Un client déclare un paiement, un vérificateur indépendant signale l'avoir observé.
- Chaque côté peut arriver en premier : le paiement reste `PENDING` jusqu'à l'autre.
- Le montant signalé doit couvrir le montant déclaré pour passer en `VERIFIED`.
- Des clés d'API distinctes pour les clients et les vérificateurs.

## Stack

Java · Gradle · Docker

## Aller plus loin

[Le dépôt](https://github.com/daniel-langio/ifay)
