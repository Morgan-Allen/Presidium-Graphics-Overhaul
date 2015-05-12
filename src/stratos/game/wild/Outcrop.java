


package stratos.game.wild;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.util.*;
import stratos.game.economic.Owner;
import stratos.game.maps.StageTerrain;



public class Outcrop extends Fixture {
  
  
  /**  These are utility methods intended to determine the type and appearance
    *  of an outcrop based on underlying terrain type and mineral content.
    */
  final public static int
    TYPE_MESA    =  0,
    TYPE_DUNE    =  1,
    TYPE_DEPOSIT =  2;
  
  
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
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final int type;
  int mineral = -1;
  float condition = 1.0f;
  
  
  public Outcrop(int size, int high, int type) {
    super(size, high * size);
    this.type = type;
  }
  
  
  public Outcrop(Session s) throws Exception {
    super(s);
    type = s.loadInt();
    mineral = s.loadInt();
    condition = s.loadFloat();
    if (size > 1 || type == TYPE_DUNE) sprite().scale = size / 2f;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(type);
    s.saveInt(mineral);
    s.saveFloat(condition);
  }
  
  

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
  
  
  public boolean enterWorldAt(int x, int y, Stage world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    final ModelAsset model = modelFor(this, world);
    final Sprite s = model.makeSprite();
    if (size > 1 || type == TYPE_DUNE) s.scale = size / 2f;
    attachSprite(s);
    setAsEstablished(true);
    world.presences.togglePresence(this, origin(), true , Outcrop.class);
    return true;
  }
  
  
  public int owningTier() {
    return Owner.TIER_OBJECT;
  }
  
  
  public void exitWorld() {
    world.presences.togglePresence(this, origin(), false, Outcrop.class);
    super.exitWorld();
  }
  
  
  public byte mineralType() {
    return (byte) mineral;
  }
  
  
  public void incCondition(float inc) {
    condition = Nums.clamp(condition + inc, 0, 1);
    if (condition <= 0) setAsDestroyed();
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
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Outcrop";
  }
  
  
  public String toString() {
    return fullName();
  }
}


  

  /*
  public Composite portrait(HUD UI) {
    return null;
  }


  public void writeInformation(Description d, int categoryID, HUD UI) {
    final int c = (int) (100 * condition());
    d.append("  Condition: "+c+"%");
    int varID = this.mineral;
    if (varID < 0) varID = 0;
    d.append("\n  Outcrop type: "+Terrain.MINERAL_NAMES[varID]);
    d.append("\n\n");
    d.append(helpInfo());
  }


  public Target selectionLocksOn() {
    return this;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    Selection.renderPlane(
      rendering, viewPosition(null),
      radius() + 0.5f + ((size - 1) / 5f),
      Colour.transparency(hovered ? 0.25f : 0.5f),
      Selection.SELECT_CIRCLE
    );
  }
  
  public String helpInfo() {
    return
      "Rock outcrops are a frequent indication of underlying mineral wealth.";
  }
  
  
  public String[] infoCategories() {
    return null;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new InfoPanel(UI, this, 0);
  }
  
  
  public void whenClicked() {
    //
    //  TODO:  This is some really awkward phrasing.  When have you ever used
    //  a *non*-BaseUI?
    BaseUI.current().selection.pushSelection(this, false);
  }
  //*/






