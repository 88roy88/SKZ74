package bots;

import java.util.*;
import pirates.game.Direction;
import pirates.game.Location;
import pirates.game.Treasure;
import pirates.game.Pirate;
import pirates.game.PirateBot;
import pirates.game.PirateGame;

public class MyBot implements PirateBot
{
	HashMap<Pirate, Integer> blockings = new HashMap<Pirate, Integer>(), removals = new HashMap<Pirate, Integer>();
	Pirate collector2 = null, collector3 = null;
	public int inMe=0,T;
	public int[] shots = new int[4];
	public boolean COL2=false,col2=false, checkingJihad = true;
	public List<Pirate>enemyToKill=null;
	public void doTurn(PirateGame game)
	{
		for(int i=0;i<4;i++)	shots[i] = 0;
		boolean def=false;
		if(checkingJihad) { 
			enemyToKill = checkJihad(game);
			checkingJihad = false;
		}
		if(game.allEnemyPirates().size()==1) One(game);
		else{
			game.debug("working");
			int c = game.getActionsPerTurn();

			Pirate collector = game.allMyPirates().get(0), distress = null;

			if(hadTreasure(game,collector)) inMe++;
			if(inMe==1) T=game.getTurn();
			if(inMe>3) {
				col2=true;
			}
			if(T+100==game.getTurn()){
				inMe=0;
			}

			if(col2)
			{
				game.debug("DH!");
				collector2 = game.allMyPirates().get(1);
			//	collector3 = game.allMyPirates().get(2);
			}

			HashMap<Pirate, Location> destinations = new HashMap<Pirate, Location>(), steps = new HashMap<Pirate, Location>();
			HashMap<Pirate, Integer> allocatedMoves = new HashMap<Pirate, Integer>();
			ArrayList<Pirate> jumps = new ArrayList<Pirate>();

			ArrayList<Pirate> remove_b = new ArrayList<Pirate>(), remove_r = new ArrayList<Pirate>();
			for (Pirate i : blockings.keySet()) if (i.getTurnsToRevive() > 0 || i.getTurnsToSober() > 0
					|| !game.getEnemyPirate(blockings.get(i)).hasTreasure()) remove_b.add(i);
			for (Pirate i : removals.keySet()) if (i.getTurnsToRevive() > 0 || i.getTurnsToSober() > 0
					|| !inBase(game, game.getEnemyPirate(removals.get(i)))) remove_r.add(i);

			blockings.keySet().removeAll(remove_b);
			removals.keySet().removeAll(remove_r);


			for (Pirate i : game.myLostPirates()) if (i.getTurnsToRevive() < 0) distress = i;
			for (Pirate i : game.enemySoberPirates())
				if (!removals.containsValue(i.getId()) && inBase(game, i))
				{
					Pirate pirate = available(game);
					if (pirate != null) removals.put(pirate, i.getId());
				}

			List<Pirate> p = game.myPirates();
			Collections.reverse(p);

			for (Pirate pirate : p) steps.put(pirate, pirate.getLocation());

			for (Pirate pirate : p)
				if (pirate.getTurnsToSober() <= 0)
				{
					Pirate target = check(game, pirate);
					if (!pirate.hasTreasure() && pirate.getReloadTurns() == 0 && target != null){
						shots[target.getId()]=1;
						if(!(pirate==collector && def)) game.attack(pirate, target);
					}

					else
						if (pirate == collector) // collector
						{
							game.debug("Ex"+pirate.getDefenseExpirationTurns()+" ReDef"+pirate.getDefenseReloadTurns());
							if(col2 && minDist(game, pirate, 9)!=null && minDist(game, pirate, 9).getReloadTurns()<3 && minDist(game, pirate, 9).getTurnsToSober()<3 && !minDist(game, pirate, 9).hasTreasure() && pirate.getDefenseReloadTurns()==0 && pirate.getDefenseExpirationTurns()==0) {
								game.debug("defend!");
								
								def=true;
							}
							if (def) game.defend(pirate);
							game.debug("def= "+def);
							Location destination = null;
							Treasure treasure = checkTreasure(game, pirate);

							if (treasure != null && !pirate.hasTreasure()) destination = treasure.getLocation();
							else
							{
								if (!pirate.hasTreasure() && treasure == null) destination = game.enemyPiratesWithTreasures().get(0).getLocation();

								else if (pirate.getLocation().col == pirate.getInitialLocation().col) destination = pirate.getInitialLocation();
								else destination = new Location(pirate.getLocation().row, pirate.getInitialLocation().col);
							}

							if (!pirate.hasTreasure()) game.setSail(pirate, game.getSailOptions(pirate, destination, c).get(0));
							else
							{
								Location step = game.getSailOptions(pirate, destination, 1).get(0);
								if (!threatened(game, step)) if (!(pirate==collector && def)) game.setSail(pirate, game.getSailOptions(pirate, destination, 1).get(0));
							}
						}

						else // jihadists
						{
							int indexOfShip = game.myPirates().indexOf(pirate);
							Pirate jihadTarget;
							if(indexOfShip>=enemyToKill.size())
								jihadTarget = enemyToKill.get(indexOfShip-enemyToKill.size());
							else
								jihadTarget = enemyToKill.get(indexOfShip);
							List<Location> jihadDestinations = getJihadTarget(game, pirate, jihadTarget);
							Location destination = jihadDestinations.get(0);                          
							if (distress != null)
							{
								destination = distress.getInitialLocation();
								distress = null;
							}

							if (removals.containsKey(pirate)) destination = game.getEnemyPirate(removals.get(pirate)).getLocation();

							Pirate col = checkCol(game, pirate);
							if (pirate != collector2 && pirate != collector3 && pirate.getReloadTurns() > 0 && col != null && !blockings.containsValue(col.getId()))
								blockings.put(pirate, col.getId());
							if (blockings.containsKey(pirate)) destination = game.getEnemyPirate(blockings.get(pirate)).getInitialLocation();

							if (pirate == collector2 || pirate == collector3)  		//DH!
							{
								Treasure treasure = checkTreasure(game, pirate);

								if (treasure != null && !pirate.hasTreasure()) destination = treasure.getLocation();
								else
								{
									if (!pirate.hasTreasure() && treasure == null) destination = game.enemyPiratesWithTreasures().get(0).getLocation();

									else if (pirate.getLocation().col == pirate.getInitialLocation().col) destination = pirate.getInitialLocation();
									else destination = new Location(pirate.getLocation().row, pirate.getInitialLocation().col);
								}
							}

							if (!pirate.getLocation().equals(destination))
								if (collector.getTurnsToSober() <= 0 && !collector.hasTreasure()) 
								{
									int moves = minDis(game, pirate) ? 2 : 1;
									Location step = game.getSailOptions(pirate, destination, moves).get(0);
									if (game.getPirateOn(step) != null && game.getPirateOn(step).getOwner() == 0) moves = 3 - moves;
									game.setSail(pirate, game.getSailOptions(pirate, destination, moves).get(0));
									c-= moves;
								}
								else
								{
									destinations.put(pirate, destination);
									allocatedMoves.put(pirate, 0);
									Location step = game.getSailOptions(pirate, destination, 1).get(0);
									if (game.getPirateOn(step) != null && game.getPirateOn(step).getOwner() == 0) jumps.add(pirate);
									c = collector.getTurnsToSober() <= 0 ? 5 : 6;
								}
						}
					game.debug("id= "+pirate.getId()+" reFire="+pirate.getReloadTurns()+" sober:"+pirate.getTurnsToSober());
				}

			if (!destinations.isEmpty())
				while (c > 0)
					for (Pirate pirate : destinations.keySet())
					{
						if (c > 0)
							if (jumps.contains(pirate) && allocatedMoves.get(pirate) == 0)
							{
								allocatedMoves.put(pirate, 2);
								c-= 2;
							}
							else
							{
								allocatedMoves.put(pirate, allocatedMoves.get(pirate) + 1);
								c--;
							}
					}

			for (Pirate pirate : destinations.keySet())
			{
				int moves = allocatedMoves.get(pirate);
				Location step = game.getSailOptions(pirate, destinations.get(pirate), moves).get(0);
				if (pirate.hasTreasure())
				{
					moves = 1;
					step = game.getSailOptions(pirate, destinations.get(pirate), moves).get(0);
				}
				else while (steps.containsValue(step) && moves > 0)
				{
					moves--;
					step = game.getSailOptions(pirate, destinations.get(pirate), moves).get(0);
				}
				game.debug("" + moves);
				steps.put(pirate, step);
				 game.setSail(pirate, step);
				c-= moves;
			}
		}
		game.debug("shots: "+shots[0]+shots[1]+shots[2]+shots[3]);
	}

