package events;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Board;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.unittypes.Unit;
import structures.logic.BoardLogic;
import structures.logic.CombatLogic;

/**
 * Indicates that the user has clicked a tile on the game canvas.
 * <p>
 * Handles Story Card #3: when the human player clicks one of their units, the unit is
 * selected and its valid movement range is highlighted in white.
 * <pre>
 * {
 *   messageType = "tileClicked"
 *   tilex = &lt;x index of the tile&gt;
 *   tiley = &lt;y index of the tile&gt;
 * }
 * </pre>
 *
 * @author Dr. Richard McCreadie
 * @author Minghao
 */
public class TileClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        // Give nothing if clicked outside of turn
        // or if a unit is moving
        if (!gameState.player1Turn) {
            BasicCommands.addPlayer1Notification(out, "It is not your turn.", 2);
            return;
        } else if (gameState.unitMoving) {
            BasicCommands.addPlayer1Notification(out, "A unit is already moving.", 2);
            return;
        }

        int tilex = message.get("tilex").asInt();
        int tiley = message.get("tiley").asInt();

        // Get information
        Board board = gameState.board;
        Tile clickedTile = gameState.getBoard().getTile(tilex, tiley);
        Card selectedCard = null;
        Integer cardIndex = null;

        // Clear board selection
        BoardLogic.clearSelection(out, board);

        // Check if a card is selected in hand
        if (gameState.selectedHandPosition != null) {
            int handPosition = gameState.selectedHandPosition;
            cardIndex = handPosition - 1;
            selectedCard = gameState.getPlayer1().getHand().get(cardIndex);
        }

        // If a card is selected and a valid target is clicked, use the card on the target
        if (selectedCard != null && gameState.highlightedTiles.contains(clickedTile)) {

            // Check for mana first
            if (gameState.player1.enoughMana(out, selectedCard.getManacost())) {
                BasicCommands.deleteCard(out, gameState.selectedHandPosition);
                gameState.getPlayer1().useCard(out, gameState,
                        cardIndex, clickedTile, selectedCard.getManacost());
            }
        }

        // Clicked on a unit (no card selected)
        else if (clickedTile.getUnit() != null) {
            Unit clickedUnit = clickedTile.getUnit();

            // If a friendly unit is already selected and player clicks an enemy -> try attack
            if (gameState.selectedUnit != null && clickedUnit.getOwner() != gameState.getPlayer1() &&
                !clickedUnit.hasAttacked) {
                CombatLogic.tryAttackSelectedUnit(out, gameState, clickedTile); //TODO rename method
            }

            // Select friendly unit for movement if unit has not moved
            else if (clickedUnit.getOwner() == gameState.getPlayer1() && !clickedUnit.hasMoved) {
                gameState.selectedHandPosition = null;
                gameState.selectedUnit = clickedUnit;
                BasicCommands.drawTile(out, clickedTile, 1); // white highlight
                BoardLogic.highlightMovement(out, clickedTile, clickedUnit, board);
                gameState.highlightedTiles = BoardLogic.findValidMovement(clickedTile, gameState.selectedUnit, board);

            } else if (clickedUnit.getOwner() == gameState.getPlayer1()  &&
                        clickedUnit.hasAttacked) {
                BasicCommands.addPlayer1Notification(out, "Unit has moved.", 1);
            }

            // Else, enemy unit is clicked

        }


        // Empty tile + selected unit => try move
        else if (gameState.getSelectedUnit() != null && gameState.highlightedTiles.contains(clickedTile)) {
            BoardLogic.moveSelectedUnit(out, gameState, clickedTile, board);
        }

        // Deselect card
        gameState.selectedHandPosition = null;
        gameState.player1.drawHand(out);
    }
}
