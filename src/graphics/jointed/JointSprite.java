


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







