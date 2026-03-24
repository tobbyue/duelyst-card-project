package structures.basic.players;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.unittypes.BetterUnit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

// Called AIPlayer for our purposes, but more realistically
// can be used as a player 2.
public class AIPlayer extends Player {

    public AIPlayer() {
        super();
    }

    @Override
    public void setHealth(ActorRef out, int health) {
        this.health = health;
        BasicCommands.setPlayer2Health(out, this);
    }

    @Override
    public void setMana(ActorRef out, int mana) {
        super.setMana(out, mana);
        BasicCommands.setPlayer2Mana(out, this);
    }

    @Override
    public void setAvatar(ActorRef out, GameState gameState) {
        this.avatar = (BetterUnit) BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, gameState.getNextUnitId(), BetterUnit.class);
        avatar.setOwner(this);
        avatar.setMaxHealth(20);
        avatar.setHealth(out, this, 20);
        avatar.setAttack(out, 2);
        System.out.println("Avatar created: HP " + avatar.getHealth() + " ATK " + avatar.getAttack());
    }
    
    @Override
    public String toString() {
        return "Player 2";
    }

}

