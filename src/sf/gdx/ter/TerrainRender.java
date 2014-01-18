

package sf.gdx.ter;
import static gl.GL.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;



public class TerrainRender {
	
	
	ShaderProgram tshader;
	TerrainChunk chunk; // should be composed of more chunks in the future
	// if we want bigger terrain or something
	// currently one chunk is max 128x128
	// but it can be much bigger if I remove indices.
	// I'll do that later
	Camera camera;
	private Texture texture;
	
	
	public TerrainRender(Camera cam) {
		tshader = new ShaderProgram(
			Gdx.files.internal("shaders/terrain.vert"),
			Gdx.files.internal("shaders/terrain.frag")
		);
		
		TerTileLoad load = new TerTileLoad();
		camera = cam;
		chunk = new TerrainChunk(128, 128);
		chunk.mesh.setAutoBind(true);
		chunk.types = load.load();
		
		load.update();
		texture = load.texture;
		
		chunk.tiledata[3][3] = 1;
		chunk.tiledata[3][4] = 1;
		chunk.tiledata[3][5] = 2;
		
		System.out.println("generating mesh");
		chunk.generateMesh();
		if(!tshader.isCompiled()) {
			throw new GdxRuntimeException(tshader.getLog());
		}
	}
	
	
	public void render(Texture fog) {
		
		Gdx.gl.glEnable(GL10.GL_DEPTH_TEST);
		Gdx.gl.glDepthFunc(GL20.GL_LESS);
		Gdx.gl.glEnable(GL10.GL_BLEND);
		Gdx.gl.glDepthMask(true);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	
		texture.bind(0);
		if (fog != null) fog.bind(1);
		
		tshader.begin();
		tshader.setUniformi("u_texture", 0);
		tshader.setUniformi("u_fog", 1);
		tshader.setUniformi("u_fogFlag", fog == null ? GL_FALSE : GL_TRUE );
		
		if (fog != null) tshader.setUniformi(
			"u_fogSize",
			fog.getWidth(),
			fog.getHeight()
		);
		
		tshader.setUniformMatrix("u_camera", camera.combined);
		chunk.mesh.render(tshader, GL20.GL_TRIANGLES);
		tshader.end();
	}

}



