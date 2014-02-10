

package graphics.common;

import static graphics.common.GL.*;
import graphics.jointed.* ;
import graphics.terrain.* ;
import graphics.cutout.* ;
import graphics.widgets.* ;
import util.* ;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;


import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.assets.* ;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.*;




public class SFMain implements ApplicationListener {
	
	
	private IsoCameraControl camControl;
	private Environment environment ;
	private long initTime ;
	private boolean doneLoad = false ;
	
	
	private AssetManager assets;
	private String solidFiles[];
	private Array<ModelInstance> modelSprites = new Array<ModelInstance>();
	private ModelBatch modelBatch;
	
	
	private CutoutsPass cutoutsPass ;
	private Array <CutoutSprite> allCutouts = new Array <CutoutSprite> () ;
	
	private TerrainSet terrain ;
	//  TODO:  Maybe the TerrainSet should initialise it's own default shader,
	//         closer to previous behaviour?
	private ShaderProgram terrainShader ;
	
	private HUD UI ;
	
	
	
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
    
		camControl = new IsoCameraControl();
		Gdx.input.setInputProcessor(camControl);
		initTime = System.currentTimeMillis();
		
    assets = new AssetManager();
		
		reportVersion() ;
		setupTerrain() ;
		setupCutouts() ;
		setupSolids() ;
		setupHUD();
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
		
		final String path = "models/", xml = "FaunaModels.xml";
	  final String assetFiles[] = {
	    "Micovore.ms3d",
	    "wall_corner.ms3d"
	  };
	  final String xmlNames[] = {
	    "Micovore",
	    null
	  };
	  this.solidFiles = new String[assetFiles.length] ;
	  for (int i = solidFiles.length ; i-- > 0;) {
	    solidFiles[i] = path+""+assetFiles[i];
	  }
		
		assets.setLoader(
			Model.class, ".ms3d",
			new MS3DLoader(new InternalFileHandleResolver())
		);
		
		for (int i = assetFiles.length ; i-- > 0 ;) {
		  MS3DLoader.beginLoading(path, assetFiles[i], xml, xmlNames[i], assets);
		}
	}
	
	
	private void setupHUD() {
    UI = new HUD();
    
    Button button = new Button(UI, "UI/arcade_button.png", "Basic button!");
    button.absBound.set(20, 20, 100, 100);
    button.attachTo(UI);
    
    Text text = new Text(UI, Text.INFO_FONT);
    text.relBound.set(0, 0.5f, 1, 0.5f);
    text.setText("Experimental text display...");
    //text.setText("...");
    text.attachTo(UI);
	}
	
	
	
	private void checkLoading() {
		assets.update() ;
		doneLoad = true ;
		int n = 0 ;
		for (String file : solidFiles) {
			n++ ;
			if (assets.isLoaded(file)) {
				final Model model = assets.get(file, Model.class);
				final JointSprite sprite = new JointSprite(model);
				sprite.transform.translate(0, 0, -5 * n) ;
				//sprite.setAnimation("walk") ;
				modelSprites.add(sprite) ;
			}
			else doneLoad = false ;
		}
	}
	
	
	public void render() {
		if (! doneLoad) {
      //  TODO:  Throw up a loading screen or something here.
			checkLoading() ;
			return ;
		}
		
		camControl.update();
		final Camera camera = camControl.camera;
		
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
		
    //final float delta = Gdx.graphics.getDeltaTime() ;
		modelBatch.begin(camera);
		for (ModelInstance MI : modelSprites) {
			final JointSprite sprite = (JointSprite) MI ;
			sprite.updateAnim("move", seconds % 1) ;
			modelBatch.render(sprite, environment) ;
		}
		modelBatch.end();
		
		UI.updateMouse();
		UI.renderHUD();
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
	  camControl.onScreenResize(width, height) ;
	}
	
	
	public void pause() {
	}
  
	
  
  public static void main(String[] args) {
    final LwjglApplicationConfiguration
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
    //config.depth = 0;
    new LwjglApplication(new SFMain(), config);
  }
}






