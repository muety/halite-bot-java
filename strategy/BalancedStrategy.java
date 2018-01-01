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
		updateTargets();

		List<Move> moves = gameMap.getMyPlayer().getShips().values().stream()
				.filter(ship -> ship.getDockingStatus() == Ship.DockingStatus.Undocked)
				.map(ship -> {
					//Optional<Move> m = avoidCollision(ship);
					//if (avoidCollision(ship).isPresent()) return m.get();

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
					else if (isTargetEnemyShipValid(target)) {
						Ship targetEnemy = (Ship) target.get();
						Log.log(String.format("Ship %s has target enemy ship %s with distance of %s.", ship.getId(), targetEnemy.getId(), ship.getDistanceTo(targetEnemy)));
						return targetTo(ship, targetEnemy);
					}

					shipTargets.remove(ship.getId());

					Log.log(String.format("Ship %s doesn't have a target, yet.", ship.getId()));

					// Choose a target
					Optional<Planet> closestEmptyPlanet = findClosestEmptyNonTargetedPlanet(ship);
					Optional<Planet> closestOwnPlanet = findClosestOwnPlanets(ship).stream().filter(this::isDockingCandidate).findFirst();
					Optional<Ship> closestEnemy = findClosestEnemyShip(ship);
					Optional<Ship> closestTargetedEnemy = findClosestTargetedEnemyShip(ship);

					// Most preferable option is to go to the next empty planet.
					// If it's too far, but an enemy ship is near, attack that ship, even more if
					// another one of ours ships has also targeted the enemy, because 2 vs. 1 is always good.
					// If enemy ships and empty planets are both too far or not available at all, target a
					// planet we already own, but never dock more than 3 of our own ships at the same planet.
					Map<Optional, Double> weightMap = new HashMap<>();
					weightMap.put(closestEmptyPlanet, 1.0);
					weightMap.put(closestOwnPlanet, 4.0);
					weightMap.put(closestTargetedEnemy, 2.0);
					weightMap.put(closestEnemy, 2.5);

					List<Entity> priorityList = getTargetPriorityByWeightedDistance(ship, new Optional[]{closestEmptyPlanet, closestOwnPlanet, closestTargetedEnemy, closestEnemy}, weightMap);
					if (!priorityList.isEmpty()) return targetTo(ship, priorityList.get(0));

					Log.log(String.format("Ship %s can't find a good target anymore.", ship.getId()));

					// - No more empty planets
					// - No more planets with less than 3 of our own ships
					// - No more enemy ships
					// --> actually, if this even happens, we should have won already
					return new Move(Move.MoveType.Noop, ship);
				})
				.collect(Collectors.toList());

		return modifyAvoidCollisions(moves);
	}

	@Override
	public boolean keep() {
		return true;
	}

	@Override
	protected boolean isDockingCandidate(Planet target) {
		if (!super.isDockingCandidate(target)) return false;
		if (target.isOwned() && target.getOwner() != gameMap.getMyPlayer().getId()) return false;
		if (target.numDockedShips() >= MAX_OWN_DOCKINGS) return false;
		return true;
	}

	@Override
	protected boolean isTargetPlanetValid(Optional<Entity> target) {
		if (!super.isTargetPlanetValid(target)) return false;
		return isDockingCandidate((Planet) target.get());
	}

	private List<Entity> getTargetPriorityByWeightedDistance(Ship ship, Optional<Entity>[] targets, Map<Optional, Double> weightMap) {
		return Arrays.stream(targets)
				.filter(t -> t.isPresent())
				.map(Optional::get)
				.sorted(Comparator.comparingDouble(t -> ship.getDistanceTo(t) * weightMap.getOrDefault(t, 1.0)))
				.collect(Collectors.toList());
	}
}
