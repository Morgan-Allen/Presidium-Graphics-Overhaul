/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.solids.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Devices.*;



public class ActorGear extends Inventory {
  
  
  final public static float
    SHIELD_REGEN_TIME = Stage.STANDARD_HOUR_LENGTH,
    FUEL_DEPLETE      = 0.1f;
  final public static int
    MAX_AMMO_COUNT = 40,
    MAX_POWER_CELLS = 5 ;
  
  private static boolean verbose = false;
  
  
  final Actor actor;
  private float baseDamage, baseArmour;
  
  private Item device = null;
  private Item outfit = null;
  private UsedItemType usedTypes[] = null;
  
  //  TODO- GET RID OF THESE.
  private int   ammoCount      =  0;
  private float powerCells     =  0;
  private float currentShields =  0;
  private float encumbrance    = -1;
  
  
  public ActorGear(Actor actor) {
    super(actor);
    this.actor = actor;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveFloat(baseDamage);
    s.saveFloat(baseArmour);
    Item.saveTo(s, device);
    Item.saveTo(s, outfit);
    
    s.saveInt  (ammoCount     );
    s.saveFloat(powerCells    );
    s.saveFloat(currentShields);
    s.saveFloat(encumbrance   );
  }
  

  public void loadState(Session s) throws Exception {
    super.loadState(s);
    
    baseDamage = s.loadFloat();
    baseArmour = s.loadFloat();
    device = Item.loadFrom(s);
    outfit = Item.loadFrom(s);
    
    ammoCount      = s.loadInt  ();
    powerCells     = s.loadFloat();
    currentShields = s.loadFloat();
    encumbrance    = s.loadFloat();
  }
  
  
  public void setBaseDamage(float d) {
    baseDamage = d;
  }
  
  
  public void setBaseArmour(float a) {
    baseArmour = a;
  }
  
  
  public float baseDamage() {
    return baseDamage;
  }
  
  
  public float baseArmour() {
    return baseArmour;
  }
  
  
  
  /**  Maintenance, updates and spring cleaning-
    */
  public void updateGear(int numUpdates) {
    if (Float.isNaN(credits)) credits = 0;
    if (Float.isNaN(taxed  )) taxed   = 0;
    
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("\nUpdating gear");
    
    if (outfit != null) regenerateShields();
    else currentShields = 0;
    Item.checkForBreakdown(actor, outfit, 1, 1);
    
    for (Item item : allItems()) {
      if (item.refers instanceof Item.Passive) {
        if (report) I.say("  Applying item effect: "+item.refers);
        ((Item.Passive) item.refers).applyPassiveItem(actor, item);
      }
    }
    
    encumbrance = -1;
    encumbrance();
  }
  
  
  public float encumbrance() {
    if (encumbrance != -1) return encumbrance;
    float sum = 0; for (Item i : allItems()) {
      if (i.type.form != Economy.FORM_MATERIAL) continue;
      sum += i.amount;
    }
    sum /= actor.health.maxHealth() * (1 - actor.health.fatigueLevel());
    return encumbrance = sum * sum;
  }
  
  
  public UsedItemType[] usedItemTypes() {
    if (usedTypes != null) return usedTypes;
    final Batch all = new Batch();
    for (Item i : allItems()) if (i.type instanceof UsedItemType) {
      all.add(i.type);
    }
    return usedTypes = (UsedItemType[]) all.toArray(UsedItemType.class);
  }
  
  
  public boolean removeItem(Item item) {
    
    if (item.matchKind(outfit) && item.amount > 0) {
      outfit = Item.withAmount(outfit, outfit.amount - item.amount);
      if (outfit.amount <= 0) equipOutfit(null);
      return true;
    }
    
    if (item.matchKind(device) && item.amount > 0) {
      device = Item.withAmount(device, device.amount - item.amount);
      if (device.amount <= 0) equipDevice(null);
      return true;
    }
    
    final boolean OK = super.removeItem(item);
    if (OK) {
      encumbrance = -1;
      if (item.type instanceof UsedItemType) usedTypes = null;
    }
    return OK;
  }
  
  
  public boolean addItem(Item item) {
    if (item == null || item.amount == 0) return false;
    encumbrance = -1;
    if (item.type instanceof UsedItemType) usedTypes = null;
    
    final int oldAmount = (int) amountOf(item);
    if (item.refers == actor) item = Item.withReference(item, null);
    
    if      (item.type instanceof DeviceType) equipDevice(item);
    else if (item.type instanceof OutfitType) equipOutfit(item);
    else if (! super.addItem(item)) return false;
    //
    //  Add a special 'item acquired' message to the actor's chat bubbles, if
    //  it crosses an integer threshold.
    final int inc = ((int) amountOf(item)) - oldAmount;
    if (actor.inWorld() && inc != 0 && ! actor.indoors()) {
      String phrase = inc >= 0 ? "+" : "-";
      phrase+=" "+Item.withAmount(item, inc);
      actor.chat.addPhrase(phrase);
    }
    return true;
  }
  
  
  public void incCredits(float inc) {
    if (Float.isNaN(inc)) I.complain("INC IS NOT-A-NUMBER!");
    if (Float.isNaN(credits)) credits = 0;
    if (inc == 0) return;
    
    final int oldC = (int) allCredits();
    super.incCredits(inc);
    final int newC = (int) allCredits();
    if (! actor.inWorld() || oldC == newC) return;
    
    String phrase = inc >= 0 ? "+" : "-";
    phrase+=" "+(int) Nums.abs(inc)+" credits";
    actor.chat.addPhrase(phrase);
  }
  
  
  
