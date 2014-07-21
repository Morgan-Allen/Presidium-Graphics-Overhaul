


package stratos.game.campaign;
import stratos.game.building.*;



public interface Location {
  
  
  Location[] nearby();
  float[] distances();
  int transportTypes();
  
  int climate();
  Item[] resources();
  BaseDemands profile();
}