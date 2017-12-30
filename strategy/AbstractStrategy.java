package strategy;

import hlt.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractStrategy {
	protected GameMap gameMap;
	protected Map<Integer, Entity> shipTargets = new HashMap<>();

	public AbstractStrategy(GameMap gameMap) {
		this.gameMap = gameMap;
	}

	public abstract List<Move> apply();

	protected Optional<Planet> findClosestEmptyNonTargetedPlanet(Ship ship) {
		return gameMap.nearbyEntitiesByDistance(ship).entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(Map.Entry::getValue)
				.filter(e -> e instanceof Planet)
				.map(e -> (Planet) e)
				.filter(p -> !p.isFull() && !p.isOwned())
				.filter(p -> !shipTargets.values().contains(p))
				.findFirst();
	}

	protected Optional<Ship> findClosestNonTargetedEnemyShip(Ship ship) {
		return gameMap.nearbyEntitiesByDistance(ship).entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(Map.Entry::getValue)
				.filter(e -> e instanceof Ship)
				.map(e -> (Ship) e)
				.filter(s -> s.getOwner() != gameMap.getMyPlayer().getId())
				.filter(s -> !shipTargets.values().contains(s))
				.findFirst();
	}

	protected Optional<Entity> getShipTarget(Ship ship) {
		return shipTargets.containsKey(ship.getId()) ? Optional.of(shipTargets.get(ship.getId())) : Optional.empty();
	}

	protected void cleanUp() {
		Map<Integer, Ship> myShips = gameMap.getMyPlayer().getShips();
		shipTargets = shipTargets.entrySet().stream()
				.filter(e -> myShips.containsKey(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	protected Move targetTo(Ship ship, Entity target) {
		Log.log(String.format("Ship %s chooses %s as new target with distance %s.", ship.getId(), target.summary(), ship.getDistanceTo(target)));
		shipTargets.put(ship.getId(), target);
		return new Navigation().navigateShipToDock(gameMap, ship, target, Constants.MAX_SPEED);
	}
}
