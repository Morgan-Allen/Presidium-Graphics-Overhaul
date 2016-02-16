

package stratos.content.wip;
import static stratos.game.actors.Qualities.ASSEMBLY;
import static stratos.game.actors.Qualities.CHEMISTRY;
import static stratos.game.actors.Qualities.FIELD_THEORY;

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



public class TekPriestessSchool extends Venue {
  

  private static boolean
    verbose = false;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    TekPriestessSchool.class,
    "media/Buildings/schools and preserves/initiate_school.png", 6, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    TekPriestessSchool.class, "media/GUI/Buttons/hospice_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    TekPriestessSchool.class, "tek_priestess_school",
    "Tek Priestess School", Target.TYPE_SCHOOL, ICON,
    "<Tek Priestess School Description>",
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
  
  
  
  
  public TekPriestessSchool(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public TekPriestessSchool(Session s) throws Exception {
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



