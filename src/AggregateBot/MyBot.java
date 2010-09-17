package AggregateBot;
import java.util.*;
import planet_wars.*;

public class MyBot {
  // The DoTurn function is where your code goes. The PlanetWars object
  // contains the state of the game, including information about all planets
  // and fleets that currently exist. Inside this function, you issue orders
  // using the pw.IssueOrder() function. For example, to send 10 ships from
  // planet 3 to planet 8, you would say pw.IssueOrder(3, 8, 10).
  //
  // There is already a basic strategy in place here. You can use it as a
  // starting point, or you can throw it out entirely and replace it with
  // your own. Check out the tutorials and articles on the contest website at
  // http://www.ai-contest.com/resources.
  public static void DoTurn(PlanetWars pw) {
    // Calculate scores
    ArrayList<Score> scores = new ArrayList<Score>();
    for (Planet srcPlanet : pw.MyPlanets()) {
      LinkedList<Planet> otherPlanets = new LinkedList<Planet>(pw.Planets());
      otherPlanets.remove(srcPlanet);
      for (Planet dstPlanet : otherPlanets) {
        Score currentScore = new Score();
        currentScore.src = srcPlanet;
        currentScore.dst = dstPlanet;
        currentScore.value = calculateScore(srcPlanet, dstPlanet);
        scores.add(currentScore);
      }
    }
    // Determine max score
    Score maxScore = Collections.max(scores, new ScoreComparator());
    // Calculate number of ships in fleet
    int numShips = calculateShips(maxScore.src, maxScore.dst);
    // Orders modeling
    // Check time
    // Orders issues
    ArrayList<Order> orders = new ArrayList<Order>();
    for (Order order : orders) {
      pw.IssueOrder(order.src, order.dst, order.numShips);
    }
    
    /*
    // (1) If we currently have a fleet in flight, just do nothing.
    if (pw.MyFleets().size() >= 1) {
      return;
    }
    // (2) Find my strongest planet.
    Planet source = null;
    double sourceScore = Double.MIN_VALUE;
    for (Planet p : pw.MyPlanets()) {
      double score = (double) p.NumShips();
      if (score > sourceScore) {
        sourceScore = score;
        source = p;
      }
    }
    // (3) Find the weakest enemy or neutral planet.
    Planet dest = null;
    double destScore = Double.MIN_VALUE;
    for (Planet p : pw.NotMyPlanets()) {
      double score = 1.0 / (1 + p.NumShips());
      if (score > destScore) {
        destScore = score;
        dest = p;
      }
    }
    // (4) Send half the ships from my strongest planet to the weakest
    // planet that I do not own.
    if (source != null && dest != null) {
      int numShips = source.NumShips() / 2;
      pw.IssueOrder(source, dest, numShips);
    }
    */
  }
  
  private static double calculateScore(Planet src, Planet dst) {
    return 1.0;
  }
  
  private static int calculateShips(Planet src, Planet dst) {
    return src.NumShips() / 2;
  }

  public static void main(String[] args) {
    String line = "";
    String message = "";
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
      // Owned.
    }
  }
}

class Score {
  public Planet src;
  public Planet dst;
  public double value;
}

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

class Order {
  public Planet src;
  public Planet dst;
  public int numShips;
}
