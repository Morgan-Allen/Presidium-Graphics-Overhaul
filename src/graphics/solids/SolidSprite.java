


package src.graphics.solids;
import src.graphics.common.*;
import src.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.math.*;






//TODO:  Get rid of the LibGDX code for this entirely and just go directly
//       to the Mesh and a custom bone-shader.  All the code is there.


public class SolidSprite extends Sprite {
  
  
  final ModelAsset model;
  final Model gdxModel;
  
  //  Note:  We don't instantiate this immediately because sprites might be
  //  initialised on a background loading thread, whereas anything directly
  //  derived from libgdx typically needs to be created on the render() thread.
  private ModelInstance gdxSprite = null;
  private AnimationController animControl = null;
  
  private float lastTime = -1;
  /*
  //private Stack <Overlay> overlays = null;
  //  TODO:  Not used at the moment.  Implement that...
  private class Overlay {
    Texture tex ;
    String skin ;
  }
  //*/
  
  
  protected SolidSprite(ModelAsset model, Model gdxModel) {
    this.model = model;
    this.gdxModel = gdxModel;
  }
  
  
  public ModelAsset model() {
    return model;
  }
  
  
  private void initGDX() {
    if (gdxSprite == null) {
      gdxSprite = new ModelInstance(gdxModel);
      animControl = new AnimationController(gdxSprite);
    }
  }
  
  
  public void setAnimation(String animName, float progress) {
    initGDX();
    if (
      animControl.current == null ||
      ! animControl.current.animation.id.equals(animName)
    ) {
      if (gdxSprite.model.getAnimation(animName) == null) return;
      animControl.animate(animName, -1, null, -1);
      lastTime = -1;
    }
    if (lastTime == -1) lastTime = 0;
    if (progress < lastTime) lastTime--;
    float delta = progress - lastTime;
    delta *= animControl.current.animation.duration;
    animControl.update(delta);
    ///I.say("Update delta is: "+delta);
    lastTime = progress;
  }
  
  
  private static Vector3 temp = new Vector3();
  
  public void registerFor(Rendering rendering) {
    initGDX();
    rendering.view.worldToGL(position, temp);
    gdxSprite.transform.setToTranslation(temp);
    
    final float radians = (float) Math.toRadians(90 - rotation);
    gdxSprite.transform.rot(Vector3.Y, radians);
    rendering.solidsPass.register(this);
  }
  
  
  public void update() {
  }
  
  
  protected ModelInstance gdxSprite() {
    return gdxSprite;
  }
  
  
  
  /**  'Dummy' methods that need proper re-implementation later.
    */
  public void applyOverlay(Texture overlay, String skin, boolean on) {
    //  TODO:  IMPLEMENT
    /*
    final Overlay added = new Overlay();
    added.tex = overlay;
    added.skin = skin;  //TODO:  Identify group instead?  I'm not really sure
                        //  how to render this, for that matter.
    if (overlays == null) {
      overlays = new Stack <Overlay> ();
    }
    overlays.add(added);
    //*/
  }
  
  
  public Vec3D attachPoint(String label) {
    return position;
  }
  
  
  public void toggleGroup(String groupName, boolean on) {
    //  TODO:  IMPLEMENT
  }
}






