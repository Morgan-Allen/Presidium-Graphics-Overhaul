
/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.util.*;
import stratos.graphics.widgets.Alphabet.Letter;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;



/**  A text object that wraps will continue onto subsequent lines when a given
  *  line is filled.  Non-wrapping text will adjust width to match current
  *  entries.
  *  NOTE:  Scaling does not apply to any inserted images.
  */
public class Text extends UINode implements Description {
  
  
  private static boolean verbose = false;
  private static Colour tint = new Colour();
  
  final public static Colour
    LINK_COLOUR  = new Colour().set(0.2f, 0.6f, 0.8f, 1),
    HOVER_COLOUR = new Colour().set(1.0f, 1.0f, 0.0f, 1);
  
  
  
  /**  Essential component classes and interfaces- clickables can be used to
    *  navigate to other objects, with image and text entries provide
    *  information or emphasis-
    */
  public static interface Clickable {
    String fullName();
    void whenClicked();
  }
  
  
  static class UIEntry extends Box2D {
    UINode graphic;
    boolean visible;
    int wide, high;
    boolean bullet;
  }
  
  
  static class TextEntry extends Box2D implements Pool.Poolable {
    char key;
    Letter letter;
    boolean visible;
    Colour colour = null;
    Clickable link = null;
    
    public void reset() {
      letter = null;
      colour = null;
      link = null;
      visible = true;
      key = ' ';
      super.set(0, 0, 0, 0);
    }
  }
  
  
  
  final protected Alphabet alphabet;
  public float scale = 1.0f;
  protected List <Box2D  > allEntries = new List <Box2D  > ();
  protected List <UIEntry> allNodes   = new List <UIEntry> ();
  private boolean needsFormat = false;
  
  private Scrollbar scrollbar;
  private Box2D scrolled = new Box2D();
  private Box2D fullSize = new Box2D();
  private float oldWide = 0, oldHigh = 0;
  
  
  
  public Text(HUD UI, Alphabet a) {
    this(UI, a, "");
  }
  
  
  public Text(HUD myHUD, Alphabet a, String s) {
    super(myHUD);
    alphabet = a;
    setText(s);
  }
  
  
  public Scrollbar makeScrollBar(ImageAsset tex) {
    return this.scrollbar = new Scrollbar(UI, tex, this);
  }
  
  
  protected Box2D scrolledArea() {
    return scrolled;
  }
  
  
  protected Box2D fullTextArea() {
    return fullSize;
  }
  
  
  
  /**  Adds the given String to this text object in association with
    *  the specified selectable.
    */
  public void append(String s, Clickable link, Colour c) {
    if (s == null) s = "(none)";
    for (int n = 0, l = s.length(); n < l; n++)
      addEntry(s.charAt(n), link, c) ;
  }
  
  
  public void append(Clickable l, Colour c) {
    if (l == null) append("(none)");
    else append(l.fullName(), l, c);
  }
  
  
  public void append(Clickable l) {
    if (l == null) append("(none)");
    else append(l.fullName(), l, LINK_COLOUR);
  }
  
  
  public void append(Object o) {
    if (o instanceof Clickable) append((Clickable) o);
    else if (o != null) append(o.toString());
    else append("(none)");
  }
  
  
  public void appendList(String s, Object... l) {
    if (l.length == 0) return;
    append(s);
    int i = 0; for (Object o : l) {
      append(o);
      if (++i < l.length) append(", ");
    }
  }
  
  
  public void appendList(String s, Series l) {
    appendList(s, l.toArray());
  }
  
  
  public void append(String s, Clickable l) { append(s, l, LINK_COLOUR); }
  public void append(String s, Colour c) { append(s, null, c); }
  public void append(String s) { append(s, null, null); }
  

  boolean addEntry(char k, Clickable links, Colour c) {
    Letter l = null;
    if (((l = alphabet.map[k]) == null) && (k != '\n')) return false;
    final TextEntry entry = new TextEntry();
    entry.key = k;
    entry.letter = l;
    entry.colour = c;
    entry.link = links;
    allEntries.addLast(entry);
    needsFormat = true;
    return true;
  }
  
  
  public void setText(String s) {
    allEntries.clear();
    append(s, null, null);
    needsFormat = true;
  }
  
  
  public String getText() {
    int n = 0;
    char charS[] = new char[allEntries.size()];
    for (Box2D entry : allEntries) {
      if (entry instanceof TextEntry)
        charS[n++] = ((TextEntry) entry).key;
      else
        charS[n++] = '*';
    }
    return new String(charS);
  }
  
  
  
  /**  Adds a single image entry to this text object.  Image entries can either
    *  function as hyperlinks given the right reference object, or as bullets
    *  to indent other elements, or both.
    */
  
