

package src.graphics.kerai_src;
import src.util.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;



public class ModelInstance implements RenderableProvider {
  
  
  
  public final Array<Material> materials = new Array();
  public final Array<Node> nodes = new Array(); // root nodes of model...
  public final Array<Animation> animations = new Array();
  
  public final Model model;
  public Matrix4 transform;

  private ObjectMap <NodePart, ArrayMap<Node, Matrix4>>
    nodePartBones = new ObjectMap <NodePart, ArrayMap<Node, Matrix4>>();
  
  final AnimControl anim = new AnimControl(this);
  final OverlayAttribute attr = new OverlayAttribute(null);
  
  private Animation currentAnim;
  private float animTime;
  private List <NodePart> hidden = new List();
  
  
  
  public ModelInstance(final Model model) {
    this.model = model;
    this.transform = new Matrix4();
    copyNodes(model.nodes, (String[]) null);
    copyAnimations(model.animations);
    calculateTransforms();
    
    for (Material m : materials) m.set(attr);
  }
  
  
  private void copyNodes(Array<Node> nodes, final String... nodeIds) {
    nodePartBones.clear();
    for (int i = 0, n = nodes.size; i < n; ++i) {
      final Node node = nodes.get(i);
      boolean match = false;
      if (nodeIds != null) for (final String nodeId : nodeIds) {
        if (nodeId.equals(node.id)) {
          match = true;
          break;
        }
      }
      else match = true;
      if (match) this.nodes.add(copyNode(null, node));
    }
    setBones();
  }
  

  private Node copyNode(Node parent, Node node) {
    Node copy = new Node();
    copy.id = node.id;
    copy.parent = parent;
    copy.translation.set(node.translation);
    copy.rotation.set(node.rotation);
    copy.scale.set(node.scale);
    copy.localTransform.set(node.localTransform);
    copy.globalTransform.set(node.globalTransform);

    for (NodePart nodePart : node.parts) {
      copy.parts.add(copyNodePart(nodePart));
    }
    for (Node child : node.children) {
      copy.children.add(copyNode(copy, child));
    }
    return copy;
  }
  

  private NodePart copyNodePart(NodePart nodePart) {
    NodePart copy = new NodePart();
    copy.meshPart = new MeshPart();
    copy.meshPart.id            = nodePart.meshPart.id;
    copy.meshPart.indexOffset   = nodePart.meshPart.indexOffset;
    copy.meshPart.numVertices   = nodePart.meshPart.numVertices;
    copy.meshPart.primitiveType = nodePart.meshPart.primitiveType;
    copy.meshPart.mesh          = nodePart.meshPart.mesh;

    if (nodePart.invBoneBindTransforms != null)
      nodePartBones.put(copy, nodePart.invBoneBindTransforms);

    final int index = materials.indexOf(nodePart.material, false);
    if (index < 0)
      materials.add(copy.material = nodePart.material.copy());
    else
      copy.material = materials.get(index);

    return copy;
  }
  

  private void copyAnimations(final Iterable<Animation> source) {
    for (final Animation anim : source) {
      Animation Animation = new Animation();
      Animation.id = anim.id;
      Animation.duration = anim.duration;
      for (final NodeAnimation nanim : anim.nodeAnimations) {
        final Node Node = getNode(nanim.node.id);
        if (Node == null) continue;
        NodeAnimation nodeAnim = new NodeAnimation();
        nodeAnim.node = Node;
        nodeAnim.keyframes = new Array <NodeKeyframe> ();
        
        for (final NodeKeyframe kf : nanim.keyframes) {
          NodeKeyframe keyframe = new NodeKeyframe();
          keyframe.keytime = kf.keytime;
          keyframe.rotation.set(kf.rotation);
          keyframe.scale.set(kf.scale);
          keyframe.translation.set(kf.translation);
          nodeAnim.keyframes.add(keyframe);
        }
        if (nodeAnim.keyframes.size > 0)
          Animation.nodeAnimations.add(nodeAnim);
      }
      if (Animation.nodeAnimations.size > 0)
        animations.add(Animation);
    }
  }
  
  
  private void setBones() {
    for (
      ObjectMap.Entry<NodePart, ArrayMap<Node, Matrix4>> e :
      nodePartBones.entries()
    ) {
      final NodePart part = e.key;
      final ArrayMap <Node, Matrix4> map = e.value;
      
      if (part.invBoneBindTransforms == null) {
        part.invBoneBindTransforms = new ArrayMap<Node, Matrix4>(
          true, map.size, Node.class, Matrix4.class
        );
      }
      part.invBoneBindTransforms.clear();
      
      for (final ObjectMap.Entry<Node, Matrix4> b : map.entries()) {
        part.invBoneBindTransforms.put(getNode(b.key.id), b.value);
        // Share the inv bind matrix with the model
      }
      part.bones = new Matrix4[map.size];
      for (int i = 0; i < e.key.bones.length; i++) {
        part.bones[i] = new Matrix4();
      }
    }
  }
  
  
  
