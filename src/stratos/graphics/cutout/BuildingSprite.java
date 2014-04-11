

package stratos.graphics.cutout;
import java.io.*;

import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.sfx.PlaneFX.Model;
import stratos.util.*;



public class BuildingSprite extends Sprite {
  
  
  final private static Class <BuildingSprite> C = BuildingSprite.class ;
  final static ModelAsset BUILDING_MODEL = new ModelAsset("building_model", C) {
    public boolean isLoaded() { return true; }
    protected void loadAsset() {}
    protected void disposeAsset() {}
    public Sprite makeSprite() { return new BuildingSprite() ; }
  };
  final public static float
    ITEM_SIZE = 0.33f;
  
  final public static ModelAsset
    SCAFF_MODELS[] = CutoutModel.fromImages(
      "media/Buildings/civilian/",
      BuildingSprite.class, 1, 1, false,
      "scaff_0.png",
      "scaff_1.png",
      "scaff_2.png",
      "scaff_3.png",
      "scaff_4.png",
      "scaff_5.png",
      "scaff_6.png"
    ),
    CRATE_MODEL = CutoutModel.fromImage(
      BuildingSprite.class, "media/Items/crate.gif",
      ITEM_SIZE, ITEM_SIZE * 0.4f
    );
  final public static PlaneFX.Model
    BLAST_MODEL = new PlaneFX.Model(
      "blast_model", BuildingSprite.class,
      "media/SFX/blast_anim.gif", 5, 4, 25, (25 / 25f)
    ),
    POWER_MODEL = new PlaneFX.Model(
      "power_model", BuildingSprite.class,
      "media/SFX/power.png", 0.5f, 0, 0, true, true
    ),
    LIFE_SUPPORT_MODEL = new PlaneFX.Model(
      "power_model", BuildingSprite.class,
      "media/SFX/life_S.png", 0.5f, 0, 0, true, true
    ),
    WATER_MODEL = new PlaneFX.Model(
      "power_model", BuildingSprite.class,
      "media/SFX/water.png", 0.5f, 0, 0, true, true
    );
  
  
  private Sprite baseSprite;
  private CutoutSprite scaffoldBase;
  private GroupSprite scaffolding;
  
  private GroupSprite allStacks;
  private List <PlaneFX> statusFX = new List <PlaneFX> ();
  private int statusDisplayIndex = -1;
  
  private int size, high;
  boolean intact;
  float condition;
  
  

  public static BuildingSprite fromBase(
    ModelAsset model, int size, int high
  ) {
    return fromBase((CutoutSprite) model.makeSprite(), size, high);
  }
  
  
  public static BuildingSprite fromBase(
    Sprite sprite, int size, int high
  ) {
    final BuildingSprite BS = new BuildingSprite();
    BS.baseSprite = sprite;

    final int SI = Visit.clamp(size, SCAFF_MODELS.length);
    BS.scaffoldBase = (CutoutSprite) SCAFF_MODELS[SI].makeSprite();
    
    BS.scaffolding = BS.scaffoldFor(size, high, 0);
    BS.allStacks = new GroupSprite();
    BS.size = size;
    BS.high = high;
    BS.intact = true;
    BS.condition = 0;
    return BS;
  }
  
  
  
  public ModelAsset model() {
    return BUILDING_MODEL;
  }
  

  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    size = in.readInt();
    high = in.readInt();
    intact = in.readBoolean();
    condition = in.readFloat();
    baseSprite = ModelAsset.loadSprite(in);
    scaffoldBase = (CutoutSprite) ModelAsset.loadSprite(in);
    scaffolding = (GroupSprite) ModelAsset.loadSprite(in);
    allStacks = (GroupSprite) ModelAsset.loadSprite(in);
    
