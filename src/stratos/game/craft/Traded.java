/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import stratos.game.common.*;

import static stratos.game.craft.Economy.*;

import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.Text;
import stratos.user.*;
import stratos.util.*;
import stratos.start.Assets;



/**  Used to represent the types of goods and services that venues can provide
  *  or produce.
  */
public class Traded extends Constant implements Session.Saveable {
  
  
  /**  Data fields, constructors, setup and save/load methods-
    */
  final static String ITEM_PATH = "media/Items/";
  final public static CutoutModel
    DEFAULT_MODEL  = CutoutModel.fromImage(
      Traded.class, ITEM_PATH+"crate.gif", 0.4f, 0.5f
    ),
    SHORTAGE_MODEL = CutoutModel.fromImage(
      Traded.class, ITEM_PATH+"short_icon.png", 0.4f, 0.5f
    ),
    OKAY_MODEL     = CutoutModel.fromImage(
      Traded.class, ITEM_PATH+"okay_icon.png", 0.4f, 0.5f
    ),
    QUESTION_MODEL = CutoutModel.fromImage(
      Traded.class, ITEM_PATH+"what_icon.png", 0.4f, 0.5f
    );
  
  final static Conversion
    NATURAL_MATERIALS = new Conversion((Class) null, "natural_materials");
  
  final public static Index <Traded> INDEX = new Index <Traded> ();
  
  final public int form;
  final public String description;
  
  final public ImageAsset icon;
  final public CutoutModel model;
  
  final public String supplyKey, demandKey;
  
  //  TODO:  You may need multiple materials!
  private Conversion materials;
  private float priceMargin, defaultPrice;
  final Batch <Blueprint> sources = new Batch();
  
  
  protected Traded(
    Class typeClass, int form,
    String name,
    int basePrice,
    String description
  ) {
    this(typeClass, name, null, form, basePrice, description);
  }
  

  protected Traded(
    Class typeClass,
    String name, String imgName,
    int form, int basePrice
  ) {
    this(typeClass, name, imgName, form, basePrice, null);
  }
  
  
  public Traded(
    Class typeClass,
    String name, String imgName,
    int form, int basePrice,
    String description
  ) {
    super(INDEX, name, name);
    this.form         = form;
    this.priceMargin  = basePrice * GameSettings.SPENDING_MULT;
    this.defaultPrice = priceMargin;
    
    this.description = description;
    final String imagePath = ITEM_PATH+imgName;
    final float IS = BuildingSprite.ITEM_SIZE;
    
    if (Assets.exists(imagePath)) {
      this.icon  = ImageAsset .fromImage(typeClass, imagePath        );
      this.model = CutoutModel.fromImage(typeClass, imagePath, IS, IS);
    }
    else {
      this.icon  = ImageAsset.fromImage(typeClass, DEFAULT_MODEL.fileName());
      this.model = DEFAULT_MODEL;
    }
    
    this.supplyKey = name+"_supply";
    this.demandKey = name+"_demand";
  }
  
  
  protected void setPriceMargin(float margin, Conversion materials) {
    this.priceMargin  = margin * GameSettings.SPENDING_MULT;
    this.materials    = materials;
    this.defaultPrice = Item.priceFor(this, -1, -1, null, false);
  }
  
  
  public void addSource(Blueprint b) {
    sources.include(b);
  }
  
  
  public static Traded loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Other assorted property-queries:
    */
  protected float priceMargin() {
    return priceMargin;
  }
  
  
  public float defaultPrice() {
    return defaultPrice;
  }
  
  
  public Conversion materials() {
    return materials;
  }
  
  
  public boolean hasSourceAt(Venue v) {
    if (sources.includes(v.blueprint)) return true;
    if (materials == null) return false;
    return materials.producesAt(v);
  }
  
  
  public boolean common() {
    return form == FORM_MATERIAL || form == FORM_PROVISION;
  }
  
  
  public boolean natural() {
    return materials == NATURAL_MATERIALS;
  }
  
  
  public int normalCarry(Actor actor) {
    return Item.DEFAULT_CARRY;
  }
  
  
  public float useRating(Actor actor) {
    return 0;
  }
  
  
  public void applyPassiveEffects(Item item, Actor owner) {
    return;
  }
  
  
  public Technique[] techniques() {
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeHelp(Description d, Selectable prior) {
    
    Text.insert(icon.asTexture(), 20, 20, true, d);
    d.append(description);
    d.append("\n  (Base price "+defaultPrice()+" credits)");
    Text.cancelBullet(d);
    
    final Base base = BaseUI.currentPlayed();
    if (base == null) return;

    final Batch <Blueprint>
      canMake = new Batch <Blueprint> (),
      canUse  = new Batch <Blueprint> ();
    for (Blueprint b : base.setup.available()) {
      if (b.category == Target.TYPE_WIP) continue;
      if (b.producing(this) != null) canMake.include(b);
      if (b.consuming(this) != null) canUse .include(b);
    }
    
    d.append("\n");
    for (Blueprint b : canUse) {
      final Conversion c = b.consuming(this);
      d.append("\nUsed by ");
      d.append(b);
      if (c.out != null) { d.append(" to make "); d.append(c.out.type); }
    }
    
    if (canMake.size() > 0) d.append("\n");
    
    for (Blueprint b : canMake) {
      final Conversion c = b.producing(this);
      if (c.raw.length == 0) {
        d.append("\nMade at ");
        d.append(b);
      }
      else {
        d.append("\n"+c.out+" made from ");
        for (Item i : c.raw) { i.describeTo(d); d.append(" "); }
        d.append("at ");
        d.append(b);
      }
    }
    
    if (common()) {
      d.append("\n");
      final float localShort = base.demands.primaryShortage(this);
      if (localShort >= 0) {
        final int percent = (int) (localShort * 100);
        d.append("\nLocal demand: "+percent+"% shortage");
      }
      else {
        final int percent = (int) (localShort * -100);
        d.append("\nLocal demand: "+percent+"% surplus");
      }
      final float tradeShort = base.demands.tradingShortage(this);
      if (tradeShort >= 0) {
        final int percent = (int) (tradeShort * 100);
        d.append("\nTrade demand: "+percent+"% shortage");
      }
      else {
        final int percent = (int) (tradeShort * -100);
        d.append("\nTrade demand: "+percent+"% surplus");
      }
    }
  }
  
  
  public void describeFor(Actor owns, Item i, Description d) {
    //
    //  First describe yourself:
    String s = "";
    if (this != SAMPLES && (
      form == FORM_DEVICE ||
      form == FORM_OUTFIT ||
      form == FORM_USED_ITEM ||
      form == FORM_SPECIAL
    )) {
      s = (I.shorten(i.amount, 1))+" "+i.descQuality()+" "+s;
    }
    else if (i.refers == null && i.amount != Item.ANY) {
      s = (I.shorten(i.amount, 1))+" "+s;
    }
    d.append(s);
    d.append(name, this);
    describeRefers(owns, i, d);
  }
  
  
  protected void describeRefers(Actor owns, Item i, Description d) {
    if (i.refers != null && i.refers != owns) {
      d.append(" (");
      d.append(i.refers);
      d.append(")");
    }
  }
}












