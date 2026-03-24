package structures.basic.players;
import akka.actor.ActorRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.unittypes.BetterUnit;
import structures.basic.Card;
import structures.basic.Deck;
import structures.basic.unittypes.Unit;
import structures.logic.BoardLogic;

/**
 * A basic representation of the Player. A player
 * has health and mana.
 * <p>
 * Extended to include the player's avatar unit, deck, hand, and card-draw logic.
 *
 * @author Dr. Richard McCreadie
 * @author Minghao
 *
 */
public class Player {

	// For use in subclasses, these are now protected instead of private
	protected int health;
	protected int mana;
	private static final int MAX_MANA = 9;
	protected BetterUnit avatar;
	protected Deck deck;
	protected List<Card> hand;
	protected Map<Integer, Unit> unitList = new HashMap<>();

	public Player() {
		super();
		this.health = 20;
		this.mana = 0;
		// Check player class and pass it to deck.
		// Return an error otherwise.
		try {
			this.deck = new Deck(this);
		}  catch (IllegalArgumentException e) {
			System.err.println(e.getMessage() + "Initialising empty deck...");
			this.deck = new Deck();
		}
		this.hand = new ArrayList<>();
	}

	public Player(int health, int mana) {
		super();
		this.health = health;
		this.mana = mana;
		this.deck = new Deck(this);
		this.hand = new ArrayList<>();
	}

	public int getHealth() { return health; }
	public void setHealth(int health) { this.health = health; }

	public void setHealth(ActorRef out, int health) {
		throw new Error("Unknown Player subclass");
	}

	public int getMana() { return mana; }
	public void setMana(ActorRef out, int mana) {
		if (mana > 9) {
			mana = 9;
		} else if (mana < 0) {
			mana = 0;
		}
		this.mana = mana;
	}

	public void setMana(int mana) {
		if (mana > 9) {
			mana = 9;
		} else if (mana < 0) {
			mana = 0;
		}
		this.mana = mana;
	}

	public static int getMaxMana() { return MAX_MANA; }

	public BetterUnit getAvatar() { return avatar; }

	public void setAvatar(ActorRef out, GameState gameState) {
		throw new Error("Unknown Player subclass");
	}

	public List<Card> getDeck() { return deck.cards; }

	// setDeck() removed. It never needs to be set after initialisation.

	public List<Card> getHand() { return hand; }

	public Map<Integer, Unit> getUnitList() { return unitList; }

	public boolean enoughMana(ActorRef out, int manaCost) { return  mana >= manaCost; }

	/**
	 * Draws the top card from the deck into the hand. Does nothing if the deck is empty or full.
	 * @author Minghao
	 * @author Scott
	 */
	public void drawCardIntoHand() { // Changed the name for less confusion with BasicCommands -- Scott
		if (deck != null && !deck.cards.isEmpty()) {
			Card drawn = deck.cards.remove(0); // always remove from deck regardless of hand size (SC#21)
			if (this.hand.size() < 6) {
				hand.add(drawn);
			}
			// if hand is full, drawn is silently discarded — not returned to deck
		}
	}

	/// Destroys all cards on screen.
	public void destroyHand(ActorRef out) {
		for (Card card : hand) {
			BasicCommands.deleteCard(out, hand.indexOf(card) + 1);
		}
	}
	/// Displays all cards in hand to the screen.
	/// @author Scott
	public void drawHand(ActorRef out) {
		for (Card card : hand) {
			BasicCommands.drawCard(out, card, hand.indexOf(card) + 1, 0);
		}
	}

	public void useCard(ActorRef out,
											 GameState gameState,
											 int cardIndex,
											 Tile clickedTile,
											 int manaCost) {

		int newMana = mana - manaCost;
		this.setMana(out, newMana);
		Card card = hand.get(cardIndex);

		if (card.isCreature()) {
			card.summon(out, gameState, this, clickedTile);
		} else {
			card.getSpell().cast(out, gameState, this, clickedTile);
		}

		destroyHand(out);
		this.getHand().remove(cardIndex);
		drawHand(out);

		BoardLogic.clearSelection(out, gameState.board);
	}

	/// Gets the current unitId, then increments the count.
	/// @author Scott
	// TODO See if this should be put in GameState or if there's already a function for that
//	protected int useUnitId() {
//		int id = unitId;
//		unitId++;
//		return id;
//	}

	public String toString() {
		return "Unknown Player";
	}
}
