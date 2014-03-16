

package src.graphics.kerai_src;
//import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.graphics.g3d.ModelInstance;
//import com.badlogic.gdx.graphics.g3d.model.Animation;
//import com.badlogic.gdx.graphics.g3d.model.Node;
//import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
//import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;

import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.Pool.Poolable;



public class AnimControl {
  
  
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
  
  
  private boolean applying = false;
  public final ModelInstance target;  //  TODO:  Not really needed?
  
  private final static ObjectMap <Node, Transform>
    transforms = new ObjectMap <Node, Transform>();
  private final static Transform
    tmpT = new Transform();
  
  private final static Pool<Transform> transformPool = new Pool<Transform>() {
    @Override
    protected Transform newObject() {
      return new Transform();
    }
  };
  
  
  
  public AnimControl(final ModelInstance target) {
    this.target = target;
  }
  
  
  protected void begin() {
    if (applying) throw new GdxRuntimeException(
      "You must call end() after each call to begin()"
    );
    applying = true;
  }
  
  
  protected void apply(
    final Animation Animation, final float time, final float weight
  ) {
    if (! applying) throw new GdxRuntimeException(
      "You must call begin() before adding an animation"
    );
    applyAnimation(transforms, transformPool, weight, Animation, time);
  }
  
  
  protected void applyAnimation(
    final ObjectMap <Node, Transform> out,
    final Pool <Transform> pool, final float alpha,
    final Animation animation, final float time
  ) {
    for (final NodeAnimation nodeAnim : animation.nodeAnimations) {
      final Node node = nodeAnim.node;
      
      // Find the keyframe(s)
      //  TODO:  See if this can't be cached somehow
      final NodeKeyframe frames[] = nodeAnim.keyframes.toArray(NodeKeyframe.class);
      
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
      final NodeKeyframe firstFrame = frames[first];
      transform.set(
        firstFrame.translation,
        firstFrame.rotation,
        firstFrame.scale
      );
      
      // Lerp the second keyframe
      if (second > first) {
        final NodeKeyframe secondFrame = frames[second];
        final float t =
          (time - firstFrame.keytime) /
          (secondFrame.keytime - firstFrame.keytime);
        transform.lerp(
          secondFrame.translation,
          secondFrame.rotation,
          secondFrame.scale, t
        );
      }
      
      // Apply the transform-
      Transform t = out.get(node);
      if (t == null) {
        t = pool.obtain();
        t.set(node.translation, node.rotation, node.scale);
        out.put(node, t);
      }
      if (alpha > 0.999999f) t.set(transform);
      else t.lerp(transform, alpha);
    }
    
    //  TODO:  Restore this later?
    /*
    //if (out != null) {
      for (final ObjectMap.Entry<Node, Transform> e : out.entries()) {
        final Node node = e.key;
        final Transform t = e.value;
        t.lerp(node.translation, node.rotation, node.scale, alpha);
        //if (! node.isAnimated) {
          //node.isAnimated = true;
        //}
      }
    //}
    //*/
  }
  
  
  protected void removeAnimation(final Animation Animation) {
    for (final NodeAnimation nodeAnim : Animation.nodeAnimations) {
      nodeAnim.node.isAnimated = false;
    }
  }
  
  
  protected void end() {
    if (! applying) throw new GdxRuntimeException(
      "You must call begin() first"
    );
    for (Entry <Node, Transform> entry : transforms.entries()) {
      final Node node = entry.key;
      final Transform t = entry.value;
      t.toMatrix4(target.boneFor(node));
      transformPool.free(t);
    }
    transforms.clear();
    //target.calculateTransforms();
    applying = false;
  }
}







