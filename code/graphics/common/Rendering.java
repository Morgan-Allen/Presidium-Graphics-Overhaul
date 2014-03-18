


package code.graphics.common ;
import static code.graphics.common.GL.*;
import code.graphics.cutout.*;
import code.graphics.sfx.*;
import code.graphics.solids.*;
import code.graphics.terrain.*;
import code.graphics.widgets.*;
import code.start.*;
import code.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;




//  NOTE:  This class should not be instantiated until the LibGdx engine has
//  invoked the create() method for the ApplicationListener.


public class Rendering {
  
  
  final public static int FRAMES_PER_SECOND = PlayLoop.FRAMES_PER_SECOND;
  
  final public Viewport view;
  final public Lighting lighting;
  public Colour backColour = null, foreColour = null ;
  
  private static float activeTime, frameAlpha ;
  
  //  first terrain, then cutouts, then solids, then sfx, then the UI.
  final public TerrainPass terrainPass;
  final public SolidsPass solidsPass;
  final public CutoutsPass cutoutsPass;
  final public SFXPass sfxPass;
  
  final public SpriteBatch batch2D ;
  final public Fading fading;
  
  
  public Rendering() {
    lighting = new Lighting(this);
    view = new Viewport();
    
    terrainPass = new TerrainPass(this);
    solidsPass  = new SolidsPass (this);
    cutoutsPass = new CutoutsPass(this);
    sfxPass     = new SFXPass    (this);
    
    batch2D = new SpriteBatch();
    fading = new Fading(this);
    reportVersion();
  }
  
  
  public void dispose() {
    terrainPass.dispose();
    solidsPass .dispose();
    cutoutsPass.dispose();
    sfxPass    .dispose();
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
  
  
  public void updateViews(float worldTime, float frameTime) {
    Rendering.activeTime = worldTime;
    Rendering.frameAlpha = frameTime;
    view.update();
  }
  
  
  public void clearAll() {
    terrainPass.clearAll();
    cutoutsPass.clearAll();
    solidsPass .clearAll();
    sfxPass    .clearAll();
  }
  
  
  public void renderDisplay(HUD UI) {
    ///I.say("World and frame time are:"+worldTime+"/"+frameTime);
    
    glEnable(GL10.GL_DEPTH_TEST);
    glDepthFunc(GL20.GL_LEQUAL);
    glEnable(GL10.GL_BLEND);
    glDepthMask(true);
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    final Colour BC = backColour == null ? Colour.DARK_GREY : backColour ;
    glClearColor(BC.r, BC.g, BC.b, BC.a);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    terrainPass.performPass();
    glClear(GL_DEPTH_BUFFER_BIT);
    
    solidsPass.performPass();
    //  NOTE:  These are apparently switched off by the solids pass (using
    //  ModelBatch internally.)  TODO:  FIX using RenderContext?
    glEnable(GL10.GL_BLEND);
    glEnable(GL10.GL_DEPTH_TEST);
    
    //  TODO:  It's probably a good idea to take everything transparent and
    //  render it later.  But for the moment, cutouts are more likely to
    //  exhibit transparency.
    cutoutsPass.performPass();
    
    //  TODO:  This is causing some odd overlap problems
    glDepthMask(false);
    sfxPass.performPass();

    batch2D.begin();
    if (UI != null) {
      UI.updateInput();
      UI.renderHUD(this);
    }
    fading.applyTo(batch2D);
    batch2D.end();
  }
}






