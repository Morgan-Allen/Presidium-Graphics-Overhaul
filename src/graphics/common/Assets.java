


package src.graphics.common;
import src.util.*;

import java.io.*;
import java.util.zip.*;
import java.net.URL;
import java.security.CodeSource;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;



public class Assets {
  
  
  final static String JAR_NAME = "presidium_run_jar.jar";
  final static boolean EXCLUDE_BASE_DIR = false;
  final public static char REP_SEP = '/';
  private static boolean verbose = false;
  
  
  protected static abstract class Loadable {

    final String assetID;
    final Class sourceClass;
    
    Loadable(String modelName, Class sourceClass) {
      this.assetID = modelName;
      this.sourceClass = sourceClass;
      Assets.registerForLoading(this);
    }
    
    public abstract boolean isLoaded();
    protected abstract void loadAsset();
    protected abstract void disposeAsset();
  }
  
  
  final static List <String>
    classesToLoad = new List <String> ();
  final static List <Class>
    classesLoaded = new List <Class> ();
  
  final static List <Loadable>
    assetsToLoad = new List <Loadable> (),
    assetsLoaded = new List <Loadable> ();
  
  protected static Table <String, Object>
    resCache = new Table <String, Object> (1000) ;
  
  
  
  /**  Methods called externally by the PlayLoop class.
    */
  public static void compileAssetList(String sourcePackage) {
    final Batch <String> names = new Batch <String> ();
    compileClassNames(sourcePackage, names);
    compileJarredClassNames(sourcePackage, names);
    
    if (verbose) I.say("\nCompiling list of classes to load-");
    final String prefix = sourcePackage+".";
    for (String name : names) {
      if (EXCLUDE_BASE_DIR) name = name.substring(prefix.length());
      if (verbose) I.say("  Class to load is: "+name);
      classesToLoad.add(name);
    }
  }
  
  
  public static void advanceAssetLoading(int timeLimit) {
    
    if (verbose) I.say("Advancing asset loading...");
    final long initTime = System.currentTimeMillis();
    while (true) {
      final long timeSpent = System.currentTimeMillis() - initTime;
      if (timeSpent >= timeLimit) return;
      
      if (classesToLoad.size() > 0) {
        final String className = classesToLoad.removeFirst();
        try {
          final Class match = Class.forName(className);
          classesLoaded.add(match);
          if (verbose) I.say("  Class loaded okay: "+match);
        }
        catch (ClassNotFoundException e) {
          I.say("CLASS NOT FOUND: "+className);
        }
      }
      
      else if (assetsToLoad.size() > 0) {
        final Loadable asset = assetsToLoad.first();
        asset.loadAsset();
        assetsToLoad.remove(asset);
        assetsLoaded.add(asset);
        modelCache.put(asset.assetID, asset);
        if (verbose) I.say("  Asset loaded okay: "+asset.assetID);
      }
      
      else return;
    }
  }
  
  
  public static float loadProgress() {
    final int classTotal = classesToLoad.size() + classesLoaded.size();
    final float classProgress = (classTotal == 0) ?
      0 : classesLoaded.size() * 1f / classTotal;
    
    final int assetTotal = assetsToLoad.size() + assetsLoaded.size();
    final float assetProgress = (assetTotal == 0) ?
      0 : assetsLoaded.size() * 1f / assetTotal;
    
    if (classProgress == 1 && assetTotal == 0) return 1;
    float progress = 0;
    progress += classProgress * 0.1f;
    progress += assetProgress * 0.9f;
    return progress;
  }
  
  
  public static void dispose() {
    for (Loadable asset : assetsLoaded) asset.disposeAsset();
  }
  
  
  public static void registerForLoading(Loadable asset) {
    if (verbose) I.say("    Registering- "+asset.assetID);
    assetsToLoad.add(asset);
  }
  
  
  
