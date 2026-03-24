package structures.logic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Board;
import structures.basic.Tile;
import structures.basic.unittypes.Unit;
import structures.basic.UnitAnimationType;

public class CombatLogic {
    public static void tryAttackSelectedUnit(ActorRef out, GameState gameState, Tile target) {
        Unit attacker = gameState.getSelectedUnit();
        int[] attackerPosition = {attacker.getPosition().getTilex(), attacker.getPosition().getTiley()};
        Tile origin = gameState.getBoard().getTile(attackerPosition[0], attackerPosition[1]);

        if (BoardLogic.findValidAttackUnits(origin, attacker, gameState.getBoard()).contains(target)) {

            BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
            BasicCommands.playUnitAnimation(out, target.getUnit(), UnitAnimationType.hit);

            gameState.dealDamage(out, attacker, target.getUnit());

            BoardLogic.clearSelection(out, gameState.getBoard());
            gameState.selectedUnit = null;
            gameState.selectedHandPosition = null;
        }
    }


}
