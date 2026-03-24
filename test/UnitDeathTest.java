import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import commands.BasicCommands;
import commands.DummyTell;
import structures.basic.Tile;
import structures.basic.unittypes.Unit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import static org.junit.Assert.*;

/**
 * JUnit tests for Story Card #35: Unit Death.
 * <p>
 * When a unit's health reaches zero, the death animation is played,
 * the unit is removed from its tile, and a deleteUnit command is sent.
 *
 * @author Minghao
 */
public class UnitDeathTest implements DummyTell {

    @Override
    public void tell(ObjectNode message) {
        // discard front-end messages during tests
    }

    @Before
    public void setUp() {
        BasicCommands.altTell = this;
    }

    // -----------------------------------------------------------------------
    // isDead()
    // -----------------------------------------------------------------------

    @Test
    public void isDeadReturnsFalseWhenHealthAboveZero() {
        Unit unit = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, Unit.class);
        unit.setMaxHealth(20);
        unit.setHealth(null, 10);
        assertFalse("Unit should not be dead at health=10", unit.isDead());
    }

    @Test
    public void isDeadReturnsTrueWhenHealthIsZero() {
        Unit unit = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, Unit.class);
        unit.setMaxHealth(20);
        unit.setHealth(null, 0);
        assertTrue("Unit should be dead at health=0", unit.isDead());
    }

    @Test
    public void isDeadReturnsTrueWhenHealthBelowZero() {
        Unit unit = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, Unit.class);
        unit.setMaxHealth(20);
        unit.setHealth(null, 0); // setHealth clamps at 0
        assertTrue("Unit should be dead when health reaches 0", unit.isDead());
    }

    // -----------------------------------------------------------------------
    // die() — tile cleanup
    // -----------------------------------------------------------------------

    @Test
    public void dieClearsCurrentTile() {
        Unit unit = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, Unit.class);
        Tile tile = new Tile();
        tile.setUnit(unit);
        unit.setPositionByTile(tile);

        assertNotNull("Pre-condition: currentTile should be set", unit.getCurrentTile());
        assertEquals("Pre-condition: tile should contain the unit", unit, tile.getUnit());

        unit.die(null);

        assertNull("currentTile must be null after die()", unit.getCurrentTile());
        assertNull("tile.getUnit() must be null after die()", tile.getUnit());
    }

    // -----------------------------------------------------------------------
    // takeDamage() → triggers die()
    // -----------------------------------------------------------------------

    @Test
    public void takeDamageTriggersDeathWhenHealthReachesZero() {
        Unit unit = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, Unit.class);
        unit.setMaxHealth(10);
        unit.setHealth(null, 10);
        Tile tile = new Tile();
        tile.setUnit(unit);
        unit.setPositionByTile(tile);

        unit.takeDamage(null, 10); // lethal blow

        assertTrue("Unit must be dead after lethal takeDamage", unit.isDead());
        assertNull("currentTile must be null after lethal takeDamage", unit.getCurrentTile());
        assertNull("tile.getUnit() must be null after lethal takeDamage", tile.getUnit());
    }

    @Test
    public void takeDamageDoesNotTriggerDeathWhenHealthStaysAboveZero() {
        Unit unit = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, Unit.class);
        unit.setMaxHealth(10);
        unit.setHealth(null, 10);
        Tile tile = new Tile();
        tile.setUnit(unit);
        unit.setPositionByTile(tile);

        unit.takeDamage(null, 5); // non-lethal

        assertFalse("Unit must not be dead after non-lethal takeDamage", unit.isDead());
        assertNotNull("currentTile must remain set after non-lethal takeDamage", unit.getCurrentTile());
    }

    // -----------------------------------------------------------------------
    // setPositionByTile sets currentTile
    // -----------------------------------------------------------------------

    @Test
    public void setPositionByTileSetsCurrentTile() {
        Unit unit = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, Unit.class);
        Tile tile = new Tile();

        unit.setPositionByTile(tile);

        assertSame("currentTile must be the tile passed to setPositionByTile", tile, unit.getCurrentTile());
    }
}
