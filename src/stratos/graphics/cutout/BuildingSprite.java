/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.cutout;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.start.Assets;
import stratos.util.*;
import java.io.*;
import com.badlogic.gdx.math.*;



public class BuildingSprite extends Sprite implements TileConstants {
  
  
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
      "media/Buildings/civilian/scaffold.png", 1, 1
    ),
    FOUNDATION_FRINGE_MODEL = CutoutModel.fromImage(
      BuildingSprite.class,
      "media/Buildings/civilian/foundation_fringe.png", 1, 0
    ),
    CRATE_MODEL = CutoutModel.fromImage(
      BuildingSprite.class,
      "media/Items/crate.gif", ITEM_SIZE, ITEM_SIZE * 0.4f
    );
  
  final public static PlaneFX.Model
    BLAST_MODEL = PlaneFX.animatedModel(
      "blast_model", BuildingSprite.class,
      "media/SFX/blast_anim.gif", 5, 4, 25,
      (25 / 25f), 1.0f
    ),
    //  TODO:  try to find a way to derive these from the item-icons?
    
    POWER_MODEL = PlaneFX.imageModel(
      "power_model", BuildingSprite.class,
      "media/Items/power.png" , 0.25f, 0, 0, true, true
    ),
    ATMO_MODEL  = PlaneFX.imageModel(
      "atmo_model", BuildingSprite.class,
      "media/Items/atmo.png", 0.25f, 0, 0, true, true
    ),
    WATER_MODEL = PlaneFX.imageModel(
      "water_model", BuildingSprite.class,
      "media/Items/water.png" , 0.25f, 0, 0, true, true
    );
  
  
  private Sprite baseSprite;
  private CutoutSprite buildSteps[] = null;
  
  private float condition;
  private int size, high;
  private boolean intact;
  
  public boolean flagChange = false;
  
  
  
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
    BS.size       = Nums.min(size, MAX_SIZE);
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
  
  
  public int size() {
    return size;
  }
  
  
  public int high() {
    return high;
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
    flagChange = false;
  }
  
  
  
  /**  Display-state updates-
    */
  final static int MAX_SIZE = 8, AL = MAX_SIZE + 1;
  final static Vector3     GROUND_COORDS[][]   = new Vector3    [AL][];
  final static int         ALL_INDICES[][][]   = new int        [AL][][];
  final static CutoutModel FOUNDATION_MODELS[] = new CutoutModel[AL];
  
  static {
    String lastOK = null; for (int size = 1; size <= MAX_SIZE; size++) {
      String modelName = "media/Buildings/civilian/";
      modelName += "foundation_"+size+"x"+size+".png";
      if (! Assets.exists(modelName)) modelName = lastOK;
      else lastOK = modelName;
      
      if (modelName == null) {
        I.complain("\nNO SPLAT MODEL FOR SIZE "+size+"!");
      }
      else FOUNDATION_MODELS[size] = CutoutModel.fromSplatImage(
        BuildingSprite.class, modelName, size
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
      ALL_INDICES  [size] = new int[size][size];
      
      int index = 0; for (Vector3 v : coords) {
        ALL_INDICES[size][(int) v.x][(int) v.y] = index++;
      }
    }
  }
  
  
  private void provideOffset(CutoutSprite step, int index) {
    step.matchTo(this);
    if (step.model().size == 1) {
      final Vector3 coords[] = GROUND_COORDS[size];
      final Vector3 offset = coords[index % coords.length];
      step.position.x += offset.x - ((size - 1f) / 2);
      step.position.y += offset.y - ((size - 1f) / 2);
      step.position.z += Nums.max(0, (index / coords.length) - 1);
    }
  }
  
  
  private void updateStep(CutoutModel used, int x, int y, int z, int index) {
    //
    //  Check for redundancy-
    final CutoutSprite prior = buildSteps[index];
    if (prior == null && used == null) return;
    if (prior != null && prior.model() == used) return;
    //
    //  And create a new sprite if different-
    if (used == null) {
      buildSteps[index] = null;
    }
    else if (used.size == 1) {
      buildSteps[index] = used.facingSprite(0, 0, 0);
    }
    else {
      buildSteps[index] = used.facingSprite(x, y, z - 1);
    }
  }
  
  
  public void toggleFoundation(boolean map[][]) {
    if (buildSteps == null) return;
    
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      final int index = ALL_INDICES[size][c.x][c.y];
      CutoutModel used = null;
      
      if (map[c.x][c.y]) {
        int numNear = 0;
        for (int t : T_ADJACENT) try {
          final int nX = c.x + T_X[t], nY = c.y + T_Y[t];
          if (map[nX][nY]) numNear++;
        }
        catch (ArrayIndexOutOfBoundsException e) { numNear++; }
        
        final CutoutModel ground = FOUNDATION_MODELS[size];
        used = numNear >= 4 ? ground : FOUNDATION_FRINGE_MODEL;
      }
      
      updateStep(used, c.x, c.y, 0, index);
    }
  }
  
  
  private void updateStepForIndex(
    int stepIndex, int progressIndex, int maxIndex,
    CutoutModel ground, CutoutModel normal
  ) {
    final Vector3 groundCoords[] = GROUND_COORDS[size];
    final int groundSize = groundCoords.length;
    if (stepIndex < groundSize) return;
    
    final int groundIndex = stepIndex % groundSize;
    final Vector3 offset = groundCoords[groundIndex];
    final int
      xoff = (int) offset.x,
      yoff = (int) offset.y,
      zoff = stepIndex / groundSize;
    
    final float margin = size * 1f / groundSize;
    float localProgress = 0;
    final CutoutModel used;
    localProgress += progressIndex * (1 + (margin * 2)) / maxIndex;
    localProgress -= groundIndex * 1f / groundSize;
    localProgress -= margin;
    
    if (localProgress <= 0) {
      float highBonus = 1 - (zoff * 1f / high);
      localProgress += (highBonus - 0.5f) * margin * 2;
    }
    if (localProgress <= 0) {
      used = null;
    }
    else {
      if (localProgress <= margin) used = SCAFFOLD_MODEL;
      else used = normal;
    }
    
    updateStep(used, xoff, yoff, zoff, stepIndex);
  }
  
  
  public void updateCondition(
    float newCondition, boolean normalState, boolean burning
  ) {
    final Vector3 coords[] = GROUND_COORDS[size];
    final int totalSteps = coords.length * (high + 1);
    
    final int oldIndex = Nums.round(condition * totalSteps, 1, true);
    final boolean wasIntact = intact;
    
    this.condition = newCondition;
    this.intact    = normalState ;
    if (intact) { buildSteps = null; return; }
    else if (buildSteps == null) buildSteps = new CutoutSprite[totalSteps];
    
    final int newIndex = Nums.round(condition * totalSteps, 1, true);
    if (oldIndex == newIndex && wasIntact == intact) return;
    
    final CutoutModel ground = FOUNDATION_MODELS[size];
    final CutoutModel normal = basisModel();
    for (int i = totalSteps; i-- > 0;) {
      updateStepForIndex(i, newIndex, totalSteps, ground, normal);
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


/*
  
  private GroupSprite allStacks;
  private List <PlaneFX> statusFX = new List <PlaneFX> ();
  private int statusDisplayIndex = -1;

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
  
  
  public void readyFor(Rendering rendering) {
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






