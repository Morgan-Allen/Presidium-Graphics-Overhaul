

package stratos.start;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import stratos.graphics.common.*;
import stratos.graphics.charts.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class DebugCharts extends VisualDebug {
  
  
  final SolidModel PLANET_MODEL = MS3DModel.loadFrom(
    "media/Charts/", "Planet.ms3d",
    DebugCharts.class, null, null
  );
  final ImageAsset PLANET_SKIN = ImageAsset.fromImage(
    "media/Charts/planet_skin.png", DebugCharts.class
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
    
    //  TODO:  There needs to be a neater way to implement this
    UI = new HUD();
    
    final UINode renderNode = new UINode(UI) {
      protected void render(SpriteBatch batch2d) {
        batch2d.end();
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



