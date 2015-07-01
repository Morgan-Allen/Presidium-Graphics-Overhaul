/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.wild.Habitat;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class Tailing extends Element {
  
  
  private static boolean verbose = true;
  
  final static int
    MIN_FILL =  0,
    MAX_FILL = 40;
  
  final Traded wasteType;
  
  private float fillLevel = 0;
  
  
  public Tailing(Traded wasteType) {
    super();
    this.wasteType = wasteType;
    updateSprite();
  }
  
  
  public Tailing(Session s) throws Exception {
    super(s);
    this.wasteType = (Traded) s.loadObject();
    this.fillLevel = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(wasteType);
    s.saveFloat (fillLevel);
  }
  
  
  
  /**  Life-cycle, updates and maintenance-
    */
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    world.terrain().setHabitat(origin(), Habitat.TOXIC_RUNOFF);
    
    for (Tile t : origin().allAdjacent(null)) {
      if (t != null) t.clearUnlessOwned(intact);
    }
    return true;
  }
  
  
  public int owningTier() {
    return Owner.TIER_PRIVATE;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS;
  }
  
  
  public boolean takeFill(float amount) {
    final float newFill = fillLevel + amount;
    if (amount < 0 || newFill > MAX_FILL) return false;
    this.fillLevel = newFill;
    updateSprite();
    return true;
  }
  
  
  public Traded wasteType() {
    return wasteType;
  }
  
  
  public void onGrowth(Tile at) {
    //  TODO:  Very gradually disappear based on terraforming effects?
  }
  
  
  
  /**  Graphical constants and update methods-
    */
  final static String IMG_DIR = "media/Buildings/artificer/";
  final public static ModelAsset
    ALL_MOLD_MODELS[][] = CutoutModel.fromImageGrid(
      Tailing.class, IMG_DIR+"all_molds.png",
      4, 5, 1, 1, true
    ),
    ISOTOPE_TAILING_MODEL = CutoutModel.fromImage(
      Tailing.class, IMG_DIR+"slab.png", 1, 1
    ),
    //  TODO:  You need to include artifacts/ruins as well.
    METAL_ORE_TAILINGS[] = ALL_MOLD_MODELS[2],
    RAW_SLAG_TAILINGS [] = ALL_MOLD_MODELS[3];
  
  
  private void updateSprite() {
    
    final int stage = 1 + (int) ((fillLevel / MAX_FILL) * 3);
    final ModelAsset model;
    
    if      (wasteType == FUEL_RODS) model = ISOTOPE_TAILING_MODEL    ;
    else if (wasteType == METALS   ) model = METAL_ORE_TAILINGS[stage];
    else                             model = RAW_SLAG_TAILINGS [stage];
    
    final Sprite oldSprite = sprite();
    attachModel(model);
    
    if (oldSprite != null && oldSprite.model() != model) {
      world.ephemera.addGhost(this, 1, oldSprite, 0.5f);
    }
  }
}



//  TODO:  Try to find a place for these activities:

/*
public boolean actionReassemble(Actor actor, Venue site) {
  //  TODO:  Test and restore
  /*
  final Item sample = Item.withReference(SAMPLES, ARTIFACTS);
  final Structure s = site.structure();
  final float
    AAU = s.upgradeLevel(ExcavationSite.ARTIFACT_ASSEMBLY),
    SPU = s.upgradeLevel(ExcavationSite.SAFETY_PROTOCOL);
  
  float success = 1;
  if (actor.skills.test(ASSEMBLY, 10, 1)) success++;
  else success--;
  if (actor.skills.test(ANCIENT_LORE, 5, 1)) success++;
  else success--;

  site.stocks.removeItem(Item.withAmount(sample, 1.0f));
  if (success >= 0) {
    success *= 1 + (AAU / 2f);
    final Item result = Item.with(ARTIFACTS, null, 0.1f, success * 2);
    site.stocks.addItem(result);
  }
  if (site.stocks.amountOf(ARTIFACTS) >= 10) {
    site.stocks.removeItem(Item.withAmount(ARTIFACTS, 10));
    final Item match = site.stocks.matchFor(Item.withAmount(ARTIFACTS, 1));
    final float quality = (match.quality + AAU + 2) / 2f;
    if (Rand.num() < 0.1f * match.quality / (1 + SPU)) {
      final boolean hostile = Rand.num() < 0.9f / (1 + SPU);
      releaseArtilect(actor, hostile, quality);
    }
    else createArtifact(site, quality);
  }
  //*/
/*
  return true;
}


private void releaseArtilect(Actor actor, boolean hostile, float quality) {
  //  TODO:  TEST AND RESTORE THIS
  /*
  final int roll = (int) (Rand.index(5) + quality);
  final Artilect released = roll >= 5 ? new Tripod() : new Drone();
  
  final World world = actor.world();
  if (hostile) {
    released.assignBase(world.baseWithName(Base.KEY_ARTILECTS, true, true));
  }
  else {
    released.assignBase(actor.base());
    released.mind.assignMaster(actor);
  }
  released.enterWorldAt(actor.aboard(), world);
  released.goAboard(actor.aboard(), world);
  //*/
/*
}


private void createArtifact(Venue site, float quality) {
  final TradeType basis = Rand.yes() ?
    (TradeType) Rand.pickFrom(ALL_DEVICES) :
    (TradeType) Rand.pickFrom(ALL_OUTFITS);
  //
  //  TODO:  Deliver to artificer for sale or recycling!
  final Item found = Item.with(ARTIFACTS, basis, 1, quality);
  site.stocks.addItem(found);
}
//*/



