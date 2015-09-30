


package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  NOTE:  I'm moving these methods here essentially for the sake of reducing
//  clutter/space demands within the main Human or Actor classes.

//  TODO- MAKE THIS ANOTHER INFO-PANE

public class ActorDescription implements Qualities {
  
  
  private static boolean
    showPriorities = true ,
    showRelations  = true ;
  
  final static String
    CAT_GEAR      = "GEAR"  ,
    CAT_SKILLS    = "SKILLS",
    CAT_AGENDA    = "AGENDA",
    CAT_RELATIONS = "PEERS" ;
  
  final Actor h;
  
  
  private ActorDescription(Actor human) {
    this.h = human;
  }
  
  
  public static SelectionPane configPanel(
    Actor human, SelectionPane panel, BaseUI UI
  ) {
    //  TODO:  Break into Inventory, Skills, Traits and Relations.
    
    if (panel == null) panel = new SelectionPane(
      UI, human, human.portrait(UI), true,
      CAT_GEAR, CAT_SKILLS, CAT_AGENDA, CAT_RELATIONS
    );
    final String category = panel.category();
    final Description d = panel.detail(), l = panel.listing();
    
    final ActorDescription HD = new ActorDescription(human);
    HD.describeStatus(d, UI);
    
    if (category == CAT_GEAR     ) HD.describeGear     (l, UI);
    if (category == CAT_SKILLS   ) HD.describeSkills   (l, UI);
    if (category == CAT_AGENDA   ) HD.describeAgenda   (l, UI);
    if (category == CAT_RELATIONS) HD.describeRelations(l, UI);
    return panel;
  }
  
  
  private void describeStatus(Description d, HUD UI) {
    //
    //  Describe your job, place of work, and current residence-
    final Background job = h.mind.vocation();
    if (job != null) {
      d.append("\nVocation: "+job.nameFor(h));
    }
    if (h.mind.work() != null) {
      d.append("\nEmployed at: ");
      d.append(h.mind.work());
    }
    if (h.mind.home() != null) {
      d.append("\nResident at: ");
      d.append(h.mind.home());
    }
    //
    //  Then describe the actor's current behaviour-
    d.append("\nStatus: ");
    /*
    if (true) {
      Action current = h.currentAction();
      d.append(current);
      
      if (h.inWorld()) d.append("\n  In world!");
      else d.append("\n  Not in world!");
      return;
    }
    //*/
    
    h.describeStatus(d, h);
    if (showPriorities) {
      final Behaviour b = h.mind.rootBehaviour();
      float priority = Plan.ROUTINE;
      if (b instanceof Plan) {
        priority = ((Plan) b).priorityEval;
      }
      else if (h.currentAction() != null) {
        priority = h.currentAction().priorityFor(h);
      }
      else if (h.health.asleep()) {
        priority = Resting.sleepPriority(h);
      }
      if (priority > 0 || true) {
        d.append(" ("+Plan.priorityDescription(priority)+")");
      }
    }
    //
    //  Then describe your physical condition-
    d.append("\n\nCondition:");
    final int
      IL = (int) h.health.injury    (),
      FL = (int) h.health.fatigue   (),
      MH = (int) h.health.maxHealth ();
    
    d.append("\n  Health: "+(MH - IL)+"/"+MH);
    if (FL > 0) d.append(" (Fatigue "+FL+")");
    
    final int
      hunger = (int) (h.health.hungerLevel() * 100),
      morale = (int) (h.health.moraleLevel() * 100);
    d.append("\n  Morale: "+morale+"%");
    if (hunger > 0) d.append(" (Hunger "+hunger+"%)");
    //
    //  And describe any special status FX-
    final Batch <String> healthDesc = new Batch <String> ();
    for (Trait t : h.traits.conditions()) {
      healthDesc.add(h.traits.description(t));
    }
    d.append("\n  ");
    for (String s : healthDesc) if (s != null) {
      d.append(s);
      d.append(" ");
    }
    if (h.mind.work() == null) d.append("Unemployed ");
    if (h.mind.home() == null) d.append("Homeless "  );
  }
  
  
  private void describeGear(Description d, HUD UI) {
    //
    //  First, describe major pieces of equipment:
    d.append("Outfit & Equipment: ");
    final int
      MS = (int) h.gear.maxShields  (),
      SC = (int) h.gear.shieldCharge(),
      PC = (int) h.gear.powerCells  ();
    final Item device = h.gear.deviceEquipped();
    if (device != null) {
      d.append("\n  "+device+" ("+((int) h.gear.totalDamage())+")");
      if (PC > 0) d.append(" (Power "+PC+")");
    }
    final Item outfit = h.gear.outfitEquipped();
    final boolean showShields = MS > 0 || SC > 0;
    if (outfit != null) {
      d.append("\n  "+outfit+" ("+((int) h.gear.totalArmour())+")");
    }
    else if (showShields) d.append("\n  No outfit");
    if (showShields) d.append(" (Shields "+SC+"/"+MS+")");
    //
    //  Then any other items carried, including current credits:
    d.append("\n\nCarried: ");
    final Batch <Item> carried = h.gear.allItems();
    for (Item item : carried) d.append("\n  "+item);
    final int credits = (int) h.gear.allCredits();
    if (credits > 0) {
      d.append("\n  "+(int) h.gear.taxedCredits()+" Credits Saved");
      d.append(  " ("+(int) h.gear.unTaxed     ()+" Untaxed)"     );
    }
    if (credits < 0) d.append("\n  "+(0 - credits)+" Credits in debt");
  }
  
  
  private void describeSkills(Description d, HUD UI) {
    //
    //  Describe attributes, skills and psyonic techniques.
    //  TODO:  INCLUDE ICONS HERE?
    
    d.append("Job Skills: ");
    final List <Skill> sorting = new List <Skill> () {
      protected float queuePriority(Skill skill) {
        return 0 - h.traits.traitLevel(skill);
      }
    };
    final Background job = h.mind.vocation();
    for (Skill skill : job.skills()) sorting.add(skill);
    sorting.queueSort();
    for (Skill skill : sorting) descSkill(skill, d);
    
    d.append("\n\nOther Skills: ");
    sorting.clear();
    for (Skill skill : h.traits.skillSet()) {
      if (! job.skills().includes(skill)) sorting.add(skill);
    }
    sorting.queueSort();
    for (Skill skill : sorting) descSkill(skill, d);
    
    d.append("\n\nTechniques: ");
    for (Technique p : h.skills.knownTechniques()) {
      d.append("\n  "+p.name);
    }
    //if (h.skills.known.size() == 0) d.append("\n  None known");
  }
  
  
  private void descSkill(Skill skill, Description d) {
    final int
      baseLevel = (int) h.traits.traitLevel(skill),
      rootBonus = (int) h.traits.bonusFrom (skill.parent),
      bonus     = (int) h.traits.effectBonus(skill);
    d.append("\n  "+skill.name);
    
    Colour c = Colour.WHITE;
    if (bonus > 0) c = Colour.GREEN;
    if (bonus < 0) c = Colour.RED  ;
    d.append(" "+baseLevel+" ("+rootBonus+")", c);
    if (bonus != 0) d.append(" ("+bonus+")", c);
  }
  
  
  
