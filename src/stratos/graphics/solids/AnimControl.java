


package stratos.graphics.solids;
import stratos.util.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.ObjectMap.Entry;



//  NOTE:  I'm associating animation control with the model, rather than the
//  sprite, since that makes it easier and faster to re-use transform objects,
//  rather than relying on table initialisation, etc.


public class AnimControl {
  
  
  final SolidModel model;
  private SolidSprite bound = null;
  
  final ObjectMap <Node, Transform>
    transforms = new ObjectMap <Node, Transform>();
  final Transform
    temp = new Transform();

  final Vars.Ref <NodeKeyframe>
    firstT  = new Vars.Ref(),
    secondT = new Vars.Ref(),
    firstR  = new Vars.Ref(),
    secondR = new Vars.Ref(),
    firstS  = new Vars.Ref(),
    secondS = new Vars.Ref()
  ;
  
  
  protected AnimControl(final SolidModel model) {
    this.model = model;
    for (Node node : model.allNodes) {
      transforms.put(node, new Transform());
    }
  }
  
  
  protected void begin(SolidSprite toBind) {
    if (bound != null) throw new GdxRuntimeException(
      "You must call end() after each call to begin()"
    );
    bound = toBind;
    for (Node node : model.allNodes) {
      final Transform t = transforms.get(node);
      t.set(node.translation, node.rotation, node.scale);
    }
  }
  
  
  protected void apply(
    final Animation animation, final float time, final float alpha
  ) {
    if (bound == null) throw new GdxRuntimeException(
      "You must call begin() before adding an animation"
    );
    
    for (final NodeAnimation nodeAnim : animation.nodeAnimations) {
      //
      //  Basic sanity checks and data hooks-
      //  TODO:  See if this can't be cached somewhere?
      
      firstT.value = secondT.value = null;
      firstR.value = secondR.value = null;
      firstS.value = secondS.value = null;
      putLerp(nodeAnim.rotation   , time, alpha, firstR, secondR);
      putLerp(nodeAnim.translation, time, alpha, firstT, secondT);
      putLerp(nodeAnim.scaling    , time, alpha, firstS, secondS);
      
      if (firstT.value != null) temp.translation.set(
        (Vector3) firstT.value.value
      );
      if (firstR.value != null) temp.rotation.set(
        (Quaternion) firstR.value.value
      );
      if (firstS.value != null) temp.scale.set(
        (Vector3) firstS.value.value
      );
      if (secondT.value != null) temp.translation.slerp(
        (Vector3) secondT.value.value, between(firstT, secondT, time)
      );
      if (secondR.value != null) temp.rotation.slerp(
        (Quaternion) secondR.value.value, between(firstR, secondR, time)
      );
      if (secondS.value != null) temp.scale.slerp(
        (Vector3) secondS.value.value, between(firstS, secondS, time)
      );
      
      //  And apply the transform-
      final Transform t = transforms.get(nodeAnim.node);
      if (alpha > 0.999f) t.set(temp);
      else t.lerp(temp, alpha);
    }
  }
  
  
  void putLerp(
    Array array, float time, float alpha,
    Vars.Ref first, Vars.Ref second
  ) {
    if (array == null) return;
    final NodeKeyframe frames[];
    frames = ((Array <NodeKeyframe>) array).toArray(NodeKeyframe.class);
    if (frames.length <= 1) return;
    
    //  Firstly, find the keyframe(s) before and after the reference time.
    //  NOTE:  We apply a binary search here for efficiency, as some models
    //  can have hundreds of animation-frames:
    int top = frames.length - 2, bot = 0, mid;
    NodeKeyframe firstFrame = null, secondFrame = null;
    
    if (top < bot) firstFrame = frames[0];
    else while (bot >= 0 && top < frames.length) {
      mid = (top + bot) / 2;
      final float
        time1 = (frames[mid    ]).keytime,
        time2 = (frames[mid + 1]).keytime;
      
      if (time >= time1 && time <= time2) {
        firstFrame  = frames[mid    ];
        secondFrame = frames[mid + 1];
        break;
      }
      else if (top == bot     ) top = --bot;
      else if ((top - bot) < 2) bot = top  ;
      else if (time < time2   ) top = mid  ;
      else if (time > time1   ) bot = mid  ;
    }
    if (firstFrame == null) return;
    
    first.value = firstFrame;
    second.value = (first == second) ? null : secondFrame;
  }
  
  
  float between(
    Vars.Ref <NodeKeyframe> a, Vars.Ref <NodeKeyframe> b, float time
  ) {
    return (time - a.value.keytime) / (b.value.keytime - a.value.keytime);
  }
  
  
  public final static class Transform {
    
    final Vector3 translation = new Vector3();
    final Quaternion rotation = new Quaternion();
    final Vector3 scale = new Vector3(1, 1, 1);
    
    Transform set(final Transform other) {
      return set(other.translation, other.rotation, other.scale);
    }
    
    Transform lerp(final Transform target, final float alpha) {
      return lerp(target.translation, target.rotation, target.scale, alpha);
    }
    
    Transform set(
      final Vector3 t, final Quaternion r, final Vector3 s
    ) {
      if (t != null) translation.set(t);
      if (r != null) rotation.set(r);
      if (s != null) scale.set(s);
      return this;
    }
    
    Transform lerp(
      final Vector3 targetT, final Quaternion targetR,
      final Vector3 targetS, final float alpha
    ) {
      if (targetT != null) translation.lerp (targetT, alpha);
      if (targetR != null) rotation   .slerp(targetR, alpha);
      if (targetS != null) scale      .lerp (targetS, alpha);
      return this;
    }
    
    Matrix4 toMatrix4(final Matrix4 out) {
      return out.set(translation, rotation, scale);
    }
  }
  
  
  protected void end() {
    if (bound == null) throw new GdxRuntimeException(
      "You must call begin() first"
    );
    for (Entry <Node, Transform> entry : transforms.entries()) {
      final Node node = entry.key;
      final Transform t = entry.value;
      t.toMatrix4(bound.boneFor(node));
    }
    bound = null;
  }
}







