package strategy;

import hlt.*;

import java.util.*;
import java.util.stream.Collectors;

public class BasicStrategy extends AbstractStrategy {
	public BasicStrategy(GameMap gameMap) {
		super(gameMap);
	}

	@Override
	public List<Move> apply() {
		cleanUp();

		return gameMap.getMyPlayer().getShips().values().stream()
				.filter(ship -> ship.getDockingStatus() == Ship.DockingStatus.Undocked)
				.map(ship -> {
					Optional<Entity> target = getShipTarget(ship);
					if (target.isPresent() && target.get() instanceof Planet && !((Planet) target.get()).isFull()) {
						Planet targetPlanet = (Planet) target.get();
						Log.log(String.format("Ship %s has target planet %s with distance of %s.", ship.getId(), targetPlanet.getId(), ship.getDistanceTo(targetPlanet)));
						if (ship.canDock(targetPlanet)) return new DockMove(ship, targetPlanet);
						return Navigation.navigateShipToDock(gameMap, ship, targetPlanet, Constants.MAX_SPEED);
					}
					else if (target.isPresent() && target.get() instanceof Ship) {
						Ship targetEnemy = (Ship) target.get();
						Log.log(String.format("Ship %s has target enemy ship %s with distance of %s.", ship.getId(), targetEnemy.getId(), ship.getDistanceTo(targetEnemy)));
						return Navigation.navigateShipToDock(gameMap, ship, targetEnemy, Constants.MAX_SPEED);
					}

					shipTargets.remove(ship.getId());

					Log.log(String.format("Ship %s doesn't have a target, yet.", ship.getId()));

					Optional<Planet> closestEmptyPlanet = findClosestEmptyNonTargetedPlanet(ship);
					Optional<Planet> closestOwnPlanet = findClosestOwnPlanet(ship);
					Optional<Ship> closestEnemy = findClosestEnemyShip(ship);
					Optional<Ship> closestTargetedEnemy = findClosestTargetedEnemyShip(ship);

					Map<Optional, Double> weightMap = new HashMap<>();
					weightMap.put(closestEmptyPlanet, 1.0);
					weightMap.put(closestOwnPlanet, 1.5);
					weightMap.put(closestTargetedEnemy, 1.5);
					weightMap.put(closestEnemy, 2.0);

					Optional<Entity> bestTarget = getBestTarget(ship, new Optional[]{closestEmptyPlanet, closestOwnPlanet, closestEnemy}, weightMap);
					if (bestTarget.isPresent()) return targetTo(ship, bestTarget.get());

					Log.log(String.format("Ship %s can't find a good target anymore.", ship.getId()));

					return new Move(Move.MoveType.Noop, ship);
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

	}

	private Optional<Entity> getBestTarget(Ship ship, Optional<Entity>[] targets, Map<Optional, Double> weightMap) {
		return Arrays.stream(targets)
				.filter(t -> t.isPresent() && t.get() != null)
				.map(Optional::get)
				.sorted(Comparator.comparingDouble(t -> ship.getDistanceTo(t) * weightMap.getOrDefault(t, 1.0)))
				.findFirst();
	}
}
