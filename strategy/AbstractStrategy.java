package strategy;

import hlt.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractStrategy {
	protected GameMap gameMap;
	protected Map<Integer, Entity> shipTargets = new HashMap<>();

	public AbstractStrategy(GameMap gameMap) {
		this.gameMap = gameMap;
	}

	public abstract List<Move> apply();

	protected List<Planet> findClosestOwnPlanets(Ship ship) {
		return gameMap.nearbyEntitiesByDistance(ship).entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(Map.Entry::getValue)
				.filter(e -> e instanceof Planet)
				.map(e -> (Planet) e)
				.filter(p -> p.isOwned() && p.getOwner() == gameMap.getMyPlayer().getId())
				.filter(p -> !shipTargets.values().contains(p))
				.collect(Collectors.toList());
	}

	protected Optional<Planet> findClosestOwnPlanet(Ship ship) {
		List<Planet> ownPlanets = findClosestOwnPlanets(ship);
		return ownPlanets.size() > 0 ? Optional.of(ownPlanets.get(0)) : Optional.empty();
	}

	protected Optional<Planet> findClosestEmptyNonTargetedPlanet(Ship ship) {
		return gameMap.nearbyEntitiesByDistance(ship).entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(Map.Entry::getValue)
				.filter(e -> e instanceof Planet)
				.map(e -> (Planet) e)
				.filter(p -> !p.isOwned())
				.filter(p -> !shipTargets.values().contains(p))
				.findFirst();
	}

	protected Optional<Ship> findClosestEnemyShip(Ship ship) {
		return gameMap.nearbyEntitiesByDistance(ship).entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(Map.Entry::getValue)
				.filter(e -> e instanceof Ship)
				.map(e -> (Ship) e)
				.filter(s -> s.getOwner() != gameMap.getMyPlayer().getId())
				.findFirst();
	}

	protected Optional<Ship> findClosestTargetedEnemyShip(Ship ship) {
		return gameMap.nearbyEntitiesByDistance(ship).entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(Map.Entry::getValue)
				.filter(e -> e instanceof Ship)
				.map(e -> (Ship) e)
				.filter(s -> s.getOwner() != gameMap.getMyPlayer().getId())
				.filter(s -> shipTargets.values().contains(s))
				.findFirst();
	}

	protected Optional<Entity> getShipTarget(Ship ship) {
		if (!shipTargets.containsKey(ship.getId())) return Optional.empty();
		else {
			Entity target = shipTargets.get(ship.getId());
			if (target instanceof Planet && gameMap.getPlanet(target.getId()) == null) return Optional.empty();
			else if (target instanceof Ship && gameMap.getAllShips().stream().noneMatch(s -> s.getId() == target.getId())) return Optional.empty();
			return Optional.of(target);
		}
	}

	protected void cleanUp() {
		Map<Integer, Ship> allShipIds = gameMap.getAllShips().stream().collect(Collectors.toMap(Ship::getId, Function.identity()));
		shipTargets = shipTargets.entrySet().stream()
				.filter(e -> allShipIds.containsKey(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	protected Move targetTo(Ship ship, Entity target) {
		Log.log(String.format("Ship %s chooses %s as new target with distance %s.", ship.getId(), target.summary(), ship.getDistanceTo(target)));
		shipTargets.put(ship.getId(), target);
		return new Navigation().navigateShipToDock(gameMap, ship, target, Constants.MAX_SPEED);
	}
}
