

package src.graphics.kerai_src;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;



public class Main {
	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "SFCityBuilder";
		//cfg.useGL30 = false;
		cfg.useGL20 = true;
		cfg.vSyncEnabled = true;
		cfg.width = 1424;
		cfg.height = 900;
//		cfg.width = 1920;
//		cfg.height = 1080;
		cfg.foregroundFPS = -1;
		cfg.backgroundFPS = 30;
		cfg.resizable = false;
		cfg.fullscreen = false;
		//cfg.depth = 0;
		
		//Charsete
		
		//System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
		
		new LwjglApplication(new NewApp(), cfg);
	}
}
