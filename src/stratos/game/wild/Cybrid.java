

package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.Species;
import stratos.graphics.widgets.Composite;
import stratos.user.BaseUI;



//  ...Everybody loves Cylon Terminators!

public class Cybrid extends Artilect {
  
  
  Actor template;
  
  
  public Cybrid(Base base) {
    super(base, Species.HUMAN);
  }
  
  
  public Cybrid(Session s) throws Exception {
    super(s);
    template = (Actor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(template);
  }
  
  
  protected ActorMind initAI() {
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
  
  
  protected String helpInfo() {
    return null;
  }
}





