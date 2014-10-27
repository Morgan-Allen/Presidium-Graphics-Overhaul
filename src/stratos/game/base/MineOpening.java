


package stratos.game.base;
import stratos.game.building.*;
import stratos.game.common.*;



//  TODO:  You can use these to build up an underground tunnel network!

//  ...It can be almost any resolution, as long as you don't insist on plugging
//  into the edges exactly.  Then, you can even link into the openings from
//  other shafts.

//  I think it's best if it still uses the 'onTop' method for ownership
//  purposes, though?  ...Well, something to ponder.



public class MineOpening extends Fixture {
  
  
  final static int
    OPENING_SIZE = 2;
  
  final boolean onSurface;
  private Boarding canBoard[];
  
  
  
  protected MineOpening(ExcavationSite opens, boolean onSurface) {
    super(OPENING_SIZE, onSurface ? 1 : 0);
    this.onSurface = onSurface;
    
    attachModel(Smelter.OPENING_SHAFT_MODEL);
  }
  
  
  public MineOpening(Session s) throws Exception {
    super(s);
    this.onSurface = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveBool(onSurface);
  }
  
  
}



