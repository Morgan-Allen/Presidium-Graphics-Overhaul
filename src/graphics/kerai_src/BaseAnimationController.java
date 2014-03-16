package src.graphics.kerai_src;

import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.graphics.g3d.ModelInstance;
//import com.badlogic.gdx.graphics.g3d.model.Animation;
//import com.badlogic.gdx.graphics.g3d.model.Node;
//import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
//import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.Pool.Poolable;


/**
 * Base class for applying one or more {@link Animation2}s to a
 * {@link ModelInstance}. This class only applies the actual {@link Node}
 * transformations, it does not manage animations or keep track of animation
 * states. See {@link AnimationController} for an implementation of this class
 * which does manage animations.
 * 
 * @author Xoppa
 */
public class BaseAnimationController {
  
  
  public final static class Transform implements Poolable {
    
    final Vector3 translation = new Vector3();
    final Quaternion rotation = new Quaternion();
    final Vector3 scale = new Vector3(1, 1, 1);
    
    public Transform() {}
    public void reset() { idt(); }
    
    Transform idt() {
      translation.set(0, 0, 0);
      rotation.idt();
      scale.set(1, 1, 1);
      return this;
    }
    
    Transform set(
      final Vector3 t, final Quaternion r, final Vector3 s
    ) {
      translation.set(t);
      rotation.set(r);
      scale.set(s);
      return this;
    }
    
    Transform set(final Transform other) {
      return set(other.translation, other.rotation, other.scale);
    }
    
    Transform lerp(final Transform target, final float alpha) {
      return lerp(target.translation, target.rotation, target.scale, alpha);
    }
    
    Transform lerp(
      final Vector3 targetT, final Quaternion targetR,
      final Vector3 targetS, final float alpha
    ) {
      translation.lerp(targetT, alpha);
      rotation.slerp(targetR, alpha);
      scale.lerp(targetS, alpha);
      return this;
    }
    
    Matrix4 toMatrix4(final Matrix4 out) {
      return out.set(translation, rotation, scale);
    }
  }
  
  
  private final Pool<Transform> transformPool = new Pool<Transform>() {
    @Override
    protected Transform newObject() {
      return new Transform();
    }
  };
  
  
  private boolean applying = false;
  public final ModelInstance target;  //  TODO:  Not really needed- use args
  
  private final static ObjectMap <Node2, Transform>
    transforms = new ObjectMap <Node2, Transform>();
  private final static Transform
    tmpT = new Transform();
  
  
  
  public BaseAnimationController(final ModelInstance target) {
    this.target = target;
  }
  
  
  protected void begin() {
    if (applying) throw new GdxRuntimeException(
      "You must call end() after each call to begin()"
    );
    applying = true;
  }
  
  
  protected void apply(
    final Animation2 animation2, final float time, final float weight
  ) {
    if (! applying) throw new GdxRuntimeException(
      "You must call begin() before adding an animation"
    );
    applyAnimation(transforms, transformPool, weight, animation2, time);
  }
  
  
  protected static void applyAnimation(
    final ObjectMap <Node2, Transform> out,
    final Pool <Transform> pool, final float alpha,
    final Animation2 animation, final float time
  ) {
    if (out != null) {
      for (final Node2 node : out.keys())
        node.isAnimated = false;
    }
    
    for (final NodeAnimation2 nodeAnim : animation.nodeAnims) {
      final Node2 node = nodeAnim.node;
      node.isAnimated = true;
      
      // Find the keyframe(s)
      final NodeKeyframe2 frames[] = nodeAnim.keyframes;
      
      int first = 0, second = -1;
      for (int i = 0; i < frames.length - 1; i++) if (
        time >= frames[i].keytime &&
        time <= frames[i + 1].keytime
      ) {
        first = i;
        second = i + 1;
        break;
      }
      
      // Apply the first keyframe:
      final Transform transform = tmpT;
      final NodeKeyframe2 firstFrame = frames[first];
      transform.set(
        firstFrame.translation,
        firstFrame.rotation,
        firstFrame.scale
      );
      
      // Lerp the second keyframe
      if (second > first) {
        final NodeKeyframe2 secondFrame = frames[second];
        final float t =
          (time - firstFrame.keytime) /
          (secondFrame.keytime - firstFrame.keytime);
        transform.lerp(
          secondFrame.translation,
          secondFrame.rotation,
          secondFrame.scale, t
        );
      }
      
      // Apply the transform, either directly to the bone or to out when
      // blending
      if (out == null) {
        transform.toMatrix4(node.localTransform);
      }
      else {
        Transform t = out.get(node, null);
        if (t == null) {
          t = pool.obtain();
          t.set(node.translation, node.rotation, node.scale);
          out.put(node, t);
        }
        if (alpha > 0.999999f) t.set(transform);
        else t.lerp(transform, alpha);
      }
    }
    
    if (out != null) {
      for (final ObjectMap.Entry<Node2, Transform> e : out.entries()) {
        final Node2 node = e.key;
        final Transform t = e.value;
        if (! node.isAnimated) {
          node.isAnimated = true;
          t.lerp(e.key.translation, e.key.rotation, e.key.scale, alpha);
        }
      }
    }
  }
  
  
  
  protected void removeAnimation(final Animation2 animation2) {
    for (final NodeAnimation2 nodeAnim : animation2.nodeAnims) {
      nodeAnim.node.isAnimated = false;
    }
  }
  
  
  protected void end() {
    if (!applying) throw new GdxRuntimeException(
      "You must call begin() first"
    );
    for (Entry<Node2, Transform> entry : transforms.entries()) {
      entry.value.toMatrix4(entry.key.localTransform);
      transformPool.free(entry.value);
    }
    transforms.clear();
    target.calculateTransforms();
    applying = false;
  }
}




