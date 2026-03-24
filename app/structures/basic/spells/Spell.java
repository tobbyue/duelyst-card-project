package structures.basic.spells;
import akka.actor.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Board;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.players.*;
import structures.logic.BoardLogic;

import java.util.Set;

public abstract class Spell {
    @JsonIgnore
    Card card = null;

    @JsonIgnore
    public Spell() {}


    public Card getCard() {
        return this.card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public Set<Tile> validTargets(Player player, Board board) {return null;}

    public void highlightTargets(ActorRef out, Player player, Board board) {
        Set<Tile> validTargets = validTargets(player, board);
        // base class returns null; subclasses may also return null if no targets exist
        if (validTargets == null || validTargets.isEmpty()) {
            BasicCommands.addPlayer1Notification(out, "No valid tiles!", 2);
            return;
        }
        for (Tile tile : validTargets) {
            BasicCommands.drawTile(out, tile, 2);
            BoardLogic.blink();
        }
    }


    public void cast(ActorRef out, GameState gameState,
                     Player player, Tile clickedTile) {}
}

