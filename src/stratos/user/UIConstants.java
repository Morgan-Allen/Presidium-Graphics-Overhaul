


package stratos.user;
import stratos.graphics.common.*;
import stratos.graphics.sfx.PlaneFX;
import stratos.graphics.widgets.*;
import stratos.util.*;



public interface UIConstants {
  
  
  final public static String
    BUTTONS_PATH = "media/GUI/Buttons/",
    TABS_PATH    = "media/GUI/Tabs/";
  
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
    TYPE_MILITANT  = "Militant" ,
    TYPE_MERCHANT  = "Merchant" ,
    TYPE_AESTHETE  = "Aesthete" ,
    TYPE_ARTIFICER = "Artificer",
    TYPE_ECOLOGIST = "Ecologist",
    TYPE_PHYSICIAN = "Physician",
    
    INSTALL_CATEGORIES[] = {
      TYPE_MILITANT, TYPE_MERCHANT, TYPE_AESTHETE,
      TYPE_ARTIFICER, TYPE_ECOLOGIST, TYPE_PHYSICIAN
    },
    TYPE_SPECIAL   = "Special" ,
    TYPE_HIDDEN    = "<hidden>";
  
  
  
  final public static int
    MINIMAP_WIDE    = 200,
    CHARTS_WIDE     = 500,
    CHART_INFO_WIDE = 240,
    
    READOUT_HIGH    =  40,
    QUICKBAR_HIGH   =  65,
    PANEL_TAB_SIZE  =  40,
    
    GUILDS_WIDE     = 320,
    INFO_PANEL_HIGH = 240,
    INFO_PANEL_WIDE = 640,
    SCROLLBAR_WIDE  =  20;
}







