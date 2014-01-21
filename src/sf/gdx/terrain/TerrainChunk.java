


package sf.gdx.terrain;
//import java.util.Arrays;
import com.badlogic.gdx.graphics.* ;
//import com.badlogic.gdx.graphics.g2d.TextureRegion;
import util.*;
import java.util.Iterator ;



public class TerrainChunk implements TileConstants {
	
	
	final int width, height, gridX, gridY ;
	final TerrainSet belongs ;
	final int layerID ;
	
	protected float[] vertices;
	protected short[] indices;
	protected Mesh mesh;
	
	
	public TerrainChunk(
		int width, int height,
		int gridX, int gridY,
		TerrainSet belongs, int layerID
	) {
		this.width = width;
		this.height = height;
		this.gridX = gridX;
		this.gridY = gridY;
		this.belongs = belongs;
		this.layerID = layerID;
	}
	
	
	protected void generateMesh() {
		
		//  First of all, compile a list of all occupied tiles and their
		//  associated UV fringing, based on the position of any adjacent tiles
		//  with the same layer assignment.
		
		final Batch <Coord> gridBatch = new Batch <Coord> () ;
		final Batch <float[]> textBatch = new Batch <float[]> () ;

		final boolean near[] = new boolean[8] ;
		for (Coord c : Visit.grid(gridX, gridY, width, height, 1)) {
			final int ID = belongs.layerIndices[c.y][c.x] ;
			
			if (ID == layerID) {
				//  TODO:  Use some variation here...
				gridBatch.add(new Coord(c)) ;
				textBatch.add(TerrainPattern.OUTER_FRINGE_CENTRE) ;
				continue ;
			}
			if (ID > layerID) continue ;
			
			for (int n : N_INDEX) try {
				final int x = c.x + N_X[n], y = c.y + N_Y[n] ;
				final int sample = belongs.layerIndices[y][x] ;
				near[n] = sample == layerID ;
			}
			catch (ArrayIndexOutOfBoundsException e) { near[n] = false ; }
			
			final float fringes[][] = TerrainPattern.outerFringeUV(near) ;
			if (fringes != null) for (float UV[] : fringes) if (UV != null) {
				gridBatch.add(new Coord(c)) ;
				textBatch.add(UV) ;
			}
		}

		//  We have 4 vertices per tile and 2 tiles per face.
		//  Each vertex requires 3 floats for geometry and 2 for tex coords.
		//  Each face requires 3 vertex indices.
		
		final int numTiles = gridBatch.size() ;
		vertices = new float[numTiles * 4 * (3 + 2)] ;
		indices = new short[numTiles * 2 * 3] ;
		final Iterator
		  iterV = gridBatch.iterator(),
		  iterT = textBatch.iterator() ;
		I.say("Total tiles accumulated: "+numTiles) ;
		
		//  Then we just fill up the arrays with the appropriate concatenation
		//  of data-
		for (int n = 0, pointV = 0, pointI = 0 ; n < numTiles ; n++) {
			//  TODO:  Implement true height maps?
			final float TILE_HIGH = 0 ;
			final Coord coord = (Coord) iterV.next() ;
			final float VP[] = TerrainPattern.VERT_PATTERN ;
			final float[] UV = (float[]) iterT.next() ;
			
			for (int c = 0, p = 0, t = 0 ; c < 4 ; c++) {
				vertices[pointV++] = VP[p++] + coord.x - 0.5f ;
				vertices[pointV++] = VP[p++] + TILE_HIGH ;
				vertices[pointV++] = VP[p++] + coord.y - 0.5f ;
				vertices[pointV++] = UV[t++] ;//* texW ;
				vertices[pointV++] = UV[t++] ;//* texH ;
			}
			for (float i : TerrainPattern.INDEX_PATTERN) {
				indices[pointI++] = (short) ((n * 4) + i) ;
			}
		} ;
		
		//  Finally set up the mesh object itself, ready for rendering-
		
		mesh = new Mesh(
			true, vertices.length, indices.length,
			VertexAttribute.Position(),
			VertexAttribute.TexCoords(0)
		) ;
		mesh.setVertices(vertices) ;
		mesh.setIndices(indices) ;
	}
}

