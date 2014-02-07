

package graphics.common;
import org.lwjgl.input.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;



public class IsoCameraControl extends InputAdapter {
	
  
  final public float
    KTF   = 0.4f,  //key translation factor
    KRF   = 0.5f,  //key rotation factor (unused)
    MTF   = 20f,   //mouse translation factor
    MRF   = 360f ; //mouse rotation factor
  //  TODO:  Include some information about default rotation/view elevation,
  //  zoom levels, etc.
  
  
  final Camera camera ;
  final Vector3 target = new Vector3() ;
  
  private int button = -1;
  private float startX, startY;
  private Vector3 t1 = new Vector3(), t2 = new Vector3();
	
	
	
	public IsoCameraControl() {
    final float
      wide = Gdx.graphics.getWidth(),
      high = Gdx.graphics.getHeight();
    camera = new OrthographicCamera(20, (high / wide) * 20);
    
    camera.position.set(0, 50f, 100f);
    target.set(0, 0, 0);
    camera.lookAt(target);
    camera.rotateAround(target, Vector3.Y, -45);
    camera.near = 0.1f;
    camera.far = 300f;
    camera.update();
	}
	
	
	
	public void update() {
		if(Gdx.input.isKeyPressed(Keys.W)) {
			t1.set(camera.direction).y = 0;
			translate(t1.nor().scl(KTF));
		}
		
		if(Gdx.input.isKeyPressed(Keys.S)) {
			t1.set(camera.direction).y = 0;
			translate(t1.nor().scl(-KTF));
		}
		
		if(Gdx.input.isKeyPressed(Keys.A)) {
			t1.set(camera.direction).y = 0;
			float tmp = t1.z;
			t1.z = -t1.x;
			t1.x = tmp;
			translate(t1.nor().scl(KTF));
		}
		
		if(Gdx.input.isKeyPressed(Keys.D)) {
			t1.set(camera.direction).y = 0;
			float tmp = t1.z;
			t1.z = -t1.x;
			t1.x = tmp;
			translate(t1.nor().scl(-KTF));
		}
		camera.update();
	}
	
	
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (this.button < 0) {
			startX = screenX;
			startY = screenY;
			this.button = button;
		}
		return true;
	}

	
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (button == this.button)
			this.button = -1;
		return true;
	}
	
	
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		final float deltaX = (screenX - startX) / Gdx.graphics.getWidth();
		final float deltaY = (startY - screenY) / Gdx.graphics.getHeight();
		startX = screenX;
		startY = screenY;
		process(deltaX, deltaY);
		return true;
	}
  
  
  public boolean scrolled(int amount) {
    //camera.translate(t1.set(camera.direction).scl(amount * -1f));
    float am = amount > 0? 1.1f : 0.9f;
    camera.viewportHeight*=am;
    camera.viewportWidth*=am;
    return true;
  }
  
  
  protected void onScreenResize(int width, int height) {
    camera.viewportWidth = 20;
    camera.viewportHeight = (float) height/width * 20;
    camera.update();
  }
	
	
	
	private void process(float deltaX, float deltaY) {
		if (button == Buttons.RIGHT) {
			t1.set(camera.direction).crs(camera.up).y = 0f;
			camera.rotateAround(target, Vector3.Y, deltaX * -MRF);
		}
		if (button == Buttons.LEFT) {
			t1.set(camera.direction).y = 0;
			t1.nor();
			t2.set(t1).crs(Vector3.Y);
			translate(t1.scl(deltaY * -MTF).add(t2.scl(deltaX * -MTF)));
		}

		camera.update();
	}
	
	
	private void translate(Vector3 vec) {
		camera.translate(vec.scl(camera.viewportHeight * 0.08f));
		target.add(vec);
	}
}






