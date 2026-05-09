package blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Représente un paquet de 52 cartes.
 */
public class Deck {
    private final List<Card> cards;

    public Deck() {
        cards = new ArrayList<>();
        String[] suits = {"Coeur", "Carreau", "Trèfle", "Pique"};
        String[] values = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

        for (String suit : suits) {
            for (String value : values) {
                cards.add(new Card(suit, value));
            }
        }
    }

    /**
     * Mélange le paquet de cartes.
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * Pioche une carte du dessus du paquet.
     * @return La carte piochée.
     */
    public Card draw() {
        if (cards.isEmpty()) {
            return null; // Devrait être géré par un nouveau mélange si nécessaire
        }
        return cards.remove(0);
    }
}
