import hlt.GameMap;
import hlt.Log;
import hlt.Move;
import hlt.Networking;
import strategy.AbstractStrategy;
import strategy.StrategyFactory;

import java.util.LinkedList;
import java.util.List;

// TODO: Avoid collisions among own ships (especially in the beginning)
// TODO: Dynamically adapt different strategies
// TODO: AggressiveStrategy: Instantly attack opponents
// TODO: MiningStrategy: Dock all ships at largest planet until it's full and spam ships afterrwads

public class MyBot {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("n1try-basic-v1.12");
        List<Move> lastMoves = new LinkedList<>();

        AbstractStrategy currentStrategy = null;

        while (true) {
            networking.updateMap(gameMap);
            currentStrategy = StrategyFactory.chooseStrategy(gameMap, lastMoves, currentStrategy);
            Log.log(String.format("Choose %s.", currentStrategy.getClass().getSimpleName()));
            lastMoves = currentStrategy.apply();
            Networking.sendMoves(lastMoves);
        }
    }
}
