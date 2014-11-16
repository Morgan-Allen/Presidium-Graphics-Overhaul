/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



public class Lictovore extends Fauna {
  
  
  
  /**  Constructors, setup and save/load methods-
    */
  public Lictovore(Base base) {
    super(Species.LICTOVORE, base);
  }
  
  
  public Lictovore(Session s) throws Exception {
    super(s);
    /*
    if (! inWorld()) I.say("Must be dead...");
    if (! health.alive()) {
      I.say("DEAD LICTOVORE STILL REFERENCED");
      //new Exception().printStackTrace();
    }
    //*/
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected void initStats() {
    //
    //  TODO:  PUT ALL THESE ATTRIBUTES IN THE SPECIES FIELDS
    traits.initAtts(20, 15, 5);
    traits.setLevel(HAND_TO_HAND, 10);
    traits.setLevel(STEALTH_AND_COVER, 10);
    
    traits.setLevel(DEFENSIVE, 2);
    traits.setLevel(FEARLESS , 1);
    
    health.initStats(
      20,  //lifespan
      species.baseBulk ,//bulk bonus
      species.baseSight,//sight range
      species.baseSpeed,//move speed,
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(15);
    gear.setBaseArmour(5);
  }
  
  
  public float radius() {
    return 0.5f;
  }
  
  
  
  /**  Supplemental behaviour methods-
    */
  protected void addReactions(Target seen, Choice choice) {
    if (seen == null) return;
    if (seen instanceof Actor) {
      ///I.say("\nHAVE SEEN: "+seen+"\n");
      choice.add(new Retreat(this));
      choice.add(new Combat(this, (Actor) seen));
    }
  }
  

  protected Behaviour nextHunting() {
    if (mind.home() instanceof Venue) {
      final Venue nest = (Venue) mind.home();
      if (health.juvenile() && nest.stocks.amountOf(PROTEIN) > 0) {
        final Action homeFeed = new Action(
          this, nest,
          this, "actionHomeFeeding",
          Action.REACH_DOWN, "Feeding at nest"
        );
        homeFeed.setPriority((1 - health.caloryLevel()) * Action.PARAMOUNT);
        return homeFeed;
      }
    }
    return super.nextHunting();
  }
  
  
  protected void addChoices(Choice choice) {
    super.addChoices(choice);
    //
    //  Determine whether you should fight with others of your kind-
    float crowding = Nest.crowdingFor(this);
    crowding += 1 - ((health.caloryLevel() + 0.5f) / 2);
    final Fauna fights = findCompetition();
    if (fights != null && crowding > 1) {
      final Plan fighting = new Combat(this, fights).setMotive(
        Plan.MOTIVE_EMERGENCY,
        (crowding - 1) * Plan.PARAMOUNT
      );
      choice.add(fighting);
    }
    //
    //  Determine whether to regurgitate meat at home-
    choice.add(nextRegurgitation());
    //
    //  Determine whether you should mark your territory-
    final Tile toMark = findTileToMark();
    if (toMark != null && ! health.juvenile()) {
      //I.sayAbout(this, "Tile to mark: "+toMark);
      final Action marking = new Action(
        this, toMark,
        this, "actionMarkTerritory",
        Action.STAND, "Marking Territory"
      );
      marking.setPriority(Action.CASUAL);
      choice.add(marking);
    }
  }
  
  
  private Action nextRegurgitation() {
    if (health.juvenile() || ! (mind.home() instanceof Venue)) return null;
    final Venue nest = (Venue) mind.home();
    
    int numYoung = 0;
    for (Actor a : nest.personnel().residents()) {
      if (a.health.juvenile()) numYoung++;
    }
    final float excessFood = health.caloryLevel() - 1;
    if (numYoung == 0 || excessFood <= 0) return null;
    if (nest.inventory().amountOf(PROTEIN) >= (numYoung * 5)) return null;
    
    final Action deposit = new Action(
      this, nest,
      this, "actionDepositFood",
      Action.REACH_DOWN, "Returning with food for young"
    );
    deposit.setPriority(excessFood * Action.PARAMOUNT * numYoung);
    return deposit;
  }
  
  
  public boolean actionDepositFood(Fauna actor, Venue nest) {
    float energy = health.caloryLevel() - 0.5f;
    if (energy <= 0) return false;
    actor.health.loseSustenance(energy);
    energy *= actor.health.maxHealth() / MEAT_CONVERSION;
    nest.stocks.addItem(Item.withAmount(PROTEIN, energy));
    return true;
  }
  
  
  public boolean actionHomeFeeding(Fauna actor, Venue nest) {
    float hunger = 1 - health.caloryLevel();
    hunger *= actor.health.maxHealth() / MEAT_CONVERSION;
    final float amountTaken = Math.min(hunger, nest.stocks.amountOf(PROTEIN));
    if (amountTaken <= 0) return false;
    nest.stocks.removeItem(Item.withAmount(PROTEIN, amountTaken));
    actor.health.takeCalories(amountTaken * MEAT_CONVERSION, 1);
    return true;
  }
  
  
  private Fauna findCompetition() {
    final Batch <Fauna> tried = new Batch <Fauna> ();
    for (Target e : senses.awareOf()) if (e instanceof Lictovore) {
      if (e == this) continue;
      final Lictovore m = (Lictovore) e;
      tried.add(m);
    }
    return (Fauna) Rand.pickFrom(tried);
  }
  
  
  private Tile findTileToMark() {
    if (! (mind.home() instanceof Venue)) return null;
    final Venue lair = (Venue) mind.home();
    float angle = Rand.num() * (float) Math.PI * 2;
    final Vec3D p = lair.position(null);
    final int range = Nest.forageRange(species);
    final Tile tried = world.tileAt(
      p.x + (float) (Math.cos(angle) * range),
      p.y + (float) (Math.sin(angle) * range)
    );
    if (tried == null) return null;
    final Tile free = Spacing.nearestOpenTile(tried, tried);
    if (free == null) return null;
    
    final PresenceMap markMap = world.presences.mapFor(SpiceMidden.class);
    final SpiceMidden near = (SpiceMidden) markMap.pickNearest(free, range);
    final float dist = near == null ? 10 : Spacing.distance(near, free);
    if (dist < 5) return null;
    
    return free;
  }
  
  
  public boolean actionMarkTerritory(Lictovore actor, Tile toMark) {
    if (toMark.onTop() != null || toMark.blocked()) return false;
    final SpiceMidden midden = new SpiceMidden();
    midden.enterWorldAt(toMark.x, toMark.y, world);
    return true;
  }
}














