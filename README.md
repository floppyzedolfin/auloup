# Au loup! <img src="docs/iris.svg" alt="Iris, la louve endormie" align="right" width="76">

[![CI](https://github.com/floppyzedolfin/auloup/actions/workflows/ci.yml/badge.svg)](https://github.com/floppyzedolfin/auloup/actions/workflows/ci.yml)

[English](README.en.md) · **Français**

Bloque les appels entrants par **préfixe de numéro**. Vous gardez une liste de
préfixes (par ex. `+1900`, `0900`) ; tout appel entrant dont le numéro commence
par l'un d'eux est rejeté automatiquement.

## L'histoire d'Iris

Iris a longtemps été la coupable idéale. Chaque fois que Pierre s'ennuyait, il
criait ; le village accourait, fourche au poing — et c'est elle, la louve,
qu'on venait débusquer. Pourtant Iris n'avait jamais touché au troupeau. Elle
ne demandait qu'à dormir en paix, le museau posé sur ses pattes.

Fatiguée d'être traquée pour des alarmes qui n'étaient pas les siennes, Iris a
fini par retourner la meute. Aujourd'hui, c'est elle qui veille — non plus
contre les villageois, mais aux côtés de celles et ceux qui en ont assez d'être
harcelés. Quand un démarcheur appelle, Iris ouvre un œil, et le silence revient.

Libre et open source (GPL-3.0). Android d'abord ; iOS est prévu (voir
[Plateformes](#plateformes)).

## Soutenir les loups

Au loup! est gratuite et ne demande rien pour elle-même. Si vous voulez aider,
soutenez de **vrais** loups via l'une de ces associations françaises qui les
défendent (toutes membres fondatrices du collectif [CAP Loup](https://www.cap-loup.fr/)) :

- **[ASPAS](https://www.aspas-nature.org/)** — protection des animaux sauvages ;
  reconnue d'utilité publique, comptes audités de façon indépendante. Don en
  ligne : <https://www.aspas-nature.org/nous-soutenir/faire-un-don/>
- **[FERUS](https://www.ferus.fr/)** — la spécialiste du loup, de l'ours et du
  lynx. Don en ligne : <https://www.ferus.fr/soutenez-nos-actions/dons-2>
- **[WWF France](https://www.wwf.fr/)** — conservation de la nature ; fondation
  reconnue d'utilité publique. Don en ligne : <https://faireundon.wwf.fr/>

Don en ligne par carte ; en France, 66 % de votre don est déductible des impôts
(un don de 5 € vous coûte 1,70 €).

## Statut

MVP (version initiale) :

- Liste persistante de préfixes bloqués, choisis via un sélecteur de pays
  (drapeau + indicatif) et un champ de préfixe national (au moins 3 chiffres).
- Comprend le préfixe national (d'acheminement) de chaque pays : saisir `01 60`
  en France équivaut à `+33 1 60`, `07…` au Royaume-Uni à `+44 7…`, et un appel
  correspond que son numéro arrive au format national ou international.
- Rejette les appels correspondants — silencieusement, sans trace dans le journal
  d'appels.
- Compte le nombre d'appels bloqués par préfixe. Quand plusieurs préfixes
  correspondent, c'est le plus précis (le plus long) qui est crédité.
- Touchez un préfixe pour voir l'historique des appels qu'il a bloqués (numéro +
  heure).
- Graphiques par jour et par heure des appels bloqués, sur l'écran principal et
  par préfixe.
- Liste groupée par pays, repliable, avec un total par pays.
- Import des listes **officielles** des régulateurs (plages de démarchage ARCEP
  en France) ; les préfixes importés sont marqués *Officiel*.
- Notification discrète (facultative) dans la barre d'état quand un appel est
  bloqué.
- Page Réglages : changer la langue de l'appli et activer/désactiver les
  notifications.

Aucun compte, aucun réseau, aucun pistage. Votre liste de préfixes ne quitte
jamais l'appareil.

## Plateformes

| Plateforme | Blocage par préfixe | Comment |
| --- | --- | --- |
| **Android** (10 / API 29+) | ✅ Complet | Un [`CallScreeningService`](https://developer.android.com/reference/android/telecom/CallScreeningService) exécute notre code à chaque appel entrant et fait la correspondance au moment de l'appel. |
| **iOS** | ⏳ Prévu, limité | Le blocage iOS (`CallKit` Call Directory) n'accepte qu'une liste statique de **numéros complets** — c'est l'OS qui fait la correspondance et le code de l'appli ne s'exécute jamais pendant l'appel, donc le vrai blocage par préfixe est impossible. Le support iOS arrivera séparément, avec un modèle différent fondé sur les numéros complets. |

C'est pourquoi le projet est « Android d'abord » : le blocage par préfixe y est
une fonctionnalité de premier plan, et fondamentalement contraint sur iOS.

## Comment ça marche

```
MainActivity (UI Compose)  ─┐
                            ├─►  PrefixRepository  ──►  DataStore (sur l'appareil)
PrefixCallScreeningService ─┘            ▲
   (invoqué par le système)              │
                                   Prefixes (logique de correspondance pure)
```

- `Prefixes.kt` — logique de correspondance pure, sans Android (`normalize`, `longestMatch`, `isBlocked`) ; testée unitairement.
- `Countries.kt` — données ISO→indicatif ; noms de pays via `Locale`, drapeaux en emoji.
- `PrefixRepository.kt` — persiste les préfixes, l'historique des appels bloqués et la préférence de notification (Jetpack DataStore). Les comptes sont dérivés de l'historique.
- `PrefixCallScreeningService.kt` — lié par le système à chaque appel entrant ; rejette les correspondances et enregistre le blocage.
- `Notifications.kt` — le canal et l'émetteur de la notification silencieuse « appel bloqué ».
- `MainActivity.kt` — les écrans Compose : la liste principale (activer le blocage, basculer les notifications, ajouter/retirer des préfixes, voir les compteurs) et l'historique des appels bloqués par préfixe.

Pour bloquer les appels, l'appli doit obtenir le **rôle de filtrage d'appels**
(`RoleManager.ROLE_CALL_SCREENING`) ; l'interface le demande.

## Compiler et lancer

### Avec Android Studio (recommandé)

1. Installez [Android Studio](https://developer.android.com/studio). Il fournit
   un JDK compatible, le SDK Android et un émulateur.
2. **Ouvrez** ce dossier. Laissez Gradle se synchroniser.
3. Choisissez un appareil/émulateur (Android 10+) et appuyez sur **Run**.
4. Dans l'appli, touchez **Activer le blocage des appels**, accordez le rôle,
   puis ajoutez un préfixe.

### En ligne de commande

Nécessite le SDK Android et un JDK LTS (17 ou 21 — Gradle ne supporte pas encore
JDK 25). Renseignez `sdk.dir` dans un fichier `local.properties` (Android Studio
le fait pour vous).

Le plus simple est `make`, qui trouve un JDK compatible et écrit l'APK dans
`app/build/outputs/apk/debug/auloup.apk` :

```sh
make            # compile l'APK debug (auloup.apk)
make install    # compile et installe sur un appareil/émulateur connecté
make clean      # supprime les artefacts de build
```

Si `make` ne trouve pas de JDK, indiquez-le : `make JAVA_HOME=/chemin/vers/jdk-21`.

Ou appelez Gradle directement (fonctionne aussi sous Windows via `gradlew.bat` ;
indiquez le JDK LTS avec `JAVA_HOME` ou `org.gradle.java.home`) :

```sh
./gradlew test           # lance les tests unitaires JVM
./gradlew assembleDebug  # compile un APK debug -> auloup.apk
./gradlew installDebug   # installe sur un appareil/émulateur connecté
```

### Compiler un APK et l'installer sur votre téléphone

Produit un fichier installable que vous copiez à la main sur un téléphone Android
— sans Android Studio ni débogage USB.

**1. Prérequis** (une fois) : un JDK LTS (17 ou 21 — *pas* 25, que Gradle rejette)
et le SDK Android. Avec Android Studio, les deux sont fournis. Sinon, installez
les [outils en ligne de commande](https://developer.android.com/tools), puis :

```sh
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # chemin de votre SDK
```

**2. Compilez l'APK** depuis la racine du projet :

```sh
make
```

L'APK est écrit dans :

```
app/build/outputs/apk/debug/auloup.apk
```

**3. Copiez-le sur le téléphone.** N'importe quel transfert marche — copie USB
(le téléphone apparaît comme un disque ; déposez l'APK dans `Download/`), Quick
Share / Nearby Share, e-mail, ou un cloud.

**4. Installez-le sur le téléphone.** Ouvrez l'appli **Fichiers**, touchez
`auloup.apk` dans `Download/`, et confirmez **Installer**. La première fois,
Android demande d'*autoriser l'installation depuis cette source* — activez-la
pour Fichiers (ou votre navigateur), revenez en arrière et touchez à nouveau
l'APK. (Chemin dans les réglages si besoin : *Applis → Accès spécial des applis →
Installer des applis inconnues*.)

**5. Premier lancement.** Ouvrez **Au loup!**, touchez **Activer le blocage des
appels** et faites-en votre appli de filtrage d'appels, autorisez la notification,
puis ajoutez un préfixe.

> C'est un APK **debug** signé avec la clé debug jetable — parfait pour
> l'installer sur votre propre téléphone. Pour une publication Play Store (`.aab`
> signé), voir [RELEASE.md](RELEASE.md) ; compilez-le avec `make bundle`.

## Localisation

L'interface est entièrement traduisible — toutes les chaînes sont dans
`res/values/strings.xml`, les compteurs utilisent les pluriels Android (avec les
bonnes catégories CLDR par langue), et les noms de pays sont localisés
automatiquement via `Locale`. Sur Android 13+, la langue se choisit par appli
dans les réglages système.

Disponible en **anglais** plus **62 autres langues** (espagnol, variantes du
portugais, français, allemand, chinois, japonais, arabe, hindi, russe, et bien
d'autres — voir `res/values-*/`).

> ⚠️ Les traductions autres qu'anglais/français sont **générées
> automatiquement** comme point de départ et **n'ont pas encore été relues par
> des locuteurs natifs**. Les corrections par PR sont les bienvenues.

Pour ajouter ou améliorer une langue, copiez
`app/src/main/res/values/strings.xml` vers `values-<code>/strings.xml`, traduisez
les valeurs (avec les bonnes catégories de pluriel pour cette langue) et ajoutez
la locale à `res/xml/locales_config.xml`.

## Contribuer

Contributions bienvenues — le code est volontairement petit et soigné. Voir
[CONTRIBUTING.md](CONTRIBUTING.md). En bref : gardez la logique pure dans
`Prefixes.kt` / `Countries.kt` avec des tests, et lancez
`./gradlew ktlintFormat ktlintCheck lintDebug testDebugUnitTest` avant d'ouvrir
une PR (la CI lance les mêmes).

## Licence

[GPL-3.0](LICENSE). © les contributeur·rices d'Au loup!.
