/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.charts.*;



public class StarsPane extends UIGroup implements UIConstants {
  
  
  final static ImageAsset
    STARS_ICON = ImageAsset.fromImage(
      StarsPane.class, "media/GUI/Panels/charts_tab.png"
    ),
    STARS_ICON_LIT = Button.CIRCLE_LIT;
  
  final static String IMG_DIR = ChartUtils.LOAD_PATH;
  final static ImageAsset
    LEFT_BUTTON_IMG  = ImageAsset.fromImage(
      StarsPane.class  , IMG_DIR+"button_left.png"
    ),
    RIGHT_BUTTON_IMG = ImageAsset.fromImage(
      StarsPane.class , IMG_DIR+"button_right.png"
    ),
    BACKING_TEX = ImageAsset.fromImage(
      StarsPane.class, IMG_DIR+"stars_backing.png"
    ),
    BORDER_TEX = ImageAsset.fromImage(
      StarsPane.class , IMG_DIR+"planet_frame.png"
    );
  
  
  final Image backdrop;
  final StarField display;
  final Image border;
  final UIGroup displayArea;
  final Button left, right;
  
  private Sector focus;
  final SectorPanel infoPanel;
  
  
  public StarsPane(HUD UI) {
    super(UI);
    
    this.alignHorizontal(0.5f, CHARTS_WIDE + CHART_INFO_WIDE, 0);
    this.alignVertical  (0.5f, CHARTS_WIDE                  , 0);
    
    display = ChartUtils.createStarField(
      ChartUtils.LOAD_PATH, ChartUtils.STARS_LOAD_FILE
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
        ChartUtils.renderStars(display, this, pass);
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
  //  TODO:  Include controls for both rotation AND elevation...
  //  TODO:  Include a zoom/grab function?
  private void incRotation(float amount, boolean inFrame) {
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
          infoPanel.detail.setText(focus.info);
        }
      }
    }
    
    super.updateState();
  }
}





