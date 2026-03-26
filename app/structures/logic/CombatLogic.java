package structures.logic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Board;
import structures.basic.Tile;
import structures.basic.UnitAnimationType;
import structures.basic.players.Player;
import structures.basic.unittypes.Unit;

import java.util.Set;

public class CombatLogic {

    /**
     * Resolves the full combat sequence:
     * 1) attacker hits defender
     * 2) defender dies -> stop, no counterattack
     * 3) otherwise defender may counterattack once per turn
     */
    public static void resolveCombat(ActorRef out, GameState gameState, Unit attacker, Unit defender) {
        gameState.animationInProgress = true;
        // Active attack — wait for each animation to finish before sending the next
        try {
            Thread.sleep(BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack) / 4 * 3);
            Thread.sleep(BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.hit));
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        defender.takeDamage(out, gameState, attacker.getAttack());

        // After an attack the unit cannot attack again this turn; per Moodle FAQ
        // ("If a unit attacks and has not moved it loses its move action that turn")
        // attacking also consumes the move action.
        attacker.hasAttacked = true;
        attacker.hasMoved = true;

        // Restore idle animation after the attack sequence (SC#14)
        BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.idle);
        if (!defender.isDead()) {
            BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.idle);
        }

        // Counterattack: allowed once per turn if defender survived
        if (!defender.isDead() && !defender.hasCounterattacked) {
            try {
                Thread.sleep(BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.attack) / 4 * 3);
                Thread.sleep(BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.hit));
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            attacker.takeDamage(out, gameState, defender.getAttack());

            defender.hasCounterattacked = true;

            // Restore idle after counterattack (SC#14)
            BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.idle);
            if (!attacker.isDead()) {
                BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.idle);
            }
        }
        gameState.animationInProgress = false;
    }

    public static void death(ActorRef out, GameState gameState, Unit unit) {
        if (unit == null) return;
        unit.die(out, gameState);
    }



    /**
     * Finds the best tile to move to in order to attack the given enemy tile.
     * Returns null if no reachable attack position exists.
     */
    public static Tile findAutoAttackDestination(Unit attacker, Tile enemyTile, Board board) {
        if (attacker == null || enemyTile == null || enemyTile.getUnit() == null || board == null) {
            return null;
        }

        Tile origin = attacker.getCurrentTile();
        if (origin == null) {
            return null;
        }

        Set<Tile> moveTiles = BoardLogic.findValidMovement(origin, attacker, board);

        Tile bestTile = null;
        int bestDistance = Integer.MAX_VALUE;

        int enemyX = enemyTile.getTilex();
        int enemyY = enemyTile.getTiley();

        for (Tile moveTile : moveTiles) {
            Set<Tile> attackTilesFromMoveTile = BoardLogic.findValidAttackUnits(moveTile, attacker, board);

            if (attackTilesFromMoveTile.contains(enemyTile)) {
                int distance = Math.abs(moveTile.getTilex() - enemyX)
                        + Math.abs(moveTile.getTiley() - enemyY);

                if (bestTile == null
                        || distance < bestDistance
                        || (distance == bestDistance && moveTile.getTilex() < bestTile.getTilex())
                        || (distance == bestDistance
                            && moveTile.getTilex() == bestTile.getTilex()
                            && moveTile.getTiley() < bestTile.getTiley())) {
                    bestTile = moveTile;
                    bestDistance = distance;
                }
            }
        }

        return bestTile;
    }

    public static Set<Tile> findUnitsWithProvokeAdjacent(Board board, Unit startingUnit) {
        Set<Tile> targets = BoardLogic.findAdjacentTiles(startingUnit.getCurrentTile(), board);
        Player unitOwner = startingUnit.getOwner();
        targets.removeIf(tile -> tile.getUnit() == null
                || !tile.getUnit().hasProvoke()
                || tile.getUnit().getOwner().equals(unitOwner));
        return targets;
    }
}
