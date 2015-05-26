


package stratos.user;
import stratos.graphics.common.*;
import stratos.graphics.sfx.PlaneFX;
import stratos.graphics.widgets.*;
import stratos.util.*;



public interface UIConstants {
  
  
  final public static String
    BUTTONS_PATH = "media/GUI/Buttons/",
    TABS_PATH    = "media/GUI/Tabs/",
    
    SECTORS_BUTTON_ID  = "sectors_pane_button",
    BUDGETS_BUTTON_ID  = "budgets_pane_button",
    ROSTER_BUTTON_ID   = "roster_pane_button" ,
    INSTALL_BUTTON_ID  = "install_pane_button",
    
    OPTIONS_BUTTON_ID  = "game_options_button",
    
    STRIKE_BUTTON_ID   = "strike_mission_button"  ,
    RECON_BUTTON_ID    = "recon_mission_button"   ,
    SECURITY_BUTTON_ID = "security_mission_button",
    CONTACT_BUTTON_ID  = "contact_mission_button" ;
  
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
    
    TYPE_SECURITY  = "Security" ,
    TYPE_COMMERCE  = "Commerce" ,
    TYPE_AESTHETIC = "Aesthetic",
    TYPE_ENGINEER  = "Engineer" ,
    TYPE_ECOLOGIST = "Ecologist",
    TYPE_PHYSICIAN = "Physician",
    
    INSTALL_CATEGORIES[] = {
      TYPE_SECURITY, TYPE_COMMERCE, TYPE_AESTHETIC,
      TYPE_ENGINEER, TYPE_ECOLOGIST, TYPE_PHYSICIAN
    },
    
    TYPE_SPECIAL   = "Special"  ,
    TYPE_HIDDEN    = "<hidden>" ,
    TYPE_ACTOR     = "<actor>"  ,
    TYPE_VEHICLE   = "<vehicle>",
    TYPE_TERRAIN   = "<terrain>",
    TYPE_MISSION   = "<mission>";
  
  
  final public static int
    BAR_BUTTON_SIZE = 40,
    BAR_SPACING     = 2 ,
    BAR_MAX_SLOTS   = 9 ;
  
  final public static int
    NUM_GUILDS = 6;
  
  final public static float
    BIG_FONT_SIZE   = 1.0f ,
    SMALL_FONT_SIZE = 0.75f;
  final public static int
    MARGIN_SIZE    = 10 ,
    HEADER_HIGH    = 35 ;
  
  final public static int
    
    MINIMAP_WIDE    = 200,
    MINIMAP_HIGH    = 200,
    CHARTS_WIDE     = 500,
    CHART_INFO_WIDE = 240,
    
    READOUT_HIGH    =  20,
    QUICKBAR_HIGH   =  65,
    PANEL_TAB_SIZE  =  65,
    PANEL_TABS_HIGH =  65,
    
    MESSAGE_PANE_WIDE = 500,
    MESSAGE_PANE_HIGH = 200,
    
    INFO_PANEL_WIDE = 280,
    SCROLLBAR_WIDE  =  20,
    DEFAULT_MARGIN  =  10,
    MIN_WIDGET_SIZE =  20;
}









