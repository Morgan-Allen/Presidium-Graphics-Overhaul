package sf.gdx;

import org.lwjgl.input.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;

public class RTSCameraControl extends InputAdapter {
	public int rotateButton = Buttons.RIGHT;
	public int translateButton = Buttons.LEFT;
	
	public float keyTranslateFactor = 0.4f;
	public float mouseTranslateFactor = 20f;
	public float mouseRotateFactor = 360f;

	private final Vector3 t1 = new Vector3();
	private final Vector3 t2 = new Vector3();
	
	public final Vector3 target = new Vector3();
	
	float zoom;
	
	public final Camera camera;
	private int button = -1;
	private float startX;
	private float startY;
	
	public RTSCameraControl(Camera cam) {
		this.camera = cam;
		
		camera.lookAt(target);
		camera.update();
	}
	
	public void update() {
		if(Gdx.input.isKeyPressed(Keys.W)) {
			t1.set(camera.direction).y = 0;
			translate(t1.nor().scl(keyTranslateFactor));
		}
		if(Gdx.input.isKeyPressed(Keys.S)) {
			t1.set(camera.direction).y = 0;
			translate(t1.nor().scl(-keyTranslateFactor));
		}
		if(Gdx.input.isKeyPressed(Keys.A)) {
			t1.set(camera.direction).y = 0;
			float tmp = t1.z;
			t1.z = -t1.x;
			t1.x = tmp;
			translate(t1.nor().scl(keyTranslateFactor));
		}
		if(Gdx.input.isKeyPressed(Keys.D)) {
			t1.set(camera.direction).y = 0;
			float tmp = t1.z;
			t1.z = -t1.x;
			t1.x = tmp;
			translate(t1.nor().scl(-keyTranslateFactor));
		}
		camera.update();
	}
	
	

	@Override
	public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		if (this.button < 0) {
			startX = screenX;
			startY = screenY;
			this.button = button;
		}
		return true;
	}

	
	@Override
	public boolean touchUp (int screenX, int screenY, int pointer, int button) {
		if (button == this.button)
			this.button = -1;
		return true;
	}
	
	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		final float deltaX = (screenX - startX) / Gdx.graphics.getWidth();
		final float deltaY = (startY - screenY) / Gdx.graphics.getHeight();
		startX = screenX;
		startY = screenY;
		
		process(deltaX, deltaY);
		return true;
	}

	private void process(float deltaX, float deltaY) {
		if(button == rotateButton) {
			t1.set(camera.direction).crs(camera.up).y = 0f;
			//camera.rotateAround(target, t1.nor(), deltaY * mouseRotateFactor);
			camera.rotateAround(target, Vector3.Y, deltaX * -mouseRotateFactor);
		}
		if(button == translateButton) {
			t1.set(camera.direction).y = 0;
			t1.nor();
			t2.set(t1).crs(Vector3.Y);
			
			translate(t1.scl(deltaY * -mouseTranslateFactor).add(t2.scl(deltaX * -mouseTranslateFactor)));
			
		}

		camera.update();
	}
	
	private void translate(Vector3 vec) {
		camera.translate(vec.scl(camera.viewportHeight * 0.08f));
		target.add(vec);
	}
	
	@Override
	public boolean scrolled(int amount) {
		//camera.translate(t1.set(camera.direction).scl(amount * -1f));
		float am = amount > 0? 1.1f : 0.9f;
		camera.viewportHeight*=am;
		camera.viewportWidth*=am;
		return true;
	}
}
