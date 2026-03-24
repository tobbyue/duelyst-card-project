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
import static org.junit.Assume.assumeNotNull;

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

    // ──────────────────── SC#17: AI Spell Usage ────────────────────

    @Test
    public void AICastsSpellOnValidTarget() {
        // Give AI a Truestrike (costs 1 mana) and enough mana to play it
        Card truestrike = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_a1_c_s_truestrike.json", 1, Card.class);
        assumeNotNull("Truestrike spell must load correctly", truestrike.getSpell());
        opponent.getHand().add(truestrike);
        opponent.setMana(truestrike.getManacost());

        // Ensure there is at least one valid target (P1 avatar is already on board)
        AI.AILogic.castSpells(null, gs);

        // Spell was cast: card removed from hand and mana deducted
        assertEquals("Truestrike should have been removed from hand after casting", 0, opponent.getHand().size());
        assertEquals("AI mana should be 0 after casting", 0, opponent.getMana());
    }

    @Test
    public void AICastsBeamshockOnValidTarget() {
        // Beamshock targets enemy non-avatar units — costs 1 mana
        Card beamshock = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_5_c_s_beamshock.json", 1, Card.class);
        assumeNotNull("Beamshock spell must load correctly", beamshock.getSpell());
        opponent.getHand().add(beamshock);
        opponent.setMana(beamshock.getManacost());

        // Place a P1 non-avatar unit
        Card unitCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 2, Card.class);
        player.getHand().add(unitCard);
        Tile targetTile = gs.getBoard().getTile(3, 2);
        player.useCard(null, gs, 0, targetTile, 0);
        Unit target = targetTile.getUnit();
        assertNotNull(target);

        AI.AILogic.castSpells(null, gs);

        // Beamshock sets hasMoved and hasAttacked to true on the target
        assertTrue("Beamshocked unit should be stunned (hasMoved=true)", target.hasMoved);
        assertTrue("Beamshocked unit should be stunned (hasAttacked=true)", target.hasAttacked);
    }

    @Test
    public void AIDoesNotCastSpellWithoutMana() {
        Card truestrike = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_a1_c_s_truestrike.json", 1, Card.class);
        opponent.getHand().add(truestrike);
        opponent.setMana(0); // no mana

        Card unitCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json", 2, Card.class);
        player.getHand().add(unitCard);
        Tile targetTile = gs.getBoard().getTile(3, 2);
        player.useCard(null, gs, 0, targetTile, 0);
        Unit target = targetTile.getUnit();
        int hpBefore = target.getHealth();

        AI.AILogic.castSpells(null, gs);

        assertEquals("Spell should not be cast without enough mana", hpBefore, target.getHealth());
        assertEquals("Spell card should remain in hand", 1, opponent.getHand().size());
    }

    @Test
    public void AIDoesNotCastSpellWithoutValidTarget() {
        // Truestrike targets enemy units — but no P1 units on board (only avatar)
        // True Strike CAN target the avatar, so use Beamshock which excludes avatars
        Card beamshock = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_5_c_s_beamshock.json", 1, Card.class);
        assumeNotNull(beamshock.getSpell());
        opponent.getHand().add(beamshock);
        opponent.setMana(9);

        // No non-avatar P1 units on the board
        int handSizeBefore = opponent.getHand().size();
        AI.AILogic.castSpells(null, gs);

        assertEquals("Spell should not be cast when no valid targets exist", handSizeBefore, opponent.getHand().size());
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
