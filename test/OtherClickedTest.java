import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import commands.BasicCommands;
import commands.DummyTell;
import events.CardClicked;
import events.Initalize;
import events.OtherClicked;
import play.libs.Json;
import structures.GameState;
import structures.basic.Card;
import structures.basic.unittypes.Unit;

/**
 * JUnit tests for Story Card #12:
 * Clicking a non-interactive board area (otherClicked) or re-clicking the same card
 * must cancel any active unit or card selection and clear all board highlights.
 *
 * @author Minghao
 */
public class OtherClickedTest {

    // -----------------------------------------------------------------------
    // Recording DummyTell
    // -----------------------------------------------------------------------

    static class RecordingTell implements DummyTell {
        final List<ObjectNode> messages = new ArrayList<>();

        @Override
        public void tell(ObjectNode message) {
            messages.add(message.deepCopy());
        }

        boolean wasTileDrawnWithMode(int x, int y, int mode) {
            for (ObjectNode m : messages) {
                if (!"drawTile".equals(m.get("messagetype").asText())) continue;
                int tx = m.get("tile").get("tilex").asInt();
                int ty = m.get("tile").get("tiley").asInt();
                if (tx == x && ty == y && m.get("mode").asInt() == mode) return true;
            }
            return false;
        }

        boolean anyMode0Sent() {
            for (ObjectNode m : messages) {
                if ("drawTile".equals(m.get("messagetype").asText())
                        && m.get("mode").asInt() == 0) return true;
            }
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Shared test state
    // -----------------------------------------------------------------------

    private RecordingTell recorder;
    private GameState gameState;
    private OtherClicked otherClicked;
    private CardClicked cardClicked;

    private ObjectNode otherClickMsg() {
        ObjectNode msg = Json.newObject();
        msg.put("messagetype", "otherClicked");
        return msg;
    }

    private ObjectNode cardClickMsg(int position) {
        ObjectNode msg = Json.newObject();
        msg.put("messagetype", "cardClicked");
        msg.put("position", position);
        return msg;
    }

    @Before
    public void setUp() {
        recorder = new RecordingTell();
        BasicCommands.altTell = recorder;

        gameState = new GameState();
        new Initalize().processEvent(null, gameState, Json.newObject());

        // Give Player 1 a known creature card at position 1
        gameState.getPlayer1().getHand().clear();
        Card creatureCard = new Card();
        creatureCard.setIsCreature(true);
        creatureCard.setManacost(2);
        gameState.getPlayer1().getHand().add(creatureCard);

        otherClicked = new OtherClicked();
        cardClicked  = new CardClicked();
    }

    // -----------------------------------------------------------------------
    // AC1 — Background click clears a selected unit
    // -----------------------------------------------------------------------

    /**
     * Story Card #12 - AC1:
     * After the player selects a unit, clicking a non-interactive area must
     * deselect it (selectedUnit becomes null).
     */
    @Test
    public void backgroundClickDeselectsUnit() {
        // Manually simulate a unit being selected
        Unit unit = gameState.getBoard().getTile(1, 2).getUnit(); // P1 avatar
        gameState.selectedUnit = unit;

        otherClicked.processEvent(null, gameState, otherClickMsg());

        assertNull("selectedUnit must be null after otherClicked",
                gameState.getSelectedUnit());
    }

    // -----------------------------------------------------------------------
    // AC2 — Background click clears a selected card
    // -----------------------------------------------------------------------

    /**
     * Story Card #12 - AC2:
     * After the player selects a card, clicking a non-interactive area must
     * deselect it (selectedHandPosition becomes -1).
     */
    @Test
    public void backgroundClickDeselectsCard() {
        // Select card at position 1
        cardClicked.processEvent(null, gameState, cardClickMsg(1));
        assertEquals(1, (int) gameState.selectedHandPosition);

        recorder.messages.clear();
        otherClicked.processEvent(null, gameState, otherClickMsg());

        assertNull("selectedHandPosition must be null after otherClicked",
                gameState.selectedHandPosition);
    }

    // -----------------------------------------------------------------------
    // AC3 — Background click sends mode-0 resets for all tiles
    // -----------------------------------------------------------------------

    /**
     * Story Card #12 - AC3:
     * Clicking a non-interactive area must send drawTile(mode=0) for every
     * board tile to clear any lingering highlights.
     */
    @Test
    public void backgroundClickClearsAllHighlights() {
        recorder.messages.clear();
        otherClicked.processEvent(null, gameState, otherClickMsg());

        assertTrue("Background click must send at least one mode-0 drawTile",
                recorder.anyMode0Sent());

        // Verify every tile on the 9x5 board received a mode-0 reset
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 5; y++) {
                assertTrue("Tile [" + x + "," + y + "] must be reset to mode 0",
                        recorder.wasTileDrawnWithMode(x, y, 0));
            }
        }
    }

    // -----------------------------------------------------------------------
    // AC4 — Re-clicking the same card deselects it (toggle)
    // -----------------------------------------------------------------------

    /**
     * Story Card #12 - AC4:
     * If the player clicks a card that is already selected, it must be
     * deselected (selectedHandPosition becomes -1) and highlights cleared.
     */
    @Test
    public void reclickingSameCardDeselectsIt() {
        // First click — selects the card
        cardClicked.processEvent(null, gameState, cardClickMsg(1));
        assertEquals(1, (int) gameState.selectedHandPosition);

        recorder.messages.clear();

        // Second click on the same card — must deselect
        cardClicked.processEvent(null, gameState, cardClickMsg(1));

        assertNull("Re-clicking same card must set selectedHandPosition to null",
                gameState.selectedHandPosition);
    }

    // -----------------------------------------------------------------------
    // AC5 — Re-clicking the same card clears highlights
    // -----------------------------------------------------------------------

    /**
     * Story Card #12 - AC5:
     * When a card is deselected by re-clicking it, all summon highlights
     * (mode 2) must be cleared from the board.
     */
    @Test
    public void reclickingSameCardClearsHighlights() {
        // Select the card (highlights appear)
        cardClicked.processEvent(null, gameState, cardClickMsg(1));
        recorder.messages.clear();

        // Re-click to deselect
        cardClicked.processEvent(null, gameState, cardClickMsg(1));

        assertTrue("Deselecting a card must send mode-0 drawTile resets",
                recorder.anyMode0Sent());

        // Ensure no green (mode 2) highlights remain after deselect
        for (ObjectNode m : recorder.messages) {
            if ("drawTile".equals(m.get("messagetype").asText())
                    && m.get("mode").asInt() == 2) {
                fail("No green highlights should remain after card is deselected");
            }
        }
    }
}
