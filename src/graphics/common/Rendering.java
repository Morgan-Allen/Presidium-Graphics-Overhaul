


package src.graphics.common ;
import static src.graphics.common.GL.*;
import src.graphics.cutout.*;
import src.graphics.solids.*;
import src.graphics.terrain.*;
import src.graphics.widgets.*;
import src.start.PlayLoop;
import src.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;




//  NOTE:  This class should not be instantiated until the LibGdx engine has
//  invoked the create() method for the ApplicationListener.


public class Rendering {
  
  
  final public static int FRAMES_PER_SECOND = PlayLoop.FRAMES_PER_SECOND;
  
  final public Viewport view;
  final public Lighting lighting;
  private static float activeTime, frameAlpha ;
  
  //  first terrain, then cutouts, then solids, then sfx, then the UI.
  final public TerrainPass terrainPass;
  final public SolidsPass solidsPass;
  final public CutoutsPass cutoutsPass;
  //  TODO:  We'll also need a pass for special FX.
  
  
  public Rendering() {
    lighting = new Lighting(this);
    view = new Viewport();
    //Gdx.input.setInputProcessor(view);
    
    terrainPass = new TerrainPass(this);
    solidsPass  = new SolidsPass (this);
    cutoutsPass = new CutoutsPass(this);
    reportVersion();
  }
  
  
  public void dispose() {
    terrainPass.dispose();
    solidsPass .dispose();
    cutoutsPass.dispose();
  }
  
  
  public Environment environment() { return lighting.environment; }
  public Camera camera() { return view.camera; }
  
  public static float activeTime() { return activeTime; }
  public static float frameAlpha() { return frameAlpha; }
  
  
  private void reportVersion() {
    I.say(
      "Please send me this info"+
      "\n--- GL INFO -----------"+
      "\n   GL_VENDOR: "+glGetString(GL_VENDOR)+
      "\n GL_RENDERER: "+glGetString(GL_RENDERER)+
      "\n  GL_VERSION: "+glGetString(GL_VERSION)+
      "\nGLSL_VERSION: "+glGetString(GL_SHADING_LANGUAGE_VERSION)+
      "\n-----------------------\n"
    );
  }
  
  
  public void updateViews() {
    view.update();
  }
  
  
  public void renderDisplay(float worldTime, float frameTime, HUD UI) {
    
    Rendering.activeTime = worldTime;
    Rendering.frameAlpha = frameTime;
    ///I.say("World and frame time are:"+worldTime+"/"+frameTime);
    view.update();
    
    glEnable(GL10.GL_DEPTH_TEST);
    glDepthFunc(GL20.GL_LEQUAL);
    glEnable(GL10.GL_BLEND);
    glDepthMask(true);
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    glClearColor(1, 0, 0, 0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    terrainPass.performPass();
    glClear(GL_DEPTH_BUFFER_BIT);

    solidsPass.performPass();
    //  NOTE:  These are apparently switched off by the solids pass (using
    //  ModelBatch internally.)  TODO:  FIX using RenderContext.
    glEnable(GL10.GL_BLEND);
    glEnable(GL10.GL_DEPTH_TEST);
    //  TODO:  It's probably a good idea to take everything transparent and
    //  render it later.  But for the moment, cutouts are more likely to
    //  exhibit transparency.
    cutoutsPass.performPass();
    
    if (UI != null) {
      UI.updateInput();
      UI.renderHUD();
      UI = null;
    }
  }
}


