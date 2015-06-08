/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.cutout;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.start.PlayLoop;
import stratos.util.*;

import java.io.*;

import com.badlogic.gdx.math.*;



public class BuildingSprite extends Sprite {
  
  
  final static ModelAsset BUILDING_MODEL = new ClassModel(
    "building_model", BuildingSprite.class
  ) {
    public Sprite makeSprite() { return new BuildingSprite(); }
  };
  
  final public static float
    ITEM_SIZE = 0.33f;
  
  final static CutoutModel
    SCAFFOLD_MODEL = CutoutModel.fromImage(
      BuildingSprite.class,
      "media/Buildings/civilian/scaffold.png", 1.1f, 1
    ),
    FOUNDATION_FRINGE_MODEL = CutoutModel.fromImage(
      BuildingSprite.class,
      "media/Buildings/civilian/foundation_fringe.png", 1, 1
    ),
    CRATE_MODEL = CutoutModel.fromImage(
      BuildingSprite.class,
      "media/Items/crate.gif", ITEM_SIZE, ITEM_SIZE * 0.4f
    );
  
  final public static PlaneFX.Model
    BLAST_MODEL = new PlaneFX.Model(
      "blast_model", BuildingSprite.class,
      "media/SFX/blast_anim.gif", 5, 4, 25,
      (25 / 25f), 1.0f
    ),
    //  TODO:  try to find a way to derive these from the item-icons?
    
    POWER_MODEL = new PlaneFX.Model(
      "power_model", BuildingSprite.class,
      "media/Items/power.png" , 0.25f, 0, 0, true, true
    ),
    LIFE_SUPPORT_MODEL = new PlaneFX.Model(
      "power_model", BuildingSprite.class,
      "media/Items/life_S.png", 0.25f, 0, 0, true, true
    ),
    WATER_MODEL = new PlaneFX.Model(
      "power_model", BuildingSprite.class,
      "media/Items/water.png" , 0.25f, 0, 0, true, true
    );
  
  
  private Sprite baseSprite;
  private CutoutSprite buildSteps[] = null;
  
