/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.sfx;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.util.*;
import java.io.*;



public class TalkFX extends SFX {
  
  
  
  /**  Field definitions, constants and constructors-
    */
  final public static ModelAsset TALK_MODEL = new ClassModel(
    "talk_fx_model", TalkFX.class
  ) {
    public Sprite makeSprite() { return new TalkFX(); }
  };
  final static ImageAsset BUBBLE_TEX = ImageAsset.fromImage(
    TalkFX.class, "media/GUI/textBubble.png"
  );
  final static Alphabet FONT = Alphabet.loadAlphabet(
    "media/GUI/", "FontVerdana.xml"
  );
  
  final public static int
    MAX_LINES = 3,
    NOT_SPOKEN = 0,
    FROM_LEFT  = 1,
    FROM_RIGHT = 2;
  final static float
    FADE_TIME  = 0.5f,
    FADE_RATE  = 1f / (Rendering.FRAMES_PER_SECOND * FADE_TIME);
  
  public float fadeRate = 1.0f;
  final float LINE_HIGH, LINE_SPACE;
  final Stack <Bubble> toShow  = new Stack <Bubble> ();
  final Stack <Bubble> showing = new Stack <Bubble> ();
  
  
  public TalkFX() {
    super(PRIORITY_FIRST);
    LINE_HIGH = FONT.letterFor(' ').height;
    LINE_SPACE = LINE_HIGH + 10;
  }
  
  
  public ModelAsset model() {
    return TALK_MODEL;
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    out.writeInt(toShow .size());
    out.writeInt(showing.size());
    final Batch <Bubble> all = new Batch <Bubble> ();
    for (Bubble b : toShow ) all.add(b);
    for (Bubble b : showing) all.add(b);
    for (Bubble b : all) {
      Assets.writeString(out, b.phrase);
      out.writeInt  (b.type   );
      out.writeFloat(b.width  );
      out.writeFloat(b.xoff   );
      out.writeFloat(b.yoff   );
      out.writeFloat(b.addTime);
    }
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    final int numT = in.readInt(), numS = in.readInt();
    for (int n = 0; n < numT + numS; n++) {
      final Bubble b = new Bubble();
      b.phrase  = Assets.readString(in);
      b.type    = in.readInt();
      b.width   = in.readFloat();
      b.xoff    = in.readFloat();
      b.yoff    = in.readFloat();
      b.addTime = in.readFloat();
      if (n < numT) toShow.add(b);
      else showing.add(b);
    }
  }
  
  
  
  /**  Updates and modifications-
    */
  static class Bubble {
    
    String phrase;
    int type;
    
    float width;
    float xoff, yoff;
    float addTime;
    
    float alpha() {
      float alpha = FADE_TIME + addTime - Rendering.activeTime();
      alpha /= FADE_TIME;
      return alpha;
    }
  }
  
  
  public void addPhrase(String phrase) {
    addPhrase(phrase, NOT_SPOKEN);
  }
  
  
  public void addPhrase(String phrase, int bubbleType) {
    final Bubble b = new Bubble();
    if (! PlayLoop.rendering().view.intersects(position, 1)) return;
    
    if (phrase == null) I.complain("\nCANNOT ADD NULL STRING AS PHRASE!");
    b.phrase  = phrase;
    b.type    = bubbleType;
    toShow.add(b);
  }
  
  
  public void readyFor(Rendering rendering) {
    //
    //  If there are bubbles awaiting display, see if you can move the existing
    //  bubbles up to make room.
    final Bubble first = showing.first(); 
    
    final boolean
      shouldMove = toShow.size() > 0,
      canMove    = showing.size() == 0 || first.alpha() <= 1,
      isSpace    = showing.size() == 0 || first.yoff >= LINE_SPACE;
    
    if (shouldMove && canMove) {
      if (isSpace) {
        showBubble(toShow.removeFirst());
      }
      else for (Bubble b : showing) {
        b.yoff += FADE_RATE * fadeRate * LINE_SPACE;
      }
    }
    //
    //  In either case, gradually fate out existing bubbles-
    for (Bubble b : showing) if (b.alpha() <= 0) showing.remove(b);
    super.readyFor(rendering);
  }
  
  
  private void showBubble(Bubble b) {
    final float fontScale = 1;
    float width = Label.phraseWidth(b.phrase, FONT, fontScale);
    //
    //  You also need to either left or right justify, depending on the bubble
    //  type.
    b.width = width;
    b.yoff = 5;
    if (b.type == NOT_SPOKEN) b.xoff = width / -2;
    if (b.type == FROM_LEFT ) b.xoff = width / -2;
    if (b.type == FROM_RIGHT) b.xoff = width / -2;
    b.addTime = Rendering.activeTime();
    showing.addFirst(b);
  }
  
  
  public int numPhrases() {
    return showing.size() + toShow.size();
  }
  
  
  
