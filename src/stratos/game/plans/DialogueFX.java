

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.graphics.sfx.*;
import stratos.util.*;



public class DialogueFX {
  
  
  private static boolean verbose = false;
  
  
  protected static TalkFX transcriptFor(Actor talks) {
    final Dialogue chat = (Dialogue) talks.matchFor(Dialogue.class);
    if (chat == null) return null;
    
    final Actor other = (talks == chat.other) ? chat.actor() : chat.other;
    final Target location = chat.location();
    final boolean report = verbose && (
      I.talkAbout == talks || I.talkAbout == other
    );
    if (report) {
      I.say("\nGetting transcript between "+talks+" and "+other);
      I.say("  Location is: "+location);
    }
    
    final Ephemera e = talks.world().ephemera;
    TalkFX match = (TalkFX) e.matchSprite(location, TalkFX.TALK_MODEL);
    if (match == null) {
      match = new TalkFX();
      e.addGhost(location, 2, match, 2.0f);
      if (report) I.say("  Creating new FX: "+match);
    }
    else {
      if (report) I.say("  Match found! "+match);
      e.updateGhost(location, 2, TalkFX.TALK_MODEL, 2.0f);
    }
    
    final Vec3D p = match.position;
    p.setTo(talks.position(null));
    p.add(other.position(null));
    p.scale(1 / 2f);
    p.z += Nums.max(talks.height(), other.height());
    if (report) I.say("  FX position: "+match.position);
    return match;
  }
  
  
}