  private float condition;
  private int size, high;
  private boolean intact;
  
  
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
    BS.size       = size;
    BS.high       = high;
    BS.intact     = true;
    BS.condition  = 0;
    return BS;
  }
  
  
  public ModelAsset model() {
    return BUILDING_MODEL;
  }
  

  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    size      = in.readInt    ();
    high      = in.readInt    ();
    intact    = in.readBoolean();
    condition = in.readFloat  ();
    baseSprite = ModelAsset.loadSprite(in);
    int numS = in.readInt();
    if (numS > 0) buildSteps = new CutoutSprite[numS];
    for (int i = 0; i < numS; i++) {
      buildSteps[i] = (CutoutSprite) ModelAsset.loadSprite(in);
    }
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    out.writeInt    (size     );
    out.writeInt    (high     );
    out.writeBoolean(intact   );
    out.writeFloat  (condition);
    ModelAsset.saveSprite(baseSprite, out);
    if (buildSteps == null) out.writeInt(-1);
    else {
      out.writeInt(buildSteps.length);
      for (CutoutSprite s : buildSteps) ModelAsset.saveSprite(s, out);
    }
  }
  
  
  public Sprite baseSprite() {
    return baseSprite;
  }
  
  
  
  /**  Interface contract methods-
    */
  public void setAnimation(String animName, float progress, boolean loop) {
    //  TODO:  Could one make some use of this?...
  }
  
  
  public void readyFor(Rendering rendering) {
    
    if (intact) {
      baseSprite.matchTo(this);
      baseSprite.passType = this.passType;
      baseSprite.readyFor(rendering);
    }
    else if (buildSteps != null) {
      for (int index = 0; index < buildSteps.length; index++) {
        final CutoutSprite step = buildSteps[index];
        if (step == null) continue;
        provideOffset(step, index);
        step.readyFor(rendering);
      }
    }
    /*
    final PlaneFX displayed = statusFX.atIndex(statusDisplayIndex);
    if (displayed != null) {
      
      displayed.matchTo(this);
      displayed.position.z += high + 0.5f;
      displayed.readyFor(rendering);
      
      final float progress = displayed.animProgress(false) % 1;
      final float alpha = Nums.clamp(progress * 4 * (1 - progress), 0, 1);
      displayed.colour = Colour.transparency(alpha);
    }
    else statusDisplayIndex = statusFX.size() - 1;
    //*/
  }
  
  
  
  /**  Display-state updates-
    */
  final static int MAX_SIZE = 6, AL = MAX_SIZE + 1;
  final static Vector3     GROUND_COORDS[][]   = new Vector3    [AL][];
  final static CutoutModel FOUNDATION_MODELS[] = new CutoutModel[AL];
  static {
    for (int size = 1; size <= MAX_SIZE; size++) {
      FOUNDATION_MODELS[size] = CutoutModel.fromSplatImage(
        BuildingSprite.class,
        "media/Buildings/civilian/"+"foundation_"+size+"x"+size+".png", size
      );
      final List <Vector3> coords = new List <Vector3> () {
        protected float queuePriority(Vector3 r) { return r.z; }
      };
      final Vector3 temp = new Vector3();
      coords.clear();
      for (Coord c : Visit.grid(0, 0, size, size, 1)) {
        final Vector3 coord = new Vector3(c.x, c.y, 0);
        coord.z = Viewport.isometricRotation(coord, temp).z;
        coords.add(coord);
      }
      coords.queueSort();
      GROUND_COORDS[size] = coords.toArray(Vector3.class);
    }
  }
  
  
  private void provideOffset(CutoutSprite step, int index) {
    step.matchTo(this);
    if (step.faceIndex == 0) {
      final Vector3 coords[] = GROUND_COORDS[size];
      final Vector3 offset = coords[index % coords.length];
      step.position.x += offset.x - ((size - 1f) / 2);
      step.position.y += offset.y - ((size - 1f) / 2);
      step.position.z += Nums.max(0, (index / coords.length) - 1);
    }
  }
  
  
  private void updateStepForIndex(
    int stepIndex, int high, int progressIndex,
    CutoutModel ground, CutoutModel normal
  ) {
    final Vector3 coords[] = GROUND_COORDS[size];
    final Vector3 offset = coords[stepIndex % coords.length];
    final int
      xoff = (int) offset.x,
      yoff = (int) offset.y,
      zoff = stepIndex / coords.length;
    CutoutModel used;
    boolean singleton = false;
    
    //  Not reached yet...
    if (progressIndex < stepIndex || zoff > high) {
      used = null;
    }
    //  Ground-floor...
    else if (zoff == 0) {
      if (stepIndex <= progressIndex - size) used = ground;
      else { used = FOUNDATION_FRINGE_MODEL; singleton = true; }
    }
    //  Building-up...
    else {
      if (stepIndex <= progressIndex - size) used = normal;
      else { used = SCAFFOLD_MODEL; singleton = true; }
    }
    
    //  Check for redundancy-
    final CutoutSprite prior = buildSteps[stepIndex];
    if (prior == null && used == null) return;
    if (prior != null && prior.model() == used) return;
    
    //  And create a new sprite if different-
    if (used == null) {
      buildSteps[stepIndex] = null;
    }
    else if (singleton) {
      buildSteps[stepIndex] = (CutoutSprite) used.makeSprite();
    }
    else {
      buildSteps[stepIndex] = used.topSprite(xoff, yoff);
    }
  }
  
  
  public void updateCondition(
    float newCondition, boolean normalState, boolean burning
  ) {
    final Vector3 coords[] = GROUND_COORDS[Nums.clamp(size, AL)];
    final int totalSteps = size + (coords.length * (high + 1));
    final int oldIndex = Nums.round(condition * totalSteps, 1, true);
    
    this.condition = newCondition;
    this.intact    = normalState ;
    if (intact) { buildSteps = null; return; }
    else if (buildSteps == null) buildSteps = new CutoutSprite[totalSteps];
    
    final int newIndex = Nums.round(condition * totalSteps, 1, true);
    if (oldIndex == newIndex) return;
    
    final CutoutModel ground = FOUNDATION_MODELS[size];
    final CutoutModel normal = basisModel();
    for (int i = totalSteps; i-- > 0;) {
      updateStepForIndex(i, high, newIndex, ground, normal);
    }
  }
  
  
  private CutoutModel basisModel() {
    Sprite basis = baseSprite;
    while (true) {
      if (basis instanceof CutoutSprite) {
        return (CutoutModel) basis.model();
      }
      else if (basis instanceof GroupSprite) {
        final GroupSprite GS = (GroupSprite) basis;
        basis = GS.childOfHeight(high);
        if (basis == null) basis = GS.atIndex(0);
      }
      else return null;
    }
  }
  
  
  
  /**  TODO:  Get rid of these- they don't seem appropriate for buildings.
    *  (Might be useful for actors/agents, though?)
    */
  public void toggleFX(ModelAsset model, boolean on) {
  }
  

  public void clearFX() {
    //statusFX.clear();
    //allStacks.clearAllAttachments();
  }
  
  
  public void updateItemDisplay(
    CutoutModel itemModel, float amount, float xoff, float yoff, float zoff
  ) {
    
  }
}


    /*
    CutoutModel basisModel = basisModel();
    if (basisModel == null) basisModel = scaffModel;
    
    this.condition = newCondition;
    this.intact    = normalState ;
    if (intact) { scaffolds.clear(); return; }
    
    final int index = Nums.round(condition * size * size, 1, true);
    while (index > scaffolds.size()) {
      final int
        nextI = (scaffolds.size() + 1) - 1,
        inX   = nextI / size,
        inY   = nextI % size;
      
      final CutoutSprite s = basisModel.facingSprite(
        inX, inY, (int) basisModel.high
      );
      if (s != null) scaffolds.add(s); else break;
    }
    while (index < scaffolds.size()) {
      scaffolds.removeLast();
    }
    //*/


