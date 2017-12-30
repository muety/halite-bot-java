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

					Optional<Planet> closestPlanet = findClosestEmptyNonTargetedPlanet(ship);
					Optional<Ship> closestEnemy = findClosestNonTargetedEnemyShip(ship);
					double closestPlanetDistance = closestPlanet.isPresent() ? ship.getDistanceTo(closestPlanet.get()) : Integer.MAX_VALUE / 2 - 1;
					double closestEnemyDistance = closestEnemy.isPresent() ? ship.getDistanceTo(closestEnemy.get()) : Integer.MAX_VALUE / 2 - 1;

					if (closestPlanet.isPresent() && closestEnemy.isPresent()) {
						if (closestPlanetDistance <= closestEnemyDistance * 2) targetTo(ship, closestPlanet.get());
						else targetTo(ship, closestEnemy.get());
					}
					else if (closestPlanet.isPresent()) targetTo(ship, closestPlanet.get());
					else if (closestEnemy.isPresent()) targetTo(ship, closestEnemy.get());

					Log.log(String.format("Ship %s can't find an empty planet or enemy ship anymore.", ship.getId()));

					return new Move(Move.MoveType.Noop, ship);
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

	}
}
