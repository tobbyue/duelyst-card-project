package structures.basic.unittypes;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Tile;

public class GloomChaser extends Unit {

    @Override
    public void openingGambit(ActorRef out, GameState gameState) {
        int x = this.position.getTilex();
        int y = this.position.getTiley();
        Tile tile = gameState.getBoard().getTile(x, y);
        try {
            tile = gameState.board.getTile(x - 1,y); // Tile behind unit (from player perspective)
        } catch (ArrayIndexOutOfBoundsException ignored) {}
        if (tile.getUnit() == null)
            summonWraithling(out, tile, this.owner, gameState);
    }
}
