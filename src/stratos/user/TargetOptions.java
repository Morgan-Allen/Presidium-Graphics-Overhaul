

package stratos.user;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.plans.*;
//import stratos.game.civilian.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
//import stratos.start.PlayLoop;
import stratos.util.*;

import com.badlogic.gdx.math.Vector2;



//  TODO:  This needs to be re-located in a consistent position near the foot
//  of the screen.


public class TargetOptions extends UIGroup {
  
  
  final int
    OB_SIZE   = 40,
    OB_MARGIN = 2;
  //  need health bar, title, status fx,
  
  final BaseUI BUI;
  final Target subject;
  protected boolean active = true;
  
  
  public TargetOptions(BaseUI UI, Target subject) {
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
  
  
  //  TODO:  Just make this a function of the Contact button, if the subject
  //         doesn't need much persuasion.
  private class SummonsButton extends Button {
    final Actor subject;
    
    SummonsButton(BaseUI UI, Actor subject) {
      super(
        UI, MissionsTab.SUMMONS_ICON.asTexture(),
        MissionsTab.MISSION_ICON_LIT.asTexture(),
        "Summon this subject to the bastion"
      );
      this.subject = subject;
    }
    
    
    protected void whenClicked() {
      final Base base = ((BaseUI) UI).played();
      if (! Summons.canSummon(subject, base)) {
        //  TODO:  Print this message on-screen in some form.
        I.say("CANNOT SUMMON AT THE MOMENT");
        return;
      }
      Summons.beginSummons(subject);
    }
  }
  
  
  private void setup() {
    final BaseUI BUI = (BaseUI) UI;
    final Base base = BUI.played();
    final List <Button> options = new List <Button> ();
    
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
      subject instanceof Actor &&
      ((Actor) subject).base() != BUI.played()
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
    
    if (Summons.canSummon(subject, BUI.played())) {
      options.add(new SummonsButton(BUI, (Actor) subject));
    }
    
    
    int sumWide = options.size() * (OB_SIZE + OB_MARGIN), across = 0;
    for (Button option : options) {
      option.alignToArea(across - (sumWide / 2), 0, OB_SIZE, OB_SIZE);
      option.attachTo(this);
      across += OB_SIZE + OB_MARGIN;
    }
  }
  
  
  protected void updateState() {
    //  TODO:  This still needs to be fixed when fading away...
    
    final Vec3D screenPos = BUI.tracking.screenPosFor(subject);
    final float
      SS   = BUI.tracking.view.screenScale(),
      high = subject.height() * SS,
      wide = subject.radius() * SS * 2;
    final int highAdjust = (int) (
      screenPos.y + (subject instanceof Mobile ?
        -(wide + OB_SIZE) : (0 - OB_SIZE)
      )
    );
    this.relBound.set(0, 0, 0, 0);
    this.absBound.set((int) screenPos.x, highAdjust, 0, 0);
    //  ...You need the parent position for this to work.
    
    //absBound.incX(0 - trueBounds().xpos());
    //absBound.incY(0 - trueBounds().ypos());
    
    if (active) {
      if (BUI.tracking.fullyLocked(subject)) {
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








