package strategy;

import hlt.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MiningStrategy extends AbstractStrategy {
	private Planet globalTarget = null;
	private int turn = 0;

	public MiningStrategy(GameMap gameMap) {
		super(gameMap);
	}

	@Override
	public List<Move> apply() {
		updateTargets();
		if (globalTarget != null) globalTarget = gameMap.getPlanet(globalTarget.getId()); // update object

		List<Move> moves = gameMap.getMyPlayer().getShips().values().stream()
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
						return targetTo(ship, largestPlanet);
					}

					return targetTo(ship, globalTarget);
				})
				.collect(Collectors.toList());

		// Some additional collision avoidance
		if (turn < 6) {
			double leftmost = moves.stream().map(Move::getShip).mapToDouble(Ship::getXPos).min().orElse(-1);
			double rightmost = moves.stream().map(Move::getShip).mapToDouble(Ship::getXPos).max().orElse(-1);
			double upper = moves.stream().map(Move::getShip).mapToDouble(Ship::getYPos).min().orElse(-1);
			double lower = moves.stream().map(Move::getShip).mapToDouble(Ship::getYPos).max().orElse(-1);
			final boolean horizontal = Math.abs(leftmost - rightmost) > Math.abs(upper - lower);

			Optional<ThrustMove> tm1 = moves.stream().filter(m -> m instanceof ThrustMove).map(m -> (ThrustMove) m).filter(m -> horizontal ? m.getShip().getXPos() == leftmost : m.getShip().getYPos() == upper).findFirst();
			Optional<ThrustMove> tm2 = moves.stream().filter(m -> m instanceof ThrustMove).map(m -> (ThrustMove) m).filter(m -> horizontal ? m.getShip().getXPos() == rightmost : m.getShip().getYPos() == lower).findFirst();

			if (tm1.isPresent() && (!tm2.isPresent() || !tm1.get().equals(tm2.get()))) {
				ThrustMove m1 = tm1.get();
				moves.add(new ThrustMove(m1.getShip(), (int) Math.round(m1.getAngle() * 1.2), m1.getThrust()));
				moves.remove(m1);
			}

			if (tm2.isPresent() && (!tm1.isPresent() || !tm1.get().equals(tm2.get()))) {
				ThrustMove m2 = tm2.get();
				moves.add(new ThrustMove(m2.getShip(), (int) Math.round(m2.getAngle() * 0.8), m2.getThrust()));
				moves.remove(m2);
			}

		}

		turn++;

		return modifyAvoidCollisions(moves);
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
