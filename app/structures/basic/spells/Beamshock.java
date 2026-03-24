package structures.basic.spells;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Board;
import structures.basic.EffectAnimation;
import structures.basic.Tile;
import structures.basic.UnitAnimationType;
import structures.basic.players.Player;
import structures.basic.unittypes.BetterUnit;
import structures.basic.unittypes.Unit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.HashSet;
import java.util.Set;

public class Beamshock extends Spell {
    @Override
    public Set<Tile> validTargets(Player player, Board board) {
        Set<Tile> targets = new HashSet<Tile>();
        for (Tile[] row : board.getTiles()) {
            for (Tile tile : row) {
                if (tile.getUnit() != null && tile.getUnit().getOwner() != player && !(tile.getUnit() instanceof BetterUnit)) {
                    targets.add(tile);
                }
            }
        }
        return targets;
    }

    public void cast(ActorRef out, GameState gameState,
                     Player player, Tile clickedTile,
                     Board board, int cardIndex) {
        BasicCommands.playUnitAnimation(out, player.getAvatar(), UnitAnimationType.channel);
        EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
        try { Thread.sleep(BasicCommands.playEffectAnimation(out, effect, clickedTile)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        BasicCommands.playUnitAnimation(out, player.getAvatar(), UnitAnimationType.idle);

        Unit enemy = clickedTile.getUnit();
        enemy.hasAttacked = true;
        enemy.hasMoved = true;
    }
}
