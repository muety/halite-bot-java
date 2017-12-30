package strategy;

import hlt.*;

import java.util.*;
import java.util.stream.Collectors;

public class BalancedStrategy extends AbstractStrategy {
	private static final int MAX_OWN_DOCKINGS = 3;

	public BalancedStrategy(GameMap gameMap) {
		super(gameMap);
	}

	@Override
	public List<Move> apply() {
		cleanUp();

		return gameMap.getMyPlayer().getShips().values().stream()
				.filter(ship -> ship.getDockingStatus() == Ship.DockingStatus.Undocked)
				.map(ship -> {
					Optional<Entity> target = getShipTarget(ship);
					if (isTargetPlanetValid(target)) {
						Planet targetPlanet = (Planet) target.get();
						Log.log(String.format("Ship %s has target planet %s with distance of %s.", ship.getId(), targetPlanet.getId(), ship.getDistanceTo(targetPlanet)));
						if (ship.canDock(targetPlanet)) {
							shipTargets.remove(ship.getId());
							return new DockMove(ship, targetPlanet);
						}
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
					Optional<Planet> closestOwnPlanet = findClosestOwnPlanets(ship).stream().filter(p -> p.getDockedShips().size() < MAX_OWN_DOCKINGS).findFirst();
					Optional<Ship> closestEnemy = findClosestEnemyShip(ship);
					Optional<Ship> closestTargetedEnemy = findClosestTargetedEnemyShip(ship);

					Map<Optional, Double> weightMap = new HashMap<>();
					weightMap.put(closestEmptyPlanet, 1.0);
					weightMap.put(closestOwnPlanet, 4.0);
					weightMap.put(closestTargetedEnemy, 2.0);
					weightMap.put(closestEnemy, 2.5);

					List<Entity> priorityList = getTargetPriorityByWeightedDistance(ship, new Optional[]{closestEmptyPlanet, closestOwnPlanet, closestTargetedEnemy, closestEnemy}, weightMap);
					if (!priorityList.isEmpty()) return targetTo(ship, priorityList.get(0));

					Log.log(String.format("Ship %s can't find a good target anymore.", ship.getId()));

					return new Move(Move.MoveType.Noop, ship);
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

	}

	private boolean isTargetPlanetValid(Optional<Entity> target) {
		if (!target.isPresent()) return false;
		if (!(target.get() instanceof Planet)) return false;
		if (((Planet) target.get()).isOwned()) {
			if (target.get().getOwner() != gameMap.getMyPlayer().getId()) return false;
			if (((Planet) target.get()).getDockedShips().size() > MAX_OWN_DOCKINGS) return false;
		}
		return true;
	}

	private List<Entity> getTargetPriorityByWeightedDistance(Ship ship, Optional<Entity>[] targets, Map<Optional, Double> weightMap) {
		return Arrays.stream(targets)
				.filter(t -> t.isPresent())
				.map(Optional::get)
				.sorted(Comparator.comparingDouble(t -> ship.getDistanceTo(t) * weightMap.getOrDefault(t, 1.0)))
				.collect(Collectors.toList());
	}
}
