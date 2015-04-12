

package stratos.game.common;
import stratos.util.*;


//
//  A utility class intended for record 'reasons' for why X or Y or Z is or
//  isn't possible.

//  TODO:  USE THIS TO REPORT ON HOLDING-UPGRADES, MANUFACTURE-PROBLEMS, ETC.

public class Account {
  
  
  boolean isOkay = false;
  Batch <String> failReasons = new Batch <String> ();
  
  
  public boolean asSuccess() {
    isOkay = true;
    return true;
  }
  
  
  public boolean asFailure(String reason) {
    failReasons.add(reason);
    return false;
  }
  
  
  public boolean success() {
    return isOkay;
  }
  
  
  public Series <String> failReasons() {
    return failReasons;
  }
  
  
  
  /**  
    */
  final public static Account NONE = new Account() {
    public boolean asSuccess() { return true; }
    public boolean asFailure(String reason) { return false; }
  };
}







