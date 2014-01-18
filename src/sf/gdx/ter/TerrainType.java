

package sf.gdx.ter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.IntMap;



/**
 * Temporal this way..
 *
 * For now it just holds int->TextureRegion map
 * int keys are masks, where every bit represents a corner, in order from top
 * left, to bottom right.
 */
public class TerrainType {
	public final IntMap<TextureRegion> regions = new IntMap<TextureRegion>();
}

