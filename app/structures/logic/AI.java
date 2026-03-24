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

        /**
         * SC#15: AI Movement.
         * For each AI unit that has not yet moved and cannot directly attack,
         * finds the nearest enemy and moves to the valid movement tile closest
         * to that enemy.
         *
         * @author Minghao
         */
        public static void moveUnit(ActorRef out, GameState gs) {
            List<Unit> aiUnits = new ArrayList<>(gs.getPlayer2().getUnitList().values());
            if (gs.getPlayer2().getAvatar() != null) {
                aiUnits.add(gs.getPlayer2().getAvatar());
            }

            for (Unit unit : aiUnits) {
                if (unit.hasMoved) continue;
                Tile unitTile = unit.getCurrentTile();
                if (unitTile == null) continue;

                // Already adjacent to an enemy — no need to move, will attack in attack phase
                if (!BoardLogic.findValidAttackUnits(unitTile, unit, gs.getBoard()).isEmpty()) continue;

                // Find the nearest enemy tile
                Tile nearestEnemy = findNearestEnemy(unitTile, unit, gs);
                if (nearestEnemy == null) continue;

                // Find valid movement destinations
                Set<Tile> validMoves = BoardLogic.findValidMovement(unitTile, unit, gs.getBoard());
                if (validMoves.isEmpty()) continue;

                // Pick the move tile closest to the nearest enemy
                Tile bestMove = findClosestTileToTarget(validMoves, nearestEnemy);
                if (bestMove == null || bestMove.equals(unitTile)) continue;

                // Execute the move
                unitTile.setUnit(null);
                bestMove.setUnit(unit);
                unit.setPositionByTile(bestMove);

                BasicCommands.moveUnitToTile(out, unit, bestMove);
                for (int i = 0; i < 60; i++) BoardLogic.blink();

                unit.hasMoved = true;
            }
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

            moveUnit(out, gs);
            attack(out, gs);
            gs.endTurn(out, p2, p1);
        }

        // ── helpers ──────────────────────────────────────────────────────────

        /** Returns the tile of the nearest enemy unit (or null if none exists). */
        private static Tile findNearestEnemy(Tile from, Unit unit, GameState gs) {
            Tile nearest = null;
            double minDist = Double.MAX_VALUE;
            Player owner = unit.getOwner();

            for (Tile[] row : gs.getBoard().getTiles()) {
                for (Tile tile : row) {
                    Unit occupant = tile.getUnit();
                    if (occupant != null && occupant.getOwner() != owner) {
                        double dist = tileDistance(from, tile);
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = tile;
                        }
                    }
                }
            }
            return nearest;
        }

        /** Returns the tile in candidates that is closest to the target tile. */
        private static Tile findClosestTileToTarget(Set<Tile> candidates, Tile target) {
            Tile best = null;
            double minDist = Double.MAX_VALUE;
            for (Tile tile : candidates) {
                double dist = tileDistance(tile, target);
                if (dist < minDist) {
                    minDist = dist;
                    best = tile;
                }
            }
            return best;
        }

        /** Euclidean distance between two tiles by grid coordinates. */
        private static double tileDistance(Tile a, Tile b) {
            int dx = a.getTilex() - b.getTilex();
            int dy = a.getTiley() - b.getTiley();
            return Math.sqrt(dx * dx + dy * dy);
        }
    }
}
