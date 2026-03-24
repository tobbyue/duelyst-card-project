package structures.basic.players;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.unittypes.BetterUnit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * This class represents the human player of the game.
 * 2 different classes are required to be able to use the frontend commands.
 */
public class HumanPlayer extends Player {

    public HumanPlayer() {
        super();
    }

    @Override
    public void setHealth(ActorRef out, int health) {
        this.health = health;
        BasicCommands.setPlayer1Health(out, this);
    }

    @Override
    public void setMana(ActorRef out, int mana) {
        super.setMana(out, mana);
        BasicCommands.setPlayer1Mana(out, this);
    }

    @Override
    public void setAvatar(ActorRef out, GameState gameState) {
        this.avatar = (BetterUnit) BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, gameState.getNextUnitId(), BetterUnit.class);
        avatar.setOwner(this);
        avatar.setMaxHealth(20);
        avatar.setHealth(out, this,20);
        avatar.setAttack(out,  2);

        System.out.println("Avatar created: HP " + avatar.getHealth() + " ATK " + avatar.getAttack());
    }

    @Override
    public boolean enoughMana(ActorRef out, int manaCost) {
        boolean hasEnough =  mana >= manaCost;
        if (!hasEnough) {
            BasicCommands.addPlayer1Notification(out, "Not enough mana.", 2);
        }
        return hasEnough;
    }
    @Override
    public String toString() {
        return "Player 1";
    }
}
