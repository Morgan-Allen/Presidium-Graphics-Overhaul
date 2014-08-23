


package stratos.game.actors;
import stratos.game.building.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;

//NOTE:  I'm moving these methods here essentially for the sake of reducing
//clutter/space demands within the main Human or Actor classes.


//   TODO:  Adapt this to actors in general instead.
public class HumanDescription implements Qualities {
  
  
  final Human h;
  
  
  private HumanDescription(Human human) {
    this.h = human;
  }
  
  
  public static SelectionInfoPane configPanel(
    Human human, SelectionInfoPane panel, BaseUI UI
  ) {
    if (panel == null) panel = new SelectionInfoPane(
      UI, human, human.portrait(UI), "STATUS", "SKILLS", "PROFILE"
    );
    final int categoryID = panel.categoryID();
    final Description d = panel.detail();
    final HumanDescription HD = new HumanDescription(human);
    
    if (categoryID == 0) HD.describeStatus(d, UI);
    if (categoryID == 1) HD.describeSkills(d, UI);
    if (categoryID == 2) HD.describeProfile(d, UI);
    return panel;
  }
  
  
  private void describeStatus(Description d, HUD UI) {
    //
    //  Describe your job, place of work, and current residence:
    d.append("Is: "); h.describeStatus(d);
    final String VN = h.vocation().nameFor(h);
    d.append("\nVocation: ");
    if (h.mind.work() != null) {
      d.append(VN+" at ");
      d.append(h.mind.work());
    }
    else d.append("Unemployed "+VN);
    d.append("\nResidence: ");
    if (h.mind.home() != null) {
      d.append(h.mind.home());
    }
    else d.append("Homeless");
    //
    //  Describe your current health, outlook, or special FX.
    d.append("\n\nHealth: (Max "+(int) h.health.maxHealth()+")");
    final Batch <String> healthDesc = h.health.conditionsDesc();
    for (String desc : healthDesc) {
      d.append("\n  "+desc);
    }
    final Batch <Condition> conditions = h.traits.conditions();
    for (Condition c : conditions) {
      final String desc = h.traits.levelDesc(c);
      if (desc != null) d.append("\n  "+desc);
    }
    if (healthDesc.size() == 0 && conditions.size() == 0) {
      d.append("\n  Okay");
    }
    //
    //  Describe your current gear and anything carried.
    d.append("\n\nInventory: ");
    
    final Item device = h.gear.deviceEquipped();
    if (device != null) d.append("\n  "+device);
    else d.append("\n  No device");
    d.append(" ("+((int) h.gear.attackDamage())+")");
    
    final Item outfit = h.gear.outfitEquipped();
    if (outfit != null) d.append("\n  "+outfit);
    else d.append("\n  Nothing worn");
    d.append(" ("+((int) h.gear.armourRating())+")");
    
    final int MS = (int) h.gear.maxShields(), SC = (int) h.gear.shieldCharge();
    if (MS > 0 || SC > 0) {
      d.append("\n  Shields "+SC+" (Max "+MS+")");
    }
    
    for (Item item : h.gear.allItems()) d.append("\n  "+item);
    d.append("\n  "+((int) h.gear.credits())+" Credits");
    
    final int FC = (int) h.gear.fuelCells();
    if (FC > 0) d.append("\n  Fuel Cells: "+FC);
  }
  
  
  private void describeSkills(Description d, HUD UI) {
    //
    //  Describe attributes, skills and psyonic techniques.
    d.append("Attributes: ");
    for (Skill skill : h.traits.attributes()) {
      final int level = (int) h.traits.traitLevel(skill);
      final int bonus = (int) h.traits.effectBonus(skill);
      d.append("\n  "+skill.name+" "+level+" ");
      d.append(Skill.attDesc(level), Skill.skillTone(level));
      if (bonus != 0) {
        d.append((bonus >= 0 ? " (+" : " (-")+Math.abs(bonus)+")");
      }
    }
    d.append("\n\nSkills: ");
    final List <Skill> sorting = new List <Skill> () {
      protected float queuePriority(Skill skill) {
        return h.traits.traitLevel(skill);
      }
    };
    
    for (Skill skill : h.traits.skillSet()) sorting.add(skill);
    sorting.queueSort();
    for (Skill skill : sorting) {
      final int level = (int) h.traits.traitLevel(skill);
      final int bonus = (int) (
          h.traits.rootBonus(skill) +
          h.traits.effectBonus(skill)
      );
      final Colour tone = Skill.skillTone(level);
      d.append("\n  "+skill.name+" "+level+" ", tone);
      if (bonus != 0) {
        d.append((bonus >= 0 ? "(+" : "(-")+Math.abs(bonus)+")");
      }
    }
    
    d.append("\n\nTechniques: ");
    for (Technique p : h.skills.known) {
      d.append("\n  "+p.name);
    }
  }
  
  
  private void describeProfile(Description d, HUD UI) {
    //
    //  Describe background, personality, relationships and memories.
    //  TODO:  Allow for a chain of arbitrary vocations in a career?
    d.append("Background: ");
    d.append("\n  "+h.career.birth()+" on "+h.career.homeworld());
    d.append("\n  Trained as "+h.career.vocation().nameFor(h));
    d.append("\n  "+h.traits.levelDesc(ORIENTATION));
    d.append(" "+h.traits.levelDesc(GENDER));
    d.append("\n  Age: "+h.health.exactAge()+" ("+h.health.agingDesc()+")");
    
    ///d.appendList("\n\nAppearance: " , descTraits(h.traits.physique   ()));
    d.appendList("\n\nPersonality: ", descTraits(h.traits.personality(), h));
    //d.appendList("\n\nMutations: "  , descTraits(traits.mutations  ()));
    
    d.append("\n\nRelationships: ");
    for (Relation r : h.relations.relations()) {
      if (! (r.subject instanceof Actor)) continue;
      d.append("\n  ");
      d.append(r.subject);
      d.append(" ("+Relation.describe(r)+")");
    }
  }
  
  
  public static List <Trait> sortTraits(
    Batch <? extends Trait> traits, final Actor h
  ) {
    final List <Trait> sorting = new List <Trait> () {
      protected float queuePriority(Trait r) {
        return h.traits.traitLevel(r);
      }
    };
    for (Trait t : traits) sorting.queueAdd(t);
    return sorting;
  }
  
  
  public static Batch <String> descTraits(
    Batch <? extends Trait> traits, final Actor h
  ) {
    final List <Trait> sorting = sortTraits(traits, h);
    final Batch <String> desc = new Batch <String> ();
    for (Trait t : sorting) desc.add(h.traits.levelDesc(t));
    return desc;
  }
}



