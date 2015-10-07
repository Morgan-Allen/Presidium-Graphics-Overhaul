/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.sfx;
import stratos.graphics.common.*;
import stratos.util.*;
import com.badlogic.gdx.graphics.*;



public class PlaneFX extends SFX {
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final Model model;
  public float timeScale = 1.0f;
  private float inceptTime = -1;
  
  
  protected PlaneFX(Model model) {
    super(PRIORITY_MIDDLE);
    this.model = model;
  }
  
  
  public Model model() {
    return model;
  }
  
  
  /**  Model definitions and factory methods-
    */
  public static class Model extends stratos.graphics.common.ModelAsset {
    
    private String imageName;
    private float initSize, spin, growth;
    private boolean tilted, vivid;
    
    private Box2D animUV[];
    private Box2D bounds = new Box2D();
    private float duration;
    
    private Texture texture;
    
    
    private Model(String modelName, Class sourceClass) {
      super(modelName, sourceClass);
    }
    
    
    protected State loadAsset() {
      texture = ImageAsset.getTexture(imageName);
      
      float
        w = texture.getWidth(),
        h = texture.getHeight(),
        m = Nums.max(w, h);
      bounds.set(0, 0, w / m, h / m);
      
      if (animUV.length > 1) {
        w = bounds.xdim() * animUV[0].xdim();
        h = bounds.ydim() * animUV[0].ydim();
        m = Nums.max(w, h);
        bounds.set(0, 0, w / m, h / m);
      }
      
      return state = State.LOADED;
    }
    
    
    protected State disposeAsset() {
      texture.dispose();
      return state = State.DISPOSED;
    }
    
    
    public Sprite makeSprite() { return new PlaneFX(this); }
  }
  
  
  public static Model imageModel(
    String modelName, Class modelClass,
    String image,
    float initSize, float spin, float growth, boolean tilted, boolean vivid
  ) {
    final Model m = new Model(modelName, modelClass);
    m.imageName = image;
    m.initSize  = initSize;
    m.spin      = spin;
    m.growth    = growth;
    m.tilted    = tilted;
    m.vivid     = vivid;
    m.animUV    = new Box2D[] { new Box2D().set(0, 0, 1, 1) };
    m.duration  = -1;
    return m;
  }
  
  
  public static Model animatedModel(
    String modelName, Class modelClass,
    String image,
    int gridX, int gridY, int numFrames, float duration, float scale
  ) {
    final Model m = imageModel(
      modelName, modelClass,
      image,
      scale, 0, 0, true, false
    );
    m.animUV   = new Box2D[numFrames];
    m.duration = duration;
    
    final float gW = 1f / gridX, gH = 1f / gridY;
    int frame = 0;
    for (int y = 0; y < gridY; y++) {
      for (int x = 0; x < gridX; x++) {
        if (frame >= numFrames) break;
        final Box2D b = m.animUV[frame++] = new Box2D();
        b.set(x * gW, y * gH, gW, gH);
      }
    }
    
    return m;
  }
  
  
  
  /**  Actual rendering-
    */
  private static Mat3D trans = new Mat3D();
  
  
  public void readyFor(Rendering rendering) {
    if (inceptTime == -1) reset();
    super.readyFor(rendering);
  }
  
  
  public float animProgress(boolean clamp) {
    float progress = Rendering.activeTime() - inceptTime;
    if (model.duration > 0) progress /= model.duration;
    if (timeScale      > 0) progress /= timeScale;
    if (clamp) return Nums.clamp(progress, 0, 1);
    else return progress;
  }
  
  
  public void reset() {
    inceptTime = Rendering.activeTime();
  }
  
  
  protected void renderInPass(SFXPass pass) {
    final boolean report = verbose && (model.spin > 0 || model.growth > 0);
    
    //
    //  Determine basic measurements-
    float progress = animProgress(false);
    if (model.duration > 0 && progress >= 1) return;
    final float radius = model.initSize + (model.growth * progress);
    final float r = radius * scale;
    final float newRot = (rotation + (model.spin * progress)) % 360;

    if (report) {
      I.say("\nRendering plane FX:");
      I.say("  Progress: "+progress);
      I.say("  Rotation: "+newRot  );
      I.say("  Radius:   "+radius  );
    }
    
    //
    //  Determine the correct animation frame-
    Box2D frameUV;
    if (model.duration > 0) {
      final int
        maxFrame = model.animUV.length,
        frame    = Nums.clamp((int) (progress * maxFrame), maxFrame);
      
      frameUV  = model.animUV[frame];
      if (report) I.say("  Animation frame: "+progress);
    }
    else frameUV = model.animUV[0];
    if (frameUV == null) return;
    
    //
    //  Setup and translate vertex positions-
    final Viewport view = pass.rendering.view;
    trans.setIdentity();
    trans.rotateZ((float) (newRot * Nums.PI / 180));
    final Vec3D screenPos = view.translateToScreen(new Vec3D().setTo(position));
    final float screenScale = view.screenScale();
    
    if (report) I.say("Vertices are: ");
    
    final float QV[] = SFXPass.QUAD_VERTS;
    int i = 0; for (Vec3D v : verts) {
      v.set(QV[i++], QV[i++], QV[i++]);
      v.x = (v.x - 0.5f) * r * 2 * model.bounds.xdim();
      v.y = (v.y - 0.5f) * r * 2 * model.bounds.ydim();
      v.z = 0;
      trans.trans(v);
      
      if (model.tilted) {
        v.x = (v.x * screenScale) + screenPos.x;
        v.y = screenPos.y - (v.y * screenScale);
        v.z = screenPos.z;
        view.translateFromScreen(v);
      }
      else v.add(position);
      if (report) I.say("  "+v);
    }
    
    //
    //  Finally, compile geometry and return.
    pass.compileQuad(model.texture, colour, model.vivid, verts, frameUV);
  }
}





