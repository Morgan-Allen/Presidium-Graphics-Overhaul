/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.Image;
import stratos.user.*;
import stratos.util.*;



public class Faction extends Constant {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static Index <Faction> INDEX = new Index();
  
  
  private Faction parent;
  private VerseLocation startSite;
  private boolean primal;
  
  String startInfo;
  final ImageAsset crestImage;
  final Colour bannerColour;
  
  
  protected Faction(
    String name, String crestPath, Colour banner,
    boolean primal
  ) {
    super(INDEX, name, name);
    
    this.primal = primal;
    
    this.bannerColour = new Colour(banner == null ? Colour.WHITE : banner);
    this.crestImage = crestPath == null ?
      Image.SOLID_WHITE : ImageAsset.fromImage(Verse.class, crestPath)
    ;
  }
  
  
  protected Faction(String name, String crestPath, Faction parent) {
    this(name, crestPath, parent.bannerColour, parent.primal);
    this.parent = parent;
  }
  
  
  protected void bindToStartSite(VerseLocation site) {
    //
    //  Must be called separately to avoid initialisation loop...
    if (startSite == null) return;
    this.startSite = site;
  }
  
  
  public static Faction loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Political setup information-
    */
  public VerseLocation startSite() {
    return startSite;
  }
  
  
  public Faction parent() {
    return parent;
  }
  
  
  public boolean primal() {
    return primal;
  }
  
  
  
  
  
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeHelp(Description d, Selectable prior) {
    d.append(startInfo);
  }
  
  
  public Colour bannerColour() {
    return bannerColour;
  }
  
}








