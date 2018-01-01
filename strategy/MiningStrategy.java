package strategy;

import hlt.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MiningStrategy extends AbstractStrategy {
	private Planet globalTarget = null;

	public MiningStrategy(GameMap gameMap) {
		super(gameMap);
	}

	@Override
	public List<Move> apply() {
		updateTargets();
		if (globalTarget != null) globalTarget = gameMap.getPlanet(globalTarget.getId()); // update object

		return gameMap.getMyPlayer().getShips().values().stream()
				.map(ship -> {
					Optional<Move> m = avoidCollision(ship);
					if (avoidCollision(ship).isPresent()) return m.get();

					// Does the ship already have a target? If yes, keep following it
					Optional<Entity> target = getShipTarget(ship);
					if (isTargetPlanetValid(target)) {
						Planet targetPlanet = (Planet) target.get();
						Log.log(String.format("Ship %s has target planet %s with distance of %s.", ship.getId(), targetPlanet.getId(), ship.getDistanceTo(targetPlanet)));
						if (ship.canDock(targetPlanet)) {
							shipTargets.remove(ship.getId());
							return new DockMove(ship, targetPlanet);
						}
						return targetTo(ship, targetPlanet);
					}

					if (globalTarget == null) {
						List<Planet> closestEmptyPlanets = findClosestEmptyPlanets(ship);
						int maxSlots = closestEmptyPlanets.stream()
								.mapToInt(Planet::getDockingSpots)
								.max()
								.getAsInt();

						Planet largestPlanet = closestEmptyPlanets.stream()
								.filter(p -> p.getDockingSpots() == maxSlots)
								.findFirst().orElse(null);

						globalTarget = largestPlanet;
						return targetTo(ship, largestPlanet); // returns noop if largestPlanet is null
					}

					return targetTo(ship, globalTarget); // returns noop if largestPlanet is null
				})
				.collect(Collectors.toList());
	}

	@Override
	public boolean keep() {
		return !globalTarget.isFull();
	}

	@Override
	protected boolean isTargetPlanetValid(Optional<Entity> target) {
		if (!super.isTargetPlanetValid(target)) return false;
		return isDockingCandidate((Planet) target.get());
	}

	@Override
	protected boolean isDockingCandidate(Planet target) {
		if (!super.isDockingCandidate(target)) return false;
		if (target.isOwned() && target.getOwner() != gameMap.getMyPlayer().getId()) return false;
		return true;
	}
}
