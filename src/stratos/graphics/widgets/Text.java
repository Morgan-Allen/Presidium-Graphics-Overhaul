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
  */
public class Text extends UINode implements Description {
  
  final public static Colour LINK_COLOUR = new Colour().set(
    0.2f, 0.6f, 0.8f, 1
  );
  
  
  public float scale = 1.0f;
  
  final protected Alphabet alphabet;
  private boolean needsFormat = false;
  private Scrollbar scrollbar;
  
  protected List <Box2D> allEntries = new List <Box2D> ();
  private Box2D fullSize = new Box2D();
  private float oldWide, oldHigh = 0;
  
  final Pool <TextEntry> letterPool = new Pool <TextEntry> (1000) {
    protected TextEntry newObject() {
      return new TextEntry();
    }
  };
  
  
  
  public Text(HUD UI, Alphabet a) {
    this(UI, a, "");
  }
  
  
  public Text(HUD myHUD, Alphabet a, String s) {
    super(myHUD);
    alphabet = a;
    setText(s);
  }
  
  
  public Scrollbar makeScrollBar(ImageAsset tex) {
    return this.scrollbar = new Scrollbar(UI, tex, fullSize);
  }
  
  
  public void continueWrap(Text continues) {
    if (needsFormat) format(xdim());
    final Box2D textArea = new Box2D().set(0, 0, xdim(), ydim());
    
    continues.setText("");
    boolean spilt = false;
    for (ListEntry LE = allEntries;;) {
      if ((LE = LE.nextEntry()) == allEntries) break;
      
      if ((! spilt) && LE.refers instanceof TextEntry) {
        final TextEntry TE = (TextEntry) LE.refers;
        if (TE.containedBy(textArea) || ! TE.visible) continue;
        if (Character.isWhitespace(TE.key)) continue;
        spilt = true;
      }
      
      if ((! spilt) && LE.refers instanceof ImageEntry) {
        final ImageEntry IE = (ImageEntry) LE.refers;
        if (IE.containedBy(textArea)) continue;
        spilt = true;
      }
      
      if (spilt) {
        final Box2D b = (Box2D) LE.refers;
        b.set(0, 0, 0, 0);
        continues.allEntries.add(b); 
        LE.delete();
      }
    }
    
    continues.needsFormat = true;
  }
  
  
  
  /**  Essential component classes and interfaces- clickables can be used to
    *  navigate to other objects, with image and text entries provide
    *  information or emphasis-
    */
  public static interface Clickable {
    String fullName();
    void whenTextClicked();
  }
  
  
  static class ImageEntry extends Box2D {
    UINode graphic;
    boolean visible;
    int wide, high;
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
  
  
  
