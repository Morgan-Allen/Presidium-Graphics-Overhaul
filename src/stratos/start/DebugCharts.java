

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
      "media/Charts/field_objects.png", DebugCharts.class
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
    for (int n = 16; n-- > 0;) {
      final Vec3D starPos = new Vec3D(
        Rand.num() - 0.5f,
        Rand.num() - 0.5f,
        Rand.num() - 0.5f
      ).scale(8);
      chartDisplay.starfield.addFieldObject(
        t,  5, 5,  n / 4, n % 4,
        0.5f,  0, 80,  starPos
      );
      chartDisplay.starfield.addFieldObject(
        t,  5, 5,  0, 4,
        0.5f,  0, 0,  starPos
      );
    }
    
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



