package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.logic.BoardLogic;
import structures.basic.Tile;
import structures.basic.UnitAnimationType;
import structures.basic.unittypes.Unit;

/**
 * Indicates that a unit instance has stopped moving.
 * The event reports the unique id of the unit.
 */
public class UnitStopped implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        // Clear all highlights and selection after unit finishes moving
        BoardLogic.clearSelection(out, gameState.board);
        gameState.selectedUnit = null;

		int unitid = message.get("id").asInt();

		if (!gameState.unitMoving) return;
		if (gameState.movingUnit == null) return;
		if (gameState.moveTargetTile == null) return;

		Unit movingUnit = gameState.movingUnit;

		if (movingUnit.getId() != unitid) return;

		Tile oldTile = gameState.getBoard().getTile(
				movingUnit.getPosition().getTilex(),
				movingUnit.getPosition().getTiley()
		);

		Tile targetTile = gameState.moveTargetTile;

		if (oldTile != null) {
			oldTile.setUnit(null);
		}

		movingUnit.setPositionByTile(targetTile);
		targetTile.setUnit(movingUnit);

		movingUnit.hasMoved = true;
		BasicCommands.playUnitAnimation(out, movingUnit, UnitAnimationType.idle);

		gameState.movingUnit = null;
		gameState.moveTargetTile = null;
		gameState.unitMoving = false;
	}
}