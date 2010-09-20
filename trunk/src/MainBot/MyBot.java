//package MainBot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;
//import planet_wars.*;

///////////////////////////////////////////////////////////////////////////////////////////////
//MyBot 
///////////////////////////////////////////////////////////////////////////////////////////////
public class MyBot {
  public static int Turn = 0;

  // public static Debugger dBg = new Debugger("loooooooooog.txt");

  public static List<Order> defenseOrders = new LinkedList<Order>();
  public static List<Order> attackOrders = new LinkedList<Order>();
  private static List<Score> defenseScores = new LinkedList<Score>();
  private static List<Planet> destinationPlanets = null;

  public static void DoTurn(PlanetWars pw) {
    // Turn++;
    // dBg.Writeln(":: Init Start ::");

    Game.Initialize(pw);
    // dBg.Writeln(":: Init End ::");

    Game.Premodelling();
    // dBg.Writeln(":: Premodeling end::");
    PrepareContainers();
    // dBg.Writeln(":: PrepareContainers end::");
    CalculateScores();
    // dBg.Writeln(":: CAlc scores end::");
    CalculateDefense();
    // dBg.Writeln(":: Cals def end::");
    CalculateAttack();
    // dBg.Writeln(":: Cals attack end::");
    Game.IssueOrders();
    // dBg.Writeln(":: Issue order end::");
  }

  private static void PrepareContainers() {
    destinationPlanets = Game.NotMyPlanets();
    defenseOrders.clear();
    attackOrders.clear();
    defenseScores.clear();
  }
///////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////  CalculateScores  ////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
  @SuppressWarnings("unchecked")
  private static void CalculateScores() {
    // Destination planets (aggregate scores)
    for (Planet planet : destinationPlanets) {
      // dBg.Writeln("::calculateAGGRScore start::");
      planet.Score(CalculateAggregateScore(planet));
      // dBg.Writeln("::calculateAGGRScore end::");
    }

    // Defense scores
    for (Planet planet : Game.myPlanets) {
      int defensePriority = -1;
      if (planet.NumShips() < 0) {
        defensePriority = Game.DEFENSE_FROM_OCCUPATION;
      }
      else if (planet.NumShips() < planet.Baseline()) {
        defensePriority = Game.DEFENSE_FROM_BASELINE;
      }
      if (defensePriority != -1) {
        Score score = new Score(null, planet, 0);
        final double GR_WEIGHT = 5.0;
        score.value = CalculateDistanceScore(planet) + GR_WEIGHT
            * planet.GrowthRate() - (planet.NumShips() + planet.Baseline())
            / 10.0 + defensePriority;
        defenseScores.add(score);
      }
    }

    // dBg.Writeln("::Sort start::");
    Game.Sort(defenseScores, new ScoreComparator());
    Game.Sort(destinationPlanets, new AggregateScoreComparator());
    // dBg.Writeln("::Sort end::");
    // dBg.Writeln("::CalculateScores finished::");
  }
///////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////  CalculateDefence  ////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
  private static void CalculateDefense() {
    for (Planet planet : Game.myPlanets) {
      List<Order> orders = GetDefenceOrdersFor(planet);
      if (orders != null)
        defenseOrders.addAll(orders);
    }
  }
  private static List<Order> GetDefenceOrdersFor(Planet planet) {
    List<Order> orders = new LinkedList<Order>();
    /*for (Score score : defenseScores) {
      // get source planet
      LinkedList<Score> sourceScores = GetSourceScores(score.dst);
      score.src = sourceScores.getFirst().src;
      // calculate number of ships
      int numShips = 0;
      if (score.dst.NumShips() < 0) {
        numShips = 1 - score.dst.NumShips();
      }
      else {
        numShips = Game.PLANETS_BASELINE - score.dst.NumShips();
      }
      if ((score.src.NumShips() - score.src.Baseline() - numShips) > 0) {
        Order order = new Order(score.src, score.dst, numShips);
        orders.add(order);
      }
    }*/
    return (orders.size() != 0) ? orders : null; 
  }
///////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////  CalculateAttack  ////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
  private static void CalculateAttack() {
    for (Planet planet : destinationPlanets) {
      List<Order> orders = GetAttackOrdersFor(planet);
      if (orders != null)
        attackOrders.addAll(orders);
      else
        break;
    }
  }

