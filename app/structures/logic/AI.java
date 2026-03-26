package structures.logic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.unittypes.Unit;
import structures.basic.players.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class AI {

    private static int timePassed = 0;

    public static void passTime() {
        timePassed++;
    }

    public static class AILogic {

        private AILogic() {}

        /**
         * AI Unit Summoning.
         * Plays all affordable creature cards in the AI's hand onto a valid adjacent
         * tile. Keeps trying until no more creatures can be summoned this turn.
         *
         * @author Minghao
         */
        public static void summonUnits(ActorRef out, GameState gs) {
            Player ai = gs.getPlayer2();
            boolean played = true;

            while (played) {
                played = false;
                for (Card card : ai.getHand()) {
                    if (!card.isCreature()) continue;
                    if (!ai.enoughMana(out, card.getManacost())) continue;

                    Set<Tile> summonTiles = BoardLogic.findValidSummonTiles(ai, gs.getBoard());
                    if (summonTiles == null || summonTiles.isEmpty()) continue;

                    int handIndex = ai.getHand().indexOf(card);
                    if (handIndex == -1) continue;

                    Tile target = summonTiles.iterator().next();
                    ai.useCard(out, gs, handIndex, target, card.getManacost());
                    played = true;
                    break; // hand changed — restart the loop
                }
            }
        }

        /**
         * SC#17: AI Spell Usage.
         * Plays all affordable spell cards in the AI's hand that have at least one
         * valid target. Keeps trying until no more spells can be played this turn.
         *
         * @author Minghao
         */
        public static void castSpells(ActorRef out, GameState gs) {
            Player ai = gs.getPlayer2();
            boolean played = true;

            while (played) {
                played = false;
                for (Card card : ai.getHand()) {
                    if (card.isCreature()) continue;
                    if (card.getSpell() == null) continue;
                    if (!ai.enoughMana(out, card.getManacost())) continue;

                    Set<Tile> targets = card.getTargets(ai, gs.getBoard());
                    if (targets == null || targets.isEmpty()) continue;

                    int handIndex = ai.getHand().indexOf(card);
                    if (handIndex == -1) continue;

                    Tile target = targets.iterator().next();
                    ai.useCard(out, gs, handIndex, target, card.getManacost());
                    System.out.println("AI casted spell: " + card.getCardname() + " at "
                    + target.getTilex() + "," + target.getTiley());
                    played = true;
                    break; // hand changed — restart the loop
                }
            }
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

                // Send the move command first (unit still has old position so frontend
                // knows where to animate from), then update backend state
                BasicCommands.moveUnitToTile(out, unit, bestMove);
                unitTile.setUnit(null);
                bestMove.setUnit(unit);
                unit.setPositionByTile(bestMove);
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

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
                    CombatLogic.resolveCombat(out, gs, unit, target);
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
            summonUnits(out, gs);
            castSpells(out, gs);
            // Allow Rush units summoned above to act
            moveUnit(out, gs);
            attack(out, gs);
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            gs.endTurn(out, p2, p1);
        }

        // ── helpers ──────────────────────────────────────────────────────────

        /** Returns the tile of the nearest enemy unit (or null if none exists). */
        private static Tile findNearestEnemy(Tile from, Unit unit, GameState gs) {
            Tile nearest = null;
            int minDist = Integer.MAX_VALUE;
            Player owner = unit.getOwner();

            for (Tile[] row : gs.getBoard().getTiles()) {
                for (Tile tile : row) {
                    Unit occupant = tile.getUnit();
                    if (occupant != null && occupant.getOwner() != owner) {
                        int dist = tileDistance(from, tile);
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
            int minDist = Integer.MAX_VALUE;
            for (Tile tile : candidates) {
                int dist = tileDistance(tile, target);
                if (dist < minDist) {
                    minDist = dist;
                    best = tile;
                }
            }
            return best;
        }

        /** Manhattan distance between two tiles — reflects actual grid movement cost. */
        private static int tileDistance(Tile a, Tile b) {
            return Math.abs(a.getTilex() - b.getTilex()) + Math.abs(a.getTiley() - b.getTiley());
        }
    }
}
