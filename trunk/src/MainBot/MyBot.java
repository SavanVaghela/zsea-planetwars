 //  package MainBot;
 //   import java.io.BufferedWriter;
  import java.io.FileWriter;
  import java.io.BufferedWriter;
import java.util.*;

 // import planet_wars.*;
///////////////////////////////////////////////////////////////////////////////////////////////
//MyBot 
///////////////////////////////////////////////////////////////////////////////////////////////
public class MyBot { 
  public static int Turn = 0;
//  public static Debugger dBg = new Debugger("loooooooooog.txt");
 // private static LinkedList<Order> HelpOrders;
  
  @SuppressWarnings("unchecked")
  public static void DoTurn(PlanetWars pw) {
    // Calculate scores
    if(pw.NotMyPlanets().size() == 0) return;
//    dBg.Writeln("::Start::");
    Game.Initialize(pw);
//    dBg.Writeln("::Initialize end::");
  //  HelpOrders = new LinkedList<Order>();
    PremodellingGameMap();
    //Calculate aggregate scores
    List<Planet> notMyPlanets = Game.NotMyPlanets();
    for( Planet planet : notMyPlanets) {
//      dBg.Writeln("::calculateAGGRScore start::");
       planet.Score(CalculateAggregateScore(planet, pw));  
//       dBg.Writeln("::calculateAGGRScore end::");
    }
    List<Score> scores = new ArrayList<Score>();

    
    for (Planet srcPlanet : Game.myPlanets) {
      List<Planet> otherPlanets = Game.Planets();
      otherPlanets.remove(srcPlanet);
      for (Planet dstPlanet : otherPlanets) {
        Score currentScore = new Score();
        currentScore.src = srcPlanet;
        currentScore.dst = dstPlanet;
   //     dBg.Writeln("::calculateScore start::");
        currentScore.value = calculateScore(srcPlanet, dstPlanet, pw);
  //      dBg.Writeln("::calculateScore end::");
        scores.add(currentScore);
      }
    }
    //List<Score> sortedScores = 
 //   dBg.Writeln("::Sort start::");
    Game.Sort(scores, new ScoreComparator());
    Game.Sort(notMyPlanets, new AggregateScoreComparator());
//    dBg.Writeln("::Sort end::");
    // Orders modeling
     ArrayList<Order> orders = new ArrayList<Order>();
    
    for(Planet attackedPlanet : notMyPlanets){
      ArrayList<Order> _orders = GetOrdersForAttackThePlanet(attackedPlanet, scores, pw);
      
      if(_orders!=null)
       orders.addAll(_orders);
      else break;
        /*
      for(Order o :_orders){
        dBg.Writeln("::"+o.src.PlanetID() + "::" + o.dst.PlanetID() + "::" + o.numShips);
      }
   //   if(_orders!=null)
*/
    }
     
//    dBg.Writeln("Order preIssue");
    // Check time
   
    // Orders issues
    
    for (Order order : orders) {     
      pw.IssueOrder(order.src, order.dst, order.numShips);
   
   }
 //   dBg.Writeln("Order Issues");
  }
 ////////////////////////////////////////////////////////////////////////////////////
  //
 ////////////////////////////////////////////////////////////////////////////////////
  
  private static double calculateScore(Planet src, Planet dst, PlanetWars pw) {
   // double c = (src.NumShips() - dst.NumShips());
  
    double c = 1;//( c <= 0 ? 1:c); 
    return pw.Distance(src.PlanetID(), dst.PlanetID());//*(c) + src.PlanetID()/100 + dst.PlanetID()/100;
  }
  