	private Pirate check(PirateGame game, Pirate pirate)
	{
		for (Pirate enemy : game.enemySoberPirates())
			if (game.inRange(pirate, enemy) && shots[enemy.getId()]==0) return enemy;
		return null;
	}

	private Treasure checkTreasure(PirateGame game, Pirate pirate)
	{
		Treasure treasure = null;
		int min = 0;
		for (Treasure i : game.treasures())
		{
			int d = game.distance(pirate, i);;
			if (d < min || min == 0)
			{
				min = d;
				treasure = i;
			}
		}
		return treasure;
	}

	private Pirate checkPirate(PirateGame game, Pirate pirate)
	{
		Pirate target = null;
		int min = 0;
		for (Pirate i : game.enemyPirates())
			if (i.getLocation().equals(i.getInitialLocation()))
			{
				int d = game.distance(pirate, i);;
				if (d < min || min == 0)
				{
					min = d;
					target = i;
				}
			}
		return target;
	}
	
	private boolean minDis(PirateGame game, Pirate pirate)
	{
		for (Pirate i : game.enemyPirates())
			if (game.distance(pirate, i) < 2) return true;
		return false;
	}
	
	private Pirate minDist(PirateGame game, Pirate pirate,int D)
	{
		for (Pirate i : game.enemyPirates())
			if (game.distance(pirate, i) < D) return i;
		return null;
	}

