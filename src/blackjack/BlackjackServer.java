package blackjack;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Serveur de jeu de Blackjack multijoueur.
 * Gère les connexions, les tours de jeu et la logique du croupier.
 */
public class BlackjackServer {
    private static final int PORT = 5555;
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 4;
    private static final int WAIT_TIME_MS = 10000; // 10 secondes d'attente
    private static final int RESTART_DELAY_MS = 5000; // 5 secondes avant redémarrage

    private final List<ClientHandler> players = new CopyOnWriteArrayList<>();
    private Deck deck;
    private List<Card> dealerHand = new ArrayList<>();
    private volatile boolean gameInProgress = false;

    public static void main(String[] args) {
        new BlackjackServer().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur Blackjack démarré sur le port " + PORT);

            // Thread pour gérer le démarrage du jeu
            new Thread(this::manageGameLifecycle).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                if (players.size() < MAX_PLAYERS && !gameInProgress) {
                    ClientHandler handler = new ClientHandler(clientSocket, players.size() + 1);
                    players.add(handler);
                    new Thread(handler).start();
                    System.out.println("Joueur " + handler.getPlayerId() + " connecté.");
                    broadcast("Un nouveau joueur a rejoint la partie. (" + players.size() + "/" + MAX_PLAYERS + ")");
                } else {
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println("Le jeu est complet ou une partie est en cours. Réessayez plus tard.");
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }

    /**
     * Gère le cycle de vie du jeu : attente des joueurs, début, fin et redémarrage.
     */
    private void manageGameLifecycle() {
        while (true) {
            try {
                if (players.size() < MIN_PLAYERS) {
                    if (players.size() > 0) {
                        broadcast("En attente de joueurs... (Minimum " + MIN_PLAYERS + " requis)");
                    }
                    Thread.sleep(3000);
                    continue;
                }

                // Attente de 10 secondes ou démarrage immédiat si 4 joueurs
                long startTime = System.currentTimeMillis();
                broadcast("La partie commence bientôt (10s d'attente ou 4 joueurs)...");
                
                while (players.size() < MAX_PLAYERS && (System.currentTimeMillis() - startTime) < WAIT_TIME_MS) {
                    Thread.sleep(1000);
                }

                if (players.size() >= MIN_PLAYERS) {
                    playGame();
                }

                // Pause avant la prochaine partie
                Thread.sleep(RESTART_DELAY_MS);
                resetGame();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void playGame() {
        gameInProgress = true;
        broadcast("\n=== NOUVELLE PARTIE ===\n");
        deck = new Deck();
        deck.shuffle();

        dealerHand = new ArrayList<>();
        dealerHand.add(deck.draw());
        dealerHand.add(deck.draw());

        // Distribution initiale
        for (ClientHandler player : players) {
            player.getHand().clear();
            player.getHand().add(deck.draw());
            player.getHand().add(deck.draw());
            player.setBusted(false);
            player.setStanding(false);
        }

        // Affichage initial
        for (ClientHandler player : players) {
            player.sendMessage("Vos cartes: " + handToString(player.getHand()) + " (score: " + calculateScore(player.getHand()) + ")");
            player.sendMessage("Carte visible du croupier: " + dealerHand.get(0));
        }

        // Tour de chaque joueur
        for (ClientHandler player : players) {
            if (player.isConnected()) {
                handlePlayerTurn(player);
            }
        }

        // Tour du croupier
        playDealerTurn();

        // Résultats
        announceResults();

        gameInProgress = false;
    }

    private void handlePlayerTurn(ClientHandler player) {
        broadcast("\nC'est au tour du [Joueur " + player.getPlayerId() + "]");
        player.sendMessage("\nC'est votre tour. Tirer ou rester?");

        while (!player.isStanding() && !player.isBusted()) {
            String choice = player.receiveMessage();
            if (choice == null) break;

            if (choice.equalsIgnoreCase("tirer")) {
                Card drawn = deck.draw();
                player.getHand().add(drawn);
                int score = calculateScore(player.getHand());
                
                String update = "[Joueur " + player.getPlayerId() + "] a tiré: " + drawn + " (score: " + score + ")";
                broadcastExcept(update, player);
                player.sendMessage("Vous avez tiré: " + drawn + " (score: " + score + ")");

                if (score > 21) {
                    player.setBusted(true);
                    player.sendMessage("Vous avez dépassé 21, perdu!");
                    broadcastExcept("[Joueur " + player.getPlayerId() + "] a dépassé 21!", player);
                } else {
                    player.sendMessage("Tirer ou rester?");
                }
            } else if (choice.equalsIgnoreCase("rester")) {
                player.setStanding(true);
                int score = calculateScore(player.getHand());
                broadcast("[Joueur " + player.getPlayerId() + "] reste avec " + score + ".");
            } else {
                player.sendMessage("Commande invalide. Tapez 'tirer' ou 'rester'.");
            }
        }
    }

    private void playDealerTurn() {
        broadcast("\n=== RÉSULTAT DU CROUPIER ===");
        int dealerScore = calculateScore(dealerHand);
        
        while (dealerScore < 17) {
            Card drawn = deck.draw();
            dealerHand.add(drawn);
            dealerScore = calculateScore(dealerHand);
        }
        
        broadcast("Croupier: " + handToString(dealerHand) + " (score: " + dealerScore + ")");
    }

    private void announceResults() {
        int dealerScore = calculateScore(dealerHand);
        broadcast("\n=== VOTRE RÉSULTAT ===");

        for (ClientHandler player : players) {
            int playerScore = calculateScore(player.getHand());
            String result;

            if (player.isBusted()) {
                result = "PERDU (Bust)";
            } else if (dealerScore > 21) {
                result = "GAGNÉ (Le croupier a dépassé 21)";
            } else if (playerScore > dealerScore) {
                result = "GAGNÉ";
            } else if (playerScore < dealerScore) {
                result = "PERDU";
            } else {
                result = "ÉGALITÉ";
            }

            player.sendMessage("Votre score: " + playerScore + " | Croupier: " + dealerScore + " → " + result + "!");
        }
    }

    private void resetGame() {
        // Supprimer les joueurs déconnectés
        players.removeIf(p -> !p.isConnected());
        if (players.isEmpty()) {
            gameInProgress = false;
        }
    }

    private void broadcast(String message) {
        for (ClientHandler player : players) {
            player.sendMessage(message);
        }
        System.out.println(message);
    }

    private void broadcastExcept(String message, ClientHandler exceptPlayer) {
        for (ClientHandler player : players) {
            if (player != exceptPlayer) {
                player.sendMessage(message);
            }
        }
        System.out.println(message);
    }

    public static int calculateScore(List<Card> hand) {
        int score = 0;
        int aces = 0;
        for (Card card : hand) {
            score += card.getPoints();
            if (card.getValue().equals("A")) {
                aces++;
            }
        }
        while (score > 21 && aces > 0) {
            score -= 10;
            aces--;
        }
        return score;
    }

    private String handToString(List<Card> hand) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            sb.append(hand.get(i));
            if (i < hand.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * Gère la communication avec un client spécifique.
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final int playerId;
        private final List<Card> hand = new ArrayList<>();
        private BufferedReader in;
        private PrintWriter out;
        private boolean standing = false;
        private boolean busted = false;
        private volatile boolean connected = true;

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                connected = false;
            }
        }

        @Override
        public void run() {
            try {
                sendMessage("Connecté au serveur Blackjack! (Joueur " + playerId + ")");
                while (connected) {
                    // Le thread reste vivant pour maintenir la connexion
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                // Interruption normale
            } finally {
                disconnect();
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        public String receiveMessage() {
            try {
                return in.readLine();
            } catch (IOException e) {
                disconnect();
                return null;
            }
        }

        private void disconnect() {
            connected = false;
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            players.remove(this);
            System.out.println("Joueur " + playerId + " déconnecté.");
        }

        public int getPlayerId() { return playerId; }
        public List<Card> getHand() { return hand; }
        public boolean isStanding() { return standing; }
        public void setStanding(boolean standing) { this.standing = standing; }
        public boolean isBusted() { return busted; }
        public void setBusted(boolean busted) { this.busted = busted; }
        public boolean isConnected() { return connected; }
    }
}
