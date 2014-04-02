/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.actors ;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.common.AnimNames;
import stratos.graphics.solids.*;
import stratos.util.*;



//
//  You need to generate Special Effects for weapon-beams, shield bursts, and
//  acquisitions of credits and loot.



public class ActorGear extends Inventory implements Economy {
  
  
  final public static float
    SHIELD_CHARGE     = 5f,
    SHIELD_SHORTS     = 2f,
    SHIELD_REGEN_TIME = 10f ;
  final public static int
    MAX_RATIONS    = 5,
    MAX_FOOD_TYPES = 5,
    MAX_FUEL_CELLS = 5 ;
  
  private static boolean verbose = false ;
  
  
  final Actor actor ;
  float baseDamage, baseArmour ;
  Item device = null ;
  Item outfit = null ;
  float fuelCells = 0, currentShields ;
  
  
  public ActorGear(Actor actor) {
    super(actor) ;
    this.actor = actor ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(baseDamage) ;
    s.saveFloat(baseArmour) ;
    Item.saveTo(s, device) ;
    Item.saveTo(s, outfit) ;
    s.saveFloat(fuelCells) ;
    s.saveFloat(currentShields) ;
  }
  

  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
    baseDamage = s.loadFloat() ;
    baseArmour = s.loadFloat() ;
    device = Item.loadFrom(s) ;
    outfit = Item.loadFrom(s) ;
    fuelCells = s.loadFloat() ;
    currentShields = s.loadFloat() ;
  }
  
  
  public void setDamage(float d) {
    baseDamage = d ;
  }
  
  
  public void setArmour(float a) {
    baseArmour = a ;
  }
  
  
  
  /**  Maintenance, updates and spring cleaning-
    */
  public void updateGear(int numUpdates) {
    if (Float.isNaN(credits)) credits = 0 ;
    if (Float.isNaN(taxed)) taxed = 0 ;
    
    if (verbose) I.sayAbout(actor, "Updating gear...") ;
    if (outfit != null) regenerateShields() ;
    else currentShields = 0 ;
    for (Item item : allItems()) {
      if (item.refers instanceof Action) {
        if (verbose) I.sayAbout(actor, "  Applying item effect: "+item.refers) ;
        ((Action) item.refers).applyEffect() ;
      }
    }
  }
  
  
  public boolean addItem(Item item) {
    if (item == null || item.amount == 0) return false ;
    final int oldAmount = (int) amountOf(item) ;
    if (item.refers == actor) item = Item.withReference(item, null) ;
    if      (item.type instanceof DeviceType) equipDevice(item) ;
    else if (item.type instanceof OutfitType) equipOutfit(item) ;
    else if (! super.addItem(item)) return false ;

    final int inc = ((int) amountOf(item)) - oldAmount ;
    if (actor.inWorld() && inc != 0 && ! actor.indoors()) {
      String phrase = inc >= 0 ? "+" : "-" ;
      phrase+=" "+Item.withAmount(item, inc) ;
      actor.chat.addPhrase(phrase) ;
    }
    return true ;
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
  
  
  public float encumbrance() {
    //
    //  TODO:  Cache this each second?
    float sum = 0 ; for (Item i : allItems()) sum += i.amount ;
    sum /= actor.health.maxHealth() * (1 - actor.health.fatigueLevel()) ;
    return sum * sum ;
  }
  
  
  
  /**  Returns this actor's effective attack damage.  Actors without equipped
    *  weapons, or employing weapons in melee, gain a bonus based on their
    *  physical brawn.
    */
  public float attackDamage() {
    final Item weapon = deviceEquipped() ;
    final float brawnBonus = actor.traits.traitLevel(MUSCULAR) / 4 ;
    if (weapon == null) return (brawnBonus / 2) + baseDamage ;
    final DeviceType type = (DeviceType) weapon.type ;
    final float damage = type.baseDamage * (weapon.quality + 2f) / 4 ;
    if (type.hasProperty(MELEE)) return damage + brawnBonus + baseDamage ;
    else return damage + baseDamage ;
  }
  
  
  public float attackRange() {
    if (deviceType().hasProperty(RANGED))
      return actor.health.sightRange() ;
    else
      return 1 ;
  }
  
  
  public boolean meleeWeapon() {
    final Item weapon = deviceEquipped() ;
    if (weapon == null) return true ;
    if (deviceType().hasProperty(MELEE)) return true ;
    return false ;
  }
  
  
  public boolean physicalWeapon() {
    final Item weapon = deviceEquipped() ;
    if (weapon == null) return true ;
    if (deviceType().hasProperty(PHYSICAL)) return true ;
    return false ;
  }
  
  
  public boolean armed() {
    final DeviceType type = deviceType() ;
    return (type != null) && type.baseDamage > 0 ;
  }
  
  
  public boolean hasDeviceProperty(int bits) {
    final DeviceType type = deviceType();
    if (type == null) return false;
    return type.hasProperty(bits);
  }
  
  
  
  /**  Returns this actor's effective armour rating.  Actors not wearing any
    *  significant armour, or only lightly armoured, gain a bonus based on
    *  their reflexes.
    */
  public float armourRating() {
    final Item armour = outfitEquipped() ;
    float reflexBonus = actor.traits.traitLevel(MOTOR) / 4 ;
    if (armour == null) return reflexBonus + baseArmour ;
    
    final OutfitType type = (OutfitType) armour.type ;
    reflexBonus *= (20 - type.defence) ;
    final float rating = type.defence * (armour.quality + 1) / 4 ;
    return rating + baseArmour + Math.max(0, reflexBonus) ;
  }
  
  
  /**  Shield depletion and regeneration are handled here-
    */
  public float shieldCharge() {
    if (outfit == null) return 0 ;
    final OutfitType type = (OutfitType) outfit.type ;
    return type.shieldBonus * currentShields / SHIELD_CHARGE ;
  }
  
  
  public void boostShields(float boost, boolean capped) {
    currentShields += boost ;
    if (capped) currentShields = Visit.clamp(currentShields, 0, maxShields()) ;
  }
  
  
  public float afterShields(float damage, boolean physical) {
    if (damage <= 0 || ! actor.health.conscious()) return damage ;
    float reduction = shieldCharge() * Rand.num() ;
    if (physical) reduction /= 2 ;
    if (reduction > damage) reduction = damage ;
    currentShields -= reduction / SHIELD_CHARGE ;
    return damage - reduction ;
  }
  
  
  public float fuelCells() {
    return fuelCells ;
  }
  
  
  public float maxShields() {
    if (outfit == null) return 0 ;
    final OutfitType type = (OutfitType) outfit.type ;
    return 
      (SHIELD_CHARGE + type.shieldBonus) *
      (float) Math.sqrt(fuelCells / MAX_FUEL_CELLS) ;
  }
  
  
  public boolean hasShields() {
    if (outfit == null) return false ;
    final OutfitType type = (OutfitType) outfit.type ;
    return type.shieldBonus > 0 ;
  }
  
  
  private void regenerateShields() {
    final OutfitType type = (OutfitType) outfit.type ;
    final float regenTime =
      SHIELD_REGEN_TIME * 2f / (2 + type.shieldBonus) ;
    final float maxShield = maxShields() ;
    if (currentShields < maxShield) {
      final float nudge = maxShield / regenTime ;
      currentShields += nudge ;
      fuelCells -= nudge / 10f ;
      if (currentShields > maxShield) currentShields = maxShield ;
      if (fuelCells < 0) fuelCells = 0 ;
    }
    else {
      currentShields -= SHIELD_CHARGE / regenTime ;
      if (currentShields < maxShield) currentShields = maxShield ;
    }
  }
  
  

  /**  Here we deal with equipping/removing Devices-
    */
  public void equipDevice(Item device) {
    if (device != null && ! (device.type instanceof DeviceType))
      return ;
    this.device = device ;
    /*
    final Actor actor = (Actor) owner ;
    final JointSprite sprite = (JointSprite) actor.sprite() ;
    final Item oldItem = this.device ;
    //
    //  Attach/detach the appropriate media-
    if (oldItem != null && sprite != null) {
      final DeviceType oldType = (DeviceType) oldItem.type ;
      sprite.toggleGroup(oldType.groupName, false) ;
    }
    if (device != null && sprite != null) {
      final DeviceType newType = (DeviceType) device.type ;
      sprite.toggleGroup(newType.groupName, true) ;
    }
    //*/
  }
  
  
  public Item deviceEquipped() {
    return device ;
  }
  
  
  public DeviceType deviceType() {
    if (device == null) return null ;
    return (DeviceType) device.type ;
  }
  
  
  
  /**  Here, we deal with applying/removing Outfits-
    */
  public void equipOutfit(Item outfit) {
    if (! (outfit.type instanceof OutfitType)) return ;
    final Actor actor = (Actor) owner ;
    final SolidSprite sprite = (SolidSprite) actor.sprite() ;
    final Item oldItem = this.outfit ;
    this.outfit = outfit ;
    if (hasShields()) fuelCells = MAX_FUEL_CELLS ;
    //
    //  Attach/detach the appropriate media-
    if (oldItem != null) {
      final OutfitType type = (OutfitType) oldItem.type ;
      //  TODO:  FIGURE THIS OUT
      /*
      if (type.skin != null) sprite.applyOverlay(
        type.skin.asTexture(), AnimNames.MAIN_BODY, true
      );
      //*/
    }
    if (outfit != null) {
      final OutfitType type = (OutfitType) outfit.type ;
      //  TODO:  FIGURE THIS OUT
      /*
      if (type.skin != null) sprite.applyOverlay(
        type.skin.asTexture(), AnimNames.MAIN_BODY, false
      );
      //*/
      currentShields = SHIELD_CHARGE + type.shieldBonus ;
    }
  }
  
  
  public Item outfitEquipped() {
    return outfit ;
  }
  
  
  public OutfitType outfitType() {
    if (outfit == null) return null ;
    return (OutfitType) outfit.type ;
  }
}








