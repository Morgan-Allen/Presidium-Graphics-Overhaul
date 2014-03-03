

package src.graphics.cutout;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import src.graphics.common.*;
import src.util.Visit;



public class BuildingSprite extends Sprite {
  
  
  final static int
    STAGE_INSTALL = 0,
    STAGE_INTACT  = 1,
    STAGE_DAMAGED = 2,
    STAGE_SALVAGE = 3;
  
  final private static Class <BuildingSprite> C = BuildingSprite.class ;
  final static ModelAsset BUILDING_MODEL = new ModelAsset("building_model", C) {
    public boolean isLoaded() { return true; }
    protected void loadAsset() {}
    protected void disposeAsset() {}
    public Sprite makeSprite() { return new BuildingSprite() ; }
  } ;

  final static String DIR = "media/Buildings/artificer/" ;
  final public static ModelAsset SCAFF_MODELS[] = CutoutModel.fromImages(
    DIR, BuildingSprite.class, 1, 1,
    "scaff_0.png",
    "scaff_1.png",
    "scaff_2.png",
    "scaff_3.png",
    "scaff_4.png",
    "scaff_5.png",
    "scaff_6.png"
  );
  
  
  private CutoutSprite baseSprite;
  private Sprite scaffolding;
  private int size, high;
  
  
  public static BuildingSprite fromBase(
    CutoutSprite sprite, int size, int high
  ) {
    final BuildingSprite BS = new BuildingSprite();
    BS.baseSprite = sprite;
    final int SI = Visit.clamp(size - 1, SCAFF_MODELS.length);
    BS.scaffolding = SCAFF_MODELS[SI].makeSprite();
    BS.size = size;
    BS.high = high;
    return BS;
  }
  
  
  
  public ModelAsset model() {
    return BUILDING_MODEL;
  }
  

  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    baseSprite = (CutoutSprite) ModelAsset.loadSprite(in);
    scaffolding = ModelAsset.loadSprite(in);
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    ModelAsset.saveSprite(baseSprite, out);
    ModelAsset.saveSprite(scaffolding, out);
  }
  
  
  
  public void setAnimation(String animName, float progress) {
  }
  
  
  public void registerFor(Rendering rendering) {
    baseSprite.matchTo(this);
    scaffolding.matchTo(this);
    scaffolding.scale = size;
    rendering.cutoutsPass.register(baseSprite);
    //  TODO:  RESTORE THIS
    //else rendering.cutoutsPass.register(scaffolding);
  }
  
  
  //public Sprite baseSprite()  { return baseSprite  ; }
  //public Sprite scaffolding() { return scaffolding ; }
  
  
  
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
    
  }
  
  public void updateItemDisplay(
    ModelAsset itemModel, float amount, float xoff, float yoff
  ) {
  }
  
  public void clearFX() {
  }
  
  public void updateCondition(
    float newCondition, boolean normalState, boolean burning
  ) {
  }
}





