

package stratos.graphics.solids;
import stratos.graphics.common.*;
import stratos.util.*;
//import static stratos.graphics.common.GL.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.shaders.*;
//import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
//import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import java.nio.*;
import org.lwjgl.BufferUtils;




public class GDXShader extends DefaultShader {
  
  final int MAX_SKINS = 10;
  
  
  public GDXShader(Renderable renderable, Config config, Texture fog) {
    super(renderable, config);

    register("u_overlays", new Setter() {
      int[] units = new int[MAX_SKINS];
      IntBuffer unitB = BufferUtils.createIntBuffer(MAX_SKINS);
      
      public void set(
        BaseShader shader, int inputID, Renderable renderable,
        Attributes combinedAttributes
      ) {
        OverlayAttribute attribute = (OverlayAttribute) combinedAttributes
            .get(OverlayAttribute.Overlay);
        
        if (attribute == null || attribute.textures == null) {
          // System.out.println("doesnt have " + attr);
          shader.program.setUniformi("u_overnum", 0);
          return;
        }
        int overnum = attribute.textures.length;
        if (overnum > MAX_SKINS) I.complain("TOO MANY OVERLAYS!");
        
        for (int i = 0; i < overnum; ++i) {
          units[i] = shader.context.textureBinder.bind(attribute.textures[i]);
        }
        int location = shader.program.getUniformLocation("u_overlays");
        shader.program.setUniformi("u_overnum", overnum);
        
        unitB.rewind();
        unitB.put(units);
        unitB.rewind();
        GL.glUniform1iv(location, overnum, unitB);
      }
      
      public boolean isGlobal(BaseShader shader, int inputID) {
        return false;
      }
    });
  }
}