  //  TODO:  Consider making these static methods, so that arbitrary
  //  Descriptions can be passed in (and simply ignored if they're not Texts.)
  
  
  public boolean insert(
    Texture texGraphic, int wide, int high,
    final Clickable link, boolean asBullet
  ) {
    final Button linked = new Button(UI, texGraphic, link.fullName());
    linked.setLinks(link);
    return insert(linked, wide, high, asBullet);
  }
  
  
  public boolean insert(
    Texture texGraphic, int wide, int high, boolean asBullet
  ) {
    return insert(new Image(UI, texGraphic), wide, high, asBullet);
  }
  
  
  public boolean insert(UINode graphic, int wide, int high, boolean asBullet) {
    if (graphic == null) return false;
    if (asBullet && allEntries.size() > 0) append("\n");
    
    graphic.absBound.set(0, 0, wide, high);
    graphic.relBound.set(0, 0, 0, 0);
    graphic.updateRelativeParent();
    graphic.updateAbsoluteBounds();
    graphic.attachTo(this);
    
    final UIEntry entry = new UIEntry();
    entry.graphic = graphic;
    entry.wide = (int) graphic.xdim();
    entry.high = (int) graphic.ydim();
    entry.bullet = asBullet;
    
    allEntries.add(entry);
    needsFormat = true;
    return true;
  }
  
  
  public void cancelBullet() {
    insert(ImageAsset.WHITE_TEX(), 0, 0, true);
  }
  
  
  
  /**  Various overrides of UINode functionality-
    */
  protected void updateState() {
    super.updateState();
    
    this.scrolled.set(0, 0, xdim(), ydim());
    if (scrollbar != null) {
      final float down = 1 - scrollbar.scrollPos();
      scrolled.ypos(0 - (fullSize.ydim() - ydim()) * down);
    }
    
    allNodes.clear();
    for (Box2D entry : allEntries) if (entry instanceof UIEntry) {
      final UIEntry node = (UIEntry) entry;
      allNodes.add(node);
      node.graphic.updateState();
      node.graphic.relAlpha = this.absAlpha;
    }
  }
  
  
  protected void updateRelativeParent() {
    super.updateRelativeParent();
    
    if ((oldWide != xdim()) || (oldHigh != ydim())) {
      needsFormat = true;
      oldWide = xdim();
      oldHigh = ydim();
    }
    if (needsFormat && (allEntries.size() > 0)) format(xdim());
    
    for (UIEntry node : allNodes) {
      final Box2D b = node.graphic.absBound;
      b.xpos(node.xpos() - scrolled.xpos());
      b.ypos(node.ypos() - scrolled.ypos());
      node.graphic.updateRelativeParent();
    }
  }
  
  
  protected void updateAbsoluteBounds() {
    super.updateAbsoluteBounds();
    for (UIEntry node : allNodes) node.graphic.updateAbsoluteBounds();
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    if (! trueBounds().contains(mousePos.x, mousePos.y)) return null;

    final Clickable link = getTextSelection(UI.mousePos(), scrolled);
    if (link != null && UI.mouseClicked()) whenLinkClicked(link);
    
    for (UIEntry node : allNodes) {
      final UINode match = node.graphic.selectionAt(mousePos);
      if (match != null) return match;
    }
    return super.selectionAt(mousePos);
  }
  
  
  protected void render(WidgetsPass pass) {
    if (allEntries.size() == 0) return;
    final Object link = getTextSelection(UI.mousePos(), scrolled);
    
    //  Then we begin the rendering pass.  In order to accomodate scissor
    //  culling, we flush the pipeline of existing elements before and after,
    //  and set the bounds to fit.
    pass.flush();
    Gdx.gl.glEnable(GL11.GL_SCISSOR_TEST);
    Gdx.gl.glScissor((int) xpos(), (int) ypos(), (int) xdim(), (int) ydim());
    
    for (Box2D entry : allEntries) if (entry instanceof TextEntry) {
      renderText(scrolled, (TextEntry) entry, link, pass);
    }
    for (UIEntry entry : allNodes) if (entry.intersects(scrolled)) {
      entry.graphic.render(pass);
    }
    
    pass.flush();
    Gdx.gl.glDisable(GL11.GL_SCISSOR_TEST);
  }
  
  
  public String toString() {
    return "Info Pane";
  }
  
  
  protected void whenLinkClicked(Clickable link) {
    link.whenClicked();
  }
  
  
  protected Clickable getTextSelection(Vector2 mousePos, Box2D scrolled) {
    final float
      mX = mousePos.x + scrolled.xpos() - this.xpos(),
      mY = mousePos.y + scrolled.ypos() - this.ypos();
    if (! scrolled.contains(mX, mY)) return null;
    
    for (Box2D entry : allEntries) {
      if (! entry.overlaps(scrolled)) continue;
      if (! entry.contains(mX, mY  )) continue;
      if (entry instanceof TextEntry) return ((TextEntry) entry).link;
    }
    return null;
  }
  

