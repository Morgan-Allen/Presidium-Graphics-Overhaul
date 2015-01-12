

package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.politic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
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
        UI, img.asTexture(), Mission.MISSION_ICON_LIT.asTexture(), info
      );
      this.mission = m;
    }
    
    protected void whenClicked() {
      final Base base = BUI.played();
      //
      //  If an existing mission matches this one, just select that instead.
      final Mission match = base.matchingMission(mission);
      if (match != null) {
        BUI.selection.pushSelection(match, true);
        return;
      }
      //
      //  Otherwise, create a new mission for the target.
      base.tactics.addMission(mission);
      BUI.selection.pushSelection(mission, true);
    }
  }
  
  
  //  TODO:  Just make this a function of the Contact mission.
  private class SummonsButton extends Button {
    final Actor subject;
    
    SummonsButton(BaseUI UI, Actor subject) {
      super(
        UI, Mission.SUMMONS_ICON.asTexture(),
        Mission.MISSION_ICON_LIT.asTexture(),
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
        BUI, Mission.STRIKE_ICON, "Destroy or capture subject",
        new StrikeMission(base, subject)
      ));
    }
    if (
      subject instanceof Actor ||
      subject instanceof Venue ||
      subject instanceof Item.Dropped
    ) {
      options.add(new OptionButton(
        BUI, Mission.SECURITY_ICON, "Secure or protect subject",
        new SecurityMission(base, subject)
      ));
    }
    if (
      subject instanceof Actor &&
      ((Actor) subject).base() != BUI.played()
    ) {
      options.add(new OptionButton(
        BUI, Mission.CONTACT_ICON, "Contact or negotiate with subject",
        new ContactMission(base, subject)
      ));
    }
    if (
      subject instanceof Tile
    ) {
      options.add(new OptionButton(
        BUI, Mission.RECON_ICON, "Surveil or follow subject",
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
    
    this.alignBottom(0, 0);
    this.alignHorizontal(0.5f, 0, 0);
    
    if (active) {
      this.relAlpha += DEFAULT_FADE_INC;
      if (relAlpha > 1) relAlpha = 1;
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








