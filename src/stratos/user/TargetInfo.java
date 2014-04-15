

package stratos.user;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.graphics.common.ImageAsset;
import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.*;
import stratos.start.PlayLoop;
import stratos.util.*;
import com.badlogic.gdx.math.Vector2;



public class TargetInfo extends UIGroup {
  
  
  final int
    OB_SIZE = 40,
    OB_MARGIN  = 2;
  //  need health bar, title, status fx,
  
  final BaseUI BUI;
  final Target subject;
  protected boolean active = true;
  
  
  public TargetInfo(BaseUI UI, Target subject) {
    super(UI);
    this.BUI = UI;
    this.subject = subject;
    this.relAlpha = 0;
    setup();
  }
  
  
  private class OptionButton extends Button {
    final Mission mission;
    
    OptionButton(BaseUI UI, ImageAsset img, String info, Mission m) {
      super(
        UI, img.asTexture(), MissionsTab.MISSION_ICON_LIT.asTexture(), info
      );
      this.mission = m;
    }
    
    protected void whenClicked() {
      BUI.played().addMission(mission);
      BUI.selection.pushSelection(mission, true);
    }
  }
  
  
  private void setup() {
    final BaseUI BUI = (BaseUI) UI;
    final Base base = BUI.played();
    final List <OptionButton> options = new List <OptionButton> ();
    
    if (
      subject instanceof Actor ||
      subject instanceof Venue
    ) {
      options.add(new OptionButton(
        BUI, MissionsTab.STRIKE_ICON, "Neutralise subject",
        new StrikeMission(base, subject)
      ));
    }
    if (
      subject instanceof Actor ||
      subject instanceof Venue ||
      subject instanceof Item
    ) {
      options.add(new OptionButton(
        BUI, MissionsTab.SECURITY_ICON, "Secure subject",
        new SecurityMission(base, subject)
      ));
    }
    if (
      subject instanceof Actor
    ) {
      options.add(new OptionButton(
        BUI, MissionsTab.CONTACT_ICON, "Contact subject",
        new ContactMission(base, subject)
      ));
    }
    if (
      subject instanceof Tile
    ) {
      options.add(new OptionButton(
        BUI, MissionsTab.RECON_ICON, "Surveil subject",
        new ReconMission(base, (Tile) subject)
      ));
    }
    
    int sumWide = options.size() * (OB_SIZE + OB_MARGIN), across = 0;
    for (OptionButton option : options) {
      option.absBound.set(across - (sumWide / 2), 0, OB_SIZE, OB_SIZE);
      option.attachTo(this);
      across += OB_SIZE + OB_MARGIN;
    }
  }
  
  
  protected void updateState() {
    //  TODO:  This still needs to be fixed when fading away...
    
    final Vec3D screenPos = BUI.viewTracking.screenPosFor(subject);
    final float
      SS = BUI.viewTracking.view.screenScale(),
      high = subject.height() * SS,
      wide = subject.radius() * SS * 2;
    final int highAdjust = (int) (
      screenPos.y + (subject instanceof Mobile ? -(wide + OB_SIZE) : high)
    );
    this.relBound.set(0, 0, 0, 0);
    this.absBound.set((int) screenPos.x, highAdjust, 0, 0);
    
    if (active) {
      if (BUI.viewTracking.fullyLocked(subject)) {
        this.relAlpha += DEFAULT_FADE_INC;
        if (relAlpha > 1) relAlpha = 1;
      }
    }
    else {
      this.relAlpha -= DEFAULT_FADE_INC;
      if (relAlpha <= 0) detach();
    }
    
    super.updateState();
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    if ((! active) || relAlpha < 1) return null;
    return super.selectionAt(mousePos);
  }
}








