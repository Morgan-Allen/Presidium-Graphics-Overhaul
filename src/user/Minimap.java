/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.common.* ;
import src.game.planet.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.graphics.widgets.UINode ;

//import org.lwjgl.opengl.* ;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.g2d.*;

//
//  TODO:  Have the minimap refresh itself every second or so, and simply fade
//  in the new version on top of the old?  Something like that.  If you wanted,
//  you could do some kind of fancy burn-in or flicker transition-effect.

//
//  TODO:  You need to be able to present the ambience map here.

//  TODO:  RESTORE THIS, AND POSSIBLY MOVE TO THE WIDGETS PACKAGE?

public class Minimap extends UINode {
  
  
  final static float
    FADE_DELAY = 1.0f ;
  
  final BaseUI UI ;
  final World world ;
  private Base base ;
  
  //private Texture mapImage ;
  
  
  
  public Minimap(BaseUI UI, World world, Base realm) {
    super(UI) ;
    this.UI = UI ;
    this.world = world ;
    this.base = realm  ;
  }
  
  
  private void updateMapImage() {
    /*
    final int texSize = world.size ;
    if (mapImage == null) mapImage = Texture.createTexture(texSize, texSize) ;
    byte RGBA[] = new byte[texSize * texSize * 4] ;
    for (int y = 0, m = 0 ; y < texSize ; y++) {
      for (int x = 0 ; x < texSize ; x++) {
        final Tile t = world.tileAt(x, y) ;
        final Colour avg = t.minimapHue() ;
        avg.storeByteValue(RGBA, m) ;
        m += 4 ;
        RGBA[m - 1] = (byte) 0xff ;
      }
    }
    mapImage.putBytes(RGBA) ;
    //*/
  }
  
  
  public void setBase(Base base) {
    this.base = base ;
  }
  
  
  public void updateAt(Tile t) {
    //final Colour avg = t.minimapHue() ;
    //mapImage.putColour(avg, t.x, t.y) ;
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    if (super.selectionAt(mousePos) == null) return null ;
    return (getMapPosition(mousePos) == null) ? null : this ;
  }
  
  
  protected void whenClicked() {
    //final Tile pos = getMapPosition(UI.mousePos()) ;
    //if (pos == null) return ;
    //UI.camera.lockOn(pos) ;
  }
  
  
  protected Tile getMapPosition(final Vector2 pos) {
    final float
      cX = (pos.x -  xpos()) / bounds.xdim(),
      cY = (pos.y - (ypos() + (bounds.ydim() * 0.5f))) / bounds.ydim() ;
    final Vec2D mapPos = new Vec2D(
      (cX - cY) * world.size,
      (cY + cX) * world.size
    ) ;
    return world.tileAt(mapPos.x, mapPos.y) ;
  }
  
  
  protected void render(SpriteBatch batch2D) {
    /*
    if (mapImage == null) updateMapImage() ;
    
    GL11.glColor4f(1, 1, 1, 1) ;
    mapImage.bindTex() ;
    renderTex(-1) ;
    if (base != null && ! GameSettings.fogFree) {
      GL11.glEnable(GL12.GL_TEXTURE_3D) ;
      FogOverlay fogOver = base.intelMap.fogOver() ;
      fogOver.bindAsTex() ;
      renderTex(fogOver.trueFadeVal()) ;
      GL11.glDisable(GL12.GL_TEXTURE_3D) ;
    }
    //*/
  }
  
  
  private void renderTex(float fadeVal) {
    //
    //You draw a diamond-shaped area around the four points-
    final float
      w = xdim(),
      h = ydim(),
      x = xpos(),
      y = ypos() ;
    /*
    GL11.glBegin(GL11.GL_QUADS) ;
    
    if (fadeVal == -1) GL11.glTexCoord2f(0, 0) ;
    else GL11.glTexCoord3f(0, 0, fadeVal) ;
    GL11.glVertex2f(x, y + (h / 2)) ;
    
    if (fadeVal == -1) GL11.glTexCoord2f(0, 1) ;
    else GL11.glTexCoord3f(0, 1, fadeVal) ;
    GL11.glVertex2f(x + (w / 2), y + h) ;
    
    if (fadeVal == -1) GL11.glTexCoord2f(1, 1) ;
    else GL11.glTexCoord3f(1, 1, fadeVal) ;
    GL11.glVertex2f(x + w, y + (h / 2)) ;
    
    if (fadeVal == -1) GL11.glTexCoord2f(1, 0) ;
    else GL11.glTexCoord3f(1, 0, fadeVal) ;
    GL11.glVertex2f(x + (w / 2), y) ;
    
    GL11.glEnd() ;
    //*/
  }
}

//*/


