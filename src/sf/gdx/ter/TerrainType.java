

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
	
	public static final int b0000 = 0;
	public static final int b0001 = 1;
	public static final int b0010 = 2;
	public static final int b0011 = 3;
	public static final int b0100 = 4;
	public static final int b0101 = 5;
	public static final int b0110 = 6;
	public static final int b0111 = 7;
	public static final int b1000 = 8;
	public static final int b1001 = 9;
	public static final int b1010 = 10;
	public static final int b1011 = 11;
	public static final int b1100 = 12;
	public static final int b1101 = 13;
	public static final int b1110 = 14;
	public static final int b1111 = 15;
	
	public final IntMap<TextureRegion> regions = new IntMap<TextureRegion>();
}

