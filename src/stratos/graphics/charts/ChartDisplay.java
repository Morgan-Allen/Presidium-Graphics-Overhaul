


package stratos.graphics.charts;
import stratos.graphics.common.*;
import static stratos.graphics.common.GL.*;
import stratos.util.*;
import com.badlogic.gdx.graphics.*;


//  TODO:  Try breaking this into separate displays.  And you'll need the
//  ability to key selectables off particular regions/field objects- plus an
//  info panel by the side.


public class ChartDisplay {
  
  
  final Rendering rendering;
  final public PlanetDisplay planet;
  final public StarField starfield;
  
  
  public ChartDisplay(Rendering rendering) {
    this.rendering = rendering;
    planet = new PlanetDisplay(this);
    starfield = new StarField(this);
  }
  
  
  public void dispose() {
    planet.dispose();
    starfield.dispose();
  }
  
  
  public void renderWithin(Box2D area) {
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    //planet.render();
    starfield.render();
  }
  
  
  
  public void loadStarfield(String path, String file) {
    final XML xml = XML.load(path+file);
    
    final XML imgNode = xml.child("imageField");
    final String imgFile = path + imgNode.value("name");
    final Texture image = ImageAsset.getTexture(imgFile);
    final int gridW = imgNode.getInt("gridU"), gridH = imgNode.getInt("gridV");
    
    final Table <String, int[]> starImages = new Table <String, int[]> ();
    for (XML type : xml.child("starTypes").children()) {
      final String name = type.value("name");
      final int coords[] = new int[] {
        type.getInt("imgU"),
        type.getInt("imgV")
      };
      starImages.put(name, coords);
    }
    
    final XML systems = xml.child("systems");
    for (XML system : systems.children()) {
      final String
        name = system.value("name"),
        type = system.value("type");
      
      final Vec3D position = new Vec3D(
        system.getFloat("x"),
        system.getFloat("y"),
        system.getFloat("z")
      );
      starfield.addFieldObject(
        image,
        gridW, gridH, system.getInt("imgU"), system.getInt("imgV"),
        0.66f, 0, 100, position
      );
      
      final int starImg[] = starImages.get(type);
      starfield.addFieldObject(
        image,
        gridW, gridH, starImg[0], starImg[1],
        1, 0, 0, position
      );
    }
  }
  
}












