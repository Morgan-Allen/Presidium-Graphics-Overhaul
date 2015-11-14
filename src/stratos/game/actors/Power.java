/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.start.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  Make this available to actors, once the feedback mechanism is in
//  place.  (This will most likely require individual instancing.)
//  TODO:  Merge with Techniques for the purpose.


public abstract class Power extends Technique {
  
  
  private static boolean
    applyVerbose = true ;
  
  final static String
    IMG_DIR = "media/GUI/Powers/",
    SFX_DIR = "media/SFX/";
  
  final static Class BC = Power.class;
  
  
  Power(
    String name, String uniqueID, String imgFile, String helpInfo,
    Skill skillUsed, int minLevel
  ) {
    
    //  TODO:  FILL THESE IN!
    
    super(
      name, IMG_DIR+imgFile, helpInfo,
      Power.class, uniqueID,
      0, 
      0,
      0,
      0,
      Technique.IS_SOVEREIGN_POWER,
      skillUsed, minLevel, Action.PSY_QUICK,
      Action.QUICK | Action.RANGED  //  TODO- CUSTOMISE!
    );
  }
  
  
  public abstract boolean appliesTo(Actor caster, Target selected);
  public abstract int costFor(Actor caster, Target selected);
  
  public abstract boolean finishedWith(
    Actor caster, String option,
    Target selected, Target hovered
  );
  
  
  public String[] options() {
    return null;
  }
  
  
  public float passiveBonus(Actor using, Skill skill, Target subject) {
    return 0;
  }
  
  
  final public static PlaneFX.Model
    
    REMOTE_VIEWING_FX_MODEL = PlaneFX.imageModel(
      "RV_swirl_fx", Power.class,
      SFX_DIR+"remote_viewing.png", 0.5f, -360, 1, false, true
    ),
    
    LIGHT_BURST_MODEL = PlaneFX.imageModel(
      "RV_burst_fx", Power.class,
      SFX_DIR+"light_burst.png", 0.5f, 0, 1, true, true
    ),
    
    KINESTHESIA_FX_MODEL = PlaneFX.imageModel(
      "kinesthesia_fx", Power.class,
      SFX_DIR+"kinesthesia.png", 0.5f, 360, 0, true, true
    ),
    
    SUSPENSION_FX_MODEL = PlaneFX.imageModel(
      "suspension_fx", Power.class,
      SFX_DIR+"suspension.png", 0.5f, 360, 0, true, true
    ),
    
    TELEKINESIS_FX_MODEL = PlaneFX.imageModel(
      "telekinesis_fx", Power.class,
      SFX_DIR+"telekinesis.png", 0.5f, 360, 0, true, true
    ),
    
