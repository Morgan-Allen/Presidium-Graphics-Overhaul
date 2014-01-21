

package sf.gdx.terrain ;
import util.Coord;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import util.* ;


//
//  TODO:  This might be a good place to stick any fancy texture-atlas creation-
//  as such, I've stuck the old TerTileLoad code in down below.  Or maybe merge
//  this with the TerrainPattern class?


public class LayerType implements TileConstants {
	
	
	final Texture texture ;
	final int layerID ;
	final boolean innerFringe ;
	final private static boolean near[] = new boolean[8] ;
	
	
	protected LayerType(String texName, boolean innerFringe, int layerID) {
		this.innerFringe = innerFringe ;
		this.layerID = layerID ;
		//  ...This is a *lot* more concise.  Thanks, kerai
		this.texture = new Texture(Gdx.files.internal(texName));
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
	}
	
	
	protected boolean maskedAt(int tx, int ty, TerrainSet terrain) {
		//  TODO:  For the moment, I'm assuming that the only inner-fringe
		//  terrain layer is going to be roads.  This assumption is invalid and
		//  will require correction later.
		try {
			if (innerFringe) return terrain.paveCount[tx][ty] > 0 ;
			else return terrain.layerIndices[tx][ty] == layerID ;
		}
		catch (ArrayIndexOutOfBoundsException e) { return false ; }
	}
	
	
	protected void addFringes(
			int tx, int ty, TerrainSet terrain,
			Batch <Coord> gridBatch, Batch <float[]> textBatch
	) {
		final int ID = terrain.layerIndices[tx][ty] ;
		final boolean masked = maskedAt(tx, ty, terrain) ;
		if (innerFringe && ! masked) return ;
		if (ID >= layerID && ! innerFringe) {
			if (ID == layerID) {
				//  TODO:  Use some variation here...
				gridBatch.add(new Coord(tx, ty)) ;
				textBatch.add(LayerPattern.OUTER_FRINGE_CENTRE) ;
			}
			return ;
		}
		
		for (int n : N_INDEX) {
			final int x = tx + N_X[n], y = ty + N_Y[n] ;
			near[n] = maskedAt(x, y, terrain) ;
		}
		final float fringes[][] = innerFringe ?
			LayerPattern.innerFringeUV(near) :
			LayerPattern.outerFringeUV(near) ;
		
		if (fringes != null) for (float UV[] : fringes) if (UV != null) {
			gridBatch.add(new Coord(tx, ty)) ;
			textBatch.add(UV) ;
		}
	}
}






/*
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
                type.regions.put(b1111, putTile(pix, 0));
                
                types.add(type);
        }
        
        
        public void loadTiles(FileHandle handle) {
                Pixmap pix = new Pixmap(handle);
                
                TerrainType type = new TerrainType();
                type.regions.put(b1111, putTile(pix, 0));
                
                type.regions.put(b1000, putTile(pix, 1));
                type.regions.put(b0100, putTile(pix, 2));
                type.regions.put(b0010, putTile(pix, 3));
                type.regions.put(b0001, putTile(pix, 4));

                type.regions.put(b1001, putTile(pix, 5));
                type.regions.put(b0110, putTile(pix, 6));
                
                type.regions.put(b0111, putTile(pix, 7));
                type.regions.put(b1011, putTile(pix, 8));
                type.regions.put(b1101, putTile(pix, 9));
                type.regions.put(b1110, putTile(pix, 10));
                
                type.regions.put(b1100, putTile(pix, 11));
                type.regions.put(b0011, putTile(pix, 12));
                type.regions.put(b1010, putTile(pix, 13));
                type.regions.put(b0101, putTile(pix, 14));
                
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
//*/