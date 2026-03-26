package events;


import java.util.HashSet;
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
        if (gameState.gameOver || !gameState.player1Turn || gameState.unitMoving || gameState.animationInProgress)
            return;

        int tilex = message.get("tilex").asInt();
        int tiley = message.get("tiley").asInt();

        // Get information
        Board board = gameState.board;
        Tile clickedTile = gameState.getBoard().getTile(tilex, tiley);
        Card selectedCard = null;
        Integer cardIndex = null;

        // Clear board selection
        BoardLogic.clearSelection(out, board);

        // Ensure highlighted tile cache exists.
        // Do NOT clear it here, because card targeting relies on the previously
        // highlighted valid targets from cardClicked / unit selection.
        if (gameState.highlightedTiles == null) { //TODO just set it as an emtpy hashset on initialisation
            gameState.highlightedTiles = new HashSet<>();
        }

        // Check if a card is selected in hand
        if (gameState.selectedHandPosition != null) {
            int handPosition = gameState.selectedHandPosition;
            cardIndex = handPosition - 1;
            selectedCard = gameState.getPlayer1().getHand().get(cardIndex);
        }

        // Card click flow
        if (selectedCard != null) {
            if (gameState.highlightedTiles.contains(clickedTile)) {
                if (gameState.player1.enoughMana(out, selectedCard.getManacost())) {
                    gameState.getPlayer1().useCard(out, gameState,
                            cardIndex, clickedTile, selectedCard.getManacost());
                }
            }

            // Do not fall through into unit logic after a card click attempt
            gameState.selectedHandPosition = null;
            gameState.highlightedTiles.clear();
            gameState.player1.destroyHand(out);
            gameState.player1.drawHand(out);
            return;
        }

        // Clicked on a unit (no card selected)
        else if (clickedTile.getUnit() != null) {
            Unit clickedUnit = clickedTile.getUnit();

            // 1) Click own unit: allow selection if the unit still has any action left
            if (clickedUnit.getOwner() == gameState.getPlayer1()) {
                boolean canMove = !clickedUnit.hasMoved;
                boolean canAttack = !clickedUnit.hasAttacked;

                if (!canMove && !canAttack) {
                    BasicCommands.addPlayer1Notification(out, "Unit has no actions left.", 1);
                    gameState.player1.drawHand(out);
                    return;
                }

                gameState.selectedHandPosition = null;
                gameState.selectedUnit = clickedUnit;
                gameState.highlightedTiles.clear();

                // highlight selected unit tile
                BasicCommands.drawTile(out, clickedTile, 1);

                // highlight movement range if movement is still available
                if (canMove) {
                    gameState.highlightedTiles = BoardLogic.findValidMovement(gameState.selectedUnit.getCurrentTile(), gameState.selectedUnit, gameState.getBoard());
                    BoardLogic.highlightMovement(out, clickedTile, clickedUnit, board);
                }

                // highlight valid enemy targets if attack is still available
                if (canAttack) {
                    BoardLogic.highlightAttackTiles(out, clickedTile, clickedUnit, board);
                    // Highlight units attackable from a distance as well
                    for (Tile tile : gameState.highlightedTiles) {
                        BoardLogic.highlightAttackTiles(out, tile, clickedUnit, board);
                    }

                    gameState.player1.drawHand(out);
                }
            }


            // 2) Click enemy while a friendly unit is selected
            else if (gameState.selectedUnit != null
                    && gameState.selectedUnit.getOwner() == gameState.getPlayer1()
                    && !gameState.selectedUnit.hasAttacked) {

                Unit attacker = gameState.selectedUnit;

                Tile attackerTile = attacker.getCurrentTile();

                // Case A: already in direct attack range -> attack immediately
                if (BoardLogic.findValidAttackUnits(attackerTile, attacker, board).contains(clickedTile))
                    CombatLogic.resolveCombat(out, gameState, attacker, clickedUnit);
                    // Case B: not in range, but can move to a tile from which the target becomes attackable
                else if (!attacker.hasMoved) {
                    Tile autoAttackDestination = CombatLogic.findAutoAttackDestination(attacker, clickedTile, board);

                    if (autoAttackDestination != null) {
                        gameState.startPendingAttackAfterMove(attacker, clickedTile);
                        BoardLogic.moveSelectedUnit(out, gameState, autoAttackDestination, board);
                    }
                }
            }

            // 3) Enemy click with no usable selection: do nothing
            return;
        }


        // Empty tile + selected unit => try move
        else if (gameState.getSelectedUnit() != null && gameState.highlightedTiles.contains(clickedTile)) {
            BoardLogic.moveSelectedUnit(out, gameState, clickedTile, board);
            gameState.player1.drawHand(out);
        }

        // Deselect card
        gameState.selectedHandPosition = null;
        gameState.selectedUnit = null;
        gameState.highlightedTiles.clear();
        gameState.player1.drawHand(out);
    }
}