  /**  Rendering methods-
    */
  protected void renderInPass(SFXPass pass) {
    if (showing.size() == 0) return;
    
    final Vec3D flatPoint = new Vec3D(position);
    pass.rendering.view.translateToScreen(flatPoint);
    final float fontScale = LINE_HIGH / FONT.letterFor(' ').height;
    
    for (Bubble bubble : showing) if (bubble.type != NOT_SPOKEN) {
      renderBubble(pass, bubble, flatPoint, bubble.type == FROM_RIGHT);
    }
    
    for (Bubble bubble : showing) {
      final boolean speaks = bubble.type != NOT_SPOKEN;
      final Colour c = new Colour(Colour.WHITE);
      float alpha = bubble.alpha();
      if (this.colour != null) alpha *= this.colour.a;
      
      if (speaks) c.blend(Colour.BLUE, 0.33f);
      c.blend(Colour.BLACK, fog);
      c.a = alpha;
      
      Label.renderPhrase(
        bubble.phrase, FONT, fontScale, c,
        flatPoint.x + bubble.xoff,
        flatPoint.y + bubble.yoff,
        flatPoint.z + 0.05f,
        pass, true
      );
    }
  }
  
  
  //  TODO:  Unify this code with the Border-display routines.
  private void renderBubble(
    SFXPass pass, Bubble bubble,
    Vec3D flatPoint, boolean fromRight
  ) {
    //
    //  Some of this could be moved to the constants section-
    final float
      x = flatPoint.x + bubble.xoff,
      y = flatPoint.y + bubble.yoff,
      //
      //  In the case of bubble from the right, we just flip U values-
      MIN_U = fromRight ? 1 : 0,
      MAX_U = fromRight ? 0 : 1,
      CAP_LU = fromRight ? 0.75f : 0.25f,
      CAP_RU = fromRight ? 0.25f : 0.75f,
      BOT_V = 0,
      TOP_V = 1,//BUBBLE_TEX.maxV(),
      
      //pad = 5,
      texHigh = (LINE_HIGH + 10) * 1.5f,
      minY = y - (5 + (texHigh / 3)),
      maxY = minY + texHigh,
      
      texWide = 128 * 40f / texHigh,  //True width/height for the texture.
      minX = x - 10,
      maxX = x + Nums.max(bubble.width, 64) + 10,
      capXL = minX + (texWide * 0.25f),
      capXR = maxX - (texWide * 0.25f);
    //
    //  Render the three segments of the bubble-
    final Colour colour = Colour.transparency(bubble.alpha());
    pass.compileQuad(
      BUBBLE_TEX.asTexture(), colour,
      false, minX, minY, capXL - minX,
      maxY - minY, MIN_U, BOT_V, CAP_LU,
      TOP_V, flatPoint.z, true
    );
    pass.compileQuad(
      BUBBLE_TEX.asTexture(), colour,
      false, capXL, minY, capXR - capXL,
      maxY - minY, CAP_LU, BOT_V, CAP_RU,
      TOP_V, flatPoint.z, true
    );
    pass.compileQuad(
      BUBBLE_TEX.asTexture(), colour,
      false, capXR, minY, maxX - capXR,
      maxY - minY, CAP_RU, BOT_V, MAX_U,
      TOP_V, flatPoint.z, true
    );
  }
}






