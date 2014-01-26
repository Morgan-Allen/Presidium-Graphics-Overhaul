package graphics.jointed;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.shaders.GLES10Shader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;


/**
 * This class exist just because default libgdx shader provider had
 * hardcoded 12 bones limit.  Well I changed it to 20- bones count shouldn't be
 * dynamic for a shader, as it would make it impossible to use the same shader
 * with multiple models.
 */

//  TODO:  Port over cel-shading related code here as well.


public class JointShading extends DefaultShaderProvider {
  
  
  final public static int
    MAX_BONES = 20 ;
  
  
  public JointShading() {
  }

  public JointShading(String vertexShader, String fragmentShader) {
    super(vertexShader, fragmentShader);
  }

  public JointShading(FileHandle vertexShader, FileHandle fragmentShader) {
    super(vertexShader, fragmentShader);
  }
  
  
  protected Shader createShader(final Renderable renderable) {
    if (Gdx.graphics.isGL20Available()) {
      final DefaultShader.Config config = new DefaultShader.Config() ;
      //  Bit of a hacky workaround here.  Look up solutions.
      config.numBones = MAX_BONES ;
      //config.fragmentShader = Gdx.files.internal("shaders/default.frag").readString();
      //config.vertexShader = Gdx.files.internal("shaders/default.vert").readString();
      return new DefaultShader(renderable, config) ;
    }
    return new GLES10Shader();
  }
}


//  TODO:  Try looking this up:
//  http://cgg-journal.com/2008-2/06/index.html
//  http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter09.html
//  http://stackoverflow.com/questions/11188314/opengl-glsl-cel-shading-and-outline-algorithm


//private Shader outline;

//outline = new OutlineShader();
//outline.init();
//  Performed after normal sprite-batch rendering-
/*
celbatch.begin(cam);
for (ModelInstance instance : instances) {
        celbatch.render(instance, outline);
}
celbatch.end();
//*/


/**
 * I had some few other ideas on how to make outlines, but I guess with low
 * poly models they are overkill, and some of them wouldnt work.
 * This uses oldschool method of glPolygonMode
 * PROBLEM:   This doesn't work with OpenGL ES!
 */
/*
public class OutlineShader implements Shader {
    private ShaderProgram program;
        private RenderContext context;
        private Camera camera;
        
    int u_projViewTrans;
    int u_worldTrans;
    int u_bones;
    int u_skinningFlag;
    
        @Override
    public void init () {
        String vert = Gdx.files.internal("shaders/black.vert").readString();
        String frag = Gdx.files.internal("shaders/black.frag").readString();
        program = new ShaderProgram(vert, frag);
        if (!program.isCompiled())
            throw new GdxRuntimeException(program.getLog());
        
        u_projViewTrans        = program.getUniformLocation("u_projViewTrans");
        u_worldTrans        = program.getUniformLocation("u_worldTrans");
        u_bones                        = program.getUniformLocation("u_bones[0]"); // god... DAMN
        u_skinningFlag        = program.getUniformLocation("u_skinningFlag");
        String[] uniforms = program.getUniforms();
        
        for(String uni : uniforms) {
                System.out.println(uni);
        }
        
        System.out.println("u_bones:" + u_bones);
    }
        
    @Override
    public void dispose () {
            program.dispose();
    }
    
    @Override
    public void begin (Camera camera, RenderContext context) {
        this.camera = camera;
        this.context = context;
            program.begin();
        program.setUniformMatrix(u_projViewTrans, camera.combined);
        
        context.setDepthMask(true);
        context.setDepthTest(GL_LESS);
        
        glPolygonMode(GL_BACK, GL_LINE);
        glLineWidth(3f);
    }
    
    
        private final static Matrix4 idt = new Matrix4();
        private final float bones[] = new float[20 * 16];
        
        Mesh meshbind = null;
        Material curmaterial = null;
        
        protected void bindMesh(Mesh mesh) {
                if(meshbind==mesh)
                        return;
                
                mesh.bind(program);
                meshbind = mesh;
        }
        
        
        protected boolean skinningFlag = false;
        
        protected final void setSkinningFlag(boolean flag) {
                if(skinningFlag == flag)
                        return;
                skinningFlag = flag;
                program.setUniformi(u_skinningFlag, skinningFlag ? GL_TRUE : GL_FALSE);
        }
        
    @Override
    public void render (Renderable renderable) {
        program.setUniformMatrix(u_worldTrans, renderable.worldTransform);
                
//        for (int i = 0; i < bones.length; i++) {
//                        final int idx = i/16;
//                        bones[i] = (renderable.bones == null || idx >= renderable.bones.length || renderable.bones[idx] == null) ? idt.val[i%16] : renderable.bones[idx].val[i%16];
//                }
//        program.setUniformMatrix4fv("u_bones", bones, 0, bones.length);
        if(renderable.bones != null && renderable.bones.length!=0) {
                setSkinningFlag(true);
                int len = Math.min(bones.length, renderable.bones.length * 16);
                        for (int i = 0; i < len; i++) {
                                bones[i] = renderable.bones[i/16].val[i%16];
                        }
                        program.setUniformMatrix4fv(u_bones, bones, 0, len);
        } else {
                setSkinningFlag(false);
                        //for (int i = 0; i < bones.length; i++) {
                        //        bones[i] = idt.val[i%16];
                        //}
                        //program.setUniformMatrix4fv(u_bones, bones, 0, bones.length);
        }
                
        //renderable.mesh.bind(program);
        bindMesh(renderable.mesh);
        
        renderable.mesh.render(program, GL20.GL_TRIANGLES, renderable.meshPartOffset, renderable.meshPartSize, false);
    }
    
    @Override
    public void end () {
        glPolygonMode(GL_BACK, GL_FILL);
            meshbind = null;
            program.end();
    }
    
    @Override
    public int compareTo (Shader other) {
        return 0;
    }
    
    @Override
    public boolean canRender (Renderable instance) {
        return true;
    }
    
    

}

//*/





