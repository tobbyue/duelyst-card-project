import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.players.AIPlayer;
import structures.basic.players.HumanPlayer;
import structures.basic.unittypes.BetterUnit;
import structures.basic.unittypes.GloomChaser;
import structures.basic.unittypes.Unit;
import structures.basic.unittypes.Wraithling;
import structures.logic.BoardLogic;
import utils.BasicObjectBuilders;

import java.util.Set;

import static org.junit.Assert.*;

public class AbilityTest {
    GameState gs = new GameState();
    HumanPlayer player = gs.player1;
    AIPlayer opponent = gs.player2;
    CheckMessageIsNotNullOnTell altTell;

    @Before
    public void setUp() {
        altTell = new CheckMessageIsNotNullOnTell(); // create an alternative tell
        BasicCommands.altTell = altTell; // specify that the alternative tell should be used
        player.setAvatar(null, gs);
        gs.placeAvatar(null, player.getAvatar(), 3, 2);
        gs.selectedHandPosition = 1;
        player.getHand().clear();
        opponent.getHand().clear();
        opponent.setAvatar(null, gs);
        gs.placeAvatar(null, opponent.getAvatar(), 7, 2);

    }

    @After
    public void tearDown() {
        for (Tile[] row : gs.getBoard().getTiles()) {
            for (Tile tile : row) {
                tile.setUnit(null);
            }
        }
    }


    ///////////// Gloom Chaser //////////////
    @Test
    public void GloomChaserGambitSummonedTest () {
        // Gloomchaser summons wraithling in emtpy space
        Card testCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/1_3_c_u_gloom_chaser.json", 12, Card.class);

        player.getHand().add(testCard);
        Tile summonTile = gs.getBoard().getTile(3, 3);
        player.useCard(null, gs, 0, summonTile, 0);
        Unit unitOnTile = gs.getBoard().getTile(2, 3).getUnit();
        assertNotNull("Unit should be summoned", unitOnTile);
        assertEquals("Wraithling should appear in empty space behind summoned unit.", Wraithling.class, unitOnTile.getClass());
    }

    @Test
    public void GloomChaserGambitDoesNotOverwriteTest () {
        // Gloomchaser does not overwrite occupied tile
        Card testCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/1_3_c_u_gloom_chaser.json", 1, Card.class);
        player.getHand().add(testCard);
        Tile summonTile = gs.getBoard().getTile(4, 2);
        player.useCard(null, gs, 0, summonTile, 0);
        Unit unitOnTile = gs.getBoard().getTile(3, 2).getUnit();
        assertNotNull("Unit should be summoned", unitOnTile);
        assertEquals("Wraithling should not overwrite existing unit.", BetterUnit.class, unitOnTile.getClass());
    }

    @Test
    public void GloomChaserDoesNotCrash() {
        // Summon does not crash if done at edge of board
        Card testCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/1_3_c_u_gloom_chaser.json", 1, Card.class);
        player.getHand().add(testCard);
        Tile summonTile = gs.getBoard().getTile(0, 3);
        player.useCard(null, gs, 0, summonTile, 0);
        Unit unitOnTile = gs.getBoard().getTile(0, 3).getUnit();

        assertNotNull("Unit should be summoned", unitOnTile);
        assertEquals("Wraithling should not overwrite existing unit.", GloomChaser.class, unitOnTile.getClass());
    }

    @Test
    public void NightsorrowAssassinOpeningGambitTest() {
        // Check if valid target is killed
        Card opponentCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_6_c_u_young_flamewing.json", 2, Card.class);
        opponent.getHand().add(opponentCard);

        Card testCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/1_6_c_u_nightsorrow_assassin.json", 1, Card.class);
        player.getHand().add(testCard);

        Tile targetTile = gs.getBoard().getTile(3, 0);
        opponent.useCard(null, gs, 0, targetTile, 0);
        Unit target = targetTile.getUnit();
        target.takeDamage(null, 1);

        Tile summonTile = gs.getBoard().getTile(4, 1);
        player.useCard(null, gs, 0, summonTile, 0);
        Unit unit = summonTile.getUnit();

        assertNull("Target should be deleted.", targetTile.getUnit());
        summonTile.setUnit(null);
    }

