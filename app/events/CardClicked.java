package events;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.logic.BoardLogic;

/**
 * Indicates that the user has clicked a card in their hand.
 *
 * Handles Story Card #22: selecting a creature card highlights all empty board tiles
 * adjacent (8-directional) to a friendly unit in green (mode 2).
 * Handles Story Card #12: re-clicking the same card deselects it.
 *
 * {
 *   messageType = "cardClicked"
 *   position = <hand index position [1-6]>
 * }
 *
 * @author Dr. Richard McCreadie
 * @author Minghao
 */
public class CardClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        // Reset card drawn
        gameState.player1.drawHand(out);

        if (gameState.gameOver || !gameState.player1Turn || gameState.animationInProgress)
            return;

        int handPosition = message.get("position").asInt(); // 1-indexed

        // Re-click same card -> deselect and stop
        if (Integer.valueOf(handPosition).equals(gameState.selectedHandPosition)) {
            gameState.selectedHandPosition = null;
            gameState.selectedUnit = null;
            BoardLogic.clearSelection(out, gameState.board);
            return;
        }

        // Clear previous selections
        gameState.selectedUnit = null;
        BoardLogic.clearSelection(out, gameState.board);

        List<Card> hand = gameState.getPlayer1().getHand();
        int index = handPosition - 1;

        Card card = hand.get(index);
        BasicCommands.drawCard(out, card, handPosition, 1);

        // Keep the card selected so the player can preview targets
        gameState.selectedHandPosition = handPosition;

        // Use the existing target/highlight flow
        gameState.highlightedTiles = card.getTargets(gameState.player1, gameState.board);
        card.highlightTargets(out, gameState.player1, gameState.board);

        // Show warnings but still allow preview/highlighting of targets
        if (!gameState.player1.enoughMana(out, card.getManacost())) {
            BasicCommands.addPlayer1Notification(out, "Not enough mana to play this card.", 2);
        } else if (gameState.highlightedTiles == null || gameState.highlightedTiles.isEmpty()) {
            BasicCommands.addPlayer1Notification(out, "No valid target.", 2);
        }
    }
}