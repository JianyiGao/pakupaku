package game.controllers.examples;

import game.controllers.AttackerController;
import game.models.*;

import java.util.*;

public class NearestPillEscape implements AttackerController
{
	public void init(Game game) { }
	public void shutdown(Game game) { }
	public int update(Game game,long timeDue)
	{
//		List<Node> targets = game.getPillList();
//		targets.addAll(game.getPowerPillList())
//		if (targets.isEmpty())
//			targets = game.getPillList();

		Attacker attacker = game.getAttacker();


//		ArrayList<Node> targets=new ArrayList<Node>();

/*		for (Node pill : pills)
			if(game.checkPill(pill))
				targets.add(pill);*/

/*		for (Node pill : powerPills)
			if(game.checkPowerPill(pill))
				targets.add(pill);*/

		Defender closestDefender = (Defender) attacker.getTargetActor(game.getDefenders(), true);
		int closestDistance = attacker.getLocation().getPathDistance(closestDefender.getLocation());

		if (closestDistance >= 0 && closestDistance < 25)
			return attacker.getNextDir(closestDefender.getLocation(), closestDefender.isVulnerable());

		Node closestPill = attacker.getTargetNode(game.getPowerPillList(), true);
		if (closestPill == null)
			closestDistance = -1;
		else
			closestDistance = attacker.getLocation().getPathDistance(closestPill);

		if (closestDistance >= 0 && closestDistance < 25)
			return attacker.getNextDir(closestPill, true);

		//return the next direction once the closest target has been identified
		return attacker.getNextDir(attacker.getTargetNode(game.getPillList(), true), true);
	}
}