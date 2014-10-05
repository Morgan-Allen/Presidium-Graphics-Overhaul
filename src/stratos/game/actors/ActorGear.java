/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.actors;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



public class ActorGear extends Inventory {
  
  
  final public static float
    SHIELD_REGEN_TIME = World.STANDARD_HOUR_LENGTH,
    FUEL_DEPLETE      = 0.1f;
  final public static int
    MAX_AMMO_COUNT = 40,
    MAX_POWER_CELLS = 5 ;
  
  private static boolean verbose = false;
  
  
  final Actor actor;
  private float baseDamage, baseArmour;
  
  private Item device = null;
  private Item outfit = null;
  
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
  
  
  public void setDamage(float d) {
    baseDamage = d;
  }
  
  
  public void setArmour(float a) {
    baseArmour = a;
  }
  
  
  
  /**  Maintenance, updates and spring cleaning-
    */
  public void updateGear(int numUpdates) {
    if (Float.isNaN(credits)) credits = 0;
    if (Float.isNaN(taxed)) taxed = 0;
    
    if (verbose && I.talkAbout == actor) I.say("Updating gear...");
    
    if (outfit != null) regenerateShields();
    else currentShields = 0;
    for (Item item : allItems()) {
      if (item.refers instanceof Action) {
        //if (verbose) I.sayAbout(actor, "  Applying item effect: "+item.refers);
        ((Action) item.refers).applyEffect();
      }
    }
    encumbrance = -1;
    encumbrance();
  }
  
  
  public float encumbrance() {
    if (encumbrance != -1) return encumbrance;
    float sum = 0; for (Item i : allItems()) sum += i.amount;
    sum /= actor.health.maxHealth() * (1 - actor.health.fatigueLevel());
    return encumbrance = sum * sum;
  }
  
  
  public boolean removeItem(Item item) {
    final boolean OK = super.removeItem(item);
    if (OK) encumbrance = -1;
    return OK;
  }
  
  
  public boolean addItem(Item item) {
    if (item == null || item.amount == 0) return false;
    encumbrance = -1;
    
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
    
    final int oldC = (int) credits();
    super.incCredits(inc);
    final int newC = (int) credits();
    if (! actor.inWorld() || oldC == newC) return;
    
    String phrase = inc >= 0 ? "+" : "-";
    phrase+=" "+(int) Math.abs(inc)+" credits";
    actor.chat.addPhrase(phrase);
  }
  
  
  
  /**  Here we deal with equipping/removing Devices-
    */
  public void equipDevice(Item device) {
    if (device != null && ! (device.type instanceof DeviceType)) return;
    this.device = device;
    this.ammoCount = MAX_AMMO_COUNT;
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
  public float attackDamage() {
    final Item weapon = deviceEquipped();
    
    final float brawnBonus = actor.traits.traitLevel(MUSCULAR) / 4;
    if (weapon == null) return (brawnBonus / 2) + baseDamage;
    
    final DeviceType type = (DeviceType) weapon.type;
    final float damage = type.baseDamage * (weapon.quality + 2f) / 4;
    
    if (type.hasProperty(MELEE)) return damage + brawnBonus + baseDamage;
    else return damage + baseDamage;
  }
  
  
  public float attackRange() {
    if (deviceType().hasProperty(RANGED)) {
      return actor.health.sightRange();
    }
    else return 1;
  }
  
  
  public boolean meleeWeapon() {
    final Item weapon = deviceEquipped();
    if (weapon == null) return true;
    if (deviceType().hasProperty(MELEE)) return true;
    return false;
  }
  
  
  public boolean physicalWeapon() {
    final Item weapon = deviceEquipped();
    if (weapon == null) return true;
    if (deviceType().hasProperty(Economy.KINETIC)) return true;
    return false;
  }
  
  
  public boolean armed() {
    final DeviceType type = deviceType();
    return baseDamage > 0 || ((type != null) && type.baseDamage > 0);
  }
  
  
  public boolean hasDeviceProperty(int bits) {
    final DeviceType type = deviceType();
    if (type == null) return false;
    return type.hasProperty(bits);
  }
  
  
  public int ammoCount() {
    return ammoCount;
  }
  
  
  public void incAmmo(int inc) {
    ammoCount += inc;
    ammoCount = Visit.clamp(ammoCount, MAX_AMMO_COUNT);
  }
  
  
  public float ammoLevel() {
    return ammoCount * 1f / MAX_AMMO_COUNT;
  }
  
  

  /**  Here, we deal with applying/removing Outfits-
    */
  public void equipOutfit(Item outfit) {
    if (! (outfit.type instanceof OutfitType)) return;
    final Actor actor = (Actor) owner;
    final SolidSprite sprite = (SolidSprite) actor.sprite();
    final Item oldItem = this.outfit;
    this.outfit = outfit;
    if (maxShields() > 0) powerCells = MAX_POWER_CELLS;
    //
    //  Attach/detach the appropriate media-
    if (oldItem != null) {
      final OutfitType type = (OutfitType) oldItem.type;
      //  TODO:  FIGURE THIS OUT
      /*
      if (type.skin != null) sprite.applyOverlay(
        type.skin.asTexture(), AnimNames.MAIN_BODY, true
      );
      //*/
    }
    if (outfit != null) {
      final OutfitType type = (OutfitType) outfit.type;
      //  TODO:  FIGURE THIS OUT
      /*
      if (type.skin != null) sprite.applyOverlay(
        type.skin.asTexture(), AnimNames.MAIN_BODY, false
      );
      //*/
      currentShields = maxShields();
    }
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
  public float armourRating() {
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
    
    return rating + baseArmour + Math.max(0, reflexBonus);
  }
  
  
  public float shieldCharge() {
    return currentShields;
  }
  
  
  public void boostShields(float boost, boolean capped) {
    currentShields += boost;
    if (capped) currentShields = Visit.clamp(currentShields, 0, maxShields());
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
    if (currentShields < max) {
      float regen = max / SHIELD_REGEN_TIME;
      regen = Visit.clamp(regen, 0, max - currentShields);
      currentShields = Visit.clamp(currentShields + regen, 0, max);
      powerCells -= FUEL_DEPLETE * regen / (max * SHIELD_REGEN_TIME);
    }
    if (currentShields > max) {
      final float sink = 5f / SHIELD_REGEN_TIME;
      currentShields = Visit.clamp(currentShields - sink, max, (max + 5) * 2);
    }
  }
  
  
  public float powerCells() {
    return powerCells;
  }
  
  
  public void incPowerCells(float inc) {
    if (inc <= 0) return;
    powerCells += inc;
    powerCells = Visit.clamp(powerCells, 0, MAX_POWER_CELLS);
  }
}








