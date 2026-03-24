import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import commands.BasicCommands;
import commands.DummyTell;
import structures.basic.unittypes.BetterUnit;
import structures.basic.players.*;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import static org.junit.Assert.*;

public class BetterUnitTest implements DummyTell {

    @Override
    public void tell(ObjectNode message) {
        // no-op: discard front-end messages during tests
    }

    @Before
    public void setUp() {
        BasicCommands.altTell = this;
    }

    @Test
    public void testPlayerHPLink() {

        HumanPlayer player1 = new HumanPlayer();
        AIPlayer player2 = new AIPlayer();

        BetterUnit playerAvatar = (BetterUnit) BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, BetterUnit.class);
        BetterUnit aiAvatar = (BetterUnit) BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, 0, BetterUnit.class);

        // Test setting health
        playerAvatar.setHealth(null, player1, 20);
        aiAvatar.setHealth(null, player2, 20);

        assertEquals(playerAvatar.getHealth(), player1.getHealth());
        assertEquals(aiAvatar.getHealth(), player2.getHealth());

        // Test taking damage
        playerAvatar.takeDamage(null, 10);
        aiAvatar.takeDamage(null, 15);

        assertEquals(player1.getHealth(), playerAvatar.getHealth());
        assertEquals(player2.getHealth(), aiAvatar.getHealth());

        // Test healing
        playerAvatar.takeDamage(null, -10);
        aiAvatar.takeDamage(null, -5);

        assertEquals(player1.getHealth(), playerAvatar.getHealth());
        assertEquals(player2.getHealth(), aiAvatar.getHealth());
    }

    @Test
    public void testBottomOut() {
        HumanPlayer player1 = new HumanPlayer();
        AIPlayer player2 = new AIPlayer();
        BetterUnit playerAvatar = (BetterUnit) BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, BetterUnit.class);
        BetterUnit aiAvatar = (BetterUnit) BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, 0, BetterUnit.class);

        playerAvatar.setHealth(null, player1, 20);
        aiAvatar.setHealth(null, player2, 20);

        playerAvatar.takeDamage(null, 50);
        aiAvatar.takeDamage(null, 5);
        aiAvatar.takeDamage(null, 16);

        assertEquals(0, playerAvatar.getHealth());
        assertEquals(0, aiAvatar.getHealth());
    }
}
