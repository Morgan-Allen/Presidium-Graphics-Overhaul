/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.util.*;
import stratos.graphics.common.*;
import java.awt.MouseInfo;



public class FormingTest {
  
  
  public static void main(String args[]) {
    new FormingTest().setupAndGo();
  }
  
  
  final int size = 64;
  final float initBio = 0.02f;
  final boolean showGraph = false;
  
  boolean init;
  HeightMap heightMap;
  float bioMap[][];
  
  final int dimMap = 256, dimGraph = 64;
  final String displayKey = "forming_test";
  final String graphKey = "thresholds";
  int colorVals[][];
  float graphVals[][];
  
  
  void setupAndGo() {
    reset();
    
    while (true) {
      updateVals();
      
      try { Thread.sleep(200); }
      catch (Exception e) {}
      
      if (I.checkMouseClicked(displayKey)) reset();
    }
  }
  
  
  void reset() {
    init = true;
    heightMap = new HeightMap(size);
    bioMap    = new float[size][size];
    colorVals = new int[size][size];
  }
  
  
  void updateVals() {
    
    float globalBio = 0, sumGrowth = 0;
    Colour tone = new Colour();
    graphVals = new float[dimGraph][dimGraph];
    float increment = 0.1f;
    
    if (init) {
      globalBio = initBio;
      increment = 1;
      init = false;
    }
    else {
      for (Coord c : Visit.grid(0, 0, size, size, 1)) {
        globalBio += bioMap[c.x][c.y];
      }
      globalBio /= size * size;
    }

    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      final float elevation = Nums.clamp(heightMap.value()[c.x][c.y], 0, 1);
      float biomass = bioMap[c.x][c.y];
      //
      //  After some trial-and-error, this seems to give some satisfactory
      //  results.  Basically, low-elevation tiles have a low threshold for
      //  colonisation, and existing biomass (either local or global) improves
      //  capacity for growth, but with diminishing returns.
      final float threshold = elevation * elevation * 1.0f, oldBio = biomass;
      float maxTileBio = (globalBio + biomass + 0.5f - threshold) / 2.5f;
      maxTileBio = Nums.clamp(maxTileBio, 0, 1);
      if (biomass <= maxTileBio) {
        biomass += increment;
      }
      else {
        biomass -= increment;
      }
      bioMap[c.x][c.y] = biomass = Nums.clamp(biomass, 0, maxTileBio);
      sumGrowth += biomass - oldBio;
      //
      //  Populate the display fields (using ARGB, rather than RGBA.)
      final int
        axisX = Nums.clamp((int) (elevation * dimGraph), dimGraph),
        axisY = Nums.clamp((int) (threshold * dimGraph), dimGraph);
      graphVals[axisX][axisY] = 1;
      tone.set(1, elevation, Nums.max(biomass, elevation), elevation);
      if (elevation < globalBio / 2) {
        //  Paint the water-line (a purely cosmetic effect...)
        tone.g /= 2;
        tone.a = (tone.a + 1) / 2;
      }
      colorVals[c.x][c.y] = tone.getRGBA();
    }
    
    sumGrowth /= size * size;
    I.say("\nUpdating map...");
    I.say("  Total biomass: "+I.shorten(globalBio * 100, 2)+"%");
    I.say("  Net growth:    "+I.shorten(sumGrowth * 100, 2)+"%");

    I.present(colorVals, displayKey, dimMap, dimMap);
    if (showGraph) {
      I.present(graphVals, graphKey, dimGraph * 2, dimGraph * 2, 0, 1);
    }
  }
  
  
}












