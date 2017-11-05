package game.controllers.examples;

import java.util.ArrayList;

import game.controllers.AttackerController;
import game.models.Game;
import game.models.Node;
import game.models.Attacker;
import java.util.List;

public class NearestPillAttacker implements AttackerController
{
	public void init(Game game) { }
	public void shutdown(Game game) { }
	public int update(Game game,long timeDue)
	{
		List<Node> pills = game.getCurMaze().getPillNodes();
		List<Node> powerPills=game.getCurMaze().getPowerPillNodes();
		Attacker attacker = game.getAttacker();
		
		ArrayList<Node> targets=new ArrayList<Node>();

		for (Node pill : pills)
			if(game.checkPill(pill))
				targets.add(pill);

		for (Node pill : powerPills)
			if(game.checkPowerPill(pill))
				targets.add(pill);
		
		//return the next direction once the closest target has been identified
		return game.getAttacker().getNextDir(attacker.getTarget(targets,true), true);
	}
}