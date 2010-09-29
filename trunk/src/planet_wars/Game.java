//package planet_wars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

//import MainBot.MyBot;

public class Game {
  public static List<Planet> myPlanets, enemyPlanets, neutralPlanets,
      allPlanets;
  public static List<Fleet> myFleets, enemyFleets;
  public static PlanetWars pw;

  public static double ENEMY_ATTACK_PRIORITY = 1.1;
  public static int ATTACKER_SHIPS_LAY_UP_ENEMY = 15;
  public static int ATTACKER_SHIPS_LAY_UP_NEUTRAL = 1;
  public static int PLANETS_BASELINE = 10;
  public static int PLANETS_HELP_STOCK = 20;
  public static int NUMB_OF_SHIPS_PRIORITY = 1;
  public static double GENERAL_DISTANCE_PRIORITY = 100;
  public static double EXPONENTIAL_DISTANCE_PRIORITY = 1.5; // 1.5
  public static double EXPONENTIAL_POWER_DISTANCE_PRIORITY = 10; // 10
  public static int DEFENSE_FROM_OCCUPATION = 15;
  public static int DEFENSE_FROM_BASELINE = 5;

  public static void Initialize(PlanetWars _pw) {
    pw = _pw;

    allPlanets = new ArrayList<Planet>();
    for (Planet p : pw.Planets())
      allPlanets.add(new Planet(p));

    myPlanets = new ArrayList<Planet>();
    for (Planet p : allPlanets) {
      if (p.Owner() == 1)
        myPlanets.add(p);
    }

    enemyPlanets = new ArrayList<Planet>();
    for (Planet p : allPlanets) {
      if (p.Owner() > 1)
        enemyPlanets.add(p);
    }

    neutralPlanets = new ArrayList<Planet>();
    for (Planet p : allPlanets) {
      if (p.Owner() == 0)
        neutralPlanets.add(p);
    }

    myFleets = new ArrayList<Fleet>();
    for (Fleet f : pw.MyFleets())
      myFleets.add(new Fleet(f));

    enemyFleets = new ArrayList<Fleet>();
    for (Fleet f : pw.EnemyFleets())
      enemyFleets.add(new Fleet(f));

    List<Fleet> sortedFleets = new ArrayList<Fleet>(myFleets);
    sortedFleets.addAll(enemyFleets);
    Game.Sort(sortedFleets, new FleetTurnsComparator());
    for (Fleet fleet : sortedFleets) {
      Planet p = Game.GetPlanet(fleet.DestinationPlanet());
      p.AddFleet(fleet);
    }
  }

  public static void Rollback() {
    LinkedList<Planet> planetsForRemove = new LinkedList<Planet>();
    for (Planet planet : myPlanets) {
      if (planet.Owner() == 0) {
        neutralPlanets.remove(planet);
      }
      if (planet.Owner() == 2) {
        enemyPlanets.remove(planet);
      }
      if (planet.Owner() != 1) {
        planetsForRemove.add(planet);
      }
    }
    myPlanets.removeAll(planetsForRemove);
    // true rollback
    for (Planet planet : allPlanets) {
      planet.NumShipsModelled(planet.NumShips());
      planet.OwnerModelled(planet.Owner());
    }
  }

  public static Planet GetPlanet(int planetID) {
    return allPlanets.get(planetID);
  }

  // /////////////////////////////////////////////////////////////////////////////////////////////////
  // ////////////////////////////////// Premodelling
  // //////////////////////////////////////////
  // /////////////////////////////////////////////////////////////////////////////////////////////////
  public static void Premodelling() {
    // MyBot.dBg.Writeln("\r\n:: Turn " + MyBot.Turn + ":");
    List<Fleet> fleets = Fleets();
    int turnsModeled = 0;
    Game.Sort(fleets, new FleetTurnsComparator());
    for (Fleet fleet : fleets) {
      // MyBot.dBg.Writeln("fleet remain::" + fleet.TurnsRemaining());
      int dstPlanetID = fleet.DestinationPlanet();
      Planet dstPlanet = GetPlanet(dstPlanetID);
      // Planet growing
      if (dstPlanet.Owner() != 0) {
        dstPlanet.AddShips(dstPlanet.GrowthRate()
            * (fleet.TurnsRemaining() - turnsModeled));
        turnsModeled = fleet.TurnsRemaining();
      }
      // Fleets arrived
      if (dstPlanet.Owner() == fleet.Owner()) {
        dstPlanet.AddShips(fleet.NumShips());
      }
      else {
        dstPlanet.RemoveShips(fleet.NumShips());
      }

      if (dstPlanet.NumShips() < 0) {

        dstPlanet.Owner(fleet.Owner());
        dstPlanet.NumShips(-dstPlanet.NumShips());
      }
    }
    // Owner change
    for (Planet planet : myPlanets) {
      if (planet.Owner() == 2) {
        planet.NumShips(-planet.NumShips());
        planet.Owner(1);
      }
    }
    // Some cleaning for planets
    List<Planet> planetsForDelete = new LinkedList<Planet>();
    for (Planet planet : Game.neutralPlanets) {
      if (planet.Owner() == 1) {
        planetsForDelete.add(planet);
      }
      if (planet.Owner() == 2)
        Game.ChangePlanetOwner(planet, 2);
    }
    Game.neutralPlanets.removeAll(planetsForDelete);
    planetsForDelete.clear();
    for (Planet planet : Game.enemyPlanets) {
      if (planet.Owner() == 1)
        planetsForDelete.add(planet);
    }
    Game.enemyPlanets.removeAll(planetsForDelete);
    for (Planet p : myPlanets) {
      p.NumShips(Math.min(pw.GetPlanet(p.PlanetID()).NumShips(), p.NumShips()));
    }
  }

