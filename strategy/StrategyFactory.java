package strategy;

import hlt.GameMap;

public class StrategyFactory {
	public static AbstractStrategy chooseStrategy(GameMap map) {
		return new BasicStrategy(map);
	}
}
