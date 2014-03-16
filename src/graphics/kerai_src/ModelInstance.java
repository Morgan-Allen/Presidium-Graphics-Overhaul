

package src.graphics.kerai_src;
import src.util.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;



public class ModelInstance implements RenderableProvider {
  
  
  private static boolean verbose = true;
  
  public final Model model;
  
  //  TODO:  Offload these to a single convenience object?
  private Node modelNodes[];
  private NodePart modelParts[];
  final ObjectMap <Node, Integer> nodeIndex = new ObjectMap <Node, Integer> ();
  
  

  public Matrix4 transform;
  final Matrix4 nodeTransforms[];
  public final List <Material> materials = new List <Material> ();
  
  final AnimControl anim = new AnimControl(this);
  final OverlayAttribute attr = new OverlayAttribute(null);
  
  private Animation currentAnim;
  private float animTime;
  private List <NodePart> hidden = new List <NodePart> ();
  
  
  
  public ModelInstance(final Model model) {
    this.model = model;
    this.transform = new Matrix4();
    
    if (verbose) I.say("\nCompiling structure...");
    final Batch <Node> nodeB = new Batch <Node> ();
    final Batch <NodePart> partB = new Batch <NodePart> ();
    for (Node n : model.nodes) compileFrom(n, nodeB, partB);
    if (verbose) I.say("\n\n");
    
    modelNodes = nodeB.toArray(Node.class);
    modelParts = partB.toArray(NodePart.class);
    nodeTransforms = new Matrix4[modelNodes.length];
    int i = 0; for (Node n : modelNodes) {
      nodeTransforms[i] = new Matrix4();
      nodeIndex.put(n, i++);
    }
    
    //  TODO:  Replace with copies.
    for (Material m : materials) m.set(attr);
  }
  
  
  private void compileFrom(
    Node node, Batch <Node> nodeB, Batch <NodePart> partB
  ) {
    nodeB.add(node);
    if (verbose) I.say("Node is: "+node.id);
    for (NodePart p : node.parts) {
      partB.add(p);
      materials.include(p.material);
      if (verbose) I.say("  Part is: "+p.meshPart.id);
    }
    for (Node n : node.children) compileFrom(n, nodeB, partB);
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
    for (int i = 0; i < modelNodes.length; i++) {
      final Node node = modelNodes[i];
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
    for (int i = 0; i < modelParts.length; i++) {
      final NodePart part = modelParts[i];
      if (hidden.includes(part)) continue;
      final Renderable r = pool.obtain();
      
      final int numBones = part.invBoneBindTransforms.size;
      final Matrix4 boneSet[] = new Matrix4[numBones];  //TODO:  CACHE?
      for (int b = 0; b < numBones; b++) {
        final Node node = part.invBoneBindTransforms.keys[b];
        final Matrix4 offset = part.invBoneBindTransforms.values[b];
        boneSet[b] = new Matrix4(boneFor(node)).mul(offset);
      }
      
      r.worldTransform.set(transform);
      r.material = part.material;  //TODO:  NEEDS CUSTOMISATION
      r.bones = boneSet;
      r.mesh           = part.meshPart.mesh;
      r.meshPartOffset = part.meshPart.indexOffset;
      r.meshPartSize   = part.meshPart.numVertices;
      r.primitiveType  = part.meshPart.primitiveType;
      
      renderables.add(r);
    }
  }
  
  
  protected Matrix4 boneFor(Node node) {
    final int index = nodeIndex.get(node);
    return nodeTransforms[index];
  }
  
  
  public void setAnimation(String id, float progress) {
    final Animation match = model.getAnimation(id);
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
    Node node = model.nodes.get(0);
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
    Node node = model.nodes.get(0);
    for (NodePart np : node.parts) {
      if (np.meshPart.id.equals(id)) {
        if (visible) hidden.remove(np);
        else hidden.include(np);
      }
    }
  }
}







