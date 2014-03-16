package src.graphics.kerai_src;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.model.*;
//import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.utils.ObjectMap;



/** This probably needs a better name */
public class MS3DInstance {
  
  
  public ModelInstance mi;
  public BaseAnimationController anim;
  OverlayAttribute attr;
  

  public MS3DInstance(ModelInstance mi) {
    this.mi = mi;
    anim = new BaseAnimationController(mi);
    attr = new OverlayAttribute(null);
    for (Material mat : mi.materials) {
      mat.set(attr);
    }
  }
  
  public MS3DInstance(Model m) {
    this(new ModelInstance(m));
  }
  
  
  //  TODO:  This needs refinement.  Only apply to materials of a certain name!
  public void setOverlaySkins(Texture... skins) {
    attr.textures = skins;
  }
  
  
  
  
  public void hideParts(String... ids) {
    for (String id : ids) {
      togglePart(id, false);
    }
  }
  
  
  public void showOnly(String partID) {
    Node2 node = mi.nodes.get(0);
    for (NodePart2 np : node.parts) {
      if (np.meshPart.id.equals(partID)) {
        np.enabled = true;
      }
      else {
        np.enabled = false;
      }
    }
  }
  
  
  public void togglePart(String id, boolean visible) {
    Node2 node = mi.nodes.get(0);
    for (NodePart2 np : node.parts) {
      if (np.meshPart.id.equals(id)) {
        np.enabled = visible;
      }
    }
  }
  
  
  public void setAnimation(String id, float progress) {
    final Animation2 match = mi.getAnimation(id);
    if (match == null) return;
    anim.begin();
    anim.apply(match, progress * match.duration, 1);
    anim.end();
  }
}








