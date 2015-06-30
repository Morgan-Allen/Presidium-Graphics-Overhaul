

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
    "media/Buildings/schools and preserves/logician_school.png", 6, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    LogicianSchool.class, "media/GUI/Buttons/hospice_button.gif"
  );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    LogicianSchool.class, "logician_school",
    "Logician School", UIConstants.TYPE_HIDDEN, ICON,
    "<Logician School Description>",
    6, 2, Structure.IS_NORMAL,
    NO_REQUIREMENTS, Owner.TIER_FACILITY,
    200, 4, 650, Structure.BIG_MAX_UPGRADES
  );
  
  
  public LogicianSchool(Base base) {
    super(BLUEPRINT, base);
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
}