  private static double CalculateAggregateScore(Planet targetP, PlanetWars pw){
    double distanceScore = targetP.ATTACK_PRIORITY*
                          (Game.GENERAL_DISTANCE_PRIORITY*CalculateDistanceScore(targetP, pw)
                          + Game.NUMB_OF_SHIPS_PRIORITY *targetP.GrowthRate()/(targetP.NumShips()+1));
    
    return distanceScore;
  }
//  , pw) / planet.NumShips() * planet.GrowthRate());
 ///////////////////////////////////////////////
 
  
  private static ArrayList<Order> GetOrdersForAttackThePlanet(Planet targetPlanet, List<Score> scores,
                                                                PlanetWars pw){
    ArrayList<Order> orders = new ArrayList<Order>();
   // dBg.Writeln("::Getting oreder::");
    int growthTurnsAnalized = -1;
    int shipsOnTargetPlanet = targetPlanet.NumShips();
    shipsOnTargetPlanet += (targetPlanet.Owner() == 0 ? Game.ATTACKER_SHIPS_LAY_UP_NEUTRAL :
                                                        Game.ATTACKER_SHIPS_LAY_UP_ENEMY);
    for(Score s : scores){
      if(s.dst.PlanetID() == targetPlanet.PlanetID()){
        //Order o = new Order(s.src, s.dst, 1);
       // orders.add(o);
            
        int possibleShips2Attack = (int)(s.src.NumShips() - Game.PLANETS_BASELINE);///// replace to Baseline
        if(possibleShips2Attack <= 0) 
          continue; 
        
        int ships2Attack = 0 ; 
        
        int actualDistance = pw.Distance(s.src.PlanetID(), s.dst.PlanetID());
        if(s.dst.Owner() == 2 && actualDistance > growthTurnsAnalized) // if enemy's planet? calculate his growth rate.
        {
          int gr = targetPlanet.GrowthRate()+3;
          int turnsGrowsRateConsidered = (int)(possibleShips2Attack / gr);
          actualDistance -= growthTurnsAnalized;
          if(turnsGrowsRateConsidered >= actualDistance){
            growthTurnsAnalized += actualDistance;
            possibleShips2Attack -= actualDistance*gr; 
          }
          else continue;
        }
        ships2Attack = (shipsOnTargetPlanet <= possibleShips2Attack ? 
                          shipsOnTargetPlanet + 1 : possibleShips2Attack);
        shipsOnTargetPlanet -= ships2Attack;
        orders.add(new Order(s.src, s.dst, ships2Attack));
          
        //  shipsOnTargetPlanet -= ships2Attack;
        
        if(shipsOnTargetPlanet <= 0) break;
      }
    }
    if(shipsOnTargetPlanet > 0)
      return null;
    for(Order o :orders){
      o.src.RemoveShips(o.numShips);
  }
    //dBg.Writeln("::oreder complete::");
    return orders;
  }
  
  private static void PremodellingGameMap()
  {
 // Our reinforcements;
    for(Planet planet : Game.myPlanets){
      
     // for(Planet planet: Game.myPlanets){
       LinkedList<Fleet> enemyFleets = Game.GetAttackedFleets(planet);
       LinkedList<Fleet> reinfFleets = Game.GetReinforcementFleets(planet);//(planet);
           
       int shipsOnPlanet = planet.NumShips();
       short sign = 1;
       int turnsAlreadyModelled = 0;
       for(Fleet reinfFleet : reinfFleets){
         shipsOnPlanet += reinfFleet.NumShips();
         }
         for(Fleet enemyFleet : enemyFleets){
         shipsOnPlanet += //(sign*enemyFleet.TurnsRemaining()- turnsAlreadyModelled )
                                - enemyFleet.NumShips();
         turnsAlreadyModelled  = enemyFleet.TurnsRemaining();
         if(shipsOnPlanet < 0)
              sign = -1;
         }
         if(sign == -1){ //DEFENCE! will be in future
              //planet.Owner(2);
   //        Order order = new Order(planet, null, -shipsOnPlanet + Game.PLANETS_BASELINE);
  //        HelpOrders.add(order);
      //     planet.NumShips(0); ///!!!!!!!!!! replace to smth. more smart))
    //       planet.ATTACK_PRIORITY = 100;
           //Game.ChangePlanetOwner(planet, 2);
              //pw.SetPlanet(planet);
         }
          else{ 
            planet.NumShips(Math.min(shipsOnPlanet, planet.NumShips()));
          }
    ///////////////////////////////////////////////////////////////////////////////////////////        
        /* LinkedList<Fleet> myReinfFleets = Game.GetReinforcementFleets(planet);
         int reinforcement = 0;
         for(Fleet f : myReinfFleets)
           reinforcement += f.NumShips();
         if(reinforcement != 0){
           planet.AddShips(reinforcement);
         }*/
      }
      // Enemies reinforcements;
     for(Planet planet : Game.enemyPlanets){
        LinkedList<Fleet> enemyReinfFleets = Game.GetReinforcementFleets(planet);
        int reinforcement = 0;
        for(Fleet f : enemyReinfFleets)
          reinforcement += f.NumShips();
        if(reinforcement != 0){
          planet.AddShips(reinforcement);
        }
        // Our attacker ships;
        LinkedList<Fleet> enemyAttackersFleets = Game.GetAttackedFleets(planet,1);
        int attackers = 0;
        for(Fleet f : enemyAttackersFleets)
          attackers += f.NumShips();
  
          planet.RemoveShips(attackers);
       
         if(planet.NumShips() < 0) planet.ATTACK_PRIORITY = 0;
         
        planet.NumShips(Math.max(0, planet.NumShips()));
      }
  //HERE DO NOT CONSIDERAD ENEMY FLEETS THAT ATTACKED NEUTRAL PLANET!!!!!! correct it in future.
    for(Planet planet: Game.neutralPlanets){
  // Our attacker ships to neutral planet;
     LinkedList<Fleet> myAttackersFleets = Game.GetAttackedFleets(planet, 1);
     int attackers = 0;
     for(Fleet f : myAttackersFleets)
       attackers += f.NumShips();
       planet.RemoveShips(attackers);
       
       if(planet.NumShips() < 0) planet.ATTACK_PRIORITY = 0;
       planet.NumShips(Math.max(0, planet.NumShips()));
   }

  }