  /**  Various overrides of UINode functionality-
    */
  protected void updateAbsoluteBounds() {
    super.updateAbsoluteBounds();
    if ((oldWide != xdim()) || (oldHigh != ydim())) {
      needsFormat = true;
      oldWide = xdim();
      oldHigh = ydim();
    }
    if (needsFormat && (allEntries.size() > 0)) format(xdim());
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
  
  
  public void append(String s, Clickable l) { append(s, l, LINK_COLOUR); }
  public void append(String s, Colour c) { append(s, null, c); }
  public void append(String s) { append(s, null, null); }
  
  
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
  
  
  
  /**  Adds a single image entry to this text object.  Images are used as
    *  'bullets' to indent and separate text, and this needsFormat is retained until
    *  the next carriage return or another image is inserted.
    */
  public boolean insert(Texture texGraphic, int maxSize) {
    return insert(new Image(UI, texGraphic), maxSize);
  }
  
  
  public boolean insert(UINode graphic, int maxSize) {
    if (graphic == null) return false;
    graphic.absBound.set(0, 0, maxSize, maxSize);
    graphic.relBound.set(0, 0, 0, 0);
    graphic.updateRelativeParent();
    graphic.updateAbsoluteBounds();
    final ImageEntry entry = new ImageEntry();
    entry.graphic = graphic;
    entry.wide = (int) graphic.xdim();
    entry.high = (int) graphic.ydim();
    allEntries.add(entry);
    needsFormat = true;
    return true;
  }
  
  
  public void cancelBullet() {
    //  TODO:  Get rid of the indent effect associated with the last image?
  }
  
  
  
  /**  Adds a single letter entry to this text object.
    */
  boolean addEntry(char k, Clickable links, Colour c) {
    Letter l = null;
    if (((l = alphabet.map[k]) == null) && (k != '\n')) return false;
    final TextEntry entry = letterPool.obtain();
    entry.key = k;
    entry.letter = l;
    entry.colour = c;
    entry.link = links;
    allEntries.addLast(entry);
    needsFormat = true;
    return true;
  }
  
  
  
  /**  Sets this text object to the given string.
    */
  public void setText(String s) {
    for (Object e : allEntries) {
      if (e instanceof TextEntry) letterPool.free((TextEntry) e);
    }
    allEntries.clear();
    append(s, null, null);
    needsFormat = true;
  }
  
  
  
  /**  Gets the string this text object contains.
    */
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
  
  
  
  /**  Returns the selectable associated with the currently hovered unit of
    *  text.
    */
  protected void render(WidgetsPass pass) {
    if (allEntries.size() == 0) return;
    final Box2D textArea = new Box2D().set(0, 0, xdim(), ydim());
    
    //  We offset text-area position based on the scrollbar.
    if (scrollbar != null) {
      final float down = 1 - scrollbar.scrollPos();
      //I.say("Scrolled down: "+down+", pane height: "+ydim());
      //I.say("  Full height: "+fullSize.ydim());
      textArea.ypos(0 - (fullSize.ydim() - ydim()) * down);
    }
    final List <ImageEntry> bullets = new List <ImageEntry> ();
    final Clickable link = getTextLink(UI.mousePos(), textArea);
    
    //  Then we begin the rendering pass.  In order to accomodate scissor
    //  culling, we flush the pipeline of existing elements before and after,
    //  and set the bounds to fit.
    pass.flush();
    Gdx.gl.glEnable(GL11.GL_SCISSOR_TEST);
    Gdx.gl.glScissor((int) xpos(), (int) ypos(), (int) xdim(), (int) ydim());
    for (Box2D entry : allEntries) {
      if (entry instanceof TextEntry) {
        renderText(textArea, (TextEntry) entry, link, pass);
      }
      else bullets.add((ImageEntry) entry);
    }
    for (ImageEntry entry : bullets) {
      renderImage(textArea, entry, pass);
    }
    pass.flush();
    Gdx.gl.glDisable(GL11.GL_SCISSOR_TEST);
    
    if (UI.mouseClicked() && link != null) whenLinkClicked(link);
  }
  
  
  protected Clickable getTextLink(Vector2 mousePos, Box2D textArea) {
    if (UI.selected() != this) return null;
    final float
      mX = mousePos.x + textArea.xpos() - xpos(),
      mY = mousePos.y + textArea.ypos() - ypos();
    if (! textArea.contains(mX, mY)) return null;
    Box2D box = new Box2D();
    for (Box2D entry : allEntries) {
      box.set(
        entry.xpos() - 1, entry.ypos() - 1,
        entry.xdim() + 2, entry.ydim() + 2
      );
      if (box.contains(mX, mY) && entry.containedBy(textArea)) {
        if (entry instanceof TextEntry) return ((TextEntry) entry).link;
        return null;
      }
    }
    return null;
  }
  
  
  protected void whenLinkClicked(Clickable link) {
    link.whenTextClicked();
  }
  
  
  protected void renderImage(
    Box2D bounds, ImageEntry entry, WidgetsPass pass
  ) {
    if (! entry.intersects(bounds)) return;
    final Box2D b = entry.graphic.absBound;
    entry.graphic.relAlpha = this.absAlpha;
    b.xpos(entry.xpos() + xpos() - bounds.xpos());
    b.ypos(entry.ypos() + ypos() - bounds.ypos());
    entry.graphic.updateState();
    entry.graphic.updateRelativeParent();
    entry.graphic.updateAbsoluteBounds();
    
    entry.graphic.render(pass);
  }
  

  protected boolean renderText(
    Box2D area, TextEntry entry, Clickable link, WidgetsPass pass
  ) {
    if (entry.letter == null || ! entry.intersects(area)) return false;
    //
    //  If this text links to something, we may need to colour the text (and
    //  possibly select it's link target if clicked.)
    if (link != null && entry.link == link) {
      pass.setColor(1, 1, 0, absAlpha);
    }
    else {
      final Colour c = entry.colour != null ? entry.colour : Colour.WHITE;
      pass.setColor(c.r, c.g, c.b, c.a * absAlpha);
    }
    final float xoff = xpos() - area.xpos(), yoff = ypos() - area.ypos();
    //
    //  Draw the text entry-
    pass.draw(
      alphabet.fontTex,
      entry.xpos() + xoff, entry.ypos() + yoff,
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
    ListEntry <Box2D>
      open = allEntries,
      wordOpen = open,
      lineOpen = open;
    ImageEntry
      lastBullet = null;
    boolean
      newWord,
      newLine;
    float
      xpos = 0,
      ypos = 0;
    final float
      lineHigh = alphabet.map[' '].height * scale,
      charWide = alphabet.map[' '].width  * scale;
    //
    //  Here's the main loop for determining entry positions...
    while ((open = open.nextEntry()) != allEntries) {
      newLine = newWord = false;
      //
      //  In the case of an image entry...
      if (open.refers instanceof ImageEntry) {
        if (lastBullet != null) {
          final float minY = lastBullet.ypos() - (lineHigh * 1.5f);
          if (ypos > minY) ypos = minY;
        }
        final ImageEntry entry = (ImageEntry) open.refers;
        entry.visible = true;
        entry.set(
          0, ypos + lineHigh - (entry.high),
          entry.wide * scale, entry.high
        );
        xpos = entry.wide;
        lastBullet = entry;
      }
      //
      //  In the case of a text entry...
      else {
        final TextEntry entry = (TextEntry) open.refers;
        entry.visible = true;
        switch (entry.key) {
          //
          //  In the case of a return character, you definitely need a new line,
          //  and you automatically escape from the last bullet.
          //  Either that or a space means a new word.
          case('\n'):
            newLine = newWord = true;
            entry.visible = false;
          break;
          case(' '):
            newWord = true;
          default:
            entry.set(
              xpos, ypos,
              entry.letter.width  * scale,
              entry.letter.height * scale
            );
            xpos += entry.xdim();
            //
            //  If the width of the current line exceeds the space allowed, you
            //  need to go back to that start of the current word and jump to
            //  the next line.
            if ((maxWidth > 0) && (xpos > maxWidth) && (wordOpen != lineOpen)) {
              newLine = true;
            }
        }
      }
      //
      //  Mark the first character of a new word so you can escape back to it.
      //  If a new line is called for, flow around the current bullet.
      if (newWord) wordOpen = open;
      if (newLine) {
        xpos = (lastBullet == null) ? 0 : (lastBullet.wide + charWide);
        ypos -= lineHigh;
        open = lineOpen = wordOpen;
      }
    }
    //
    //  We now reposition entries to fit the window, and update the full bounds.
    fullSize.set(0, 0, 0, lineHigh);
    final float heightAdjust = ydim() - lineHigh;
    for (Box2D entry : allEntries) {
      fullSize.include(entry);
      entry.ypos(entry.ypos() + heightAdjust);
    }
    needsFormat = false;
  }
}





/*
final public static Alphabet INFO_FONT = new Alphabet(
  "UI/", "FontVerdana.png", "FontVerdana.png",
  "FontVerdana.map", 8, 16
);
//*/