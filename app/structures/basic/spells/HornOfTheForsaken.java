package structures.basic.spells;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Board;
import structures.basic.Tile;
import structures.basic.players.Player;
import structures.logic.BoardLogic;

import java.util.HashSet;
import java.util.Set;

public class HornOfTheForsaken extends Spell {

    // Returns only the related avatar unit.
    @Override
    public Set<Tile> validTargets(Player player, Board board) {
        Set<Tile> targets =  new HashSet<Tile>();
        int x = player.getAvatar().getPosition().getTilex();
        int y = player.getAvatar().getPosition().getTiley();
        Tile target = board.getTile(x, y);
        targets.add(target);
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
        player.getAvatar().setHornCharges(player.getAvatar().MAX_HORN_CHARGES);
        BasicCommands.addPlayer1Notification(out, "Horn equipped", 1);
    }
}

