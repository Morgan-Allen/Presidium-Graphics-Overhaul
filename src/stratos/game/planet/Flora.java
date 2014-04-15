/**  
 *  Written by Morgan Allen.
 *  I intend to slap on some kind of open-source license here in a while, but
 *  for now, feel free to poke around for non-commercial purposes.
 */

package stratos.game.planet;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;



public class Flora extends Element implements TileConstants {
  
  
  /**
   * Field definitions and constructors-
   */
  final public static int MAX_GROWTH = 4;
  final public static float GROWTH_PER_UPDATE = 0.25f;
  private static boolean verbose = false;
  
  final Habitat habitat;
  final int varID;
  float growth = 0;
  
  
  private Flora(Habitat h) {
    this.habitat = h;
    this.varID = Rand.index(4);
  }
  
  
  public Flora(Session s) throws Exception {
    super(s);
    habitat = Habitat.ALL_HABITATS[s.loadInt()];
    varID = s.loadInt();
    growth = s.loadFloat();
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(habitat.ID);
    s.saveInt(varID);
    s.saveFloat(growth);
  }
  
  
  /**
   * Attempts to seed or grow new flora at the given coordinates.
   */
  public static void populateFlora(World world) {
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      final Tile t = world.tileAt(c.x, c.y);
      if (t.blocked()) continue;
      final float growChance = growChance(t);
      if (growChance == -1 || Rand.num() > growChance) return;
      
      final int crowding = crowdingAt(t);
      if (crowding >= 2) return;
      
      final Flora f = new Flora(t.habitat());
      f.enterWorldAt(t.x, t.y, world);
      float stage = 0.5f;
      for (int n = MAX_GROWTH; n-- > 0;) {
        if (Rand.num() < growChance * 4)
          stage++;
      }
      stage = Visit.clamp(stage, 0, MAX_GROWTH - 0.5f);
      f.incGrowth(stage, world, true);
      f.setAsEstablished(true);
      
      if (verbose) I.say("Initial flora at: " + t);
      world.ecology().impingeBiomass(t, f.growth, World.STANDARD_DAY_LENGTH);
    }
  }
  
  
  private static float growChance(Tile t) {
    if (t.habitat().floraModels == null) return -1;
    return (t.habitat().moisture / 10) / 4;
  }
  
  
  private static int crowdingAt(Tile t) {
    int numBlocked = 0;
    for (Tile n : t.allAdjacent(Spacing.tempT8)) if (n != null) {
      if (n.blocked()) numBlocked++;
      if (n.owningType() > Element.ELEMENT_OWNS) return 8;
    }
    return numBlocked;
  }
  
  
  public static boolean canGrowAt(Tile t) {
    if (t.blocked()) return false;
    final float growChance = growChance(t);
    if (growChance == -1) return false;
    final int crowding = crowdingAt(t);
    if (crowding >= 2) return false;
    return true;
  }
  
  
  public static Flora tryGrowthAt(Tile t) {
    final float growChance = growChance(t);
    if (growChance == -1) return null;
    
    if (t.owner() instanceof Flora) {
      final Flora f = (Flora) t.owner();
      if (Rand.num() < (growChance * 4 * GROWTH_PER_UPDATE)) {
        f.incGrowth(1, t.world, false);
        t.world.ecology().impingeBiomass(t, f.growth, World.GROWTH_INTERVAL);
      }
      return f;
    }
    
    if (! canGrowAt(t)) return null;
    if (Rand.num() > growChance) return null;

    if (Rand.num() < GROWTH_PER_UPDATE) {
      if (verbose) I.say("Seeding new tree at: "+t);
      final Flora f = new Flora(t.habitat());
      f.enterWorldAt(t.x, t.y,t.world);
      f.incGrowth(1, t.world, false);
      t.world.ecology().impingeBiomass(t, f.growth, World.GROWTH_INTERVAL);
      return f;
    }
    
    return null;
  }
  
  
  public void incGrowth(float inc, World world, boolean init) {
    final int oldGrowth = (int) growth;
    growth += inc;
    if (growth <= 0) {
      setAsDestroyed();
      return;
    }
    final int newGrowth = (int) growth;
    if (oldGrowth == newGrowth && !init)
      return;

    if (inc > 0 && !init) {
      final float moisture = origin().habitat().moisture / 10f;
      final int minGrowth = (int) ((moisture * moisture * MAX_GROWTH) + 1f);
      final float dieChance = 1 - moisture;
      if ((growth < 0) || (growth >= MAX_GROWTH * 2)
          || (growth > minGrowth && Rand.num() < dieChance)) {
        setAsDestroyed();
        return;
      }
    }
    final int tier = Visit.clamp((int) growth, MAX_GROWTH);
    final CutoutModel model = habitat.floraModels[varID][tier];
    final Sprite oldSprite = this.sprite();
    attachSprite(model.makeSprite());
    setAsEstablished(false);
    if (oldSprite != null)
      world.ephemera.addGhost(this, 1, oldSprite, 2.0f);
  }
  
  
  public int growStage() {
    return Visit.clamp((int) growth, MAX_GROWTH);
  }
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (!super.enterWorldAt(x, y, world))
      return false;
    world.presences.togglePresence(this, true);
    return true;
  }
  
  
  public void exitWorld() {
    world.presences.togglePresence(this, false);
    super.exitWorld();
  }
  
  
  public String toString() {
    return "Flora";
  }
}




/*
for (int i : N_INDEX) {
  final Tile n = world.tileAt(t.x + N_X[i], t.y + N_Y[i]);
  if (n == null || n.blocked())
    numBlocked++;
  if (n != null) {
    if (n.owningType() > Element.ELEMENT_OWNS) {
      numBlocked = 8;
      break;
    }
    if (n.owner() instanceof Boardable) {
      if (t.isEntrance((Boardable) n.owner())) {
        numBlocked = 8;
        break;
      }
    }
  }
}
//*/