    @Test
    public void NightsorrowAssassinGambitFullHPStaysTest() {
        // Check if full HP target is not deleted
        Card opponentCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_6_c_u_young_flamewing.json", 2, Card.class);
        Card testCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/1_6_c_u_nightsorrow_assassin.json", 1, Card.class);
        Tile  targetTile = gs.getBoard().getTile(3, 0);
        Tile summonTile = gs.getBoard().getTile(4, 1);

        opponent.getHand().add(opponentCard);
        player.getHand().add(testCard);

        opponent.useCard(null, gs, 0, targetTile, 0);
        player.useCard(null, gs, 0, summonTile, 0);

        assertNotNull("Full HP unit should not be targeted", targetTile.getUnit());
        summonTile.setUnit(null);
    }

    @Test
    public void NightsorrowAssassinGambitNoAllyKillTest() {
        // Check if allied unit is deleted
        Card opponentCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_6_c_u_young_flamewing.json", 2, Card.class);
        Card testCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/1_6_c_u_nightsorrow_assassin.json", 1, Card.class);
        Tile targetTile = gs.getBoard().getTile(3, 0);
        Tile summonTile = gs.getBoard().getTile(4, 1);

        player.getHand().add(opponentCard);
        player.getHand().add(testCard);

        player.useCard(null, gs, 0, targetTile, 0);
        player.useCard(null, gs, 0, summonTile, 0);

        assertNotNull("Allied unit should not be targeted", targetTile.getUnit());
        summonTile.setUnit(null);
    }

    @Test
    public void NightsorrowAssassinGambitNoAvatarKillTest() {
        Card testCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/1_6_c_u_nightsorrow_assassin.json", 1, Card.class);

        player.getHand().add(testCard);
        Tile avatarTile = gs.getBoard().getTile(7, 2);
        Tile nextToAvatar = gs.getBoard().getTile(7, 3);
        opponent.getAvatar().takeDamage(null, gs, 1);
        player.useCard(null, gs, 0, nextToAvatar, 0);
        assertNotNull("Avatar unit should not be targeted", avatarTile.getUnit());
        nextToAvatar.setUnit(null);
    }

    @Test
    public void NightsorrowAssassinGambit1UnitKilledTest() {
        Card opponentCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_6_c_u_young_flamewing.json", 2, Card.class);
        Card testCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/1_6_c_u_nightsorrow_assassin.json", 1, Card.class);
        Tile summonTile = gs.getBoard().getTile(4, 1);

        // Check if only one unit is killed
        int after = 0;
        for (int i = 0; i < 8; i++)
            opponent.getHand().add(opponentCard);
        Set<Tile> surroundingTargets = BoardLogic.findAdjacentTiles(summonTile, gs.getBoard());
        for  (Tile tile : surroundingTargets) {
            opponent.useCard(null, gs, 0, tile, 0);
            tile.getUnit().takeDamage(null, gs, 1);
        }
        player.getHand().add(testCard);
        player.useCard(null, gs, 0, summonTile, 0);
        for (Tile tile : surroundingTargets) {
            if (tile.getUnit() != null)
                after++;
        }
        assertEquals("Only one target should be deleted.",  7, after);
    }

    @Test
    public void SilverguardSquireBuffOneUnitTest() {
        Card allyCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json",  1, Card.class);
        Card squireCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_7_c_u_silverguard_squire.json", 1 , Card.class);
        Tile summonLeft = gs.getBoard().getTile(2,2);
        Tile summonRight = gs.getBoard().getTile(4,2);
        Tile unitSummon = gs.getBoard().getTile(0,0);

        player.getHand().add(allyCard);
        player.getHand().add(squireCard);

        player.useCard(null, gs, 0, summonLeft, 0);
        Unit ally = summonLeft.getUnit();
        ally.takeDamage(null, gs, 1);
        int startHP = ally.getHealth(), startMax = ally.getMaxHealth(), startAttack = ally.getAttack();
        int expHP = startHP + 1, expMax = startMax + 1, expAttack = startAttack + 1;

        player.useCard(null, gs, 0, unitSummon, 0);

        assertEquals("HP should increase.", expHP, ally.getHealth());
        assertEquals("Max HP should increase.", expMax, ally.getMaxHealth());
        assertEquals("Attack should increase.", expAttack, ally.getAttack());
    }

