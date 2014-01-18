


package sf.gdx.ter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;



/**
 * this is very temporal
 */

public class TerTileLoad {
	
	
	Pixmap atlas;
	public Texture texture;
	
	public Array<TerrainType> types = new Array<TerrainType>(TerrainType.class);
	int curx = 1;
	int cury = 1;
	
	
	public TerrainType[] load() {
		
		atlas = new Pixmap(512, 512, Format.RGBA4444);
		texture = new Texture(atlas);
		texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		
		loadTile(Gdx.files.internal("tiles/water.png"));
		loadTiles(Gdx.files.internal("tiles/dirt.png"));
		loadTiles(Gdx.files.internal("tiles/grass.png"));
		
		return types.toArray();
	}
	
	
	public void loadTile(FileHandle handle) {
		Pixmap pix = new Pixmap(handle);
		
		TerrainType type = new TerrainType();
		type.regions.put(0, putTile(pix, 0));
		
		types.add(type);
	}
	
	
	public void loadTiles(FileHandle handle) {
		Pixmap pix = new Pixmap(handle);
		
		TerrainType type = new TerrainType();
		type.regions.put(0, putTile(pix, 0));
		
		type.regions.put(1, putTile(pix, 1));
		type.regions.put(2, putTile(pix, 2));
		type.regions.put(3, putTile(pix, 3));
		type.regions.put(4, putTile(pix, 4));
		
		type.regions.put(5, putTile(pix, 5));
		type.regions.put(6, putTile(pix, 6));
		
		type.regions.put(7, putTile(pix, 7));
		type.regions.put(8, putTile(pix, 8));
		type.regions.put(9, putTile(pix, 9));
		type.regions.put(10, putTile(pix, 10));
		
		type.regions.put(11, putTile(pix, 11));
		type.regions.put(12, putTile(pix, 12));
		type.regions.put(13, putTile(pix, 13));
		type.regions.put(14, putTile(pix, 14));
		
		types.add(type);
	}
	
	
	private TextureRegion putTile(Pixmap from, int num) {
		if(curx+13 >= atlas.getWidth()) {
			cury+=13;
			curx=1;
		}
		int fromx = num * 13 + 1;
		Pixmap.setBlending(Blending.None);
		atlas.drawPixmap(from, fromx, 1, 12, 12, curx, cury, 12, 12);
		TextureRegion reg =  new TextureRegion(texture, curx, cury, 12, 12);
		curx+=13;
		return reg;
	}
	
	
	public void update() {
		texture.draw(atlas, 0, 0);
	}
}

