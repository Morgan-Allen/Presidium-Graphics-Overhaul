/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.base.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;
import stratos.start.Assets;
import stratos.util.*;
import static stratos.graphics.common.GL.*;

import com.badlogic.gdx.graphics.*;




public class SectorsPane extends UIGroup implements UIConstants {
  
  
  
  final static ImageAsset
    PLANET_ICON     = ImageAsset.fromImage(
      SectorsPane.class, "media/GUI/Panels/planet_tab.png"
    ),
    PLANET_ICON_LIT = Button.CROSSHAIRS_LIT;
  
  final static String IMG_DIR = ChartUtils.LOAD_PATH;
  final static ImageAsset
    LEFT_BUTTON_IMG = ImageAsset.fromImage(
      SectorsPane.class , IMG_DIR+"button_left.png"
    ),
    RIGHT_BUTTON_IMG = ImageAsset.fromImage(
      SectorsPane.class, IMG_DIR+"button_right.png"
    ),
    BACKING_TEX = ImageAsset.fromImage(
      StarsPane.class, IMG_DIR+"stars_backing.png"
    ),
    BORDER_TEX = ImageAsset.fromImage(
      SectorsPane.class, IMG_DIR+"planet_frame.png"
    );
  
  
  /**  Interface presented-
    */
  public static Button createButton(
    final BaseUI baseUI
  ) {
    return new SelectionPane.PaneButton(
      new SectorsPane(baseUI), baseUI,
      SECTORS_BUTTON_ID, PLANET_ICON, PLANET_ICON_LIT, "Sectors"
    );
  }
  
  
  
  /**  Data fields and construction-
    */
  final PlanetDisplay display;
  final Image backdrop, border;
  final UIGroup displayArea;
  final Button left, right;
  
  private VerseLocation focus;
  final SectorPanel infoPanel;
  
  
  public SectorsPane(HUD UI) {
    super(UI);
    setWidgetID(SECTORS_PANE_ID);
    
    this.alignHorizontal(0.5f, CHARTS_WIDE + CHART_INFO_WIDE, 0);
    this.alignVertical  (0.5f, CHARTS_WIDE                  , 0);
    
    display = ChartUtils.createPlanetDisplay(
      ChartUtils.LOAD_PATH, ChartUtils.PLANET_LOAD_FILE
    );
    
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
        ChartUtils.renderPlanet(display, this, pass);
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
      UI, null,
      LEFT_BUTTON_IMG  .asTexture(),
      Button.CIRCLE_LIT.asTexture(),
      "Rotate left"
    ) {
      protected void whenPressed() { incRotation( 15, true); }
    };
    left.alignLeft  (0, 55);
    left.alignBottom(0, 55);
    left.attachTo(leftSide);
    
    right = new Button(
      UI, null,
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
  
  
  
  /**  Navigation and feedback-
    */
  //  TODO:  Control elevation as well.  (Include a zoom function?)
  private void incRotation(float degrees, boolean inFrame) {
    display.spinAtRate(degrees * 2, 0);
  }
  
  
  
  /**  Main rendering methods-
    */
  protected void updateState() {
    
    final DisplaySector DS = display.selectedAt(UI.mousePos());
    final VerseLocation hovered = DS == null ? null :
      VerseLocation.sectorNamed(DS.label)
    ;
    if (UI.mouseClicked()) {
      focus = hovered;
      if (focus != null) {
        display.setSelection(focus.name, true);
        infoPanel.header.setText(focus.name);
        infoPanel.detail.setText(focus.info);
      }
    }
    
    super.updateState();
  }
}








