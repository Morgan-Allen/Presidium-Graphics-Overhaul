
//  Credit to theagentd from javagaming forums...

package stratos.graphics.common;
import com.badlogic.gdx.utils.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL33.*;
import java.util.ArrayList;



public class GPUTaskProfile implements Pool.Poolable {
	
	private GPUTaskProfile parent;
	private String name;
	private int startQuery, endQuery;
	
	private ArrayList <GPUTaskProfile> children;
	
	
	public GPUTaskProfile() {
		children = new ArrayList <GPUTaskProfile> ();
	}
	
	
	public GPUTaskProfile init(
	  GPUTaskProfile parent, String name, int startQuery
	) {
		this.parent = parent;
		this.name = name;
		this.startQuery = startQuery;
		if (parent != null) {
			parent.addChild(this);
		}
		return this;
	}
	
	
	private void addChild(GPUTaskProfile profilerTask) {
		children.add(profilerTask);
	}
	
	
	public GPUTaskProfile end(int endQuery) {
		this.endQuery = endQuery;
		return parent;
	}
	
	
	public GPUTaskProfile getParent() {
		return parent;
	}
	
	
	public boolean resultsAvailable() {
		return glGetQueryObjectui(endQuery, GL_QUERY_RESULT_AVAILABLE) == GL_TRUE;
	}
	
	
	public String getName() {
		return name;
	}
	
	
	public int getStartQuery() {
		return startQuery;
	}
	
	
	public int getEndQuery() {
		return endQuery;
	}
	
	
	public long getStartTime() {
		return glGetQueryObjectui64(startQuery, GL_QUERY_RESULT);
	}
	
	
	public long getEndTime() {
		return glGetQueryObjectui64(endQuery, GL_QUERY_RESULT);
	}
	
	
	public long getTimeTaken() {
		return getEndTime() - getStartTime();
	}
	
	
	public ArrayList <GPUTaskProfile> getChildren() {
		return children;
	}
	
	
	public void reset() {
		startQuery = -1;
		endQuery = -1;
		children.clear();
	}
	
	
	public void dump() {
		dump(0);
	}
	
	
	private void dump(int indentation) {
		for (int i = 0; i < indentation; i++) {
			System.out.print("    ");
		}
		System.out.println(name + " : " + getTimeTaken()/1000 / 1000f + "ms");
		for (int i = 0; i < children.size(); i++) {
			children.get(i).dump(indentation + 1);
		}
	}
}




