package src.graphics.solids;


import src.graphics.common.*;
import src.util.I;
import static src.graphics.common.GL.*;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;

import java.nio.*;

import org.lwjgl.BufferUtils;




public class GDXShader extends DefaultShader {
  
  Texture fogtex; // temporal, lol
  
  
  public GDXShader(Renderable renderable, Config config, Texture fog) {
    super(renderable, config);

    this.fogtex = fog;

    register("u_fogmap", new Setter() {
      @Override
      public void set(BaseShader shader, int inputID, Renderable renderable,
          Attributes combinedAttributes) {

        if (fogtex == null)
          return;
        final int unit = shader.context.textureBinder.bind(fogtex);
        // final int unit =
        // shader.context.textureBinder.bind(((FogMapAttribute)(combinedAttributes.get(FogMapAttribute.Fogmap))).getFogmap());
        shader.set(inputID, unit);
      }

      @Override
      public boolean isGlobal(BaseShader shader, int inputID) {
        return true;
      }
    });

    register("u_fogFlag", new Setter() {
      @Override
      public void set(BaseShader shader, int inputID, Renderable renderable,
          Attributes combinedAttributes) {
        // System.out.println("FOG TEX" + fagtex);
        shader.set(inputID, fogtex == null ? GL_FALSE : GL_TRUE);
      }

      @Override
      public boolean isGlobal(BaseShader shader, int inputID) {
        return true;
      }
    });
    //*/

    register("u_overlays", new Setter() {
      int[] units = new int[10];
      IntBuffer unitB = BufferUtils.createIntBuffer(10);

      @Override
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
        if (overnum > 8) I.complain("TOO MANY OVERLAYS!");
        
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

      @Override
      public boolean isGlobal(BaseShader shader, int inputID) {
        return false;
      }
    });
  }

}
