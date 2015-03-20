/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.Placement;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class Bastion extends Venue {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    Bastion.class, "media/Buildings/military/bastion.png", 7, 4
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    Bastion.class, "media/GUI/Buttons/bastion_button.gif"
  );
  
  final static int
    EXCLUDE_RADIUS = 2,
    CLAIM_RADIUS   = Stage.SECTOR_SIZE / 2;
  
  final static VenueProfile PROFILE = new VenueProfile(
    Bastion.class, "bastion", "Bastion",
    7, 4, false, NO_REQUIREMENTS
  );
  private Box2D excludes, claims;
  
  
  public Bastion(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      650, 15, 1000,
      Structure.BIG_MAX_UPGRADES, Structure.TYPE_FIXTURE
    );
    staff.setShiftType(SHIFTS_BY_HOURS);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public Bastion(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Placement and claims-assertion methods:
    */
  public int owningTier() {
    return TIER_UNIQUE;
  }
  
  
  protected Box2D areaClaimed() {
    if (claims == null || ! inWorld()) {
      claims = new Box2D(footprint()).expandBy(CLAIM_RADIUS);
    }
    return claims;
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    if (excludes == null || ! inWorld()) {
      excludes = new Box2D(footprint()).expandBy(EXCLUDE_RADIUS);
    }
    if (other.footprint().overlaps(excludes)) {
      return true;
    }
    if (other.base() == base()) return false;
    else return super.preventsClaimBy(other);
  }
  
  
  public void setPosition(int x, int y, Stage world) {
    super.setPosition(x, y, world);
    this.facing = FACING_EAST;
    
    //  TODO:  SORT THIS OUT IN A CLEANER WAY
    final Tile o = origin();
    final int off[] = Placement.entranceCoords(size, size, facing);
    Tile e = world.tileAt(o.x + off[0], o.y + off[1]);
    this.entrance = e;
  }
  
  
  public float ratePlacing(Target point, boolean exact) {
    
    //  TODO:  This could probably be sophisticated a bit...
    
    final Stage world = point.world();
    float rating = 5;
    if (inWorld()) return rating;
    
    //  TODO:  Also minerals & insolation, etc?
    rating *= 1 + world.terrain().fertilitySample(world.tileAt(point));
    
    final Target nearest = world.presences.nearestMatch(Venue.class, point, -1);
    if (nearest == null) return rating;
    final int SS = Stage.SECTOR_SIZE;
    return rating * (SS + Spacing.distance(point, nearest)) / SS;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    if (super.allowsEntry(m)) return true;
    if (Summons.summonedTo(m) == this) return true;
    return false;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    LOGISTIC_SUPPORT = new Upgrade(
      "Logistic Support",
      "Provides more openings for your Reservists and Auditors, thereby "+
      "aiding construction efforts and revenue flow.",
      200,
      Upgrade.THREE_LEVELS, null, 1,
      null, Bastion.class, ALL_UPGRADES
    ),
    SECURITY_MEASURES = new Upgrade(
      "Security Measures",
      "Increases patrols of Veterans in and around your settlement and "+
      "augments your Bastion's output of power and life support.",
      300,
      Upgrade.THREE_LEVELS, null, 1,
      null, Bastion.class, ALL_UPGRADES
    ),
    NOBLE_QUARTERS = new Upgrade(
      "Noble Quarters",
      "Increases the space available to your family, advisors, honour guard "+
      "and honoured guests.",
      400,
      Upgrade.THREE_LEVELS, null, 1,
      null, Bastion.class, ALL_UPGRADES
    ),
    GUEST_QUARTERS = new Upgrade(
      "Guest Quarters",
      "Makes more space for prisoners and hostages, and creates openings for "+
      "Stewards in your employ.",
      250,
      Upgrade.THREE_LEVELS, null, 1,
      null, Bastion.class, ALL_UPGRADES
    ),
    BLAST_SHIELDS = new Upgrade(
      "Blast Shields",
      "Increases the structural integrity of the Bastion, particularly vital "+
      "in the event of atomic attack.",
      450,
      Upgrade.THREE_LEVELS, null, 1,
      null, Bastion.class, ALL_UPGRADES
    ),
    SEAT_OF_POWER = new Upgrade(
      "Seat of Power",
      "Augments the strength and range of your psyonic powers and capacity "+
      "to function without sleep or rest.",
      500,
      Upgrade.THREE_LEVELS, null, 1,
      null, Bastion.class, ALL_UPGRADES
    );
  
  final static Condition SEAT_OF_POWER_EFFECT = new Condition(
    "seat_of_power_effect", true, "Seat of Power"
  ) {
    public void affect(Actor a) {
      if ((a.aboard() instanceof Bastion) && (a == a.base().ruler())) {
        final Bastion b = (Bastion) a.aboard();
        final Actor ruler = a;
        final float bonus = (b.structure.upgradeLevel(SEAT_OF_POWER) + 2) / 5f;
        
        float psiGain = ruler.health.maxConcentration();
        psiGain *= bonus / ActorHealth.CONCENTRATE_REGEN_TIME;
        ruler.health.gainConcentration(psiGain);
        
        float hitGain = ruler.health.maxHealth();
        hitGain *= bonus / Stage.STANDARD_DAY_LENGTH;
        ruler.health.liftFatigue(hitGain * 2);
        ruler.health.liftInjury (hitGain * 1);
        
        Resting.dineFrom(ruler, b);
      }
      else {
        a.traits.setLevel(SEAT_OF_POWER_EFFECT, 0);
      }
    }
  };

  
  
  public int numOpenings(Background b) {
    final int nO = super.numOpenings(b);
    if (b == Backgrounds.TROOPER) {
      return nO + 2 + structure.upgradeLevel(SECURITY_MEASURES);
    }
    if (b == Backgrounds.TECHNICIAN) {
      return nO + 2 + structure.upgradeLevel(LOGISTIC_SUPPORT);
    }
    if (b == Backgrounds.AUDITOR) {
      return nO + ((1 + structure.upgradeLevel(LOGISTIC_SUPPORT)) / 2);
    }
    if (b == Backgrounds.STEWARD) {
      return nO + ((1 + structure.upgradeLevel(GUEST_QUARTERS)) / 2);
    }
    //
    //  TODO:  Return the amount of space open to hostages and guests as well.
    return 0;
  }
  
  
  public float crowdRating(Actor actor, Background background) {
    if (background == Backgrounds.AS_RESIDENT) {
      if (! staff.doesBelong(actor)) return 1;
      if (staff.isWorker(actor)) return 0;
      
      final int maxPop = 6 + (structure.upgradeLevel(NOBLE_QUARTERS) * 2);
      return staff.residents().size() * 1f / maxPop;
    }
    else return super.crowdRating(actor, background);
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! structure.intact()) return null;
    
    //  TODO:  Apply to all advisors!
    if (actor == base().ruler()) {
      return Supervision.stayForVIP(this, actor);
    }
    final Background v = actor.vocation();
    if (v == Backgrounds.STEWARD || v == Backgrounds.FIRST_CONSORT) {
      return Supervision.stayForVIP(this, actor);
    }
    
    //
    //  Otherwise, return occupations for more regular staff-
    if (! staff.onShift(actor)) return null;
    if (v == Backgrounds.TROOPER || v == Backgrounds.WAR_MASTER) {
      return Patrolling.nextGuardPatrol(actor, this, Plan.ROUTINE);
    }
    if (v == Backgrounds.TECHNICIAN) {
      return Repairs.getNextRepairFor(actor, true);
    }
    if (v == Backgrounds.AUDITOR || v == Backgrounds.MINISTER_FOR_ACCOUNTS) {
      return Audit.nextOfficialAudit(actor);
    }
    return Supervision.oversight(this, actor);
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    //
    //  Look after the ruler and any other housebound guests-
    final Actor ruler = base.ruler();
    if (ruler != null && ruler.aboard() == this) {
      ruler.traits.setLevel(SEAT_OF_POWER_EFFECT, 1);
    }
    //
    //  Provide power and life support-
    final float condition = Nums.clamp(structure.repairLevel() + 0.5f, 0, 1);
    final int SB = structure.upgradeLevel(SECURITY_MEASURES);
    int powerLimit = 20 + (SB * 10), lifeSLimit = 10 + (SB * 5);
    powerLimit *= condition;
    lifeSLimit *= condition;
    structure.assignOutputs(
      Item.withAmount(POWER       , powerLimit),
      Item.withAmount(ATMO, lifeSLimit)
    );
    //
    //  Demand provisions-
    final int foodNeed = staff.residents().size() + 2;
    stocks.forceDemand(CARBS   , foodNeed * 1.5f, Tier.CONSUMER);
    stocks.forceDemand(PROTEIN , foodNeed * 1.0f, Tier.CONSUMER);
    stocks.forceDemand(GREENS  , foodNeed * 1.0f, Tier.CONSUMER);
    stocks.forceDemand(MEDICINE, foodNeed * 0.5f, Tier.CONSUMER);
    
    final int partNeed = structure.upgradeLevel(LOGISTIC_SUPPORT) + 2;
    stocks.forceDemand(PARTS   , partNeed * 1.0f, Tier.CONSUMER);
    stocks.forceDemand(PLASTICS, partNeed * 0.5f, Tier.CONSUMER);
    
    for (Traded type : Economy.ALL_MATERIALS) {
      if (stocks.demandTier(type) == Tier.CONSUMER) continue;
      stocks.forceDemand(type, 0, Tier.TRADER);
    }
    //
    //  Modify maximum integrity based on upgrades-
    final int BB = structure.upgradeLevel(BLAST_SHIELDS);
    structure.updateStats(650 + 250 * BB, 15 + 5 * BB, 0);
    
    int ambience = structure.numUpgrades() / 4;
    if (ambience == 3) ambience = 10;
    if (ambience == 2) ambience = 5;
    if (ambience == 1) ambience = 2;
    ambience += structure.upgradeLevel(GUEST_QUARTERS);
    ambience += structure.upgradeLevel(NOBLE_QUARTERS);
    structure.setAmbienceVal(ambience);
  }
  
  
  public Background[] careers() {
    //  TODO:  Base this off the Court setting!
    
    return new Background[] {
      Backgrounds.TECHNICIAN, Backgrounds.TROOPER,
      Backgrounds.AUDITOR, Backgrounds.STEWARD
      //Backgrounds.HONOUR_GUARD, Backgrounds.CONSORT
      //Backgrounds.HEIR, Backgrounds.ADVISOR
    };
  }
  
  
  public Traded[] services() {
    return new Traded[] {
      SERVICE_ADMIN, SERVICE_REFUGE, SERVICE_SECURITY,
      POWER, ATMO
    };
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "bastion");
  }
  
  
  public String helpInfo() {
    return
      "The Bastion is your seat of command for the settlement as a "+
      "whole, houses your family, advisors and bodyguards, and provides "+
      "basic logistic support.";
  }
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_SECURITY;
  }
}


