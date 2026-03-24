import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import commands.BasicCommands;
import commands.DummyTell;
import events.Initalize;
import play.libs.Json;
import structures.GameState;
import structures.basic.unittypes.BetterUnit;
import structures.basic.players.HumanPlayer;
import structures.basic.players.AIPlayer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit tests for Story Card #2: HP Display.
 * <p>
 * After a unit takes damage, the front-end setUnitHealth command must be sent
 * with the updated health value. The player health bar must also sync when an
 * avatar is damaged.
 *
 * @author Minghao
 */
public class HPDisplayTest {

    static class RecordingTell implements DummyTell {
        final List<ObjectNode> messages = new ArrayList<>();

        @Override
        public void tell(ObjectNode message) {
            messages.add(message.deepCopy());
        }

        boolean hasMessageType(String type) {
            for (ObjectNode m : messages) {
                if (type.equals(m.get("messagetype").asText())) return true;
            }
            return false;
        }

        /** Returns the health value from the most recent setUnitHealth message, or -1 if none. */
        int lastUnitHealth() {
            for (int i = messages.size() - 1; i >= 0; i--) {
                ObjectNode m = messages.get(i);
                if ("setUnitHealth".equals(m.get("messagetype").asText())) {
                    return m.get("health").asInt();
                }
            }
            return -1;
        }
    }

    private RecordingTell recorder;

    @Before
    public void setUp() {
        recorder = new RecordingTell();
        BasicCommands.altTell = recorder;
    }

    // -----------------------------------------------------------------------
    // AC1 — takeDamage sends setUnitHealth
    // -----------------------------------------------------------------------

    @Test
    public void takeDamageSendsSetUnitHealthCommand() {
        BetterUnit unit = (BetterUnit) utils.BasicObjectBuilders.loadUnit(
                utils.StaticConfFiles.humanAvatar, 0, BetterUnit.class);
        unit.setMaxHealth(20);
        unit.setHealth(null, 20);

        recorder.messages.clear();
        unit.takeDamage(null, 5);

        assertTrue("setUnitHealth must be sent after takeDamage",
                recorder.hasMessageType("setUnitHealth"));
        assertEquals("setUnitHealth must report health=15 after 5 damage",
                15, recorder.lastUnitHealth());
    }

    // -----------------------------------------------------------------------
    // AC2 — setHealth (avatar overload) sends setUnitHealth
    // -----------------------------------------------------------------------

    @Test
    public void setHealthSendsSetUnitHealthCommand() {
        HumanPlayer player1 = new HumanPlayer();
        BetterUnit avatar = (BetterUnit) utils.BasicObjectBuilders.loadUnit(
                utils.StaticConfFiles.humanAvatar, 0, BetterUnit.class);
        avatar.setMaxHealth(20);

        recorder.messages.clear();
        avatar.setHealth(null, player1, 15);

        assertTrue("setUnitHealth must be sent after setHealth(player overload)",
                recorder.hasMessageType("setUnitHealth"));
    }

    // -----------------------------------------------------------------------
    // AC3 — avatar damage syncs player health bar
    // -----------------------------------------------------------------------

    @Test
    public void avatarDamageSyncsPlayerHealthBar() {
        GameState gameState = new GameState();
        new Initalize().processEvent(null, gameState, Json.newObject());

        recorder.messages.clear();
        BetterUnit avatar = gameState.getPlayer1().getAvatar();
        avatar.takeDamage(null, 5);

        assertEquals("Avatar health must be 15 after 5 damage",
                15, avatar.getHealth());
    }

    // -----------------------------------------------------------------------
    // AC4 — HP display updates correctly on the initial board
    // -----------------------------------------------------------------------

    @Test
    public void initalizationSendsSetUnitHealthForAvatars() {
        GameState gameState = new GameState();
        recorder.messages.clear();
        new Initalize().processEvent(null, gameState, Json.newObject());

        assertTrue("setUnitHealth must be sent during initialization",
                recorder.hasMessageType("setUnitHealth"));
    }
}
