package structures.basic.spells;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Board;
import structures.basic.Tile;
import structures.basic.players.Player;
import structures.basic.unittypes.Unit;

import java.util.HashSet;
import java.util.Set;

public class Truestrike extends Spell {
    @Override
    public Set<Tile> validTargets(Player player, Board board) {
        Set<Tile> validTargets = new HashSet<>();
        for (Tile[] row : board.getTiles()) {
            for (Tile tile : row) {
                // null-check required: empty tiles return null from getUnit()
                if (tile.getUnit() != null && tile.getUnit().getOwner() != player) {
                    validTargets.add(tile);
                }
            }
        }
        return validTargets;
    }

    @Override
    public void cast(ActorRef out, GameState gameState, Player player, Tile clickedTile) {
        Unit enemy = clickedTile.getUnit();
        enemy.takeDamage(out, 2);
        ;
    }
}
