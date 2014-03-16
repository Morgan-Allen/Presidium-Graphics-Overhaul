


package src.graphics.kerai_src;
import src.graphics.common.*;
import static src.graphics.common.GL.*;
import src.graphics.kerai_src.MS3DLoader.MS3DParameters;
import src.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
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

  public OrthographicCamera cam;
  public RTSCameraControl rtscam;

  public Array<ModelInstance> instances = new Array<ModelInstance>();
  private Environment env;
  private ModelBatch batch;

  private ModelInstance controlled;
  private float activeTime = 0;
  private String animName = AnimNames.MOVE;
  
  

  private InputAdapter inp = new InputAdapter() {
    public boolean keyDown(int key) {
      if (key == Keys.SPACE) {
        Array<Animation> anims = controlled.model.animations;
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

    MS3DLoader loader = new MS3DLoader(new InternalFileHandleResolver());
    
    // copied from XML
    MS3DParameters params = new MS3DParameters();
    params.add("stand", 0, 0, 0.5f);
    params.add("move", 5, 21, 1.5f);
    params.add("fire", 26, 28, 1.5f);
    params.add("reach_down", 30, 34, 1.2f);
    params.add("strike", 36, 39, 1.5f);
    params.add("fall", 40, 46, 2.1f);
    params.add("look", 47, 48, 1.3f);
    params.add("talk", 49, 50, 2f);
    params.add("talk_long", 52, 57, 4f);
    params.add("evade", 58, 62, 1f);
    params.add("build", 63, 65, 1f);
    params.add("block", 66, 67, .5f);
    params.add("strike_big", 70, 74, 1.5f);
    params.add("move_fast", 75, 83, .7f);
    params.add("move_sneak", 85, 93, .7f);
    params.scale = 0.025f;
    
    final String dir = "media/Actors/human/";
    final Model model = loader.loadModel(
      loader.resolve(dir + "male_final.ms3d"), params
    );
    
    final Texture[] overlays = new Texture[3];
    overlays[0] = new Texture(dir + "physician_skin.gif");
    overlays[1] = new Texture(dir + "ecologist_skin.gif");
    overlays[2] = new Texture(dir + "highborn_male_skin.gif");
    
    {
      ModelInstance guy = new ModelInstance(model);
      guy.setOverlaySkins(overlays);
      guy.showOnly(AnimNames.MAIN_BODY);
      guy.transform.setToTranslation(0, 0, 0);
      guy.transform.setToRotation(Vector3.Y, 90);
      instances.add(guy);
      controlled = guy;
    }
    {
      ModelInstance guy = new ModelInstance(model);
      guy.transform.setToTranslation(1, 0, 0);
      guy.setOverlaySkins(overlays[1]);
      instances.add(guy);
      guy.showOnly(AnimNames.MAIN_BODY);
    }
  }
  
  
  public void resize(int width, int height) {
    cam.viewportHeight = (float) height / width * 20;
  }
  
  
  public void render() {
    rtscam.update();

    glClearColor(1, 0, 1, 0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    batch.begin(cam);
    
    activeTime += Gdx.graphics.getRawDeltaTime();
    
    for (ModelInstance instance : instances) {
      instance.setAnimation(animName, activeTime % 1);
      batch.render(instance, env);
    }
    batch.end();
  }
}




