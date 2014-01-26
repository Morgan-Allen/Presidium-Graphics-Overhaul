


package graphics.jointed ;
import com.badlogic.gdx.graphics.g3d.* ;
import com.badlogic.gdx.graphics.g3d.utils.* ;
import com.badlogic.gdx.graphics.g3d.model.* ;



public class JointSprite extends ModelInstance {
	
	
	
	final AnimationController animControl ;
	public float fog ;
	
	
	public JointSprite(Model model) {
		super(model) ;
		animControl = new AnimationController(this) ;
	}
	
	
	public boolean setAnimation(String animName) {
		for (Animation anim : model.animations) {
			if (anim.id.equals(animName)) {
				animControl.animate(animName, -1, 0.2f, null, 1) ;
				return true ;
			}
		}
		return false ;
	}
	
	
	public void updateAnim(float time) {
		animControl.update(time) ;
	}
}