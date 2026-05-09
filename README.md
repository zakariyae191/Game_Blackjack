# Blackjack Multijoueur

Application Java de Blackjack multijoueur avec serveur TCP, client console et client graphique Swing.

## Structure

```text
.
+-- src/
|   +-- blackjack/
|       +-- BlackjackServer.java
|       +-- BlackjackClient.java
|       +-- BlackjackGUIClient.java
|       +-- Card.java
|       +-- Deck.java
+-- out/                  # Genere par la compilation
+-- build.bat             # Compile le projet
+-- lancer_serveur.bat    # Compile puis lance le serveur
+-- lancer_client.bat     # Lance le client console
+-- lancer_gui.bat        # Lance le client graphique
```

## Compilation

```bat
build.bat
```

La compilation place les fichiers `.class` dans le dossier `out`.

## Execution

Demarrer le serveur :

```bat
lancer_serveur.bat
```

Demarrer un client graphique :

```bat
lancer_gui.bat
```

Demarrer un client console :

```bat
lancer_client.bat
```

## Regles

- La partie commence automatiquement quand au moins 2 joueurs sont connectes.
- Le serveur accepte jusqu'a 4 joueurs par partie.
- Le croupier tire jusqu'a atteindre au moins 17.
- Le client console utilise les commandes `tirer` et `rester`.
