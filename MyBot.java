import hlt.GameMap;
import hlt.Networking;
import strategy.AbstractStrategy;
import strategy.StrategyFactory;

// TODO: Avoid collisions among own ships (especially in the beginning)
// TODO: Dynamically adapt different strategies

public class MyBot {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("n1try-basic-v1.9");

        AbstractStrategy currentStrategy = StrategyFactory.chooseStrategy(gameMap);

        while (true) {
            networking.updateMap(gameMap);
            Networking.sendMoves(currentStrategy.apply());
        }
    }
}