	private Pirate checkCol(PirateGame game, Pirate pirate)
	{
		for (Pirate enemy : game.enemyPiratesWithTreasures())
			if (game.distance(pirate, enemy) < 7) return enemy;
		return null;
	}

	private boolean inBase(PirateGame game, Pirate pirate)
	{
		return pirate.getLocation().row > 20
				&& Math.abs(pirate.getLocation().col - game.allMyPirates().get(0).getInitialLocation().col) < 6;
	}

	private Pirate available(PirateGame game)
	{
		List<Pirate> p = game.myPirates();
		Collections.reverse(p);
		for (Pirate pirate : p)
			if (pirate != game.allMyPirates().get(0) && pirate.getTurnsToSober() <= 0
			&& !blockings.containsKey(pirate) && !removals.containsKey(pirate)) return pirate;
		return null;
	}

	private boolean threatened(PirateGame game, Location step)
	{
		for (Integer i : removals.values())
			if ((game.getEnemyPirate(i).getTurnsToSober() <= 0 && game.getEnemyPirate(i).getReloadTurns() <= 0
			&& game.inRange(game.getEnemyPirate(i).getLocation(), step))
					|| game.getEnemyPirate(i).getLocation().equals(game.allMyPirates().get(0).getInitialLocation())) return true;
		return false;
	}

	private boolean hadTreasure(PirateGame game , Pirate P){
		boolean x;
		if((P.isLost() || P.getTurnsToSober()!=0) && COL2) x = true ;
		else x = false;
		if(P.hasTreasure())
			COL2=true;
		else COL2 = false;
		return x;
	}

