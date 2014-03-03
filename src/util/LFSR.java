/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.util ;



/**  A Linear Feedback Shift Register used to generate a sequence of pseudo-
  *  random numbers covering every possible value between 1 and 2^n, for a
  *  specified value of n up to 24.
  */
public class LFSR {
  
  
  final static int
    MIN_BITS = 2,
    MAX_BITS = 24 ;
  final static int TAP_INDICES[][] = {
    null, null,
    {1, 0},//2
    {2, 1},//3
    {3, 2},//4
    {4, 2},//5
    {5, 4},//6
    {6, 5},//7
    {7, 5, 4, 3},//8
    {8, 4},//9
    {9, 6},//10
    {10, 8},//11
    {11, 10, 9, 3},//12
    {12, 11, 10, 7},//13
    {13, 12, 11, 1},//14
    {14, 13},//15
    {15, 13, 12, 10},//16
    {16, 13},//17
    {17, 10},//18
    {18, 5, 1, 0},//19
    {19, 16},//20
    {20, 18},//21
    {21, 20},//22
    {22, 17},//23
    {23, 22, 21, 16},//24
  } ;
  
  final int bitSize ;
  final int taps[] ;
  final int mask ;
  int state = 1 ;
  
  public LFSR(int bitSize, int seed) {
    if (bitSize < MIN_BITS || bitSize > MAX_BITS)
      throw new RuntimeException("Illegal bit size for register!") ;
    this.bitSize = bitSize ;
    this.taps = TAP_INDICES[bitSize] ;
    mask = (1 << bitSize) - 1 ;
    state = seed ;
  }
  
  public int nextVal() {
    boolean bit = false ; for (int t : taps) bit ^= bitAt(t, state) ;
    state = (state << 1) & mask ; if (bit) state++ ;
    return state ;
  }
  
  final private static boolean bitAt(final int i, final int state) {
    return (state & (1 << i)) != 0 ;
  }
  
  public static void main(String s[]) {
    testLoop: for (int bits = MIN_BITS ; bits <= MAX_BITS ; bits++) {
      final int period = (1 << bits) - 1 ;
      final boolean reg[] = new boolean[period] ;
      final LFSR lfsr = new LFSR(bits, 1) ;
      I.say("\nBeginning test for "+bits+" bit LFSR...") ;
      //
      //  Begin an exhaustive check.
      for (int i = 0 ; i++ <= period ;) {
        final int val = lfsr.nextVal() ;
        if (reg[val - 1] == true) {
          I.say("Duplicated value after "+i+" steps- ") ;
          if (i < period) I.add("___TEST FAILED___") ;
          else I.add("Test successful!") ;
          continue testLoop ;
        }
        else reg[val - 1] = true ;
      }
      I.say("Test failed- seed value does not recur.") ;
    }
  }
}







