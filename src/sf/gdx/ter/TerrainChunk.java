
package sf.gdx.ter;
import java.util.Arrays;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import static sf.gdx.ter.TerrainType.*;



public class TerrainChunk {
        
        
        public int width;
        public int height;
        
        public TerrainType[] types;
        public int[][] tiledata;
        
        protected float[] vertices;
        protected short[] indices;
        
        public int ptr;
        public Mesh mesh;
        
        public static TextureRegion lol;
        
        
        
        public TerrainChunk(int width, int height) {
                this.width = width;
                this.height = height;
                tiledata = new int[width][height];
                
                VertexAttribute pos = VertexAttribute.Position();
                VertexAttribute layer1 = VertexAttribute.TexCoords(0);
                VertexAttribute layer2 = VertexAttribute.TexCoords(1);
                VertexAttribute layer3 = VertexAttribute.TexCoords(2);
                VertexAttribute layer4 = VertexAttribute.TexCoords(3);
                int verts = (width-1)*(height-1) * 4 * 11;
                int indis = (width-1)*(height-1)*6;
                
                if(((width-1)*(height-1) * 4) > 65536) {
                        throw new RuntimeException(
                                "Too much vertices("+((width-1)*(height-1) * 4)+
                                ") - out of indices bounds (65536) maximum size is: "+
                                "128x128"
                        );
                }
                mesh = new Mesh(
                        true, verts, indis, pos,
                        layer1, layer2, layer3, layer4
                );
                vertices = new float[verts];
                indices  = new short[indis];
                
                int vi = 0;
                int ii = 0;
                while(ii < indis) {
                        indices[ii++] = (short) vi;
                        indices[ii++] = (short) (vi+1);
                        indices[ii++] = (short) (vi+2);
                        indices[ii++] = (short) (vi +2);
                        indices[ii++] = (short) vi;
                        indices[ii++] = (short) (vi +3);
                        vi+=4;
                }
                mesh.setIndices(indices);
        }
        
        
        private final boolean contains(int[] ar, int val) {
                for(int i : ar) {
                        if(i == val)
                                return true;
                }
                return false;
        }
        
        
        
        
        private final int[] typet = new int[4];
        private final TextureRegion[] regst = new TextureRegion[4];
        
    
        public void generateMesh() {
                ptr = 0;
                for(int x=0; x<width-1; x++) {
                        for(int z=0; z<height-1; z++) {
                                typet[0] = tiledata[x][z];
                                typet[1] = tiledata[x+1][z];
                                typet[2] = tiledata[x][z+1];
                                typet[3] = tiledata[x+1][z+1];
                                
                                Arrays.fill(regst, null);
                                
                                int curreg = 0;
                                
                                for(int i=0; i<types.length; i++) {
                                        if(! contains(typet, i)) continue;
                                        
                                        int mask = mask(i,typet);
                                        TextureRegion reg = types[i].regions.get(mask);
                                        
                                        regst[curreg++] = reg;
                                        if(curreg==4) break;
                                }
                                addTile(x, z, regst);
                        }
                }
                mesh.setVertices(vertices);
        }
        
        
        private static final int mask(int type, int[] typet) {
                int mask = 0;
                if(typet[0] >= type)
                        mask |= b1000;
                if(typet[1] >= type)
                        mask |= b0100;
                if(typet[2] >= type)
                        mask |= b0010;
                if(typet[3] >= type)
                        mask |= b0001;
                return mask;
        }
        
        
        
        private void addTile(int x, int z, TextureRegion[] reg) {
                addVertice(x  , 0, z  , reg, 0, 0);
                addVertice(x  , 0, z+1, reg, 0, 1);
                addVertice(x+1, 0, z+1, reg, 1, 1);
                addVertice(x+1, 0, z  , reg, 1, 0);
        }
        
        
        private final float getU(TextureRegion reg, int index) {
                if(reg==null) return 0;
                return index == 0? reg.u : reg.u2;
        }
        
        
        private final float getV(TextureRegion reg, int index) {
                if(reg==null) return 0;
                return index == 0? reg.v : reg.v2;
        }
        
        
        private void addVertice(float x, float y, float z,TextureRegion regs[], int u, int v) {
                vertices[ptr++] = x;
                vertices[ptr++] = y;
                vertices[ptr++] = z;
                for (TextureRegion reg : regs) {
                        vertices[ptr++] = getU(reg, u);
                        vertices[ptr++] = getV(reg, v);
                }
        }
}