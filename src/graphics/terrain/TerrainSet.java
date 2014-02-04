

package graphics.terrain;
import util.* ;
import static graphics.common.GL.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;



public class TerrainSet {
	
	final static int
	  DEFAULT_CHUNK_SIZE = 64,
	  MAX_CHUNK_SIZE     = 128 ;
	
	final public int size, numLayers ;
	final int chunkSize, chunkGrid ;
	final byte layerIndices[][], paveCount[][] ;
	
	TerrainChunk chunks[][][] ;
	LayerType layers[] ;
	final public FogOverlay fog ;
	
	
	public TerrainSet(
		int size, int chunkSize,
		byte layerIndices[][], boolean fogged,
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
		
		//  Set up reference data for layer types-
		this.layerIndices = layerIndices ;
		this.paveCount = new byte[size][size] ;
		this.fog = fogged ? new FogOverlay(size, this) : null ;
		
		this.layers = new LayerType[numLayers + 1] ;
		for (int n = 0 ; n < numLayers ; n++) {
			final String name = ""+texDir+texLayerNames[n] ;
			layers[n] = new LayerType(name, false, n) ;
		}
		//  TODO:  Bit of a temporary hack here...
		final String roadName = ""+texDir+"road_map_new.gif" ;
		layers[numLayers] = new LayerType(roadName, true, numLayers) ;
		
		//  And finally, the bite-size terrain chunks that actually get
		//  rendered on a need-to-see basis-
		this.chunks = new TerrainChunk[chunkGrid][chunkGrid][layers.length] ;
	}
	
	
	public void dispose() {
		//TODO:  Dispose of layers as well?
		if (fog != null) fog.dispose();
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
	
	//  TODO:  Definitely have the shader program specified internally.
	
	
	private ShaderProgram setupShader() {
		return null;
	}
	
	
	public void render(Camera camera, ShaderProgram shader, float time) {
		shader.begin();
		shader.setUniformMatrix("u_camera", camera.combined);
		
		if (fog != null) {
			fog.applyToShader(shader) ;
			shader.setUniformi("u_fogFlag", GL_TRUE);
		}
		else shader.setUniformi("u_fogFlag", GL_FALSE);
		
		for (LayerType type : layers) {
			layers[type.layerID].texture.bind(0) ;
			for (Coord c : Visit.grid(0, 0, chunkGrid, chunkGrid, 1)) {
				final TerrainChunk chunk = chunks[c.x][c.y][type.layerID];
				chunk.mesh.render(shader, GL20.GL_TRIANGLES);
			}
		}
		
		shader.end();
		if (fog != null) fog.checkBufferSwap(time);
	}
}

