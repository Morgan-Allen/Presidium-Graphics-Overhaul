


package stratos.game.economic;
import stratos.game.common.*;
import static stratos.game.economic.Economy.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.user.BaseUI;
import stratos.user.InstallPane;
import stratos.user.Selectable;
import stratos.user.UIConstants;
import stratos.user.notify.*;
import stratos.util.*;



/**  Used to represent the types of goods and services that venues can provide
  *  or produce.
  */
public class Traded extends Constant implements Session.Saveable {
  
  
  final static String
    ITEM_PATH = "media/Items/",
    DEFAULT_PIC_PATH = ITEM_PATH+"crate.gif";
  
  final public static Index <Traded> INDEX = new Index <Traded> ();
  
  final public int form;
  final public String name, description;
  
  final public String picPath;
  final public ImageAsset icon;
  final public CutoutModel model;
  
  final public String supplyKey, demandKey;
  
  private Conversion materials;
  private float basePrice;
  
  
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
    
    this.form = form;
    this.name = name;
    this.description = description;
    
    this.basePrice = basePrice * GameSettings.SPENDING_MULT;
    final String imagePath = ITEM_PATH+imgName;
    final float IS = BuildingSprite.ITEM_SIZE;
    
    if (new java.io.File(imagePath).exists()) {
      this.picPath = imagePath;
      this.icon  = ImageAsset.fromImage(typeClass, imagePath);
      this.model = CutoutModel.fromImage(typeClass, imagePath, IS, IS);
    }
    else {
      this.picPath = DEFAULT_PIC_PATH;
      this.icon  = ImageAsset.fromImage(typeClass, picPath);
      this.model = CutoutModel.fromImage(typeClass, picPath, IS, IS);
    }
    
    this.supplyKey = name+"_supply";
    this.demandKey = name+"_demand";
  }
  
  
  protected void setPrice(float base, Conversion materials) {
    this.basePrice = base / 5f;
    this.materials = materials;
    if (materials != null) for (Item i : materials.raw) {
      this.basePrice += i.defaultPrice();
    }
  }
  
  
  public static Traded loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public Conversion materials() {
    return materials;
  }
  
  
  public float basePrice() {
    return basePrice;
  }
  
  
  public boolean common() {
    return form == FORM_MATERIAL || form == FORM_PROVISION;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void describeHelp(Description d, Selectable prior) {
    d.append(description);
    final Base base = BaseUI.currentPlayed();
    if (base == null) return;

    final Batch <Blueprint>
      canMake = new Batch <Blueprint> (),
      canUse  = new Batch <Blueprint> ();
    for (Blueprint b : base.setup.available()) {
      if (b.category == UIConstants.TYPE_HIDDEN) continue;
      else if (b.producing(this) != null) canMake.include(b);
      else if (b.consuming(this) != null) canUse .include(b);
    }
    
    //if (canUse.size() > 0) {
      //d.append("\n\nThis commodity is used at:");
    //}
    
    d.append("\n");
    for (Blueprint b : canUse) {
      final Conversion c = b.consuming(this);
      d.append("\nUsed by ");
      d.append(b);
      if (c.out != null) { d.append(" to make "); d.append(c.out.type); }
    }
    
    for (Blueprint b : canMake) {
      final Conversion c = b.producing(this);
      if (c.raw.length == 0) {
        d.append("\nMade at ");
        d.append(b);
      }
      else {
        d.append("\nMade from ");
        for (Item i : c.raw) { d.append(i.type); d.append(" "); }
        d.append("at ");
        d.append(b);
      }
    }
  }
}











