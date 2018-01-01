package strategy;

import hlt.GameMap;
import hlt.Move;
import utils.RandomUtils;

import java.util.List;

public class StrategyFactory {
	public static AbstractStrategy chooseStrategy(GameMap map, List<Move> lastMoves, AbstractStrategy currentStrategy) {
		if (currentStrategy == null) {
			RandomUtils.setSeed(System.currentTimeMillis());
			if (map.getAllPlayers().size() == 2) return new AggressiveStrategy(map);
			if (RandomUtils.bernoulli()) return new MiningStrategy(map);
			return new BalancedStrategy(map);
		}

		if (currentStrategy instanceof AggressiveStrategy && !currentStrategy.keep()) return new BalancedStrategy(map);
		if (currentStrategy instanceof MiningStrategy && !currentStrategy.keep()) return new BalancedStrategy(map);
		return currentStrategy;
	}

}
