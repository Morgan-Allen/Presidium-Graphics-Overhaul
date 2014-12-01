


package stratos.start;
import stratos.game.actors.*;
import stratos.game.campaign.*;
import stratos.game.economic.*;
import stratos.util.*;



public class DebugBigSim {
  
  
  public static void main(String args[]) {
    
    final BaseDemands d1 = new BaseDemands(null);
    d1.initWithSupply(
      Backgrounds.CULTIVATOR, 8 ,
      Backgrounds.TECHNICIAN, 8 ,
      Backgrounds.MINDER    , 3 ,
      Backgrounds.FAB_WORKER, 3 ,
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
      int numI = (int) Visit.clamp(Integer.parseInt(input), 1, 100);
      
      while (numI-- > 0) {
        d1.update(1.0f);
        iters++;
      }
      d1.reportState();
    }
    
    I.say("Debugging complete.  Total iterations: "+iters+".");
  }
}




