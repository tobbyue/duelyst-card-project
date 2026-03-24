package structures.logic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Board;
import structures.basic.Tile;
import structures.basic.unittypes.Unit;
import structures.basic.UnitAnimationType;

public class CombatLogic {
    public static void tryAttackSelectedUnit(ActorRef out, GameState gameState, Tile target, Board board) {
        Unit attacker = gameState.getSelectedUnit();
        int[] attackerPosition = {attacker.getPosition().getTilex(), attacker.getPosition().getTiley()};
        Tile origin = board.getTile(attackerPosition[0], attackerPosition[1]);

        if (BoardLogic.findValidAttackUnits(origin, attacker, board).contains(target)) {

            Unit defender = target.getUnit();
            BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
            BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.hit);
            gameState.dealDamage(out, attacker, defender);

            BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.idle);
            if (defender.getHealth() > 0) {
                BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.idle);
            }

            BoardLogic.clearSelection(out, board);
            gameState.selectedUnit = null;
            gameState.selectedHandPosition = null;
        }
    }

    /**
     * Remove a dead unit from the board and UI.
     */
    public static void death(ActorRef out, GameState gameState, Unit target) {
        Tile tile = gameState.board.getTile(target.getPosition().getTilex(), target.getPosition().getTiley());

        tile.setUnit(null);
        BasicCommands.deleteUnit(out, target);
    }
}
