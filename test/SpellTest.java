import akka.actor.ActorRef;
import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import org.junit.Before;
import org.junit.Test;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.players.HumanPlayer;
import structures.basic.unittypes.BetterUnit;
import structures.basic.unittypes.SkyrockGolem;
import structures.basic.unittypes.Unit;
import structures.basic.unittypes.Wraithling;
import structures.logic.BoardLogic;
import utils.BasicObjectBuilders;

import static org.junit.Assert.*;

public class SpellTest {

    GameState gs = new GameState();
    HumanPlayer player = new HumanPlayer();
    CheckMessageIsNotNullOnTell altTell;

    @Before
    public void setUp() {
        CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell(); // create an alternative tell
        BasicCommands.altTell = altTell; // specify that the alternative tell should be used
        player.setAvatar(null, gs);
        player.getAvatar().setPositionByTile(gs.getBoard().getTile(3,2));
    }

    @Test
    public void TestHornOfTheForsaken() {
        BetterUnit avatar = player.getAvatar();
        avatar.setHornCharges(3);
        avatar.takeDamage(null, gs, 2);

        assertEquals("Horn charges should go down on hit.", 2, avatar.getHornCharges());
        assertEquals("Unit should take normal damage.", 18, avatar.getHealth());

        boolean hasWraithling = false;
        for (Tile tile : BoardLogic.findAdjacentTiles(avatar.getCurrentTile(), gs.getBoard())) {
            if (tile.getUnit() != null && tile.getUnit() instanceof Wraithling) {
                hasWraithling = true;
                break;
            }
        }
        assertTrue(hasWraithling);
    }

    @Test
    public void TestHornNoValidSummon() {
        BetterUnit avatar = player.getAvatar();
        avatar.setHornCharges(3);
        Unit fillerUnit = BasicObjectBuilders.loadUnit("conf/gameconfs/units/skyrock_golem.json", 2, SkyrockGolem.class);

        avatar.takeDamage(null, gs,  2);


        boolean hasWraithling = false;
        for (Tile tile : BoardLogic.findAdjacentTiles(avatar.getCurrentTile(), gs.getBoard()))
            tile.setUnit(fillerUnit);
        for (Tile tile : BoardLogic.findAdjacentTiles(avatar.getCurrentTile(), gs.getBoard()))
            if (tile.getUnit() != null && tile.getUnit() instanceof Wraithling) {
                hasWraithling = true;
                break;
            }
        assertFalse(hasWraithling);
    }
}