  /**
   * Traverses the Node hierarchy and collects {@link Renderable} instances for
   * every node with a graphical representation. Renderables are obtained from
   * the provided pool. The resulting array can be rendered via a
   * {@link ModelBatch}.
   * 
   * @param renderables the output array
   * @param pool the pool to obtain Renderables from
   */
  public void getRenderables(
    Array<Renderable> renderables,
    Pool<Renderable> pool
  ) {
    anim.begin();
    anim.apply(currentAnim, animTime, 1);
    anim.end();
    for (Node node : nodes) getRenderables(node, renderables, pool);
  }
  
  
  private void getRenderables(
    Node node, Array<Renderable> renderables, Pool<Renderable> pool
  ) {
    for (NodePart part : node.parts) {
      if (hidden.includes(part)) continue;
      final Renderable out = pool.obtain();
      
      part.setRenderable(out);
      if (part.bones == null && transform != null) {
        out.worldTransform.set(transform).mul(node.globalTransform);
      }
      else if (transform != null) {
        out.worldTransform.set(transform);
      }
      else {
        out.worldTransform.idt();
      }
      renderables.add(out);
    }
    for (Node child : node.children) {
      getRenderables(child, renderables, pool);
    }
  }
  
  
  /**
   * Calculates the local and world transform of all {@link Node} instances in
   * this model, recursively. First each {@link Node#localTransform} transform
   * is calculated based on the translation, rotation and scale of each Node.
   * Then each {@link Node#calculateWorldTransform()} is calculated, based on
   * the parent's world transform and the local transform of each Node. Finally,
   * the animation bone matrices are updated accordingly.</p>
   * 
   * This method can be used to recalculate all transforms if any of the Node's
   * local properties (translation, rotation, scale) was modified.
   */
  public void calculateTransforms() {
    final int n = nodes.size;
    for (int i = 0; i < n; i++) {
      nodes.get(i).calculateTransforms(true);
    }
    for (int i = 0; i < n; i++) {
      nodes.get(i).calculateBoneTransforms(true);
    }
  }
  
  
  public Animation getAnimation(final String id) {
    final int n = animations.size;
    Animation Animation;
    for (int i = 0; i < n; i++)
      if ((Animation = animations.get(i)).id.equals(id))
        return Animation;
    return null;
  }
  
  
  public Node getNode(final String id) {
    return Node.getNode(nodes, id, true, false);
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
    Node node = nodes.get(0);
    for (NodePart np : node.parts) {
      if (np.meshPart.id.equals(partID)) {
        hidden.remove(np);
        //np.enabled = true;
      }
      else {
        hidden.include(np);
        //np.enabled = false;
      }
    }
  }
  
  
  public void togglePart(String id, boolean visible) {
    Node node = nodes.get(0);
    for (NodePart np : node.parts) {
      if (np.meshPart.id.equals(id)) {
        if (visible) hidden.remove(np);
        else hidden.include(np);
        //np.enabled = visible;
      }
    }
  }
  
  
  public void setAnimation(String id, float progress) {
    final Animation match = getAnimation(id);
    if (match == null) return;
    currentAnim = match;
    animTime = progress * match.duration;
  }
}







