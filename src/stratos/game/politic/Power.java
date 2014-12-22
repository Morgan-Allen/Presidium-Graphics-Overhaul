


package stratos.game.politic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.politic.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.start.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Make this available to actors, once the feedback mechanism is in
//  place.  (This will most likely require individual instancing.)
//  TODO:  Merge with Techniques for the purpose.


public class Power implements Qualities {
  
  
  final static String
    IMG_DIR = "media/GUI/Powers/",
    SFX_DIR = "media/SFX/";
  
  final public static int
    NONE        = 0,
    PLAYER_ONLY = 1,
    MAINTAINED  = 2;
  
  
  final public String name, helpInfo;
  final public ImageAsset buttonImage;
  final int properties;
  
  
  Power(String name, int properties, String imgFile, String helpInfo) {
    this.name = name;
    this.helpInfo = helpInfo;
    this.buttonImage = ImageAsset.fromImage(Power.class, IMG_DIR+imgFile);
    this.properties = properties;
  }
  
  
  public boolean finishedWith(
    Actor caster, String option,
    Target selected, boolean clicked
  ) {
    return false;
  }
  
  
  public String[] options() {
    return null;
  }
  
  
  final public static PlaneFX.Model
    
    REMOTE_VIEWING_FX_MODEL = new PlaneFX.Model(
      "RV_swirl_fx", Power.class,
      SFX_DIR+"remote_viewing.png", 0.5f, -360, 1, false, true
    ),
    
    LIGHT_BURST_MODEL = new PlaneFX.Model(
      "RV_burst_fx", Power.class,
      SFX_DIR+"light_burst.png", 0.5f, 0, 1, true, true
    ),
    
    KINESTHESIA_FX_MODEL = new PlaneFX.Model(
      "kinesthesia_fx", Power.class,
      SFX_DIR+"kinesthesia.png", 0.5f, 360, 0, true, true
    ),
    
    SUSPENSION_FX_MODEL = new PlaneFX.Model(
      "suspension_fx", Power.class,
      SFX_DIR+"suspension.png", 0.5f, 360, 0, true, true
    ),
    
    TELEKINESIS_FX_MODEL = new PlaneFX.Model(
      "telekinesis_fx", Power.class,
      SFX_DIR+"telekinesis.png", 0.5f, 360, 0, true, true
    ),
    
    VOICE_OF_COMMAND_FX_MODEL = new PlaneFX.Model(
      "voice_command_fx", Power.class,
      SFX_DIR+"voice_of_command.png", 1, 360, 1, true, true
    );
  
  
  
  //  TODO:  Move this into the actual technique.
  
  public static void applyTimeDilation(float gameSpeed, Scenario scenario) {
    final Actor caster = scenario.base().ruler();
    if (caster == null || GameSettings.psyFree) return;
    
    final float
      bonus = caster.traits.usedLevel(PROJECTION) / 10f,
      drain = 1f / ((1 + bonus) * PlayLoop.UPDATES_PER_SECOND * gameSpeed);
    caster.health.takeConcentration(drain);
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
    caster.health.takeConcentration(cost);
    caster.skills.practiceAgainst(10, cost, PREMONITION);
  }
  
  
  final public static Power
    
