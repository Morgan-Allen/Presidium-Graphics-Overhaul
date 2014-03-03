/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.* ;
import src.util.* ;


//
//  TODO:  Cut this out for the moment.  The interface needs some
//  simplification.


public class MissionsTab extends InfoPanel {
  
  
  /**  Constants, field definitions and constructors-
    */
  final static String
    IMG_DIR = "media/GUI/Missions/" ;
  final public static ImageAsset
    ALL_ICONS[] = ImageAsset.fromImages(
      MissionsTab.class, IMG_DIR,
      "mission_strike.png",
      "mission_recon.png",
      "mission_contact.png",
      "mission_security.png"
    ),
    STRIKE_ICON   = ALL_ICONS[0],
    RECON_ICON    = ALL_ICONS[1],
    CONTACT_ICON  = ALL_ICONS[2],
    SECURITY_ICON = ALL_ICONS[3];
  //
  //  These icons need to be worked on a little more...
  final public static CutoutModel
    ALL_MODELS[] = CutoutModel.fromImages(
      IMG_DIR, MissionsTab.class, 1, 2,
      "flag_strike.gif",
      "flag_recon.gif",
      "flag_contact.gif",
      "flag_security.gif"
    ),
    STRIKE_MODEL   = ALL_MODELS[0],
    RECON_MODEL    = ALL_MODELS[1],
    CONTACT_MODEL  = ALL_MODELS[2],
    SECURITY_MODEL = ALL_MODELS[3];
  
  
  
  public MissionsTab(BaseUI UI) {
    super(UI, null, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText
  ) {
    //super.updateText(UI, headerText, detailText) ;
    headerText.setText("Missions") ;
    detailText.setText("") ;
    //
    //  List Strike, Recon, Contact and Security missions for now.
    detailText.insert(STRIKE_ICON.asTexture(), 40) ;
    detailText.append(" Strike Mission\n") ;
    detailText.append(new Text.Clickable() {
      public String fullName() { return "Target" ; }
      public void whenClicked() { initStrikeTask(UI) ; }
    }) ;
    detailText.append("\n") ;
    
    detailText.insert(RECON_ICON.asTexture(), 40) ;
    detailText.append(" Recon Mission\n") ;
    detailText.append(new Text.Clickable() {
      public String fullName() { return "Target" ; }
      public void whenClicked() { initReconTask(UI) ; }
    }) ;
    detailText.append("\n") ;
    
    detailText.insert(SECURITY_ICON.asTexture(), 40) ;
    detailText.append(" Security Mission\n") ;
    detailText.append(new Text.Clickable() {
      public String fullName() { return "Target" ; }
      public void whenClicked() { initSecurityTask(UI) ; }
    }) ;
    detailText.append("\n") ;
    
    detailText.insert(CONTACT_ICON.asTexture(), 40) ;
    detailText.append(" Contact Mission\n") ;
    detailText.append(new Text.Clickable() {
      public String fullName() { return "Target" ; }
      public void whenClicked() { initContactTask(UI) ; }
    }) ;
    detailText.append("\n") ;
  }
  
  
  
  
  private static void previewFlag(
    BaseUI UI, Sprite flag, Target picked, boolean valid
  ) {
    flag.scale = 0.5f ;
    if (! valid) {
      final World world = UI.world() ;
      final Vec3D onGround = world.pickedGroundPoint(UI, UI.rendering.view) ;
      flag.position.setTo(onGround) ;
      flag.colour = Colour.RED ;
    }
    else {
      Mission.placeFlag(flag, picked) ;
      flag.colour = Colour.GREEN ;
    }
    flag.registerFor(UI.rendering);
    //UI.rendering.addClient(flag) ;
  }
  
  
  protected static void initStrikeTask(BaseUI UI) {
    final Sprite flagSprite = STRIKE_MODEL.makeSprite() ;
    UI.beginTask(new TargetTask(UI, STRIKE_ICON) {
      
      boolean validPick(Target pick) {
        if (! (pick instanceof Actor || pick instanceof Venue)) return false ;
        if (! ((Element) pick).visibleTo(UI.played())) return false ;
        return true ;
      }
      
      void previewAt(Target picked, boolean valid) {
        previewFlag(UI, flagSprite, picked, valid) ;
      }
      
      void performAt(Target picked) {
        final Mission mission = new StrikeMission(UI.played(), picked) ;
        UI.played().addMission(mission) ;
        UI.selection.pushSelection(mission, true) ;
      }
    }) ;
  }
  
  
  protected static void initReconTask(BaseUI UI) {
    final Sprite flagSprite = RECON_MODEL.makeSprite() ;
    UI.beginTask(new TargetTask(UI, RECON_ICON) {
      
      boolean validPick(Target pick) {
        if (! (pick instanceof Tile)) return false ;
        final Tile tile = (Tile) pick ;
        if (UI.played().intelMap.fogAt(tile) == 0) return true ;
        if (! tile.habitat().pathClear) return false ;
        return true ;
      }
      
      void previewAt(Target picked, boolean valid) {
        previewFlag(UI, flagSprite, picked, valid) ;
      }
      
      void performAt(Target picked) {
        final Mission mission = new ReconMission(UI.played(), (Tile) picked) ;
        UI.played().addMission(mission) ;
        UI.selection.pushSelection(mission, true) ;
      }
    }) ;
  }
  
  
  protected static void initSecurityTask(BaseUI UI) {
    final Sprite flagSprite = SECURITY_MODEL.makeSprite() ;
    UI.beginTask(new TargetTask(UI, SECURITY_ICON) {
      
      boolean validPick(Target pick) {
        if (! (pick instanceof Actor || pick instanceof Venue)) return false ;
        if (! ((Element) pick).visibleTo(UI.played())) return false ;
        return true ;
      }
      
      void previewAt(Target picked, boolean valid) {
        previewFlag(UI, flagSprite, picked, valid) ;
      }
      
      void performAt(Target picked) {
        final Mission mission = new SecurityMission(UI.played(), picked) ;
        UI.played().addMission(mission) ;
        UI.selection.pushSelection(mission, true) ;
      }
    }) ;
  }
  
  
  protected static void initContactTask(BaseUI UI) {

    final Sprite flagSprite = CONTACT_MODEL.makeSprite() ;
    UI.beginTask(new TargetTask(UI, CONTACT_ICON) {
      
      boolean validPick(Target pick) {
        if (! (pick instanceof Actor || pick instanceof Venue)) return false ;
        if (! ((Element) pick).visibleTo(UI.played())) return false ;
        if (((Actor) pick).base() == UI.played()) return false ;
        return true ;
      }
      
      void previewAt(Target picked, boolean valid) {
        previewFlag(UI, flagSprite, picked, valid) ;
      }
      
      void performAt(Target picked) {
        final Mission mission = new ContactMission(
          UI.played(), (Actor) picked
        ) ;
        UI.played().addMission(mission) ;
        UI.selection.pushSelection(mission, true) ;
      }
    }) ;
  }
}



