

package graphics.common;

import static graphics.common.GL.*;

import org.lwjgl.opengl.GL11;

import graphics.jointed.* ;
import graphics.terrain.* ;
import graphics.cutout.* ;
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
import com.badlogic.gdx.math.Vector3;
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
	private long initTime ;
	private boolean doneLoad = false ;
	
	
	private Array<ModelInstance> modelSprites = new Array<ModelInstance>();
	private ModelBatch modelBatch;
	private AssetManager assets;
	private String assetFiles[] = {
		"models/Micovore.ms3d",
		"models/wall_corner.ms3d"
	};
	
	
	private CutoutsPass cutoutsPass ;
	private Array <CutoutSprite> allCutouts = new Array <CutoutSprite> () ;
	
	private TerrainSet terrain ;
	//  TODO:  Maybe the TerrainSet should initialise it's own default shader,
	//         closer to previous behaviour?
	private ShaderProgram terrainShader ;
	
	
	
	
	public void create() {
		
    environment = new Environment();
    environment.add(new DirectionalLight().set(
      0.8f, 0.8f, 0.8f,
      -1f, -0.8f, -0.2f
    ));
    environment.set(new ColorAttribute(
      ColorAttribute.AmbientLight,
      0.4f, 0.4f, 0.4f, 0.1f
    ));
    
		
		final float
		  wide = Gdx.graphics.getWidth(),
		  high = Gdx.graphics.getHeight() ;
		
		
		//
		//  TODO:  Move the basic setup functions here to the IsoCameraControl
		//  class.
		camera = new OrthographicCamera(20, high / wide * 20);
		camera.position.set(0, 50f, 100f) ;
		final Vector3 origin = new Vector3(0, 0, 0) ;
		camera.lookAt(origin);
		
		camera.rotateAround(origin, Vector3.Y, -45) ;
		camera.near = 0.1f;
		camera.far = 300f;
		camera.update();
		
		camControl = new IsoCameraControl(camera);
		
		
		Gdx.input.setInputProcessor(camControl);
		initTime = System.currentTimeMillis();
		
		reportVersion() ;
		setupTerrain() ;
		setupCutouts() ;
		setupSolids() ;
	}
	
	
	private void reportVersion() {
    I.say(
      "Please send me this info"+
      "\n--- GL INFO -----------"+
      "\n   GL_VENDOR: "+glGetString(GL_VENDOR)+
      "\n GL_RENDERER: "+glGetString(GL_RENDERER)+
      "\n  GL_VERSION: "+glGetString(GL_VERSION)+
      "\nGLSL_VERSION: "+glGetString(GL_SHADING_LANGUAGE_VERSION)+
      "\n-----------------------\n"
    ) ;
	}
	
	
	private void setupTerrain() {
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
	
	
	private void setupCutouts() {
	  cutoutsPass = new CutoutsPass() ;
	  final CutoutModel
	    modelA = CutoutModel.fromImage("buildings/bastion_L1.png", 7, 7),
	    modelB = CutoutModel.fromImage("buildings/bastion_L2.png", 7, 1),
	    modelC = CutoutModel.fromImage("buildings/bastion_L3.png", 7, 1),
	    modelG[][] = CutoutModel.fromImageGrid(
	      "buildings/old_flora_resize.png", 4, 4, 2, 2
	    ) ;
    
    final CutoutSprite SC = new CutoutSprite(modelC);
    SC.position.set(3.0f, 0, 3.0f);
    //BS.colour = Color.GREEN.toFloatBits();
    allCutouts.add(SC) ;
    
    final CutoutSprite SA = new CutoutSprite(modelA);
    SA.position.set(10.0f, 0, 10.0f);
    allCutouts.add(SA);
    
    final CutoutSprite SB = new CutoutSprite(modelB);
    SB.position.set(10.0f, 0, 3.0f);
    allCutouts.add(SB);
	  
    for (int n = 30 + Rand.index(20) ; n-- > 0 ;) {
      final CutoutModel TM = modelG[Rand.index(4)][Rand.index(4)] ;
      final CutoutSprite TS = new CutoutSprite(TM) ;
      TS.position.set(
        Rand.range(0, 15), 0, Rand.range(0, 15)
      ) ;
      allCutouts.add(TS) ;
    }
	}
	
	
	private void setupSolids() {
		modelBatch = new ModelBatch(new JointShading());
		
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
		
		camControl.update() ;
		
    Gdx.gl.glEnable(GL10.GL_DEPTH_TEST);
    Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
    Gdx.gl.glEnable(GL10.GL_BLEND);
    Gdx.gl.glDepthMask(true);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    
		glClearColor(1, 0, 0, 0) ;
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT) ;
		
		long totalTime = System.currentTimeMillis() - initTime ;
		float seconds = (float) (totalTime / 1000f) ;
		terrain.render(camera, terrainShader, seconds) ;
    //  TODO:  Just for testing purposes, remove later
    if (Math.random() < 1f / 60) {
      terrain.fog.liftAround(
        (int) (Math.random() * terrain.size),
        (int) (Math.random() * terrain.size),
        (int) (Math.sqrt(terrain.size) * (1 + Math.random()))
      );
    }
    glClear(GL_DEPTH_BUFFER_BIT) ;
    
    cutoutsPass.performPass(allCutouts, camera);
    //cutoutsPass.dispose();
    //cutoutsPass = new CutoutsPass();
		
    /*
    final float delta = Gdx.graphics.getDeltaTime() ;
		modelBatch.begin(camera);
		for (ModelInstance MI : modelSprites) {
			final JointSprite sprite = (JointSprite) MI ;
			sprite.updateAnim(delta) ;
			modelBatch.render(sprite, environment) ;
		}
		modelBatch.end();
		//*/
	}
	
	
	public void dispose() {
		terrain.dispose();
		cutoutsPass.dispose();
		modelSprites.clear();
    modelBatch.dispose();
		assets.dispose();
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






