


package stratos.user;
import stratos.game.campaign.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.graphics.common.*;
import stratos.util.*;

import static stratos.graphics.common.GL.*;
import com.badlogic.gdx.graphics.*;



//  TODO:  This should include an InfoPanel of some kind.

public class ChartsPanel extends UIGroup {
  
  
  final static ImageAsset
    SELECT_CIRCLE = ImageAsset.fromImage(
      "media/Charts/selection.png", ChartsPanel.class
    );

  
  Bordering border;
  Image hoverImage, selectImage;
  //  TODO:  The info panel must be re-sized for this.  REPLACE
  Text infoPanel;
  
  private Sector hoverFocus, selectFocus;
  
  final StarField starfield;
  final PlanetDisplay planet;
  private UIGroup starfieldGroup, planetGroup;
  
  
  public ChartsPanel(HUD UI) {
    super(UI);
    
    starfield = new StarField();
    starfieldGroup = new UIGroup(UI);
    starfieldGroup.relBound.set(0.25f, 0.5f, 0.5f, 0.5f);
    starfieldGroup.attachTo(this);

    planet = new PlanetDisplay();
    planetGroup = new UIGroup(UI);
    planetGroup.relBound.set   (0.25f, 0.0f, 0.5f, 0.5f);
    planetGroup.attachTo(this);
    
    hoverImage = new Image(UI, SELECT_CIRCLE);
    selectImage = new Image(UI, SELECT_CIRCLE);
    hoverImage.attachTo(this);
    selectImage.attachTo(this);
    
    infoPanel = new Text(UI, UIConstants.INFO_FONT);
    infoPanel.relBound.set(0.66f, 0, 0.33f, 1);
    infoPanel.attachTo(this);
    infoPanel.scale = 0.75f;
  }
  
  
  void dispose() {
    starfield.dispose();
    planet.dispose();
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
      path, modelNode.value("name"), ChartsPanel.class, null, null
    );
    final ImageAsset sectorKeys = ImageAsset.fromImage(
      path + keysNode.value("name"), ChartsPanel.class
    );
    Assets.loadNow(globeModel);
    Assets.loadNow(sectorKeys);
    final String
      surfaceFile = path + surfaceNode.value("name"),
      sectorsFile = path + sectorsNode.value("name");
    final Texture
      surfaceTex = ImageAsset.getTexture(surfaceFile),
      sectorsTex = ImageAsset.getTexture(sectorsFile);
    
    planet.attachModel(globeModel, surfaceTex, sectorsTex, sectorKeys);
    
    final XML sectors = xml.child("sectors");
    for (XML sector : sectors.children()) {
      final String name = sector.value("name");
      final Colour key = new Colour().set(
        sector.getFloat("R"),
        sector.getFloat("G"),
        sector.getFloat("B"),
        1
      );
      planet.attachSector(name, key);
    }
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
    
