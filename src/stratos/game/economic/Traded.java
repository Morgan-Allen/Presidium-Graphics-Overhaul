


package stratos.game.economic;
import stratos.game.common.*;
import stratos.graphics.common.ImageAsset;
import stratos.graphics.cutout.*;
import stratos.util.*;



/**  Used to represent the types of goods and services that venues can provide
  *  or produce.
  */
public class Traded extends Index.Entry implements Session.Saveable {
  
  
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
    super(INDEX, name);
    
    this.form = form;
    this.name = name;
    this.description = description;
    
    this.basePrice = basePrice / GameSettings.SPEND_DIVISOR;
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
    return INDEX.loadFromEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public Conversion materials() { return materials; }
  public float basePrice() { return basePrice; }
  
  
  
  public String toString() { return name; }
}









