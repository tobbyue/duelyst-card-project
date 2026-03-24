import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import org.junit.Before;
import org.junit.Test;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.players.AIPlayer;
import structures.basic.players.HumanPlayer;
import structures.basic.unittypes.Unit;
import structures.logic.AI;
import utils.BasicObjectBuilders;

import static org.junit.Assert.*;

/**
 * JUnit tests for Story Card #11 (AI Attacking) and Story Card #15 (AI Movement).
 *
 * @author Minghao
 */
public class AITest {

    GameState gs = new GameState();
    HumanPlayer player = gs.player1;
    AIPlayer opponent = gs.player2;
    CheckMessageIsNotNullOnTell altTell;

    @Before
    public void setUp() {
        altTell = new CheckMessageIsNotNullOnTell();
        BasicCommands.altTell = altTell;
        player.setAvatar(null, gs);
        gs.placeAvatar(null, player.getAvatar(), 1, 2);
        opponent.setAvatar(null, gs);
        gs.placeAvatar(null, opponent.getAvatar(), 7, 2);
        // Simulate start of AI turn: reset flags as endTurn() would
        opponent.getAvatar().hasAttacked = false;
        opponent.getAvatar().hasMoved = false;
    }

    // ──────────────────── SC#11: AI Attacking ────────────────────

    @Test
    public void AIAttacksAdjacentEnemy() {
        Card card = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 1, Card.class);
        player.getHand().add(card);
        Tile summonTile = gs.getBoard().getTile(6, 2);
        player.useCard(null, gs, 0, summonTile, 0);

        Unit target = summonTile.getUnit();
        assertNotNull("Player unit should be on tile", target);
        int hpBefore = target.getHealth();

        AI.AILogic.attack(null, gs);

        assertTrue("Target HP should decrease after AI attack",
                target.getHealth() < hpBefore || target.getCurrentTile() == null);
    }

    @Test
    public void AIDoesNotAttackAllies() {
        Card card = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 1, Card.class);
        opponent.getHand().add(card);
        Tile allyTile = gs.getBoard().getTile(6, 2);
        opponent.useCard(null, gs, 0, allyTile, 0);

        Unit ally = allyTile.getUnit();
        assertNotNull(ally);
        int hpBefore = ally.getHealth();

        AI.AILogic.attack(null, gs);

        assertEquals("Ally HP should not change", hpBefore, ally.getHealth());
    }

    @Test
    public void AIOnlyAttacksWhenInRange() {
        Card card = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 1, Card.class);
        player.getHand().add(card);
        Tile farTile = gs.getBoard().getTile(0, 0);
        player.useCard(null, gs, 0, farTile, 0);

        Unit target = farTile.getUnit();
        assertNotNull(target);
        int hpBefore = target.getHealth();

        AI.AILogic.attack(null, gs);

        assertEquals("Out-of-range unit should not be attacked", hpBefore, target.getHealth());
    }

    @Test
    public void AIUnitDoesNotAttackTwice() {
        Card card = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 1, Card.class);
        player.getHand().add(card);
        Tile summonTile = gs.getBoard().getTile(6, 2);
        player.useCard(null, gs, 0, summonTile, 0);

        Unit target = summonTile.getUnit();
        assertNotNull(target);

        AI.AILogic.attack(null, gs);
        int hpAfterFirst = target.isDead() ? 0 : target.getHealth();

        if (!target.isDead()) {
            AI.AILogic.attack(null, gs);
            assertEquals("Unit should not attack again after hasAttacked = true", hpAfterFirst, target.getHealth());
        }
    }

    // ──────────────────── SC#15: AI Movement ────────────────────

    @Test
    public void AIMovesTowardsEnemy() {
        // P1 unit at (4,2): not adjacent to AI avatar at (7,2), but reachable within 2 steps
        Card card = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 1, Card.class);
        player.getHand().add(card);
        Tile p1Tile = gs.getBoard().getTile(4, 2);
        player.useCard(null, gs, 0, p1Tile, 0);

        Tile avatarTileBefore = opponent.getAvatar().getCurrentTile();
        AI.AILogic.moveUnit(null, gs);
        Tile avatarTileAfter = opponent.getAvatar().getCurrentTile();

        assertNotEquals("AI avatar should have moved towards the enemy", avatarTileBefore, avatarTileAfter);
    }

    @Test
    public void AIDoesNotMoveWhenAlreadyInAttackRange() {
        // P1 unit at (6,2): directly adjacent to AI avatar at (7,2) — no move needed
        Card card = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 1, Card.class);
        player.getHand().add(card);
        Tile adjacentTile = gs.getBoard().getTile(6, 2);
        player.useCard(null, gs, 0, adjacentTile, 0);

        Tile avatarTileBefore = opponent.getAvatar().getCurrentTile();
        AI.AILogic.moveUnit(null, gs);
        Tile avatarTileAfter = opponent.getAvatar().getCurrentTile();

        assertEquals("AI avatar should not move when already adjacent to enemy", avatarTileBefore, avatarTileAfter);
    }

    @Test
    public void AIUnitDoesNotMoveTwice() {
        // P1 unit at (4,2), AI avatar at (7,2)
        Card card = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 1, Card.class);
        player.getHand().add(card);
        player.useCard(null, gs, 0, gs.getBoard().getTile(4, 2), 0);

        AI.AILogic.moveUnit(null, gs);
        Tile afterFirstMove = opponent.getAvatar().getCurrentTile();

        AI.AILogic.moveUnit(null, gs);
        Tile afterSecondMove = opponent.getAvatar().getCurrentTile();

        assertEquals("AI unit should not move again after hasMoved = true", afterFirstMove, afterSecondMove);
    }

    @Test
    public void AIMovesCloserToNearestEnemy() {
        // P1 unit at (4,2), AI avatar at (7,2) — AI should end up closer (x < 7)
        Card card = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 1, Card.class);
        player.getHand().add(card);
        player.useCard(null, gs, 0, gs.getBoard().getTile(4, 2), 0);

        AI.AILogic.moveUnit(null, gs);
        int xAfter = opponent.getAvatar().getCurrentTile().getTilex();

        assertTrue("AI avatar should have moved closer (x < 7)", xAfter < 7);
    }
}
