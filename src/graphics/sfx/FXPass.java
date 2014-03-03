

package src.graphics.sfx;

import src.graphics.common.*;
import src.util.*;

import com.badlogic.gdx.* ;
import com.badlogic.gdx.graphics.* ;
//import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.utils.* ;
import com.badlogic.gdx.graphics.glutils.* ;



public class FXPass {
  

  final Rendering rendering;
  final Batch <SFX> inPass = new Batch <SFX> ();
  
  
  
  
  private Mesh compiled ;
  private float vertComp[] ;
  private short compIndex[] ;
  
  private int total = 0 ;
  private Texture lastTex = null ;
  private ShaderProgram shading ;
  
  
  public FXPass(Rendering rendering) {
    this.rendering = rendering;
  }
}

//  TODO:  Try using an ImmediateModeRenderer for the sake of simplicity-
//  http:// ...com/badlogic/gdx/graphics/glutils/ImmediateModeRenderer.html
//  http://www.java-gaming.org/index.php?topic=30430.0

/*
ImmediateModeRenderer r;

... create()
   if (Gdx.graphics.isGL20Available()) {
       //normals=false, colors=true, numTexCoords=none 
       r = new ImmediateModeRenderer20(false, true, 0); 
   } else {
       r = new ImmediateModeRenderer10();
   }


... render()
   //enable srcOver blending
   Gdx.gl.glEnable(GL10.GL_BLEND);
   Gdx.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

   //for example, this can use your OrthographicCamera
   r.begin(camera.combined, GL20.GL_TRIANGLE_FAN);

   ...

   //push our vertex data here...

   //.. similar to glColor4f
   r.color(0f, 0f, 0f, intensity);
   
   //.. similar to glVertex3f
   r.vertex(center.x, center.y, depth);
   
   ...

   //flush the renderer
   r.end();
//*/