    final int selectCoords[] = logoImages.get("Selected");  //TODO:  USE
    starfield.setupWith(chartImg, axisImg, fieldSize);
    
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
        image, null,
        gridW, gridH, system.getInt("imgU"), system.getInt("imgV"),
        0.67f, 0, 100 * 0.67f, position
      );
      
      final int starImg[] = logoImages.get(type);
      starfield.addFieldObject(
        image, name,
        gridW, gridH, starImg[0], starImg[1],
        1, 0, 0, position
      );
    }
    
    final int[][] starTypes = new int[1][];
    for (int[] type : logoImages.values()) { starTypes[0] = type; break; }
    starfield.addRandomScatter(image, gridW, gridH, starTypes, 10, 1);
  }
  
  
  
  /**  Updates the currently hovered and selected field object:
    */
  private Sector hoveredSystem() {
    final FieldObject fieldPick = starfield.selectedAt(UI.mousePos());
    final DisplaySector dispPick = planet.selectedAt(UI.mousePos());
    
    final String label;
    if      (fieldPick != null) label = fieldPick.label;
    else if (dispPick  != null) label = dispPick.label;
    else                        label = "";
    
    for (Sector system : Sectors.ALL_SECTORS) {
      if (label.equals(system.name)) {
        return system;
      }
    }
    return null;
  }
  
  
  //  TODO:  I'm going to have to implement separate selection/highlighting
  //  functions here, aren't I?  Use that instead.
  private Vec3D screenPosition(Sector system) {
    if (system == null) return null;
    final FieldObject fieldMatch = starfield.objectLabelled(system.name);
    if (fieldMatch != null) return starfield.screenPosition(fieldMatch, null);
    
    final DisplaySector dispMatch = planet.sectorLabelled(system.name);
    if (dispMatch != null) return planet.screenPosition(dispMatch, null);
    
    return null;
  }
  
  
  private void updateHovered() {
    //
    //  First, we determine which field object (if any) the user is currently
    //  hovering over.
    final Sector oldHover = this.hoverFocus;//, oldSelect = this.selectFocus;
    Sector newFocus = hoveredSystem();//, newSelect = oldSelect;
    //
    //  Then, update the current hover-focus, along with appropriate fade-in
    //  for the associated image-
    if (oldHover != newFocus) {
      this.hoverFocus = newFocus;
      hoverImage.relAlpha = 0;
    }
    final Vec3D hoverPos = screenPosition(hoverFocus);
    if (hoverPos != null && hoverFocus != selectFocus) {
      final float size = 60, hS = size / 2;  //  TODO:  REPLACE THIS
      hoverImage.hidden = false;
      hoverImage.absBound.set(hoverPos.x - hS, hoverPos.y - hS, size, size);
      
      hoverImage.relAlpha = Visit.clamp(
        hoverImage.relAlpha + (1f / Rendering.FRAMES_PER_SECOND), 0, 0.5f
      );
      //if (UI.mouseClicked()) newSelect = newFocus;
    }
    else {
      hoverImage.hidden = true;
    }
  }
  
  
  private void updateSelected() {
    final Sector oldSelect = this.selectFocus, newSelect = hoverFocus;
    //
    //  Then, if the user has clicked on their current focus, we make it the
    //  current selection-
    if (oldSelect != newSelect && UI.mouseClicked()) {
      this.selectFocus = newSelect;
      selectImage.relAlpha = 0.0f;
      if (selectFocus != null) {
        infoPanel.setText(hoverFocus.name+"\n\n");
        infoPanel.append(hoverFocus.description);
      }
      else infoPanel.setText("");
    }
    final Vec3D selectPos = screenPosition(selectFocus);
    if (selectPos != null) {
      final float size = 60, hS = size / 2;  //  TODO:  REPLACE THIS
      selectImage.hidden = false;
      selectImage.absBound.set(selectPos.x - hS, selectPos.y - hS, size, size);
      
      selectImage.relAlpha = Visit.clamp(
        selectImage.relAlpha + (1f / Rendering.FRAMES_PER_SECOND), 0, 1
      );
    }
    else selectImage.hidden = true;
  }
  
  
  protected void updateState() {
    updateHovered();
    updateSelected();
    super.updateState();
  }
  
  
  
  /**  Clears the screen for further UI rendering.
    */
  public void render(WidgetsPass batch2d) {
    //  TODO:  Fiddling directly with OpenGL calls this way is messy and risky.
    //  See if you can centralise this somewhere (along with the Minimap
    //  rendering.)
    batch2d.end();
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glEnable(GL10.GL_BLEND);
    glDepthMask(true);
    
    final Box2D planetBounds = planetGroup.trueBounds();
    planet.renderWith(UI.rendering, planetBounds, UIConstants.INFO_FONT);
    
    final Box2D fieldBounds = starfieldGroup.trueBounds();
    starfield.renderWith(UI.rendering, fieldBounds, UIConstants.INFO_FONT);
    
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    glDepthMask(false);
    
    batch2d.begin();
    super.render(batch2d);
  }
}