  private void describeAgenda(Description d, HUD UI) {
    //
    //  Describe background, personality, relationships and memories.
    //  TODO:  Allow for a chain of arbitrary vocations in a career.  Later!
    
    d.append("Background: ");
    if (h instanceof Human) {
      final Career career = ((Human) h).career();
      d.append("\n  "+career.birth()+" on "+career.homeworld());
      d.append("\n  Trained as "+career.vocation().nameFor(h));
      d.append("\n  "+h.traits.description(ORIENTATION));
      d.append(" "+h.traits.genderDescription());
      d.append("\n  Age: "+h.health.exactAge()+" ("+h.health.agingDesc()+")");
    }
    
    d.appendList("\n\nPersonality: ", descTraits(h.traits.personality(), h));
    
    d.append("\nTo Do:");
    for (Behaviour b : h.mind.todoList()) {
      d.append("\n  ");
      b.describeBehaviour(d);
    }
    /*
    d.append("\n\nAttributes: ");
    for (Skill skill : h.traits.attributes()) {
      final int level = (int) h.traits.traitLevel(skill);
      final int bonus = (int) h.traits.bonusFrom(skill);
      d.append("\n  "+skill.name+" "+level+" ");
      d.append((bonus >= 0 ? " (+" : " (-")+Nums.abs(bonus)+")");
    }
    //*/
  }
  
  
  private void describeRelations(Description d, HUD UI) {
    
    final List <Relation> sorting = new List <Relation> () {
      protected float queuePriority(Relation r) {
        if (! (r.subject instanceof Actor)) return 0 - (10 + r.value());
        return 0 - r.value();
      }
    };
    Visit.appendTo(sorting, h.relations.relations());
    sorting.queueSort();
    
    d.append("Relationships: ");
    for (Relation r : sorting) {
      d.append("\n  ");
      d.append(r.subject);
      if (showRelations) {
        final int percent = (int) (r.value() * 100);
        d.append(" "+percent+"%");
      }
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
    for (Trait t : sorting) desc.add(h.traits.description(t));
    return desc;
  }
  
  
  
  /**  Abbreviated version for simpler creatures:
    */
  public static SelectionPane configSimplePanel(
    Actor actor, SelectionPane panel, BaseUI UI
  ) {
    if (panel == null) panel = new SelectionPane(
      UI, actor, actor.portrait(UI), true
    );
    final Description d = panel.detail(), l = panel.listing();
    
    final ActorDescription HD = new ActorDescription(actor);
    HD.describeStatus(d, UI);
    
    final Batch <Skill> skills = actor.traits.skillSet();
    if (skills.size() > 0) {
      l.append("\n\nSkills: ");
      for (Skill skill : skills) {
        final int level = (int) actor.traits.traitLevel(skill);
        l.append("\n  "+skill.name+" "+level+" ");
        l.append(Skill.skillDesc(level), Skill.skillTone(level));
      }
    }
    
    final Series <Technique> known = actor.skills.knownTechniques();
    if (known.size() > 0) {
      l.append("\n\nTechniques: ");
      for (Technique p : known) {
        l.append("\n  "+p.name);
      }
    }
    
    final Series <Item> carried = actor.gear.allItems();
    if (carried.size() > 0) {
      l.append("\n\nCarried: ");
      for (Item item : carried) l.append("\n  "+item);
    }
    
    l.append("\n\n");
    l.append(actor.species().info, Colour.LITE_GREY);
    
    return panel;
  }
}



