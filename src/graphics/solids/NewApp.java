


package src.graphics.solids;
import src.game.planet.Species;
import src.graphics.common.*;
import static src.graphics.common.GL.*;
import src.graphics.solids.*;
//import src.graphics.kerai_src.MS3DLoader0.MS3DParameters;
import src.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;




public class NewApp extends ApplicationAdapter {

  public static void main(String[] args) {
      LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
      cfg.title = "SFCityBuilder";
      //cfg.useGL30 = false;
      cfg.useGL20 = true;
      cfg.vSyncEnabled = true;
      cfg.width = 1424;
      cfg.height = 900;
//    cfg.width = 1920;
//    cfg.height = 1080;
      cfg.foregroundFPS = -1;
      cfg.backgroundFPS = 30;
      cfg.resizable = false;
      cfg.fullscreen = false;
      //cfg.depth = 0;
      
      //Charsete
      
      //System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
      
      new LwjglApplication(new NewApp(), cfg);
  }
  

  public OrthographicCamera cam;
  public RTSCameraControl rtscam;

  public Array<SolidSprite> instances = new Array<SolidSprite>();
  private Environment env;
  private ModelBatch batch;

  private SolidSprite controlled;
  private float activeTime = 0;
  private String animName = AnimNames.MOVE;
  
  

  private InputAdapter inp = new InputAdapter() {
    public boolean keyDown(int key) {
      if (key == Keys.SPACE) {
        Array <Animation> anims = controlled.model.gdxModel.animations;
        Animation pick = (Animation) Rand.pickFrom(anims.toArray());
        animName = pick.id;
        return true;
      }
      return false;
    };
  };
  
  
  public void create() {

    env = new Environment();
    env.set(new ColorAttribute(
      ColorAttribute.AmbientLight,
      0.2f, 0.2f, 0.2f, 1
    ));
    env.add(new DirectionalLight().set(
      0.8f, 0.8f, 0.8f,
      1, 1, 1
    ));
    
    
    DefaultShaderProvider provider = new DefaultShaderProvider() {
      protected Shader createShader(Renderable renderable) {
        DefaultShader shad = new GDXShader(renderable, config, null);
        return shad;
      }
    };
    
    provider.config.numBones = 20;
    
    provider.config.fragmentShader = Gdx.files.internal(
      "shaders/solids.frag"
    ).readString();
    provider.config.vertexShader = Gdx.files.internal(
      "shaders/solids.vert"
    ).readString();
    batch = new ModelBatch(provider);
    
    
    float w = Gdx.graphics.getWidth();
    float h = Gdx.graphics.getHeight();
    cam = new OrthographicCamera(20, h / w * 20);

    cam.position.set(100f, 100f, 100f);
    cam.lookAt(0, 0, 0);
    cam.near = 0.1f;
    cam.far = 300f;
    cam.update();

    rtscam = new RTSCameraControl(cam);
    Gdx.input.setInputProcessor(new InputMultiplexer(inp, rtscam));
    
    
    final String dir = "media/Actors/fauna/";
    final MS3DModel model = MS3DModel.loadFrom(
      dir, "Micovore.ms3d", Species.class,
      "FaunaModels.xml", "Micovore"
    );
    Assets.loadNow(model);
    
    {
      SolidSprite guy = new SolidSprite(model);
      //guy.transform.setToTranslation(1, 0, 0);
      instances.add(guy);
    }
    
    /*
    final String dir = "media/Actors/human/";
    final MS3DModel model = MS3DModel.loadFrom(
      dir, "male_final.ms3d", NewApp.class,
      "HumanModels.xml", "MalePrime"
    );
    Assets.loadNow(model);
    
    final Texture[] overlays = new Texture[3];
    overlays[0] = new Texture(dir + "physician_skin.gif");
    overlays[1] = new Texture(dir + "ecologist_skin.gif");
    overlays[2] = new Texture(dir + "highborn_male_skin.gif");
    
    {
      SolidSprite guy = new SolidSprite(model);
      guy.setOverlaySkins(overlays);
      guy.showOnly(AnimNames.MAIN_BODY);
      guy.transform.setToTranslation(0, 0, 0);
      guy.transform.setToRotation(Vector3.Y, 90);
      instances.add(guy);
      controlled = guy;
    }
    {
      SolidSprite guy = new SolidSprite(model);
      guy.transform.setToTranslation(1, 0, 0);
      guy.setOverlaySkins(overlays[1]);
      instances.add(guy);
      guy.showOnly(AnimNames.MAIN_BODY);
    }
    //*/
  }
  
  
  public void resize(int width, int height) {
    cam.viewportHeight = (float) height / width * 20;
  }
  
  
  public void render() {
    rtscam.update();
    
    glClearColor(0, 0, 1, 0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    batch.begin(cam);
    
    activeTime += Gdx.graphics.getRawDeltaTime();
    
    for (SolidSprite instance : instances) {
      instance.setAnimation(animName, activeTime % 1);
      batch.render(instance, env);
    }
    batch.end();
  }
}




