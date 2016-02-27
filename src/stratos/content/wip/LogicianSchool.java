

package stratos.content.wip;
import static stratos.game.actors.Qualities.ASSEMBLY;
import stratos.game.actors.Background;
import stratos.game.common.*;
import stratos.game.craft.*;
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
    LogicianSchool.class, "logician_school_model",
    "media/Buildings/schools and preserves/logician_school.png", 6, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    LogicianSchool.class, "logician_school_icon",
    "media/GUI/Buttons/hospice_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    LogicianSchool.class, "logician_school",
    "Logician School", Target.TYPE_WIP, ICON,
    "<Logician School Description>",
    6, 2, Structure.IS_NORMAL,
    Owner.TIER_FACILITY, 200,
    4
  );
  
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.TWO_LEVELS, null,
      new Object[] { 10, ASSEMBLY },
      1000,
      1300
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



