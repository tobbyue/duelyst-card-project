package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Board;
import structures.basic.Tile;
import structures.basic.players.AIPlayer;
import structures.basic.players.HumanPlayer;
import structures.basic.players.Player;
import structures.basic.unittypes.BetterUnit;
import structures.basic.unittypes.Unit;
import structures.basic.EffectAnimation;
import structures.logic.AI;
import structures.logic.BoardLogic;
import structures.logic.CombatLogic;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.HashSet;
import java.util.Set;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 */
public class GameState {

	public boolean gameInitalised = false;
	public boolean something = false;

	public HumanPlayer player1 =  new HumanPlayer();
	public AIPlayer player2 = new AIPlayer();
	public Board board = new Board();
	public Unit selectedUnit = null;
	public boolean player1Turn = true;

	public int turnCount = 1;

	/** 1-indexed hand position of the selected card, or null if none selected */
	public Integer selectedHandPosition = null;

	/** Next unique unit id for summoned units. Avatars already use 1 and 2 */ //TODO make fit with summon method instead
	private int nextUnitId = 3;

	/** List of currently highlighted tiles. Used for validation.*/
	public Set<Tile> highlightedTiles = new HashSet<Tile>();

	/** Movement state */
	public Unit movingUnit = null;
	public Tile moveTargetTile = null;
	public boolean unitMoving = false;

	public Player getPlayer1() { return player1; }

	public Player getPlayer2() { return player2; }

	public Board getBoard() { return board; }

	public Unit getSelectedUnit() { return selectedUnit; }

	public int getNextUnitId() { return nextUnitId++; }


	public void placeAvatar(ActorRef out, GameState gameState, BetterUnit avatar, int x, int y) {
		Tile tile = gameState.getBoard().getTile(x, y);
		tile.setUnit(avatar);
		avatar.setPositionByTile(tile);

		BasicCommands.drawUnit(out, avatar, tile);
		EffectAnimation summonEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
		try { Thread.sleep(BasicCommands.playEffectAnimation(out, summonEffect, tile)); }
		catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		BasicCommands.setUnitHealth(out, avatar, avatar.getHealth());
		for (int i = 0; i < 30; i++) {
			BoardLogic.blink();
		}
		BasicCommands.setUnitAttack(out, avatar, avatar.getAttack());
	}
	//TODO Create CombatLogic class
	/**
	 * Combat damage based on attacker attack stat.
	 */
	public void dealDamage(ActorRef out, Unit attacker, Unit target) {
		if (attacker == null || target == null) return;

		int damage = attacker.getAttack();

		if (damage <= 0) {
			BasicCommands.addPlayer1Notification(out, "Attacker has 0 attack.", 2);
			return;
		}

		dealDirectDamage(out, target, damage);
	}

	/**
	 * Direct spell / combat damage.
	 */
	public void dealDirectDamage(ActorRef out, Unit target, int damage) {
		if (target == null || damage <= 0) return;

		int newHealth = target.getHealth() - damage;
		target.setHealth(out, newHealth);

		BasicCommands.setUnitHealth(out, target, target.getHealth());

		if (target == player1.getAvatar()) {
			player1.setHealth(target.getHealth());
			BasicCommands.setPlayer1Health(out, player1);
		} else if (target == player2.getAvatar()) {
			player2.setHealth(target.getHealth());
			BasicCommands.setPlayer2Health(out, player2);
		}

		if (target.getHealth() <= 0) {
			CombatLogic.death(out, this, target);
		}
	}


	public void endTurn(ActorRef out, Player playerEndingTurn, Player playerStartingTurn) {
		if (!player1Turn) {
			turnCount++;
		}
		player1Turn = !player1Turn;
		// Refresh mana
		int startingMana = Math.min(turnCount + 1, Player.getMaxMana());
		playerStartingTurn.setMana(out, startingMana);
		playerEndingTurn.setMana(out, 0);

		// Draw card
		playerEndingTurn.drawCardIntoHand();

		// Reset flags
		playerEndingTurn.getAvatar().hasAttacked = false; playerEndingTurn.getAvatar().hasMoved = false;
		for (Unit unit : playerEndingTurn.getUnitList().values()) {
			unit.hasAttacked = false; unit.hasMoved = false;
		}

		// Run AI on AI turn
		if (playerEndingTurn instanceof HumanPlayer) {
			AI.AILogic.runAI(out, this, player1, player2);
		}
	}
}
