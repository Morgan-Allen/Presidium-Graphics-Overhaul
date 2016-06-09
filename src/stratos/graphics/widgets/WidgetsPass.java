/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets;
import stratos.graphics.common.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;



//  NOTE:  I recently adapted this directly from the LibGDX Spritebatch code,
//  and will probably trim it down some more later.
//  TODO:  You can most likely just use a MeshCompile here.

public class WidgetsPass {
  
  
  final public Rendering rendering;
  
  private Mesh mesh;
  private final float[] vertices;
  
  private int idx = 0;
  private Texture lastTexture = null;
  private boolean drawing = false;

  private final Matrix4 transformMatrix = new Matrix4();
  private final Matrix4 projectionMatrix = new Matrix4();
  private final Matrix4 combinedMatrix = new Matrix4();

  private boolean blendingDisabled = false;
  private int blendSrcFunc = GL20.GL_SRC_ALPHA;
  private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
  
  private final ShaderProgram shader;
  
  
  
  public WidgetsPass(Rendering rendering) {
    this.rendering = rendering;
    final int size = 1000;
    
    shader = createDefaultShader();
    mesh = new Mesh(
        VertexDataType.VertexArray,
        false,
        size * 4,
        size * 6,
        new VertexAttribute(
          Usage.Position, 2,
          ShaderProgram.POSITION_ATTRIBUTE
        ),
        new VertexAttribute(
          Usage.ColorPacked, 4,
          ShaderProgram.COLOR_ATTRIBUTE
        ),
        new VertexAttribute(
          Usage.TextureCoordinates, 2,
          ShaderProgram.TEXCOORD_ATTRIBUTE + "0"
        )
    );
    
    projectionMatrix.setToOrtho2D(
      0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()
    );
    
    //  2 + 1 + 2 = vertex size (position, colour, and tex coords.)  4 vertices
    //  per sprite == 20.
    vertices = new float[size * 20];
    
    int len = size * 6;
    short[] indices = new short[len];
    short j = 0;
    for (int i = 0; i < len; i += 6, j += 4) {
      indices[i] = j;
      indices[i + 1] = (short) (j + 1);
      indices[i + 2] = (short) (j + 2);
      indices[i + 3] = (short) (j + 2);
      indices[i + 4] = (short) (j + 3);
      indices[i + 5] = j;
    }
    mesh.setIndices(indices);
  }
  
  
  public void dispose() {
    shader.dispose();
    mesh.dispose();
  }
  
  
  //  TODO:  You can most likely get rid of this, and replace with an actual
  //  vert/frag file.
  static public ShaderProgram createDefaultShader() {
    String vertexShader = "attribute vec4 "
        + ShaderProgram.POSITION_ATTRIBUTE
        + ";\n" //
        + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE
        + ";\n" //
        + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE
        + "0;\n" //
        + "uniform mat4 u_projTrans;\n" //
        + "varying vec4 v_color;\n" //
        + "varying vec2 v_texCoords;\n" //
        + "\n" //
        + "void main()\n" //
        + "{\n" //
        + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE
        + ";\n" //
        + "   v_color.a = v_color.a * (256.0/255.0);\n" //
        + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE
        + "0;\n" //
        + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE
        + ";\n" //
        + "}\n";
    String fragmentShader = "#ifdef GL_ES\n" //
        + "#define LOWP lowp\n" //
        + "precision mediump float;\n" //
        + "#else\n" //
        + "#define LOWP \n" //
        + "#endif\n" //
        + "varying LOWP vec4 v_color;\n" //
        + "varying vec2 v_texCoords;\n" //
        + "uniform sampler2D u_texture;\n" //
        + "void main()\n"//
        + "{\n" //
        + "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" //
        + "}";

    ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
    if (shader.isCompiled() == false)
      throw new IllegalArgumentException("Error compiling shader: "
          + shader.getLog());
    return shader;
  }
  
  
  public void begin() {
    if (drawing) throw new IllegalStateException(
        "SpriteBatch.end must be called before begin."
    );

    Gdx.gl.glDepthMask(false);
    //if (customShader != null) customShader.begin();
    //else
    shader.begin();
    
    setupMatrices();
    drawing = true;
  }
  
  
  public void end() {
    if (!drawing) throw new IllegalStateException(
      "SpriteBatch.begin must be called before end."
    );
    if (idx > 0) flush();
    lastTexture = null;
    drawing = false;
    
    GL20 gl = Gdx.gl20;
    gl.glDepthMask(true);

    shader.end();
  }
  
  
  public void draw(
    Texture texture, Colour colour,
    float x, float y, float width, float height,
    float u, float v, float u2, float v2
  ) {
    if (!drawing)
      throw new IllegalStateException(
          "SpriteBatch.begin must be called before draw.");

    float[] vertices = this.vertices;

    if (texture != lastTexture)
      switchTexture(texture);
    else if (idx == vertices.length) //
      flush();

    final float fx2 = x + width;
    final float fy2 = y + height;
    
    float color = colour.floatBits;
    int idx = this.idx;
    vertices[idx++] = x;
    vertices[idx++] = y;
    vertices[idx++] = color;
    vertices[idx++] = u;
    vertices[idx++] = v;

    vertices[idx++] = x;
    vertices[idx++] = fy2;
    vertices[idx++] = color;
    vertices[idx++] = u;
    vertices[idx++] = v2;

    vertices[idx++] = fx2;
    vertices[idx++] = fy2;
    vertices[idx++] = color;
    vertices[idx++] = u2;
    vertices[idx++] = v2;

    vertices[idx++] = fx2;
    vertices[idx++] = y;
    vertices[idx++] = color;
    vertices[idx++] = u2;
    vertices[idx++] = v;
    this.idx = idx;
  }
  
  
  public void flush() {
    if (idx == 0 || lastTexture == null) return;
    int spritesInBatch = idx / 20;
    int count = spritesInBatch * 6;
    
    lastTexture.bind(0);
    Mesh mesh = this.mesh;
    mesh.setVertices(vertices, 0, idx);
    mesh.getIndicesBuffer().position(0);
    mesh.getIndicesBuffer().limit(count);

    if (blendingDisabled) {
      Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    else {
      Gdx.gl.glEnable(GL20.GL_BLEND);
      if (blendSrcFunc != -1)
        Gdx.gl.glBlendFunc(blendSrcFunc, blendDstFunc);
    }
    
    mesh.render(shader, GL20.GL_TRIANGLES, 0, count);
    idx = 0;
  }
  
  
  private void setupMatrices() {
    combinedMatrix.set(projectionMatrix).mul(transformMatrix);
    shader.setUniformMatrix("u_projTrans", combinedMatrix);
    shader.setUniformi("u_texture", 0);
  }
  
  
  private void switchTexture(Texture texture) {
    flush();
    lastTexture = texture;
  }
}




