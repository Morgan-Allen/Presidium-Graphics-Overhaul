

import sf.gdx.* ;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;



public class Main {
	public static void main(String[] args) {
		LwjglApplicationConfiguration
		  config = new LwjglApplicationConfiguration();
		config.title = "SFCityBuilder2";
		config.useGL20 = true;
		config.vSyncEnabled = false;
		config.width = 800;
		config.height = 600;
		config.foregroundFPS = 120;
		config.backgroundFPS = 120;
		config.resizable = false;
		config.fullscreen = false;
		
		//cfg.depth = 0;
		//System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
		new LwjglApplication(new SFMain(), config);
	}
}
