package game.controllers.examples;

import java.util.List;
import java.util.ArrayList;
import game.controllers.AttackerController;
import game.models.Game;
import game.models.Node;

/**
 * Ms. Pac-Man AI for 2011-Summer deliverables
 * @author McAuliffe
 */
public class Devastator implements AttackerController
{
	// Previous gamestate
	Game previousGameState;

	// Most current gamestate
	Game currentGameState;
	
	// Value for an invalid node
	private final int InvalidNode = 0xdead;
	// Maximum distance to look for edible ghosts
	private final int MaximumSeekEdibleGhostDistance = 50;
	// Minimum distance a ghost can come before ending the holding pattern for the power pill
	private final int MinimumDistanceToEndHoldingPattern = 10;
	
	// Closest entity Nodes
	private Node closestNodeWithPill = null;
	private Node closestNodeWithPowerPill = null;
	private Node closestNodeWithEdibleGhost = null;
	private Node closestNodeWithGhost = null;
	
	// Distances to the closest entity nodes
	private int distanceToClosestPill = InvalidNode;
	private int distanceToClosestPowerPill = InvalidNode;
	private int distanceToClosestEadibleGhost = InvalidNode;
	private int distanceToClosestGhost = InvalidNode;
	
	// States
	StateFlee flee = new StateFlee();
	StateHoldingPattern holdingPattern = new StateHoldingPattern();
	StateReevaluate reevaluate = new StateReevaluate();
	StateSeekEdibleGhosts seekEdibleGhosts = new StateSeekEdibleGhosts();
	StateSeekPill seekPill = new StateSeekPill();
	StateSeekPowerPill seekPowerPill = new StateSeekPowerPill();
	
	// Current state
	private IState currentState = reevaluate;

	enum EntityType
	{
		Ghost,
		EdibleGhost,
		Pill,
		PowerPill
	}

	enum StateType
	{
		DoNothing,
		HoldingPattern,
		SeekPowerPill,
		SeekPill,
		SeekEdibleGhosts,
		Flee,
		Reevaluate
	}

	/**
	 * Interface for all states
	 */
	private interface IState
	{
		/**
		 * Determines what the next state should be
		 * @return Next state to run, or DoNothing for no action
		 */
		public abstract StateType GetNextState();
		/**
		 * Determines what direction PacMan should travel next
		 * @return Direction to travel
		 */
		public abstract int GetNextDirection();
		/**
		 * Resets state data
		 */
		public void Reset();
	}

	/**
	 * Holding pattern waits at a power pill until a close gets close enough and then eats the
	 * pill. If successful, the next state is to eat the ghosts
	 */
	private class StateHoldingPattern implements IState
	{
		// True if we're heading towards the pill
		private boolean isHeadingToPill = false;
		// True if holding pattern should come to an end
		private boolean isEndingHoldingPattern = false;
		// Node containing the power pill we're in a holding pattern for
		private Node powerPillNode = null;

		@Override
		public StateType GetNextState()
		{
			// We're on the spot of the powerpill, start eating the nearby ghost(s)
			if (currentGameState.getAttacker().getLocation() == powerPillNode)
			{
				return StateType.SeekEdibleGhosts;
			}
			
			// Something blocked our path to the pill or it no longer exists since the closest power pill is not the one
			// we're holding for
			if (closestNodeWithPowerPill != powerPillNode)
			{
				return StateType.Reevaluate;
			}

			return StateType.DoNothing;
		}

		@Override
		public int GetNextDirection()
		{
			if (distanceToClosestGhost <= MinimumDistanceToEndHoldingPattern)
			{
				isEndingHoldingPattern = true;
			}

			int nextDirection = currentGameState.getAttacker().getNextDir(powerPillNode, isEndingHoldingPattern || isHeadingToPill);

			isHeadingToPill = !isHeadingToPill;
			return nextDirection;
		}

		@Override
		public void Reset()
		{
			powerPillNode = closestNodeWithPowerPill;
			isEndingHoldingPattern = false;
			isHeadingToPill = false;
		}
	}

	/**
	 * Seek power pill state looks for the closest power pill and heads towards it. If
	 * successful, next state will be holding pattern
	 */
	private class StateSeekPowerPill implements IState
	{
		@Override
		public StateType GetNextState()
		{
			// Make sure the pill
			if (closestNodeWithPowerPill == null)
			{
				return StateType.Reevaluate;
			}

			List<Node> neighbors = currentGameState.getAttacker().getLocation().getNeighbors();
			int directionOfTarget = currentGameState.getAttacker().getNextDir(closestNodeWithPowerPill, true);

			if (neighbors.get(directionOfTarget) == closestNodeWithPowerPill)
			{
				return StateType.HoldingPattern;
			}

			return StateType.DoNothing;
		}

		@Override
		public int GetNextDirection()
		{
			return currentGameState.getAttacker().getNextDir(closestNodeWithPowerPill, true);
		}

