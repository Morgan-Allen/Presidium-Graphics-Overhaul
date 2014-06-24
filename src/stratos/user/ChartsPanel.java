


package stratos.user;
import stratos.game.campaign.*;
import stratos.game.actors.Backgrounds;
import stratos.game.campaign.System;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.graphics.common.*;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;



//  TODO:  This should include an InfoPanel of some kind.
//  Consider supplying the axis and sector images directly(?)
public class ChartsPanel extends UIGroup {
  
  
  final static ImageAsset
    SELECT_CIRCLE = ImageAsset.fromImage(
      "media/Charts/selection.png", ChartsPanel.class
    );
  
  
  final StarField starfield;
  
  Bordering border;
  Image hoverImage, selectImage;
  //  TODO:  The info panel must be re-sized for this.  REPLACE
  Text infoPanel;
  
  private System hoverFocus, selectFocus;
  
  
  public ChartsPanel(HUD UI) {
    super(UI);
    starfield = new StarField();
    
    hoverImage = new Image(UI, SELECT_CIRCLE);
    selectImage = new Image(UI, SELECT_CIRCLE);
    hoverImage.attachTo(this);
    selectImage.attachTo(this);
    
    infoPanel = new Text(UI, UIConstants.INFO_FONT);
    infoPanel.relBound.set(0.66f, 0, 0.33f, 1);
    infoPanel.attachTo(this);
    infoPanel.scale = 0.75f;
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
  protected void updateState() {
    //  TODO:  You may have to supply the true area bounds to the starfield
    //  here, so that view transform can be calculated accordingly.
    
    
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
    
    super.updateState();
  }
  
  
  
  /**  Clears the screen for further UI rendering.
    */
  public void render(SpriteBatch batch2d) {
    //  TODO:  Fiddling directly with OpenGL calls this way is messy and risky.
    //  See if you can centralise this somewhere (along with the Minimap
    //  rendering.)
    batch2d.end();
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glEnable(GL10.GL_BLEND);
    glDepthMask(true);
    
    starfield.renderWith(UI.rendering, trueBounds(), UIConstants.INFO_FONT);
    
    glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    glDepthMask(false);
    
    batch2d.begin();
    super.render(batch2d);
  }
}








