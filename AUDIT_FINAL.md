# Audit final — Inscription/connexion 100 % locale — MediRappel

Date : 27/11/2026  
Projet : `C:\Users\hamdi\Downloads\medirappel` (aucun autre projet utilisé)  
Campagne : **sans émulateur**, aucun AVD lancé, aucun test instrumenté exécuté.  
Aucune opération Git, aucun secret, aucun compte distant et aucune clé d'API.

## Cause réelle (3 lignes)

La migration 2→3 créait `email`, `passwordHash` et `salt` en `TEXT NOT NULL`, alors que l'entité `User` les déclare nullables : Room rejetait le schéma après migration avec `IllegalStateException`.  
`LocalAuth.register()` avalait cette exception dans `catch (Exception e)` et renvoyait `AuthError.UNKNOWN`, mappé sur l'ancien libellé générique ; le mot de passe n'était pas en cause.  
Sur une installation neuve le bug n'apparaît pas, mais dès qu'une base existante était mise à jour l'inscription échouait ; s'ajoutait l'absence de repli pour `PBKDF2WithHmacSHA256`/`Base64` sur API 24/25.

Diagnostic vérifié avant correction : `RegisterActivity` n'appelait aucun service distant ; `AppDatabase` exposait bien `User` et `UserDao`, mais la migration produisait un schéma différent de celui attendu ; Room et PBKDF2 tournaient déjà hors thread principal ; le `catch` large masquait l'exception.

## Correction appliquée

- `AppDatabase.java` : version 4, suppression des migrations manuelles incompatibles et conservation de `fallbackToDestructiveMigration()` — toute ancienne base est recréée proprement.
- `User.java` : `@NonNull` sur email/hash/sel pour aligner l'entité sur les contraintes attendues par Room.
- `LocalAuth.java` : `Log.e(TAG, "register failed", e)` et `Log.e(TAG, "login failed", e)` sans jamais journaliser le mot de passe ; `SQLiteConstraintException` → email déjà utilisé, toute autre exception → `STORAGE_ERROR`.
- `AuthLogic.java` / `AuthHelper.java` : `UNKNOWN` remplacé par `STORAGE_ERROR`, messages distincts « Cet email est déjà utilisé. » et « Erreur de stockage local. » (traduits en AR).
- `PasswordHasher.java` : SHA-256 sur API 26+, repli automatique documenté vers SHA-1 sur API 24/25 avec 120 000 itérations ; Base64 maison remplace `java.util.Base64`, absent avant API 26.
- `gradlew clean` : purge des classes et rapports compilés périmés qui faisaient encore apparaître l'ancien fichier de configuration dans l'IDE.
- Aucun service distant, aucune dépendance ni plugin associé, aucun fichier de configuration distante et aucun appel au démarrage de l'application ne subsistent.

## Architecture

| Composant | Rôle |
|---|---|
| `User.java` / `UserDao.java` | Entité Room et DAO locaux : insertion et recherche par email normalisé. |
| `AppDatabase.java` | Base `dawai.db` version 4, recréation automatique si l'ancien schéma est incompatible. |
| `PasswordHasher.java` | PBKDF2 120 000 itérations, sel aléatoire, comparaison en temps constant, repli SHA-1 documenté. |
| `AuthLogic.java` | Décisions pures : email existant, email inconnu, mot de passe incorrect, erreur de stockage. |
| `LocalAuth.java` | Room et hash sur un `Executor`, callbacks sur le thread UI, exceptions journalisées sans secret. |
| `SessionManager.java` | SharedPreferences `userId` + email, effacés au logout. |
| `SecureActivity.java` | Garde d'accès et `ownerId` local ; redirection vers Login avec `NEW_TASK | CLEAR_TASK`. |
| `AuthHelper.java` | Validation conservée et mapping des messages FR/AR. |

Après une inscription ou une connexion réussie, la session est créée puis `MainActivity` est ouverte avec `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`. Le bouton Back ne ramène pas à l'écran de connexion.

## Résultats exacts des tests

| Commande | Résultat |
|---|---|
| `.\gradlew.bat clean --no-daemon` | `BUILD SUCCESSFUL in 15s`, exit `0`. |
| `.\gradlew.bat assembleDebug --no-daemon` | `BUILD SUCCESSFUL in 47s`, exit `0`. |
| `.\gradlew.bat testDebugUnitTest --no-daemon` | `BUILD SUCCESSFUL in 25s`, exit `0`. |

Détail XML JVM (**18 tests, 0 failure, 0 error, 0 skipped**) :

