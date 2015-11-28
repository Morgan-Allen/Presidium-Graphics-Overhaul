/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.economic.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;




public abstract class Artilect extends Actor {

  
  
  /**  Construction and save/load methods-
    */
  final static float
    FUEL_CELLS_REGEN = Stage.STANDARD_DAY_LENGTH;
  
  final static String
    FILE_DIR = "media/Actors/artilects/",
    XML_FILE = "ArtilectModels.xml";
  
  
  private static boolean verbose = false;
  
  final Species species;
  
  
  
  protected Artilect(Base base, Species s) {
    super();
    this.species = s;
    mind.setVocation(s);
    assignBase(base);
  }
  
  
  public Artilect(Session s) throws Exception {
    super(s);
    this.species = (Species) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(species);
  }
  
  
  public Species species() {
    return species;
  }
  

  protected ActorMind initMind() {
    final Artilect artilect = this;
    return new ActorMind(artilect) {
      
      protected Choice createNewBehaviours(Choice choice) {
        if (choice == null) choice = new Choice(actor);
        artilect.addChoices(choice);
        return choice;
      }
      
      public void updateAI(int numUpdates) {
        super.updateAI(numUpdates);
      }
      
      protected void addReactions(Target seen, Choice choice) {
        artilect.addReactions(seen, choice);
      }
      
      protected void putEmergencyResponse(Choice choice) {
        artilect.putEmergencyResponse(choice);
      }
    };
  }
  
  
  protected ActorRelations initRelations() {
    return new ActorRelations(this) {
      
      public float valueFor(Object object) {
        if (object == actor || object == actor.mind.home()) {
          return 1.0f;
        }
        else if (object instanceof Accountable) {
          final Base belongs = ((Accountable) object).base();
          if (belongs == actor.base()) return 1.0f;
          return -1.0f;
        }
        else return 0;
      }
    };
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
    if (seen instanceof Actor) {
      final Combat combat = new Combat(
        this, (Actor) seen, Combat.STYLE_EITHER, Combat.OBJECT_DESTROY
      );
      choice.add(combat);
    }
  }
  
  
  protected void putEmergencyResponse(Choice choice) {
    choice.add(new Retreat(this));
  }
  
  
  //  TODO:  Arrange for occasional scouting trips and active raids.
  //  (Intervals are slow, based on community size.)
  
  //  TODO:  Split this method out to the various subclasses and/or the ruins
  //  itself?
  
