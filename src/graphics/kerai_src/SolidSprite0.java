

package src.graphics.kerai_src;
import src.graphics.common.*;
import src.util.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;



public class SolidSprite0 extends Sprite implements RenderableProvider {
  
  
  final public SolidModel model;
  public Matrix4 transform;
  final Matrix4 nodeTransforms[];
  final Material materials[];
  
  final AnimControl anim = new AnimControl(this);
  final OverlayAttribute attr = new OverlayAttribute(null);
  
  //  TODO:  Allow fade-ins between animation states...
  private Animation currentAnim;
  private float animTime;
  private List <NodePart> hidden = new List <NodePart> ();
  
  private static Vector3 temp = new Vector3();
  
  
  
  protected SolidSprite0(final SolidModel model) {
    this.model = model;
    if (! model.compiled) I.complain("MODEL MUST BE COMPILED FIRST!");
    
    this.transform = new Matrix4();
    this.nodeTransforms = new Matrix4[model.modelNodes.length];
    this.materials = new Material[model.modelMaterials.length];
    
    int i = 0; for (Node n : model.modelNodes) {
      nodeTransforms[i++] = new Matrix4();
    }
    for (i = 0; i < materials.length; i++) {
      final Material source = model.modelMaterials[i];
      final Material m = new Material(source);
      m.set(attr);
      materials[i] = m;
    }
  }
  
  
  public ModelAsset model() {
    return model;
  }
  
  
  public void update() {
  }
  
  
  public void registerFor(Rendering rendering) {
    rendering.view.worldToGL(position, temp);
    transform.setToTranslation(temp);
    
    final float radians = (float) Math.toRadians(90 - rotation);
    transform.rot(Vector3.Y, radians);
    //  TODO:  IMPLEMENT
    //rendering.cutoutsPass.register(this);
  }






  /**  Rendering and animation-
   */
  public void getRenderables(
    Array<Renderable> renderables,
    Pool<Renderable> pool
  ) {
    anim.begin();
    anim.apply(currentAnim, animTime, 1);
    anim.end();
    
    final Matrix4 temp = new Matrix4();
    //  The nodes here are ordered so as to guarantee that parents are always
    //  visited before children, allowing a single pass-
    for (int i = 0; i < model.modelNodes.length; i++) {
      final Node node = model.modelNodes[i];
      if (node.parent == null) {
        nodeTransforms[i].setToTranslation(node.translation);
        nodeTransforms[i].scl(node.scale);
        continue;
      }
      final Matrix4 parentTransform = boneFor(node.parent);
      temp.set(parentTransform).mul(nodeTransforms[i]);
      nodeTransforms[i].set(temp);
    }
    
    //  
    for (int i = 0; i < model.modelParts.length; i++) {
      final NodePart part = model.modelParts[i];
      if (hidden.includes(part)) continue;
      final Renderable r = pool.obtain();
      
      final int numBones = part.invBoneBindTransforms.size;
      final Matrix4 boneSet[] = new Matrix4[numBones];  //TODO:  CACHE?
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
    return nodeTransforms[index];
  }
  
  
  public void setAnimation(String id, float progress) {
    final Animation match = model.gdxModel.getAnimation(id);
    if (match == null) return;
    currentAnim = match;
    animTime = progress * match.duration;
  }
  
  
  
  
  /**  Customising appearance (toggling parts, adding skins)-
    */
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
    Node node = model.modelNodes[0];
    for (NodePart np : node.parts) {
      if (np.meshPart.id.equals(partID)) {
        hidden.remove(np);
      }
      else {
        hidden.include(np);
      }
    }
  }
  
  
  public void togglePart(String id, boolean visible) {
    Node node = model.modelNodes[0];
    for (NodePart np : node.parts) {
      if (np.meshPart.id.equals(id)) {
        if (visible) hidden.remove(np);
        else hidden.include(np);
      }
    }
  }
}







