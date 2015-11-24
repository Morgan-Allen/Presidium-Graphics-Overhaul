/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.graphics.common.*;
import stratos.game.verse.*;
import stratos.graphics.charts.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.util.*;
import static stratos.graphics.common.GL.*;
import com.badlogic.gdx.graphics.*;



public class ChartUtils {
  
  
  final public static String
    LOAD_PATH        = "media/Charts/",
    PLANET_LOAD_FILE = "sectors.xml",
    STARS_LOAD_FILE  = "coordinates.xml";
  
  
  
  /**  Method for loading a carousel-display of homeworlds:
    */
  public static Carousel createWorldsCarousel(
    HUD UI
  ) {
    final Carousel carousel = new Carousel(UI);
    
    for (final VerseLocation world : Verse.ALL_PLANETS) {
      if (world.planetImage == null) continue;
      
      final UIGroup worldInfo = new UIGroup(UI);
      worldInfo.stretch = false;
      final Button b = new Button(
        UI, world.name, world.planetImage, Button.CIRCLE_LIT, world.info
      ) {
        protected void whenClicked() {
          carousel.setSpinTarget(world);
        }
      };
      b.alignToFill();
      b.attachTo(worldInfo);
      carousel.addEntryFor(world, worldInfo);
    }
    return carousel;
  }
  
  
  
  /**  Method for loading sector display information from external XML:
    */
  public static PlanetDisplay createPlanetDisplay(
    final String path, final String file
  ) {
    final PlanetDisplay display = new PlanetDisplay() {
      protected State loadAsset() {
        super.loadAsset();
        if (! stateLoaded()) return State.ERROR;
        loadPlanet(path, file, this);
        return State.LOADED;
      }
    };
    Assets.loadNow(display);
    return display;
  }
  
  
  public static void loadPlanet(
    String path, String file, PlanetDisplay display
  ) {
    final XML xml = XML.load(path+file);
    
    final XML
      modelNode   = xml.child("globeModel"),
      surfaceNode = xml.child("surfaceTex"),
      sectorsNode = xml.child("sectorsTex"),
      keysNode    = xml.child("sectorKeys");
    
    final MS3DModel globeModel = MS3DModel.loadFrom(
      path, modelNode.value("name"), SectorsPane.class, null, null
    );
    final ImageAsset sectorKeys = ImageAsset.fromImage(
      SectorsPane.class, path + keysNode.value("name")
    );
    Assets.loadNow(globeModel);
    Assets.loadNow(sectorKeys);
    final String
      surfaceFile = path + surfaceNode.value("name"),
      sectorsFile = path + sectorsNode.value("name");
    final Texture
      surfaceTex = ImageAsset.getTexture(surfaceFile),
      sectorsTex = ImageAsset.getTexture(sectorsFile);
    
    display.attachModel(globeModel, surfaceTex, sectorsTex, sectorKeys);
    
    final XML sectors = xml.child("sectors");
    for (XML sector : sectors.children()) {
      final String name = sector.value("name");
      final Colour key = new Colour().set(
        sector.getFloat("R"),
        sector.getFloat("G"),
        sector.getFloat("B"),
        1
      );
      display.attachSector(name, key);
    }
  }
  
  
  
  /**  Method for loading object coordinates from an external XML file:
    */
  public static StarField createStarField(
    final String path, final String file
  ) {
    final StarField field = new StarField() {
      protected State loadAsset() {
        super.loadAsset();
        if (! stateLoaded()) return State.ERROR;
        loadStarfield(path, file, this);
        return State.LOADED;
      }
    };
    Assets.loadNow(field);
    return field;
  }
  
  
  public static void loadStarfield(
    String path, String file, StarField display
  ) {
    final XML xml = XML.load(path+file);
    
    //  First, get the texture atlas for field objects, along with textures for
    //  the upper axis and sectors chart-
    final XML
      imgNode   = xml.child("imageField" ),
      axisNode  = xml.child("axisImage"  ),
      chartNode = xml.child("sectorImage");
    final String
      imgFile   = path + imgNode  .value("name"),
      axisFile  = path + axisNode .value("name"),
      chartFile = path + chartNode.value("name");
    final Texture
      image    = ImageAsset.getTexture(imgFile),
      axisImg  = ImageAsset.getTexture(axisFile),
      chartImg = ImageAsset.getTexture(chartFile);
    
    final int
      gridW = imgNode.getInt("gridU"),
      gridH = imgNode.getInt("gridV");
    final float
      fieldSize = chartNode.getFloat("size");
    
    //  Then, load up the array of different star types and the specific
    //  systems associated-
    final Table <String, int[]> logoImages = new Table <String, int[]> ();
    for (XML type : xml.child("logoTypes").children()) {
      final String name = type.value("name");
      final int coords[] = new int[] {
        type.getInt("imgU"),
        type.getInt("imgV")
      };
      logoImages.put(name, coords);
    }
    
    display.setupWith(chartImg, axisImg, fieldSize);
    
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
      display.addFieldObject(
        image, null,
        gridW, gridH, system.getInt("imgU"), system.getInt("imgV"),
        0.67f, 0, 100 * 0.67f, position
      );
      
      final int starImg[] = logoImages.get(type);
      display.addFieldObject(
        image, name,
        gridW, gridH, starImg[0], starImg[1],
        1, 0, 0, position
      );
    }
    
    final int selectCoords[] = logoImages.get("Selected");
    display.setSelectObject(
      image, gridW, gridH, selectCoords[0], selectCoords[1]
    );
    
    final int[][] starTypes = new int[1][];
    for (int[] type : logoImages.values()) { starTypes[0] = type; break; }
    display.addRandomScatter(image, gridW, gridH, starTypes, 10, 1);
  }
  
  
  
  /**  Methods for rendering planet and starfield displays-
    */
  public static void renderPlanet(
    PlanetDisplay display, UINode displayArea, WidgetsPass pass
  ) {
    //  TODO:  Fiddling directly with OpenGL calls this way is messy and risky.
    //  See if you can centralise this somewhere (along with the Minimap
    //  rendering.)
    pass.end();
    
    glEnable(GL10.GL_BLEND);
    glDepthMask(true);
    
    display.renderWith(
      pass.rendering, displayArea.trueBounds(), UIConstants.INFO_FONT
    );
    
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    glDepthMask(false);
    
    pass.begin();
  }
  
  
  public static void renderStars(
    StarField field, UINode displayArea, WidgetsPass pass
  ) {
    //  TODO:  Fiddling directly with OpenGL calls this way is messy and risky.
    //  See if you can centralise this somewhere (along with the Minimap
    //  rendering.)
    pass.end();
    
    glEnable(GL10.GL_BLEND);
    glDepthMask(true);
    
    final Box2D fieldBounds = displayArea.trueBounds();
    field.renderWith(pass.rendering, fieldBounds, UIConstants.INFO_FONT);
    
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    glDepthMask(false);
    
    pass.begin();
  }
}


















