/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.craft.*;
import stratos.util.*;



//  TODO:  REVISIT THIS...

public class DebugBigSim {
  
  
  public static void main(String args[]) {
    
    /*
    
    final BaseDemands d1 = new BaseDemands(null);
    d1.initWithSupply(
      Backgrounds.CULTIVATOR, 8 ,
      Backgrounds.TECHNICIAN, 8 ,
      Backgrounds.MINDER    , 3 ,
      Backgrounds.SUPPLY_CORPS, 3 ,
      Backgrounds.VOLUNTEER , 5 ,
      Backgrounds.KNIGHTED  , 1 
    );
    
    int iters = 0;
    Assets.compileAssetList("stratos.game.base");
    Assets.advanceAssetLoading(-1);
    
    while (true) {
      I.say(
        "Iterations: "+iters+".  Please enter:\n"+
        "  1-100   The number of iterations desired.\n"+
        "  x or X  To exit."
      );
      
      final String input = I.listen();
      if (input.toLowerCase().equals("x")) break;
      int numI = (int) Nums.clamp(Integer.parseInt(input), 1, 100);
      
      while (numI-- > 0) {
        d1.update(1.0f);
        iters++;
      }
      d1.reportState();
    }
    
    I.say("Debugging complete.  Total iterations: "+iters+".");
    //*/
  }
}




