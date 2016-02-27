/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import static stratos.game.craft.Economy.*;

import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.ClaimDivision;
import stratos.game.wild.*;
import stratos.game.wild.Species.Type;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Crop extends Flora {
  
  
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static CutoutModel
    COVERING_LEFT = CutoutModel.fromImage(
      Crop.class, IMG_DIR+"covering_left.png", 1, 1
    ),
    COVERING_RIGHT = CutoutModel.fromImage(
      Crop.class, IMG_DIR+"covering_right.png", 1, 1
    ),
    CROP_MODELS[][] = CutoutModel.fromImageGrid(
      Crop.class, IMG_DIR+"all_crops.png",
      4, 4, 0.5f, 0.5f, false
    ),
    GRUB_BOX_MODEL = CutoutModel.fromImage(
      Crop.class, IMG_DIR+"grub_box.png", 0.5f, 0.5f
    );
  
  final public static Species
    ONI_RICE = new Species(
      Flora.class, "Oni Rice",
      "Oni Rice grows with unrivalled speed if kept well-watered.",
      null, CROP_MODELS[0],
      Type.FLORA, 0.8f, 0.75f, true, 4, CARBS
    ) {},
    DURWHEAT = new Species(
      Flora.class, "Durwheat",
      "Durwheat is an excellent calory source suited to dryer soils.",
      null, CROP_MODELS[1],
      Type.FLORA, 0.65f, 0.35f, true, 4, CARBS
    ) {},
    TUBER_LILY = new Species(
      Flora.class, "Tuber Lily",
      "Tuber Lilies are a sweet, savory crop vulnerable to drought.",
      null, CROP_MODELS[2],
      Type.FLORA, 0.5f, 0.85f, true, 4, GREENS
    ) {},
    BROADFRUITS = new Species(
      Flora.class, "Broadfruits",
      "Broadfruits survive hard times by fattening their fleshy leaves.",
      null, CROP_MODELS[3],
      Type.FLORA, 0.35f, 0.2f, true, 4, GREENS
    ) {},
    HIVE_GRUBS = new Species(
      Flora.class, "Hive Grubs",
      "Hive Grubs help to aerate soil and provide valuable protein.",
      null, new ModelAsset[] { GRUB_BOX_MODEL },
      Type.FLORA, 0.2f, 0.5f, true, 4, PROTEIN
    ) {};
  
  
  final public HarvestVenue parent;
  private boolean covered;
  
  
  public Crop(HarvestVenue parent, Species species) {
    super(species);
    this.parent = parent;
  }
  
  
  public Crop(Session s) throws Exception {
    super(s);
    parent  = (HarvestVenue) s.loadObject();
    covered = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(parent   );
    s.saveBool  (covered  );
  }
  
  
  
  
  /**  Specific overrides-
    */
  final public static Species ALL_VARIETIES[] = {
    ONI_RICE,
    DURWHEAT,
    TUBER_LILY,
    BROADFRUITS,
    HIVE_GRUBS
  };
  
  
  public int pathType() {
    if (covered) return Tile.PATH_BLOCKS;
    return Tile.PATH_HINDERS;
  }
  
  
  public int owningTier() {
    if (parent != null && parent.structure.intact() && parent.inWorld()) {
      return Owner.TIER_PRIVATE;
    }
    return Owner.TIER_TERRAIN;
  }
  
  
  public boolean canPlace() {
    final Tile o = origin();
    if (o != null && o.reserves() == parent) return true;
    return false;
  }
  
  
  public void seedWith(Species s, float quality) {
    this.covered = parent.claimUse(origin()) != ClaimDivision.USE_NORMAL;
    super.seedWith(s, quality);
  }
  
  
  public float dailyGrowthEstimate(Tile tile) {
    float estimate = super.dailyGrowthEstimate(tile);
    if (parent != null) estimate *= parent.harvestMultiple(this, species());
    return estimate;
  }
  
  
  public void onGrowth(Tile tile) {
    //
    //  Crops disappear once their parent nursery is salvaged or destroyed, and
    //  can't grow if they're not seeded.
    if (
      parent == null || (! parent.inWorld()) ||
      parent.claimUse(tile) == ClaimDivision.USE_NONE
    ) {
      setAsDestroyed(false);
    }
    else super.onGrowth(tile);
  }
  
  
  public void exitWorld() {
    ///I.say("CROP EXITING WORLD!");
    super.exitWorld();
  }
  
  
  public Item[] materials() {
    return species().nutrients(growStage());
  }
  
  
  
  /**  Rendering and interface-
    */
  protected void updateSprite() {
    
    //  TODO:  You need to sort out facing!  Hah!
    
    if (covered) {
      if (origin().x == parent.origin().x) {
        final GroupSprite GS = new GroupSprite();
        GS.attach(BotanicalStation.NURSERY_MODEL, 0.5f, -0.5f, 0);
        attachSprite(GS);
        return;
      }
      if (origin().x == parent.origin().x + 1) {
        return;
      }
    }
    if (covered) {
      int facing = parent.claimDivision().useAlignment(origin());
      boolean across = facing == ClaimDivision.ALIGN_WE;
      if (across) attachModel(COVERING_RIGHT);
      else        attachModel(COVERING_LEFT );
      return;
    }
    
    final GroupSprite old = (GroupSprite) sprite();
    final int stage = Nums.ceil(growStage() * MAX_GROWTH * 1f / MIN_HARVEST);
    final ModelAsset model = modelForStage(stage);
    if (old != null && old.atIndex(0).model() == model) return;
    
    final GroupSprite GS = new GroupSprite();
    GS.attach(model, -0.25f, -0.25f, 0);
    GS.attach(model,  0.25f, -0.25f, 0);
    GS.attach(model, -0.25f,  0.25f, 0);
    GS.attach(model,  0.25f,  0.25f, 0);
    attachSprite(GS);
    
    if (old != null) world.ephemera.addGhost(this, 1, old, 2.0f, 1);
  }
  
  
  public Composite portrait(HUD UI) {
    //  TODO:  FILL THIS IN!
    return null;
  }
  
  
  public String helpInfo() {
    final Tile o = origin();
    float moisture = o == null ? -1: o.habitat().moisture();
    
    String growth = STAGE_NAMES[Nums.clamp(growStage() + 1, 5)];
    float percent = (int) (this.growStage() * 100f / MAX_GROWTH);
    String health = HEALTH_NAMES[(int) (health() * 5f / MAX_HEALTH)];
    if (blighted()) health+=" (Infested)";
    
    return
      "Crops take a few days to mature, depending on climate, seed stock and "+
      "planting skill."+
      "\n\n  Moisture: "+moisture+
      "\n  Health: "+health+
      "\n  Growth: "+growth+" ("+percent+"%)";
  }
}







