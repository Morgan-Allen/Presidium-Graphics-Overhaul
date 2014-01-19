


package sf.gdx.terrain;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture;



//  For the moment, I've settled on loading a single texture at a time, rather
//  than trying to stuff everything into one huge texture atlas (which can
//  apparently cause problems on certain mobile devices-)
//  http://www.java-gaming.org/index.php?topic=27795.0
//  At any rate, since each layer is rendered sequentially, each texture should
//  only require binding once, so there shouldn't be much of a performance hit.

//  Note:  Elaborate loading is being done here to work around for the apparent
//  lack of support for .gif or indexed .pngs in LibGDX.


public class TextureLoad {
	Texture load(String name) {
		Pixmap atlas = new Pixmap(512, 512, Pixmap.Format.RGBA4444);
		Texture texture = new Texture(atlas);
		texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		Pixmap from = new Pixmap(Gdx.files.internal(name));
        Pixmap.setBlending(Pixmap.Blending.None);
		atlas.drawPixmap(
			from, 1, 1, from.getWidth(), from.getHeight(), 1, 1, 512, 512
		) ;
        
		texture.draw(atlas, 0, 0);
		return texture ;
	}
}



