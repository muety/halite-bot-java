package strategy;

import hlt.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AggressiveStrategy extends AbstractStrategy {
	public AggressiveStrategy(GameMap gameMap) {
		super(gameMap);
	}

	@Override
	public List<Move> apply() {
		updateTargets();

		return gameMap.getMyPlayer().getShips().values().stream()
				.map(ship -> {
					// Does the ship already have a target? If yes, keep following it
					Optional<Entity> target = getShipTarget(ship);
					if (isTargetEnemyShipValid(target)) {
						Ship targetShip = (Ship) target.get();
						Log.log(String.format("Ship %d has valid target ship %d.", ship.getId(), target.get().getId()));
						return targetTo(ship, targetShip);
					}

					Optional<Ship> potentialTarget = findClosestNonTargetedEnemyShip(ship);
					if (!potentialTarget.isPresent()) potentialTarget = findClosestTargetedEnemyShip(ship);
					return targetTo(ship, potentialTarget.orElse(null));
				})
				.collect(Collectors.toList());
	}
}
