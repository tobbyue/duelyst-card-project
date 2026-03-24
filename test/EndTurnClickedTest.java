import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import commands.BasicCommands;
import commands.DummyTell;
import events.EndTurnClicked;
import events.Initalize;
import play.libs.Json;
import structures.GameState;

/**
 * JUnit tests for Story Card #6:
 * At the start of each turn the active player's mana replenishes to the correct
 * value and the front-end display is updated.
 *
 * Mana progression mirrors standard Duelyst rules:
 *   Player's Nth turn → mana = min(N + 1, 9)
 *   e.g. turn 1 → 2 mana, turn 2 → 3 mana, … capped at 9.
 *
 * Test setup mirrors TileClickedTest: BasicCommands.altTell is overridden and
 * Initalize is run first to establish a valid game state.
 *
 * @author Minghao
 */
public class EndTurnClickedTest {

    // -----------------------------------------------------------------------
    // Recording DummyTell — captures every BasicCommands message for inspection
    // -----------------------------------------------------------------------

    static class RecordingTell implements DummyTell {
        final List<ObjectNode> messages = new ArrayList<>();

        @Override
        public void tell(ObjectNode message) {
            messages.add(message.deepCopy());
        }

        /** Returns true if any message of the given type was ever sent. */
        boolean hasMessageType(String type) {
            for (ObjectNode m : messages) {
                if (type.equals(m.get("messagetype").asText())) return true;
            }
            return false;
        }

        /**
         * Returns the mana value from the most recent message of the given type
         * (e.g. "setPlayer1Mana" or "setPlayer2Mana"), or -1 if not found.
         */
        int lastManaFor(String type) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                ObjectNode m = messages.get(i);
                if (type.equals(m.get("messagetype").asText())) {
                    return m.get("player").get("mana").asInt();
                }
            }
            return -1;
        }
    }

    // -----------------------------------------------------------------------
    // Shared test state
    // -----------------------------------------------------------------------

    private RecordingTell recorder;
    private GameState gameState;
    private EndTurnClicked processor;

    @Before
    public void setUp() {
        recorder = new RecordingTell();
        BasicCommands.altTell = recorder;

        gameState = new GameState();
        new Initalize().processEvent(null, gameState, Json.newObject());

        processor = new EndTurnClicked();
    }

    // -----------------------------------------------------------------------
    // AC1 — Ending Player 1's turn replenishes Player 2's mana
    // -----------------------------------------------------------------------

    /**
     * Story Card #6 - AC1 (P2 mana):
     * When Player 1 clicks end turn, Player 2's mana must be set to 2
     * (their first turn) and a setPlayer2Mana command must be sent.
     */
    @Test
    public void endingPlayer1TurnReplenishesPlayer2Mana() {
        recorder.messages.clear();
        processor.processEvent(null, gameState, Json.newObject());

        assertTrue("setPlayer2Mana message must be sent",
                recorder.hasMessageType("setPlayer2Mana"));
        assertEquals("Player 2's first-turn mana must be 2",
                2, recorder.lastManaFor("setPlayer2Mana"));
        assertEquals("Player 2's mana in GameState must be 2",
                2, gameState.getPlayer2().getMana());
    }

    // -----------------------------------------------------------------------
    // AC2 — Ending Player 2's turn replenishes Player 1's mana (incremented)
    // -----------------------------------------------------------------------

    /**
     * Story Card #6 - AC1 (P1 mana, second turn):
     * After Player 2 also ends their first turn, Player 1 starts their second
     * turn with 3 mana.  A setPlayer1Mana command must be sent.
     */
    @Test
    public void endingPlayer2TurnReplenishesPlayer1Mana() {
        processor.processEvent(null, gameState, Json.newObject()); // P1 ends → P2 starts
        recorder.messages.clear();
        processor.processEvent(null, gameState, Json.newObject()); // P2 ends → P1 starts

        assertTrue("setPlayer1Mana message must be sent",
                recorder.hasMessageType("setPlayer1Mana"));
        assertEquals("Player 1's second-turn mana must be 3",
                3, recorder.lastManaFor("setPlayer1Mana"));
        assertEquals("Player 1's mana in GameState must be 3",
                3, gameState.getPlayer1().getMana());
    }

    // -----------------------------------------------------------------------
    // AC3 — Mana is capped at 9
    // -----------------------------------------------------------------------

    /**
     * Story Card #6 - AC1 (mana cap):
     * No matter how many turns pass, neither player's mana may exceed 9.
     */
    @Test
    public void manaIsNeverMoreThanNine() {
        for (int i = 0; i < 20; i++) {
            processor.processEvent(null, gameState, Json.newObject());
        }
        assertTrue("Player 1 mana must not exceed 9",
                gameState.getPlayer1().getMana() <= 9);
        assertTrue("Player 2 mana must not exceed 9",
                gameState.getPlayer2().getMana() <= 9);
    }

    // -----------------------------------------------------------------------
    // AC4 — End-turn clears the selected unit
    // -----------------------------------------------------------------------

    /**
     * Story Card #6 - AC1 (state cleanup):
     * When the turn ends, any previously selected unit must be deselected
     * so the new active player starts with a clean slate.
     */
    @Test
    public void endTurnClearsSelectedUnit() {
        gameState.selectedUnit = gameState.getPlayer1().getAvatar();
        assertNotNull("Pre-condition: selectedUnit should be set",
                gameState.getSelectedUnit());

        processor.processEvent(null, gameState, Json.newObject());

        assertNull("selectedUnit must be null after ending the turn",
                gameState.getSelectedUnit());
    }

    // -----------------------------------------------------------------------
    // AC5 — End-turn clears the selected hand position
    // -----------------------------------------------------------------------

    /**
     * Story Card #6 - AC1 (state cleanup):
     * When the turn ends, any previously selected card (hand position) must
     * be deselected.
     */
    @Test
    public void endTurnClearsSelectedHandPosition() {
        gameState.selectedHandPosition = 2;
        assertEquals("Pre-condition: selectedHandPosition should be 2",
                2, (int) gameState.selectedHandPosition);

        processor.processEvent(null, gameState, Json.newObject());

        assertNull("selectedHandPosition must be null after ending the turn",
                gameState.selectedHandPosition);
    }
}
