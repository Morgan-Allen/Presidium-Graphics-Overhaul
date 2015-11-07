/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.graphics.widgets.Composite;
import stratos.user.BaseUI;
import stratos.util.Rand;



//  ...Everybody loves Cylon Terminators!

public class Cybrid extends Artilect {
  
  
  Actor template;
  
  
  public Cybrid(Base base) {
    this(base, new Human(
      (Background) Rand.pickFrom(Backgrounds.NATIVE_CIRCLES), base
    ));
  }
  
  
  public Cybrid(Base base, Actor template) {
    super(base, Human.SPECIES);
    this.template = template;
    
    skills.addTechnique(SELF_ASSEMBLY);
    skills.addTechnique(SLOUGH_FLESH);
  }
  
  
  public Cybrid(Session s) throws Exception {
    super(s);
    template = (Actor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(template);
  }
  
  
  protected ActorMind initMind() {
    return new HumanMind(this) {
      protected Choice createNewBehaviours(Choice choice) {
        return super.createNewBehaviours(choice);
      }
      
      protected void addReactions(Target seen, Choice choice) {
        super.addReactions(seen, choice);
      }
      
      public void assignMaster(Actor master) {
        // TODO Auto-generated method stub
        super.assignMaster(master);
      }
    };
  }
  
  
  
  
  public String fullName() {
    return null;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
}





