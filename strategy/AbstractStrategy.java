package strategy;

import hlt.*;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractStrategy {
	protected GameMap gameMap;
	protected Map<Integer, Entity> shipTargets = new HashMap<>();
	protected List<Integer> shipsOnHold = new LinkedList<>();

	public AbstractStrategy(GameMap gameMap) {
		this.gameMap = gameMap;
	}

	public abstract List<Move> apply();

	public abstract boolean keep();

	protected List<Planet> findClosestOwnPlanets(Ship ship) {
		return gameMap.nearbyEntitiesByDistance(ship).entrySet().stream()
				.sorted((p1, p2) -> Double.compare(p1.getKey(), p2.getKey()))
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

	protected List<Planet> findClosestEmptyPlanets(Ship ship) {
		return gameMap.nearbyEntitiesByDistance(ship).entrySet().stream()
				.sorted((p1, p2) -> Double.compare(p1.getKey(), p2.getKey()))
				.map(Map.Entry::getValue)
				.filter(e -> e instanceof Planet)
				.map(e -> (Planet) e)
				.filter(p -> !p.isOwned())
				.collect(Collectors.toList());
	}

	protected Optional<Planet> findClosestEmptyNonTargetedPlanet(Ship ship) {
		return findClosestEmptyPlanets(ship).stream()
				.filter(p -> !shipTargets.values().contains(p))
				.findFirst();
	}

	protected List<Ship> findClosestEnemyShips(Ship ship) {
		return gameMap.nearbyEntitiesByDistance(ship).entrySet().stream()
				.sorted((p1, p2) -> Double.compare(p1.getKey(), p2.getKey()))
				.map(Map.Entry::getValue)
				.filter(e -> e instanceof Ship)
				.map(e -> (Ship) e)
				.filter(s -> s.getOwner() != gameMap.getMyPlayer().getId())
				.collect(Collectors.toList());
	}

	protected List<Ship> findClosestNonTargetedEnemyShips(Ship ship) {
		return findClosestEnemyShips(ship).stream()
				.filter(s -> !shipTargets.values().contains(s))
				.collect(Collectors.toList());
	}

	protected List<Ship> findClosestTargetedEnemyShips(Ship ship) {
		return findClosestEnemyShips(ship).stream()
				.filter(s -> shipTargets.values().contains(s))
				.collect(Collectors.toList());
	}

	protected Optional<Ship> findClosestEnemyShip(Ship ship) {
		List<Ship> ships = findClosestEnemyShips(ship);
		return ships.size() > 0 ? Optional.of(ships.get(0)) : Optional.empty();
	}

	protected Optional<Ship> findClosestTargetedEnemyShip(Ship ship) {
		List<Ship> ships = findClosestTargetedEnemyShips(ship);
		return ships.size() > 0 ? Optional.of(ships.get(0)) : Optional.empty();
	}

	protected Optional<Ship> findClosestNonTargetedEnemyShip(Ship ship) {
		List<Ship> ships = findClosestNonTargetedEnemyShips(ship);
		return ships.size() > 0 ? Optional.of(ships.get(0)) : Optional.empty();
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

	protected void updateTargets() {
		shipTargets = shipTargets.entrySet().stream()
				.map(t -> {
					Map.Entry<Integer, Entity> entry = t;
					if (t.getValue() instanceof Ship) entry = new AbstractMap.SimpleEntry(t.getKey(), gameMap.getShip(t.getValue().getOwner(), t.getValue().getId()));
					if (t.getValue() instanceof Planet) entry = new AbstractMap.SimpleEntry(t.getKey(), gameMap.getPlanet(t.getValue().getId()));
					return entry;
				})
				.filter(e -> e != null && e.getKey() != null && e.getValue() != null)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	protected Move targetTo(Ship ship, Entity target) {
		Log.log(String.format("Ship %s navigates to %s with distance %s at %f, %f.", ship.getId(), target.summary(), ship.getDistanceTo(target), target.getXPos(), target.getYPos()));
		if (target == null) return new Move(Move.MoveType.Noop, ship);
		shipTargets.put(ship.getId(), target);
		Move navigationMove = new Navigation().navigateShipToDock(gameMap, ship, target, Constants.MAX_SPEED);
		return navigationMove != null ? navigationMove : new Move(Move.MoveType.Noop, ship);
	}

	protected boolean isTargetPlanetValid(Optional<Entity> target) {
		if (!target.isPresent()) return false;
		if (!(target.get() instanceof Planet)) return false;
		return isDockingCandidate((Planet) target.get());
	}

	protected boolean isDockingCandidate(Planet target) {
		if (target.isOwned()) {
			if (target.isFull()) return false;
		}
		return true;
	}

	protected boolean isTargetEnemyShipValid(Optional<Entity> target) {
		if (!target.isPresent()) return false;
		if (!(target.get() instanceof Ship)) return false;
		return true;
	}

	protected boolean mayCollide(Ship ship) {
		Collection<Ship> myShips = gameMap.getMyPlayer().getShips().values();
		return myShips.stream()
				.filter(s -> !s.equals(ship))
				.filter(s -> s.getDockingStatus().equals(Ship.DockingStatus.Undocked))
				.anyMatch(s -> ship.getDistanceTo(s) <= 5 && (ship.orientTowardsInDeg(s) <= 60 || ship.orientTowardsInDeg(s) >= 300));
	}

	protected Optional<Move> avoidCollision(Ship ship) {
		if (mayCollide(ship)) {
			if (!shipsOnHold.contains(ship.getId())) {
				Log.log(String.format("Ship %d stopping due to potential collision.", ship.getId()));
				shipsOnHold.add(ship.getId());
				return Optional.of(new Move(Move.MoveType.Noop, ship));
			}
			shipsOnHold.remove(Integer.valueOf(ship.getId())); // only hold for one turn
		}
		return Optional.empty();
	}
}