/*
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

    final int SI = Nums.clamp(size, SCAFF_MODELS.length);
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
    size      = in.readInt    ();
    high      = in.readInt    ();
    intact    = in.readBoolean();
    condition = in.readFloat  ();
    baseSprite   =                ModelAsset.loadSprite(in);
    scaffoldBase = (CutoutSprite) ModelAsset.loadSprite(in);
    scaffolding  = (GroupSprite ) ModelAsset.loadSprite(in);
    allStacks    = (GroupSprite ) ModelAsset.loadSprite(in);
    
    allStacks.clearAllAttachments();
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    out.writeInt    (size     );
    out.writeInt    (high     );
    out.writeBoolean(intact   );
    out.writeFloat  (condition);
    ModelAsset.saveSprite(baseSprite  , out);
    ModelAsset.saveSprite(scaffoldBase, out);
    ModelAsset.saveSprite(scaffolding , out);
    ModelAsset.saveSprite(allStacks   , out);
  }
  
  
  //  TODO:  Make use of this?
  public void setAnimation(String animName, float progress, boolean loop) {}
  
  
  public void readyFor(Rendering rendering) {
    
    if (intact) {
      baseSprite.matchTo(this);
      baseSprite.passType = this.passType;
      baseSprite.readyFor(rendering);
    }
    else {
      scaffoldBase.matchTo(this);
      scaffolding.matchTo(this);
      scaffoldBase.scale = size;
      scaffoldBase.passType = Sprite.PASS_SPLAT;
      scaffoldBase.readyFor(rendering);
      scaffolding.readyFor(rendering);
    }
    allStacks.matchTo(this);
    allStacks.readyFor(rendering);
    
    final PlaneFX displayed = statusFX.atIndex(statusDisplayIndex);
    if (displayed != null) {
      
      displayed.matchTo(this);
      displayed.position.z += high + 0.5f;
      displayed.readyFor(rendering);
      
      final float progress = displayed.animProgress(false) % 1;
      final float alpha = Nums.clamp(progress * 4 * (1 - progress), 0, 1);
      displayed.colour = Colour.transparency(alpha);
    }
    else statusDisplayIndex = statusFX.size() - 1;
  }
  
  
  public Sprite baseSprite()  { return baseSprite ; }
  public Sprite scaffolding() { return scaffolding; }
  
  
  
  /**  The following are 'dummy' methods that need to be re-implemented once
    *  the simulation logic is back in action.
    */
  /*
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
    statusFX.clear();
    allStacks.clearAllAttachments();
  }
  
  
  public void updateItemDisplay(
    CutoutModel itemModel, float amount, float xoff, float yoff, float zoff
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
      if (match != null) allStacks.detach(match);
      return;
    }
    if (match == null) {
      match = new ItemStack(itemModel);
      allStacks.attach(match, xoff, yoff, zoff);
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
      if (scaffolding != null && oldStage == newStage) return;
      scaffolding = scaffoldFor(size, high, newCondition);
    }
    else {
      scaffolding = null;
    }
  }
  //*/
  
  
  
  /**  Producing and updating scaffold sprites-
    */
  /*
  private int maxStages() {
    int max = 0;
    for (int z = 0; z < high; z++)
      for (int x = 1; x < (size - z); x++)
        for (int y = 1 + z; y < size; y++) max++;
    return max;
  }
  
  
  private int scaffoldStage(int size, int high, float condition, int maxStage) {
    final int newStage = (int) (condition * (maxStage + 1));
    return newStage;
  }
  
  
  private GroupSprite scaffoldFor(int size, int high, float condition) {
    condition = Nums.clamp(condition, 0, 1);
    final int stage = scaffoldStage(size, high, condition, maxStages());
    //
    //  Otherwise, put together a composite sprite where the number of mini-
    //  scaffolds provides a visual indicator of progress.
    final GroupSprite sprite = new GroupSprite();
    sprite.setSortMode(GroupSprite.SORT_BY_ADDITION);
    if (size == 1) return sprite;
    
    final float xoff = (size / 2f), yoff = (size / 2f);
    int numS = 0;
    //
    //  Iterate over the entire coordinate space as required-
    loop: for (int z = 0; z < high; z++) {
      final float
        l = z * 1f / high,
        h = z - (l * l),
        i = z / 2f;
      
      for (int x = size - z; x-- > 1;) {
        for (int y = 1 + z; y < size; y++) {
          if (++numS > stage) break loop;
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
  //*/
//}







