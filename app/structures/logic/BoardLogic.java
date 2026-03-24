package structures.logic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Board;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.unittypes.Unit;
import structures.basic.players.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Store logic relating to board pathfinding and highlighting.
 */
public class BoardLogic {

	/** Creates a small pause.
	 * This is necessary for the tiles to load without encountering a BufferOverflow.
	 */
	public static void blink() {
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Resets every tile to its default (unhighlighted) state.
	 * Call before applying new highlights to avoid stale state.
	 *
	 * @author Scott
	 */
	public static void clearSelection(ActorRef out, Board board) {
		for (Tile[] row : board.getTiles()) {
			for (Tile tile : row) {
				BasicCommands.drawTile(out, tile, 0); // grey
				blink();
			}
		}
	}


	public static Set<Tile> findNeighbours(Tile startingTile, Board board){
		Set<Tile> neighbours = new HashSet<>();
		int[] xoffset = {1, -1 , 0, 0};
		int[] yoffset = {0, 0, 1, -1};

		int startingx = startingTile.getTilex();
		int startingy = startingTile.getTiley();
		for (int i = 0; i < xoffset.length; i++) {
			try {
				if (board.getTile(startingx + xoffset[i], startingy + yoffset[i]).getUnit() == null) {

					neighbours.add(board.getTiles()[startingTile.getTilex() + xoffset[i]][startingTile.getTiley() + yoffset[i]]);
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				continue;
			}
		}
		return neighbours;
	}

	public static Set<Tile> findValidMovement(Tile startingTile, Unit unit, Board board) {
		Set<Tile> result = new HashSet<>();
		Player unitOwner = unit.getOwner();
		int startx = startingTile.getTilex();
		int starty = startingTile.getTiley();

		int[] xoffset = {1, -1 , 0, 0};
		int[] yoffset = {0, 0, 1, -1};

		for (int i = 0; i < xoffset.length; i++) {
			try {
				Tile currTile = board.getTile(startx + xoffset[i], starty + yoffset[i]);
				// If adjacent tile is empty or an ally unit, find the spaces around it
				if (currTile.getUnit() == null) {
					result.addAll(findNeighbours(currTile, board));
					result.add(currTile);
				} else if (currTile.getUnit().getOwner() == unitOwner) {
					result.addAll(findNeighbours(currTile, board));
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				continue;
			}
		}
		return result;
	}


	/// Searches for all valid tiles, then highlights them.
	/// Enemy units block pathfinding.
	public static void highlightMovement(ActorRef out, Tile startingTile, Unit unit, Board board) {
		int range = 2; //unit.getMovement(); TODO create separate unit movement stats
		Set<Tile> targets = findValidMovement(startingTile, unit, board);
		for (Tile target : targets) {
			BasicCommands.drawTile(out, target, 1);
			blink();
		}
	}

	/// Finds all adjacent tiles to the given Tile.
	/// Searches both cardinal and diagonal directions.
	public static Set<Tile> findAdjacentTiles(Tile startingTile, Board board) {
		Set<Tile> result = new HashSet<>();
		int x = startingTile.getTilex();
		int y = startingTile.getTiley();

		int[] xoffset = {1, -1, 0,  0, 1,  1, -1, -1};
		int[] yoffset = {0,  0, 1, -1, 1, -1,  1, -1};
		for (int i = 0; i < xoffset.length; i++) {
			try {
				result.add(board.getTiles()[x + xoffset[i]][y + yoffset[i]]);
			} catch (ArrayIndexOutOfBoundsException e) {
				continue;
			}
		}
		return result;
	}

	public static Set<Tile> findValidAttackUnits(Tile startingTile, Unit unit, Board board) {
		Set<Tile> targets = findAdjacentTiles(startingTile, board);
		Set<Tile> validTargets = new HashSet<>();
		for (Tile target : targets) {
			if (target.getUnit() != null && target.getUnit().getOwner() != unit.getOwner()) {
				validTargets.add(target);
			}
		}
		return validTargets;
	}

	public static Set<Tile> findValidSummonTiles(Player summoner, Board board) {
		Tile[][] boardTiles = board.getTiles();
		Set<Tile> alliedUnits = new HashSet<>();
		// Find all allied units on the board
		for (Tile[] row : boardTiles) {
			for (Tile tile : row) {
				if (tile.getUnit() != null && tile.getUnit().getOwner() == summoner) {
				alliedUnits.add(tile);
				}
			}
		}

		Set<Tile> validTargets = new HashSet<>();

		// Find the valid summoning spaces
		for (Tile unitPos : alliedUnits) {
			Set<Tile> targets = findAdjacentTiles(unitPos, board);
			for (Tile target : targets) {
				if (target.getUnit() == null) {
					validTargets.add(target);
				}
			}
		}
		return validTargets;
	}


	public static void highlightAttackTiles(ActorRef out, Tile startingTile, Unit unit, Board board) {
		Set<Tile> targets = findValidAttackUnits(startingTile, unit, board);
		for  (Tile target : targets) {
			BasicCommands.drawTile(out, target, 2);
			blink();
		}
	}

	/**
	 * Highlights all empty tiles adjacent (8-directional) to any friendly unit
	 * owned by player1 in green (mode 2). Used for creature card summon targeting.
	 *
	 * @author Minghao
	 */
	public static void highlightSummonTiles(ActorRef out, Player summoner, Board board) {
		Set<Tile> targets = findValidSummonTiles(summoner, board);
		for  (Tile target : targets) {
			BasicCommands.drawTile(out, target, 2);
			blink();
		}
	}

	public static void moveSelectedUnit(ActorRef out, GameState gameState, Tile destination, Board board) {
		Unit selectedUnit = gameState.getSelectedUnit();
		Tile origin = board.getTile(selectedUnit.getPosition().getTilex(), selectedUnit.getPosition().getTiley());
		if (selectedUnit == null) return;

		if (findValidMovement(origin, selectedUnit, board).contains(destination)) {
			gameState.movingUnit = selectedUnit;
			gameState.moveTargetTile = destination;
			gameState.unitMoving = true;
		}

		BoardLogic.clearSelection(out, board);
		gameState.selectedUnit = null;
		gameState.selectedHandPosition = null;

		BasicCommands.moveUnitToTile(out, selectedUnit, destination);

	}
}