  /**  Utility methods for referring to assets and their derivable state from
    *  with game-save files-
    */
  static Table <String, Loadable>
    modelCache = new Table <String, Loadable> (1000) ;
  static Table <Integer, Loadable>
    IDModels = new Table <Integer, Loadable> (1000) ;
  static Table <Loadable, Integer>
    modelIDs = new Table <Loadable, Integer> (1000) ;
  static int counterID = 0 ;
  
  
  public static void clearReferenceIDs() {
    IDModels.clear() ;
    modelIDs.clear() ;
    counterID = 0 ;
  }
  
  
  public static void saveReference(
      Loadable model, DataOutputStream out
  ) throws Exception {
    if (model == null) { out.writeInt(-1) ; return ; }
    final Integer modelID = modelIDs.get(model) ;
    if (modelID != null) { out.writeInt(modelID) ; return ; }
    final int newID = counterID++ ;
    modelIDs.put(model, newID) ;
    out.writeInt(newID) ;
    Assets.writeString(out, model.assetID) ;
    Assets.writeString(out, model.sourceClass.getName()) ;
  }
  
  
  public static Loadable loadReference(
      DataInputStream in
  ) throws Exception {
    final int modelID = in.readInt() ;
    if (modelID == -1) return null ;
    Loadable loaded = IDModels.get(modelID) ;
    if (loaded != null) return loaded ;
    final String modelName = Assets.readString(in);
    final String className = Assets.readString(in);
    Class.forName(className);
    loaded = modelCache.get(modelName);
    
    if (loaded == null) I.complain(
      "MODEL NAMED: "+modelName+
      "\nNO LONGER DEFINED IN SPECIFIED CLASS: "+className
    ) ;
    IDModels.put(modelID, loaded) ;
    return loaded ;
  }
  
  
  /**  
    */
  public static Batch <Class> loadPackage(String packageName) {
    if (classesToLoad.size() > 0) I.complain("CLASS LOADING NOT COMPLETE");
    
    final String key = "package"+packageName;
    final Batch <Class> cached = (Batch <Class>) getResource(key);
    if (cached != null) return cached;
    
    final Batch <Class> matches = new Batch <Class> ();
    for (Class checked : classesLoaded) {
      if (checked.getName().startsWith(packageName)) matches.add(checked);
    }
    
    cacheResource(matches, key);
    return matches;
  }
  

  
  /**  Caches the given resource.
    */
  public static void cacheResource(Object res, String key) {
    resCache.put(key, res) ;
  }
  
  
  /**  Returns the resource matching the given key (if cached- null otherwise.)
    */
  public static Object getResource(String key) {
    return resCache.get(key) ;
  }
  
  
  public static boolean exists(String fileName) {
    final File file = new File(fileName);
    return file.exists();
  }
  
  
  
  /**  Recursively compiles the (presumed) full-path names of all classes in
    *  the given file directory.
    */
  private static void compileClassNames(
    String dirName, Batch <String> loaded
  ) {
    final File baseDir = new File(dirName) ;

    if (baseDir.isDirectory()) for (File defined : baseDir.listFiles()) try {

      final String fileName = defined.getName();
      if (defined.isDirectory()) {
        compileClassNames(dirName+"/"+fileName, loaded) ;
        continue;
      }
      if (! fileName.endsWith(".java")) continue;
      
      final int cutoff = fileName.length() - ".java".length();
      String className = fileName.substring(0, cutoff);
      className = (dirName+"/"+className).replace('/', '.');
      loaded.add(className);
    }
    catch (Exception e) {
      I.say("TROUBLE READING: "+defined);
    }
  }
  
  
  /**  Pulls the same trick, but applied to class files embedded in a .jar
    *  file.
    */
  private static void compileJarredClassNames(
    String dirName, Batch <String> loaded
  ) {
    CodeSource code = Assets.class.getProtectionDomain().getCodeSource();
    if (code == null) return;
    final File file = new File(code.getLocation().getFile());
    if (! file.exists()) return;
    
    try {
      final URL jarURL = new URL(code.getLocation()+"/"+JAR_NAME) ;
      ZipInputStream zip = new ZipInputStream(jarURL.openStream()) ;
      for (ZipEntry e ; (e = zip.getNextEntry()) != null ;) {
        if (e.isDirectory()) continue ;
        
        final String name = e.getName() ;
        if (! name.endsWith(".class")) continue ;
        if (name.indexOf('$') != -1) continue ;
        
        final int cutoff = name.length() - ".class".length() ;
        final String className = name.substring(0, cutoff).replace('/', '.') ;
        if (dirName != null && ! className.startsWith(dirName)) continue;
        loaded.add(className);
      }
    }
    catch (IOException e) { I.report(e) ; }
  }
  
  
  
  /**  Seperates the given full file path into it's path and file-name
    *  components, returning them as a two-element array- (path first, file
    *  name second.)
    */
  public static String[] sepPath(String fullPath) {
    char[] fP = fullPath.toCharArray() ;
    int ind ;
    for (ind = fP.length ; ind-- > 0 ;)
      if (fP[ind] == REP_SEP) break ;
    
    if (ind < fP.length) ind++ ;
    String PN[] = {
      new String(fP, 0, ind),
      new String(fP, ind, fP.length - ind)
    } ;
    return PN ;
  }
  
  
  /**  Returns a safe version of the given path-string (using the default
    *  separator.)
    */
  public static String safePath(String path) {
    return safePath(path, java.io.File.separatorChar) ;
  }
  
  
  /**  Returns a safe version of the given path-string (i.e, using the given
    *  seperator.)
    */
  public static String safePath(String path, char separator) {
    int l = path.length() ;
    char chars[] = new char[l] ;
    path.getChars(0, l, chars, 0) ;
    for (int x = 0 ; x < l ; x++) {
      switch(chars[x]) {
        case('/'):
        case('\\'):
          chars[x] = separator ;
        break ;
      }
    }
    return new String(chars) ;
  }
  
  
  public static void writeString(DataOutputStream dOut, String s)
      throws IOException {
    byte chars[] = s.getBytes() ;
    dOut.writeInt(chars.length) ;
    dOut.write(chars) ;
  }
  
  
  public static String readString(DataInputStream dIn) throws IOException {
    final int len = dIn.readInt() ;
    byte chars[] = new byte[len] ;
    dIn.read(chars) ;
    return new String(chars) ;
  }
}