		@Override
		public void Reset()
		{
		}
	}

	/**
	 * Seek pill state will seek a single pill before moving to the reevaluate state
	 */
	private class StateSeekPill implements IState
	{
		private boolean isComplete = false;

		@Override
		public StateType GetNextState()
		{
			if (isComplete || closestNodeWithPill == null)
			{
				return StateType.Reevaluate;
			}

			return StateType.DoNothing;
		}

		@Override
		public int GetNextDirection()
		{
			isComplete = true;
			return currentGameState.getAttacker().getNextDir(closestNodeWithPill, true);
		}

		@Override
		public void Reset()
		{
			isComplete = false;
		}
	}

	/**
	 * Seek edible ghosts state will seek all edible ghosts up to a specific range and
	 * eat them. When no more ghosts are in range it will return to reevaluation state
	 */
	private class StateSeekEdibleGhosts implements IState
	{
		@Override
		public StateType GetNextState()
		{
			if (closestNodeWithEdibleGhost == null)
			{
				return StateType.Reevaluate;
			}
			if (distanceToClosestEadibleGhost > MaximumSeekEdibleGhostDistance)
			{
				return StateType.Reevaluate;
			}

			return StateType.DoNothing;
		}

		@Override
		public int GetNextDirection()
		{
			return currentGameState.getAttacker().getNextDir(closestNodeWithEdibleGhost, true);
		}

		@Override
		public void Reset()
		{
		}
	}

	/**
	 * Flee state will flee from the closest enemy once before returning to reevaluation
	 */
	private class StateFlee implements IState
	{
		private boolean isComplete = false;

		@Override
		public StateType GetNextState()
		{
			if (isComplete)
			{
				return StateType.Reevaluate;
			}

			return StateType.DoNothing;
		}

		@Override
		public int GetNextDirection()
		{
			isComplete = true;
			return currentGameState.getAttacker().getNextDir(closestNodeWithGhost, false);
		}

		@Override
		public void Reset()
		{
			isComplete = false;
		}
	}

	/**
	 * Reevaluate state will evaluate the current situation and will either go after an entity
	 * to eat or flee
	 */
	private class StateReevaluate implements IState
	{
		/**
		 * Checks to see if it's a good time to eat a power pill. Checks to see if all ghosts are out if the lair
		 *  and no ghosts are currently edible
		 * @return true if it's a good time to eat a power pill
		 */
		private boolean isTimeToEatPowerPill()
		{
			for (int i = 0; i < 4; i++)
			{
				if (currentGameState.getDefender(i).getLairTime() != 0 || currentGameState.getDefender(i).isVulnerable())
				{
					return false;
				}
			}

			return true;
		}

		@Override
		public StateType GetNextState()
		{
			// See if there are any edible ghosts close by that we can eat
			if (closestNodeWithEdibleGhost != null && distanceToClosestEadibleGhost <= MaximumSeekEdibleGhostDistance)
			{
				return StateType.SeekEdibleGhosts;
			}
			
			// See if it's a good time to eat a power pill
			if (closestNodeWithPowerPill != null && isTimeToEatPowerPill())
			{
				return StateType.SeekPowerPill;
			}
			
			// See if there are any pills to eat
			if (closestNodeWithPill != null)
			{
				return StateType.SeekPill;
			}

			// We're probably being chased, flee
			return StateType.Flee;
		}

		@Override
		public int GetNextDirection()
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public void Reset()
		{
		}
	}

	/**
	 * Checks to see if the specified node will is safe to move to.
	 * TODO: a lot.
	 * @return true if it's kinda safe to move to
	 */
	private boolean isNodeOkToVisit(Node node)
	{
		for (int i = 0; i < 4; ++i)
		{
			if (currentGameState.getDefender(i).isVulnerable())
			{
				continue;
			}

			// Must check to see if ghost is already in this node because getNextGhostDir will
			//   return -1 if ghost is already there, or if it can't reach that spot
			if (currentGameState.getDefender(i).getLocation() == node)
			{
				return false;
			}

			int nextGhostDir = currentGameState.getDefender(i).getNextDir(currentGameState.getDefender(i).getLocation(), true);

//			int nextGhostDir = currentGameState.getNextGhostDir(i, currentGameState.getCurGhostLoc(i), true, Game.DM.PATH);

			// Check to see if ghost can reach this node
			if (nextGhostDir == -1)
			{
				continue;
			}

			Node nextGhostNode = currentGameState.getDefender(i).getLocation().getNeighbor(nextGhostDir);
			if (node.getPathDistance(nextGhostNode) <= 4)
			{
				return false;
			}
		}

		return true;
	}
	