  protected boolean renderText(
    Box2D scrolled, TextEntry entry, Object link, WidgetsPass pass
  ) {
    if (entry.letter == null || ! entry.intersects(scrolled)) return false;
    //
    //  If this text links to something, we may need to colour the text (and
    //  possibly select it's link target if clicked.)
    if (link != null && entry.link == link) {
      tint.set(HOVER_COLOUR);
      tint.a = absAlpha;
      tint.calcFloatBits();
    }
    else {
      final Colour c = entry.colour != null ? entry.colour : Colour.WHITE;
      tint.set(c.r, c.g, c.b, c.a * absAlpha);
    }
    //
    //  Draw the text entry-
    pass.draw(
      alphabet.fontTex, tint,
      entry.xpos() + xpos() - scrolled.xpos(),
      entry.ypos() + ypos() - scrolled.ypos(),
      entry.xdim(), entry.ydim(),
      entry.letter.umin, entry.letter.vmax,
      entry.letter.umax, entry.letter.vmin
    );
    return true;
  }
  
  
  
  /**  Sets this text object to the size it would ideally prefer in order to
    *  accomodate it's text.
    */
  public void setToPreferredSize(float maxWidth) {
    format(maxWidth);
    relBound.xdim(0);
    relBound.ydim(0);
    absBound.xdim(fullSize.xdim());
    absBound.ydim(fullSize.ydim());
  }
  
  
  public Box2D preferredSize() {
    return fullSize;
  }
  
  
  
  /**  Puts all letters in their proper place, allowing for roll/wrap/grow
    *  effects, and, if neccesary, adjusts the bounds of this UIObject
    *  accordingly.
    */
  protected void format(float maxWidth) {
    
    final boolean report = verbose;
    if (report) I.say("\nFormatting text, max. width: "+maxWidth+"\n");
    
    boolean
      newWord,
      newLine;
    Box2D
      bullet = null;
    float
      across     = 0,
      down       = 0,
      lineHigh   = 0;
    final Batch <Box2D>
      lastLine = new Batch <Box2D> (),
      lastWord = new Batch <Box2D> ();
    
    for (Box2D box : allEntries) {
      newLine = newWord = false;
      box.set(0, 0, 0, 0);
      
      if (box instanceof UIEntry) {
        final UIEntry entry = (UIEntry) box;
        entry.xdim(entry.wide);
        entry.ydim(entry.high);
        
        if (entry.bullet) {
          if (bullet != null) down = Nums.min(down, bullet.ypos());
          bullet = entry;
        }
        if (report) I.say("  UI entry: "+entry.graphic+", across: "+across);
      }
      
      else if (box instanceof TextEntry) {
        final TextEntry entry = (TextEntry) box;
        final char key = entry.key;
        if (key == '\n') newLine = newWord = true;
        if (key == ' ' ) newWord = true;
        if (entry.letter != null) {
          entry.xdim(entry.letter.width  * scale);
          entry.ydim(entry.letter.height * scale);
        }
        if (report) I.say("  Text entry: "+key+", across: "+across);
      }
      else continue;
      
      lastWord.add(box);
      across += box.xdim();
      if ((maxWidth > 0) && (across > maxWidth)) newLine = true;
      
      if (newWord) {
        Visit.appendTo(lastLine, lastWord);
        lastWord.clear();
      }
      
      if (newLine) {
        lineHigh = formatLine(lastLine, 0, down, bullet);
        down -= lineHigh;
        across = 0;
        for (Box2D entry : lastWord) across += entry.xdim();
        lastLine.clear();
      }
    }
    //
    //  Clean up any remainders-
    if (lastWord.size() > 0) Visit.appendTo(lastLine, lastWord);
    if (lastLine.size() > 0) formatLine(lastLine, 0, down, bullet);
    //
    //  We now reposition entries to fit the window, and update the full bounds.
    fullSize.set(0, 0, 0, 0);
    final float heightAdjust = ydim();
    for (Box2D entry : allEntries) {
      fullSize.include(entry);
      entry.incY(heightAdjust);
    }
    fullSize.clipToMultiple(1);
    needsFormat = false;
  }
  
  
  private float formatLine(
    Batch <Box2D> entries, float minX, float minY, Box2D lastBullet
  ) {
    
    final boolean report = verbose;
    final Box2D head = entries.first();
    if (head == null) return 0;
    final boolean bulleted = head == lastBullet;
    
    float across = lastBullet == null ? 0 : lastBullet.xdim();
    float down   = alphabet.letterFor(' ').height * scale;
    for (Box2D entry : entries) {
      if (bulleted && entry == head) continue;
      down = Nums.max(down, entry.ymax());
    }
    if (bulleted) {
      head.ypos(down - head.ydim());
      across -= head.xdim();
    }
    
    if (report) {
      I.say("\nFormatting string, size: "+entries.size());
      I.say("Starting from: "+minX+"/"+minY);
      I.say("Line height:   "+down);
      I.say("Bulleted?      "+bulleted);
    }
    
    for (Box2D entry : entries) {
      entry.incX(minX + across);
      entry.incY(minY - down  );
      across += entry.xdim();
      
      if (report && entry instanceof TextEntry) {
        final char key = ((TextEntry) entry).key;
        I.say("  Key: "+key+" across: "+across);
      }
    }
    if (report) I.say("");
    return down;
  }
}





