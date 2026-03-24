import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import commands.BasicCommands;
import commands.DummyTell;
import events.Initalize;
import events.TileClicked;
import play.libs.Json;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.unittypes.Unit;

/**
 * JUnit tests for Story Card #3:
 * When the human player clicks one of their own units, the unit becomes
 * selected and its valid movement range is highlighted in white on the board.
 *
 * Test setup mirrors the pattern established in InitalizationTest:
 * BasicCommands.altTell is overridden so no real front-end connection is needed.
 * Initalize is run first to place avatars at their canonical positions and set
 * ownership, exactly as would happen in a real game.
 *
 * Board layout after Initalize:
 *   Player 1 avatar → [1, 2]
 *   Player 2 avatar → [7, 2]
 *
 * @author Minghao
 */
public class TileClickedTest {

    // -----------------------------------------------------------------------
    // Recording DummyTell — captures every BasicCommands message for inspection
    // -----------------------------------------------------------------------

    /**
     * A DummyTell that records every message sent via BasicCommands so that
     * individual tests can inspect exactly which drawTile calls were made and
     * with which highlight mode.
     */
    static class RecordingTell implements DummyTell {
        final List<ObjectNode> messages = new ArrayList<>();

        @Override
        public void tell(ObjectNode message) {
            messages.add(message.deepCopy());
        }

        /** Returns true if a drawTile(mode=1) was ever sent for the given (x, y). */
        boolean wasHighlighted(int x, int y) {
            return wasDrawnWithMode(x, y, 1);
        }

