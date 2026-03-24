package structures.logic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.UnitAnimationType;
import structures.basic.unittypes.Unit;
import structures.basic.players.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AI {

    private static int timePassed = 0;

    public static void passTime() {
        timePassed++;
    }

    public static class AILogic {

        private AILogic() {}

        public static void summon() {
            // TODO: implement later
        }

        public void checkSummon() {
            // TODO: implement later
        }

        public static void moveUnit(Unit u) {
            // TODO: implement later
        }

        public void checkMove() {
            // TODO: implement later
        }

        /**
         * SC#11: AI Attacking.
         * For each AI unit that has not yet attacked this turn, finds valid adjacent
         * enemy targets and attacks one. Only legal attacks (adjacent enemies) are made.
         *
         * @author Minghao
         */
        public static void attack(ActorRef out, GameState gs) {
            List<Unit> aiUnits = new ArrayList<>(gs.getPlayer2().getUnitList().values());
            if (gs.getPlayer2().getAvatar() != null) {
                aiUnits.add(gs.getPlayer2().getAvatar());
            }

            for (Unit unit : aiUnits) {
                if (unit.hasAttacked) continue;
                Tile unitTile = unit.getCurrentTile();
                if (unitTile == null) continue;

                Set<Tile> validTargets = BoardLogic.findValidAttackUnits(unitTile, unit, gs.getBoard());
                if (!validTargets.isEmpty()) {
                    Tile targetTile = validTargets.iterator().next();
                    Unit target = targetTile.getUnit();

                    BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.attack);
                    for (int i = 0; i < 30; i++) BoardLogic.blink();
                    BasicCommands.playUnitAnimation(out, target, UnitAnimationType.hit);
                    for (int i = 0; i < 30; i++) BoardLogic.blink();

                    gs.dealDamage(out, unit, target);
                    unit.hasAttacked = true;
                }
            }
        }

        public void checkAttack() {
            // TODO: implement later
        }

        public static void runAI(ActorRef out, GameState gs, Player p1, Player p2) {
            // AI requires a live WebSocket connection; skip during unit tests
            if (out == null) return;

            attack(out, gs);
            gs.endTurn(out, p2, p1);
        }
    }
}
