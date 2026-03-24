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
 * JUnit tests for Story Card #11: AI Attacking.
 * <p>
 * The AI should attack adjacent enemy units when able, and only perform legal attacks.
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

    @Test
    public void AIAttacksAdjacentEnemy() {
        // Summon a P1 unit adjacent to the AI avatar
        Card card = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 1, Card.class);
        player.getHand().add(card);
        Tile summonTile = gs.getBoard().getTile(6, 2);
        player.useCard(null, gs, 0, summonTile, 0);

        Unit target = summonTile.getUnit();
        assertNotNull("Player unit should be on tile", target);
        int hpBefore = target.getHealth();

        // Run AI attack
        AI.AILogic.attack(null, gs);

        // AI avatar is adjacent to the summoned unit and should have attacked it
        assertTrue("Target HP should decrease after AI attack", target.getHealth() < hpBefore || target.getCurrentTile() == null);
    }

    @Test
    public void AIDoesNotAttackAllies() {
        // Summon a second AI unit adjacent to AI avatar
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
        // P1 unit far away from all AI units — no attacks should happen
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
        // Place a P1 unit next to AI avatar
        Card card = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 1, Card.class);
        player.getHand().add(card);
        Tile summonTile = gs.getBoard().getTile(6, 2);
        player.useCard(null, gs, 0, summonTile, 0);

        Unit target = summonTile.getUnit();
        assertNotNull(target);

        AI.AILogic.attack(null, gs);
        int hpAfterFirst = target.isDead() ? 0 : target.getHealth();

        // Second call — avatar hasAttacked = true, should not attack again
        if (!target.isDead()) {
            AI.AILogic.attack(null, gs);
            assertEquals("Unit should not attack again after hasAttacked = true", hpAfterFirst, target.getHealth());
        }
    }
}
