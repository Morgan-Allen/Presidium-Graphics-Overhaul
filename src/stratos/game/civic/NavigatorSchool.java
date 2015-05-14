

package stratos.game.civic;
import stratos.game.actors.Background;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.wild.Habitat;
import stratos.graphics.common.ImageAsset;
import stratos.graphics.common.ModelAsset;
import stratos.graphics.cutout.CutoutModel;
import stratos.graphics.cutout.GroupSprite;
import stratos.graphics.widgets.Composite;
import stratos.user.BaseUI;
import stratos.user.UIConstants;
import stratos.util.Rand;



public class NavigatorSchool extends Venue {
  

  private static boolean
    verbose = false;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    NavigatorSchool.class,
    "media/Buildings/schools and preserves/spacer_school.png", 5, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    NavigatorSchool.class, "media/GUI/Buttons/hospice_button.gif"
  );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    NavigatorSchool.class, "navigator_school",
    "Navigator School", UIConstants.TYPE_HIDDEN,
    5, 2, IS_NORMAL,
    NO_REQUIREMENTS, Owner.TIER_FACILITY
  );
  
  
  public NavigatorSchool(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(
      200, 4, 650,
      Structure.BIG_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public NavigatorSchool(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "navigator_school");
  }
  
  
  public String helpInfo() {
    return "Navigator School";
  }
}



