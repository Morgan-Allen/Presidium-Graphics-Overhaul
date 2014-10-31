

package stratos.game.civilian;
import stratos.game.common.*;
import stratos.game.plans.Dialogue;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.util.*;


//
//  TODO:  Have the dialogue class rely on this.  You'll also want to use a
//  more elaborate conversation-tree.


public class Transcript implements Stage.Visible, Session.Saveable {
  
  
  
  final Stack <Dialogue> inputs = new Stack <Dialogue> ();
  final TalkFX chat = new TalkFX();
  //Target around;
  
  
  protected Transcript(Dialogue starts) {
    inputs.include(starts);
  }
  
  
  void onExpiry(Stage world) {
    world.ephemera.addGhost(null, 3, chat, 2.0f);
  }
  
  
  public Transcript(Session s) throws Exception {
    s.cacheInstance(this);
    s.loadObjects(inputs);
    chat.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjects(inputs);
    chat.saveTo(s.output());
  }
  
  
  
  
  public void update() {
    //  Interpolate position correctly?
  }
  
  
  public void renderFor(Rendering r, Base b) {
    chat.readyFor(r);
  }
  
  
  public Sprite sprite() {
    return chat;
  }
  
  
  //  actors, traits, skills, vocations, activities.
  
  //  "...speaking of which"
  //  "Do you know X?"
  //  "Of course!" / "Can't say I do..."
  //  "Well/Yeah, X is a great/awful guy."
  
  //  "I/X was <doing x> recently."
  //  "It was fun!/awful!" / "That sounds fun!/awful!"
  //  "Because I was <trait y>."
  //  "...I need to practice <skill z>."
  //  "Well, here's how you do it..."
  //  "Like this?"
  //  "Exactly!"
  //  "Hmm.  Try again."
  
  //  "...I'd like to be a <background w>."
  //  "You need to be/can't be <trait t>."
  //  "You need to know <skill s>."
}








/*
static Batch <Object> associationsFor(Human actor) {
  final Batch <Object> assocs = new Batch <Object> ();
  
  assocs.add(actor.career().birth());
  assocs.add(actor.career().homeworld());
  assocs.add(actor.career().vocation());
  
  for (Trait t : actor.traits.personality()) assocs.add(t);
  for (Skill s : actor.traits.skillSet()) assocs.add(s);
  
  //for (Plan p : actor.memories.remembered()) assocs.add(p);
  for (Relation r : actor.memories.relations()) assocs.add(r.subject);
  return assocs;
}


static Batch <Object> associationsFor(Trait trait) {
  //
  //  So... actors who have that trait, or activities associated with that
  //  trait, or vocations with that trait, or associated homeworlds.
  return null;
}
//*/


