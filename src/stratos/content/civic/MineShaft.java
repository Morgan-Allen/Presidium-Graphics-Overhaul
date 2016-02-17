/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.wild.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class MineShaft extends Fixture implements Boarding {
  
  
  final static int
    OPENING_SIZE = 2;
  
  final static List <Mobile> NONE_INSIDE = new List <Mobile> ();
  
  
  final ExcavationSite parent;
  final boolean onSurface;
  private List <Mobile> inside = NONE_INSIDE;
  
  private Item mineralsLeft[];
  private Boarding canBoard[];
  
  
  
  protected MineShaft(ExcavationSite opens, boolean onSurface) {
    super(OPENING_SIZE, onSurface ? 1 : 0);
    this.parent = opens;
    this.onSurface = onSurface;
    
    attachModel(Smelter.OPENING_SHAFT_MODEL);
  }
  
  
  public MineShaft(Session s) throws Exception {
    super(s);
    this.parent = (ExcavationSite) s.loadObject();
    this.onSurface = s.loadBool();
    s.loadObjects(inside);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(parent);
    s.saveBool(onSurface);
    s.saveObjects(inside);
  }
  
  
  
  /**  Boardable Interface Contract-
    */
  public void setInside(Mobile m, boolean is) {
    if (is) {
      if (inside == NONE_INSIDE) inside = new List <Mobile> ();
      inside.include(m);
    }
    else {
      inside.remove(m);
      if (inside.size() == 0) inside = NONE_INSIDE;
    }
  }
  
  
  public Series <Mobile> inside() {
    return inside;
  }
  
  
  public boolean allowsEntry(Accountable m) {
    return m.base() == parent.base();
  }
  
  
  public int boardableType() {
    return BOARDABLE_OTHER;
  }
  
  
  public Boarding[] canBoard() {
    if (canBoard != null) return canBoard;
    final Batch <Boarding> touches = new Batch <Boarding> ();
    
    //  TODO:  How do I get the list of adjacent openings, though?  It needs
    //  some form of world-registration, but it can't go on top of existing
    //  tiles.
    
    //  Maybe you should space 'em out for it.
    
    return canBoard = touches.toArray(Boarding.class);
  }
  
  
  public boolean isEntrance(Boarding b) {
    return Visit.arrayIncludes(canBoard(), b);
  }
  
  
  
  public void openFace() {
    for (Boarding b : canBoard()) if (b instanceof MineShaft) {
      ((MineShaft) b).canBoard = null;
    }
  }
  
  
  
  /**  Placement and economic rating methods-
    */
  public Item[] mineralsLeft() {
    if (mineralsLeft != null) return mineralsLeft;
    
    final Batch <Item> left = new Batch <Item> ();
    for (Tile under : world.tilesIn(footprint(), false)) {
      final Item at = Outcrop.mineralsAt(under);
      if (at != null) left.add(at);
    }
    return mineralsLeft = left.toArray(Item.class);
  }
  
  
  //  TODO:  This should be handled using claim-radii.  Well, just see how it
  //  looks for now.
  
  //  TODO:  Make mine openings into venues.  They don't strictly need to
  //  employ anyone.
  
  public static boolean checkForPlacementAt(ExcavationSite site, Tile under) {
    
    I.say("\nChecking for opening at "+under);
    I.say("  Site position: "+site.origin());
    
    final MineShaft open = new MineShaft(site, true);
    open.setPosition(under.x, under.y, under.world);
    
    final Presences p = under.world.presences;
    final Target nearE = p.nearestMatch(ExcavationSite.class, open, 3);
    if (nearE != null) return false;
    
    final Target nearO = p.nearestMatch(MineShaft.class, open, 3);
    if (nearO != null) return false;
    
    if (! open.canPlace()) return false;
    
    open.enterWorld();
    return true;
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    ///world.presences.togglePresence(this, origin(), true, MineShaft.class);
    ///parent.base().transport.updatePerimeter(this, true);
    return true;
  }
  
  
  public void exitWorld() {
    world.presences.togglePresence(this, origin(), false, MineShaft.class);
    super.exitWorld();
  }
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Mine Shaft";
  }
  
  
  public Composite portrait(HUD UI) {
    //  TODO:  FILL THIS IN!
    return null;
  }
  
  
  public String helpInfo() {
    return
      "Mine shafts and mantle drills provide improved access to mineral "+
      "deposits deep underground or at a distance.";
  }
}






