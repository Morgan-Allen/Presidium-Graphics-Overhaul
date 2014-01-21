


package sf.gdx.terrain ;
import static com.badlogic.gdx.graphics.Texture.TextureFilter.* ;
import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.graphics.Pixmap.* ;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;



public class FogOverlay {
	

	
	final TerrainSet terrain ;
	private Pixmap drawnTo;
	protected Texture oldTex, newTex;
	private float oldTime = 0 ;
	
	
	FogOverlay(int size, TerrainSet terrain) {
		this.terrain = terrain ;
		
		drawnTo = new Pixmap(size, size, Format.RGBA4444);
		Pixmap.setBlending(Blending.None);
		drawnTo.setColor(Color.BLACK);
		drawnTo.fillRectangle(0, 0, size, size);
		
		oldTex = new Texture(drawnTo);
		oldTex.setFilter(Linear, Linear);
		newTex = new Texture(drawnTo);
		newTex.setFilter(Linear, Linear);
	}
	
	
	void dispose() {
		drawnTo.dispose();
		oldTex.dispose();
		newTex.dispose();
	}
	
	
	protected void applyToShader(ShaderProgram shader) {
		oldTex.bind(1);
		newTex.bind(2);
		shader.setUniformi("u_fog_old", 1);
		shader.setUniformi("u_fog_new", 2);
		shader.setUniformf("u_fogSize", terrain.size, terrain.size);
		shader.setUniformf("u_fogTime", oldTime % 1);
	}
	
	
	protected void checkBufferSwap(float newTime) {
		if (((int) oldTime) != ((int) newTime)) {
			final Texture temp = newTex ;
			newTex = oldTex ;
			oldTex = temp ;
			newTex.draw(drawnTo, 0, 0) ;
		}
		oldTime = newTime;
	}
	
	
	public void liftAround(int x, int y, int radius) {
		drawnTo.setColor(Color.WHITE);
		drawnTo.fillCircle(x, y, radius);
	}
}