	/**
	 * Checks to see if the immediate path to the specied node is safe
	 * TODO: a lot more.
	 * @return true if it's safe to path towards the node
	 */
	private boolean isNodeOkToPathTo(Node node)
	{
		int nextPacManDir = currentGameState.getAttacker().getNextDir(node, true);
		if (nextPacManDir == -1)
		{
			return false;
		}

		Node nextPacManNode = currentGameState.getAttacker().getLocation().getNeighbor(nextPacManDir);
		return isNodeOkToVisit(nextPacManNode);
	}

	/**
	 * Updates all the closest entity locations along with the distances to them.
	 */
	private void UpdateEntityLocations()
	{
		closestNodeWithPill = getNodeForClosestEntity(EntityType.Pill);
		closestNodeWithPowerPill = getNodeForClosestEntity(EntityType.PowerPill);
		closestNodeWithEdibleGhost = getNodeForClosestEntity(EntityType.EdibleGhost);
		closestNodeWithGhost = getNodeForClosestEntity(EntityType.Ghost);

		distanceToClosestPill = closestNodeWithPill == null ? InvalidNode : currentGameState.getAttacker().getLocation().getPathDistance(closestNodeWithPill);
		distanceToClosestPowerPill = closestNodeWithPowerPill == null ? InvalidNode : currentGameState.getAttacker().getLocation().getPathDistance(closestNodeWithPowerPill);
		distanceToClosestEadibleGhost = closestNodeWithEdibleGhost == null ? InvalidNode : currentGameState.getAttacker().getLocation().getPathDistance(closestNodeWithEdibleGhost);
		distanceToClosestGhost = closestNodeWithGhost == null ? InvalidNode : currentGameState.getAttacker().getLocation().getPathDistance(closestNodeWithGhost);
	}
	
	//Place your game logic here to play the game as Ms Pac-Man
	private int action;
	public int getAction() { return action; }

	public void init() { }
	public void shutdown() { }
	public void update(Game game,long timeDue)
	{
		currentGameState = game;
		if (previousGameState == null)
		{
			previousGameState = game;
		}

		UpdateEntityLocations();

		int directionToMove = getDirectionToClosestPill();

		previousGameState = currentGameState;
		action = directionToMove;
	}

	/**
	 * Determines the next direction to move
	 * @return direction to move
	 */
	private int getDirectionToClosestPill()
	{
		StateType newState = currentState.GetNextState();

		while (newState != StateType.DoNothing)
		{
//			System.out.println("New state: " + newState.name());
			switch (newState)
			{
				case HoldingPattern:
					currentState = holdingPattern;
					break;
				case SeekPowerPill:
					currentState = seekPowerPill;
					break;
				case SeekPill:
					currentState = seekPill;
					break;
				case SeekEdibleGhosts:
					currentState = seekEdibleGhosts;
					break;
				case Flee:
					currentState = flee;
					break;
				default:
					currentState = reevaluate;
					break;
			}

			currentState.Reset();
			newState = currentState.GetNextState();
		}

		return currentState.GetNextDirection();
	}

	/**
	 * Checks all nodes of specified type for the closest with a valid path to it.
	 * @param type - Type of entity to find
	 * @return Node of the closest entity of type or InvalidNode if entity doesn't exist or no safe path exists to it 
	 */
	private Node getNodeForClosestEntity(EntityType type)
	{
		List<Node> nodesWithEntity = new ArrayList<Node>();;
		Node closestNode = null;
		int distanceToClosestNode = -1;

		// Get the nodes containing the specified entity type
		switch (type)
		{
			case Ghost:
				for (int index = 0; index < 4; index++)
					if (!currentGameState.getDefender(index).isVulnerable())
						nodesWithEntity.add(currentGameState.getDefender(index).getLocation());
				break;
			case EdibleGhost:
				for (int index = 0; index < 4; index++)
					if (currentGameState.getDefender(index).isVulnerable())
						nodesWithEntity.add(currentGameState.getDefender(index).getLocation());
				break;
			case Pill:
				List<Node> pillList = currentGameState.getPillList();
				for (Node pill : pillList)
					if (currentGameState.checkPill(pill))
						nodesWithEntity.add(pill);
				break;
			case PowerPill:
				List<Node> powerPillList = currentGameState.getPowerPillList();
				for (Node pill : powerPillList)
					if (currentGameState.checkPowerPill(pill))
						nodesWithEntity.add(pill);
				break;
		}

		for (Node node : nodesWithEntity)
		{
			// For edible entities, make sure there's a safe path to it
			if (type != EntityType.Ghost)
			{
				if (!isNodeOkToPathTo(node))
				{
					continue;
				}
			}

			int distanceToCurrentEntity = currentGameState.getAttacker().getLocation().getPathDistance(node);
			if (distanceToCurrentEntity == -1)
			{
				continue;
			}

			if (closestNode == null || distanceToCurrentEntity < distanceToClosestNode)
			{
				closestNode = node;
				distanceToClosestNode = distanceToCurrentEntity;
			}
		}

		return closestNode;
	}
}