    FORESIGHT = new Power(
      "Foresight", PLAYER_ONLY, "power_foresight.gif",
      "Accept your vision of events and allow them to be fulfilled.\n(Saves "+
      "current game.)"
    ) {
      final String
        OPTION_QUIT = "Save and Exit",
        OPTION_REST = "Save and Rest",
        OPTION_MARK = "Save and Remember";
    
      public String[] options() {
        return new String[] {
          OPTION_QUIT, OPTION_REST, OPTION_MARK
        };
      }
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
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
        return true;
      }
    },
    
    REMEMBRANCE = new Power(
      "Remembrance", PLAYER_ONLY, "power_remembrance.gif",
      "Aborts your precognitive vision and lets you choose a different path."+
      "\n(Loads a previous game.)"
    ) {
      public String[] options() {
        return Scenario.current().loadOptions();
      }
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        //  Clean up arguments to allow garbage collection, and go back in the
        //  timeline-
        caster = null;
        selected = null;
        Scenario.current().revertTo(option);
        return true;
      }
    },
    
    TIME_DILATION = new Power(
      "Time Dilation", PLAYER_ONLY | MAINTAINED, "power_time_dilation.gif",
      "Insulates your experience from temporal passage.\n(Reduces game speed.)"
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
      
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
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
      "Remote Viewing", PLAYER_ONLY, "power_remote_viewing.gif",
      "Grants you an extra-sensory perception of distant places or persons."+
      "\n(Lifts fog around target terrain.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Tile)) return false;
        final Tile tile = (Tile) selected;
        
        float bonus = 0;
        if (caster != null && ! GameSettings.psyFree) {
          bonus += caster.traits.usedLevel(PROJECTION) / 5;

          float dist = (float) Nums.sqrt(Spacing.distance(tile, caster));
          float cost = 10 * (1 + (dist / Stage.SECTOR_SIZE));
          caster.health.takeConcentration(cost);
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
        tile.world.ephemera.addGhost(null, SS, swirlFX, 1.0f);
        
        final Sprite burstFX = LIGHT_BURST_MODEL.makeSprite();
        burstFX.scale = SS / 2;
        burstFX.position.setTo(p);
        tile.world.ephemera.addGhost(null, SS, burstFX, 2.0f);
        
        return true;
      }
    },
    
    TELEKINESIS = new Power(
      "Telekinesis", NONE, "power_telekinesis.gif",
      "Imparts spatial moment to a chosen material object.\n(Hurls or carries "+
      "the target in an indicated direction.)"
    ) {
      private Mobile grabbed = null;
      
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (grabbed == null) {
          if (clicked && selected instanceof Mobile) {
            grabbed = (Mobile) selected;
          }
          return false;
        }
        else if (clicked) {
          grabbed = null;
          return true;
        }
        else return ! pushGrabbed(caster, selected);
      }
      
      
      private boolean pushGrabbed(Actor caster, Target toward) {
        if (grabbed == null) return false;
        
        if (grabbed instanceof Actor) {
          ((Actor) grabbed).enterStateKO(Action.FALL);
        }
        
        float maxDist = 1;
        if (caster != null && ! GameSettings.psyFree) {
          maxDist = 1 + (caster.traits.usedLevel(TRANSDUCTION) / 10f);
          final float drain = 4f / PlayLoop.FRAMES_PER_SECOND;
          caster.health.takeConcentration(drain);
          caster.skills.practiceAgainst(10, drain, TRANSDUCTION);
        }
        maxDist *= 10f / PlayLoop.FRAMES_PER_SECOND;
        maxDist /= 4 * grabbed.radius();
        
        final Vec3D pushed = new Vec3D();
        toward.position(pushed).sub(grabbed.position(null));
        if (pushed.length() > maxDist) pushed.normalise().scale(maxDist);
        pushed.add(grabbed.position(null));
        
        grabbed.setHeading(pushed, -1, false, grabbed.world());
        grabbed.world().ephemera.updateGhost(
          grabbed, 1, TELEKINESIS_FX_MODEL, 0.2f
        );
        return true;
      }
    },
    
    FORCEFIELD = new Power(
      "Forcefield", MAINTAINED, "power_forcefield.gif",
      "Encloses the subject in a selectively permeable suspension barrier.\n"+
      "(Temporarily raises shields on target.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Actor)) return false;
        final Actor subject = (Actor) selected;
        
        if (caster == null || GameSettings.psyFree) {
          subject.gear.boostShields(5, false);
          return true;
        }
        ///I.say("BOOSTING SHIELDS FOR "+selected);
        
        final float
          bonus = caster.traits.usedLevel(TRANSDUCTION) / 2,
          cost = 5;
        subject.gear.boostShields(5 + bonus, false);
        caster.health.takeConcentration(cost);
        caster.skills.practiceAgainst(10, cost, TRANSDUCTION);
        return true;
      }
    },
    
    SUSPENSION = new Power(
      "Suspension", MAINTAINED, "power_suspension.gif",
      "Suspends metabolic function in subject.\n(Can be used to arrest "+
      "injury or incapacitate foes.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Actor)) return false;
        final Actor subject = (Actor) selected;
        
        float bonus = 1;
        if (caster != null && ! GameSettings.psyFree) {
          final float cost = 5;
          caster.health.takeConcentration(cost);
          bonus += caster.traits.usedLevel(METABOLISM) / 2;
          caster.skills.practiceAgainst(10, cost, METABOLISM);
        }
        
        if (subject.health.conscious()) {
          if (subject.skills.test(IMMUNE, 10 + bonus, 10)) bonus = 0;
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
      "Kinesthesia", MAINTAINED, "power_kinesthesia.gif",
      "Augments hand-eye coordination and reflex response.\n(Boosts most "+
      "combat and acrobatic skills.)"
    ) {
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Actor)) return false;
        final Actor subject = (Actor) selected;
        float bonus = 5f;
        
        if (caster != null && ! GameSettings.psyFree) {
          bonus += caster.traits.usedLevel(SYNESTHESIA) / 2;
          final float cost = 2.5f;
          caster.health.takeConcentration(cost);
          caster.skills.practiceAgainst(10, cost, SYNESTHESIA);
        }
        subject.traits.incLevel(KINESTHESIA_EFFECT, bonus * 2 / 10f);
        return true;
      }
    },
    
    VOICE_OF_COMMAND = new Power(
      "Voice of Command", PLAYER_ONLY, "power_voice_of_command.gif",
      "Employs mnemonic triggering to incite specific behavioural response."+
      "\n(Compels subject to fight, flee, help or speak.)"
    ) {
      
      final String options[] = new String[] {
        "Flee",
        "Fight",
        "Treat",
        "Talk"
      };
      
      Actor affects = null;
      
      public String[] options() { return options; }
      
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (affects == null) {
          if (clicked && selected instanceof Actor) {
            affects = (Actor) selected;
          }
        }
        else {
          if (clicked) return applyEffect(caster, option, selected);
        }
        return false;
      }
      
      
      private boolean applyEffect(
        Actor caster, String option, Target selected
      ) {
        Plan command = null;
        if (option == options[0] && selected instanceof Tile) {
          command = new Retreat(affects, (Tile) selected);
        }
        if (option == options[1] && selected instanceof Actor) {
          command = new Combat(affects, (Actor) selected);
        }
        if (option == options[2] && selected instanceof Actor) {
          command = new FirstAid(affects, (Actor) selected);
        }
        if (option == options[3] && selected instanceof Actor) {
          command = new Dialogue(
            affects, (Actor) selected, Dialogue.TYPE_CONTACT
          );
        }
        if (command == null) return false;
        
        final float truePriority = command.priorityFor(affects);
        final Behaviour root = affects.mind.rootBehaviour();
        final float
          oldPriority = root == null ? 0 : root.priorityFor(affects),
          affinity = (truePriority - oldPriority) / Plan.PARAMOUNT;
        
        float priorityMod = Plan.ROUTINE;
        if (caster != null && ! GameSettings.psyFree) {
          final float cost = 5f;
          priorityMod += caster.traits.usedLevel(SUGGESTION) / 5f;
          caster.health.takeConcentration(cost);
          caster.skills.practiceAgainst(10, cost, SYNESTHESIA);
          affects.relations.incRelation(caster, affinity, 0.1f, 0);
        }
        else priorityMod = Plan.PARAMOUNT;
        
        command.setMotive(Plan.MOTIVE_EMERGENCY, priorityMod);
        
        if (! affects.mind.mustIgnore(command)) {
          DialogueUtils.utters(affects, "Yeah, maybe I'd better...", affinity);
          affects.mind.assignBehaviour(command);
        }
        else {
          DialogueUtils.utters(affects, "Get out of my head!!!", affinity);
          I.say("Compulsion refused! "+command);
          I.say("Priority was: "+command.priorityFor(affects));
        }
        
        affects.world().ephemera.addGhost(
          affects, 1, VOICE_OF_COMMAND_FX_MODEL.makeSprite(), 0.5f
        );
        final Sprite selectFX = VOICE_OF_COMMAND_FX_MODEL.makeSprite();
        selectFX.scale = selected.radius();
        selected.position(selectFX.position);
        affects.world().ephemera.addGhost(
          null, 1, selectFX, 0.5f
        );

        affects = null;
        return true;
      }
    },
    
    
    BASIC_POWERS[] = {
        FORESIGHT, REMEMBRANCE,
        TIME_DILATION, REMOTE_VIEWING,
        TELEKINESIS, FORCEFIELD,
        SUSPENSION,
        KINESTHESIA,
        VOICE_OF_COMMAND
    }
 ;
}




