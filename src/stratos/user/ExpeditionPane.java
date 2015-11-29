/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class ExpeditionPane extends SelectionPane {
  
  final static String
    CAT_COLONISTS = "Colonists",
    CAT_SUPPLIES  = "Supplies",
    CATEGORIES[] = {};
  
  
  final Expedition expedition;
  
  
  public ExpeditionPane(HUD UI, Expedition expedition) {
    super(UI, null, null, true, CATEGORIES);
    this.expedition = expedition;
  }
  
  
  protected void updateText(Text header, Text detail, Text listing) {
    super.updateText(headerText, detailText, listingText);
    
    appendActor(expedition.leader(), listing);
    for (Actor a : expedition.advisors ()) appendActor(a, listing);
    for (Actor c : expedition.colonists()) appendActor(c, listing);
  }
  
  
  void appendActor(Actor a, Description d) {
    if (a == null || d == null) return;
    final Composite portrait = a.portrait(UI);
    if (portrait != null) Text.insert(portrait.texture(), 40, 40, true, d);
    d.append(" ");
    d.append(a);
    d.append("\n ("+a.mind.vocation()+")", Colour.LITE_GREY);
  }
}










