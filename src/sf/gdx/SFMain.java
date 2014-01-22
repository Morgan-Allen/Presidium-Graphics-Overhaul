


package sf.gdx;
import static gl.GL.*;
import static com.badlogic.gdx.graphics.Texture.TextureFilter.*;


import sf.gdx.ms3d.MS3DLoader;
import sf.gdx.terrain.TerrainSet;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;



public class SFMain implements ApplicationListener {
	
	
	public OrthographicCamera cam;
	public RTSCameraControl rtscam;
	public AssetManager assets;
	Environment env;
	private float time;
	
	
	public Array<ModelInstance> instances = new Array<ModelInstance>();
	public boolean loading;
	private AnimationController ctrl4;
	private String model1 = "wall_corner.ms3d";
	//private String model4 = "micovore/Micovore.ms3d";
	ModelBatch celbatch;
	//private Shader outline;
	
	
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
		
		env = new Environment();
		env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 0.1f));
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
		assets.setLoader(Model.class, ".ms3d", new MS3DLoader(new InternalFileHandleResolver()));
		assets.load(model1, Model.class);
		//assets.load(model4, Model.class);
		loading = true;
		
		DefaultShaderProvider provider = new DefaultShaderProvider();
		
		provider.config.numBones = 20;
		//provider.config.fragmentShader = Gdx.files.internal("shaders/default.frag").readString();
		//provider.config.vertexShader = Gdx.files.internal("shaders/default.vert").readString();
		celbatch = new ModelBatch(provider);

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
		
		for (int x = 10 ; x-- > 0 ;) terrain.maskPaving(x, 2, true) ;
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
	
	
	
	private void doneLoading() {
		
		if(assets.isLoaded(model1)) {
			Model kutas = assets.get(model1, Model.class);
			Material mat = kutas.materials.get(0);
			BlendingAttribute bl = (BlendingAttribute) mat.get(BlendingAttribute.Type);
			System.out.println("OPACITY: " + bl.opacity);
			bl.opacity = 1f;
			bl.blended = false;
			
			{
				ModelInstance k = new ModelInstance(kutas);
				
				// oh great there is some bug... either in model loading or in rendering.
				// scaling is off (it doubles) and I'll have to find why
				k.transform.setToTranslation(0.25f,0,0.25f);
				instances.add(k);
			}
			
		}
//		if(assets.isLoaded(model4)) {
//			Model kutas = assets.get(model4, Model.class);
//			
//			ModelInstance k = new ModelInstance(kutas);
//			k.transform.translate(10, 0, 10);
//			k.transform.scale(0.1f, 0.1f, 0.1f);
//			
//			instances.add(k);
//			ctrl4 = new AnimationController(k);
//			ctrl4.animate(k.animations.get(0).id, -1, 0.2f, null, 1);
//		}
		loading = false;
	}
	
	
	public void render() {
		if (loading && assets.update())
			doneLoading();
		rtscam.update();
		
		glClearColor(1, 0, 0, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		if (ctrl4 != null) {
			ctrl4.update(Gdx.graphics.getDeltaTime());
		}

		
		celbatch.begin(cam);
		for (ModelInstance instance : instances) {
			celbatch.render(instance, env);
		}
		celbatch.end();
		
		
		terrain.render(cam, terrainShader, time) ;
		time += 1f / 60 ;
		
		//  TODO:  Just for testing purposes, remove later
		if (Math.random() < 1f / 60) {
			terrain.fog.liftAround(
				(int) (Math.random() * terrain.size),
				(int) (Math.random() * terrain.size),
				(int) (Math.sqrt(terrain.size) / 1)
			);
		}
	}
	
	
	public void dispose() {
		instances.clear();
		assets.dispose();
		celbatch.dispose();
		terrain.dispose();
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




