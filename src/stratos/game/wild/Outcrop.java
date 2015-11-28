/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.economic.*;
import static stratos.game.economic.Economy.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.maps.StageTerrain;



public class Outcrop extends Fixture {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static int
    TYPE_MESA    =  0,
    TYPE_DUNE    =  1,
    TYPE_DEPOSIT =  2;
  
  final int type;
  int mineral = -1;
  float condition = 1.0f;
  
  
  public Outcrop(int size, int high, int type) {
    super(size, high * size);
    this.type = type;
  }
  
  
  public Outcrop(Session s) throws Exception {
    super(s);
    type      = s.loadInt  ();
    mineral   = s.loadInt  ();
    condition = s.loadFloat();
    if (size > 1 || type == TYPE_DUNE) sprite().scale = size / 2f;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt  (type     );
    s.saveInt  (mineral  );
    s.saveFloat(condition);
  }
  

  
  /**  These are utility methods intended to determine the type and appearance
    *  of an outcrop based on underlying terrain type and mineral content.
    */
  static float rubbleFor(Outcrop outcrop, Stage world) {
    float rubble = 0, sum = 0;;
    for (Tile t : outcrop.surrounds()) if (t != null) {
      rubble += t.habitat().minerals();
      sum++;
    }
    return rubble * 0.1f / sum;
  }
  
  
  static int mineralTypeFor(Outcrop outcrop, Stage world) {
    //
    //  First, we sum up the total for each mineral type in the surrounding
    //  area (including the rockiness of the terrain.)
    float amounts[] = new float[4];
    int numTiles = 0;
    for (Tile t : outcrop.surrounds()) if (t != null) {
      final byte type = world.terrain().mineralType(t);
      final float amount = world.terrain().mineralsAt(t, type);
      amounts[type] += amount;
      amounts[0] += t.habitat().minerals();
      numTiles++;
    }
    amounts[0] *= Rand.num() / 2f;
    //
    //  Then perform a weighted pick from the range of types (having tweaked
    //  the odds a little...)
    float sumAmounts = 0;
    for (int i = 4; i-- > 0;) {
      final float a = amounts[i];
      sumAmounts += (amounts[i] = (a + (a * a)) / 2);
    }
    float pickRoll = Rand.num() * sumAmounts;
    int pickType = 0;
    int type = 0; for (float f : amounts) {
      if (pickRoll < f) { pickType = type; break; }
      pickRoll -= f;
      type++;
    }
    return pickType;
  }
  
  
  public static ModelAsset modelFor(Outcrop outcrop, Stage world) {
    
    final int   mineral = mineralTypeFor(outcrop, world);
    final float rubble  = rubbleFor(outcrop, world);
    outcrop.mineral = mineral;
    final int size = outcrop.size, type = outcrop.type;
    
    if (size == 1 && type != TYPE_DUNE) {
      return Habitat.SPIRE_MODELS[Rand.index(3)][2];
    }
    if (type == TYPE_DUNE) {
      return Habitat.DUNE_MODELS[Rand.index(3)];
    }
    if (mineral == 0 || size != 3) {
      int highID = Rand.yes() ? 1 : (3 - size);
      return Habitat.SPIRE_MODELS[Rand.index(3)][highID];
    }
    else {
      return Rand.num() < rubble ?
        Habitat.ROCK_LODE_MODELS[mineral - 1] :
        Habitat.MINERAL_MODELS  [mineral - 1] ;
    }
  }
  
  
  
  /**  Positioning and life-cycle:
    */
  public boolean canPlace() {
    //  This only gets called just before entering the world, so I think I can
    //  put this here.  TODO:  Move the location-verification code from the
    //  TerrainGen class to here?  ...Might be neater.
    final Stage world = origin().world;
    for (Tile t : world.tilesIn(footprint(), false)) {
      if (t == null || t.blocked()) return false;
      if (type == TYPE_DUNE && t.habitat() != Habitat.DUNE) return false;
    }
    return true;
  }
  
  
  public boolean setPosition(float x, float y, Stage world) {
    if (! super.setPosition(x, y, world)) return false;
    final ModelAsset model = modelFor(this, world);
    final Sprite s = model.makeSprite();
    if (size > 1 || type == TYPE_DUNE) s.scale = size / 2f;
    attachSprite(s);
    return true;
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    if (intact) refreshIncept(true);
    world.presences.togglePresence(this, origin(), true , Outcrop.class);
    return true;
  }
  
  
  public void exitWorld() {
    world.presences.togglePresence(this, origin(), false, Outcrop.class);
    super.exitWorld();
  }
  
  
  
  /**  Physical attributes and queries-
    */
  public int owningTier() {
    return Owner.TIER_OBJECT;
  }
  
  
  public Traded mineralType() {
    return mineralFor((byte) mineral);
  }
  
  
  public void incCondition(float inc) {
    condition = Nums.clamp(condition + inc, 0, 1);
    if (condition <= 0) setAsDestroyed(false);
  }
  
  
  public float condition() {
    return condition;
  }
  
  
  public float bulk() {
    return size * size * high;
  }
  
  
  public float mineralAmount() {
    return condition * bulk() * StageTerrain.MAX_MINERAL_AMOUNT / 2f;
  }
  
  
  public static Traded mineralFor(byte type) {
    if (type == -1) return null;
    Traded minType = SLAG;
    if (type == StageTerrain.TYPE_ISOTOPES) minType = FUEL_RODS;
    if (type == StageTerrain.TYPE_METALS  ) minType = METALS   ;
    if (type == StageTerrain.TYPE_RUINS   ) minType = FOSSILS  ;
    return minType;
  }
  
  
  public static Item mineralsAt(Tile face) {
    final StageTerrain terrain = face.world().terrain();
    final Habitat h = face.habitat();
    
    if (face.above() instanceof Flora) return face.above().materials()[0];
    if (h.biomass() > 0) return Item.withAmount(POLYMER, h.biomass());
    
    if (terrain.mineralsAt(face) == 0) return null;
    
    final byte  type   = terrain.mineralType(face);
    final float amount = terrain.mineralsAt(face, type);
    return Item.withAmount(mineralFor(type), amount);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    if (this.type == TYPE_DUNE) return "Dunes";
    
    final Traded minType = mineralType();
    if (minType != null && minType != SLAG && size >= 3) {
      return "Deposit ("+minType+")";
    }
    else return "Outcrop";
  }
  
  
  public Composite portrait(HUD UI) {
    //  TODO:  FILL THIS IN!
    return null;
  }
  
  
  public String helpInfo() {
    return
      "Rock outcrops are a frequent indication of underlying mineral wealth.";
  }
}


//  TODO:  CONSIDER RE-USING THIS!  
  /*
  public void writeInformation(Description d, int categoryID, HUD UI) {
    final int c = (int) (100 * condition());
    d.append("  Condition: "+c+"%");
    int varID = this.mineral;
    if (varID < 0) varID = 0;
    d.append("\n  Outcrop type: "+Terrain.MINERAL_NAMES[varID]);
    d.append("\n\n");
    d.append(helpInfo());
  }
  //*/






