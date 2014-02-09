


package graphics.jointed ;
import util.I;

import com.badlogic.gdx.graphics.g3d.* ;
import com.badlogic.gdx.graphics.g3d.utils.* ;
import com.badlogic.gdx.graphics.g3d.model.* ;



public class JointSprite extends ModelInstance {
	
	
	
	final AnimationController animControl ;
	public float fog ;
	private float lastTime = -1;
	
	
	public JointSprite(Model model) {
		super(model) ;
		animControl = new AnimationController(this) ;
	}
	
	
	public void updateAnim(String animName, float time) {
	  if (
	    animControl.current == null ||
	    ! animControl.current.animation.id.equals(animName)
	  ) {
	    if (model.getAnimation(animName) == null) return;
	    animControl.animate(animName, -1, null, -1);
	    lastTime = -1;
	  }
	  
	  if (lastTime == -1) lastTime = 0;
	  if (time < lastTime) lastTime--;
	  float delta = time - lastTime;
	  delta *= animControl.current.animation.duration;
	  animControl.update(delta);
	  ///I.say("Update delta is: "+delta) ;
	  lastTime = time;
	}
}



//  TODO:  You might want to write your own animation controller for safety's
//  sake.  Later, though.
/*
public class BaseAnimationController {
  public final static class Transform implements Poolable {
    public final Vector3 translation = new Vector3();
    public final Quaternion rotation = new Quaternion();
    public final Vector3 scale = new Vector3(1,1,1);
    public Transform () { }
    public Transform idt() {
      translation.set(0,0,0);
      rotation.idt();
      scale.set(1,1,1);
      return this;
    }
    public Transform set(final Vector3 t, final Quaternion r, final Vector3 s) {
      translation.set(t);
      rotation.set(r);
      scale.set(s);
      return this;
    }
    public Transform set(final Transform other) {
      return set(other.translation, other.rotation, other.scale);
    }
    public Transform lerp(final Transform target, final float alpha) {
      return lerp(target.translation, target.rotation, target.scale, alpha);
    }
    public Transform lerp(final Vector3 targetT, final Quaternion targetR, final Vector3 targetS, final float alpha) {
      translation.lerp(targetT, alpha);
      rotation.slerp(targetR, alpha);
      scale.lerp(targetS, alpha);
      return this;
    }
    public Matrix4 toMatrix4(final Matrix4 out) {
      return out.set(translation, rotation, scale);
    }
    @Override
    public void reset () {
      idt();
    }
  }
  
  private final Pool<Transform> transformPool = new Pool<Transform>() {
    @Override
    protected Transform newObject () {
      return new Transform();
    }
  };
  private final static ObjectMap<Node, Transform> transforms = new ObjectMap<Node, Transform>();
  private boolean applying = false;
  /** The {@link ModelInstance} on which the animations are being performed. */
/*
  public final ModelInstance target;
  
  /** Construct a new BaseAnimationController.
   * @param target The {@link ModelInstance} on which the animations are being performed. */
/*
  public BaseAnimationController(final ModelInstance target) {
    this.target = target;
  }
  
  /** Begin applying multiple animations to the instance, 
   * must followed by one or more calls to {{@link #apply(Animation, float, float)} and finally {{@link #end()}. */
/*
  protected void begin() {
    if (applying)
      throw new GdxRuntimeException("You must call end() after each call to being()");
    applying = true;
  }
  
  /** Apply an animation, must be called between {{@link #begin()} and {{@link #end()}.
   * @param weight The blend weight of this animation relative to the previous applied animations. */
/*
  protected void apply(final Animation animation, final float time, final float weight) {
    if (!applying)
      throw new GdxRuntimeException("You must call begin() before adding an animation");
    applyAnimation(transforms, transformPool, weight, animation, time);
  }
  
  /** End applying multiple animations to the instance and update it to reflect the changes. */
/*
  protected void end() {
    if (!applying)
      throw new GdxRuntimeException("You must call begin() first");
    for (Entry<Node, Transform> entry : transforms.entries()) {
      entry.value.toMatrix4(entry.key.localTransform);
      transformPool.free(entry.value);
    }
    transforms.clear();
    target.calculateTransforms();
    applying = false;
  }
  
  /** Apply a single animation to the {@link ModelInstance} and update the it to reflect the changes. */ 
/*
  protected void applyAnimation(final Animation animation, final float time) {
    if (applying)
      throw new GdxRuntimeException("Call end() first");
    applyAnimation(null, null, 1.f, animation, time);
    target.calculateTransforms();
  }
  
  /** Apply two animations, blending the second onto to first using weight. */
/*
  protected void applyAnimations(final Animation anim1, final float time1, final Animation anim2, final float time2, final float weight) {
    if (anim2 == null || weight == 0.f)
      applyAnimation(anim1, time1);
    else if (anim1 == null || weight == 1.f)
      applyAnimation(anim2, time2);
    else if (applying)
      throw new GdxRuntimeException("Call end() first");
    else {
      begin();
      apply(anim1, time1, 1.f);
      apply(anim2, time2, weight);
      end();
    }
  }
  
  private final static Transform tmpT = new Transform();
  /** Helper method to apply one animation to either an objectmap for blending or directly to the bones. */
/*
  protected static void applyAnimation(final ObjectMap<Node, Transform> out, final Pool<Transform> pool, final float alpha, final Animation animation, final float time) {
    for (final NodeAnimation nodeAnim : animation.nodeAnimations) {
      final Node node = nodeAnim.node;
      node.isAnimated = true;
      // Find the keyframe(s)
      final int n = nodeAnim.keyframes.size - 1;
      int first = 0, second = -1;
      for (int i = 0; i < n; i++) {
        if (time >= nodeAnim.keyframes.get(i).keytime && time <= nodeAnim.keyframes.get(i+1).keytime) {
          first = i;
          second = i+1;
          break;
        }
      }
      // Apply the first keyframe:
      final Transform transform = tmpT;
      final NodeKeyframe firstKeyframe = nodeAnim.keyframes.get(first);
      transform.set(firstKeyframe.translation, firstKeyframe.rotation, firstKeyframe.scale);
      // Lerp the second keyframe
      if (second > first) {
        final NodeKeyframe secondKeyframe = nodeAnim.keyframes.get(second);
        final float t = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime);
        transform.lerp(secondKeyframe.translation, secondKeyframe.rotation, secondKeyframe.scale, t);
      }
      // Apply the transform, either directly to the bone or to out when blending
      if (out == null)
        transform.toMatrix4(node.localTransform);
      else {
        if (out.containsKey(node)) {
          if (alpha == 1.f)
            out.get(node).set(transform);
          else
            out.get(node).lerp(transform, alpha);
        } else {
          out.put(node, pool.obtain().set(transform));
        }
      }
    }
  }
  
  /** Remove the specified animation, by marking the affected nodes as not animated. When switching animation, this should
   * be call prior to applyAnimation(s). */
/*
  protected void removeAnimation(final Animation animation) {
    for (final NodeAnimation nodeAnim : animation.nodeAnimations) {
      nodeAnim.node.isAnimated = false;
    }
  }
}

//*/

