# MediRappel

Application Android (Java) de rappels de médicaments avec authentification 100 % locale,
interface bilingue français/arabe et vérification des interactions médicamenteuses.

## Fonctionnalités

- Inscription et connexion locales (Room + PBKDF2), sans serveur ni configuration à fournir.
- Session persistée sur l'appareil, déconnexion et garde des écrans protégés.
- Rappel de prise de médicament programmé à **maintenant + 45 secondes**, avec repli approximatif sur Android 12+ si les alarmes exactes ne sont pas autorisées.
- Vérification des interactions médicamenteuses via openFDA, avec avertissement : le résultat ne remplace pas l'avis d'un médecin ou d'un pharmacien.
- Interface français/arabe avec mise en page RTL.
- Champs texte simplifiés : aucun clavier stylet système ne vient perturber la saisie.

## Comment lancer

Prérequis : Android Studio avec un SDK Android configuré dans `local.properties` (fichier ignoré par Git).

```powershell
./gradlew.bat assembleDebug
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebugAndroidTest
```

Ouvrez le projet dans Android Studio et lancez l'application sur un émulateur ou un appareil.
`assembleDebugAndroidTest` compile les tests instrumentés sans les exécuter : aucun émulateur n'est nécessaire pour les commandes ci-dessus.

## Limites

- Les comptes et les rappels sont stockés **uniquement sur l'appareil** : désinstaller l'application ou effacer ses données supprime tout.
- **Aucune récupération de mot de passe** : en cas d'oubli, créez un nouveau compte local.
- Aucune synchronisation entre appareils ; les emails sont des identifiants locaux, non vérifiés.
- Sur API 24/25, le hachage utilise automatiquement PBKDF2WithHmacSHA1 (120 000 itérations) ; SHA-256 est utilisé sur API 26+.
- Internet ne sert qu'à la vérification des interactions et à la traduction ; l'authentification fonctionne en mode avion.

## Règles métier

- Chaque rappel est associé à l'identifiant du compte local (`ownerId`).
- La notification est programmée avec `AlarmManager.setAlarmClock` à **maintenant + 45 secondes** après chaque enregistrement ou modification.
- Le destinataire affiche le nom du médicament dans la langue choisie (fr/ar).
