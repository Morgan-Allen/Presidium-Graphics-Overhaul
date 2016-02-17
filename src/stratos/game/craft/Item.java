/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.craft;
import static stratos.game.craft.Economy.*;

import stratos.game.common.*;
import stratos.game.common.Session.Saveable;
import stratos.user.*;
import stratos.util.*;



/**  More representative of the abstract 'listing' of an item than a specific
  *  concrete object.
  */
public class Item {
  
  
  /**  Type definition.
    */
  final static String QUAL_NAMES[] = {
    "Crude", "Basic", "Standard", "Quality", "Luxury"
  };
  final static float PRICE_MULTS[] = {
    0.75f, 1.0f, 1.35f, 1.85f, 2.5f
  };
  final static float OUTPUT_MULTS[] = {
    0.75f, 0.9f, 1.0f, 1.1f, 1.25f
  };
  
  
  
  /**  Field definitions, standard constructors and save/load functionality-
    */
  final public static int
    ANY = -1,
    BAD_QUALITY = 0,
    AVG_QUALITY = 2,
    MAX_QUALITY = 4,
    DEFAULT_CARRY = 1;
  
  final public Traded type;
  final public Saveable refers;
  final public float amount;
  final public float quality;
  
  
  private Item(
    Traded type, Saveable refers, float amount, float quality
  ) {
    this.type = type;
    this.amount = amount;
    this.quality = quality;
    this.refers = refers;
  }
  
  
  public static Item loadFrom(Session s) throws Exception {
    final Traded type = (Traded) s.loadObject();
    if (type == null) return null;
    return new Item(
      type,
      s.loadObject(),
      s.loadFloat(),
      s.loadFloat()
    );
  }
  
  
  public static void saveTo(Session s, Item item) throws Exception {
    if (item == null) { s.saveObject(null); return; }
    s.saveObject(item.type);
    s.saveObject(item.refers);
    s.saveFloat(item.amount);
    s.saveFloat(item.quality);
  }
  
  
  public static void saveItemsTo(Session s, Item items[]) throws Exception {
    if (items == null) { s.saveInt(-1); return; }
    s.saveInt(items.length);
    for (Item i : items) saveTo(s, i);
  }
  
  
  public static Item[] loadItemsFrom(Session s) throws Exception {
    final int count = s.loadInt();
    if (count == -1) return null;
    final Item items[] = new Item[count];
    for (int i = 0; i < count; i++) items[i] = loadFrom(s);
    return items;
  }
  
  
  
  /**  Methods for delivering special FX-
    */
  public static interface Dropped extends Owner, Selectable {
  }
  
  
  public static void checkForBreakdown(
    Actor actor, Item implement, float damageLevel, int period
  ) {
    if (implement == null || implement.type.natural()) return;
    if (damageLevel <= 0 || period <= 0) return;
    
    float wearChance = period * 1f / Stage.STANDARD_DAY_LENGTH;
    wearChance *= damageLevel / GameSettings.ITEM_WEAR_DAYS;
    wearChance *= (Item.AVG_QUALITY + 0.5f) / (1 + implement.quality);
    float wearFraction = Rand.num();
    
    if (wearChance > (Rand.num() * wearFraction)) {
      actor.gear.removeItem(Item.withAmount(implement, wearFraction));
    }
  }
  
  
  
  /**  Outside-accessible factory methods-
    */
  public static Item withAmount(Traded type, float amount) {
    return with(type, null, amount, 0);
  }
  
  
  public static Item withAmount(Item item, float amount) {
    return with(item.type, item.refers, amount, item.quality);
  }
  
  
  public static Item withReference(Traded type, Saveable refers) {
    return with(type, refers, 1, 0);
  }
  
  
  public static Item withReference(Item item, Saveable refers) {
    return with(item.type, refers, item.amount, item.quality);
  }
  
  
  public static Item withQuality(Traded type, int quality) {
    return with(type, null, 1, Nums.clamp(quality, 5));
  }
  
  
  public static Item withQuality(Item item, int quality) {
    return with(
      item.type, item.refers, item.amount, Nums.clamp(quality, 5)
    );
  }
  
  
  public static Item with(
    Traded type, Saveable refers, float amount, float quality
  ) {
    return new Item(
      type, refers,
      amount <= 0 ? 1 : amount,
      Nums.clamp(quality, BAD_QUALITY, MAX_QUALITY)
    );
  }
  
  
  public float outputFromQuality() {
    return OUTPUT_MULTS[(int) quality];
  }
  
  
  
  /**  Pricing calculations-
    */
  public float priceAt(Owner venue, boolean sold) {
    return priceFor(type, amount, quality, venue, sold);
  }
  
  
  public float defaultPrice() {
    return priceFor(type, amount, -1, null, false);
  }
  
  
  public float pricePerDay() {
    return defaultPrice() / GameSettings.ITEM_WEAR_DAYS;
  }
  

  protected static float priceFor(
    Traded type, float amount, float quality, Owner venue, boolean sold
  ) {
    float price = 0;
    if (venue != null) price += venue.priceFor(type, sold);
    else price += type.priceMargin();
    
    if (amount  >= 0 &&   type.common()) price *= amount;
    if (quality >= 0 && ! type.common()) price *= PRICE_MULTS[(int) quality];
    
    final Conversion m = type.materials();
    if (m != null) for (Item i : m.raw) {
      price += priceFor(i.type, i.amount, i.quality, venue, sold);
    }
    
    return price;
  }
  
  
  
  /**  Matching/equality functions-
    */
  public static Item asMatch(Traded type, Saveable refers) {
    return new Item(type, refers, ANY, ANY);
  }
  
  
  public static Item asMatch(Traded type, int quality) {
    return new Item(type, null, ANY, quality);
  }
  
  
  public static Item asMatch(Traded type, Saveable refers, int quality) {
    return new Item(type, refers, ANY, Nums.clamp(quality, 5));
  }
  
  
  public boolean matchKind(Item item) {
    if (item == null || this.type != item.type) {
      return false;
    }
    if (this.refers != null) {
      if (item.refers == null) return false;
      if (! this.refers.equals(item.refers)) return false;
    }
    return true;
  }
  
  
  public boolean isMatch() {
    return amount == ANY || quality == ANY;
  }
  
  
  public boolean equals(Object o) {
    return matchKind((Item) o);
  }
  
  
  public int hashCode() {
    return
      (type.uniqueID() * 13 * 5) +
      ((refers == null ? 0 : (refers.hashCode() % 13)) * 5);
  }
  
  
  
  /**  Rendering/interface functions-
    */
  public void describeTo(Description d) {
    type.describeFor(null, this, d);
  }
  
  
  public String descQuality() {
    return QUAL_NAMES[(int) (quality + 0.5f)];
  }
  
  
  public String toString() {
    final StringDescription SD = new StringDescription();
    type.describeFor(null, this, SD);
    return SD.toString();
  }
}


