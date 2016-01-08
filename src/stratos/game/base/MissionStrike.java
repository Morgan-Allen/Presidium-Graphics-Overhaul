/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.maps.*;
import stratos.game.verse.*;
import stratos.graphics.widgets.Text;
import stratos.user.BaseUI;
import stratos.user.notify.MessageTopic;
import stratos.util.*;




public class MissionStrike extends Mission {
  
  
  /**  Field definitions, constants and save/load methods-
    */
  private static boolean
    rateVerbose = BaseTactics.updatesVerbose,
    verbose     = false;
  
  
  private MissionStrike(Base base, Element subject) {
    super(
      base, subject, STRIKE_MODEL,
      "Striking at "+subject
    );
  }
  
  
  private MissionStrike(Base base, Sector subject) {
    super(
      base, subject, STRIKE_MODEL,
      "Striking at "+subject
    );
  }
  
  
  public MissionStrike(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Importance/suitability assessment-
    */
  public static MissionStrike strikeFor(Object target, Base base) {
    if (target instanceof Sector) {

      //  We only permit strikes against occupied enemy territories-
      final Sector sector = (Sector) target;
      final SectorBase b = base.world.offworld.baseForSector(sector);
      final Faction owns = b.faction();
      final Verse verse = base.world.offworld;
      
      if (owns == null || sector == base.world.localSector()) return null;
      if (Faction.relationValue(owns, base.faction(), verse) >= 0) return null;
      
      final MissionStrike m = new MissionStrike(base, sector);
      m.setJourney(Journey.configForMission(m, true));
      return m.journey() == null ? null : m;
    }
    if ((
      target instanceof Actor ||
      target instanceof Venue
    ) && ((Target) target).base() != base) {
      return new MissionStrike(base, (Element) target);
    }
    return null;
  }
  
  
  public float targetValue(Base base) {
    final boolean report = I.matchOrNull(
      base.title(), BaseTactics.verboseBase
    ) && rateVerbose && verbose;
    
    float targetValue = 0;
    if (subject instanceof Venue) {
      final Venue v = (Venue) subject;
      final Siting s = v.blueprint.siting();
      final int HP = v.structure.maxIntegrity();
      targetValue = s == null ? 1 : s.ratePointDemand(v.base(), v, false);
      targetValue += HP * 1f / Structure.DEFAULT_INTEGRITY;
      targetValue = Nums.clamp(targetValue / BaseSetup.MAX_PLACE_RATING, 0, 1);
    }
    if (subject instanceof Actor) {
      //  TODO:  GET A VALUE FOR THIS
    }
    if (subject instanceof Sector) {
      //  TODO:  GET A VALUE FOR THIS
    }
    
    if (report) {
      I.say("\nRating "+this+" for "+base);
      I.say("  Target value:   "+targetValue);
    }
    return targetValue;
  }
  
  
  public float rateCompetence(Actor actor) {
    if (subject instanceof Target) {
      return PlanUtils.combatWinChance(actor, (Target) subject, 1);
    }
    if (subject instanceof Sector) {
      return CombatUtils.powerLevel(actor) * 0.5f / CombatUtils.AVG_POWER;
    }
    return -1;
  }
  
  
  public float harmLevel() {
    if (objective() == Combat.OBJECT_SUBDUE ) return Plan.MILD_HARM;
    if (objective() == Combat.OBJECT_EITHER ) return Plan.REAL_HARM;
    if (objective() == Combat.OBJECT_DESTROY) return Plan.EXTREME_HARM;
    return 0;
  }
  
  
  public boolean allowsMissionType(int type) {
    if (isOffworld() && type == TYPE_PUBLIC) return false;
    else return super.allowsMissionType(type);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour createStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = nextStepFor(actor, false);
    if (cached != null) return cached;
    
    final Combat combat = new Combat(
      actor, (Element) subject, Combat.STYLE_EITHER, objective()
    );
    combat.addMotives(Plan.MOTIVE_MISSION, basePriority(actor));
    
    return cacheStepFor(actor, combat);
  }
  
  
  protected boolean shouldEnd() {
    if (CombatUtils.isDowned((Element) subject, objective())) return true;
    return false;
  }
  
  
  public boolean resolveMissionOffworld() {
    final Verse verse = base.world.offworld;
    final Sector s = verse.currentSector(subject);
    final SectorBase b = verse.baseForSector(s);
    
    float sumPower    = MissionUtils.partyPower(this);
    float targetPower = b.powerLevel(b.faction());
    final Batch <Actor> defenders = new Batch();
    
    for (Mobile m : b.allUnits()) {
      if (! (m instanceof Actor)) continue;
      final Actor unit = (Actor) m;
      if (unit.mind.mission() instanceof MissionSecurity) {
        targetPower += CombatUtils.powerLevel(unit) * 1.5f;
        defenders.add(unit);
      }
      else if (unit.base() == b.baseInWorld()) {
        targetPower += CombatUtils.powerLevel(unit);
        defenders.add(unit);
      }
    }
    
    //  TODO:  You could make this a little more nuanced based on some
    //  CombatUtils checks.
    
    float winChance = sumPower / (sumPower + targetPower);
    if (Rand.num() < winChance) {
      TOPIC_STRIKE_OKAY.dispatchMessage("Strike successful: "+s, s);
      b.setPowerLevel(b.powerLevel(b.faction()) - (2 * winChance * Rand.num()));
      b.setPopulation(b.population() - (winChance * Rand.num()));
    }
    else {
      TOPIC_STRIKE_FAIL.dispatchMessage("Strike failed: "+s, s);
      b.setPowerLevel(b.powerLevel(b.faction()) - (winChance * Rand.num()));
    }
    inflictDamage(applicants(), 1f - winChance);
    inflictDamage(defenders   , winChance     );
    return true;
  }
  
  
  private void inflictDamage(Series <Actor> side, float hurtChance) {
    for (Actor a : side) {
      final float maxHP = a.health.maxHealth();
      if (Rand.num() < hurtChance / 2f) a.health.takeInjury(maxHP * 2, true);
      else a.health.takeInjury(maxHP * hurtChance * Rand.num(), false);
    }
  }
  
  
  
  /**  Rendering and interface-
    */
  final static MessageTopic TOPIC_STRIKE_OKAY = new MessageTopic(
    "topic_strike_okay", true, Sector.class
  ) {
    protected void configMessage(BaseUI UI, Text d, Object... args) {
      d.appendAll("Our strike against ", args[0], " was successful!");
      d.append("Their defences crumpled against our onslaught.");
    }
  };
  
  final static MessageTopic TOPIC_STRIKE_FAIL = new MessageTopic(
    "topic_strike_fail", true, Sector.class
  ) {
    protected void configMessage(BaseUI UI, Text d, Object... args) {
      d.appendAll("Our strike against ", args[0], " was repelled-");
      d.append(" the defending forces proved more tenacious than hoped.");
    }
  };
  
  
  public String[] objectiveDescriptions() {
    return Combat.OBJECT_NAMES;
  }
  
  
  public void describeMission(Description d) {
    d.append("Strike Mission", this);
    d.append(" against ");
    d.append(subject);
  }
}








