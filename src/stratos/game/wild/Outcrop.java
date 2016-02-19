/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import static stratos.game.craft.Economy.*;

import stratos.game.common.*;
import stratos.game.craft.*;
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
    TYPE_DEPOSIT =  2,
    MAX_MINERALS = 10,
    MAX_DIG_DEEP =  4;
  
  final public static Traded
    ORE_TYPES[] = { METALS, METALS, FUEL_RODS };
  
  final int type;
  Object resource;
  float condition = 1.0f;
  
  
  public Outcrop(int size, int high, int type) {
    super(size, high * size);
    this.type = type;
  }
  
  
  public Outcrop(Session s) throws Exception {
    super(s);
    type      = s.loadInt();
    resource  = s.loadObject();
    condition = s.loadFloat();
    if (size > 1 || type == TYPE_DUNE) sprite().scale = size / 2f;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt   (type);
    s.saveObject((Session.Saveable) resource);
    s.saveFloat (condition);
  }
  

  
  /**  These are utility methods intended to determine the type and appearance
    *  of an outcrop based on underlying terrain type and mineral content.
    */
  private void assignResource() {
    final Stage world = origin().world();
    if (type == TYPE_DUNE) resource = null;
    
    //  TODO:  DERIVE THIS FROM THE UNDERLYING TERRAIN SOMEHOW!
    else resource = Rand.pickFrom(ORE_TYPES);
  }
  
  
  private float rubbleForOutcrop() {
    float rubble = 0, sum = 0;;
    for (Tile t : surrounds()) if (t != null) {
      rubble += t.habitat().minerals();
      sum++;
    }
    return rubble * 0.1f / sum;
  }
  
  
  public static ModelAsset modelFor(Outcrop outcrop, Stage world) {
    
    final Object ore = outcrop.resource;
    int oreID = Visit.indexOf(ore, ORE_TYPES);
    final float rubble  = outcrop.rubbleForOutcrop();
    final int size = outcrop.size, type = outcrop.type;
    
    if (size == 1 && type != TYPE_DUNE) {
      return Habitat.SPIRE_MODELS[Rand.index(3)][2];
    }
    if (type == TYPE_DUNE) {
      return Habitat.DUNE_MODELS[Rand.index(3)];
    }
    if (ore == FOSSILS || size != 3 || oreID == -1) {
      int highID = Rand.yes() ? 1 : Nums.clamp(3 - size, 3);
      return Habitat.SPIRE_MODELS[Rand.index(3)][highID];
    }
    else {
      return Rand.num() < rubble ?
        Habitat.ROCK_LODE_MODELS[oreID]:
        Habitat.MINERAL_MODELS  [oreID];
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
    
    this.assignResource();
    
    final ModelAsset model = modelFor(this, world);
    final Sprite s = model.makeSprite();
    if (size > 1 || type == TYPE_DUNE) s.scale = size / 2f;
    attachSprite(s);
    return true;
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    if (intact) refreshIncept(world, true);
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
    //if (this.type == TYPE_DUNE) return Owner.TIER_TERRAIN;
    //return Owner.TIER_PRIVATE;
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
  
  
  public Traded oreType() {
    if (resource instanceof Traded) return (Traded) resource;
    return null;
  }
  
  
  public float oreAmount() {
    return bulk() * MAX_MINERALS / 2f;
  }
  
  
  public static Traded oreType(Tile at) {
    final StageTerrain terrain = at.world.terrain();
    final int var = terrain.varAt(at);
    return ORE_TYPES[var % ORE_TYPES.length];
  }
  
  
  public static float oreAmount(Tile at) {
    final Traded type = oreType(at);
    if (type == null) return 0;
    final float amount = at.habitat().minerals() * MAX_MINERALS / 10f;
    return Nums.clamp(amount, 1, MAX_MINERALS);
  }
  
  
  public static Item mineralsAt(Target face) {
    Traded type = null;
    float amount = 0;
    
    if (face instanceof Tile) {
      final Tile at = (Tile) face;
      final Habitat h = at.habitat();
      if (at.above() instanceof Flora) return at.above().materials()[0];
      if (h.biomass() > 0) return Item.withAmount(POLYMER, h.biomass());
      
      type   = oreType  (at);
      amount = oreAmount(at);
    }
    
    if (face instanceof Outcrop) {
      final Outcrop at = (Outcrop) face;
      type   = at.oreType  ();
      amount = at.oreAmount() * at.condition();
    }
    
    if (type == null || amount == 0) return null;
    return Item.withAmount(type, amount);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    if (this.type == TYPE_DUNE) return "Dunes";
    final Item ore = mineralsAt(this);
    if (ore != null && ore.type != SLAG && size >= 3) {
      return "Deposit ("+ore.type+")";
    }
    else return "Outcrop";
  }
  
  
  public Composite portrait(HUD UI) {
    //  TODO:  FILL THIS IN!
    return null;
  }
  
  
  public String helpInfo() {
    final Item ore = mineralsAt(this);
    String s =
      "Rock outcrops are a frequent indication of underlying mineral wealth.";
    if (ore != null) s += "\n  Ore: "+ore;
    s += "\n  Condition: "+((int) (condition * 100))+"%";
    return s;
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






