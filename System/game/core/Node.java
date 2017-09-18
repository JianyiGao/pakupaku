package game.core;

import java.util.Arrays;

public class Node
{
    protected Node[] neighbors = null;
    private int x, y;
    private int pillIndex, powerPillIndex;
    private int numNeighbors = 0;
    private G.Maze maze;

    public int getX()
    {
        return x;
    }
    public int getY()
    {
        return y;
    }
    public int getPillIndex()
    {
        return pillIndex;
    }
    public int getPowerPillIndex()
    {
        return powerPillIndex;
    }
    public int getNumNeighbors()
    {
        return numNeighbors;
    }

    protected Node(int _x, int _y, int _pillIndex, int _powerPillIndex, G.Maze _maze)
    {
        x = _x;
        y = _y;
        pillIndex = _pillIndex;
        powerPillIndex = _powerPillIndex;
        maze = _maze;
    }

    protected void setNeighbors(Node[] _neighbors)
    {
        neighbors = _neighbors;
        numNeighbors = 0;

        for (int index = 0; index < _neighbors.length; index++)
            if (_neighbors[index] != null)
                numNeighbors++;
    }

    //Checks of a node is a junction
    public boolean isJunction()
    {
        return numNeighbors > 2;
    }

    public Node[] getNeighbors()
    {
        return Arrays.copyOf(neighbors, neighbors.length);
    }

    //Returns the neighbour of node index that corresponds to direction. In the case of neutral, the
    //same node index is returned
	public Node getNeighbor(int inDirection)
	{
		if(inDirection < 0 || inDirection > 3) //this takes care of "neutral"
			return this;
		else
			return neighbors[inDirection];
	}

    public int getPathDistance(Node to)
    {
        return maze.distances.get(this, to);
    }

    //Returns the EUCLEDIAN distance between two nodes in the current maze.
    public double getEuclideanDistance(Node to)
    {
        int dx = x - to.x;
        int dy = y - to.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    //Returns the MANHATTAN distance between two nodes in the current maze.
    public int getManhattanDistance(Node to)
    {
        return (int)(Math.abs(x - to.x) + Math.abs(y - to.y));
    }
}
