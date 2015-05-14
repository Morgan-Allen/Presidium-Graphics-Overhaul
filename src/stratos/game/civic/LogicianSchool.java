

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



public class LogicianSchool extends Venue {
  

  private static boolean
    verbose = false;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    LogicianSchool.class,
    "media/Buildings/schools and preserves/logician_school.png", 5, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    LogicianSchool.class, "media/GUI/Buttons/hospice_button.gif"
  );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    LogicianSchool.class, "logician_school",
    "Logician School", UIConstants.TYPE_HIDDEN,
    5, 2, IS_NORMAL,
    NO_REQUIREMENTS, Owner.TIER_FACILITY
  );
  
  
  public LogicianSchool(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(
      200, 4, 650,
      Structure.BIG_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public LogicianSchool(Session s) throws Exception {
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
    return Composite.withImage(ICON, "logician_school");
  }
  
  
  public String helpInfo() {
    return "Logician School";
  }
  
}



