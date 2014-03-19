


package stratos.user ;
import stratos.graphics.common.*;
import stratos.graphics.sfx.PlaneFX;
import stratos.graphics.widgets.*;
import stratos.util.*;



public interface UIConstants {
  
  
  final public static String
    BUTTONS_PATH = "media/GUI/Buttons/",
    TABS_PATH    = "media/GUI/Tabs/" ;
  
  final public static PlaneFX.Model
    SELECT_CIRCLE = new PlaneFX.Model(
      "select_circle_fx", UIConstants.class,
      "media/GUI/selectCircle.png", 1, 0, 0, false, false
    ),
    SELECT_SQUARE = new PlaneFX.Model(
      "select_square_fx", UIConstants.class,
      "media/GUI/selectSquare.png", 1, 0, 0, false, false
    );
  
  final public static Alphabet INFO_FONT = Alphabet.loadAlphabet(
    "media/GUI/", "FontVerdana.xml"
  );
  
  
  //
  //  TODO:  I'll probably rework this into a different system:
  //    Security, Economic, Recreation,
  //    Health & Government, Schools, Wards & Preserves
  //
  //  By default, all buildings will appear in one big list, and you can
  //  optionally filter them along those lines.
  
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
    TYPE_SPECIAL   = "Special",
    TYPE_HIDDEN    = "hidden" ;
  
  
  final public static int
    NUM_TABS         = 3,
    NUM_QUICK_SLOTS  = 10,
    MAX_TIP_WIDTH    = 200,
    INFO_AREA_WIDE   = 320,
    CAT_BUTTONS_HIGH = 65;
  //  Move the tooltips class over to this package.
  
  //  ...Include insets as well?
  final public static Box2D
    MAIN_BOUNDS = new Box2D().set(0, 1, 0.66f, 1.0f),
    MINI_BOUNDS = new Box2D().set(0, 1, 0, 0),
    MINI_INSETS = new Box2D().set(0, -256, 256, 256);
  
  final static Box2D
    
    //  TODO:  Have a constant for the width.
    INFO_BOUNDS = new Box2D().set(1, 0, 0, 1.0f),
    INFO_INSETS = new Box2D().set(
      -INFO_AREA_WIDE, CAT_BUTTONS_HIGH,
      INFO_AREA_WIDE, -CAT_BUTTONS_HIGH
    );
    //PANE_BOUNDS = new Box2D().set(0, 0, 1.0f, 0.9f),
    //TABS_BOUNDS = new Box2D().set(0, 0.9f, 1.0f, 0.1f),
  
  
  //  The size of the tooltip text.
  //  The size of the info header and label/category text.
  //  The area allocated to buttons for the guilds/missions/powers tabs.
  
  //  The size of the minimap.
  //  The size of the credits readout.
  //  The area allocated to buttons for the charts/career/logs tabs.
  //  The size of the quickbar.
}







