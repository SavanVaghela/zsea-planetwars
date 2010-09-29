//package planet_wars;

import java.util.ArrayList;

public class Planet implements Cloneable {
  // Initializes a planet.
  public Planet(int planetID, int owner, int numShips, int growthRate,
      double x, double y) {
    this.planetID = planetID;
    this.owner = owner;
    this.numShips = numShips;
    this.growthRate = growthRate;
    this.x = x;
    this.y = y;
    this.ATTACK_PRIORITY = 1;
    numShipsModelled = numShips;
    ownerModelled = owner;
    realNumShips = numShips;
  }

  // Accessors and simple modification functions. These should be mostly
  // self-explanatory.
  // private int ownerModelled;

  public int OwnerModelled() {
    return ownerModelled;
  }

  public void OwnerModelled(int _owner) {
    ownerModelled = _owner;
  }

  public int NumShipsModelled() {
    return numShipsModelled;
  }

  public void NumShipsModelled(int _numShips) {
    numShipsModelled = _numShips;
  }

  public int PlanetID() {
    return planetID;
  }

  public int Owner() {
    return owner;
  }

  public int NumShips() {
    return numShips;
  }

  public int GrowthRate() {
    return growthRate;
  }

  public double X() {
    return x;
  }

  public double Y() {
    return y;
  }

  public void AddFleet(Fleet fleet) {
    sortedFleets.add(fleet);
  }

  public void RemoveFleet(Fleet fleet) {
    sortedFleets.remove(fleet);
  }

  public void Owner(int newOwner) {
    this.owner = newOwner;
  }

  public void NumShips(int newNumShips) {
    this.numShips = newNumShips;
  }

  public void AddShips(int amount) {
    numShips += amount;
  }

  public void RemoveShips(int amount) {
    numShips -= amount;
  }

  public void AddShipsModelled(int _numShips) {
    numShipsModelled += _numShips;
  }

  public void RemoveShipsModelled(int _numShips) {
    numShipsModelled -= _numShips;
  }

  public void Score(double newScore) {
    score = newScore;
  }

  public double Score() {
    return score;
  }

  public int Baseline() {
    return baseline;
  }

  private int planetID;
  private int owner;
  private int numShips;
  private int growthRate;
  private double x, y;
  public double score;
  private double d_2_e, d_2_o;
  private int baseline;
  public double ATTACK_PRIORITY;
  public ArrayList<Fleet> sortedFleets = new ArrayList<Fleet>();
  private int ownerModelled;
  private int numShipsModelled;
  public int realNumShips;

  public Planet(Planet _p) {
    planetID = _p.planetID;
    owner = _p.owner;
    numShips = _p.numShips;
    growthRate = _p.growthRate;
    x = _p.x;
    y = _p.y;
    ATTACK_PRIORITY = _p.ATTACK_PRIORITY;
    numShipsModelled = _p.numShipsModelled;
    ownerModelled = _p.OwnerModelled();
    realNumShips = _p.realNumShips;
  }

  public Object clone() {
    return new Planet(this);
  }
}
