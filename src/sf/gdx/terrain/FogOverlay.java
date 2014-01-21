


package sf.gdx.terrain ;
import static com.badlogic.gdx.graphics.Texture.TextureFilter.* ;
import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.graphics.Pixmap.* ;



public class FogOverlay {
	

	
	final TerrainSet terrain ;
	private Pixmap fogpix;
	protected Texture tex;
	
	
	FogOverlay(int size, TerrainSet terrain) {
		this.terrain = terrain ;
		
		fogpix = new Pixmap(size, size, Format.RGBA4444);
		Pixmap.setBlending(Blending.None);
		
		for(int x=0; x<size; x++) {
			for(int z=0; z<size; z++) {
				// this is color 0x000000XX where XX is alpha, randomed 0-255
				int pix = (int) (Math.random() * 256); 
				
				fogpix.drawPixel(x,z,pix);
				
			}
		}
		
		tex = new Texture(fogpix);
		tex.setFilter(Linear, Linear);
	}

	
	
	//  TODO:  Update and blending methods have to be included here.
}





