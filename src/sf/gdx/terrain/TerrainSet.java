

package sf.gdx.terrain;
import util.* ;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;



public class TerrainSet {
	
	final static int
	  DEFAULT_CHUNK_SIZE = 64,
	  MAX_CHUNK_SIZE     = 128 ;
	
	final int size, numLayers ;
	final int chunkSize, chunkGrid ;
	final byte layerIndices[][], paveCount[][] ;
	
	TerrainChunk chunks[][][] ;
	LayerType layers[] ;
	Texture fog ;
	
	
	public TerrainSet(
		int size, int chunkSize,
		byte layerIndices[][],
		String texDir, String... texLayerNames
	) {
		//  Basic sanity checks first-
		if (chunkSize > MAX_CHUNK_SIZE) {
			throw new RuntimeException("Chunks too big!") ;
		}
		else if (chunkSize <= 0) chunkSize = DEFAULT_CHUNK_SIZE ;
		
		if (layerIndices.length != size || layerIndices[0].length != size) {
			throw new RuntimeException("Grid size does not match array!") ;
		}
		
		//  Appropriate dimensions calculated-
		this.numLayers = texLayerNames.length ;
		this.size = size ;
		this.chunkSize = chunkSize ;
		this.chunkGrid = (int) Math.ceil(size / chunkSize) ;
		this.layerIndices = layerIndices ;
		this.paveCount = new byte[size][size] ;
		this.fog = null ;
		
		//  TODO:  Include an initFog() method?  Look back at SFMain...
		this.layers = new LayerType[numLayers + 1] ;
		for (int n = 0 ; n < numLayers ; n++) {
			final String name = ""+texDir+texLayerNames[n] ;
			layers[n] = new LayerType(name, false, n) ;
		}
		//  TODO:  Bit of a temporary hack here...
		final String roadName = ""+texDir+"road_map_new.gif" ;
		layers[numLayers] = new LayerType(roadName, true, numLayers) ;
		
		this.chunks = new TerrainChunk[chunkGrid][chunkGrid][layers.length] ;
	}
	
	
	public void maskPaving(int x, int y, boolean is) {
		paveCount[x][y] += is ? 1 : -1 ;
		//  TODO:  This needs to flag the underlying terrain chunks for mesh
		//         updates...
	}
	
	
	public void generateAllMeshes() {
		for (Coord c : Visit.grid(0, 0, chunkGrid, chunkGrid, 1)) {
			for (LayerType layer : layers) {
				final TerrainChunk chunk = new TerrainChunk(
					chunkSize, chunkSize,
					c.x * chunkSize, c.y * chunkSize,
					this, layer.layerID
				) ;
				chunk.generateMesh() ;
				chunks[c.x][c.y][layer.layerID] = chunk ;
			}
		}
	}
	
	
	//
	//  TODO:  You might want to build in some basic frustrum culling here-
	//  or a set of visible terrain areas, and check for overlap?
	
	public void render(Camera camera, ShaderProgram tshader) {
		Gdx.gl.glEnable(GL10.GL_DEPTH_TEST);
		Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
		Gdx.gl.glEnable(GL10.GL_BLEND);
		Gdx.gl.glDepthMask(true);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		
		tshader.begin();
		tshader.setUniformMatrix("u_camera", camera.combined);
		for (LayerType type : layers) {
			layers[type.layerID].texture.bind(0) ;
			for (Coord c : Visit.grid(0, 0, chunkGrid, chunkGrid, 1)) {
				final TerrainChunk chunk = chunks[c.x][c.y][type.layerID];
				chunk.mesh.render(tshader, GL20.GL_TRIANGLES);
			}
		}
		tshader.end();
	}
}



//  TODO:  Try and restore/integrate this up above.
/*
fogpix = new Pixmap(128, 128, Format.RGBA8888);

for(int x=0; x<fogpix.getWidth(); x++) {
	for(int z=0; z<fogpix.getHeight(); z++) {
		int dupa = (int) (Math.random() * 255);
		//System.out.println(dupa);
		if(z>8)
			dupa = 180;
		if(z>9)
			dupa = 255;
		fogpix.drawPixel(x, z, dupa);
	}
}

fogtex = new Texture(fogpix);
fogtex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
fogtex.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
fogtex = null;
//*/

/*
texture.bind(0);
if (fog != null) fog.bind(1);
tshader.begin();
tshader.setUniformi("u_texture", 0);

if (fog != null) {
	tshader.setUniformi("u_fog", 1);
	tshader.setUniformi("u_fogFlag", fog == null ? GL_FALSE : GL_TRUE);
	tshader.setUniformi(
		"u_fogSize",
		fog.getWidth(),
		fog.getHeight()
	);
}
chunk.mesh.render(tshader, GL20.GL_TRIANGLES);
tshader.end();
//*/





