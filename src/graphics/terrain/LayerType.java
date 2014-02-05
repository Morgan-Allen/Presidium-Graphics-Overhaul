

package graphics.terrain ;
import util.* ;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture;




public class LayerType implements TileConstants {
	
	
	final Texture texture ;
	final int layerID ;
	final boolean innerFringe ;
	final private static boolean near[] = new boolean[8] ;
	
	
	protected LayerType(String texName, boolean innerFringe, int layerID) {
		this.innerFringe = innerFringe ;
		this.layerID = layerID ;
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





