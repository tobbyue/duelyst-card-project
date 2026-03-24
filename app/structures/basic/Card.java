package structures.basic;


import akka.actor.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.players.Player;
import structures.basic.spells.*;
import structures.basic.unittypes.Unit;
import structures.logic.BoardLogic;
import utils.BasicObjectBuilders;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is the base representation of a Card which is rendered in the player's hand.
 * A card has an id, a name (cardname) and a manacost. A card then has a large and mini
 * version. The mini version is what is rendered at the bottom of the screen. The big
 * version is what is rendered when the player clicks on a card in their hand.
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Card {

	int id;

	String cardname;
	int manacost;

	MiniCard miniCard;
	BigCard bigCard;

	boolean isCreature;
	String unitConfig;

	@JsonIgnore
	Spell spell = null;

	public Card() {
	}

	;
	public Card(int id, String cardname, int manacost, MiniCard miniCard, BigCard bigCard, boolean isCreature, String unitConfig) {
		super();
		this.id = id;
		this.cardname = cardname;
		this.manacost = manacost;
		this.miniCard = miniCard;
		this.bigCard = bigCard;
		this.isCreature = isCreature;
		this.unitConfig = unitConfig;
		setSpell();
		if (spell != null)
			spell.setCard(this);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCardname() {
		return cardname;
	}

	public void setCardname(String cardname) {
		this.cardname = cardname;
	}

	public int getManacost() {
		return manacost;
	}

	public void setManacost(int manacost) {
		this.manacost = manacost;
	}

	public MiniCard getMiniCard() {
		return miniCard;
	}

	public void setMiniCard(MiniCard miniCard) {
		this.miniCard = miniCard;
	}

	public BigCard getBigCard() {
		return bigCard;
	}

	public void setBigCard(BigCard bigCard) {
		this.bigCard = bigCard;
	}

	public boolean getIsCreature() {
		return isCreature;
	}

	public void setIsCreature(boolean isCreature) {
		this.isCreature = isCreature;
	}

	public void setCreature(boolean isCreature) {
		this.isCreature = isCreature;
	}

	public boolean isCreature() {
		return isCreature;
	}

	public String getUnitConfig() {
		return unitConfig;
	}

	public void setUnitConfig(String unitConfig) {
		this.unitConfig = unitConfig;
	}

	public Spell getSpell() {
		return this.spell;
	}

	public void setSpell() {
		Class<? extends Spell> spellClass = findSpell(this.cardname);
		if (spellClass != null) {
			try {
				this.spell = spellClass.getDeclaredConstructor().newInstance();
			} catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
				System.err.println("Whoops!");
			} catch (NoSuchMethodException e) {
				System.err.println("No such method found!");
			}
		}
	}

	///  Creates a unit and casts it to its relevant subclass.
	///  Takes information from the BigCard to fill in attributes.
	public Unit createUnit(ActorRef out, GameState gameState, Player player) {
		Class<? extends Unit> unitClass = Unit.findUnitClass(unitConfig);
		Unit summonedUnit = BasicObjectBuilders.loadUnit(this.unitConfig, gameState.getNextUnitId(), unitClass);
		summonedUnit = unitClass.cast(summonedUnit);

		summonedUnit.setOwner(player);
		player.getUnitList().put(summonedUnit.getId(), summonedUnit);
		summonedUnit.setAttack(out, this.bigCard.attack);
		summonedUnit.setMaxHealth(this.bigCard.health);
		summonedUnit.setHealth(out, this.bigCard.health);

		System.out.println("Unit created: " + summonedUnit.getClass());
		System.out.println("Owner: " + player);

		return summonedUnit;
	}

	/// Summons a unit from a card.
	/// Needs a card so does not work with Wraithling specifically.
	public void summon(ActorRef out, GameState gameState, Player player, Tile clickedTile) {
		Unit summonedUnit = createUnit(out, gameState, player);

		summonedUnit.setPositionByTile(clickedTile);
		clickedTile.setUnit(summonedUnit);

		BasicCommands.drawUnit(out, summonedUnit, clickedTile);
		for (int i = 0; i < 30; i++)
			BoardLogic.blink();
		BasicCommands.setUnitAttack(out, summonedUnit, summonedUnit.getAttack());
		for (int i = 0; i < 10; i++)
			BoardLogic.blink();
		BasicCommands.setUnitHealth(out, summonedUnit, summonedUnit.getHealth());

		System.out.println("Unit summoned: " + summonedUnit.getClass());
		System.out.println("HP: " + summonedUnit.getHealth() + " ATK " + summonedUnit.getAttack() + " at (" +
				clickedTile.getTilex() + "," + clickedTile.getTiley() + ")");

		// Opening gambit usage
		summonedUnit.openingGambit(out, gameState);
	}

	/// Auto-decides on whether to use spell highlighting method or summon highlighting based on isCreature attribute.
	public Set<Tile> getTargets(Player player, Board board) {
		if (this.isCreature()) {
			return BoardLogic.findValidSummonTiles(player, board);
		} else {
			if (this.spell == null) return new HashSet<>();
			return this.spell.validTargets(player, board);
		}
	}

	public void highlightTargets(ActorRef out, Player player, Board board) {
		if (this.isCreature()) {
			BoardLogic.highlightSummonTiles(out, player, board);
		} else {
			if (this.spell != null) this.spell.highlightTargets(out, player, board);
		}
	}

	/// Method to find the relevant spell's class
	public Class<? extends Spell> findSpell(String spellName) {
		Class<? extends Spell> spellClass = null;
		switch (spellName) {
			case "Dark Terminus":
				spellClass = DarkTerminus.class;
				break;

			case "Wraithling Swarm":
				spellClass = WraithlingSwarm.class;
				break;

			case "Horn of the Forsaken":
				spellClass = HornOfTheForsaken.class;
				break;

			case "Sundrop Elixir":
				spellClass = SundropElixir.class;
				break;

			case "True Strike":
			case "Truestrike":
				spellClass = Truestrike.class;
				break;

			case "Beam Shock":
			case "Beamshock":
				spellClass = Beamshock.class;
				break;
		}
		return spellClass;
	}
}

