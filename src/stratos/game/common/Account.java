

package stratos.game.common;
import stratos.util.*;


//
//  A utility class intended for record 'reasons' for why X or Y or Z is or
//  isn't possible.

//  TODO:  USE THIS TO REPORT ON HOLDING-UPGRADES, MANUFACTURE-PROBLEMS, ETC.

public class Account {
  
  
  boolean isOkay = false;
  Batch <String> failReasons = new Batch <String> ();
  
  
  public boolean setSuccess() {
    isOkay = true;
    return true;
  }
  
  
  public boolean setFailure(String reason) {
    failReasons.add(reason);
    return false;
  }
  
  
  public boolean wasSuccess() {
    return isOkay;
  }
  
  
  public Series <String> failReasons() {
    return failReasons;
  }
  
  
  
  /**  
    */
  final public static Account NONE = new Account() {
    public boolean setSuccess() { return true; }
    public boolean setFailure(String reason) { return false; }
  };
}







