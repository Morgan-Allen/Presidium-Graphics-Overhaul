/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.maps;
import java.io.*;

import stratos.game.common.*;
import stratos.util.*;



/**  A mipmap is a quadtree-based averaging system for data over a
  *  two-dimensional area (e.g, a greyscale image or intensity levels.)
  */
public class MipMap implements TileConstants {
  
  
  /**  Fields and basic accessors-
    */
  final byte baseLevels[][];
  final int quadLevels[][][];
  final int high, size;
  
  public int high() { return high; }
  public int size() { return size; }
  
  
  /**  Creates a basic mipmap of size matching the lowest power of 2 greater
    *  than the minimum size specified.
    */
  public MipMap(final int minSize) {
    int d = 0, s = 1;
    while (s < minSize) { d++; s *= 2; }
    high = d;
    size = s;
    baseLevels = new byte[size][size];
    quadLevels = new int[high][][];
    for (d = 0, s = size / 2; d < high; d++, s /= 2) {
      quadLevels[d] = new int[s][s];
    }
  }
  
  
  public MipMap(HeightMap heights, int scaleHigh) {
    this(heights.span());
    final byte vals[][] = heights.asScaledBytes(scaleHigh);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      accum(vals[c.x][c.y], c.x, c.y);
    }
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    for (int x = size; x-- > 0;) {
      in.read(baseLevels[x]);
    }
    for (int d = 0, s = size / 2; d < high; d++, s /= 2) {
      for (Coord c : Visit.grid(0, 0, s, s, 1)) {
        quadLevels[d][c.x][c.y] = in.readShort();
      }
    }
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    for (int x = size; x-- > 0;) {
      out.write(baseLevels[x]);
    }
    for (int d = 0, s = size / 2; d < high; d++, s /= 2) {
      for (Coord c : Visit.grid(0, 0, s, s, 1)) {
        out.writeShort(quadLevels[d][c.x][c.y]);
      }
    }
  }
  
  
  public void clear() {
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      baseLevels[c.x][c.y] = 0;
    }
    for (int d = 0, s = size / 2; d < high; d++, s /= 2) {
      for (Coord c : Visit.grid(0, 0, s, s, 1)) {
        quadLevels[d][c.x][c.y] = 0;
      }
    }
  }
  
  
  /**  Accumulates the specified value at the given base array coordinates.
    */
  public void accum(final int val, final int x, final int y) {
    baseLevels[x][y] += val;
    for (int h = 0, dX = x / 2, dY = y / 2; h < high; dX /= 2, dY /= 2, h++) {
      quadLevels[h][dX][dY] += val;
    }
  }
  
  
  /**  Sets the specified value at the given base array coordinates.
    */
  public void set(final int val, final int x, final int y) {
    final int current = baseLevels[x][y];
    accum(val - current, x, y);
  }
  
  
  /**  Gets the total accumulation at the given array coordinates (the higher
   *  the level, the smaller x and y need to be.)
   */
  public int getTotalAt(final int mX, final int mY, final int h) {
    if (h == 0) return baseLevels[mX][mY];
    return quadLevels[h - 1][mX][mY];
  }
  
  
  /**  Gets the average accumulation at the given array coordinates (the higher
    *  the level, the smaller x and y can be.)
    */
  public float getAvgAt(final int mX, final int mY, final int h) {
    final int s = 1 << h;
    return (getTotalAt(mX, mY, h) * 1f) / (s * s);
  }
  
  
  public static int sizeToDepth(int size) {
    int d = 0;
    while (size > 1) { d++; size /= 2; }
    return d;
  }
  
  
  /**  Returns an interpolated average of values at the given coordinates over
    *  all levels of the map.  Higher stepFade gives a 'fuzzier' result.
    */
  //
  //  Higher levels of stepFade don't seem to average correctly.  Work on that.
  public float blendValAt(float x, float y, float stepFade) {
    x = Visit.clamp(x, 0.5f, size - 0.5f);
    y = Visit.clamp(y, 0.5f, size - 0.5f);
    if (stepFade < 0) stepFade = 0;
    float sum = 0, weight = 1, sumWeights = 0;
    int minX, minY, maxX, maxY, range;
    float pX, pY, aX, aY;
    
    for (int h = 0, s = 1; h <= high; h++, s *= 2) {
      range = (this.size / s) - 1;
      pX = (x - 0.5f) / s;
      pY = (y - 0.5f) / s;
      minX = (int) pX;
      minY = (int) pY;
      maxX = (minX == range) ? minX : (minX + 1);
      maxY = (minY == range) ? minY : (minY + 1);
      aX = pX - minX;
      aY = pY - minY;
      //if (aX < 0 || aY < 0) I.complain("Negative relAlpha values! "+aX+" "+aY);
      final float blend =
        (getTotalAt(minX, minY, h) * (1 - aX) * (1 - aY)) +
        (getTotalAt(maxX, minY, h) *      aX  * (1 - aY)) +
        (getTotalAt(minX, maxY, h) * (1 - aX) *      aY ) +
        (getTotalAt(maxX, maxY, h) *      aX  *      aY );
      //if (blend < 0) I.complain("Negative blend value: "+blend);
      sum += (blend * weight) / (s * s);
      sumWeights += weight;
      weight *= stepFade;
    }
    /*
    I.say(
      "Sum weights: "+sumWeights+", sum: "+sum+
      ", height: "+(quadLevels.length + 1)
    );
    //*/
    //return sumWeights;
    return sum / sumWeights;
  }
  
  
  
  
  
  /**  Returns a list of all actors within range of the given point.
    */
  public Batch <Coord> allNear(
    final World world, final Target t, final float radius
  ) {
    final Batch <Coord> near = new Batch <Coord> ();
    final Vec3D tP = t.position(null);
    class Bound extends Box2D { float proximity; }
    final Bound first = new Bound();
    first.set(-0.5f, -0.5f, size, size);
    first.proximity = first.distance(tP.x, tP.y);
    
    final Sorting <Bound> sorting = new Sorting <Bound> () {
      public int compare(Bound a, Bound b) {
        if (a == b) return 0;
        return a.proximity > b.proximity ? 1 : -1;
      }
    };
    sorting.add(first); while (sorting.size() > 0) {
      final Object closestRef = sorting.leastRef();
      final Bound closest = sorting.refValue(closestRef);
      sorting.deleteRef(closestRef);
      
      if (closest.proximity > radius) continue;
      if (closest.xdim() < 2) {
        near.add(new Coord(
          (int) (closest.xpos() + 1),
          (int) (closest.ypos() + 1)
        ));
        continue;
      }
      
      final float
        xp = closest.xpos(),
        yp = closest.ypos(),
        hw = closest.xdim() / 2f,
        hh = closest.ydim() / 2f;
      final Bound
        kidA = new Bound(), kidB = new Bound(),
        kidC = new Bound(), kidD = new Bound();
      
      kidA.set(xp, yp, hw, hh);
      kidB.set(xp + hw, yp, hw, hh);
      kidC.set(xp, yp + hh, hw, hh);
      kidD.set(xp + hw, yp + hh, hw, hh);
      kidA.proximity = kidA.distance(tP.x, tP.y);
      kidB.proximity = kidB.distance(tP.x, tP.y);
      kidC.proximity = kidC.distance(tP.x, tP.y);
      kidD.proximity = kidD.distance(tP.x, tP.y);
      sorting.add(kidA);
      sorting.add(kidB);
      sorting.add(kidC);
      sorting.add(kidD);
    }
    return near;
  }
  
  
  
  /**  Testing routine-
    */
  public static void printVals(MipMap map) {
    I.add("\n\nPRINTING MIPMAP");
    for (int x = 0 ; x < map.size; x++) {
      I.add("\n  ");
      for (int y = 0; y < map.size; y++) {
        I.add(map.baseLevels[x][y]+" ");
      }
    }
    
    for (int d = 0, s = map.size / 2; d < map.high; d++, s /= 2) {
      I.add("\nPRINTING LEVEL "+(d + 1)+"");
      for (int x = 0 ; x < s; x++) {
        I.add("\n  ");
        for (int y = 0; y < s; y++) {
          I.add(map.quadLevels[d][x][y]+" ");
        }
      }
    }
  }
  
  
  public static void main(String s[]) {
    
    final MipMap testMap = new MipMap(16);
    
    for (Coord c : Visit.grid(0, 0, 16, 16, 1)) {
      testMap.set((byte) Rand.index(4), c.x, c.y);
    }
    for (Coord c : Visit.grid(0, 0, 16, 16, 1)) {
      testMap.set((byte) Rand.index(4), c.x, c.y);
    }
    printVals(testMap);
    
    final File file = new File("saves/test_mipmap.dat");
    try {
      final DataOutputStream out = new DataOutputStream(
        new FileOutputStream(file)
      );
      testMap.saveTo(out);
      out.close();
      
      final MipMap nextMap = new MipMap(16);
      
      final DataInputStream in = new DataInputStream(
        new FileInputStream(file)
      );
      nextMap.loadFrom(in);
      in.close();
      
      printVals(nextMap);
      
      nextMap.clear();
      for (Coord c : Visit.grid(0, 0, 16, 16, 1)) {
        final int flag = testMap.baseLevels[c.x][c.y];
        nextMap.set(flag, c.x, c.y);
      }
      
      printVals(nextMap);
    }
    catch (Exception e) { I.report(e); }
  }
}



