

package stratos.graphics.cutout;
import stratos.graphics.common.*;
import stratos.util.*;




/**  Provides a visual representation of a stack of items within the inventory
  *  of a particular venue.
  */
public class ItemStack extends GroupSprite {
  

  final public static float
    ITEM_SIZE = 0.33f,
    H         = 0 - ITEM_SIZE / 2,
    L         =     ITEM_SIZE / 2,
    ATTACH_COORDS[][] = {
      {L, H}, {L, L}, {H, H}, {H, L}
    },
    UP = ITEM_SIZE * 0.8f
  ;
  final static int ITEM_UNIT = 5;
  
  
  final CutoutModel itemModel, batchModel;
  private int amount = 0;
  
  
  protected ItemStack(CutoutModel itemModel, CutoutModel batchModel) {
    this.itemModel  = itemModel ;
    this.batchModel = batchModel;
  }
  
  
  protected void updateAmount(int newAmount) {
    final int oldAmount = this.amount;
    if (oldAmount == newAmount) return;
    clearAllAttachments();
    
    //  First, determine how many crates and packets of the good should be
    //  shown-
    int numPacks = (int) Nums.ceil(newAmount * 1f / ITEM_UNIT), numCrates = 0;
    while (numPacks > 4) { numPacks -= 4; numCrates++; }
    final int
      total = numCrates + numPacks,
      numLevels = total / 4,
      topOffset = (4 * (int) Nums.ceil(total / 4f)) - total;
    
    //  Then iterate through the list of possible positions, and fill 'em up.
    for (int i = 0; i < total; i++) {
      final int level = i / 4, coordIndex = (level < numLevels) ?
        (i % 4) :
        (i % 4) + topOffset;
      final float coord[] = ATTACH_COORDS[coordIndex];
      
      final CutoutSprite box = (CutoutSprite) ((i < numCrates) ?
        batchModel : itemModel
      ).makeSprite();
      attach(box, coord[0], coord[1], level * UP);
    }
    amount = newAmount;
  }
  
  
  public void readyFor(Rendering rendering) {
    super.readyFor(rendering);
  }
}






