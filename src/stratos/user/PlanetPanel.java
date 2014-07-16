

package stratos.user;
import stratos.game.campaign.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.graphics.*;



//TODO:  Enclose together with the sector info in a single large panel?


public class PlanetPanel extends UIGroup {
  
  
  final static ImageAsset
    PLANET_ICON     = ImageAsset.fromImage(
      "media/GUI/Panels/planet_tab.png", PlanetPanel.class
    ),
    PLANET_ICON_LIT = Button.CIRCLE_LIT;
  
  final static ImageAsset
    LEFT_BUTTON_IMG  = ImageAsset.fromImage(
      "media/Charts/button_left.png" , PlanetPanel.class
    ),
    RIGHT_BUTTON_IMG = ImageAsset.fromImage(
      "media/Charts/button_right.png", PlanetPanel.class
    ),
    BORDER_TEX = ImageAsset.fromImage(
      "media/Charts/planet_frame.png", PlanetPanel.class
    );
  

  final PlanetDisplay display;
  final Image border;
  final UIGroup displayArea;
  final Button left, right;
  
  private Sector focus;
  final SectorPanel infoPanel;
  
  
  public PlanetPanel(HUD UI) {
    super(UI);
    
    display = new PlanetDisplay();
    
    displayArea = new UIGroup(UI) {
      public void render(WidgetsPass pass) {
        renderPlanet(pass);
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
    
    left = new Button(
      UI,
      LEFT_BUTTON_IMG.asTexture(),
      Button.CIRCLE_LIT.asTexture(),
      "Rotate left"
    ) {
      protected void whenPressed() { incRotation( 15, true); }
    };
    left.relBound.set(0, 0, 0, 0);
    left.absBound.set(0, 0, 55, 55);
    left.attachTo(displayArea);
    
    right = new Button(
      UI,
      RIGHT_BUTTON_IMG.asTexture(),
      Button.CIRCLE_LIT.asTexture(),
      "Rotate right"
    ) {
      protected void whenPressed() { incRotation(-15, true); }
    };
    right.relBound.set(0, 0, 0, 0);
    right.absBound.set(55, 0, 55, 55);
    right.attachTo(displayArea);
  }
  
  
  
  /**  Method for loading sector display information from external XML:
    */
  public void loadPlanet(String path, String file) {
    final XML xml = XML.load(path+file);
    
    final XML
      modelNode   = xml.child("globeModel"),
      surfaceNode = xml.child("surfaceTex"),
      sectorsNode = xml.child("sectorsTex"),
      keysNode    = xml.child("sectorKeys");
    
    final MS3DModel globeModel = MS3DModel.loadFrom(
      path, modelNode.value("name"), PlanetPanel.class, null, null
    );
    final ImageAsset sectorKeys = ImageAsset.fromImage(
      path + keysNode.value("name"), PlanetPanel.class
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
  
  
  
  /**  Navigation and feedback-
    */
  //  TODO:  Control elevation as well.  (Include a zoom function?)
  private void incRotation(float amount, boolean inFrame) {
    float oldRot = display.rotation();
    if (inFrame) amount *= 2f / Rendering.FRAMES_PER_SECOND;
    display.setRotation(oldRot + amount);
  }
  
  
  
  /**  Main rendering methods-
    */
  protected void updateState() {
    final DisplaySector DS = display.selectedAt(UI.mousePos());
    final Sector hovered = DS == null ? null : Sector.sectorNamed(DS.label);
    
    if (UI.mouseClicked()) {
      focus = hovered;
      if (focus != null) {
        display.setSelection(focus == null ? null : focus.name);
        
        infoPanel.header.setText(focus.name);
        infoPanel.detail.setText(focus.description);
      }
    }
    
    super.updateState();
  }
  
  
  public void renderPlanet(WidgetsPass batch2d) {
    //  TODO:  Fiddling directly with OpenGL calls this way is messy and risky.
    //  See if you can centralise this somewhere (along with the Minimap
    //  rendering.)
    batch2d.end();
    
    //glClearColor(0, 0, 0, 1);
    //glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glEnable(GL10.GL_BLEND);
    glDepthMask(true);
    
    //  TODO:  Add controls for this.
    //display.setElevation(0);
    
    final Box2D planetBounds = displayArea.trueBounds();
    display.renderWith(UI.rendering, planetBounds, UIConstants.INFO_FONT);
    
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    glDepthMask(false);
    
    batch2d.begin();
    //super.render(batch2d);
  }
}



