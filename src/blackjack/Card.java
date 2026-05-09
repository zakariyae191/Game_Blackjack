package blackjack;

/**
 * Représente une carte à jouer dans le jeu de Blackjack.
 */
public class Card {
    private final String suit;  // Coeur, Carreau, Trèfle, Pique
    private final String value; // 2-10, J, Q, K, A

    public Card(String suit, String value) {
        this.suit = suit;
        this.value = value;
    }

    /**
     * Calcule les points de la carte.
     * Les figures (J, Q, K) valent 10.
     * L'As (A) vaut 11 par défaut (la logique de réduction à 1 est gérée dans le score de la main).
     * Les autres valent leur valeur faciale.
     */
    public int getPoints() {
        if (value.equals("J") || value.equals("Q") || value.equals("K")) {
            return 10;
        } else if (value.equals("A")) {
            return 11;
        } else {
            return Integer.parseInt(value);
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        String valStr = value;
        if (value.equals("J")) valStr = "Valet";
        if (value.equals("Q")) valStr = "Dame";
        if (value.equals("K")) valStr = "Roi";
        if (value.equals("A")) valStr = "As";
        
        return valStr + " de " + suit;
    }
}
