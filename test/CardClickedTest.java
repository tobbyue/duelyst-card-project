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
import play.libs.Json;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.unittypes.Unit;

/**
 * JUnit tests for Story Card #22:
 * When the player selects a creature card from their hand, all empty board tiles
 * that are adjacent (8-directional, 1 step) to a friendly unit already on the board
 * are highlighted in green (mode 2).  Occupied tiles are never highlighted.
 *
 * Board layout after Initalize:
 *   Player 1 avatar → [1, 2]
 *   Player 2 avatar → [7, 2]
 *
 * @author Minghao
 */
public class CardClickedTest {

    // -----------------------------------------------------------------------
    // Recording DummyTell — captures every BasicCommands message for inspection
    // -----------------------------------------------------------------------

    static class RecordingTell implements DummyTell {
        final List<ObjectNode> messages = new ArrayList<>();

        @Override
        public void tell(ObjectNode message) {
            messages.add(message.deepCopy());
        }

        /** Returns true if drawTile(x, y, mode) was ever sent. */
        boolean wasTileDrawnWithMode(int x, int y, int mode) {
            for (ObjectNode m : messages) {
                if (!"drawTile".equals(m.get("messagetype").asText())) continue;
                int tx = m.get("tile").get("tilex").asInt();
                int ty = m.get("tile").get("tiley").asInt();
                if (tx == x && ty == y && m.get("mode").asInt() == mode) return true;
            }
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Shared test state
    // -----------------------------------------------------------------------

    private RecordingTell recorder;
    private GameState gameState;
    private CardClicked processor;

    /** Builds a cardClicked JSON message for the given hand position (1-indexed). */
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

        // Replace Player 1's hand with a single known creature card at position 1
        gameState.getPlayer1().getHand().clear();
        Card creatureCard = new Card();
        creatureCard.setIsCreature(true);
        creatureCard.setManacost(2);
        gameState.getPlayer1().getHand().add(creatureCard);

        processor = new CardClicked();
    }

    // -----------------------------------------------------------------------
    // AC1 — Selecting a creature card stores the hand position in GameState
    // -----------------------------------------------------------------------

    /**
     * Story Card #22 - AC1:
     * After clicking a creature card at hand position 1, GameState must
     * record that position so subsequent events can act on it.
     */
    @Test
    public void selectingCreatureCardStoresHandPosition() {
        processor.processEvent(null, gameState, cardClickMsg(1));

        assertEquals("selectedHandPosition must be 1 after clicking position 1",
                1, (int) gameState.selectedHandPosition);
    }

    // -----------------------------------------------------------------------
    // AC2 — All 8 adjacent empty tiles around the P1 avatar are highlighted
    // -----------------------------------------------------------------------

    /**
     * Story Card #22 - AC2 (summon range):
     * Selecting a creature card must highlight every empty tile adjacent to
     * the Player 1 avatar at [1,2] in green (mode 2).
     */
    @Test
    public void selectingCreatureCardHighlightsAllAdjacentEmptyTiles() {
        recorder.messages.clear();
        processor.processEvent(null, gameState, cardClickMsg(1));

        // All 8 neighbours of [1,2] are empty (P2 avatar is at [7,2], not adjacent)
        assertTrue("[0,1] must be highlighted (mode 2)", recorder.wasTileDrawnWithMode(0, 1, 2));
        assertTrue("[1,1] must be highlighted (mode 2)", recorder.wasTileDrawnWithMode(1, 1, 2));
        assertTrue("[2,1] must be highlighted (mode 2)", recorder.wasTileDrawnWithMode(2, 1, 2));
        assertTrue("[0,2] must be highlighted (mode 2)", recorder.wasTileDrawnWithMode(0, 2, 2));
        assertTrue("[2,2] must be highlighted (mode 2)", recorder.wasTileDrawnWithMode(2, 2, 2));
        assertTrue("[0,3] must be highlighted (mode 2)", recorder.wasTileDrawnWithMode(0, 3, 2));
        assertTrue("[1,3] must be highlighted (mode 2)", recorder.wasTileDrawnWithMode(1, 3, 2));
        assertTrue("[2,3] must be highlighted (mode 2)", recorder.wasTileDrawnWithMode(2, 3, 2));
    }

    // -----------------------------------------------------------------------
    // AC3 — An occupied adjacent tile is NOT highlighted
    // -----------------------------------------------------------------------

    /**
     * Story Card #22 - AC3 (blocking):
     * If a tile adjacent to a friendly unit is occupied by any unit, it must
     * not be highlighted as a valid summon location.
     */
    @Test
    public void occupiedAdjacentTileIsNotHighlighted() {
        // Place an enemy unit at [2,2] — adjacent to P1 avatar at [1,2]
        Unit blocker = new Unit();
        blocker.setOwner(gameState.getPlayer2());
        Tile blockerTile = gameState.getBoard().getTile(2, 2);
        blockerTile.setUnit(blocker);

        recorder.messages.clear();
        processor.processEvent(null, gameState, cardClickMsg(1));

        assertFalse("[2,2] must NOT be highlighted (it is occupied)",
                recorder.wasTileDrawnWithMode(2, 2, 2));
    }

    // -----------------------------------------------------------------------
    // AC4 — A spell (non-creature) card does NOT highlight summon tiles
    // -----------------------------------------------------------------------

    /**
     * Story Card #22 - AC4 (spell cards):
     * Clicking a non-creature (spell) card must not trigger any green
     * summon-tile highlights on the board.
     */
    @Test
    public void spellCardDoesNotHighlightSummonTiles() {
        // Replace the hand with a spell card
        gameState.getPlayer1().getHand().clear();
        Card spellCard = new Card();
        spellCard.setIsCreature(false);
        spellCard.setManacost(1);
        gameState.getPlayer1().getHand().add(spellCard);

        recorder.messages.clear();
        processor.processEvent(null, gameState, cardClickMsg(1));

        for (ObjectNode m : recorder.messages) {
            if ("drawTile".equals(m.get("messagetype").asText())
                    && m.get("mode").asInt() == 2) {
                fail("Spell card must not cause any green (mode 2) tile highlights");
            }
        }
    }

    // -----------------------------------------------------------------------
    // AC5 — Clicking a new card clears highlights from the previous selection
    // -----------------------------------------------------------------------

    /**
     * Story Card #22 - AC5 (highlight refresh):
     * If the player clicks a different card after already selecting one, the
     * previous highlights must be cleared (mode 0 sent) before the new ones
     * are applied, ensuring no stale green tiles remain on the board.
     */
    @Test
    public void clickingNewCardClearsPreviousHighlights() {
        // Add a second creature card at position 2
        Card secondCard = new Card();
        secondCard.setIsCreature(true);
        secondCard.setManacost(1);
        gameState.getPlayer1().getHand().add(secondCard);

        // Select position 1
        processor.processEvent(null, gameState, cardClickMsg(1));
        recorder.messages.clear();

        // Select position 2 — a mode-0 drawTile for every board tile must be sent first
        processor.processEvent(null, gameState, cardClickMsg(2));

        boolean anyMode0Found = false;
        for (ObjectNode m : recorder.messages) {
            if ("drawTile".equals(m.get("messagetype").asText())
                    && m.get("mode").asInt() == 0) {
                anyMode0Found = true;
                break;
            }
        }
        assertTrue("Selecting a new card must clear previous highlights (mode 0)", anyMode0Found);
    }
}
