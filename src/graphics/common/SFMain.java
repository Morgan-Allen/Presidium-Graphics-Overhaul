

package graphics.common;
import static graphics.common.GL.*;

import graphics.jointed.* ;
import graphics.terrain.* ;
import util.* ;


import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;




public class SFMain implements ApplicationListener {
	
	
	public static void main(String[] args) {
		LwjglApplicationConfiguration
		  config = new LwjglApplicationConfiguration();
		config.title = "SFCityBuilder2";
		config.useGL20 = true;
		config.vSyncEnabled = false;
		config.width = 800;
		config.height = 600;
		config.foregroundFPS = 120;
		config.backgroundFPS = 120;
		config.resizable = false;
		config.fullscreen = false;
		
		//cfg.depth = 0;
		//System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
		new LwjglApplication(new SFMain(), config);
	}
	
	
	
	private OrthographicCamera camera;
	private IsoCameraControl camControl;
	private Environment environment ;
	//private float totalTime = 0 ;
	private long initTime ;
	private boolean doneLoad = false ;
	
	
	private Array<ModelInstance> modelSprites = new Array<ModelInstance>();
	private ModelBatch modelBatch;
	private AssetManager assets;
	private String assetFiles[] = {
		"models/Micovore.ms3d",
		"models/wall_corner.ms3d"
	};
	
	
	private TerrainSet terrain ;
	//  TODO:  Maybe the TerrainSet should initialise it's own default shader,
	//         closer to previous behaviour?
	private ShaderProgram terrainShader ;
	
	
	
	
	public void create() {
		
		System.out.println("Please send me this info");
		System.out.println("--- GL INFO -----------");
		System.out.println("   GL_VENDOR: " + glGetString(GL_VENDOR));
		System.out.println(" GL_RENDERER: " + glGetString(GL_RENDERER));
		System.out.println("  GL_VERSION: " + glGetString(GL_VERSION));
		System.out.println("GLSL_VERSION: " + glGetString(GL_SHADING_LANGUAGE_VERSION));
		System.out.println("-----------------------");
		
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 0.1f));
		environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
		
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		camera = new OrthographicCamera(20, h/w * 20);
		
		camera.position.set(100f, 100f, 100f);
		camera.lookAt(0, 0, 0);
		camera.near = 0.1f;
		camera.far = 300f;
		camera.update();
		
		camControl = new IsoCameraControl(camera);
		Gdx.input.setInputProcessor(camControl);
		initTime = System.currentTimeMillis();

		setupTerrain() ;
		setupSprites() ;
	}
	
	
	
	private void setupTerrain() {
		//
		//  NOTE:  This is going to appear with the x/y axes flipped when
		//  rendered, but that's okay- all the actual simulation code will
		//  supply terrain data the right way.
		final byte indices[][] = {
			{ 2, 2, 2, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, },
			{ 2, 2, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, },
			{ 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, },
			{ 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, },
			{ 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, },
			{ 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, },
			{ 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, },
			{ 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, },
			{ 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, },
			{ 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, },
			{ 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, },
			{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, },
			{ 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, },
			{ 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, },
			{ 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, },
			{ 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 2, 1, 0, 0, 0, },
		} ;
		
		terrain = new TerrainSet(
			16, 16, indices, true, "tiles/",
			"ocean.2.gif",
			"meadows_ground.gif",
			"mesa_ground.gif"
		) ;
		
		for (int x = 10 ; x-- > 0 ;) terrain.maskPaving(x, 0, true) ;
		for (int y = 10 ; y-- > 0 ;) terrain.maskPaving(2, y, true) ;
		terrain.generateAllMeshes() ;
		
		terrainShader = new ShaderProgram(
			Gdx.files.internal("shaders/terrain.vert"),
			Gdx.files.internal("shaders/terrain.frag")
		) ;
		if(! terrainShader.isCompiled()) {
			throw new GdxRuntimeException("\n"+terrainShader.getLog()) ;
		}
	}
	
	
	private void setupSprites() {
		modelBatch = new ModelBatch(new JointShading());
		
		environment = new Environment();
		environment.add(new DirectionalLight().set(
			0.8f, 0.8f, 0.8f,
			-1f, -0.8f, -0.2f
		));
		environment.set(new ColorAttribute(
			ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 0.1f
		));
		
		assets = new AssetManager();
		assets.setLoader(
			Model.class, ".ms3d",
			new MS3DLoader(new InternalFileHandleResolver())
		);
		
		for (String file : assetFiles) {
			assets.load(file, Model.class);
		}
	}
	
	
	private void checkLoading() {
		assets.update() ;
		doneLoad = true ;
		int n = 0 ;
		for (String file : assetFiles) {
			n++ ;
			if (assets.isLoaded(file)) {
				final Model model = assets.get(file, Model.class);
				final JointSprite sprite = new JointSprite(model);
				sprite.transform.translate(0, 0, -5 * n) ;
				sprite.setAnimation("default") ;
				modelSprites.add(sprite) ;
			}
			else doneLoad = false ;
		}
	}
	
	
	public void render() {
		if (! doneLoad) {
			checkLoading() ;
			return ;
		}
		
		camControl.update();
		glClearColor(1, 0, 0, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		final float delta = Gdx.graphics.getDeltaTime() ;
		
		long totalTime = System.currentTimeMillis() - initTime ;
		float seconds = (float) (totalTime / 1000f) ;
		terrain.render(camera, terrainShader, seconds) ;
		I.say("Total time is: "+seconds) ;
		
		modelBatch.begin(camera);
		for (ModelInstance MI : modelSprites) {
			final JointSprite sprite = (JointSprite) MI ;
			sprite.updateAnim(delta) ;
			modelBatch.render(sprite, environment) ;
		}
		modelBatch.end();		
		
		//  TODO:  Just for testing purposes, remove later
		if (Math.random() < 1f / 60) {
			terrain.fog.liftAround(
				(int) (Math.random() * terrain.size),
				(int) (Math.random() * terrain.size),
				(int) (Math.sqrt(terrain.size) * (1 + Math.random()))
			);
		}
	}
	
	
	public void dispose() {
		terrain.dispose();
		modelSprites.clear();
		assets.dispose();
		modelBatch.dispose();
	}
	
	
	public void resume() {
	}
	
	
	public void resize(int width, int height) {
		camera.viewportWidth = 20;
		camera.viewportHeight = (float)height/width * 20;
		camera.update();
	}
	
	
	public void pause() {
	}
}






