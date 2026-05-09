package blackjack;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Client pour le jeu de Blackjack.
 * Se connecte au serveur et gère les entrées/sorties.
 */
public class BlackjackClient {
    private static final int PORT = 5555;
    private String host;
    private Socket socket;
    private volatile boolean running = true;

    public BlackjackClient(String host) {
        this.host = host;
    }

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        new BlackjackClient(host).start();
    }

    public void start() {
        try {
            socket = new Socket(host, PORT);
            System.out.println("Connecté au serveur Blackjack sur " + host + ":" + PORT);

            // Thread pour lire les messages du serveur
            Thread readerThread = new Thread(this::readFromServer);
            readerThread.start();

            // Thread principal pour lire l'entrée utilisateur
            writeToServer();

        } catch (IOException e) {
            System.err.println("Impossible de se connecter au serveur : " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    /**
     * Lit les messages envoyés par le serveur et les affiche sur la console.
     */
    private void readFromServer() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String serverMessage;
            while (running && (serverMessage = in.readLine()) != null) {
                System.out.println(serverMessage);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Connexion perdue avec le serveur.");
            }
        } finally {
            running = false;
        }
    }

    /**
     * Lit l'entrée de la console et l'envoie au serveur.
     */
    private void writeToServer() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {
            
            while (running) {
                if (scanner.hasNextLine()) {
                    String userInput = scanner.nextLine();
                    out.println(userInput);
                    if (userInput.equalsIgnoreCase("quitter")) {
                        running = false;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Fin silencieuse
        }
    }

    private void closeConnection() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
