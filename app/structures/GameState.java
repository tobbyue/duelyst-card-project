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


	public HumanPlayer player1 = new HumanPlayer();
	public AIPlayer player2 = new AIPlayer();
	public Board board = new Board();
	public Unit selectedUnit = null;
	public boolean player1Turn = true;

	public float turnCount = 1;
	public boolean gameOver = false;

	/**
	 * 1-indexed hand position of the selected card, or null if none selected
	 */
	public Integer selectedHandPosition = null;

	/**
	 * Next unique unit id for summoned units.
	 */
	private int nextUnitId = 0;

	/**
	 * List of currently highlighted tiles. Used for validation.
	 */
	public Set<Tile> highlightedTiles = new HashSet<Tile>();

	/**
	 * Movement state
	 */
	public Unit movingUnit = null;
	public Tile moveTargetTile = null;
	public boolean unitMoving = false;
	public boolean animationInProgress = false;

	/**
	 * Pending move-then-attack state for story #30
	 */
	public boolean pendingAttackAfterMove = false;
	public Unit pendingAttackAttacker = null;
	public Tile pendingAttackTargetTile = null;

	/**
	 * Optional turn timer state for story #34
	 */
	public boolean turnTimerEnabled = false; // It's not very fun so im falsing this out for now
	public int turnTimeLimitSeconds = 60;
	public long currentTurnDeadlineMillis = -1L;
	public boolean timerExpiredThisTurn = false;

	public Player getPlayer1() {
		return player1;
	}

	public Player getPlayer2() {
		return player2;
	}

	public Board getBoard() {
		return board;
	}

	public Unit getSelectedUnit() {
		return selectedUnit;
	}

	public int getNextUnitId() {
		return nextUnitId++;
	}

	public void startPendingAttackAfterMove(Unit attacker, Tile targetTile) {
		this.pendingAttackAfterMove = true;
		this.pendingAttackAttacker = attacker;
		this.pendingAttackTargetTile = targetTile;
	}

	public void clearPendingAttackAfterMove() {
		this.pendingAttackAfterMove = false;
		this.pendingAttackAttacker = null;
		this.pendingAttackTargetTile = null;
	}


	public void startTurnTimer() {
		if (!turnTimerEnabled) {
			currentTurnDeadlineMillis = -1L;
			timerExpiredThisTurn = false;
			return;
		}

		currentTurnDeadlineMillis = System.currentTimeMillis() + (turnTimeLimitSeconds * 1000L);
		timerExpiredThisTurn = false;
	}

	public void stopTurnTimer() {
		currentTurnDeadlineMillis = -1L;
		timerExpiredThisTurn = false;
	}

	public boolean isTurnTimerRunning() {
		return turnTimerEnabled && currentTurnDeadlineMillis > 0;
	}

	public boolean hasTurnTimerExpired() {
		return isTurnTimerRunning() && System.currentTimeMillis() >= currentTurnDeadlineMillis;
	}

	public void resetTurnFlags(Player player) {
		if (player.getAvatar() != null) {
			player.getAvatar().hasAttacked = false;
			player.getAvatar().hasMoved = false;
			player.getAvatar().hasCounterattacked = false;
		}

		for (Unit unit : player.getUnitList().values()) {
			unit.hasAttacked = false;
			unit.hasMoved = false;
			unit.hasCounterattacked = false;
		}
	}

	public void placeAvatar(ActorRef out, BetterUnit avatar, int x, int y) {
		Tile tile = this.board.getTile(x, y);
		tile.setUnit(avatar);
		avatar.setPositionByTile(tile);

		BasicCommands.drawUnit(out, avatar, tile);
		EffectAnimation summonEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
		try { Thread.sleep(BasicCommands.playEffectAnimation(out, summonEffect, tile) / 2); }
		catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		BasicCommands.setUnitHealth(out, avatar, avatar.getHealth());
		for (int i = 0; i < 30; i++) {
			BoardLogic.blink();
		}
		BasicCommands.setUnitAttack(out, avatar, avatar.getAttack());
	}

	public void endTurn(ActorRef out, Player playerEndingTurn, Player playerStartingTurn) {
		// Stop the previous turn timer
		stopTurnTimer();

		// Increment turn count once per full round (when P2's turn ends)
			turnCount += 0.5;
		player1Turn = !player1Turn;

		// End-turn board/input cleanup
		selectedUnit = null;
		selectedHandPosition = null;
		highlightedTiles.clear();
		movingUnit = null;
		moveTargetTile = null;
		unitMoving = false;


		// Mana transfer
		int startingMana = Math.min((int) turnCount + 1, Player.getMaxMana());
		playerEndingTurn.setMana(out, 0);
		playerStartingTurn.setMana(out, startingMana);

		// Draw card for the player ending their turn
		playerEndingTurn.drawCardIntoHand();

		// Reset action flags for the player whose turn is ending
		resetTurnFlags(playerEndingTurn);

		// Do not show turn notifications or trigger AI after game over
		if (gameOver) return;

		// Turn ownership feedback + timer
		if (playerStartingTurn instanceof HumanPlayer) {
			BasicCommands.addPlayer1Notification(out, "Player Turn", 2);

			if (turnTimerEnabled) {
				startTurnTimer();
				BasicCommands.startTurnTimer(out, currentTurnDeadlineMillis);
			} else {
				stopTurnTimer();
				BasicCommands.stopTurnTimer(out);
			}
		} else {
			BasicCommands.addPlayer1Notification(out, "AI Turn", 2);
			stopTurnTimer();
			BasicCommands.stopTurnTimer(out);
		}

		// Trigger AI after control has passed to AI
		if (playerStartingTurn instanceof AIPlayer) {
			AI.AILogic.runAI(out, this, player1, player2);
		}
	}
}
