


package sf.gdx;
import static gl.GL.*;
import sf.gdx.ms3d.MS3DLoader;
import sf.gdx.terrain.TerrainSet;

import com.badlogic.gdx.* ;
import com.badlogic.gdx.assets.* ;
import com.badlogic.gdx.graphics.* ;
import com.badlogic.gdx.graphics.g3d.* ;
import com.badlogic.gdx.graphics.g3d.attributes.* ;
import com.badlogic.gdx.graphics.g3d.environment.* ;
import com.badlogic.gdx.utils.* ;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;


import com.badlogic.gdx.graphics.g3d.shaders.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.graphics.g3d.utils.*;



public class SFMain implements ApplicationListener {
	
	
	public OrthographicCamera cam;
	public RTSCameraControl rtscam;
	public AssetManager assets;
	Environment env;
	
	
	public Array<ModelInstance> instances = new Array<ModelInstance>();
	public boolean loading;
	private AnimationController ctrl1;
	private AnimationController ctrl4;
	//private String model1 = "wall_corner.ms3d";
	private String model4 = "micovore/Micovore.ms3d";
	ModelBatch celbatch;
	//public Texture fogtex;
	//private Shader outline;
	
	
	private TerrainSet terrain ;
	//  TODO:  Maybe the TerrainSet should initialise it's own default shader,
	//         closer to previous behaviour?
	private ShaderProgram terrainShade ;
	
	
	
	
	public void create() {
		env = new Environment();
		env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 0.9f));
		env.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
		
		
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		cam = new OrthographicCamera(20, h/w * 20);
		
		cam.position.set(100f, 100f, 100f);
		cam.lookAt(0, 0, 0);
		cam.near = 0.1f;
		cam.far = 300f;
		cam.update();
		
		rtscam = new RTSCameraControl(cam);
		Gdx.input.setInputProcessor(rtscam);
		
		
		
		setupSprites() ;
		setupTerrain() ;
	}
	
	
	private void setupSprites() {
		assets = new AssetManager();
		assets.setLoader(
			Model.class, ".ms3d",
			new MS3DLoader(new InternalFileHandleResolver())
		);
		//assets.load(model1, Model.class);
		assets.load(model4, Model.class);
		loading = true;
		
		DefaultShaderProvider provider = new DefaultShaderProvider() {
			@Override
			protected Shader createShader(Renderable renderable) {
				DefaultShader shad = new DefaultShader(renderable, config) ;
				//DefaultShader shad = new FogDefault(renderable, config, fogtex);
				//System.out.println("----------------------------");
				//System.out.println(shad.program.getVertexShaderSource());
				//System.out.println("----------------------------");
				return shad;
			}
		};
		
		provider.config.numBones = 20;
		provider.config.fragmentShader = Gdx.files.internal("shaders/default.frag").readString();
		provider.config.vertexShader = Gdx.files.internal("shaders/default.vert").readString();
		celbatch = new ModelBatch(provider);

		//shad = new BlackShader();
		//outline = new OutlineShader();
		//outline.init();
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
			16, 16, indices, "tiles/",
			"ocean.2.gif",
			"meadows_ground.gif",
			"mesa_ground.gif"
		) ;
		terrainShade = new ShaderProgram(
			Gdx.files.internal("shaders/terrain.vert"),
			Gdx.files.internal("shaders/terrain.frag")
		) ;
		if(! terrainShade.isCompiled()) {
			throw new GdxRuntimeException("\n"+terrainShade.getLog()) ;
		}
	}
	
	
	
	private void doneLoading() {
		/*
		if(assets.isLoaded(model1)) {
			Model kutas = assets.get(model1, Model.class);
			Material mat = kutas.materials.get(0);
			BlendingAttribute bl = (BlendingAttribute) mat.get(BlendingAttribute.Type);
			System.out.println("OPACITY: " + bl.opacity);
			bl.opacity = 1f;
			bl.blended = false;
			
			ModelInstance k = new ModelInstance(kutas);
			k.transform.translate(0, 0, 0);
			instances.add(k);
		}
		//*/
		if(assets.isLoaded(model4)) {
			Model kutas = assets.get(model4, Model.class);
			
			ModelInstance k = new ModelInstance(kutas);
			k.transform.translate(10, 0, 10);
			k.transform.scale(0.1f, 0.1f, 0.1f);
			
			instances.add(k);
			ctrl4 = new AnimationController(k);
			ctrl4.animate(k.animations.get(0).id, -1, 0.2f, null, 1);
		}
		loading = false;
	}
	
	
	public void render() {
		if (loading && assets.update())
			doneLoading();
		rtscam.update();
		
		glClearColor(1, 0, 0, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		if (ctrl1 != null) {
			ctrl1.update(Gdx.graphics.getDeltaTime());
		}
		if (ctrl4 != null) {
			ctrl4.update(Gdx.graphics.getDeltaTime());
		}

		
		//*
		celbatch.begin(cam);
		for (ModelInstance instance : instances) {
			celbatch.render(instance, env);
			//celbatch.render(instance, outline);
		}
		celbatch.end();
		// rendering outlines
		
		/*
		celbatch.begin(cam);
		for (ModelInstance instance : instances) {
			celbatch.render(instance, outline);
		}
		celbatch.end();
		//*/
		
		// from pixmap to texture
		// you can change fog pixmap here
		// and then send it to texture
		
		//if(fogtex != null) fogtex.draw(fogpix, 0, 0);
		terrain.render(cam, terrainShade) ;
	}
	
	
	public void dispose() {
		//celbatch.dispose();
		instances.clear();
		assets.dispose();
	}
	
	
	public void resume() {
	}
	
	
	public void resize(int width, int height) {
		cam.viewportWidth = 20;
		cam.viewportHeight = (float)height/width * 20;
		cam.update();
	}
	
	
	public void pause() {
	}
}




