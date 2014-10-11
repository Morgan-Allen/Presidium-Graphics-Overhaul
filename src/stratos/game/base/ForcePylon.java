


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.Actor;
import stratos.game.actors.Background;
import stratos.game.actors.Behaviour;
import stratos.game.building.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class ForcePylon extends Venue implements Selectable {
  
  
  /**  Construction and save/load methods-
    */
  //  TODO:  Refresh the art used here!
  final public static ModelAsset
    MODEL = CutoutModel.fromImage(
      ForcePylon.class, "media/Buildings/ecologist/sensor_array.png", 0.75f, 3
    );
  
  final static ImageAsset
    ICON = ImageAsset.fromImage(
      ForcePylon.class, "media/GUI/Buttons/shield_wall_button.gif"
    );
  
  
  public ForcePylon(Base base) {
    super(2, 3, ENTRANCE_NONE, base);
    attachModel(MODEL);
  }


  public ForcePylon(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Registration, life cycle and economic functions-
    */
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public TradeType[] services() {
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Force Pylon";
  }
  
  
  public String buildCategory() {
    //  TODO:  Come back to this later, once pseer guild is complete.
    return InstallTab.TYPE_HIDDEN;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "fence_pylon");
  }


  public String helpInfo() {
    return
      "Force Pylons generate the protective shields neccesary to reinforce "+
      "your perimeter defences.";
  }
}







/*
protected static FencePylon locateNewPost(KommandoLodge parent) {
  final World world = parent.world();
  final int numAttempts = 5, bonus = parent.structure.upgradeLevel(
    KommandoLodge.SENSOR_PERIMETER
  );
  
  float range = World.SECTOR_SIZE / 2f, spacing = 10;
  range *= 1 + bonus;
  final FencePylon post = new FencePylon(parent, 2 * (1 + bonus));
  
  Tile picked = null;
  float bestRating = 0;
  for (int n = numAttempts; n-- > 0;) {
    
    final Tile t = Spacing.pickRandomTile(parent, range, world);
    if (t.blocked()) continue;
    
    final FencePylon nearest = (FencePylon) world.presences.nearestMatch(
      FencePylon.class, t, spacing
    );
    if (nearest != null) continue;
    
    post.setPosition(t.x, t.y, world);
    if (! Spacing.perimeterFits(post)) continue;
    
    float rating = 1;
    final Venue nearV = (Venue) world.presences.nearestMatch(
      Venue.class, t, -1
    );
    if (nearV != null) rating += Spacing.distance(t, nearV);
    if (rating > bestRating) { picked = t; bestRating = rating; }
  }
  
  if (picked == null) return null;
  post.setPosition(picked.x, picked.y, world);
  return post;
}


public boolean enterWorldAt(int x, int y, World world) {
  if (! super.enterWorldAt(x, y, world)) return false;
  world.presences.togglePresence(this, origin(), true, FencePylon.class);
  return true;
}


public void exitWorld() {
  world.presences.togglePresence(this, origin(), false, FencePylon.class);
  super.exitWorld();
}


public int owningType() {
  return Element.ELEMENT_OWNS;
}


public int pathType() {
  return Tile.PATH_BLOCKS;
}


public void onGrowth(Tile t) {
  float inc = World.GROWTH_INTERVAL * 1f / World.STANDARD_DAY_LENGTH;
  batteryLife -= Rand.num() * inc;
  if (batteryLife <= 0) setAsDestroyed();
  else parent.base().intelMap.liftFogAround(this, 10);
}
//*/