  /**  Overrides for supply-and-demand methods-
    */
  public float demandFor(Traded type) {
    if (! canDemand(type)) return 0;
    return type.normalCarry(actor);
  }
  

  public float shortageOf(Traded type) {
    final float demand = demandFor(type);
    if (demand <= 0) return 0;
    final float amount = amountOf(type);
    return (demand - amount) / demand;
  }
  
  
  public boolean canDemand(Traded type) {
    final Relation r = actor.relations.relationWith(type);
    if (r == null || r.type() != Relation.TYPE_GEAR) return false;
    return r.value() > 0;
  }
  
  
  public Item bestSample(
    Traded type, Session.Saveable refers, float maxAmount
  ) {
    if (type == deviceType()) return deviceEquipped();
    if (type == outfitType()) return outfitEquipped();
    return super.bestSample(type, refers, maxAmount);
  }
  
  
  
  /**  Here we deal with equipping/removing Devices-
    */
  public void equipDevice(Item device) {
    if (device != null && ! (device.type instanceof DeviceType)) return;
    
    final DeviceType oldType = deviceType();
    this.device = device;
    final DeviceType newType = deviceType();
    
    if (newType != oldType) {
      this.ammoCount = (newType != null) ? MAX_AMMO_COUNT : 0;
    }
  }
  
  
  public Item deviceEquipped() {
    return device;
  }
  
  
  public DeviceType deviceType() {
    if (device == null) return null;
    return (DeviceType) device.type;
  }
  
  
  
  /**  Returns this actor's effective attack damage.  Actors without equipped
    *  weapons, or employing weapons in melee, gain a bonus based on their
    *  physical brawn.
    */
  public float totalDamage() {
    final Item weapon = deviceEquipped();
    
    final float brawnBonus = actor.traits.traitLevel(MUSCULAR) / 4;
    if (weapon == null) return (brawnBonus / 2) + baseDamage;
    
    final DeviceType type = (DeviceType) weapon.type;
    final float damage = type.baseDamage * weapon.outputFromQuality();
    
    if (type.hasProperty(MELEE)) return damage + brawnBonus + baseDamage;
    else return damage + baseDamage;
  }
  
  
  public float attackRange() {
    if (deviceType().hasProperty(RANGED)) {
      return actor.health.sightRange();
    }
    else return 1;
  }
  
  
  public boolean hasDeviceProperty(int bits) {
    final DeviceType type = deviceType();
    return type != null && type.hasProperty(bits);
  }
  
  
  public boolean meleeDeviceOnly() {
    return ! hasDeviceProperty(RANGED);
  }
  
  
  public int ammoCount() {
    return ammoCount;
  }
  
  
  public void incAmmo(int inc) {
    ammoCount += inc;
    ammoCount = Nums.clamp(ammoCount, MAX_AMMO_COUNT);
  }
  
  
  public float ammoLevel() {
    return ammoCount * 1f / MAX_AMMO_COUNT;
  }
  
  