    allStacks.clearAllAttachments();
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    out.writeInt(size);
    out.writeInt(high);
    out.writeBoolean(intact);
    out.writeFloat(condition);
    ModelAsset.saveSprite(baseSprite, out);
    ModelAsset.saveSprite(scaffoldBase, out);
    ModelAsset.saveSprite(scaffolding, out);
    ModelAsset.saveSprite(allStacks, out);
  }
  
  
  //  TODO:  Make use of this?
  public void setAnimation(String animName, float progress) {}
  
  
  public void readyFor(Rendering rendering) {
    baseSprite.matchTo(this);
    scaffoldBase.matchTo(this);
    scaffolding.matchTo(this);
    allStacks.matchTo(this);
    
    if (intact) {
      baseSprite.passType = this.passType;
      baseSprite.readyFor(rendering);
    }
    else {
      scaffoldBase.scale = size;
      scaffoldBase.passType = Sprite.PASS_SPLAT;
      scaffoldBase.readyFor(rendering);
      scaffolding.readyFor(rendering);
    }
    allStacks.readyFor(rendering);
    
    final PlaneFX displayed = statusFX.atIndex(statusDisplayIndex) ;
    if (displayed != null) {
      
      displayed.matchTo(this);
      displayed.position.z += high + 0.5f;
      displayed.readyFor(rendering);
      
      final float progress = displayed.animProgress();
      final float alpha = Visit.clamp(progress * 4 * (1 - progress), 0, 1);
      displayed.colour = Colour.transparency(alpha);
      
      if (progress >= 1) {
        statusDisplayIndex = (statusDisplayIndex + 1) % statusFX.size();
        statusFX.atIndex(statusDisplayIndex).reset();
      }
    }
    else statusDisplayIndex = statusFX.size() - 1 ;
  }
  
  
  public Sprite baseSprite()  { return baseSprite  ; }
  public Sprite scaffolding() { return scaffolding ; }
  
  
  
  /**  The following are 'dummy' methods that need to be re-implemented once
    *  the simulation logic is back in action.
    */
  //  TODO:  this method in particular needs to be reconsidered, since *all*
  //  cutoutsprites might well have (optional) lighting attached.
  public void toggleLighting(
    ModelAsset lightsModel, boolean lit, float xoff, float yoff, float zoff
  ) {
    
  }
  

  public void toggleFX(ModelAsset model, boolean on) {
    if (on) {
      for (PlaneFX FX : statusFX) if (FX.model() == model) return;
      final PlaneFX FX = (PlaneFX) model.makeSprite();
      statusFX.add(FX);
      if (statusDisplayIndex < 0) statusDisplayIndex = 0;
    }
    else {
      int index = 0;
      for (PlaneFX FX : statusFX)
        if (FX.model() == model) {
          statusFX.remove(FX);
          if (statusDisplayIndex >= index) statusDisplayIndex--;
          return;
        }
        else index++;
    }
  }
  
  
  public void clearFX() {
    statusFX.clear() ;
    allStacks.clearAllAttachments() ;
  }
  
  
  public void updateItemDisplay(
    CutoutModel itemModel, float amount, float xoff, float yoff
  ) {
    ItemStack match = null;
    
    for (Sprite s : allStacks.modules) {
      if (s instanceof ItemStack) {
        final ItemStack IS = (ItemStack) s;
        if (IS.itemModel != itemModel) continue;
        match = (ItemStack) s;
        break;
      }
    }
    if (amount < 1) {
      if (match != null) allStacks.detach(match) ;
      return ;
    }
    if (match == null) {
      match = new ItemStack(itemModel);
      allStacks.attach(match, xoff, yoff, 0);
    }
    match.updateAmount((int) amount);
  }
  
  
  public void updateCondition(
    float newCondition, boolean normalState, boolean burning
  ) {
    intact = normalState;
    
    final float oldCondition = this.condition;
    condition = newCondition;
    
    if (! intact) {
      final int
        maxStage = maxStages(),
        oldStage = scaffoldStage(size, high, oldCondition, maxStage),
        newStage = scaffoldStage(size, high, newCondition, maxStage);
      if (oldStage == newStage) return ;
      scaffolding = scaffoldFor(size, high, newCondition) ;
    }
  }
  
  
  
  /**  Producing and updating scaffold sprites-
    */
  private int maxStages() {
    int max = 0 ;
    for (int z = 0 ; z < high ; z++)
      for (int x = 1 ; x < (size - z) ; x++)
        for (int y = 1 + z ; y < size ; y++) max++ ;
    return max ;
  }
  
  
  private int scaffoldStage(int size, int high, float condition, int maxStage) {
    final int newStage = (int) (condition * (maxStage + 1)) ;
    return newStage ;
  }
  
  
  private GroupSprite scaffoldFor(int size, int high, float condition) {
    condition = Visit.clamp(condition, 0, 1);
    final int stage = scaffoldStage(size, high, condition, maxStages());
    //
    //  Otherwise, put together a composite sprite where the number of mini-
    //  scaffolds provides a visual indicator of progress.
    final GroupSprite sprite = new GroupSprite();
    if (size == 1) return sprite;
    
    final float xoff = (size / 2f), yoff = (size / 2f);
    int numS = 0;
    //
    //  Iterate over the entire coordinate space as required-
    loop: for (int z = 0 ; z < high ; z++) {
      final float l = z * 1f / high, h = z - (l * l), i = z / 2f ;
      for (int x = 1 ; x < (size - z) ; x++) {
        for (int y = 1 + z ; y < size ; y++) {
          if (++numS > stage) break loop ;
          sprite.attach(
            SCAFF_MODELS[0],
            x + i - xoff,
            y - (yoff + i),
            h
          );
        }
      }
    }
    return sprite;
  }
}