| Fichier XML | tests | failures | errors | skipped | timestamp |
|---|---:|---:|---:|---:|---|
| `app/build/test-results/testDebugUnitTest/TEST-com.example.medirappel.AuthValidationTest.xml` | 4 | 0 | 0 | 0 | `2026-11-27T03:05:11.291Z` |
| `app/build/test-results/testDebugUnitTest/TEST-com.example.medirappel.LocalAuthLogicTest.xml` | 10 | 0 | 0 | 0 | `2026-11-27T03:05:11.312Z` |
| `app/build/test-results/testDebugUnitTest/TEST-com.example.medirappel.ReminderLogicTest.xml` | 4 | 0 | 0 | 0 | `2026-11-27T03:05:12.785Z` |

Couverture : validation email/mot de passe/confirmation (4), hachage et vérification PBKDF2, mauvais mot de passe, sels distincts, comparaison en temps constant, email existant, email inconnu, registration OK, login OK, normalisation email, round-trip Base64 (10), calcul du délai 45 s et règles de mot de passe (4).

Seul avertissement de compilation : dépréciation préexistante dans `LocaleHelper.java`, sans rapport avec l'authentification. Aucun émulateur lancé.

Contrôles complémentaires : **0** occurrence du nom de l'ancien backend distant, de son plugin Gradle, de `PLACEHOLDER`, de l'ancien libellé générique ou de `auth_error_unknown` dans `app/src`, les fichiers Gradle, `README.md` et `AUDIT_FINAL.md` ; parité des chaînes **115 / 115** entre FR et AR.

## Limites

- Comptes et rappels **uniquement sur l'appareil** : désinstaller l'app ou effacer ses données supprime tout.
- **Aucune récupération de mot de passe** : en cas d'oubli, créez un nouveau compte.
- Aucune synchronisation entre appareils ; les emails sont des identifiants locaux, non vérifiés.
- `allowMainThreadQueries()` reste actif pour les requêtes historiques des rappels ; le chemin d'authentification est entièrement sur `Executor`.
- Les tests instrumentés sont compilés mais non exécutés ici : la checklist ci-dessous reste nécessaire.

## Checklist manuelle courte

- [ ] **Désinstaller l'app** depuis les réglages Android (pour repartir d'une base propre).
- [ ] **Run** depuis Android Studio : l'écran Login s'ouvre, aucun message d'erreur.
- [ ] **Créer un compte** : nom, `dhouhahamdi93@gmail.com`, mot de passe valide, confirmation identique → « Compte créé » puis écran principal.
- [ ] **Écran principal** : le nom et les rappels s'affichent, aucune erreur de stockage.
- [ ] **Back** : le bouton Back ne ramène pas à l'écran de connexion ni à l'inscription.
- [ ] **Relance** : `force-stop` puis réouverture → accueil direct, sans ressaisie du mot de passe.
- [ ] **Logout** : Paramètres → « Se déconnecter » → Login ; un écran protégé ouvert direct est redirigé vers Login.
- [ ] **Login avec le même compte** : connexion réussie avec `dhouhahamdi93@gmail.com`.
- [ ] **Erreurs distinctes** : même email enregistré deux fois → « Cet email est déjà utilisé. » ; email inconnu → « Aucun compte ne correspond à cette adresse email. » ; mauvais mot de passe → « Mot de passe incorrect. »

## Changements expliqués en une ligne

- `AppDatabase.java` : version 4 et suppression des migrations fautives, avec recréation automatique de la base.
- `User.java` : champs obligatoires annotés pour correspondre au schéma généré par Room.
- `LocalAuth.java` : exceptions journalisées, constraint mapée sur l'email existant, autres cas sur le stockage local.
- `AuthLogic.java` : nouvelle erreur `STORAGE_ERROR` à la place de l'erreur générique.
- `AuthHelper.java` : messages FR/AR distincts pour email occupé et erreur de stockage.
- `PasswordHasher.java` : repli SHA-1 sur API 24/25 et Base64 indépendant de l'API 26.
- `strings.xml` (FR/AR) : libellé générique remplacé par « Erreur de stockage local. » et texte email aligné.
- `LocalAuthLogicTest.java` : tests Ajoutés pour register OK, login OK, email existant et round-trip Base64.
- `README.md` : documentation alignée sur l'authentification locale et le délai de 45 s.
- `AUDIT_FINAL.md` : cause réelle, correction, résultats exacts et checklist manuelle.
- `gradlew clean` : artefacts compilés périmés supprimés, l'IDE ne peut plus afficher l'ancienne classe.