    @Test
    public void SilverguardSquireBuffBothUnitsTest() {
        Card allyCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json",  1, Card.class);
        Card squireCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_7_c_u_silverguard_squire.json", 1 , Card.class);
        Tile summonLeft = gs.getBoard().getTile(2,2);
        Tile summonRight = gs.getBoard().getTile(4,2);
        Tile unitSummon = gs.getBoard().getTile(0,0);

        player.getHand().add(allyCard);
        player.getHand().add(allyCard);
        player.getHand().add(squireCard);

        player.useCard(null, gs, 0, summonLeft, 0);
        player.useCard(null, gs, 0, summonRight, 0);
        Unit ally = summonLeft.getUnit();
        Unit allyRight = summonRight.getUnit();
        ally.takeDamage(null, gs, 1);
        allyRight.takeDamage(null, gs, 1);

        int startHP = ally.getHealth(), startMax = ally.getMaxHealth(), startAttack = ally.getAttack();
        int expHP = startHP + 1, expMax = startMax + 1, expAttack = startAttack + 1;

        player.useCard(null, gs, 0, unitSummon, 0);

        assertEquals("HP should increase.", expHP, ally.getHealth());
        assertEquals("Both allies' HP should increase.", expHP, allyRight.getHealth());
        assertEquals("Max HP should increase.", expMax, ally.getMaxHealth());
        assertEquals("Both allies' max HP should increase.", expMax, allyRight.getMaxHealth());
        assertEquals("Attack should increase.", expAttack, ally.getAttack());
        assertEquals("Both allies' attack should increase.", expAttack, allyRight.getAttack());
    }

    @Test
    public void SilverguardSquireDontBuffEnemiesTest() {
        Card enemyCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json",  1, Card.class);
        Card squireCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_7_c_u_silverguard_squire.json", 1 , Card.class);
        Tile summonLeft = gs.getBoard().getTile(2,2);
        Tile unitSummon = gs.getBoard().getTile(0,0);

        opponent.getHand().add(enemyCard);
        player.getHand().add(squireCard);

        opponent.useCard(null, gs, 0, summonLeft, 0);
        Unit enemy = summonLeft.getUnit();
        enemy.takeDamage(null, gs, 1);
        int startHP = enemy.getHealth(), startMax = enemy.getMaxHealth(), startAttack = enemy.getAttack();

        player.useCard(null, gs, 0, unitSummon, 0);

        assertEquals("Enemy HP should not increase.", startHP, enemy.getHealth());
        assertEquals("Enemy max HP should not increase.", startMax, enemy.getMaxHealth());
        assertEquals("Enemy attack should not increase.", startAttack, enemy.getAttack());
    }

    @Test
    public void SilverguarSquireDontBuffAboveUnitsTest() {
            Card allyCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json",  1, Card.class);
            Card squireCard = BasicObjectBuilders.loadCard("conf/gameconfs/cards/2_7_c_u_silverguard_squire.json", 1 , Card.class);
            Tile summonUp = gs.getBoard().getTile(3,1);
            Tile summonDown = gs.getBoard().getTile(3,3);
            Tile unitSummon = gs.getBoard().getTile(0,0);

            player.getHand().add(allyCard);
            player.getHand().add(allyCard);
            player.getHand().add(squireCard);

            player.useCard(null, gs, 0, summonUp, 0);
            player.useCard(null, gs, 0, summonDown, 0);
            Unit ally = summonUp.getUnit();
            Unit allyRight = summonDown.getUnit();
            ally.takeDamage(null, gs, 1);
            allyRight.takeDamage(null, gs, 1);

            int startHP = ally.getHealth(), startMax = ally.getMaxHealth(), startAttack = ally.getAttack();

            player.useCard(null, gs, 0, unitSummon, 0);

            assertEquals("HP should not increase.", startHP, ally.getHealth());
            assertEquals("Both allies' HP should not increase.", startHP, allyRight.getHealth());
            assertEquals("Max HP should not increase.", startMax, ally.getMaxHealth());
            assertEquals("Both allies' max HP should not increase.", startMax, allyRight.getMaxHealth());
            assertEquals("Attack should not increase.", startAttack, ally.getAttack());
            assertEquals("Both allies' attack should not increase.", startAttack, allyRight.getAttack());
    }
}
