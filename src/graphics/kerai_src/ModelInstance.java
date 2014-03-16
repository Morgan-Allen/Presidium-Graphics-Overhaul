package src.graphics.kerai_src;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.*;



public class ModelInstance implements RenderableProvider {
  
  
  // TODO: Get rid of the nodes, and probably just have one animation.
  public final Array<Material> materials = new Array();
  public final Array<Node2> nodes = new Array(); // root nodes of model...
  public final Array<Animation2> animations = new Array();
  public final Model model;
  public Matrix4 transform;

  private ObjectMap <NodePart2, ArrayMap<Node, Matrix4>>
    nodePartBones = new ObjectMap <NodePart2, ArrayMap<Node, Matrix4>>();
  
  
  
  public ModelInstance(final Model model) {
    this.model = model;
    this.transform = new Matrix4();
    copyNodes(model.nodes, (String[]) null);
    copyAnimations(model.animations);
    calculateTransforms();
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
  

  private Node2 copyNode(Node2 parent, Node node) {
    Node2 copy = new Node2();
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
  

  private NodePart2 copyNodePart(NodePart nodePart) {
    NodePart2 copy = new NodePart2();
    copy.meshPart = new MeshPart();
    copy.meshPart.id = nodePart.meshPart.id;
    copy.meshPart.indexOffset = nodePart.meshPart.indexOffset;
    copy.meshPart.numVertices = nodePart.meshPart.numVertices;
    copy.meshPart.primitiveType = nodePart.meshPart.primitiveType;
    copy.meshPart.mesh = nodePart.meshPart.mesh;

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
      Animation2 animation2 = new Animation2();
      animation2.id = anim.id;
      animation2.duration = anim.duration;
      for (final NodeAnimation nanim : anim.nodeAnimations) {
        final Node2 node2 = getNode(nanim.node.id);
        if (node2 == null)
          continue;
        NodeAnimation2 nodeAnim = new NodeAnimation2();
        nodeAnim.node = node2;
        nodeAnim.keyframes = new NodeKeyframe2[nanim.keyframes.size];
        
        int i = 0 ; for (final NodeKeyframe kf : nanim.keyframes) {
          NodeKeyframe2 keyframe = new NodeKeyframe2();
          keyframe.keytime = kf.keytime;
          keyframe.rotation.set(kf.rotation);
          keyframe.scale.set(kf.scale);
          keyframe.translation.set(kf.translation);
          nodeAnim.keyframes[i++] = keyframe;
        }
        if (nodeAnim.keyframes.length > 0)
          animation2.nodeAnims.add(nodeAnim);
      }
      if (animation2.nodeAnims.size > 0)
        animations.add(animation2);
    }
  }
  
  
  //  TODO:  This might not be needed.
  //*
  private void setBones() {
    for (
      ObjectMap.Entry<NodePart2, ArrayMap<Node, Matrix4>> e :
      nodePartBones.entries()
    ) {
      final NodePart2 part = e.key;
      final ArrayMap <Node, Matrix4> map = e.value;
      
      if (part.invBoneBindTransforms == null) {
        part.invBoneBindTransforms = new ArrayMap<Node2, Matrix4>(
          true, map.size, Node2.class, Matrix4.class
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
  //*/
  
  
  
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
    for (Node2 node : nodes) getRenderables(node, renderables, pool);
  }
  
  
  private void getRenderables(
    Node2 node, Array<Renderable> renderables, Pool<Renderable> pool
  ) {
    for (NodePart2 part : node.parts) {
      if (! part.enabled) continue;
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
    for (Node2 child : node.children) {
      getRenderables(child, renderables, pool);
    }
  }
  
  
  /**
   * Calculates the local and world transform of all {@link Node2} instances in
   * this model, recursively. First each {@link Node2#localTransform} transform
   * is calculated based on the translation, rotation and scale of each Node.
   * Then each {@link Node2#calculateWorldTransform()} is calculated, based on
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
  
  
  public Animation2 getAnimation(final String id) {
    final int n = animations.size;
    Animation2 animation2;
    for (int i = 0; i < n; i++)
      if ((animation2 = animations.get(i)).id.equals(id))
        return animation2;
    return null;
  }
  
  
  public Node2 getNode(final String id) {
    return Node2.getNode(nodes, id, true, false);
  }
}










/*
  public Material getMaterial(final String id) {
    final int n = materials.size;
    Material material;
    for (int i = 0; i < n; i++)
      if ((material = materials.get(i)).id.equals(id))
        return material;
    return null;
  }
//*/


/** @return The renderable of the first node's first part. */
/*
public Renderable getRenderable(final Renderable out) {
  return getRenderable(out, nodes.get(0));
}


/** @return The renderable of the node's first part. */
/*
public Renderable getRenderable(final Renderable out, final Node2 node2) {
  return getRenderable(out, node2, node2.parts.get(0));
}


//*/

/**
 * @param id
 *          The ID of the node to fetch.
 * @param recursive
 *          false to fetch a root node only, true to search the entire node
 *          tree for the specified node.
 * @param ignoreCase
 *          whether to use case sensitivity when comparing the node id.
 * @return The {@link Node2} with the specified id, or null if not found.
 */
/*
public Node2 getNode(final String id, boolean recursive) {
  return Node2.getNode(nodes, id, recursive, false);
}
//*/
/**
 * Calculate the bounding box of this model instance. This is a potential slow
 * operation, it is advised to cache the result.
 * 
 * @param out
 *          the {@link BoundingBox} that will be set with the bounds.
 * @return the out parameter for chaining
 */
/*
public BoundingBox calculateBoundingBox(final BoundingBox out) {
  out.inf();
  return extendBoundingBox(out);
}
//*/

/**
 * Extends the bounding box with the bounds of this model instance. This is a
 * potential slow operation, it is advised to cache the result.
 * 
 * @param out
 *          the {@link BoundingBox} that will be extended with the bounds.
 * @return the out parameter for chaining
 */
/*
public BoundingBox extendBoundingBox(final BoundingBox out) {
  final int n = nodes.size;
  for (int i = 0; i < n; i++)
    nodes.get(i).extendBoundingBox(out);
  return out;
}
//*/



/**
 * @param model
 *          The source {@link Model}
 * @param transform
 *          The {@link Matrix4} instance for this ModelInstance to reference or
 *          null to create a new matrix.
 * @param nodeId
 *          The ID of the {@link Node2} within the {@link Model} for the
 *          instance to contain
 * @param recursive
 *          True to recursively search the Model's node tree, false to only
 *          search for a root node
 * @param parentTransform
 *          True to apply the parent's node transform to the instance (only
 *          applicable if recursive is true).
 * @param mergeTransform
 *          True to apply the source node transform to the instance transform,
 *          resetting the node transform.
 */
/*
 * public ModelInstance( final Model model, final Matrix4 transform, final
 * String nodeId, boolean recursive, boolean parentTransform, boolean
 * mergeTransform ) { this.model = model; this.transform = transform == null ?
 * new Matrix4() : transform; nodePartBones.clear(); Node2 copy, node2 =
 * model.getNode(nodeId, recursive); this.nodes.add(copy = copyNode(null,
 * node2)); if (mergeTransform) { this.transform.mul(parentTransform ?
 * node2.globalTransform : node2.localTransform); copy.translation.set(0,0,0);
 * copy.rotation.idt(); copy.scale.set(1,1,1); } else if (parentTransform &&
 * copy.parent != null) this.transform.mul(node2.parent.globalTransform);
 * setBones(); copyAnimations(model.animations); calculateTransforms(); } //
 */