  protected void addChoices(Choice choice) {
    final boolean report = verbose && I.talkAbout == this;
    if (report) I.say("\n  Getting next behaviour for "+this);
    //
    //  Ascertain a few basic facts first-
    final boolean
      isDrone   = this instanceof Drone  ,
      isTripod  = this instanceof Tripod ,
      isCranial = this instanceof Cranial;
    final Property home = mind.home();
    Element guards = home == null ? this : (Element) home;
    //final float distance = Spacing.distance(this, guards) / Stage.SECTOR_SIZE;
    //
    //  Security and defence related tasks-
    choice.add(Patrolling.protectionFor(this, guards, Plan.IDLE));
    choice.add(JoinMission.attemptFor(this));
    choice.add(new Retreat(this));
    //
    //  Defend home site or retreat to different site (all).
    //  Respond to obelisk or tesseract presence (all).
    for (Target e : senses.awareOf()) if (e instanceof Actor) {
      choice.add(new Combat(this, (Actor) e));
    }
    if (home != null && ! home.staff().onShift(this)) {
      final Resting rest = new Resting(this, mind.home());
      rest.addMotives(Plan.MOTIVE_JOB, Plan.CASUAL);
      choice.add(rest);
    }
    //
    //  Launch an assault on a nearby settlement, if numbers are too large.
    //  Capture specimens and bring back to lair.
    //  (Tripod specialties.)
    if ((isTripod || isCranial) && home != null) {
      for (Target t : senses.awareOf()) {
        if (t instanceof Human) {
          final Human other = (Human) t;
          if (other.health.conscious()) continue;
          final Plan recovery = new BringPerson(this, other, home);
          choice.add(recovery.addMotives(Plan.MOTIVE_JOB, Plan.CASUAL));
        }
        if (t instanceof Artilect) {
          final Artilect other = (Artilect) t;
          if (other.health.conscious()) continue;
          final Plan recovery = new BringPerson(this, other, home);
          choice.add(recovery.addMotives(Plan.MOTIVE_JOB, Plan.CASUAL));
        }
      }
    }
    //
    //  Experiment upon/dissect/interrogate/convert any captives.
    //  Perform repairs on another artilect, or refurbish a new model.
    //  (Cranial specialties.)
    if (isCranial && home instanceof Venue) {
      final Venue venue = (Venue) mind.home();
      for (Actor other : venue.staff.lodgers()) {
        choice.add(new SpawnArtilect(this, other, venue));
      }
      final Ruins ruins = (Ruins) world.presences.randomMatchNear(
        Ruins.class, this, Stage.ZONE_SIZE
      );
      choice.add(nextSpawning(this, ruins));
    }
  }
  
  
  protected Plan nextSpawning(Actor actor, Ruins lair) {
    if (lair == null) return null;
    final Pick <Species> pick = new Pick <Species> ();
    
    for (Species s : Species.ARTILECT_SPECIES) {
      final float rating = 1f - lair.crowdRating(null, s);
      if (rating <= 0) continue;
      pick.compare(s, rating);
    }
    if (pick.result() == null) return null;
    
    final Artilect spawned = (Artilect) pick.result().sampleFor(base());
    return new SpawnArtilect(actor, spawned, lair);
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    if (isDoing(Resting.class, null)) {
      final float fuelInc = 1f / FUEL_CELLS_REGEN;
      gear.bumpItem(Outfits.POWER_CELLS, fuelInc);
    }
  }
  
  
  
  /**  Special Techniques-
    */
  final static Class BASE_CLASS = Artilect.class;
  final static String
    UI_DIR = "media/GUI/Powers/",
    FX_DIR = "media/SFX/";
  
  final static PlaneFX.Model
    DETONATE_BURST_FX = PlaneFX.imageModel(
      "detonate_burst_fx", BASE_CLASS,
      FX_DIR+"frag_burst.png",
      0.25f, 0.5f, 1.5f, true, false
    ),
    DETONATE_FRINGE_FX = PlaneFX.imageModel(
      "detonate_fringe_fx", BASE_CLASS,
      FX_DIR+"detonate_burst.png",
      0.25f, 0.5f, 1.5f, false, false
    ),
    POSITRON_BURST_FX = PlaneFX.imageModel(
      "positron_burst_fx", BASE_CLASS,
      FX_DIR+"positron_burst.png",
      0.5f, 0, 0, true, true
    ),
    IMPALE_BURST_FX = PlaneFX.imageModel(
      "impale_burst_fx", BASE_CLASS,
      FX_DIR+"penetrating_shot.png",
      1, 0, 0, true, false
    );
  final static ShotFX.Model
    POSITRON_BEAM_FX = new ShotFX.Model(
      "positron_beam_fx", BASE_CLASS,
      FX_DIR+"positron_beam.png",
      0, 0, 0.08f, 1.0f, true, true
    );
  