  public static void IssueOrders() {
    LinkedList<Order> orders = new LinkedList<Order>();
    orders.addAll(MyBot.defenseOrders);
    orders.addAll(MyBot.attackOrders);
    for (Order order : orders) {
      Planet src_real = pw.GetPlanet(order.src.PlanetID());
      Planet dst_real = pw.GetPlanet(order.dst.PlanetID());
      pw.IssueOrder(order.src, order.dst, order.numShips);
    }
  }

  public static void ChangePlanetOwner(Planet p, int newOwner) {
    switch (p.Owner()) {
      case 0:
        neutralPlanets.remove(p);
        break;
      case 1:
        myPlanets.remove(p);
        break;
      default:
        enemyPlanets.remove(p);
        break;
    }
    switch (newOwner) {
      case 0:
        neutralPlanets.add(p);
        break;
      case 1:
        myPlanets.add(p);
        break;
      default:
        enemyPlanets.add(p);
        break;
    }
  }

  public static LinkedList<Fleet> GetAttackedFleets(Planet p) {
    return GetAttackedFleets(p, (p.Owner() == 1 ? 2 : 1));
  }

  public static LinkedList<Fleet> GetAttackedFleets(Planet p, int fleetOwner) {
    LinkedList<Fleet> fleets = new LinkedList<Fleet>();
    List<Fleet> targetFleets = (fleetOwner == 1 ? myFleets : enemyFleets);
    for (Fleet fleet : targetFleets)
      if (fleet.DestinationPlanet() == p.PlanetID())
        fleets.add(fleet);
    return fleets;
  }

  public static LinkedList<Fleet> GetReinforcementFleets(Planet p) {
    LinkedList<Fleet> fleets = new LinkedList<Fleet>();
    List<Fleet> targetFleets = (p.Owner() == 1 ? myFleets : enemyFleets);
    for (Fleet fleet : targetFleets)
      if (fleet.DestinationPlanet() == p.PlanetID())
        fleets.add(fleet);
    return fleets;
  }

  public static int GetShipsInFleetsCount(Collection<Fleet> fleets) {
    int ships = 0;
    for (Fleet f : fleets) {
      ships += f.NumShips();
    }
    return ships;
  }

  public static int GetSummaryPlanetGrowthRate(int playerID) {
    int gr = 0;
    List<Planet> planets = (playerID == 1 ? myPlanets : enemyPlanets);
    for (Planet p : planets) {
      gr += p.GrowthRate();
    }
    return gr;
  }

  public static <T> void Sort(Collection<T> collection, Comparator<T> comp) {
    // Weak sort O(n^2) on a quick hand :)
    List<T> res = new LinkedList<T>();
    int size = collection.size();
    while (size-- > 0) {
      T minElem = null;
      for (T elem : collection) {
        if (minElem == null) {
          minElem = elem;
          continue;
        }
        if (comp.compare(elem, minElem) == 1) {
          minElem = elem;
        }
      }
      collection.remove(minElem);
      res.add(minElem);
    }
    collection.addAll(res);
  }

  public static List<Planet> NotMyPlanets() {
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

  public static List<Fleet> Fleets() {
    List<Fleet> fleets = new ArrayList<Fleet>(myFleets);
    fleets.addAll(enemyFleets);
    return fleets;
  }

  public static int Distance(Planet source, Planet destination) {
    double dx = source.X() - destination.X();
    double dy = source.Y() - destination.Y();
    return (int) Math.ceil(Math.sqrt(dx * dx + dy * dy));
  }

  public static int Distance(int sourcePlanet, int destinationPlanet) {
    Planet source = allPlanets.get(sourcePlanet);
    Planet destination = allPlanets.get(destinationPlanet);
    double dx = source.X() - destination.X();
    double dy = source.Y() - destination.Y();
    return (int) Math.ceil(Math.sqrt(dx * dx + dy * dy));
  }

  public static int NumShips(int playerID) {
    int numShips = 0;
    for (Planet p : allPlanets) {
      if (p.Owner() == playerID) {
        numShips += p.NumShips();
      }
    }
    for (Fleet f : Fleets()) {
      if (f.Owner() == playerID) {
        numShips += f.NumShips();
      }
    }
    return numShips;
  }
}
