

package stratos.graphics.solids;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import org.apache.commons.math3.util.FastMath;



public class SolidSprite extends Sprite implements RenderableProvider {
  
  final static float
    ANIM_INTRO_TIME = 0.5f;
  
  
  final public SolidModel model;
  final Matrix4 transform = new Matrix4();
  final Matrix4 boneTransforms[];
  final Material materials[];
  private int hideMask = 0;
  
  private static class AnimState {
    Animation current;
    float time, incept;
  }
  final Stack <AnimState> animStates = new Stack <AnimState>();
  
  private static Vector3 temp = new Vector3();
  
  
  
  protected SolidSprite(final SolidModel model) {
    this.model = model;
    if (! model.compiled) I.complain("MODEL MUST BE COMPILED FIRST!");
    
    this.boneTransforms = new Matrix4[model.allNodes.length];
    for (int i = boneTransforms.length ; i-- > 0;) {
      boneTransforms[i] = new Matrix4();
    }
    
    this.materials = new Material[model.allMaterials.length];
    for (int i = materials.length; i-- > 0;) {
      materials[i] = model.allMaterials[i];
    }
    
    this.setAnimation(AnimNames.FULL_RANGE, 0);
  }
  
  
  public ModelAsset model() {
    return model;
  }
  
  
  public void readyFor(Rendering rendering) {
    //  Set up the translation matrix based on game-world position and facing-
    rendering.view.worldToGL(position, temp);
    transform.setToTranslation(temp);
    final float radians = (float) FastMath.toRadians(90 - rotation);
    transform.rot(Vector3.Y, radians);
    
    if (animStates.size() > 0) {
      //  If we're currently being animated, then we need to loop over each
      //  animation state and blend them together, while culling any that have
      //  expired-
      final float time = Rendering.activeTime();
      
      model.animControl.begin(this);
      AnimState validFrom = animStates.getFirst();
      for (AnimState state : animStates) {
        float alpha = (time - state.incept) / ANIM_INTRO_TIME;
        if (alpha >= 1) { validFrom = state; alpha = 1; }
        model.animControl.apply(state.current, state.time, alpha);
      }
      while (animStates.getFirst() != validFrom) animStates.removeFirst();
      model.animControl.end();
      
      final Matrix4 temp = new Matrix4();
      //  The nodes here are ordered so as to guarantee that parents are always
      //  visited before children, allowing a single pass-
      for (int i = 0; i < model.allNodes.length; i++) {
        final Node node = model.allNodes[i];
        if (node.parent == null) {
          boneTransforms[i].setToTranslation(node.translation);
          boneTransforms[i].scl(node.scale);
          continue;
        }
        final Matrix4 parentTransform = boneFor(node.parent);
        temp.set(parentTransform).mul(boneTransforms[i]);
        boneTransforms[i].set(temp);
      }
    }
    
    rendering.solidsPass.register(this);
  }
  
  
  
  /**  Rendering and animation-
   */
  public void getRenderables(
    Array<Renderable> renderables,
    Pool<Renderable> pool
  ) {
    //  In either case, you'll need to set up renderables for each node part-
    for (int i = 0; i < model.allParts.length; i++) {
      final NodePart part = model.allParts[i];
      if ((hideMask & (1 << i)) != 0) continue;
      final Renderable r = pool.obtain();
      
      final int numBones = part.invBoneBindTransforms.size;
      //  TODO:  Use an object pool for these, if possible?
      final Matrix4 boneSet[] = new Matrix4[numBones];
      for (int b = 0; b < numBones; b++) {
        final Node node = part.invBoneBindTransforms.keys[b];
        final Matrix4 offset = part.invBoneBindTransforms.values[b];
        boneSet[b] = new Matrix4(boneFor(node)).mul(offset);
      }
      
      final int matIndex = model.indexFor(part.material);
      r.worldTransform.set(transform);
      r.material       = materials[matIndex];
      r.bones          = boneSet;
      r.mesh           = part.meshPart.mesh;
      r.meshPartOffset = part.meshPart.indexOffset;
      r.meshPartSize   = part.meshPart.numVertices;
      r.primitiveType  = part.meshPart.primitiveType;
      
      renderables.add(r);
    }
  }
  
  
  protected Matrix4 boneFor(Node node) {
    final int index = model.indexFor(node);
    return boneTransforms[index];
  }
  
  
  public void setAnimation(String id, float progress) {
    final Animation match = model.gdxModel.getAnimation(id);
    if (match == null) {
      I.say("  WARNING:  No matching animation: "+id);
      I.say("  IN MODEL: "+model.assetID());
      return;
    }
    
    AnimState topState = animStates.getLast();
    final boolean newState =
      (animStates.size() == 0) ||
      (topState.current != match);
    
    if (newState) {
      topState = new AnimState();
      topState.current = match;
      topState.incept = Rendering.activeTime();
      animStates.addLast(topState);
    }
    topState.time = progress * match.duration;
  }
  
  
  
  
  /**  Customising appearance (toggling parts, adding skins)-
    */
  public void setOverlaySkins(String partName, Texture... skins) {
    final NodePart match = model.partWithName(partName);
    if (match == null) return;
    final Material base = match.material;
    final Material overlay = new Material(base);
    overlay.set(new OverlayAttribute(skins));
    this.materials[model.indexFor(base)] = overlay;
  }
  
  
  public Vec3D attachPoint(String label) {
    return new Vec3D(position);
  }
  
  
  
  /**  Showing and hiding model parts-
    */
  private void hideMask(NodePart p, boolean is) {
    final int index = model.indexFor(p);
    if (is) hideMask |= 1 << index;
    else hideMask &= ~ (1 << index);
  }
  
  
  public void hideParts(String... partIDs) {
    for (String id : partIDs) {
      togglePart(id, false);
    }
  }
  
  
  public void showOnly(String partID) {
    final Node root = model.allNodes[0];
    boolean match = false;
    for (NodePart np : root.parts) {
      if (np.meshPart.id.equals(partID)) {
        hideMask(np, false);
        match = true;
      }
      else hideMask(np, true);
    }
    if (! match) I.say("  WARNING:  No matching model part: "+partID);
  }
  
  
  public void togglePart(String partID, boolean visible) {
    final Node root = model.allNodes[0];
    for (NodePart np : root.parts) {
      if (np.meshPart.id.equals(partID)) {
        hideMask(np, ! visible);
        return;
      }
    }
    I.say("  WARNING:  No matching model part: "+partID);
  }
}



