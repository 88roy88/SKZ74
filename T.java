		List<Pirate> p=game.allMyPirates();
		//if(game.getDefenseExpirationTurns()<=0 && game.getDefenseReloadTurns()<=0)
			
		game.defend(p.get(0));
		game.defend(p.get(1));
		game.defend(p.get(2));
		game.defend(p.get(3));
		
	}
}