	static Location newLocation=null;
	private void One(PirateGame game){
		List<Pirate> enemy = game.allEnemyPirates();
		int c = game.getActionsPerTurn();
		List<Pirate> myPirates = game.myPirates();
		List<Treasure> treasures = game.treasures();
		List<Pirate> enemyPirates = game.enemySoberPirates();
		if(newLocation!=null)
			if(enemyPirates.size()!=0)
				if(game.inRange(myPirates.get(0), enemyPirates.get(0)))
					game.attack(myPirates.get(0), enemyPirates.get(0));		
		if(game.enemyDrunkPirates().size()==0&&!myPirates.get(0).hasTreasure()) {                        
			int addition = myPirates.get(0).getInitialLocation().col==4 ? -1:1;
			Location location = treasures.get(0).getLocation();
			newLocation = new Location(location.row, location.col+addition);
			List<Location> sailing = game.getSailOptions(myPirates.get(0), newLocation, c);
			game.setSail(myPirates.get(0), sailing.get(0));
		}
		else if(myPirates.get(0).hasTreasure()) {
			Location stupid = myPirates.get(0).getInitialLocation();
			List<Location> sailing = game.getSailOptions(myPirates.get(0), stupid , 1);
			if(sailing.size()>1)
				game.setSail(myPirates.get(0), sailing.get(1));    
			else
				game.setSail(myPirates.get(0), sailing.get(0));    
		}            
		else if(game.enemyDrunkPirates().size()>0) {    
			Location location = treasures.get(0).getLocation();            
			List<Location> sailing = game.getSailOptions(myPirates.get(0), location, c);
			game.setSail(myPirates.get(0), sailing.get(0));
		}

	}
	private List<Location> getJihadTarget(PirateGame game, Pirate pirate, Pirate enemy) {
		List<Pirate> myPirates = game.myPirates(), enemyPirates = game.allEnemyPirates();
		int index = pirate.getId();
		Location inital = enemy.getInitialLocation();
		List<Location> possible = new LinkedList<Location>();
		for(int i=-1; i<2; i++)
			for(int j=-1; j<2; j++) {
				Location trying = new Location(inital.row+i, inital.col+j);
				if(j!=0||i!=0) {
					if(trying.row>=0&&trying.col>=0&&trying.row<game.getRows()&&trying.col<game.getCols()) {
						boolean b = true;
						for(int c=0; c<enemyPirates.size()&&c!=index; c++) {
							int row = enemyPirates.get(c).getInitialLocation().row;
							int col = enemyPirates.get(c).getInitialLocation().col;
							if(row==trying.row&&col==trying.col) b = false;
							if(game.isOccupied(trying)&&!isSame(trying, pirate.getLocation()))
								b = false;
						}
						if(b)  
							possible.add(trying);						

					}
				} 
			}
		List<Integer> rangeCount = new LinkedList<Integer>();
		for(int i=0; i<possible.size(); i++) {
			rangeCount.add(new Integer(0));
			for(int c=0; c<enemyPirates.size(); c++)
				if(game.inRange(possible.get(i), game.allEnemyPirates().get(c).getInitialLocation()))
					rangeCount.set(i, rangeCount.get(i)+1);
		}
		for(int i=0; i<possible.size()-1; i++) {
			if(rangeCount.get(i)<rangeCount.get(i+1)) {
				Location tempLoc = possible.get(i);
				int tempCount = rangeCount.get(i);
				possible.set(i, possible.get(i+1));
				rangeCount.set(i, rangeCount.get(i+1));
				possible.set(i+1, tempLoc);
				rangeCount.set(i+1, tempCount);
				i = -1;
			}
		}
		Location current = pirate.getLocation();		
		int currentIndex = possible.indexOf(current);
		if(currentIndex!=-1) {
			int currentCount = rangeCount.get(currentIndex);
			if(currentCount==rangeCount.get(0)&&currentIndex!=0) {
				possible.set(currentIndex, possible.get(0));
				possible.set(0, current);
			}
		}
		return possible;
	}
	private boolean isSame(Location a, Location b) {
		return a.row==b.row&&a.col==b.col;
	}
	private List<Pirate> checkJihad(PirateGame game) {
		List<Pirate> toReturn = new LinkedList<Pirate>();
		List<Pirate> enemy = game.allEnemyPirates();
		for(int i=0; i<enemy.size()-1; i++) {			
			toReturn.add(enemy.get(i));
			if(game.distance(enemy.get(i), enemy.get(i+1))<2)
				i++;			
		}
		List<Integer> rangeCount = new LinkedList<Integer>();
		for(int i=0; i<toReturn.size(); i++) {
			rangeCount.add(new Integer(0));
			for(int c=0; c<enemy.size(); c++)
				if(game.inRange(toReturn.get(i), enemy.get(c).getInitialLocation()))
					rangeCount.set(i, rangeCount.get(i)+1);
		}
		for(int i=0; i<toReturn.size()-1; i++) {
			if(rangeCount.get(i)<rangeCount.get(i+1)) {
				Pirate tempPir = toReturn.get(i);
				int tempCount = rangeCount.get(i);
				toReturn.set(i, toReturn.get(i+1));
				rangeCount.set(i, rangeCount.get(i+1));
				toReturn.set(i+1, tempPir);
				rangeCount.set(i+1, tempCount);
				i = -1;
			}
		}
		return toReturn;
	}
}

