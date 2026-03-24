package structures.basic;

import structures.basic.players.*;
import utils.OrderedCardLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    public List<Card> cards; // Making this public, because I don't think any of this
                             // is accessible outside of the player's deck attribute?

    public Deck(Player player) {
        if (player instanceof HumanPlayer) {
            cards = OrderedCardLoader.getPlayer1Cards(2);
        } else if (player instanceof AIPlayer) {
            cards = OrderedCardLoader.getPlayer2Cards(2);
        } else {
            throw new IllegalArgumentException("Invalid player type.");
        }
        Collections.shuffle(cards);

        for (Card card : cards) {
            card.setSpell();
            if (card.getSpell() != null)
                    card.getSpell().setCard(card);
        }

    }

    public Deck() {
        this.cards = new ArrayList<>();
    }

}
