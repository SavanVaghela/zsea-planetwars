//package planet_wars;
import java.util.*;

public class Game {
    public static List <Planet> myPlanets, enemyPlanets, neutralPlanets ;
    public static List <Fleet> myFleets, enemyFleets;
    
    public static double ENEMY_ATTACK_PRIORITY = 1.1;
    public static int    ATTACKER_SHIPS_LAY_UP_ENEMY = 15;
    public static int    ATTACKER_SHIPS_LAY_UP_NEUTRAL = 1;
    public static int    PLANETS_BASELINE = 25;
    public static int    PLANETS_HELP_STOCK = 20;
    public static int    NUMB_OF_SHIPS_PRIORITY = 40;
    public static double    GENERAL_DISTANCE_PRIORITY = 1;
    public static double    EXPONENTIAL_DISTANCE_PRIORITY = 2;
    
    public static void Initialize(PlanetWars pw){
      myPlanets = new ArrayList<Planet>();
      for(Planet p : pw.MyPlanets())
       myPlanets.add(new Planet(p));
      
      enemyPlanets = new ArrayList<Planet>();
      for(Planet p : pw.EnemyPlanets())
        enemyPlanets.add(new Planet(p));
      
      neutralPlanets = new ArrayList<Planet>();
      for(Planet p : pw.NeutralPlanets())
        neutralPlanets.add(new Planet(p));
      
      myFleets = new ArrayList<Fleet>();
      for(Fleet f : pw.MyFleets())
        myFleets.add(new Fleet(f));
      
      enemyFleets = new ArrayList<Fleet>();
      for(Fleet f : pw.EnemyFleets())
        enemyFleets.add(new Fleet(f));
    }
    
    public static void SetPlanet(Planet p) {
      switch(p.Owner()){
       // case 0:  neutralPlanets.p); break;
        case 1:  myPlanets.set(p.PlanetID(), p);      break;
        default: enemyPlanets.set(p.PlanetID(), p);   break;
      }
    }
    
    public static void ChangePlanetOwner(Planet p, int newOwner)
    {
      switch(p.Owner()){
        case 0:  neutralPlanets.remove(p); break;
        case 1:  myPlanets.remove(p);      break;
        default: enemyPlanets.remove(p);   break;
      }
      switch(newOwner){
        case 0:  neutralPlanets.add(p); break;
        case 1:  myPlanets.add(p);      break;
        default: enemyPlanets.add(p);   break;
      }
    }
    
    public static LinkedList<Fleet> GetAttackedFleets(Planet p){
       return GetAttackedFleets(p, (p.Owner() == 1 ? 2:1));
    }
    
    public static LinkedList<Fleet> GetAttackedFleets(Planet p, int fleetOwner){
      LinkedList<Fleet> fleets = new LinkedList<Fleet>();
      List<Fleet> targetFleets = (fleetOwner == 1 ? myFleets:enemyFleets);
      for(Fleet fleet : targetFleets)
       if(fleet.DestinationPlanet() == p.PlanetID())
         fleets.add(fleet);
      return fleets;
   }
    
    public static LinkedList<Fleet> GetReinforcementFleets(Planet p) {
      LinkedList<Fleet> fleets = new LinkedList<Fleet>();
      List<Fleet> targetFleets = (p.Owner() == 1 ? myFleets : enemyFleets);
      for(Fleet fleet : targetFleets)
       if(fleet.DestinationPlanet() == p.PlanetID())
         fleets.add(fleet);
      return fleets;
    }
    public static int GetShipsInFleetsCount(Collection<Fleet> fleets){
      int ships = 0;
      for(Fleet f:fleets){
        ships += f.NumShips();
      }
      return ships;
    }
    public static int GetSummaryPlanetGrowthRate(int playerID) {
       int gr = 0;
       List<Planet> planets = (playerID == 1 ? myPlanets : enemyPlanets);
       for(Planet p:planets){
         gr += p.GrowthRate();
       }
       return gr;
    }
    public static<T> void Sort(Collection<T> collection, Comparator<T> comp){
  //Weak sort O(n^2) on a quick hand :)
      List<T> res = new LinkedList<T>();
      int size = collection.size();
      while(size-- > 0){
          T minElem = null;
          for(T elem : collection){
            if(minElem == null) {minElem = elem; continue;}
            if(comp.compare(elem,minElem) == 1){
              minElem = elem;
            }
          }
          collection.remove(minElem);
          res.add(minElem);
      }
      collection.addAll(res);
    }
   
    
    public static List<Planet> NotMyPlanets(){
      List<Planet> planets = new ArrayList<Planet>(neutralPlanets);
      planets.addAll(enemyPlanets);
      return planets;
    }
    public static List<Planet> Planets() {
      List<Planet> planets = new ArrayList<Planet>(neutralPlanets);
      planets.addAll(enemyPlanets);
      planets.addAll(myPlanets);
      return planets;
    }
    
}
