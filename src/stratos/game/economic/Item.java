/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.common.Session.Saveable;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



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
  final public static int ANY = -1;
  final public static int MAX_QUALITY = 4;
  
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
  public static interface Passive extends Session.Saveable {
    public void applyPassiveItem(Actor carries, Item from);
    public String describePassiveItem(Item from);
  }
  
  
  public static interface Dropped extends Owner, Selectable {
  }
  
  
  
  /**  Outside-accessible factory methods-
    */
  public static Item withAmount(Traded type, float amount) {
    return new Item(type, null, amount, 0);
  }
  
  
  public static Item withAmount(Item item, float amount) {
    final Item i = new Item(item.type, item.refers, amount, item.quality);
    return i;
  }
  
  
  public static Item withReference(Traded type, Saveable refers) {
    return new Item(type, refers, 1, 0);
  }
  
  
  public static Item withReference(Item item, Saveable refers) {
    return new Item(item.type, refers, item.amount, item.quality);
  }
  
  
  public static Item withQuality(Traded type, int quality) {
    return new Item(type, null, 1, Nums.clamp(quality, 5));
  }
  
  
  public static Item withQuality(Item item, int quality) {
    return new Item(
      item.type, item.refers, item.amount, Nums.clamp(quality, 5)
    );
  }
  
  
  public static Item with(
    Traded type, Saveable refers, float amount, float quality
  ) {
    if (amount < 0) I.complain("Amount must be positive!");
    return new Item(
      type, refers, amount, Nums.clamp(quality, 0, 4)
    );
  }
  
  
  public float priceAt(Owner venue, boolean sold) {
    return venue.priceFor(type, sold) * amount * PRICE_MULTS[(int) quality];
  }
  
  
  public float defaultPrice() {
    return type.basePrice() * amount * PRICE_MULTS[(int) quality];
  }
  
  
  public float outputFromQuality() {
    return OUTPUT_MULTS[(int) quality];
  }
  
  
  public boolean isCommon() {
    return type.form == FORM_MATERIAL || type.form == FORM_PROVISION;
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
    if (this.type != item.type) return false;
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
    describeFor(null, d);
  }
  
  
  public void describeFor(Actor owns, Description d) {
    //
    //  First describe yourself:
    String s = ""+type;
    if (
      type.form == FORM_DEVICE ||
      type.form == FORM_OUTFIT ||
      type.form == FORM_USABLE ||
      type.form == FORM_SPECIAL
    ) {
      s = (I.shorten(amount, 1))+" "+descQuality()+" "+s;
    }
    else if (refers == null && amount != ANY) {
      s = (I.shorten(amount, 1))+" "+s;
    }
    d.append(s);
    //
    //  Then describe anything your refer to-
    if (refers instanceof Passive) {
      d.append(((Passive) refers).describePassiveItem(this));
    }
    else if (refers != null && refers != owns) {
      d.append(" (");
      d.append(refers);
      d.append(")");
    }
  }
  
  
  public String descQuality() {
    return QUAL_NAMES[(int) (quality + 0.5f)];
  }
  
  
  public String toString() {
    final StringDescription SD = new StringDescription();
    describeFor(null, SD);
    return SD.toString();
  }
}


