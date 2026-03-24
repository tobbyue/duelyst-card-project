package structures.basic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.players.Player;
import utils.BasicObjectBuilders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the 9x5 game board, holding a grid of Tile objects.
 * Each tile is initialised via {@link BasicObjectBuilders#loadTile(int, int)}
 * using grid coordinates (x = column 0-8, y = row 0-4).
 *
 * @author Minghao
 */
public class Board {

	public static final int BOARD_WIDTH = 9;
	public static final int BOARD_HEIGHT = 5;

	private Tile[][] tiles;

	public Board() {
		tiles = new Tile[BOARD_WIDTH][BOARD_HEIGHT];
		for (int x = 0; x < BOARD_WIDTH; x++) {
			for (int y = 0; y < BOARD_HEIGHT; y++) {
				tiles[x][y] = BasicObjectBuilders.loadTile(x, y);
			}
		}
	}

	public Tile getTile(int x, int y) {
		return tiles[x][y];
	}

	public Tile[][] getTiles() {
		return tiles;
	}

	public int getX() {
		return BOARD_WIDTH;
	}

	public int getY() {
		return BOARD_HEIGHT;
	}
}