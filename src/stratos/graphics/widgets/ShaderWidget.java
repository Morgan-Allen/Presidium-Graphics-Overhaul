

package stratos.graphics.widgets;
import stratos.start.Disposal;
import stratos.util.*;



//  TODO:  Determine if this is still useful.  (It may be a little safer to
//  centralise the various GL functions, aperture sizes, etc. in here.)


public abstract class ShaderWidget extends UIGroup {
  
  
  final private Disposal disposal;
  String methodCall;
  
  int glBlendFunction;
  boolean useDepth;
  Box2D scissorBounds;  //TODO:  Use render-in-texture?
  
  
  
  ShaderWidget(HUD UI) {
    super(UI);
    disposal = new Disposal(true) {
      protected void performAssetSetup() { setupShaderAssets(); }
      protected void performAssetDisposal() { disposeShaderAssets(); }
    };
  }
  
  
  protected abstract void setupShaderAssets();
  protected abstract void disposeShaderAssets();
  protected abstract void performShaderRendering();
  

  protected void render(WidgetsPass pass) {
    //  TODO:  Set GL parameters, call the rendering delegate methods, and
    //  then revert back to standard GL params!
  }
}





