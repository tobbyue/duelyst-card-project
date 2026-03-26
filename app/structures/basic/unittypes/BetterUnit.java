package structures.basic.unittypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.players.AIPlayer;
import structures.basic.players.HumanPlayer;
import structures.basic.players.Player;
import structures.logic.BoardLogic;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

public class BetterUnit extends Unit {

	Set<String> keywords;
	int hornCharges = 0;
	public final int MAX_HORN_CHARGES = 3;


	public BetterUnit() {}
	
	public BetterUnit(Set<String> keywords) {
		super();
		this.keywords = keywords;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}

	/**
	 * Story Card #2: sets unit health, updates the HP badge on the front-end,
	 * and syncs the owning player's health bar.
	 * @author Minghao
	 */
	@Override
	public void setHealth(ActorRef out, Player player, int health) {
		super.setHealth(out, health);
		BasicCommands.setUnitHealth(out, this, this.health);
		player.setHealth(out, this.health);
	}

	public int getHornCharges() {return hornCharges;}
	public void setHornCharges(int charge) {
		if (charge < 0)
			this.hornCharges = 0;
		else if (charge > 3)
			this.hornCharges = 3;
		else
			this.hornCharges = charge;
		}

	/**
	 * Avatar damage taking method.
	 * Sets the health of the unit and displays it, then sets the corresponding
	 * player's health to the same value. <br />
	 * The display is currently unimplemented.
	 * @author Scott
	 */
	@Override
	public void takeDamage(ActorRef out, GameState gameState, int damage) {
		super.takeDamage(out, gameState, damage);
		this.owner.setHealth(out, this.health);

		if (!isDead()) {
			// Summon a wraithling in a random adjacent tile if unit has horn charges
			if (damage > 0) {
				if (hornCharges > 0) {
					hornCharges--;
					List<Tile> summonable = new ArrayList<>(BoardLogic.findAdjacentTiles(this.currentTile, gameState.getBoard()));
					// Remove occupied tiles
					summonable.removeIf(tile -> tile.getUnit() != null);
					if (!summonable.isEmpty()) {
						int idx = (int) (Math.random() * summonable.size());
						Tile tile = summonable.get(idx);
						summonWraithling(out, tile, this.owner, gameState);
						Unit wraithling = tile.getUnit();
						if (!gameState.player1Turn) {
							wraithling.hasAttacked = false;
							wraithling.hasMoved = false;
						}
					}
				}

				// Trigger Zeal in available allies
				for (Unit ally : owner.getUnitList().values())
					if (ally.hasZeal())
						ally.setAttack(null, ally.getAttack() + 2);
			}
		}

		// Win condition: avatar death ends the game
		if (this.owner.getHealth() <= 0) {
			gameState.gameOver = true;
			if (this.owner instanceof HumanPlayer)
				BasicCommands.addPlayer1Notification(out, "You Lose!", 5);
			else
				BasicCommands.addPlayer1Notification(out, "You Win!", 5);
		}

	}
	
	
	public static void main(String[] args) {
		
		BetterUnit unit = (BetterUnit)BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, BetterUnit.class);
		Set<String> keywords = new HashSet<>();
		keywords.add("MyKeyword");
		unit.setKeywords(keywords);
		
		System.err.println(unit.getClass());
		
	}
}
