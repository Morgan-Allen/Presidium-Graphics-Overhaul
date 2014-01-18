package sf.gdx.shaders;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attribute;

public class FogMapAttribute extends Attribute {
	
    public final static String fogmapAlias = "fogmap";
    public static final long Fogmap = register(fogmapAlias);
    
    Texture fogmap;

	public FogMapAttribute(Texture fogmap) {
		super(Fogmap);
		this.fogmap = fogmap;
	}

	@Override
	public Attribute copy() {
		return new FogMapAttribute(fogmap);
	}
	
	public Texture getFogmap() {
		System.out.println("getFogmap !!!1 lololol");
		return fogmap;
	}

	@Override
	protected boolean equals(Attribute that) {
		if(this==that)
			return true;
		if(that instanceof FogMapAttribute) {
			return this.fogmap == ((FogMapAttribute) that).fogmap;
		}
		return false;
	}

}
