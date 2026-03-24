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
 * JUnit tests for Story Card #19:
 * The player's mana starts at 2, increases by 1 each turn (the turn maximum),
 * and the mana is always filled to that maximum at the start of every turn.
 * The mana never exceeds the turn maximum (capped at 9).
 *
 * @author Minghao
 */
public class ManaIncreaseTest {

    // -----------------------------------------------------------------------
    // Recording DummyTell
    // -----------------------------------------------------------------------

    static class RecordingTell implements DummyTell {
        final List<ObjectNode> messages = new ArrayList<>();

        @Override
        public void tell(ObjectNode message) {
            messages.add(message.deepCopy());
        }

        /** Returns the mana value from the last setPlayer1Mana message, or -1 if none. */
        int lastPlayer1Mana() {
            int mana = -1;
            for (ObjectNode m : messages) {
                if ("setPlayer1Mana".equals(m.get("messagetype").asText())) {
                    mana = m.get("player").get("mana").asInt();
                }
            }
            return mana;
        }

        /** Returns the mana value from the last setPlayer2Mana message, or -1 if none. */
        int lastPlayer2Mana() {
            int mana = -1;
            for (ObjectNode m : messages) {
                if ("setPlayer2Mana".equals(m.get("messagetype").asText())) {
                    mana = m.get("player").get("mana").asInt();
                }
            }
            return mana;
        }
    }

    // -----------------------------------------------------------------------
    // Shared test state
    // -----------------------------------------------------------------------

    private RecordingTell recorder;
    private GameState     gameState;
    private EndTurnClicked endTurn;

    private ObjectNode endTurnMsg() {
        ObjectNode msg = Json.newObject();
        msg.put("messagetype", "endTurnClicked");
        return msg;
    }

    @Before
    public void setUp() {
        recorder  = new RecordingTell();
        BasicCommands.altTell = recorder;

        gameState = new GameState();
        new Initalize().processEvent(null, gameState, Json.newObject());

        endTurn = new EndTurnClicked();
    }

    // -----------------------------------------------------------------------
    // AC1 — Player 1 starts with mana = 2 on turn 1
    // -----------------------------------------------------------------------

    /**
     * Story Card #19 - AC1:
     * At game start (turn 1), Player 1's mana must be set to 2.
     * The turn-maximum formula is min(N + 1, 9) where N = 1 → mana = 2.
     */
    @Test
    public void player1StartsWithMana2() {
        assertEquals("Player 1 must start with mana = 2",
                2, gameState.getPlayer1().getMana());
    }

    // -----------------------------------------------------------------------
    // AC2 — Mana increases by 1 each turn
    // -----------------------------------------------------------------------

    /**
     * Story Card #19 - AC2:
     * After P1 ends turn 1, P2 begins their first turn with mana = 2.
     * After P2 ends their turn, P1 begins turn 2 with mana = 3.
     */
    @Test
    public void manaIncreasesEachTurn() {
        // P1 ends turn 1 → P2 turn 1 starts: mana = min(1 + 1, 9) = 2
        endTurn.processEvent(null, gameState, endTurnMsg());
        assertEquals("P2 mana on their first turn must be 2",
                2, gameState.getPlayer2().getMana());

        // P2 ends turn → P1 turn 2 starts: mana = min(2 + 1, 9) = 3
        endTurn.processEvent(null, gameState, endTurnMsg());
        assertEquals("P1 mana on their second turn must be 3",
                3, gameState.getPlayer1().getMana());

        // P1 ends turn → P2 turn 2: mana = min(2 + 1, 9) = 3
        endTurn.processEvent(null, gameState, endTurnMsg());
        assertEquals("P2 mana on their second turn must be 3",
                3, gameState.getPlayer2().getMana());
    }

    // -----------------------------------------------------------------------
    // AC3 — Mana is filled to the turn maximum at the start of each turn
    // -----------------------------------------------------------------------

    /**
     * Story Card #19 - AC3:
     * The mana is always replenished to the full turn maximum when a new turn
     * begins, regardless of how much was spent the previous turn.
     */
    @Test
    public void manaFilledToMaxAtTurnStart() {
        // Simulate P1 spending some mana mid-turn
        gameState.getPlayer1().setMana(null, 0);

        // P1 ends turn → P2's turn 1 begins with full 2 mana
        endTurn.processEvent(null, gameState, endTurnMsg());
        assertEquals("P2 mana must be filled to 2 regardless of P1 spending",
                2, gameState.getPlayer2().getMana());

        // P2 spends all mana
        gameState.getPlayer2().setMana(null, 0);

        // P2 ends turn → P1's turn 2 begins with full 3 mana
        endTurn.processEvent(null, gameState, endTurnMsg());
        assertEquals("P1 mana on turn 2 must be filled to 3 regardless of P2 spending",
                3, gameState.getPlayer1().getMana());
    }

    // -----------------------------------------------------------------------
    // AC4 — Mana never exceeds 9
    // -----------------------------------------------------------------------

    /**
     * Story Card #19 - AC4:
     * The mana cap is 9. Even after many turns the mana must not exceed 9.
     */
    @Test
    public void manaIsCapppedAt9() {
        // Simulate 20 full turn cycles (40 end-turn events)
        for (int i = 0; i < 20; i++) {
            endTurn.processEvent(null, gameState, endTurnMsg()); // end P1 turn
            endTurn.processEvent(null, gameState, endTurnMsg()); // end P2 turn
        }

        assertTrue("P1 mana must never exceed 9",
                gameState.getPlayer1().getMana() <= 9);
        assertTrue("P2 mana must never exceed 9",
                gameState.getPlayer2().getMana() <= 9);
    }

    // -----------------------------------------------------------------------
    // AC5 — Front-end display is updated when mana is set
    // -----------------------------------------------------------------------

    /**
     * Story Card #19 - AC5:
     * When a turn starts and mana is set, the front-end receives the updated
     * mana value via a setPlayer1Mana / setPlayer2Mana command.
     */
    @Test
    public void manaDisplayUpdatedAtTurnStart() {
        recorder.messages.clear();

        // P1 ends turn → P2 receives mana command
        endTurn.processEvent(null, gameState, endTurnMsg());
        assertEquals("setPlayer2Mana must report mana = 2",
                2, recorder.lastPlayer2Mana());

        recorder.messages.clear();

        // P2 ends turn → P1 receives mana command
        endTurn.processEvent(null, gameState, endTurnMsg());
        assertEquals("setPlayer1Mana must report mana = 3",
                3, recorder.lastPlayer1Mana());
    }
}