  /**  Here, we deal with applying/removing Outfits-
    */
  public void equipOutfit(Item outfit) {
    if (outfit != null && ! (outfit.type instanceof OutfitType)) return;
    
    final OutfitType oldType = outfitType();
    this.outfit = outfit;
    final OutfitType newType = outfitType();
    
    if (newType != oldType) {
      powerCells     = (maxShields() > 0) ? MAX_POWER_CELLS : 0;
      currentShields = maxShields();
    }
    
    //  TODO:  FIGURE THIS OUT
    /*
    //
    //  Attach/detach the appropriate media-
    if (oldItem != null) {
      final OutfitType type = (OutfitType) oldItem.type;
      if (type.skin != null) sprite.applyOverlay(
        type.skin.asTexture(), AnimNames.MAIN_BODY, true
      );
    }
    if (newItem != null) {
      final OutfitType type = (OutfitType) outfit.type;
        TODO:  FIGURE THIS OUT
      if (type.skin != null) sprite.applyOverlay(
        type.skin.asTexture(), AnimNames.MAIN_BODY, false
      );
      currentShields = maxShields();
    }
    //*/
  }
  
  
  public Item outfitEquipped() {
    return outfit;
  }
  
  
  public OutfitType outfitType() {
    if (outfit == null) return null;
    return (OutfitType) outfit.type;
  }
  
  
  
  /**  Returns this actor's effective armour rating.  Actors not wearing any
    *  significant armour, or only lightly armoured, gain a bonus based on
    *  their reflexes.
    */
  public float totalArmour() {
    final Item armour = outfitEquipped();
    float reflexBonus = actor.traits.traitLevel(MOTOR) / 4;
    if (armour == null) return reflexBonus + baseArmour;
    
    final OutfitType type = (OutfitType) armour.type;
    reflexBonus *= (20 - type.defence) / 10f;
    final float rating = type.defence * (armour.quality + 1) / 4;
    
    if (verbose && I.talkAbout == actor) {
      I.say("\nBase armour: "+type.defence);
      I.say("  Reflex bonus: "+reflexBonus);
      I.say("  Quality rating: "+rating);
    }
    
    return rating + baseArmour + Nums.max(0, reflexBonus);
  }
  
  
  public float shieldCharge() {
    return currentShields;
  }
  
  
  public void boostShields(float boost, boolean capped) {
    currentShields += boost;
    if (capped) currentShields = Nums.clamp(currentShields, 0, maxShields());
  }
  
  
  public float afterShields(float damage, boolean physical) {
    if (damage <= 0 || ! actor.health.conscious()) return damage;
    float reduction = shieldCharge() * Rand.num();
    if (reduction <= 0) return damage;
    if (physical) reduction /= 2;
    if (reduction > damage) reduction = damage;
    currentShields -= reduction / 2;
    return damage - reduction;
  }
  
  
  public float maxShields() {
    if (outfit == null) return 0;
    final float bulk = actor.health.baseBulk() / ActorHealth.DEFAULT_BULK;
    final OutfitType type = (OutfitType) outfit.type;
    return type.shieldBonus * bulk * (outfit.quality + 2f) / 4;
  }
  
  
  public boolean hasShields() {
    return shieldCharge() > 0;
  }
  
  
  private void regenerateShields() {
    final float max = maxShields();
    
    //  TODO:  Log these properly.
    /*
    if (I.talkAbout == actor) {
      I.say("\nShields are: "+currentShields+"/"+max);
    }
    //*/
    
    if (currentShields < max) {
      float regen = max / SHIELD_REGEN_TIME;
      regen = Nums.clamp(regen, 0, max - currentShields);
      currentShields = Nums.clamp(currentShields + regen, 0, max);
      powerCells -= FUEL_DEPLETE * regen / (max * SHIELD_REGEN_TIME);
    }
    if (currentShields > max) {
      final float sink = 5f / SHIELD_REGEN_TIME;
      currentShields = Nums.clamp(currentShields - sink, max, (max + 5) * 2);
    }
  }
  
  
  public float powerCells() {
    return powerCells;
  }
  
  
  public void incPowerCells(float inc) {
    if (inc <= 0) return;
    powerCells += inc;
    powerCells = Nums.clamp(powerCells, 0, MAX_POWER_CELLS);
  }
}








