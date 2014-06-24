

package stratos.start;
import stratos.graphics.common.*;
import stratos.graphics.charts.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import stratos.user.*;
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
  
  
  private HUD UI;
  private ChartsPanel chartsPanel;
  
  

  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugCharts());
  }
  
  
  protected void loadVisuals() {
    UI = new HUD(PlayLoop.rendering());
    chartsPanel = new ChartsPanel(UI);
    chartsPanel.relBound.set(0, 0, 1, 1);
    chartsPanel.attachTo(UI);
    
    //  TODO:  Test out the planet display separately.
    /*
    //  Here we set up the planet display:
    chartDisplay.planet.attachModel(
      PLANET_MODEL, PLANET_SKIN.asTexture(), PLANET_SKIN.asTexture()
    );
    //*/
    
    chartsPanel.loadStarfield(
      "media/Charts/", "coordinates.xml"
    );
  }
  
  
  public HUD UI() { return UI; }
  protected void onRendering(Sprite sprite) {}
}




/*
final Texture t = FIELD_OBJECTS.asTexture();
for (int n = 12; n-- > 0;) {
  final Vec3D starPos = new Vec3D(
    Rand.num() - 0.5f,
    Rand.num() - 0.5f,
    Rand.num() - 0.5f
  ).scale(10);
  
  chartDisplay.starfield.addFieldObject(
    t,  5, 5,  n % 3, n / 3,
    0.66f,  0, 100,  starPos
  );
  
  chartDisplay.starfield.addFieldObject(
    t,  5, 5,  Rand.index(1), 4,
    1,  0, 0,  starPos
  );
  
  for (int i = Rand.index(4); i-- > 0;) {
    Vec3D companion = new Vec3D(
      Rand.num() - 0.5f,
      Rand.num() - 0.5f,
      Rand.num() - 0.5f
    );
    if (Rand.yes()) companion.scale(2.5f).add(starPos);
    else companion.scale(10);
    
    chartDisplay.starfield.addFieldObject(
      t,  5, 5,  Rand.index(4), 4,
      (0.5f + Rand.num()) * 0.5f * Rand.num(),  0, 0,  companion
    );
  }
}
//*/



/*
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
//*/

/*
//  And here we set up the star-charts:
chartsPanel.starfield.setupWith(
  SECTORS_TEX.asTexture(),
  AXIS_TEX.asTexture(),
  10
);
//*/