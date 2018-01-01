package strategy;

import hlt.GameMap;
import hlt.Move;

import java.util.List;

public class StrategyFactory {
	public static AbstractStrategy chooseStrategy(GameMap map, List<Move> lastMoves, AbstractStrategy currentStrategy) {
		if (currentStrategy == null) {
			if (map.getAllPlayers().size() == 2) return new AggressiveStrategy(map);
			return new BalancedStrategy(map);
		}

		if (currentStrategy instanceof AggressiveStrategy && containsNoop(lastMoves)) return new BalancedStrategy(map);
		return currentStrategy;
	}

	private static boolean containsNoop(List<Move> moves) {
		return moves.stream().anyMatch(m -> m.getType().equals(Move.MoveType.Noop));
	}
}
