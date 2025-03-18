# ToDo's

## General

- ~~SecureBaseView -> NotSecureBaseView~~
    - ~~Umbenennen?~~
        - ~~IsAuthentificatedBaseView und IsNotAuthentificatedBaseView~~
- ~~Validation in eine Helper Klasse auslagern?~~
    - ~~Message Dateien anpassen.~~
- ~~Message Konstante~~
    - ~~Überall verwenden~~
- ~~Home~~
    - ~~Logo~~
    - ~~User~~
        - ~~Login/Logout~~
        - ~~Language~~
        - Manage Account
            - Page
        - ~~Online Status~~
            - Database
            - Change
            - Refresh on Change

## Login

- Login fertigstellen
    - ~~Grundgerüst erstellen~~
    - ~~Database verbinden~~
    - ~~Default Button~~
    - ~~Check LoggedIn~~
    - ~~CardComponent -> LumoUtility~~
    - ~~Fehlermeldungen~~
        - ~~Stylen~~
    - ~~Remember Me~~
    - Attempts Limit

## Forgot Password

- ~~Database~~
- ~~Service~~
- ~~Email~~
- ~~Page~~
    - ~~Forgot Password~~
        - ~~Background~~
    - ~~Reset Password~~
        - ~~Background~~
- ~~Create Token~~
    - ~~Old Token -> Inactive~~

## Registration

- ~~Registration fertigstellen~~
    - Entity Annotations
        - Multi Language?
    - ~~Verification~~
        - ~~Database~~
        - ~~Service~~
        - ~~Email~~
        - ~~Page~~
        - ~~Create Token~~
            - ~~Old Token -> Inactive~~

## Database

- ~~H2~~
    - ~~File Based~~

## EMail

- ~~Versenden~~
- ~~Queue~~
- ~~Database~~
- ~~Thread Safe~~
- ~~Failed~~
    - ~~Admin Notification~~
- ~~Design~~
    - ~~Verification~~
    - ~~Password Reset~~

## Notification

- ~~Anzahl (Singleton)~~
- ~~Verbessern/Verschönern~~
    - ~~Länge begrenzen~~
    - ~~Hervorhebungen~~
    - Button
    - ~~Color~~
    - ~~Bei Navigation Event, Close?~~

## Enums

- ~~RoleType~~
- ~~ConfigEntry~~
- ~~Gender~~
- ~~EmailStatus~~
- ~~EmailType~~

## Logger

- ~~Logger integrieren~~

## Multi Langauge

- Views
    - ~~MainView~~
    - ~~LoginView~~
    - AdminView
    - ~~RegistrationView~~
- ~~Speichern und Setzen~~
- Without Reload
    - Überall umsetzen
    - ~~Verbessern?~~

## Config Table

- ~~Anlegen~~
- ~~Benutzen~~
- ~~rememberMeServices~~
    - ~~Duration~~
    - ~~SecretKey~~

## Max Sessions

- Informieren
    - Umsetzen

## Maintenance Mode

- Informieren
    - Umsetzen

## SQL

```
DROP ALL OBJECTS;
```

## Backup

```
/* CardComponent */
.cardComponent {
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0px 4px 10px rgba(0, 0, 0, 0.2);
}
```