    VOICE_OF_COMMAND_FX_MODEL = PlaneFX.imageModel(
      "voice_command_fx", Power.class,
      SFX_DIR+"voice_of_command.png", 1, 360, 1, true, true
    );
  
  
  final public static Trait
    KINESTHESIA_EFFECT = new Condition(
      BC, "Kinesthesia Effect", Table.make(
        MOTOR, 10, HAND_TO_HAND, 10, MARKSMANSHIP, 10, ATHLETICS, 10
      ),
      "Kinesthesia", "Kinesthesia", "Kinesthesia", null
    ) {
      public void affect(Actor a) {
        super.affect(a);
        a.world().ephemera.updateGhost(a, 1, Power.KINESTHESIA_FX_MODEL, 1);
      }
    },
    SUSPENSION_EFFECT = new Condition(
      BC, "Suspension Effect", Table.make(),
      "Suspension", "Suspension", "Suspension", null
    ) {
      public void affect(Actor a) {
        super.affect(a);
        if (a.traits.usedLevel(this) <= 0) {
          a.health.setState(ActorHealth.STATE_RESTING);
        }
        else {
          a.health.liftInjury(0.1f);
          a.world().ephemera.updateGhost(a, 1, Power.SUSPENSION_FX_MODEL, 1);
        }
      }
    },
    SPICE_VISION_EFFECT = new Condition(
      BC, "Spice Vision Effect", Table.make(
        IMMUNE, 10, COGNITION, 5, PERCEPT, 5, NERVE, 5
      ),
      "Spice Vision", "Spice Vision", "Spice Vision", null
    ) {
      public void affect(Actor a) {
        super.affect(a);
        if (a.traits.traitLevel(Condition.SPICE_ADDICTION) <= 0) {
          a.traits.incLevel(Condition.SPICE_ADDICTION, Rand.num() / 10f);
        }
      }
    };
  
    
  //  TODO:  Just have 1 to bring up menu options.  Or remove the Quickbar?
  //         Or merge it entirely with target-options?
  //  1 and 2 to save and load.
  
  
  //  TODO:  Move this into the actual technique.
  /*
  public static void applyTimeDilation(float gameSpeed, Scenario scenario) {
    final Actor caster = scenario.base().ruler();
    if (caster == null || GameSettings.psyFree) return;
    
    final float
      bonus = caster.traits.usedLevel(PROJECTION) / 10f,
      drain = 1f / ((1 + bonus) * PlayLoop.UPDATES_PER_SECOND * gameSpeed);
    caster.health.takeFatigue(drain);
    caster.skills.practiceAgainst(10, drain * 2, PROJECTION);
  }
  
  
  public static void applyResting(float gameSpeed, Scenario scenario) {
    final Actor caster = scenario.base().ruler();
    if (caster == null || GameSettings.psyFree) {
      PlayLoop.setGameSpeed(1);
      return;
    }
    //
    //  Restore psi points/fatigue/etc. and return to normal speed when done.
    caster.health.gainConcentration(2f / PlayLoop.UPDATES_PER_SECOND);
    PlayLoop.setNoInput(true);
    if (caster.health.concentration() >= caster.health.maxConcentration()) {
      PlayLoop.setGameSpeed(1);
      PlayLoop.setNoInput(false);
    }
  }
  
  
  public static void applyWalkPath(Scenario scenario) {
    final Actor caster = scenario.base().ruler();
    if (caster == null || GameSettings.psyFree) return;
    
    final float
      bonus = caster.traits.usedLevel(PREMONITION) / 10,
      lastSave = Scenario.current().timeSinceLastSave(),
      boost = (lastSave / 1000f) * (0.5f + bonus);
    caster.health.gainConcentration(boost);
    caster.skills.practiceAgainst(10, boost / 2, PREMONITION);
  }
  
  
  public static void applyDenyVision(Scenario scenario) {
    final Actor caster = scenario.base().ruler();
    if (caster == null || GameSettings.psyFree) return;
    
    final float
      bonus = caster.traits.usedLevel(PREMONITION) / 10,
      cost = 10f / (0.5f + bonus);
    caster.health.takeFatigue(cost);
    caster.skills.practiceAgainst(10, cost, PREMONITION);
  }
  //*/
  
  
  //  TODO:  You can probably get rid of the Foresight/Remembrance/
  //  Time Dilation options now.
  
  
  final public static Power
    
