package structures.basic.unittypes;

import akka.actor.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.*;
import structures.basic.players.Player;
import structures.logic.BoardLogic;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * This is a representation of a Unit on the game board.
 * A unit has a unique id (this is used by the front-end.
 * Each unit has a current UnitAnimationType, e.g. move,
 * or attack. The position is the physical position on the
 * board. UnitAnimationSet contains the underlying information
 * about the animation frames, while ImageCorrection has
 * information for centering the unit on the tile.
 * <p>
 * Extended to include combat stats (health, maxHealth, attack).
 *
 * @author Dr. Richard McCreadie
 * @author Minghao
 *
 */
public class Unit {

	@JsonIgnore
	protected static ObjectMapper mapper = new ObjectMapper(); // Jackson Java Object Serializer, is used to read java objects from a file

	int id;
	UnitAnimationType animation;
	Position position;
	UnitAnimationSet animations;
	ImageCorrection correction;
	/**
	 * The player who owns this unit. Not serialised to JSON. @author Minghao
	 */
	@JsonIgnore
	Player owner;
	/**
	 * Maximum hit points of this unit. @author Minghao
	 */
	int maxHealth;
	/**
	 * Current hit points of this unit. @author Minghao
	 */
	int health;
	/**
	 * Attack damage this unit deals per strike. @author Minghao
	 */
	int attack;
	public boolean hasMoved = true;
	public boolean hasAttacked = true;

	public Unit() {
	}

	public Unit(int id, UnitAnimationSet animations, ImageCorrection correction) {
		super();
		this.id = id;
		this.animation = UnitAnimationType.idle;

		position = new Position(0, 0, 0, 0);
		this.correction = correction;
		this.animations = animations;
	}

	public Unit(int id, UnitAnimationSet animations, ImageCorrection correction, Tile currentTile) {
		super();
		this.id = id;
		this.animation = UnitAnimationType.idle;

		position = new Position(currentTile.getXpos(), currentTile.getYpos(), currentTile.getTilex(), currentTile.getTiley());
		this.correction = correction;
		this.animations = animations;
	}


	public Unit(int id, UnitAnimationType animation, Position position, UnitAnimationSet animations,
				ImageCorrection correction) {
		super();
		this.id = id;
		this.animation = animation;
		this.position = position;
		this.animations = animations;
		this.correction = correction;
	}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public UnitAnimationType getAnimation() {
		return animation;
	}

	public void setAnimation(UnitAnimationType animation) {
		this.animation = animation;
	}

	public ImageCorrection getCorrection() {
		return correction;
	}

	public void setCorrection(ImageCorrection correction) {
		this.correction = correction;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public UnitAnimationSet getAnimations() {
		return animations;
	}

	public void setAnimations(UnitAnimationSet animations) {
		this.animations = animations;
	}

	// Extended getter/setters

	public int getHealth() {
		return health;
	}

	/**
	 * Sets health. Ensures health does not go above maximum.
	 *
	 * @param value
	 * @author Scott
	 */
	public void setHealth(ActorRef out, int value) {
		if (value < 0) {
			health = 0;
		} else if (value > maxHealth) {
			health = maxHealth;
		} else {
			health = value;
		}
	}

	public void setHealth(ActorRef out, Player player, int health) {
		setHealth(out, health);
	}

	/**
	 * Returns the player who owns this unit.
	 *
	 * @author Minghao
	 */
	public Player getOwner() {
		return owner;
	}

	/**
	 * Sets the owning player of this unit.
	 *
	 * @param owner the player to assign as owner
	 * @author Minghao
	 */
	public void setOwner(Player owner) {
		this.owner = owner;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	public void setMaxHealth(int maxHealth) {
		this.maxHealth = maxHealth;
	}


	public int getAttack() {
		return attack;
	}

	public void setAttack(ActorRef out, int attack) {
		this.attack = attack;
		BasicCommands.setUnitAttack(out, this, this.attack);
	}

	@JsonIgnore
	public void setPositionByTile(Tile tile) {
		position = new Position(tile.getXpos(), tile.getYpos(), tile.getTilex(), tile.getTiley());
	}


	/**
	 * This command automatically calls setUnitHealth to the
	 * new health value based on the damage taken, and
	 * displays it to the UI.
	 *
	 * @param out
	 * @param damage
	 * @author Scott
	 */
	@JsonIgnore
	public void takeDamage(ActorRef out, int damage) {
		setHealth(out, health - damage);
		BasicCommands.setUnitHealth(out, this, health);
	}


	/// Method for BetterUnit to override
	public void takeDamage(ActorRef out, Player player, int damage) {
		takeDamage(out, damage);
	}

	/// Way to find a unit's related subclass since I can't think of any other way to do this
	public static Class<? extends Unit> findUnitClass(String configFile) {
		Class<? extends Unit> unitClass = Unit.class;

		switch (configFile) {
			case "conf/gameconfs/units/bad_omen.json":
				unitClass = BadOmen.class;
				break;

			case "conf/gameconfs/units/bloodmoon_priestess.json":
				unitClass = BloodmoonPriestess.class;
				break;

			case "conf/gameconfs/units/gloom_chaser.json":
				unitClass = GloomChaser.class;
				break;

			case "conf/gameconfs/units/ironcliff_guardian.json":
				unitClass = IroncliffGuardian.class;
				break;

			case "conf/gameconfs/units/nightsorrow_assassin.json":
				unitClass = NightsorrowAssassin.class;
				break;

			case "conf/gameconfs/units/rock_pulveriser.json":
				unitClass = RockPulveriser.class;
				break;

			case "conf/gameconfs/units/saberspine_tiger.json":
				unitClass = SaberspineTiger.class;
				break;

			//This is disgusting and I hate it
			case "conf/gameconfs/units/shadow_watcher.json":
				unitClass = ShadowWatcher.class;
				break;

			case "conf/gameconfs/units/shadowdancer.json":
				unitClass = Shadowdancer.class;
				break;

			case "conf/gameconfs/units/silverguard_knight.json":
				unitClass = SilverguardKnight.class;
				break;

			case "conf/gameconfs/units/silverguard_squire.json":
				unitClass = SilverguardSquire.class;
				break;

			case "conf/gameconfs/units/skyrock_golem.json":
				unitClass = SkyrockGolem.class;
				break;

			case "conf/gameconfs/units/swamp_entangler.json":
				unitClass = SwampEntangler.class;
				break;

			case "conf/gameconfs/units/wraithling.json":
				unitClass = Wraithling.class;
				break;

			case "conf/gameconfs/units/young_flamewing.json":
				unitClass = YoungFlamewing.class;
				break;
		}
	return unitClass;
	}

	//Nowhere else to put this...
	public static Wraithling createWraithling(ActorRef out, Player player, GameState gameState) {
		Wraithling wraithling = (Wraithling) BasicObjectBuilders.loadUnit("conf/gameconfs/units/wraithling.json", gameState.getNextUnitId(), Wraithling.class);

		wraithling.setOwner(player);
		wraithling.setHealth(out, 1);
		wraithling.setAttack(out, 1);
		wraithling.setMaxHealth(wraithling.getHealth());
		player.getUnitList().put(wraithling.getId(), wraithling);

		System.out.println("Unit created: " + wraithling.getClass().getSimpleName());

		return wraithling;
	}

	public static void summonWraithling(ActorRef out, Tile clickedTile, Player player, GameState gameState) {
			Wraithling wraithling = createWraithling(out, player, gameState);

			wraithling.setPositionByTile(clickedTile);
			clickedTile.setUnit(wraithling);

			BasicCommands.drawUnit(out, wraithling, clickedTile);
			EffectAnimation summonEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
			try { Thread.sleep(BasicCommands.playEffectAnimation(out, summonEffect, clickedTile)); }
			catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			BasicCommands.setUnitHealth(out,wraithling,wraithling.getHealth());
			for (int i = 0; i < 10; i++) {
				BoardLogic.blink();
			}
			BasicCommands.setUnitAttack(out, wraithling, wraithling.getAttack());
	}
}

