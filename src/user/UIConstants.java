


package src.user ;
import src.graphics.common.* ;
import src.graphics.sfx.PlaneFX ;
import src.graphics.widgets.* ;
import src.util.* ;



public interface UIConstants {
  
  
  final public static String
    BUTTONS_PATH = "media/GUI/Buttons/",
    TABS_PATH    = "media/GUI/Tabs/" ;
  
  final public static PlaneFX.Model
    SELECT_CIRCLE = new PlaneFX.Model(
      "select_circle_fx", UIConstants.class,
      "media/GUI/selectCircle.png", 1, 0, 0, false
    ),
    SELECT_SQUARE = new PlaneFX.Model(
      "select_square_fx", UIConstants.class,
      "media/GUI/selectSquare.png", 1, 0, 0, false
    );
  
  final public static ImageAsset TIPS_TEX = ImageAsset.fromImage(
    "media/GUI/tips_frame.png", UIConstants.class
  );
  
  final public static Alphabet INFO_FONT = Alphabet.loadAlphabet(
    "media/GUI/", "FontVerdana.xml"
  );

  final public static String
    TYPE_MILITANT  = "Militant",
    TYPE_MERCHANT  = "Merchant",
    TYPE_AESTHETE  = "Aesthete",
    TYPE_ARTIFICER = "Artificer",
    TYPE_ECOLOGIST = "Ecologist",
    TYPE_PHYSICIAN = "Physician",
    
    INSTALL_CATEGORIES[] = {
      TYPE_MILITANT, TYPE_MERCHANT, TYPE_AESTHETE,
      TYPE_ARTIFICER, TYPE_ECOLOGIST, TYPE_PHYSICIAN
    },
    TYPE_SPECIAL  = "Special",
    
    TYPE_HIDDEN    = "hidden" ;
  
  
  final public static int
    NUM_TABS = 3,
    NUM_QUICK_SLOTS = 10,
    MAX_TIP_WIDTH = 200,
    INFO_AREA_WIDE = 300 ;
  //  Move the tooltips class over to this package.
  
  //  ...Include insets as well?
  final public static Box2D
    MAIN_BOUNDS = new Box2D().set(0, 1, 0.66f, 1.0f),
    
    MINI_BOUNDS = new Box2D().set(0, 1, 0, 0),
    MINI_INSETS = new Box2D().set(10, -210, 200, 200) ;
  
  final static Box2D
    
    //  TODO:  Have a constant for the width.
    INFO_BOUNDS = new Box2D().set(1, 0, 0, 1.0f),
    INFO_INSETS = new Box2D().set(
      0 - (INFO_AREA_WIDE + 20), 65, INFO_AREA_WIDE, -75
    ),
    
    //PANE_BOUNDS = new Box2D().set(0, 0, 1.0f, 0.9f),
    //TABS_BOUNDS = new Box2D().set(0, 0.9f, 1.0f, 0.1f),
    
    TIPS_INSETS = new Box2D().set(-10, -10, 20, 20) ;
  
  
  //  The size of the tooltip text.
  //  The size of the info header and label/category text.
  //  The area allocated to buttons for the guilds/missions/powers tabs.
  
  //  The size of the minimap.
  //  The size of the credits readout.
  //  The area allocated to buttons for the charts/career/logs tabs.
  //  The size of the quickbar.
  
}




