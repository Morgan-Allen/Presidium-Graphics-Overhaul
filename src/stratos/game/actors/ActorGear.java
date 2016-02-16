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
    SHIELD_REGEN_TIME = Stage.STANDARD_HOUR_LENGTH;
  final public static int
    MAX_AMMO_COUNT   = 40,
    AMMO_PER_UNIT    = 10,
    MAX_POWER_CELLS  =  5,
    CHARGES_PER_CELL =  8;
  
  private static boolean verbose = false;
  
  
  final Actor actor;
  private float baseDamage, baseArmour;
  
  private Item device = null;
  private Item outfit = null;
  private Item used[] = null;
  
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
    
    s.saveFloat(currentShields);
    s.saveFloat(encumbrance   );
  }
  

  public void loadState(Session s) throws Exception {
    super.loadState(s);
    
    baseDamage = s.loadFloat();
    baseArmour = s.loadFloat();
    device = Item.loadFrom(s);
    outfit = Item.loadFrom(s);
    
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
  
  
  public void onWorldExit() {
    for (Item i : allItems()) if (
      (i.refers != null && i.refers != actor) &&
      (! (i.refers instanceof Constant))
    ) {
      removeItem(i);
    }
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
    
    Item.checkForBreakdown(actor, device, 1, 1);
    Item.checkForBreakdown(actor, outfit, 1, 1);
    for (Item i : allItems()) if (! i.type.common()) {
      i.type.applyPassiveEffects(i, actor);
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
  
  
  public Item[] usable() {
    if (used != null) return used;
    final Batch <Item> all = new Batch();
    for (Item i : allItems()) if (i.type.techniques() != null) {
      all.add(i);
    }
    return used = (Item[]) all.toArray(Item.class);
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
      if (item.type.techniques() != null) used = null;
    }
    return OK;
  }
  
  
  public boolean addItem(Item item) {
    if (item == null || item.amount == 0) return false;
    encumbrance = -1;
    if (item.type.techniques() != null) used = null;
    
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
  public float consumption(Traded type) {
    if (! canDemand(type)) return 0;
    return type.normalCarry(actor);
  }
  
  
  public float relativeShortage(Traded type, boolean production) {
    final float demand = consumption(type);
    if (demand <= 0) return 0;
    final float amount = amountOf(type);
    return (demand - amount) / demand;
  }
  
  
  public float absoluteShortage(Traded type, boolean production) {
    final float demand = consumption(type), amount = amountOf(type);
    return demand - amount;
  }
  
  
  public float totalDemand(Traded type) {
    return consumption(type);
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
  
  

  /**  Here, we deal with applying/removing Outfits-
    */
  public void equipOutfit(Item outfit) {
    if (outfit != null && ! (outfit.type instanceof OutfitType)) return;
    
    final OutfitType oldType = outfitType();
    this.outfit = outfit;
    final OutfitType newType = outfitType();
    
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
  
  
  
  /**  Here we deal with equipping/removing Devices-
    */
  public void equipDevice(Item device) {
    if (device != null && ! (device.type instanceof DeviceType)) return;
    
    final DeviceType oldType = deviceType();
    this.device = device;
    final DeviceType newType = deviceType();
  }
  
  
  public Item deviceEquipped() {
    return device;
  }
  
  
  public DeviceType deviceType() {
    if (device == null) return null;
    return (DeviceType) device.type;
  }
  
  
  public boolean hasDeviceProperty(int bits) {
    final DeviceType type = deviceType();
    return type != null && type.hasProperty(bits);
  }
  
  
  public boolean meleeDeviceOnly() {
    return ! hasDeviceProperty(RANGED);
  }
  
  
  
  /**  Returns this actor's effective attack damage.  Actors without equipped
    *  weapons, or employing weapons in melee, gain a bonus based on their
    *  physical brawn.
    */
  public float totalDamage() {
    final Item weapon = deviceEquipped();
    if (weapon == null) return baseDamage;
    
    final DeviceType type = (DeviceType) weapon.type;
    final float damage = type.baseDamage * weapon.outputFromQuality();
    return damage + baseDamage;
  }
  
  
  public float attackRange() {
    if (deviceType().hasProperty(RANGED)) {
      return actor.health.sightRange();
    }
    else return 1;
  }
  
  
  
  /**  Returns this actor's effective armour rating.  Actors not wearing any
    *  significant armour, or only lightly armoured, gain a bonus based on
    *  their reflexes.
    */
  public float totalArmour() {
    final Item armour = outfitEquipped();
    float reflexBonus = 0;
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
  
  
  
  /**  Helper methods for dealing with power cells, shields and ammunition.
    */
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
    final OutfitType type = (OutfitType) outfit.type;
    return type.shieldBonus * (outfit.quality + 2f) / 4;
  }
  
  
  public boolean hasShields() {
    return shieldCharge() > 0;
  }
  
  
  private void regenerateShields() {
    final float max = maxShields();
    float cellsUsed = 0;
    
    if (currentShields < max) {
      float regen = max / SHIELD_REGEN_TIME;
      regen = Nums.clamp(regen, 0, max - currentShields);
      currentShields = Nums.clamp(currentShields + regen, 0, max);
      cellsUsed += regen / (max * CHARGES_PER_CELL);
    }
    if (currentShields > max) {
      final float sink = 5f / SHIELD_REGEN_TIME;
      currentShields = Nums.clamp(currentShields - sink, max, (max + 5) * 2);
    }
    bumpItem(Outfits.POWER_CELLS, 0 - cellsUsed);
  }
  
  
  public int maxPowerCells() {
    if (maxShields() == 0) return 0;
    final OutfitType type = outfitType();
    if (type.natural() || type.shieldBonus == 0) return 0;
    return MAX_POWER_CELLS;
  }
  
  
  public int maxAmmoUnits() {
    if (! hasDeviceProperty(Devices.RANGED)) return 0;
    final DeviceType type = deviceType();
    if (type.natural() || type.baseDamage == 0) return 0;
    return (MAX_AMMO_COUNT / AMMO_PER_UNIT);
  }
  
  
  public void depleteAmmo(int numShots) {
    float ammoUsed = numShots * 1f / AMMO_PER_UNIT;
    bumpItem(Devices.AMMO_CLIPS, 0 - ammoUsed);
  }
  
  
  public int ammoCount() {
    return (int) (amountOf(Devices.AMMO_CLIPS) * AMMO_PER_UNIT);
  }
  
  
  public boolean canFireWeapon() {
    if (meleeDeviceOnly()) return false;
    return maxAmmoUnits() == 0 || ammoCount() > 0;
  }
}



















