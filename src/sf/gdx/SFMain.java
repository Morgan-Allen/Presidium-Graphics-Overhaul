


package sf.gdx;
import static gl.GL.*;

import org.lwjgl.opengl.GL31;

import sf.gdx.ms3d.MS3DLoader;
import sf.gdx.shaders.FogDefault;
import sf.gdx.shaders.FogMapAttribute;
import sf.gdx.shaders.OutlineShader;
import sf.gdx.ter.TerrainRender;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap ;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;



public class SFMain implements ApplicationListener {
	
	
	public OrthographicCamera cam;
	public RTSCameraControl rtscam;
	public AssetManager assets;
	
	public Array<ModelInstance> instances = new Array<ModelInstance>();
	public boolean loading;
	private AnimationController ctrl1;
	private AnimationController ctrl4;
	
	//private String model1 = "wall_corner.ms3d";
	private String model4 = "micovore/Micovore.ms3d";
	
	private TerrainRender ter;
	//ModelBatch celbatch;
	public Texture fogtex;
	//private Shader outline;
	
	Environment env;
	//private Pixmap fogpix;
	
	
	
	public void create() {
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
		
		env = new Environment();
		env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 0.9f));
		env.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
		
		/*
		DefaultShaderProvider provider = new DefaultShaderProvider() {
			@Override
			protected Shader createShader(Renderable renderable) {
				DefaultShader shad = new FogDefault(renderable, config, fogtex);
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
		//*/
		
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		//cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam = new OrthographicCamera(20, h/w * 20);
		
		cam.position.set(100f, 100f, 100f);
		cam.lookAt(0, 0, 0);
		cam.near = 0.1f;
		cam.far = 300f;
		cam.update();
		
		rtscam = new RTSCameraControl(cam);
		Gdx.input.setInputProcessor(rtscam);
		
		assets = new AssetManager();
		assets.setLoader(
			Model.class, ".ms3d",
			new MS3DLoader(new InternalFileHandleResolver())
		);
		//assets.load(model1, Model.class);
		assets.load(model4, Model.class);
		loading = true;
		
		//shad = new BlackShader();
		ter = new TerrainRender(cam);
		//outline = new OutlineShader();
		//outline.init();
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
		Gdx.graphics.setVSync(true);
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

		
		/*
		celbatch.begin(cam);
		for (ModelInstance instance : instances) {
			celbatch.render(instance, env);
			//celbatch.render(instance, outline);
		}
		celbatch.end();

		// rendering outlines
		
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
		ter.render(fogtex);
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