        /** Returns true if a drawTile with the given mode was ever sent for (x, y). */
        boolean wasDrawnWithMode(int x, int y, int mode) {
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
    private TileClicked processor;

    /** Builds a tileClicked JSON message for the given board coordinates. */
    private ObjectNode clickMsg(int x, int y) {
        ObjectNode msg = Json.newObject();
        msg.put("messagetype", "tileclicked");
        msg.put("tilex", x);
        msg.put("tiley", y);
        return msg;
    }

    /**
     * Prepares a fresh GameState and runs Initalize so that:
     * - both avatars are placed on the board with ownership set, and
     * - the RecordingTell is installed before any command is issued.
     */
    @Before
    public void setUp() {
        recorder = new RecordingTell();
        BasicCommands.altTell = recorder;

        gameState = new GameState();
        new Initalize().processEvent(null, gameState, Json.newObject());

        processor = new TileClicked();
    }

    // -----------------------------------------------------------------------
    // AC1 — Clicking a Player 1 unit selects it
    // -----------------------------------------------------------------------

    /**
     * Story Card #3 - AC1:
     * Clicking a tile occupied by a Player 1 unit must set that unit as the
     * selected unit in GameState.
     */
    @Test
    public void clickingPlayer1UnitSelectsIt() {
        assertNull("No unit should be selected before any click",
                gameState.getSelectedUnit());

        processor.processEvent(null, gameState, clickMsg(1, 2)); // P1 avatar at [1,2]

        assertNotNull("A Player 1 unit click must set selectedUnit",
                gameState.getSelectedUnit());
        assertSame("selectedUnit must be Player 1's avatar",
                gameState.getPlayer1().getAvatar(), gameState.getSelectedUnit());
    }

    // -----------------------------------------------------------------------
    // AC2 — Clicking a Player 2 unit does NOT select it
    // -----------------------------------------------------------------------

    /**
     * Story Card #3 - AC2:
     * Clicking a tile occupied by the enemy (Player 2) must not change
     * selectedUnit — the human player can only select their own units.
     */
    @Test
    public void clickingPlayer2UnitDoesNotSelectIt() {
        processor.processEvent(null, gameState, clickMsg(7, 2)); // P2 avatar at [7,2]

        assertNull("Clicking a Player 2 unit must not set selectedUnit",
                gameState.getSelectedUnit());
    }

    // -----------------------------------------------------------------------
    // AC3 — Clicking an empty tile selects nothing
    // -----------------------------------------------------------------------

    /**
     * Story Card #3 - AC3:
     * Clicking a tile that contains no unit must not select any unit.
     */
    @Test
    public void clickingEmptyTileSelectsNothing() {
        processor.processEvent(null, gameState, clickMsg(4, 2)); // guaranteed empty

        assertNull("Clicking an empty tile must not set selectedUnit",
                gameState.getSelectedUnit());
    }

    // -----------------------------------------------------------------------
    // AC4 — Re-clicking a different Player 1 unit updates the selection
    // -----------------------------------------------------------------------

    /**
     * Story Card #3 - AC4:
     * If the player clicks a second Player 1 unit after already selecting one,
     * selectedUnit must update to the newly clicked unit.
     */
    @Test
    public void reClickingDifferentUnitUpdatesSelection() {
        // Place a second P1 unit at [3, 1] with actions remaining
        Unit extra = new Unit();
        extra.setOwner(gameState.getPlayer1());
        extra.hasMoved = false;
        extra.hasAttacked = false;
        Tile t = gameState.getBoard().getTile(3, 1);
        t.setUnit(extra);
        extra.setPositionByTile(t);

        processor.processEvent(null, gameState, clickMsg(1, 2)); // select avatar first
        assertSame("First click should select the avatar",
                gameState.getPlayer1().getAvatar(), gameState.getSelectedUnit());

        processor.processEvent(null, gameState, clickMsg(3, 1)); // then select extra unit
        assertSame("Second click must update selectedUnit to the new unit",
                extra, gameState.getSelectedUnit());
    }

    // -----------------------------------------------------------------------
    // AC5 — Cardinal movement highlights (up to 2 steps)
    // -----------------------------------------------------------------------

    /**
     * Story Card #3 - AC5 (cardinal):
     * After selecting the Player 1 avatar at [2,3], tiles up to 2 steps away
     * in each cardinal direction (left, right, up, down) must be highlighted
     * in white (mode=1), provided the path is clear.
     */
    @Test
    public void cardinalTilesWithinTwoStepsAreHighlighted() {
        recorder.messages.clear();
        processor.processEvent(null, gameState, clickMsg(1, 2));

        // Right: [2,2] and [3,2]
        assertTrue("[2,2] must be highlighted (1 step right)",  recorder.wasHighlighted(2, 2));
        assertTrue("[3,2] must be highlighted (2 steps right)", recorder.wasHighlighted(3, 2));

        // Left: [0,2] (only 1 step; [-1,2] is off the board)
        assertTrue("[0,2] must be highlighted (1 step left)",   recorder.wasHighlighted(0, 2));

        // Up: [1,1] and [1,0]
        assertTrue("[1,1] must be highlighted (1 step up)",     recorder.wasHighlighted(1, 1));
        assertTrue("[1,0] must be highlighted (2 steps up)",    recorder.wasHighlighted(1, 0));

        // Down: [1,3] and [1,4]
        assertTrue("[1,3] must be highlighted (1 step down)",   recorder.wasHighlighted(1, 3));
        assertTrue("[1,4] must be highlighted (2 steps down)",  recorder.wasHighlighted(1, 4));
    }

    // -----------------------------------------------------------------------
    // AC6 — Diagonal movement highlights (exactly 1 step)
    // -----------------------------------------------------------------------

    /**
     * Story Card #3 - AC6 (diagonal):
     * After selecting the Player 1 avatar at [2,3], all four diagonal tiles
     * one step away must be highlighted in white.
     */
    @Test
    public void diagonalTilesOneStepAwayAreHighlighted() {
        recorder.messages.clear();
        processor.processEvent(null, gameState, clickMsg(1, 2));

        assertTrue("[2,3] must be highlighted (diagonal down-right)", recorder.wasHighlighted(2, 3));
        assertTrue("[2,1] must be highlighted (diagonal up-right)",   recorder.wasHighlighted(2, 1));
        assertTrue("[0,3] must be highlighted (diagonal down-left)",  recorder.wasHighlighted(0, 3));
        assertTrue("[0,1] must be highlighted (diagonal up-left)",    recorder.wasHighlighted(0, 1));
    }

    // -----------------------------------------------------------------------
    // AC7 — Path blocking stops cardinal highlights beyond the blocker
    // -----------------------------------------------------------------------

    /**
     * Story Card #3 - AC7 (path blocking):
     * If any unit (ally or enemy) occupies an intermediate tile in a cardinal
     * direction, the movement highlight must not extend beyond that tile.
     * The blocker tile itself must also not be highlighted as a valid destination.
     */
    @Test
    public void blockerUnitStopsCardinalHighlightBeyondIt() {
        // Place an enemy unit at [2,2] — one step right of P1 avatar at [1,2]
        Unit blocker = new Unit();
        blocker.setOwner(gameState.getPlayer2());
        Tile blockerTile = gameState.getBoard().getTile(2, 2);
        blockerTile.setUnit(blocker);

        recorder.messages.clear();
        processor.processEvent(null, gameState, clickMsg(1, 2));

        // The blocker tile itself must not be a valid landing square
        assertFalse("[2,2] must NOT be highlighted (occupied by blocker)",
                recorder.wasHighlighted(2, 2));
        // The tile behind the blocker must also not be highlighted
        assertFalse("[3,2] must NOT be highlighted (path blocked at [2,2])",
                recorder.wasHighlighted(3, 2));
    }

    // -----------------------------------------------------------------------
    // AC8 — No crash near board edge
    // -----------------------------------------------------------------------

    /**
     * Story Card #3 - AC8 (board edge):
     * A Player 1 unit placed at corner [0,0] must not cause an
     * ArrayIndexOutOfBoundsException or any other crash when the movement
     * range computation walks off the board boundary.
     * The unit must still be recorded as selected.
     */
    @Test
    public void movementHighlightAtCornerDoesNotCrash() {
        // Move P1 avatar from [1,2] to corner [0,0]
        gameState.getBoard().getTile(1, 2).setUnit(null);
        Unit unit = gameState.getPlayer1().getAvatar();
        Tile corner = gameState.getBoard().getTile(0, 0);
        corner.setUnit(unit);
        unit.setPositionByTile(corner);

        // Must not throw any exception
        processor.processEvent(null, gameState, clickMsg(0, 0));

        assertSame("Unit at corner [0,0] must be selected after click",
                unit, gameState.getSelectedUnit());
    }
}
