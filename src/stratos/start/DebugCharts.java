

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
    );
  
  final ImageAsset
    FIELD_OBJECTS = ImageAsset.fromImage(
      "media/Charts/field_objects.png", DebugCharts.class
    ),
    SECTORS_TEX = ImageAsset.fromImage(
      "media/Charts/chart_sectors.png", StarField.class
    ),
    AXIS_TEX = ImageAsset.fromImage(
      "media/Charts/sky_axis.png", StarField.class
    );
  
  
  private ChartDisplay chartDisplay;
  private HUD UI;
  
  

  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugCharts());
  }
  
  
  protected void loadVisuals() {
    chartDisplay = new ChartDisplay(PlayLoop.rendering());
    
    //  Here we set up the planet display:
    chartDisplay.planet.attachModel(
      PLANET_MODEL, PLANET_SKIN.asTexture(), PLANET_SKIN.asTexture()
    );
    
    //  And here we set up the star-charts:
    chartDisplay.starfield.setupWith(
      SECTORS_TEX.asTexture(),
      AXIS_TEX.asTexture(),
      10
    );
    final Texture t = FIELD_OBJECTS.asTexture();
    for (int n = 12; n-- > 0;) {
      final Vec3D starPos = new Vec3D(
        Rand.num() - 0.5f,
        Rand.num() - 0.5f,
        Rand.num() - 0.5f
      ).scale(8);
      
      chartDisplay.starfield.addFieldObject(
        t,  5, 5,  n % 3, n / 3,
        0.66f,  0, 100,  starPos
      );
      
      chartDisplay.starfield.addFieldObject(
        t,  5, 5,  Rand.index(1), 4,
        1.25f,  0, 0,  starPos
      );
      
      for (int i = Rand.index(5); i-- > 0;) {
        Vec3D companion = new Vec3D(
          Rand.num() - 0.5f,
          Rand.num() - 0.5f,
          Rand.num() - 0.5f
        ).scale(2.5f);
        companion.add(starPos);
        chartDisplay.starfield.addFieldObject(
          t,  5, 5,  Rand.index(4), 4,
          (0.5f + Rand.num()) * 0.5f * Rand.num(),  0, 0,  companion
        );
      }
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



