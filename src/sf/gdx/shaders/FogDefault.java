package sf.gdx.shaders;

import static gl.GL.*;
import sf.gdx.SFMain;

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

public class FogDefault extends DefaultShader{
	
	Texture fagtex; // temporal, lol
	
	public FogDefault(Renderable renderable, Config config, Texture fog) {
		super(renderable, config);
		
		this.fagtex = fog;
		
		register("u_fogmap", new Setter() {
			@Override
			public void set(BaseShader shader, int inputID, Renderable renderable,
					Attributes combinedAttributes) {
				
				if(fagtex == null)
					return;
				final int unit = shader.context.textureBinder.bind(fagtex);
				//final int unit = shader.context.textureBinder.bind(((FogMapAttribute)(combinedAttributes.get(FogMapAttribute.Fogmap))).getFogmap());
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
				
				shader.set(inputID, fagtex == null ? GL_FALSE : GL_TRUE);
			}
			
			@Override
			public boolean isGlobal(BaseShader shader, int inputID) {
				return true;
			}
		});
	}

	
	
}
