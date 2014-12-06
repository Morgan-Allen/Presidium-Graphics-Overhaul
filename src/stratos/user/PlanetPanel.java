

package stratos.user;
import stratos.game.campaign.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.start.Assets;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.graphics.*;




public class PlanetPanel extends UIGroup implements UIConstants {
  
  
  final static String
    LOAD_PATH = "media/Charts/",
    LOAD_FILE = "sectors.xml";
  
  final static ImageAsset
    PLANET_ICON     = ImageAsset.fromImage(
      PlanetPanel.class, "media/GUI/Panels/planet_tab.png"
    ),
    PLANET_ICON_LIT = Button.CIRCLE_LIT;
  
  final static ImageAsset
    LEFT_BUTTON_IMG  = ImageAsset.fromImage(
      PlanetPanel.class , LOAD_PATH+"button_left.png"
    ),
    RIGHT_BUTTON_IMG = ImageAsset.fromImage(
      PlanetPanel.class, LOAD_PATH+"button_right.png"
    ),
    BACKING_TEX      = ImageAsset.fromImage(
      StarsPanel.class, LOAD_PATH+"stars_backing.png"
    ),
    BORDER_TEX       = ImageAsset.fromImage(
      PlanetPanel.class, LOAD_PATH+"planet_frame.png"
    );
  
  
  final PlanetDisplay display;
  final Image backdrop, border;
  final UIGroup displayArea;
  final Button left, right;
  
  private Sector focus;
  final SectorPanel infoPanel;
  
  
  public PlanetPanel(HUD UI) {
    super(UI);
    
    this.alignHorizontal(0.5f, CHARTS_WIDE + CHART_INFO_WIDE, 0);
    this.alignVertical  (0.5f, CHARTS_WIDE                  , 0);
    
    display = new PlanetDisplay() {
      protected void loadAsset() {
        super.loadAsset();
        loadPlanet(LOAD_PATH, LOAD_FILE);
      }
    };
    
    final UIGroup leftSide = new UIGroup(UI);
    leftSide.alignLeft    (0   , CHARTS_WIDE   );
    leftSide.alignVertical(0.5f, CHARTS_WIDE, 0);
    leftSide.stretch = false;
    leftSide.attachTo(this);
    
    infoPanel = new SectorPanel(UI);
    infoPanel.alignRight   (0, CHART_INFO_WIDE);
    infoPanel.alignVertical(0, 0              );
    infoPanel.attachTo(this);
    
    backdrop = new Image(UI, BACKING_TEX);
    backdrop.alignHorizontal(20, 20);
    backdrop.alignVertical  (20, 20);
    backdrop.blocksSelect = true;
    backdrop.attachTo(leftSide);
    
    displayArea = new UIGroup(UI) {
      public void render(WidgetsPass pass) {
        renderPlanet(pass);
        super.render(pass);
      }
    };
    displayArea.alignHorizontal(25, 25);
    displayArea.alignVertical  (25, 25);
    displayArea.stretch = false;
    displayArea.attachTo(leftSide);
    
    border = new Image(UI, BORDER_TEX);
    border.alignHorizontal(20, 20);
    border.alignVertical  (20, 20);
    border.attachTo(leftSide);
    
    left = new Button(
      UI,
      LEFT_BUTTON_IMG.asTexture(),
      Button.CIRCLE_LIT.asTexture(),
      "Rotate left"
    ) {
      protected void whenPressed() { incRotation( 15, true); }
    };
    left.alignLeft  (0, 55);
    left.alignBottom(0, 55);
    left.attachTo(leftSide);
    
    right = new Button(
      UI,
      RIGHT_BUTTON_IMG.asTexture(),
      Button.CIRCLE_LIT.asTexture(),
      "Rotate right"
    ) {
      protected void whenPressed() { incRotation(-15, true); }
    };
    right.alignLeft  (55, 55);
    right.alignBottom(0 , 55);
    right.attachTo(leftSide);
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
      PlanetPanel.class, path + keysNode.value("name")
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
        infoPanel.detail.setText(focus.info);
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



