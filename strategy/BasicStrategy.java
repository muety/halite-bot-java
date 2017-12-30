package strategy;

import hlt.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
					if (closestPlanet.isPresent()) {
						Log.log(String.format("Ship %s chooses planet %s as new target with distance %s.", ship.getId(), closestPlanet.get().getId(), ship.getDistanceTo(closestPlanet.get())));
						shipTargets.put(ship.getId(), closestPlanet.get());
						return new Navigation().navigateShipToDock(gameMap, ship, closestPlanet.get(), Constants.MAX_SPEED);
					}

					Log.log(String.format("Ship %s can't find an empty planet anymore.", ship.getId()));

					Optional<Ship> closestEnemy = findClosestEnemyShip(ship);
					if (closestEnemy.isPresent()) {
						Log.log(String.format("Ship %s chooses enemy ship %s as new target with distance %s.", ship.getId(), closestEnemy.get().getId(), ship.getDistanceTo(closestEnemy.get())));
						shipTargets.put(ship.getId(), closestEnemy.get());
						return new Navigation().navigateShipToDock(gameMap, ship, closestEnemy.get(), Constants.MAX_SPEED);
					}

					Log.log(String.format("Ship %s can't find an enemy ship anymore.", ship.getId()));

					return new Move(Move.MoveType.Noop, ship);
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

	}
}
