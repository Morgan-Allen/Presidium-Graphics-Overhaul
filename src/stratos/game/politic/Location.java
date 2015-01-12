


package stratos.game.politic;
import stratos.game.common.BaseDemands;
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