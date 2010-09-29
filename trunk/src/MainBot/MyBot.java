//package MainBot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.atomic.*;
///////////////////////////////////////////////////////////////////////////////////////////////
//MyBot 
///////////////////////////////////////////////////////////////////////////////////////////////

public class MyBot {
  public static int Turn = 0;

  public static Debugger dBg = new Debugger("loooooooooog.txt");

  public static List<Order> defenseOrders = new LinkedList<Order>();
  public static List<Order> attackOrders = new LinkedList<Order>();
  private static List<Score> defenseScores = new LinkedList<Score>();
  private static List<Planet> destinationPlanets = null;

  public static void DoTurn(PlanetWars pw) {
    Turn++;
    // dBg.Writeln(":: Init Start ::");

    Game.Initialize(pw);
    // dBg.Writeln(":: Init End ::");

    // Game.Premodelling();
    // dBg.Writeln(":: Premodeling end::");
    PrepareContainers();
    // dBg.Writeln(":: PrepareContainers end::");
    CalculateScores();
    // dBg.Writeln(":: CAlc scores end::");
    CalculateDefense();
    // dBg.Writeln(":: Cals def end::");
    Game.Rollback();
    // dBg.Writeln(":: Rollback end::");
    if (Turn < 10)
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

  // /////////////////////////////////////////////////////////////////////////////////////////////////
  // ////////////////////////////////// CalculateScores
  // ////////////////////////////////////////
  // /////////////////////////////////////////////////////////////////////////////////////////////////
  // @SuppressWarnings("unchecked")
  private static void CalculateScores() {
    // Destination planets (aggregate scores)
    for (Planet planet : destinationPlanets) {
      // dBg.Writeln("::calculateAGGRScore start::");
      planet.Score(CalculateAggregateScore(planet));
      // dBg.Writeln("::calculateAGGRScore end::");
    }

    // Defense scores
    /*
     * for (Planet planet : Game.myPlanets) { int defensePriority = -1; if
     * (planet.NumShips() < 0) { defensePriority = Game.DEFENSE_FROM_OCCUPATION;
     * } else if (planet.NumShips() < planet.Baseline()) { defensePriority =
     * Game.DEFENSE_FROM_BASELINE; } if (defensePriority != -1) { Score score =
     * new Score(null, planet, 0); final double GR_WEIGHT = 5.0; score.value =
     * CalculateDistanceScore(planet) + GR_WEIGHT planet.GrowthRate() -
     * (planet.NumShips() + planet.Baseline()) / 10.0 + defensePriority;
     * defenseScores.add(score); } }
     */

    // dBg.Writeln("::Sort start::");
    // Game.Sort(defenseScores, new ScoreComparator());
    Game.Sort(destinationPlanets, new AggregateScoreComparator());
    // dBg.Writeln("::Sort end::");
    // dBg.Writeln("::CalculateScores finished::");
  }

  // /////////////////////////////////////////////////////////////////////////////////////////////////
  // ////////////////////////////////// CalculateDefence
  // ////////////////////////////////////////
  // /////////////////////////////////////////////////////////////////////////////////////////////////

  // /////////////////////////////////////////////////////////////////////////////////////////////////
  // ////////////////////////////////// CalculateAttack
  // ////////////////////////////////////////
  // /////////////////////////////////////////////////////////////////////////////////////////////////
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
    // ArrayList<Order> = new ArrayList<int>();

    // dBg.Writeln("::Getting oreder::");
    int growthTurnsAnalized = 0;
    int shipsOnTargetPlanet = targetPlanet.NumShips();
    shipsOnTargetPlanet += (targetPlanet.Owner() == 0 ? Game.ATTACKER_SHIPS_LAY_UP_NEUTRAL
        : Game.ATTACKER_SHIPS_LAY_UP_ENEMY);

    List<Score> sourceScores = CalculateSourceScores(targetPlanet);
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

  // /////////////////////////////////////////////////////////////////////////////////////////////////
  // ////////////////////////////////// GetSourceScores
  // ////////////////////////////////////////
  // /////////////////////////////////////////////////////////////////////////////////////////////////
  @SuppressWarnings("unchecked")
  private static LinkedList<Score> CalculateSourceScores(Planet dstPlanet) {
    LinkedList<Score> sourceScores = new LinkedList<Score>();
    List<Planet> filteredMyPlanets = new LinkedList<Planet>(Game.myPlanets);
    filteredMyPlanets.remove(dstPlanet);

    for (Planet srcPlanet : filteredMyPlanets) {
      if (srcPlanet.Owner() != 1)
        continue;
      Score score = new Score(srcPlanet, dstPlanet, 0);
      final double GR_WEIGHT = 5.0;
      score.value = (double) 1.0
          / Game.Distance(srcPlanet.PlanetID(), dstPlanet.PlanetID());// (srcPlanet.NumShips()
                                                                      // -
                                                                      // Game.PLANETS_BASELINE)
      // / Math.pow((double) Game.Distance(srcPlanet.PlanetID(),
      // dstPlanet.PlanetID()), 2) + GR_WEIGHT * srcPlanet.GrowthRate();
      sourceScores.add(score);
    }
    Game.Sort(sourceScores, new ScoreComparator());
    return sourceScores;
  }

  private static double CalculateAggregateScore(Planet targetP) {

    double distScore = Game.GENERAL_DISTANCE_PRIORITY
        * CalculateDistanceScore(targetP);
    double shipScore = (double) Game.NUMB_OF_SHIPS_PRIORITY
        / (double) (Math.pow(targetP.NumShips() + 1, 0.60));
    double grScore = Math.pow(targetP.GrowthRate(), 0.5);
    // dBg.Writeln("::dist score:"+ distScore + " :: ship score: " + shipScore +
    // " :: Growth rate: " + grScore);
    return distScore * (shipScore) * grScore;
  }

  // /////////////////////////////////////////////////////////////////////////////////////////////////
  // ////////////////////////////////// CalculateDistanceScore
  // /////////////////////////////////
  // /////////////////////////////////////////////////////////////////////////////////////////////////
  private static double CalculateDistanceScore(Planet currPlanet) {
    double myD = 0.001, enemyD = 0.001;
    for (Planet ourPlanet : Game.myPlanets)
      myD += Game.Distance(currPlanet, ourPlanet);// Math.pow(Game.Distance(currPlanet,
                                                  // ourPlanet),
    myD /= (Game.myPlanets.size() + 1);
    double myScore = Math.exp(Math.pow(myD, Game.EXPONENTIAL_DISTANCE_PRIORITY)
        / Game.EXPONENTIAL_POWER_DISTANCE_PRIORITY);

    /*
     * for (Planet enemyPlanet : Game.enemyPlanets) enemyD += Math.pow(
     * Game.Distance(currPlanet.PlanetID(), enemyPlanet.PlanetID()),
     * Game.EXPONENTIAL_DISTANCE_PRIORITY);
     */
    double gameRatio = (Game.myPlanets.size() + 1)
        / (Game.enemyPlanets.size() + 1);
    gameRatio *= Game.NumShips(1) / (Game.NumShips(2) + 1);
    gameRatio *= (Game.GetSummaryPlanetGrowthRate(1) + 1)
        / (Game.GetSummaryPlanetGrowthRate(2) + 1);
    // / (Game.myPlanets.size() + 1);
    // double enemyScore = 0;
    // if (Game.enemyPlanets.size() == 1 && currPlanet.Owner() == 2)
    // enemyScore = myScore * gameRatio;
    // else
    // enemyScore = enemyD / (Game.enemyPlanets.size() + 1);
    return 1 / Math.pow(myScore, 1);
  }

  public static void InevitabilityModelling() {
    int turnsModelled = 0;
    for (Planet notMyPlanet : Game.NotMyPlanets()) {
      int min = Integer.MAX_VALUE;
      for (Planet myPlanet : Game.myPlanets) {
        int distance = Game.Distance(notMyPlanet, myPlanet);
        if (distance < min)
          min = distance;
      }
      for (Fleet fleet : notMyPlanet.sortedFleets) {
        if (fleet.TurnsRemaining() >= min)
          break;
        if (notMyPlanet.Owner() != 0)
          notMyPlanet.AddShipsModelled(notMyPlanet.GrowthRate()
              * (fleet.TurnsRemaining() - turnsModelled));
        turnsModelled = fleet.TurnsRemaining();
        if (notMyPlanet.OwnerModelled() != fleet.Owner())
          notMyPlanet.RemoveShipsModelled(fleet.NumShips());
        else
          notMyPlanet.AddShipsModelled(fleet.NumShips());
        if (notMyPlanet.NumShipsModelled() < 0) {
          notMyPlanet.OwnerModelled(fleet.Owner());
          notMyPlanet.NumShipsModelled(-notMyPlanet.NumShipsModelled());
        }
      }
    }
  }

  public static void CalculateDefense() {
    // defense modelling
    // dBg.Writeln(":1:");

    for (Planet planet : Game.allPlanets) {
      int turnsModelled = 0;
      // dBg.Writeln(" BEFORE::planet real num ships: " + planet.NumShips()
      // + " planet model num ships: " + planet.NumShipsModelled()
      // + " planet real owner: " + planet.Owner() + " planet model owner: "
      // + planet.OwnerModelled() + " GR = " + planet.GrowthRate());
      for (Fleet fleet : planet.sortedFleets) {
        // dBg.Writeln(" fleet numShips: " + fleet.NumShips()
        // + " fleet turns rem: " + fleet.TurnsRemaining() + " fleet owner: "
        // + fleet.Owner());
        if (planet.OwnerModelled() != 0)
          planet.AddShipsModelled(planet.GrowthRate()
              * (fleet.TurnsRemaining() - turnsModelled));
        turnsModelled = fleet.TurnsRemaining();
        if (planet.OwnerModelled() != fleet.Owner())
          planet.RemoveShipsModelled(fleet.NumShips());
        else
          planet.AddShipsModelled(fleet.NumShips());
        if (planet.NumShipsModelled() < 0) {
          planet.OwnerModelled(fleet.Owner());
          if (planet.OwnerModelled() == 1 && !Game.myPlanets.contains(planet))
            Game.myPlanets.add(planet);
          planet.NumShipsModelled(-planet.NumShipsModelled());
        }
        else if (planet.Owner() == 1)
          planet
              .NumShips(Math.min(planet.NumShips(), planet.NumShipsModelled()));
      }
      // dBg.Writeln(" AFTER::planet real num ships: " + planet.NumShips()
      // + " planet model num ships: " + planet.NumShipsModelled()
      // + " planet real owner: " + planet.Owner() + " planet model owner: "
      // + planet.OwnerModelled());
    }
    // dBg.Writeln(":2:");
    // + defense calculating
    List<Score> defenseScores = new LinkedList<Score>();
    for (Planet planet : Game.myPlanets) {
      if (planet.OwnerModelled() != 1) {
        Score score = new Score(null, planet, 0);

        score.value = CalculateDefenseScore(planet);
        defenseScores.add(score);
      }
    }

    Game.Sort(defenseScores, new ScoreComparator());
    LinkedList<Order> orders = new LinkedList<Order>();
    for (Score dstScore : defenseScores) {
      dBg.Writeln(" ::::::::::::NEW DST PLANET::::::::::::");
      // dBg.Writeln("DESTINATION PLANET::planet real num ships: " +
      // dstScore.dst.NumShips() + "planet model num ships: " +
      // dstScore.dst.NumShipsModelled() + "planet real owner: " +
      // dstScore.dst.Owner() +
      // "planet model owner: " + dstScore.dst.OwnerModelled());
      LinkedList<Score> sourceScores = new LinkedList<Score>();
      sourceScores = CalculateSourceScores(dstScore.dst);
      int shipsToDefense = -1;
      AtomicReference<Boolean> success = new AtomicReference<Boolean>(false);// =
      // false;
      for (Score srcScore : sourceScores) {
        // dBg.Writeln("ORDER BEFORE FUNC CALL " + orders.size());
        Planet src = srcScore.src;
        // dBg.Writeln("SOURCE PLANET::planet real num ships: " + src.NumShips()
        // + "planet model num ships: " + src.NumShipsModelled() +
        // "planet real owner: " + src.Owner() +
        // "planet model owner: " + src.OwnerModelled());
        if (src.OwnerModelled() != 1)
          continue;

        shipsToDefense = CalculateShipsToDefense(src, dstScore.dst, success);
        // dBg.Writeln("ORDER AFTER FUNC CALL " + orders.size());
        // dBg.Writeln(" boolean success: " + success.toString());
        // dBg.Writeln("shipsToDefense: " + shipsToDefense);
        // if (0 == shipsToDefense)

        int distance = Game.Distance(src, dstScore.dst);
        if (shipsToDefense != -1) {
          // dBg.Writeln("ADDING FLEET:: before num fleets : "
          // + dstScore.dst.sortedFleets.size());
          dstScore.dst.AddFleet(new Fleet(1, shipsToDefense, src.PlanetID(),
              dstScore.dst.PlanetID(), distance, distance));
          // dBg.Writeln("FINISH ADDING FLEET:: after num fleets : "
          // + dstScore.dst.sortedFleets.size());
          // dBg.Writeln("ADD ORDER::");
          // dBg.Writeln("ADDING ORDER:: before num order : " + orders.size());
          orders.add(new Order(src, dstScore.dst, shipsToDefense));
          // dBg.Writeln("FINISH ADDING ORDER:: after num order : "
          // + orders.size());
        }
        // dBg.Writeln("ORDER AFTER IF STATEMENT" + orders.size());
        if (success.get())
          break;
        // dBg.Writeln("ORDER AFTER SUCCES.GET CHECKING" + orders.size());
      }
      // if (0 == shipsToDefense) {
      if (success.get()) {
        // dBg.Writeln("ENTERING ORDER::");

        for (Order o : orders) {
          // dBg.Writeln("ORDER::planet ships: " + o.src.NumShips()
          // + " :: order ships: " + o.numShips);
          o.src.RemoveShips(o.numShips);
        }
        defenseOrders.addAll(orders);
        orders.clear();
      }
    }
  }

  public static double CalculateDefenseScore(Planet dstPlanet) {
    return CalculateAggregateScore(dstPlanet);
  }

  public static int CalculateShipsToDefense(Planet src, Planet dst,
      AtomicReference<Boolean> success) {
    int possibleShips = (src.NumShips() - Game.PLANETS_BASELINE);
    // dBg.Writeln("poss ships " + possibleShips);
    if (possibleShips <= 1)
      return -1;
    // dBg.Writeln("poss ships after checking" + possibleShips);
    int maxPossibleShips = possibleShips;
    possibleShips /= 2;
    int step = possibleShips;
    int lastSuccessfulModeling = -1;
    int minFinalNumShips = Integer.MAX_VALUE;
    do {
      step = step / 2;
      int finalNumShips = CalculateModelledFinalNumShips(possibleShips, src,
          dst);
      // dBg.Writeln("finalNumShips " + finalNumShips + " :: " + possibleShips);
      if (finalNumShips >= 1) {
        if (finalNumShips < minFinalNumShips) {
          minFinalNumShips = finalNumShips;
          lastSuccessfulModeling = possibleShips;
        }
      }
      possibleShips += (finalNumShips < 1 ? step : -step);
      if (possibleShips == 0)
        break;

      /*
       * if (possibleShips >= maxPossibleShips) { // if not enough to defense
       * from this planet possibleShips = maxPossibleShips; break; }
       */
      // AtomicReference
    }
    while (step > 0);
    // dBg.Writeln("lastSuccessfulModeling " + lastSuccessfulModeling);
    // dBg.Writeln("possibleShips " + possibleShips);
    if (lastSuccessfulModeling != -1) {
      success.set(true);
      return lastSuccessfulModeling;
    }
    else {
      // dBg.Writeln("returning maxPossibleShips " + maxPossibleShips);
      return maxPossibleShips;
    }
  }

  /*
   * public static int CalculateShipsToDefense(Planet src, Planet dst) { int
   * possibleShips = (src.NumShips() - Game.PLANETS_BASELINE);
   * dBg.Writeln("poss ships " + possibleShips); if (possibleShips <= 1) return
   * -1; // dBg.Writeln("poss ships after checking" + possibleShips); int
   * maxPossibleShips = possibleShips; possibleShips /= 2; int step =
   * (possibleShips / 2); int finalNumShips =
   * CalculateModelledFinalNumShips(possibleShips, src, dst);
   * dBg.Writeln("first finalNumShips " + finalNumShips + " :: " +
   * possibleShips); int lastSuccessfulModeling = -1; while (step > 0) {
   * possibleShips += (finalNumShips < 0 ? step : -step); if (possibleShips ==
   * 0) break; step /= 2 + step % 2; // step = (step == 0 ? 1 : step);
   * finalNumShips = CalculateModelledFinalNumShips(possibleShips, src, dst);
   * 
   * if (finalNumShips >= 0) lastSuccessfulModeling = possibleShips; else if
   * (lastSuccessfulModeling != -1) possibleShips = lastSuccessfulModeling;
   * 
   * dBg.Writeln("finalNumShips " + finalNumShips + " :: possible ships: " +
   * possibleShips);
   * 
   * if (possibleShips >= maxPossibleShips) { // if not enough to defense from
   * this planet possibleShips = maxPossibleShips; break; } }
   * dBg.Writeln("return ships: possible: " + possibleShips +
   * " lastSuccessfulModeling = " + lastSuccessfulModeling); if
   * (lastSuccessfulModeling != -1) return lastSuccessfulModeling; else return
   * possibleShips; }
   */

  public static int CalculateModelledFinalNumShips(int numShips, Planet src,
      Planet dst) {
    int turnsModelled = 0;
    // LinkedList<Integer> ss = new LinkedList<Integer>();
    // ss.
    dBg.Writeln("BEFORE:: real Num ships: " + src.realNumShips
        + " Src num ships: " + src.NumShips() + " SRC ID: " + src.PlanetID()
        + " Dst num ships (real): " + dst.realNumShips + " dst owner: "
        + dst.Owner() + " dst modelled owner: " + dst.OwnerModelled() + " GR: "
        + dst.GrowthRate());
    int distance = Game.Distance(src, dst);
    Fleet dstFleet = new Fleet(1, numShips, src.PlanetID(), dst.PlanetID(),
        distance, distance);
    dst.AddFleet(dstFleet);
    Game.Sort(dst.sortedFleets, new FleetTurnsComparator());
    int modelledFinalNumShips = dst.realNumShips;
    dst.OwnerModelled(dst.Owner());
    for (Fleet fleet : dst.sortedFleets) {
      dBg.Writeln(" fleet numShips: " + fleet.NumShips() + " fleet turns rem: "
          + fleet.TurnsRemaining() + " fleet owner: " + fleet.Owner());
      if (dst.OwnerModelled() != 0)
        modelledFinalNumShips += (fleet.TurnsRemaining() - turnsModelled)
            * dst.GrowthRate();
      turnsModelled = fleet.TurnsRemaining();
      dBg.Writeln(" turns modelled: " + turnsModelled);
      if (dst.OwnerModelled() == fleet.Owner())
        modelledFinalNumShips += fleet.NumShips();
      else
        modelledFinalNumShips -= fleet.NumShips();
      if (modelledFinalNumShips < 0) {
        modelledFinalNumShips *= -1;
        dst.OwnerModelled(fleet.Owner());
      }
    }
    dBg.Writeln("AFTER::Dst num ships modelled: " + modelledFinalNumShips
        + " dst owner: " + dst.Owner() + " dst modelled owner: "
        + dst.OwnerModelled());
    dst.RemoveFleet(dstFleet);
    return dst.OwnerModelled() != 1 ? -modelledFinalNumShips
        : modelledFinalNumShips;
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
      dBg.Writeln("main Exeption" + e.getMessage());
      dBg.Close();
    }
    dBg.Close();
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
class FleetTurnsComparator implements Comparator<Fleet> {

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
