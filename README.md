# MediRappel

Application Android (Java) de rappels de médicaments avec authentification 100 % locale,
interface bilingue français/arabe et vérification des interactions médicamenteuses.

## Fonctionnalités

- Inscription et connexion locales (Room + PBKDF2), sans serveur ni configuration à fournir.
- Session persistée sur l'appareil, déconnexion et garde des écrans protégés.
- Rappel de prise de médicament programmé à **maintenant + 45 secondes**, avec repli approximatif sur Android 12+ si les alarmes exactes ne sont pas autorisées.
- Vérification des interactions entre deux médicaments à partir des notices openFDA, avec avertissement : le résultat ne remplace pas l'avis d'un médecin ou d'un pharmacien.
- Traduction facultative de l'extrait anglais vers l'arabe.
- Interface français/arabe avec mise en page RTL.
- Champs texte simplifiés : aucun clavier stylet système ne vient perturber la saisie.

## API utilisées

- **openFDA** : `GET https://api.fda.gov/drug/label.json` (`FdaApi.java`), via Retrofit + Gson. La requête recherche le nom générique ou la marque du médicament et lit le champ `drug_interactions` de la première notice. Une réponse 404, vide ou en échec réseau est traitée explicitement.
- **MyMemory** : `GET https://api.mymemory.translated.net/get` (`MyMemoryApi.java`), via Retrofit + Gson. Le paramètre `langpair=en|ar` traduit l'extrait openFDA vers l'arabe ; cet appel est facultatif et n'influence pas l'authentification ni les rappels.
- Aucune clé d'API n'est nécessaire pour ces deux services publics.

## Architecture

- **`LocalAuth.java`** : inscription et connexion locales via Room. Les accès base et les hashes tournent hors du thread principal, les callbacks reviennent sur le thread UI ; email déjà utilisé, email inconnu, mauvais mot de passe et erreur de stockage sont distingués.
- **`PasswordHasher.java`** : sel aléatoire et PBKDF2 (SHA-256 sur API 26+, repli SHA-1 à 120 000 itérations sur API 24/25), comparaison en temps constant ; le mot de passe n'est jamais journalisé.
- **`SessionManager.java`** : session persistée dans `SharedPreferences` (identifiant du compte et email), effacée lors de la déconnexion.
- **`ReminderScheduler.java`** : programme une seule alarme à « enregistrement + 45 s » avec `setAlarmClock` si l'autorisation est donnée, sinon un repli approximatif ; réarme au redémarrage et annule les anciennes alarmes pour éviter les doublons.
- **`FdaApi.java`** : client Retrofit/Gson vers openFDA qui lit `drug_interactions` ; `MyMemoryApi.java` traduit facultativement l'extrait anglais vers l'arabe.

Les décisions sans dépendance Android (`AuthLogic`, `InteractionOutcome`, `MedicationNameValidator` et `ReminderScheduler.triggerAtFrom`) sont testables en JVM.

## Comment lancer

Prérequis : Android Studio avec un SDK Android configuré dans `local.properties` (fichier ignoré par Git).

```powershell
./gradlew.bat assembleDebug
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebugAndroidTest
```

Ouvrez le projet dans Android Studio et lancez l'application sur un émulateur ou un appareil.
`assembleDebugAndroidTest` compile les tests instrumentés sans les exécuter : aucun émulateur n'est nécessaire pour les commandes ci-dessus.

## Comment installer l'APK

L'APK généré se trouve ici (fichier ignoré par Git, non commité) :

```text
C:\Users\hamdi\Downloads\medirappel\app\build\outputs\apk\debug\app-debug.apk
```

- **Copie manuelle** : transférez le fichier sur l'appareil, ouvrez-le depuis un gestionnaire de fichiers et autorisez l'installation depuis cette source quand Android le demande.
- **Via adb** (Android SDK `platform-tools`) :

```powershell
adb install -r C:\Users\hamdi\Downloads\medirappel\app\build\outputs\apk\debug\app-debug.apk
```

Il s'agit d'un APK **debug** signé avec la clé de debug d'Android Studio : convient aux tests, pas à une publication sur le Play Store. Si une version précédente a été installée avec une autre signature, désinstallez-la d'abord manuellement.

## Limites

- Les comptes et les rappels sont stockés **uniquement sur l'appareil** : désinstaller l'application ou effacer ses données supprime tout.
- **Aucune récupération de mot de passe** : en cas d'oubli, créez un nouveau compte local.
- Aucune synchronisation entre appareils ; les emails sont des identifiants locaux, non vérifiés.
- Une seule session locale est conservée à la fois : connecter un autre compte remplace la session courante dans `SessionManager`.
- Sur API 24/25, le hachage utilise automatiquement PBKDF2WithHmacSHA1 (120 000 itérations) ; SHA-256 est utilisé sur API 26+.
- Internet ne sert qu'à la vérification des interactions et à la traduction ; l'authentification fonctionne en mode avion.
- Les notifications exactes à 45 secondes nécessitent l'autorisation « alarmes et rappels » sur Android 12+ ; sinon un rappel approximatif est utilisé.

## Règles métier

- Chaque rappel est associé à l'identifiant du compte local (`ownerId`).
- La notification est programmée avec `AlarmManager.setAlarmClock` à **maintenant + 45 secondes** après chaque enregistrement ou modification.
- Le destinataire affiche le nom du médicament dans la langue choisie (fr/ar).
