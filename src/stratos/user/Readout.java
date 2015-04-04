/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.actors.*;
import stratos.game.base.BaseTransport;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class Readout extends UIGroup {
  
  
  final BaseUI UI;
  final Text read;
  
  
  protected Readout(BaseUI UI) {
    super(UI);
    this.UI = UI;
    
    this.read = new Text(UI, SelectionPane.INFO_FONT);
    read.alignToFill();
    read.scale = 0.75f;
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
    final int credits = played.finance.credits();
    if (credits >= 0) read.append(credits+" Credits", Colour.WHITE);
    else read.append((0 - credits)+" In Debt", Colour.YELLOW);
    read.append("   ");
    
    //
    //  Then psy points-
    final boolean ruled = played.ruler() != null;
    final ActorHealth RH = ruled ? played.ruler().health : null;
    int psyPoints = 0, maxPsy = 0;
    if (played.ruler() != null) {
      maxPsy    += RH.maxHealth();
      psyPoints += maxPsy - RH.fatigue();
      read.append("   Psy Points: ");
      read.append(    I.lengthen(psyPoints, 2, true));
      read.append("/"+I.lengthen(maxPsy   , 2, true));
      read.append("   ");
    }
    
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
    //  Finally, include the set of provisions and their supply/demand:
    final BaseTransport p = played.transport;
    final Traded provs[] = { Economy.POWER, Economy.WATER };
    
    for (Traded type : provs) {
      read.append("  ("+type.name+": ");
      int supply = (int) p.allSupply(type);
      int demand = (int) p.allDemand(type);
      read.append(supply+"/"+demand+")");
    }
  }
  
}
