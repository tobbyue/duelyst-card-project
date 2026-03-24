package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;
import structures.logic.BoardLogic;

/**
 * Indicates that the user has clicked a non-interactive area of the game canvas.
 * <p>
 * Handles Story Card #12 (Cancel Selection): clicking on a non-interactive area
 * cancels any active unit or card selection and clears all board highlights.
 *
 * {
 *   messageType = "otherClicked"
 * }
 *
 * @author Dr. Richard McCreadie
 * @author Minghao
 */
public class OtherClicked implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		gameState.selectedUnit = null;
		gameState.selectedHandPosition = null;
		gameState.player1.drawHand(out);
		BoardLogic.clearSelection(out, gameState.board);
	}

}
