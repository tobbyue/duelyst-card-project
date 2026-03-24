package structures.basic.unittypes;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Tile;
import structures.logic.BoardLogic;

import java.util.HashSet;
import java.util.Set;

public class SilverguardSquire extends Unit {
    public void openingGambit(ActorRef out, GameState gameState) {
        BetterUnit avatar = this.owner.getAvatar();
        int coordx = avatar.getPosition().getTilex(), coordy = avatar.getPosition().getTiley();
        Set<Tile> validTargets = new HashSet<>();
        try {validTargets.add(gameState.getBoard().getTile(coordx - 1,coordy));} catch (IndexOutOfBoundsException ignored) {}
        try {validTargets.add(gameState.getBoard().getTile(coordx + 1,coordy));} catch (IndexOutOfBoundsException ignored) {}
        validTargets.removeIf(tile -> tile.getUnit() == null || tile.getUnit().getOwner() != this.owner);

        for (Tile tile : validTargets) {
            Unit unit = tile.getUnit();
            unit.setMaxHealth(unit.getMaxHealth() + 1);
            unit.setHealth(out, unit.getHealth() + 1);
            unit.setAttack(out, unit.getAttack() + 1);
        }

    }
}