  private static double CalculateDistanceScore(Planet currPlanet, PlanetWars pw)
  {
     double myD = 0.001, enemyD = 0.001;
     for(Planet ourPlanet : Game.myPlanets)
         myD += Math.pow(pw.Distance(currPlanet.PlanetID(), ourPlanet.PlanetID()), 
             Game.EXPONENTIAL_DISTANCE_PRIORITY);
     
     for(Planet enemyPlanet : Game.enemyPlanets)
         enemyD += Math.pow(pw.Distance(currPlanet.PlanetID(), enemyPlanet.PlanetID())
                  ,Game.EXPONENTIAL_DISTANCE_PRIORITY);
     
     double gameRatio = (Game.myPlanets.size()+1) / (Game.enemyPlanets.size()+1);
            gameRatio *= pw.NumShips(1) / (pw.NumShips(2)+1);
            gameRatio *= (Game.GetSummaryPlanetGrowthRate(1)+1) / (Game.GetSummaryPlanetGrowthRate(2)+1);
     double myScore = myD / (Game.myPlanets.size()+1);
     double enemyScore = 0;
     if(Game.enemyPlanets.size() == 1 && currPlanet.Owner() == 2) //if enemy has only one planet: DESTROY IT by all forces! 
         enemyScore = myScore * gameRatio;
     else
         enemyScore = enemyD / (Game.enemyPlanets.size()+1);
     return   enemyScore/myScore;
  }
  
  private static int calculateShips(Planet src, Planet dst) {
    return src.NumShips() / 2;
  }
////////////////////////////////////////////////////////////////////////////////////////////////////
  //// MAIN ////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) {
    String line = "";
    String message = "";
    //// start debug
 /*  PlanetWars _pw = new PlanetWars("");
    int res = _pw.LoadMapFromFile("D:\\MyProjects\\Eclipse\\PlanetWars\\maps\\map3.txt");
    DoTurn(_pw); */
    //return;
    //// finish debug
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
//     dBg.Writeln("main Exeption");
//     dBg.Close();
    }
//    dBg.Close();
  }
 
}
///////////////////////////////////////////////////////////////////////////////////////////////
// Score
///////////////////////////////////////////////////////////////////////////////////////////////
class Score {
  public boolean equals(Object obj){
    if(value == ((Score) obj).value) 
      return true;
    return false;
  }
  
  public Planet src;
  public Planet dst;
  public double value;
}
///////////////////////////////////////////////////////////////////////////////////////////////
// FleetDistanseComporator
///////////////////////////////////////////////////////////////////////////////////////////////
class FleetDistanseComporator implements Comparator {
  
  int gr, ls;
  public FleetDistanseComporator(boolean ASC) {
      gr = (ASC == true ? -1 : 1);
      ls = -1*gr;
  }
  public int compare(Object fleet1, Object fleet2) {
    int dist1 = ((Fleet) fleet1).TurnsRemaining();
    int dist2 = ((Fleet) fleet2).TurnsRemaining();
    if(dist1 > dist2) return gr;
    if(dist1 < dist2) return ls;
                      return 0;
  }
}
///////////////////////////////////////////////////////////////////////////////////////////////
// ScoreComparator
///////////////////////////////////////////////////////////////////////////////////////////////
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

///////////////////////////////////////////////////////////////////////////////////////////////
// AggregateScoreComparator
///////////////////////////////////////////////////////////////////////////////////////////////
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
///////////////////////////////////////////////////////////////////////////////////////////////
//Order
///////////////////////////////////////////////////////////////////////////////////////////////
class Order {
public Order(Planet _src, Planet _dst, int _numShips){
src = _src;
dst = _dst;
numShips = _numShips;
}
public Planet src;
public Planet dst;
public int numShips;
}

class Debugger{
  private BufferedWriter bw;
  public Debugger(String logFileName){
   try{
    bw = new BufferedWriter(new FileWriter(logFileName, false));
   }
   catch(Exception e){}
  }
  
  public void Write(String str){
    try{
      bw.write(str);
      bw.flush();
     }
     catch(Exception e){}
  }
  public void Writeln(String str){
    try{
      bw.write(str+"\r\n");
      bw.flush();
     }
     catch(Exception e){}
  }
  
  public void Close(){
    try{
      bw.close();
     }
     catch(Exception e){}
  }
}
