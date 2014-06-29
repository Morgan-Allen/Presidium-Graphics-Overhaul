


package stratos.user;
import stratos.game.actors.Backgrounds;
import stratos.game.campaign.System;
//import stratos.game.campaign.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.graphics.common.*;
//import stratos.start.DebugCharts;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.graphics.*;
//import com.badlogic.gdx.graphics.g2d.*;
//import com.badlogic.gdx.math.Vector2;



//  TODO:  This should include an InfoPanel of some kind.
//  Consider supplying the axis and sector images directly(?)

public class ChartsPanel extends UIGroup {
  
  
  final static ImageAsset
    SELECT_CIRCLE = ImageAsset.fromImage(
      "media/Charts/selection.png", ChartsPanel.class
    );

  
  Bordering border;
  Image hoverImage, selectImage;
  //  TODO:  The info panel must be re-sized for this.  REPLACE
  Text infoPanel;
  
  private System hoverFocus, selectFocus;
  
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
  final static SolidModel PLANET_MODEL = MS3DModel.loadFrom(
    "media/Charts/", "Planet.ms3d",
    ChartsPanel.class, null, null
  );
  final static ImageAsset
    PLANET_SKIN = ImageAsset.fromImage(
      "media/Charts/planet_skin.png", ChartsPanel.class
    ),
    SECTOR_KEYS = ImageAsset.fromImage(
      "media/Charts/sector_keys.png", ChartsPanel.class
    );
  
  //  TODO:  This is just a dummy method at the moment, more or less.  Fill in.
  public void loadPlanet(String path, String file) {
    final Texture tex = PLANET_SKIN.asTexture();
    planet.attachModel(PLANET_MODEL, tex, tex, SECTOR_KEYS);
    planet.attachSector("Terra Sector"  , Colour.MAGENTA);
    planet.attachSector("Elysium Sector", Colour.BLUE   );
    planet.attachSector("Pavonis Sector", Colour.GREEN  );
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
    
    starfield.setupWith(chartImg, axisImg, fieldSize);
    
    //  Then, load up the array of different star types and the specific
    //  systems associated-
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
        image, null,
        gridW, gridH, system.getInt("imgU"), system.getInt("imgV"),
        0.67f, 0, 100 * 0.67f, position
      );
      
      final int starImg[] = starImages.get(type);
      starfield.addFieldObject(
        image, name,
        gridW, gridH, starImg[0], starImg[1],
        1, 0, 0, position
      );
    }
    
    final int[][] starTypes = new int[1][];
    for (int[] type : starImages.values()) { starTypes[0] = type; break; }
    starfield.addRandomScatter(image, gridW, gridH, starTypes, 10, 1);
  }
  
  
  
  /**  Updates the currently hovered and selected field object:
    */
  private void updateFieldSelection() {
    //
    //  First, we determine which field object (if any) the user is currently
    //  hovering over.
    FieldObject picked = starfield.selectedAt(UI.mousePos());
    final String label = picked == null ? "" : picked.label;
    
    final System oldHover = this.hoverFocus, oldSelect = this.selectFocus;
    System newFocus = null, newSelect = oldSelect;
    
    for (System system : Backgrounds.ALL_PLANETS) {
      if (label.equals(system.name)) {
        newFocus = system;
      }
    }
    if (picked != null && newFocus == null) {
      I.say("WARNING:  No system with name: "+picked.label);
    }
    //
    //  Then, update the current hover-focus, along with appropriate fade-in
    //  for the associated image-
    if (oldHover != newFocus) {
      this.hoverFocus = newFocus;
      hoverImage.relAlpha = 0;
    }
    if (hoverFocus != null && hoverFocus != selectFocus) {
      final Vec3D screenPos = new Vec3D();
      starfield.screenPosition(picked, screenPos);
      final float size = picked.radius(), hS = size / 2;
      hoverImage.hidden = false;
      hoverImage.absBound.set(screenPos.x - hS, screenPos.y - hS, size, size);
      
      hoverImage.relAlpha = Visit.clamp(
        hoverImage.relAlpha + (1f / Rendering.FRAMES_PER_SECOND), 0, 0.5f
      );
      if (UI.mouseClicked()) newSelect = newFocus;
    }
    else {
      hoverImage.hidden = true;
    }
    //
    //  Then, if the user has clicked on their current focus, we make it the
    //  current selection-
    if (oldSelect != newSelect) {
      this.selectFocus = newSelect;
      selectImage.relAlpha = 0.0f;
      if (selectFocus != null) infoPanel.setText(hoverFocus.description);
      else infoPanel.setText("");
    }
    if (selectFocus != null) {
      final FieldObject match = starfield.objectLabelled(selectFocus.name);
      
      final Vec3D screenPos = new Vec3D();
      starfield.screenPosition(match, screenPos);
      final float size = match.radius(), hS = size / 2;
      selectImage.hidden = false;
      selectImage.absBound.set(screenPos.x - hS, screenPos.y - hS, size, size);

      selectImage.relAlpha = Visit.clamp(
        selectImage.relAlpha + (1f / Rendering.FRAMES_PER_SECOND), 0, 1
      );
    }
    else selectImage.hidden = true;
  }
  
  
  private void updateSectorSelection() {
    /*
    final Vec3D surfacePos = planet.surfacePosition(UI.mousePos());
    final int colourVal = planet.colourSelectedAt(UI.mousePos());
    infoPanel.setText(
      "Surface position: "+surfacePos+
      "\nColour: "+colourVal
    );
    //*/
    //*
    DisplaySector sector = planet.selectedAt(UI.mousePos());
    if (sector != null) {
      infoPanel.setText(sector.label+" "+sector.key());
    }
    else {
      infoPanel.setText("Mouse: "+UI.mousePos());
    }
    //*/
  }
  
  
  protected void updateState() {
    //this.updateFieldSelection();
    this.updateSectorSelection();
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








