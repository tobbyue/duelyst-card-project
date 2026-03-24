package commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * Intercepts BasicCommands messages while still forwarding them to the UI.
 */
public class TrackingTell implements DummyTell {

    private final ActorRef out;
    private final GameState gameState;
    private final ObjectMapper mapper = new ObjectMapper();

    public TrackingTell(ActorRef out, GameState gameState) {
        this.out = out;
        this.gameState = gameState;
    }

    @Override
    public void tell(ObjectNode msg) {
        // Forward to UI
        out.tell(msg, out);
    }
}
