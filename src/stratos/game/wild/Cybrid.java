/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.graphics.widgets.*;
import stratos.util.Nums;
import stratos.util.Rand;



//  ...Everybody loves Cylon Terminators!

public class Cybrid extends Artilect {

  
  final public static Species SPECIES = new Species(
    Cybrid.class,
    "Cybrid",
    "Cybrids are lesser minions of the Cranial, created from the lifeless "+
    "husk of former victims.  Their organic remains and a vestige of their "+
    "old memories can allow them to approach undetected.",
    null,
    null,
    Species.Type.ARTILECT, 1, 1, 1
  ) {
    public Actor sampleFor(Base base) { return new Cranial(base); }
  };
  
  Human template;
  
  
  public Cybrid(Base base) {
    this(base, new Human(
      (Background) Rand.pickFrom(Backgrounds.NATIVE_CIRCLES), base
    ));
  }
  
  
  public Cybrid(Base base, Human template) {
    super(base, SPECIES);
    
    traits.initAtts(10, 10, 10);
    health.initStats(
      1000,//lifespan
      1.0f,//bulk bonus
      1.0f,//sight range
      0.5f,//move speed,
      ActorHealth.ARTILECT_METABOLISM
    );
    
    this.template = template;
    final float injury = Nums.min(0.99f, template.health.injuryLevel());
    health.setInjuryLevel(injury);
    health.setState(ActorHealth.STATE_DEAD);
    for (Skill s : template.traits.skillSet()) {
      traits.setLevel(s, template.traits.traitLevel(s) / 2f);
    }
    
    skills.addTechnique(SELF_ASSEMBLY);
    skills.addTechnique(IMPLANTATION );
    
    attachSprite(template.sprite());
  }
  
  
  public Cybrid(Session s) throws Exception {
    super(s);
    template = (Human) s.loadObject();
    Human.initSpriteFor(this, template);
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
    };
  }
  
  
  
  
  public String fullName() {
    return template.fullName()+" (Cybrid)";
  }
  
  
  public Composite portrait(HUD UI) {
    return null;
  }
}





