package structures.basic.spells;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Board;
import structures.basic.Tile;
import structures.basic.players.Player;
import structures.basic.unittypes.Unit;
import structures.logic.BoardLogic;

import java.util.HashSet;
import java.util.Set;

public class SundropElixir extends Spell {
    @Override
    public Set<Tile> validTargets(Player player, Board board) {
        Set<Tile> targets = new HashSet<Tile>();
        for (Unit ally : player.getUnitList().values()) {
            Tile tile = board.getTile(
                    ally.getPosition().getTilex(),
                    ally.getPosition().getTiley());
            targets.add(tile);
        }
        return targets;
    }

    @Override
    public void highlightTargets(ActorRef out, Player player, Board board) {
        Set<Tile> validTargets = validTargets(player, board);
        if (validTargets.isEmpty()) {
            BasicCommands.addPlayer1Notification(out, "No valid tiles!", 2);
        }
        ;
        for (Tile tile : validTargets) {
            BasicCommands.drawTile(out, tile, 1);
            BoardLogic.blink();
        }
    }

    @Override
    public void cast(ActorRef out, GameState gameState,
                     Player player, Tile clickedTile) {
        Unit target = clickedTile.getUnit();
        target.takeDamage(out, -4);
    }

}
