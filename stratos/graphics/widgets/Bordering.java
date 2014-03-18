/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets ;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;



public class Bordering extends UINode {
  
  
  final Texture borderTex ;
  final public Box2D
    insetUV = new Box2D().set(0.33f, 0.33f, 0.33f, 0.33f),
    drawInset = new Box2D().set(10, 10, -20, -20) ;
  
  
  public Bordering(HUD UI, String texFile) {
    this(UI, ImageAsset.getTexture(texFile));
  }
  
  
  public Bordering(HUD UI, Texture tex) {
    super(UI);
    this.borderTex = tex;
  }
  
  
  protected void render(SpriteBatch batch2D) {
    final float
      coordX[] = {
        0, drawInset.xpos(),
        this.xdim() + drawInset.xmax(), this.xdim()
      },
      coordY[] = {
        0, drawInset.ypos(),
        this.ydim() + drawInset.ymax(), this.ydim()
      },
      coordU[] = {
        0, insetUV.xpos(),
        insetUV.xmax(), 1
      },
      coordV[] = {
        0, insetUV.ypos(),
        insetUV.ymax(), 1
      } ;
    for (int i = 4 ; i-- > 0 ;) {
      coordX[i] += xpos() ;
      coordY[i] = ypos() + ydim() - coordY[i] ;
    }
    
    batch2D.setColor(1, 1, 1, 1);
    for (int x = 3 ; x-- > 0 ;) for (int y = 3 ; y-- > 0 ;) {
      batch2D.draw(
        borderTex,
        coordX[x], coordY[y],
        coordX[x + 1] - coordX[x], coordY[y + 1] - coordY[y],
        coordU[x], coordV[y], coordU[x + 1], coordV[y + 1]
      );
    }
  }
}

