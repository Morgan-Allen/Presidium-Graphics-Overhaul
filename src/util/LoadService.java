/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;
import java.awt.image.BufferedImage ;
import javax.imageio.ImageIO ;
import java.io.* ;
import java.lang.reflect.* ;
import java.security.* ;
import java.util.zip.* ;
import java.net.* ;



/**  This class provides various utility functions for loading and caching of
  *  external file resources.
  */
public class LoadService {
  
  protected static Table <String, Object>
    resCache = new Table <String, Object> (1000) ;
  
  
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
  
  
  /**  Recursively loads all classes in the given directory.
    */
  public static Batch <Class> loadPackage(String packageName) {
    final Batch <Class> zipped = loadZippedClasses(packageName) ;
    if (zipped != null) return zipped ;
    return loadClassesInDir(packageName, null) ;
  }
  
  
  private static Batch <Class> loadClassesInDir(
    String packageName, Batch <Class> loaded
  ) {
    if (loaded == null) loaded = new Batch <Class> () ;
    final File baseDir = new File(packageName.replace('.', '/')) ;
    
    if (baseDir.isDirectory()) for (File defined : baseDir.listFiles()) try {
      final String fileName = defined.getName() ;
      if (defined.isDirectory()) {
        loadClassesInDir(packageName+"."+fileName, loaded) ;
        continue ;
      }
      if (! fileName.endsWith(".java")) continue ;
      
      final int cutoff = fileName.length() - ".java".length() ;
      final String className = fileName.substring(0, cutoff) ;
      loaded.add(Class.forName(packageName+"."+className)) ;
    }
    catch (Exception e) { I.report(e) ; }
    return loaded ;
  }
  
  //
  //  TODO:  Only do this once, and cache the results?
  
  private static Batch <Class> loadZippedClasses(String packageName) {
    
    CodeSource code = LoadService.class.getProtectionDomain().getCodeSource() ;
    if (code == null) return null ;
    
    final File file = new File(code.getLocation().getFile()) ;
    if (! file.exists()) return null ;
    
    final Batch <Class> loaded = new Batch <Class> () ;
    try {
      //
      //  TODO:  Specifying the name of the Jar File should NOT be needed...
      final URL jarURL = new URL(code.getLocation()+"/presidium_run_jar.jar") ;
      ZipInputStream zip = new ZipInputStream(jarURL.openStream()) ;
      for (ZipEntry e ; (e = zip.getNextEntry()) != null ;) {
        if (e.isDirectory()) continue ;
        
        final String name = e.getName() ;
        if (! name.endsWith(".class")) continue ;
        if (name.indexOf('$') != -1) continue ;
        
        final int cutoff = name.length() - ".class".length() ;
        final String className = name.substring(0, cutoff).replace('/', '.') ;
        if (packageName != null && ! className.startsWith(packageName)) {
          continue ;
        }
        
        //I.say("Trying to load: "+className) ;
        try { loaded.add(Class.forName(className)) ; }
        catch (ClassNotFoundException c) { I.report(c) ; }
      }
    }
    catch (IOException e) { I.report(e) ; }
    return loaded ;
  }
  
  

  /**  Writes a string to the given data output stream.
   */
  public static void writeString(DataOutputStream dOut, String s)
      throws IOException {
    byte chars[] = s.getBytes() ;
    dOut.writeInt(chars.length) ;
    dOut.write(chars) ;
  }
  
  
  /**  Reads a string from the given data input stream.
   */
  public static String readString(DataInputStream dIn)
      throws IOException {
    final int len = dIn.readInt() ;
    byte chars[] = new byte[len] ;
    dIn.read(chars) ;
    return new String(chars) ;
  }
  

  /**  Returns an image from the given file name.
    */
  public static BufferedImage getImage(String name) {
    try { return ImageIO.read(new java.io.File(name)) ; }
    catch(java.io.IOException e) {
      I.say("  PROBLEM LOADING IMAGE. " + name + " " + e.getMessage()) ;
      e.printStackTrace() ;
    }
    return null ;
  }
  

  /**  Returns the raw RGBA data for the given image file.
    */
  public static int[] getRGBA(String name) {
    return getRGBA(getImage(name)) ;
  }
  
  /**  Returns the raw RGBA data for the given image.
    */
  public static int[] getRGBA(BufferedImage image) {
    int
      x = image.getWidth(),
      y = image.getHeight(),
      rgba[] = new int[x * y] ;
    image.getRGB(0, 0, x, y, rgba, 0, x) ;
    return rgba ;
  }
  
  
  /**  Seperates the given full file path into it's path and file-name components,
    *  returning them as a two-element array- (path first, file name second.)
    */
  final public static char REP_SEP = '/' ;
  
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

  /**  Returns a safe version of the given path-string (using the default separator.)
    */
  public static String safePath(String path) {
    return safePath(path, java.io.File.separatorChar) ;
  }
  
  /**  Returns a safe version of the given path-string (i.e, using the given seperator.)
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
}

