

package stratos.start;
import stratos.graphics.common.*;
import stratos.graphics.charts.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;



public class DebugCharts extends VisualDebug {
  
  
  final SolidModel PLANET_MODEL = MS3DModel.loadFrom(
    "media/Charts/", "Planet.ms3d",
    DebugCharts.class, null, null
  );
  final ImageAsset
    PLANET_SKIN = ImageAsset.fromImage(
      "media/Charts/planet_skin.png", DebugCharts.class
    ),
    FIELD_OBJECTS = ImageAsset.fromImage(
      "media/Charts/stellar_objects.png", DebugCharts.class
    );
  
  
  private ChartDisplay chartDisplay;
  private HUD UI;
  
  

  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugCharts());
  }
  
  
  protected void loadVisuals() {
    chartDisplay = new ChartDisplay(PlayLoop.rendering());
    
    chartDisplay.planet.attachModel(
      PLANET_MODEL, PLANET_SKIN.asTexture(), PLANET_SKIN.asTexture()
    );
    
    final Texture t = FIELD_OBJECTS.asTexture();
    chartDisplay.starfield.addFieldObject(t, 5, 5,  2, 2,  new Vec3D(1,  0, 0));
    chartDisplay.starfield.addFieldObject(t, 5, 5,  2, 0,  new Vec3D(0, -1, 0));
    chartDisplay.starfield.addFieldObject(t, 5, 5,  0, 2,  new Vec3D(1, -1, 0));
    
    //  TODO:  There needs to be a neater way to implement this
    UI = new HUD();
    
    final UINode renderNode = new UINode(UI) {
      protected void render(SpriteBatch batch2d) {
        batch2d.end();
        //  TODO:  The bits below shouldn't be needed...
        glEnable(GL10.GL_BLEND);
        glDepthMask(true);
        glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        chartDisplay.renderWithin(trueBounds());
        batch2d.begin();
      }
    };
    renderNode.relBound.set(0, 0, 1, 1);
    renderNode.attachTo(UI);
  }
  
  
  public HUD UI() { return UI; }
  protected void onRendering(Sprite sprite) {}
}



