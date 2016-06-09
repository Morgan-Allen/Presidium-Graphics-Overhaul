


package stratos.graphics.common;
import stratos.graphics.widgets.*;



public class LoadingScreen extends HUD {
  
  
  private String
    titleImage,
    backImage,
    progFillImg,
    progBackImg;
  
  private ProgressBar progBar;
  private boolean initDone = false;
  
  
  public LoadingScreen(
    Rendering rendering,
    String titleImage  ,
    String backImage   ,
    String progFillImg ,
    String progBackImg
  ) {
    super(rendering);
    this.titleImage = titleImage;
    this.backImage  = backImage ;
    this.progFillImg = progFillImg;
    this.progBackImg = progBackImg;
  }
  
  
  public void renderHUD(Rendering rendering) {
    if (! initDone) {
      final Image blankImage = new Image(this, this.backImage);
      blankImage.alignToFill();
      blankImage.stretch = true;
      blankImage.attachTo(this);
      
      final Image titleImage = new Image(this, this.titleImage);
      titleImage.alignToCentre();
      titleImage.expandToTexSize(1, true);
      titleImage.lockToPixels = true;
      titleImage.attachTo(this);
      
      progBar = new ProgressBar(
        this, progFillImg, progBackImg
      );
      progBar.alignAcross  (0.15f, 0.85f   );
      progBar.alignVertical(0.25f, 25   , 0);
      progBar.attachTo(this);
      
      initDone = true;
    }
    super.renderHUD(rendering);
  }
  
  
  public void update(String label, float progress) {
    if (! initDone) return;
    progBar.progress = progress;
  }
}