  final static int
    DETONATE_BASE_DAMAGE = 10,
    DETONATE_BASE_RADIUS =  2,
    DETONATE_USE_PERCENT = 33,
    IMPALE_DAMAGE_MIN    =  5,
    IMPALE_DAMAGE_MAX    = 15,
    POSITRON_DAMAGE_AVG  = 20,
    ASSEMBLY_DAY_REGEN   = 15,
    ASSEMBLY_MAX_PERCENT = 50,
    SHIELD_ABSORB_AVG    =  2;
  
  
  final static Technique DETONATE = new Technique(
    "Detonate", UI_DIR+"detonate.png",
    "Allows the unit to self-destruct, either upon death or as a deliberate "+
    "kamikaze tactic.  Deals base "+DETONATE_BASE_DAMAGE+" damage in a "+
    DETONATE_BASE_RADIUS+" tile radius, scaling with bulk.",
    BASE_CLASS, "detonate",
    MINOR_POWER         ,
    MILD_HARM           ,
    NO_FATIGUE          ,
    NO_CONCENTRATION    ,
    IS_PASSIVE_ALWAYS | IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.STRIKE, Action.QUICK
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (actor.health.injuryLevel() < 1f - (DETONATE_USE_PERCENT / 100f)) {
        return false;
      }
      if (
        current instanceof Combat &&
        PlanUtils.harmIntendedBy(subject, actor, true) > 0
      ) {
        return true;
      }
      return false;
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      if (passive && using.health.conscious()) return;
      final float
        radius = DETONATE_BASE_RADIUS * using.health.baseBulk(),
        damage = DETONATE_BASE_DAMAGE * using.health.baseBulk();
      //
      //  We try to ensure that we only explode in an enemy's face, rather than
      //  an ally's...
      boolean triggered = false;
      for (Actor a : PlanUtils.subjectsInRange(using, radius)) {
        final float value = PlanUtils.combatPriority(
          using, a, 0, 1, false, Plan.REAL_HARM
        );
        if (passive && a != using && value < 0) return;
        if (value > 0) triggered = true;
      }
      if (! triggered) return;
      
      super.applyEffect(using, subject, success, passive);
      //
      //  Then rig up SFX and deal damage.
      final Vec3D point = using.position(null);
      final Stage world = using.world();
      ActionFX.applyBurstFX(DETONATE_BURST_FX , point, 1, world);
      point.z += 0.1f;
      ActionFX.applyBurstFX(DETONATE_FRINGE_FX, point, 1, world);
      Wreckage.plantCraterAround(using, 0);
      
      for (Actor a : PlanUtils.subjectsInRange(using, radius)) {
        if (a != using) a.health.takeInjury(damage, false);
      }
      using.health.takeInjury(100, false);
    }
  };
  
  
  final static Technique IMPALE = new Technique(
    "Impale", UI_DIR+"artilect_impale.png",
    "Deals "+IMPALE_DAMAGE_MIN+" to "+IMPALE_DAMAGE_MAX+" damage in melee "+
    "combat while pinning the victim in place.",
    BASE_CLASS, "artilect_impale",
    MEDIUM_POWER        ,
    EXTREME_HARM        ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.STRIKE_BIG, Action.NORMAL
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      return current instanceof Combat && subject instanceof Actor;
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      if (! Combat.performGeneralStrike(
        actor, subject, Combat.OBJECT_DESTROY, actor.currentAction()
      )) return false;
      return super.checkActionSuccess(actor, subject);
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      
      if (! success) return;
      ActionFX.applyBurstFX(IMPALE_BURST_FX, subject, 0.5f, 0.5f);
      
      final Actor struck = (Actor) subject;
      float damage = roll(IMPALE_DAMAGE_MIN, IMPALE_DAMAGE_MAX);
      struck.health.takeInjury(damage, true);
      struck.forceReflex(Action.FALL, false);
    }
  };
  
  
  final static Technique POSITRON_BEAM = new Technique(
    "Positron Beam", UI_DIR+"positron_beam.png",
    "Deals up to "+(POSITRON_DAMAGE_AVG * 2)+" damage against stationary "+
    "targets.",
    BASE_CLASS, "positron_beam",
    MAJOR_POWER         ,
    EXTREME_HARM        ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.FIRE, Action.RANGED
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      return current instanceof Combat && canHit(subject);
    }
    
    
    private boolean canHit(Target subject) {
      if (subject.isMobile() && ((Mobile) subject).isMoving()) {
        return false;
      }
      return true;
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      if (! canHit(subject)) return false;
      if (! Combat.performGeneralStrike(
        actor, subject, Combat.OBJECT_DESTROY, actor.currentAction()
      )) return false;
      return super.checkActionSuccess(actor, subject);
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      
      final float damage = POSITRON_DAMAGE_AVG * Rand.avgNums(2) * 2;
      ActionFX.applyShotFX(
        POSITRON_BEAM_FX, POSITRON_BURST_FX,
        using, subject, success, 2, using.world()
      );
      
      if (success && subject instanceof Actor) {
        final Actor struck = (Actor) subject;
        struck.health.takeInjury(damage, true);
      }
      if (success && subject instanceof Placeable) {
        ((Placeable) subject).structure().takeDamage(damage);
      }
    }
  };
  
  
  final static Technique SHIELD_ABSORPTION = new Technique(
    "Shield Absorption", UI_DIR+"artilect_shield_absorb.png",
    "Allows the unit to absorb a portion of incoming attacks in order to "+
    "regenerate their own shields.",
    BASE_CLASS, "artilect_shield_absorb",
    MEDIUM_POWER    ,
    HARM_UNRATED    ,
    NO_FATIGUE      ,
    NO_CONCENTRATION,
    IS_PASSIVE_SKILL_FX | IS_NATURAL_ONLY, null, 0,
    STEALTH_AND_COVER
  ) {
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject, boolean reactive
    ) {
      if (! (subject instanceof Actor)) return false;
      final Actor strikes = (Actor) subject;
      if (strikes.actionFocus() != actor) return false;
      if (strikes.gear.meleeDeviceOnly()) return false;
      if (! strikes.isDoing(Combat.class, actor)) return false;
      return super.triggersPassive(actor, current, used, subject, reactive);
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return 0;
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      //
      //  TODO:  You need a Volley class to handle this properly.
      if (! success) return;
      ActionFX.applyShieldFX(using.gear.outfitType(), using, subject, true);
      
      float charge = SHIELD_ABSORB_AVG * Rand.num() * 2;
      using.gear.boostShields(charge, false);
    }
  };
  
  
  final static Technique IMPLANTATION = new Technique(
    "Implantation", UI_DIR+"artilect_implantation.png",
    "Converts an unconscious organic subject, living or dead, into a "+
    Cybrid.SPECIES+", which will usually awaken after a few hours.",
    BASE_CLASS, "artilect_implantation",
    MAJOR_POWER        ,
    HARM_UNRATED       ,
    NO_FATIGUE         ,
    MAJOR_CONCENTRATION,
    IS_ANY_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.BUILD, Action.NORMAL
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (! (subject instanceof Human)) return false;
      final Human focus = (Human) subject;
      return focus.health.organic() && ! focus.health.conscious();
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      
      final Human focus = (Human) subject;
      final Boarding place = focus.aboard();
      final Cybrid cybrid = new Cybrid(using.base(), focus);
      
      focus.exitWorld();
      cybrid.enterWorldAt(place, place.world());
      cybrid.mind.setHome(using.mind.home());
    }
  };
  
  
  final static Technique SELF_ASSEMBLY = new Technique(
    "Self Assembly", UI_DIR+"self_assembly.png",
    "Allows the unit to regenerate any injuries below "+ASSEMBLY_MAX_PERCENT+
    "% of total health.",
    BASE_CLASS, "self_assembly",
    MAJOR_POWER         ,
    NO_HARM             ,
    NO_FATIGUE          ,
    NO_CONCENTRATION    ,
    IS_PASSIVE_ALWAYS | IS_NATURAL_ONLY, null, 0,
    Action.FIRE, Action.RANGED
  ) {
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      
      final float
        injury    = using.health.injuryLevel(),
        minInjury = 1f - (ASSEMBLY_MAX_PERCENT / 100f),
        minRevive = (1 + minInjury) / 2;
      
      if (I.talkAbout == using) {
        I.say("\nCurrent injury: "+using.health.injuryLevel());
      }
      if (injury <= minRevive && ! using.health.conscious()) {
        using.health.setState(ActorHealth.STATE_ACTIVE);
      }
      if (injury <= minInjury) {
        return;
      }
      
      final float lift = ASSEMBLY_DAY_REGEN * 1f / Stage.STANDARD_DAY_LENGTH;
      using.health.liftInjury(lift);
    }
  };
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return ActorDescription.configSimplePanel(this, panel, UI);
  }
  
  
  protected static String nameWithBase(String base) {
    final StringBuffer nB = new StringBuffer(base);
    for (int n = 4; n-- > 0;) {
      if (Rand.yes()) nB.append((char) ('0' + Rand.index(10)));
      else nB.append((char) ('A'+Rand.index(26)));
    }
    return nB.toString();
  }
}