  private static List<Order> GetAttackOrdersFor(Planet targetPlanet) {
    List<Order> orders = new ArrayList<Order>();
    // dBg.Writeln("::Getting oreder::");
    int growthTurnsAnalized = 0;
    int shipsOnTargetPlanet = targetPlanet.NumShips();
    shipsOnTargetPlanet += (targetPlanet.Owner() == 0 ? Game.ATTACKER_SHIPS_LAY_UP_NEUTRAL
                                                      : Game.ATTACKER_SHIPS_LAY_UP_ENEMY);

    List<Score> sourceScores = GetSourceScores(targetPlanet);
    for (Score s : sourceScores) {

      int possibleShips2Attack = (int) (s.src.NumShips() - Game.PLANETS_BASELINE);
      if (possibleShips2Attack <= 0)
        continue;

      int ships2Attack = 0;
      int additionalShipsForGR = 0;
      // dBg.Writeln(" ::Start calc gr :poss ships: " + possibleShips2Attack);
      int actualDistance = Game.Distance(s.src, s.dst);
      // dBg.Writeln(" ::Distance to planet: " + actualDistance +
      // " :: planet ID: " + s.dst.PlanetID() +
      // " ::planet GR: " + s.dst.GrowthRate() + " ::planet numShips:" +
      // s.dst.NumShips());
      if (targetPlanet.Owner() > 1 && actualDistance > growthTurnsAnalized) {
        int growthRate = targetPlanet.GrowthRate();
        int turnsGrowsRateConsidered = (int) (possibleShips2Attack / (growthRate + 1));
        actualDistance -= growthTurnsAnalized;
        if (turnsGrowsRateConsidered >= actualDistance) {
          growthTurnsAnalized += actualDistance;
          additionalShipsForGR = actualDistance * growthRate;
          possibleShips2Attack -= additionalShipsForGR;
          // dBg.Writeln("::Calculate gr::" + growthRate +
          // "::poss ships after calc act dista : "
          // + possibleShips2Attack);
        }
        else
          continue;
      }
      if (possibleShips2Attack <= 0)
        continue;

      ships2Attack = (shipsOnTargetPlanet <= possibleShips2Attack ? shipsOnTargetPlanet + 1
          : possibleShips2Attack);
      // dBg.Writeln(" ::Calculate ships2Attack::" + ships2Attack);
      shipsOnTargetPlanet -= ships2Attack;
      // dBg.Writeln(" ::Calculate ships on target planet::" +
      // shipsOnTargetPlanet);
      orders.add(new Order(s.src, s.dst, ships2Attack + additionalShipsForGR));
      if (shipsOnTargetPlanet <= 0)
        break;
    }
    if (shipsOnTargetPlanet > 0)
      return null;
    for (Order o : orders) {

      o.src.RemoveShips(o.numShips);

    }
    // dBg.Writeln("::oreder complete::");
    return orders;
  }
///////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////  GetSourceScores  ////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
  @SuppressWarnings("unchecked")
  private static LinkedList<Score> GetSourceScores(Planet dstPlanet) {
    LinkedList<Score> sourceScores = new LinkedList<Score>();
    List<Planet> filteredMyPlanets = new LinkedList<Planet>(Game.myPlanets);
    filteredMyPlanets.remove(dstPlanet);

    for (Planet srcPlanet : filteredMyPlanets) {
      Score score = new Score(srcPlanet, dstPlanet, 0);
      // dBg.Writeln("::CalculateSourceScore start::");
      score.value = CalculateSourceScore(srcPlanet, dstPlanet);
      // dBg.Writeln("::CalculateSourceScore end::");
      sourceScores.add(score);
    }
    Game.Sort(sourceScores, new ScoreComparator());
    return sourceScores;
  }

  private static double CalculateSourceScore(Planet src, Planet dst) {
    final double GR_WEIGHT = 5.0;
    return (double) (src.NumShips() - src.Baseline())
        / Math.pow((double) Game.Distance(src.PlanetID(), dst.PlanetID()), 2)
        + GR_WEIGHT * src.GrowthRate();
  }

  private static double CalculateAggregateScore(Planet targetP) {
    double distanceScore = targetP.ATTACK_PRIORITY
        * (Game.GENERAL_DISTANCE_PRIORITY * CalculateDistanceScore(targetP) + Game.NUMB_OF_SHIPS_PRIORITY
            * targetP.GrowthRate() / (targetP.NumShips() + 1));

    return distanceScore;
  }
///////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////  CalculateDistanceScore  /////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
  private static double CalculateDistanceScore(Planet currPlanet) {
    double myD = 0.001, enemyD = 0.001;
    for (Planet ourPlanet : Game.myPlanets)
      myD += Math.pow(Game.Distance(currPlanet, ourPlanet),
          Game.EXPONENTIAL_DISTANCE_PRIORITY);

    for (Planet enemyPlanet : Game.enemyPlanets)
      enemyD += Math.pow(
          Game.Distance(currPlanet.PlanetID(), enemyPlanet.PlanetID()),
          Game.EXPONENTIAL_DISTANCE_PRIORITY);

    double gameRatio = (Game.myPlanets.size() + 1)
        / (Game.enemyPlanets.size() + 1);
    gameRatio *= Game.NumShips(1) / (Game.NumShips(2) + 1);
    gameRatio *= (Game.GetSummaryPlanetGrowthRate(1) + 1)
        / (Game.GetSummaryPlanetGrowthRate(2) + 1);
    double myScore = myD / (Game.myPlanets.size() + 1);
    double enemyScore = 0;
    if (Game.enemyPlanets.size() == 1 && currPlanet.Owner() == 2)
      enemyScore = myScore * gameRatio;
    else
      enemyScore = enemyD / (Game.enemyPlanets.size() + 1);
    return enemyScore / myScore;
  }

