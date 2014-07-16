


package stratos.user;
import stratos.game.campaign.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Vector2;



//  TODO:  Enclose together with the sector info in a single large panel?


public class StarsPanel extends UIGroup {
  
  
  final static ImageAsset
    STARS_ICON = ImageAsset.fromImage(
      "media/GUI/Panels/charts_tab.png", StarsPanel.class
    ),
    STARS_ICON_LIT = Button.CIRCLE_LIT;
  
  final static ImageAsset
    UP_BUTTON_IMG = ImageAsset.fromImage(
      "media/Charts/button_up.png" , StarsPanel.class
    ),
    DOWN_BUTTON_IMG = ImageAsset.fromImage(
      "media/Charts/button_down.png", StarsPanel.class
    ),
    BACKING_TEX = ImageAsset.fromImage(
      "media/Charts/stars_backing.png", StarsPanel.class
    ),
    BORDER_TEX = ImageAsset.fromImage(
      "media/Charts/planet_frame.png", StarsPanel.class
    );
  
  
  final Image backdrop;
  final StarField display;
  final Image border;
  final UIGroup displayArea;
  final Button up, down;
  
  private Sector focus;
  final SectorPanel infoPanel;
  
  
  public StarsPanel(HUD UI) {
    super(UI);
    
    display = new StarField();
    
    backdrop = new Image(UI, BACKING_TEX);
    backdrop.relBound.set(0, 0, 1, 1);
    backdrop.absBound.set(20, 20, -40, -40);
    backdrop.stretch = false;
    backdrop.blocksSelect = true;
    backdrop.attachTo(this);
    
    displayArea = new UIGroup(UI) {
      public void render(WidgetsPass pass) {
        renderStars(pass);
        super.render(pass);
      }
    };
    displayArea.relBound.set(0, 0, 1, 1);
    displayArea.absBound.set(25, 25, -50, -50);
    displayArea.stretch = false;
    displayArea.attachTo(this);
    
    border = new Image(UI, BORDER_TEX);
    border.relBound.set(0, 0, 1, 1);
    border.absBound.set(20, 20, -40, -40);
    border.stretch = false;
    border.attachTo(this);
    
    infoPanel = new SectorPanel(UI);
    infoPanel.relBound.set(1, 0, 0, 1);
    infoPanel.absBound.set(0, 0, UIConstants.INFO_PANEL_WIDE, 0);
    infoPanel.attachTo(this);
    
    up = new Button(
      UI,
      UP_BUTTON_IMG.asTexture(),
      Button.CIRCLE_LIT.asTexture(),
      "Rotate up"
    ) {
      protected void whenPressed() { incElevation( 15, true); }
    };
    up.relBound.set(0, 0, 0, 0);
    up.absBound.set(0, 55, 55, 55);
    up.attachTo(displayArea);
    
    down = new Button(
      UI,
      DOWN_BUTTON_IMG.asTexture(),
      Button.CIRCLE_LIT.asTexture(),
      "Rotate down"
    ) {
      protected void whenPressed() { incElevation(-15, true); }
    };
    down.relBound.set(0, 0, 0, 0);
    down.absBound.set(0, 0, 55, 55);
    down.attachTo(displayArea);
  }
  
  
  
  /**  Method for loading object coordinates from an external XML file:
    */
  public void loadStarfield(String path, String file) {
    final XML xml = XML.load(path+file);
    
    //  First, get the texture atlas for field objects, along with textures for
    //  the upper axis and sectors chart-
    final XML
      imgNode = xml.child("imageField"),
      axisNode = xml.child("axisImage"),
      chartNode = xml.child("sectorImage");
    final String
      imgFile = path + imgNode.value("name"),
      axisFile = path + axisNode.value("name"),
      chartFile = path + chartNode.value("name");
    final Texture
      image = ImageAsset.getTexture(imgFile),
      axisImg = ImageAsset.getTexture(axisFile),
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
  
  
  
  /**  Navigation and feedback-
    */
  //  TODO:  Include controls for both rotation AND elevation...
  //  TODO:  Include a zoom/grab function?
  private void incElevation(float amount, boolean inFrame) {
    float oldElev = display.rotation();
    if (inFrame) amount *= 2f / Rendering.FRAMES_PER_SECOND;
    display.setRotation(oldElev + amount);
    //amount = Visit.clamp(oldElev + amount, -89.99f, 89.99f);
    //display.setElevation(amount);
  }
  
  
  
  /**  Main rendering methods-
    */
  protected void updateState() {
    
    if (UI.selected() == backdrop) {
      final FieldObject DS = display.selectedAt(UI.mousePos());
      final Sector hovered = DS == null ? null : Sector.sectorNamed(DS.label);
      
      if (UI.mouseClicked()) {
        focus = hovered;
        if (focus != null) {
          display.setSelection(focus == null ? null : focus.name);
          
          infoPanel.header.setText(focus.name);
          infoPanel.detail.setText(focus.description);
        }
      }
    }
    
    super.updateState();
  }
  
  
  private void renderStars(WidgetsPass batch2d) {
    //  TODO:  Fiddling directly with OpenGL calls this way is messy and risky.
    //  See if you can centralise this somewhere (along with the Minimap
    //  rendering.)
    batch2d.end();
    
    //glClearColor(0, 0, 0, 1);
    //glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glEnable(GL10.GL_BLEND);
    glDepthMask(true);
    
    final Box2D planetBounds = displayArea.trueBounds();
    display.renderWith(UI.rendering, planetBounds, UIConstants.INFO_FONT);
    
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    glDepthMask(false);
    
    batch2d.begin();
    //super.render(batch2d);
  }
}





