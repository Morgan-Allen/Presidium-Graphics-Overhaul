

package stratos.user;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;



public class Readout extends UIGroup {
  
  
  final BaseUI UI;
  final Text read;
  
  
  protected Readout(BaseUI UI) {
    super(UI);
    this.UI = UI;
    
    this.read = new Text(UI, SelectionInfoPane.INFO_FONT);
    read.alignToFill();
    read.attachTo(this);
  }
  
  
  protected void updateState() {
    super.updateState();
    
    final Stage world = UI.world();
    final Base played = UI.played();
    
    if (read == null) return;
    read.setText("");
    //
    //  Credits first-
    final int credits = played.credits();
    if (credits >= 0) read.append(credits+" Credits", Colour.WHITE);
    else read.append((0 - credits)+" In Debt", Colour.YELLOW);
    read.append("   ");
    //
    //  Then time and date-
    final float
      time = world.currentTime() / Stage.STANDARD_DAY_LENGTH;
    final int
      days  = (int) time,
      hours = (int) ((time - days) * 24);
    String hS = hours+"00";
    while (hS.length() < 4) hS = "0"+hS;
    String dS = "Day "+days+" "+hS+" Hours";
    read.append(dS);
    //
    //  And finally current psy points-
    final boolean ruled = played.ruler() != null;
    final ActorHealth RH = ruled ? played.ruler().health : null;
    final int PS = ruled ? 2 * (int) RH.maxConcentration() : 0;
    float psyPoints = 0;
    if (played.ruler() != null) {
      psyPoints += played.ruler().health.concentration();
      psyPoints *= PS / RH.maxConcentration();
    }
    if (PS > 0 && psyPoints > 0) {
      read.append("   Psy Points: ");
      float a = psyPoints / PS;
      Colour tone = new Colour().set((1 - a) / 2, a, (1 - a), 1);
      while (--psyPoints > 0) {
        read.append("|", tone);
        a = psyPoints / PS;
        tone = new Colour().set((1 - a) / 2, a, (1 - a), 1);
        tone.setValue(1);
      }
      if ((psyPoints + 1) > 0) {
        tone.a = psyPoints + 1;
        read.append("|", tone);
      }
    }
  }
  
}
