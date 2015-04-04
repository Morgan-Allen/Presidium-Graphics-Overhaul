


package stratos.game.base;
import stratos.game.economic.*;



public interface Location {
  
  
  Location[] nearby();
  float[] distances();
  int transportTypes();
  
  int climate();
  float area();
  Item[] resources();
  BaseDemands profile();
}