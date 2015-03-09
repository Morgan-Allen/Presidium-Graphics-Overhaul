

package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;




public class ServiceHatch extends Venue {
  
  
  /**  Data fields, constructors and save/load methods:
    */
  private static boolean verbose = true;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    ServiceHatch.class, "media/Buildings/civilian/access_hatch_0.png", 2, 1
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    ServiceHatch.class, "media/GUI/Buttons/access_hatch_button.gif"
  );
  
  final static VenueProfile PROFILE = new VenueProfile(
    ServiceHatch.class, "service_hatch", "Service Hatch",
    2, 1, ENTRANCE_NONE, Bastion.PROFILE
  );
  
  
  
  public ServiceHatch(Base base) {
    super(PROFILE, base);
    attachModel(MODEL);
  }
  

  public ServiceHatch(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Updates and economic methods-
    */
  public Background[] careers() { return null; }
  public Traded[] services() { return null; }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "service_hatch");
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String helpInfo() {
    return
      "Service Hatches provide additional life support and help to combat "+
      "squalor, but can also allow entry to dangerous vermin.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_ENGINEER;
  }
}










