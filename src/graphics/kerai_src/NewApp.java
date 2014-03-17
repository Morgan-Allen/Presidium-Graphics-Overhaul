


package src.graphics.kerai_src;
import src.graphics.common.*;
import static src.graphics.common.GL.*;
import src.graphics.solids.*;

//import src.graphics.kerai_src.MS3DLoader0.MS3DParameters;
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

  public Array<SolidSprite0> instances = new Array<SolidSprite0>();
  private Environment env;
  private ModelBatch batch;

  private SolidSprite0 controlled;
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

    final String dir = "media/Actors/human/";
    final MS3DModel0 model = MS3DModel0.loadFrom(
      dir, "male_final.ms3d", NewApp.class,
      "HumanModels.xml", "MalePrime"
    );
    Assets.loadNow(model);
    
    final Texture[] overlays = new Texture[3];
    overlays[0] = new Texture(dir + "physician_skin.gif");
    overlays[1] = new Texture(dir + "ecologist_skin.gif");
    overlays[2] = new Texture(dir + "highborn_male_skin.gif");
    
    {
      SolidSprite0 guy = new SolidSprite0(model);
      guy.setOverlaySkins(overlays);
      guy.showOnly(AnimNames.MAIN_BODY);
      guy.transform.setToTranslation(0, 0, 0);
      guy.transform.setToRotation(Vector3.Y, 90);
      instances.add(guy);
      controlled = guy;
    }
    {
      SolidSprite0 guy = new SolidSprite0(model);
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
    
    for (SolidSprite0 instance : instances) {
      instance.setAnimation(animName, activeTime % 1);
      batch.render(instance, env);
    }
    batch.end();
  }
}