    FORESIGHT = new Power(
      "Foresight", "power_foresight", "power_foresight.gif",
      "Accept your vision of events and allow them to be fulfilled.\n(Saves "+
      "current game.)", PREMONITION, 5
    ) {
    /*
      final String
        OPTION_QUIT = "Save and Exit",
        OPTION_REST = "Save and Rest",
        OPTION_MARK = "Save and Remember";
    
      public String[] options() {
        return new String[] {
          OPTION_QUIT, OPTION_REST, OPTION_MARK
        };
      }
      //*/
      
      public boolean appliesTo(Actor caster, Target selected) {
        return false;
      }
      
      
      public int costFor(Actor caster, Target selected) {
        return 0;  //  TODO:  FIX UP
      }
      
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, Target hovered
      ) {
        //  TODO:  ADD MORE SPECIAL FX HERE
        Scenario.current().scheduleSave();
        /*
        if (option.equals(OPTION_QUIT)) {
          Scenario.current().saveProgress(true, true);
        }
        if (option.equals(OPTION_REST)) {
          Scenario.current().saveProgress(true, false);
          PlayLoop.setGameSpeed(5.0f);
        }
        if (option.equals(OPTION_MARK)) {
          Scenario.current().saveProgress(false, false);
        }
        //*/
        return true;
      }
    },
    
    REMEMBRANCE = new Power(
      "Remembrance", "power_remembrance", "power_remembrance.gif",
      "Aborts your precognitive vision and lets you choose a different path."+
      "\n(Loads a previous game.)", PREMONITION, 5
    ) {
      /*
      public String[] options() {
        return Scenario.current().loadOptions();
      }
      //*/
      
      public boolean appliesTo(Actor caster, Target selected) {
        return false;
      }
      
      
      public int costFor(Actor caster, Target selected) {
        return 0;  //  TODO:  FIX UP
      }
      
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, Target hovered
      ) {
        //  Clean up arguments to allow garbage collection, and go back in the
        //  timeline-
        caster = null;
        selected = null;
        Scenario.current().scheduleReload();
        return true;
      }
    },
    
    TIME_DILATION = new Power(
      "Time Dilation", "power_time_dilation", "power_time_dilation.gif",
      "Insulates your experience from temporal passage.\n(Reduces game speed.)",
      PROJECTION, 10
    ) {
      final String SPEED_SETTINGS[] = {
        "66% speed",
        "50% speed",
        "33% speed"
      };
      final float SPEED_VALS[] = { 0.66f, 0.5f, 0.33f };
      
      
      public String[] options() {
        if (PlayLoop.gameSpeed() < 1) return null;
        return SPEED_SETTINGS;
      }
      
      
      public boolean appliesTo(Actor caster, Target selected) {
        return false;
      }
      
      
      public int costFor(Actor caster, Target selected) {
        return 0;  //  TODO:  FIX UP
      }
      
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, Target hovered
      ) {
        if (option == null) {
          PlayLoop.setGameSpeed(1);
          return true;
        }
        
        float speedVal = SPEED_VALS[Visit.indexOf(option, SPEED_SETTINGS)];
        if (caster == null || GameSettings.psyFree) {
          PlayLoop.setGameSpeed(speedVal);
          return true;
        }
        PlayLoop.setGameSpeed(speedVal);
        return true;
      }
    },
    
    REMOTE_VIEWING = new Power(
      "Remote Viewing", "power_remote_viewing", "power_remote_viewing.png",
      "Lifts fog around target terrain.",
      PROJECTION, 10
    ) {
      public boolean appliesTo(Actor caster, Target selected) {
        return selected instanceof Tile;
      }
      
      public int costFor(Actor caster, Target selected) {
        float dist = Spacing.distance(selected, caster) / Stage.ZONE_SIZE;
        float cost = 5 * Nums.sqrt(dist);
        return (int) cost;
      }
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, Target hovered
      ) {
        final Tile tile = (Tile) selected;
        float bonus = 0, cost = 0;
        if (caster != null && ! GameSettings.psyFree) {
          bonus += caster.traits.usedLevel(PROJECTION) / 5;
          cost = costFor(caster, selected);
          caster.health.takeFatigue(cost);
          caster.skills.practiceAgainst(10, cost, PROJECTION);
        }
        
        final float radius = (9 + (bonus * bonus)) / 2f;
        final Base played = Scenario.current().base();
        played.intelMap.liftFogAround(tile.x, tile.y, radius);
        
        final float SS = radius / 1.5f;
        final Vec3D p = tile.position(null);
        p.z += 0.5f;
        
        final Sprite swirlFX = REMOTE_VIEWING_FX_MODEL.makeSprite();
        swirlFX.scale = SS / 2;
        swirlFX.position.setTo(p);
        tile.world.ephemera.addGhost(null, SS, swirlFX, 1.0f, 1);
        
        final Sprite burstFX = LIGHT_BURST_MODEL.makeSprite();
        burstFX.scale = SS / 2;
        burstFX.position.setTo(p);
        tile.world.ephemera.addGhost(null, SS, burstFX, 2.0f, 1);
        
        return true;
      }
    },
    
    TELEKINESIS = new Power(
      "Telekinesis", "power_telekinesis", "power_telekinesis.png",
      "Hurls the target in an indicated direction.",
      TRANSDUCTION, 10
    ) {
      public boolean appliesTo(Actor caster, Target selected) {
        return
          selected instanceof Mobile && caster != null &&
          selected.base() != caster.base();
      }
      
      public int costFor(Actor caster, Target selected) {
        return 4;
      }
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, Target hovered
      ) {
        if (BaseUI.current().mouseClicked()) {
          pushSubject(caster, (Mobile) selected, hovered);
          return true;
        }
        BaseUI.setPopupMessage("Select throw target");
        return false;
      }
      
      private void pushSubject(Actor caster, Mobile pushed, Target toward) {
        if (pushed == null || toward == null) return;
        final boolean report = applyVerbose;
        
        float maxDist = 1;
        if (! GameSettings.psyFree) {
          maxDist = 1 + (caster.traits.usedLevel(TRANSDUCTION) / 10f);
          final float drain = costFor(caster, pushed);
          caster.health.takeFatigue(drain);
          caster.skills.practiceAgainst(10, drain, TRANSDUCTION);
        }
        maxDist *= 10f;
        maxDist /= 4 * pushed.radius();
        
        final Vec3D thrownTo = new Vec3D();
        toward.position(thrownTo).sub(pushed.position(null));
        if (thrownTo.length() > maxDist) thrownTo.normalise().scale(maxDist);
        final float distance = thrownTo.length();
        thrownTo.add(pushed.position(null));
        
        if (report) {
          I.say("\nUsing TK to throw subject: "+pushed+" toward "+toward);
          I.say("  Toward is at:      "+toward.position(null));
          I.say("  Maximum distance:  "+maxDist);
          I.say("  Throwing to:       "+thrownTo+" (distance "+distance+")");
        }
        
        if (pushed instanceof Actor) {
          final Actor hurt = (Actor) pushed;
          final Target destination = hurt.pathing.target();
          float oldDist = 0, newDist = 0;
          
          if (destination != null) {
            oldDist = Spacing.distance(hurt, destination);
            newDist = destination.position(null).distance(thrownTo);
          }
          float injury = distance / 5, dislike = 0;
          dislike += (newDist - oldDist) / Stage.ZONE_SIZE;
          dislike += injury / hurt.health.maxHealth();
          
          hurt.enterStateKO(Action.FALL);
          hurt.health.takeInjury(injury, false);
          hurt.relations.incRelation(caster, 0 - dislike, 0.2f, 0);
          
          if (report) {
            I.say("  Actor destination: "+destination);
            I.say("  Old/new distance:  "+oldDist+"/"+newDist);
            I.say("  Injury dealt:      "+injury);
            I.say("  Dislike:           "+dislike);
          }
          if (dislike > 0 && hurt.base() == caster.base()) {
            hurt.chat.addPhrase("Whoa!  Stop doing that!", TalkFX.FROM_LEFT);
          }
        }
        
        pushed.setHeading(thrownTo, -1, false, pushed.world());
        pushed.world().ephemera.updateGhost(
          pushed, 1, TELEKINESIS_FX_MODEL, 0.2f
        );
      }
    },
    
    FORCEFIELD = new Power(
      "Forcefield", "power_forcefield", "power_forcefield.png",
      "Temporarily raises shields on target.",
      TRANSDUCTION, 10
    ) {
      public boolean appliesTo(Actor caster, Target selected) {
        return selected instanceof Actor;
      }
      
      public int costFor(Actor caster, Target selected) {
        return 5;
      }
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, Target hovered
      ) {
        final Actor subject = (Actor) selected;
        
        if (caster == null || GameSettings.psyFree) {
          subject.gear.boostShields(5, false);
          return true;
        }
        
        final float
          bonus = caster.traits.usedLevel(TRANSDUCTION) / 2,
          cost  = costFor(caster, selected);
        subject.gear.boostShields(5 + bonus, false);
        caster.health.takeFatigue(cost);
        caster.skills.practiceAgainst(10, cost, TRANSDUCTION);
        return true;
      }
    },
    
    SUSPENSION = new Power(
      "Suspension", "power_suspension", "power_suspension.png",
      "Puts subject into suspended animation.  Can be used to incapacitate "+
      "foes or delay bleedout.",
      METABOLISM, 5
    ) {
      public boolean appliesTo(Actor caster, Target selected) {
        return selected instanceof Actor;
      }
      
      public int costFor(Actor caster, Target selected) {
        return 5;
      }
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, Target hovered
      ) {
        final Actor subject = (Actor) selected;
        
        float bonus = 1;
        if (caster != null && ! GameSettings.psyFree) {
          final float cost = costFor(caster, selected);
          caster.health.takeFatigue(cost);
          bonus += caster.traits.usedLevel(METABOLISM) / 2;
          caster.skills.practiceAgainst(10, cost, METABOLISM);
        }
        
        if (subject.health.conscious()) {
          if (subject.skills.test(IMMUNE, 10 + bonus, 10, null)) bonus = 0;
        }
        if (bonus > 0) {
          subject.health.setState(ActorHealth.STATE_SUSPEND);
        }
        else bonus = 0.1f;
        subject.traits.incLevel(SUSPENSION_EFFECT, bonus * 2 / 10f);
        return true;
      }
    },
    
    KINESTHESIA = new Power(
      "Kinesthesia", "power_kinesthesia", "power_kinesthesia.png",
      "Boosts most combat-related and acrobatic skills.",
      SYNESTHESIA, 5
    ) {
      public boolean appliesTo(Actor caster, Target selected) {
        return selected instanceof Actor;
      }
      
      public int costFor(Actor caster, Target selected) {
        return 3;
      }
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, Target hovered
      ) {
        final Actor subject = (Actor) selected;
        float bonus = 5f;
        
        if (caster != null && ! GameSettings.psyFree) {
          bonus += caster.traits.usedLevel(SYNESTHESIA) / 2;
          final float cost = costFor(caster, selected);
          caster.health.takeFatigue(cost);
          caster.skills.practiceAgainst(10, cost, SYNESTHESIA);
        }
        
        subject.traits.incLevel(KINESTHESIA_EFFECT, bonus * 2 / 10f);
        return true;
      }
    },
    
    VOICE_OF_COMMAND = new Power(
      "Voice of Command", "power_VOC", "power_voice_of_command.png",
      "Psychically urges a subject to fight, flee, help or converse.",
      SUGGESTION, 5
    ) {
      final String options[] = new String[] {
        "Flee",
        "Fight",
        "Treat",
        "Talk"
      };
      
      public String[] options() { return options; }
      
      public boolean appliesTo(Actor caster, Target selected) {
        return selected instanceof Actor;
      }
      
      public int costFor(Actor caster, Target selected) {
        return 2;
      }
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, Target hovered
      ) {
        BaseUI.setPopupMessage(
          "Select a focus for interaction."
        );
        if (BaseUI.current().mouseClicked()) {
          return applyEffect(caster, option, (Actor) selected, hovered);
        }
        return false;
      }
      
      private boolean applyEffect(
        Actor caster, String option, Actor affects, Target selected
      ) {
        final boolean report = applyVerbose;
        
        Plan command = null;
        if (option == options[0] && selected instanceof Tile ) {
          //  TODO:  Allow retreat to venues as well.
          command = new Retreat(affects, (Tile) selected);
        }
        if (option == options[1] && selected instanceof Actor) {
          command = new Combat(affects, (Actor) selected);
        }
        if (option == options[2] && selected instanceof Actor) {
          command = new FirstAid(affects, (Actor) selected);
        }
        if (option == options[3] && selected instanceof Actor) {
          command = Dialogue.dialogueFor(affects, (Actor) selected);
        }
        if (command == null) return true;
        
        final boolean cast = caster != null && ! GameSettings.psyFree;
        final float truePriority = command.priorityFor(affects);
        final Behaviour root = affects.mind.rootBehaviour();
        final float
          oldPriority = root == null ? 0 : root.priorityFor(affects),
          affinity = (truePriority - oldPriority) / Plan.PARAMOUNT;
        
        float priorityMod = 0;
        if (cast) {
          final float
            cost      = costFor(caster, selected),
            magnitude = Nums.abs(affinity);
          priorityMod += caster.traits.usedLevel(SUGGESTION) / 2f;
          priorityMod += (Rand.num() - 0.5f) * Plan.PARAMOUNT * 2;
          caster.health.takeFatigue(cost);
          caster.skills.practiceAgainst(10, cost, SYNESTHESIA);
          affects.relations.incRelation(caster, affinity, magnitude, -0.1f);
        }
        else priorityMod = Plan.ROUTINE;
        priorityMod *= (0.5f + Rand.avgNums(2));
        command.clearMotives();
        command.addMotives(Plan.MOTIVE_EMERGENCY, priorityMod + oldPriority);
        final float activePriority = command.priorityFor(affects);
        
        if (report) {
          I.say("\nApplying suggestion to "+affects);
          I.say("  Current task:    "+root          );
          I.say("  Task priority:   "+oldPriority   );
          I.say("  Command is:      "+command       );
          I.say("  True priority:   "+truePriority  );
          I.say("  Priority bonus:  "+priorityMod   );
          I.say("  Active priority: "+activePriority);
          I.say("  Affinity rating: "+affinity      );
        }
        
        if (activePriority > 0 && ! affects.mind.mustIgnore(command)) {
          affects.chat.addPhrase("Yeah, maybe I'd better...", TalkFX.FROM_LEFT);
          affects.mind.assignBehaviour(command);
          if (report) I.say("Compulsion accepted!");
        }
        else {
          affects.chat.addPhrase("Get out of my head!!!", TalkFX.FROM_LEFT);
          if (cast) affects.relations.incRelation(caster, -0.5f, 0.1f, -0.9f);
          if (report) I.say("Compulsion resisted!");
        }
        
        if (affects.health.goodHealth()) {
          //  TODO:  INCLUDE NEGATIVE MORALE/RELATION FX!
        }
        
        applyFX(affects , 1.0f);
        applyFX(selected, 0.5f);
        return true;
      }
      
      
      private void applyFX(Target selected, float scale) {
        final Sprite selectFX = VOICE_OF_COMMAND_FX_MODEL.makeSprite();
        selectFX.scale = scale * selected.radius() * 2;
        selected.position(selectFX.position);
        selected.world().ephemera.addGhost(
          selected, 1, selectFX, 0.5f, 1
        );
      }
    }
 ;
 
 
 //
 //  These are available for initial selection for free.
 final public static Power BASIC_POWERS[] = {
   REMOTE_VIEWING, VOICE_OF_COMMAND, SUSPENSION, TELEKINESIS
 };
}

















