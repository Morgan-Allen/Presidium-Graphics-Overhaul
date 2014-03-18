


package stratos.graphics.terrain;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;



public class TerrainSet {
  
  
  final static int
    DEFAULT_CHUNK_SIZE = 64,
    MAX_CHUNK_SIZE     = 128;
  
  
  final public int size, numLayers;
  final int chunkSize, chunkGrid;
  final byte layerIndices[][];
  
  TerrainChunk chunks[][][];
  LayerType layers[];
  
  
  
  public TerrainSet(
      int size, int chunkSize,
      byte layerIndices[][],
      LayerType layers[]
  ) {
    // Basic sanity checks first-
    if (chunkSize > MAX_CHUNK_SIZE) {
      throw new RuntimeException("Chunks too big!");
    }
    else if (chunkSize <= 0) chunkSize = DEFAULT_CHUNK_SIZE;
    //
    // Appropriate dimensions calculated-
    this.numLayers = layers.length;
    this.size = size;
    this.chunkSize = chunkSize;
    this.chunkGrid = (int) Math.ceil(size / chunkSize);
    //this.fog = fogged ? new FogOverlay(size, this) : null;
    this.layers = layers;
    this.layerIndices = layerIndices;
    //
    // And finally, the bite-size terrain chunks that actually get
    // rendered on a need-to-see basis-
    this.chunks = new TerrainChunk[chunkGrid][chunkGrid][numLayers];
  }
  
  
  public void dispose() {
    for (Coord c : Visit.grid(0, 0, chunkGrid, chunkGrid, 1)) {
      for (LayerType layer : layers) {
        final TerrainChunk chunk = chunks[c.x][c.y][layer.layerID];
        chunk.dispose();
        if (chunk.fadeOut != null) chunk.fadeOut.dispose();
      }
    }
    for (LayerType layer : layers) layer.dispose();
  }
  
  
  public void refreshAllMeshes() {
    for (Coord c : Visit.grid(0, 0, chunkGrid, chunkGrid, 1)) {
      for (LayerType layer : layers) {
        final TerrainChunk oldChunk = chunks[c.x][c.y][layer.layerID];
        if (oldChunk != null && ! oldChunk.refreshFlag) continue;
        
        final TerrainChunk chunk = new TerrainChunk(
          chunkSize, chunkSize,
          c.x * chunkSize, c.y * chunkSize,
          layer, this
        );
        chunk.generateMesh();
        chunks[c.x][c.y][layer.layerID] = chunk;
        chunk.refreshFlag = false;
        chunk.fadeOut = oldChunk;
        chunk.fadeIncept = Rendering.activeTime();
      }
    }
  }
  
  
  public void flagUpdateAt(int x, int y) {
    for (LayerType layer : layers) flagUpdateAt(x, y, layer);
  }
  
  
  public void flagUpdateAt(int x, int y, LayerType layer) {
    final int tx = x / chunkSize, ty = y / chunkSize;
    if (tx < 0 || tx >= chunkGrid) return;
    if (ty < 0 || ty >= chunkGrid) return;
    chunks[tx][ty][layer.layerID].refreshFlag = true;
  }
  
  
  public void renderWithin(Box2D area, Rendering rendering) {
    final int
      minX = (int) ((area.xpos() + 1) / chunkSize),
      minY = (int) ((area.ypos() + 1) / chunkSize),
      dimX = 1 + (int) ((area.xmax() - 1) / chunkSize) - minX,
      dimY = 1 + (int) ((area.ymax() - 1) / chunkSize) - minY ;
    
    int i = 0 ; for (Coord c : Visit.grid(minX, minY, dimX, dimY, 1)) {
      for (TerrainChunk patch : chunks[c.x][c.y]) {
        patch.renderTo(rendering);
      }
    }
  }
}