  // //////////////////////////////////////////////////////////////////////////////////////////////////
  // // MAIN ////////////////////
  // //////////////////////////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) {
    String line = "";
    String message = "";
    // // start debug
    /*
     * PlanetWars _pw = new PlanetWars(""); int res =
     * _pw.LoadMapFromFile("D:\\MyProjects\\Eclipse\\PlanetWars\\maps\\map3.txt"
     * ); DoTurn(_pw);
     */
    // return;
    // // finish debug
    int c;
    try {
      while ((c = System.in.read()) >= 0) {
        switch (c) {
          case '\n':
            if (line.equals("go")) {
              PlanetWars pw = new PlanetWars(message);
              DoTurn(pw);
              pw.FinishTurn();
              message = "";
            }
            else {
              message += line + "\n";
            }
            line = "";
            break;
          default:
            line += (char) c;
            break;
        }
      }
    }
    catch (Exception e) {
      // dBg.Writeln("main Exeption");
      // dBg.Close();
    }
    // dBg.Close();
  }

}

// /////////////////////////////////////////////////////////////////////////////////////////////
// Score
// /////////////////////////////////////////////////////////////////////////////////////////////
class Score {
  public boolean equals(Object obj) {
    if (value == ((Score) obj).value)
      return true;
    return false;
  }

  public Score(Planet _src, Planet _dst, int _value) {
    src = _src;
    dst = _dst;
    value = _value;
  }

  public Planet src;
  public Planet dst;
  public double value;
}

// /////////////////////////////////////////////////////////////////////////////////////////////
// FleetDistanseComporator
// /////////////////////////////////////////////////////////////////////////////////////////////
@SuppressWarnings("rawtypes")
class FleetTurnsComporator implements Comparator<Fleet> {

  public int compare(Fleet fleet1, Fleet fleet2) {
    int dist1 = ((Fleet) fleet1).turnsRemaining;
    int dist2 = ((Fleet) fleet2).turnsRemaining;
    if (dist1 > dist2)
      return -1;
    if (dist1 < dist2)
      return 1;
    return 0;
  }
}

// /////////////////////////////////////////////////////////////////////////////////////////////
// ScoreComparator
// /////////////////////////////////////////////////////////////////////////////////////////////
@SuppressWarnings("rawtypes")
class ScoreComparator implements Comparator {
  public int compare(Object score1, Object score2) {
    double value1 = ((Score) score1).value;
    double value2 = ((Score) score2).value;
    if (value1 > value2)
      return 1;
    else if (value1 < value2)
      return -1;
    else
      return 0;
  }
}

// /////////////////////////////////////////////////////////////////////////////////////////////
// AggregateScoreComparator
// /////////////////////////////////////////////////////////////////////////////////////////////
@SuppressWarnings("rawtypes")
class AggregateScoreComparator implements Comparator {
  public int compare(Object score1, Object score2) {

    double value1 = ((Planet) score1).score;
    double value2 = ((Planet) score2).score;
    if (value1 > value2)
      return 1;
    else if (value1 < value2)
      return -1;
    else
      return 0;
  }
}

// /////////////////////////////////////////////////////////////////////////////////////////////
// Order
// /////////////////////////////////////////////////////////////////////////////////////////////
class Order {
  public Order(Planet _src, Planet _dst, int _numShips) {
    src = _src;
    dst = _dst;
    numShips = _numShips;
  }

  public Planet src;
  public Planet dst;
  public int numShips;
}

class Debugger {
  private BufferedWriter bw;

  public Debugger(String logFileName) {
    try {
      bw = new BufferedWriter(new FileWriter(logFileName, false));
    }
    catch (Exception e) {
    }
  }

  public void Write(String str) {
    try {
      bw.write(str);
      bw.flush();
    }
    catch (Exception e) {
    }
  }

  public void Writeln(String str) {
    try {
      bw.write(str + "\r\n");
      bw.flush();
    }
    catch (Exception e) {
    }
  }

  public void Close() {
    try {
      bw.close();
    }
    catch (Exception e) {
    }
  }
}
