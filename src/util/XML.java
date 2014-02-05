/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package util ;
import java.io.* ;


/**  In essence, an easier-going XML file/node, which constructs a hierarchy of
  *  tags, attributes, arguments and content from a given .xml file.
  */
public class XML {
  
  
  protected XML
    parent,
    children[] ;
  protected int
    indexAsChild = -1 ;
  protected String
    tag,
    content,
    attributes[],
    values[] ;
  protected Object attached = null ;
  
  public String tag()       { return tag ; }
  public String content()   { return content ; }
  public int numChildren()  { return children.length ; }
  public int indexAsChild() { return indexAsChild ; }
  public XML child(int n)   { return children[n] ; }
  public XML[] children()   { return children ; }
  public XML parent()       { return parent ; }
  
  public void attach(Object o) { attached = o ; }
  public Object attached() { return attached ; }
  
  
  /**  Returns the first child matching the given tag.
    */
  public XML child(String tag) {
    for (XML child : children) if (child.tag.equals(tag)) return child ;
    return null ;
  }

  /**  Returns an array of all children matching the given tag.
    */
  public XML[] allChildrenMatching(String tag) {
    Stack <XML> matches = new Stack <XML> () ;
    for (XML child : children)
      if (child.tag.equals(tag)) matches.addFirst(child) ;
    return (matches.size() > 0) ?
      (XML[]) matches.toArray(XML.class) :
      new XML[0] ;
  }
  
  /**  Returns the first child whose named attribute matches the given value.
    */
  public XML matchChildValue(String att, String value) {
    for (XML child : childList)
      if (child.value(att).equals(value))
        return child ;
    return null ;
  }
  
  
  /**  Returns this node's value for the given attribute (if present- null
    *  otherwise.)
    */
  public String value(String label) {
    for (int n = values.length ; n-- > 0 ;)
      if (attributes[n].equals(label)) return values[n] ;
    return null ;
  }
  
  
  public boolean getBool(String label) {
    final String val = value(label) ;
    return (val == null) ? false : Boolean.parseBoolean(val) ;
  }
  
  
  public float getFloat(String label) {
    final String val = value(label) ;
    return (val == null) ? 1 : Float.parseFloat(val) ;
  }
  

  public int getInt(String label) {
    return (int) getFloat(label) ;
  }
  
  
  
  /**  Returns the XML node constructed from the file with the given name.
    */
  public static XML load(String fileName) {
    Object cached = LoadService.getResource(fileName) ;
    if (cached != null) return (XML) cached ;
    final XML xml = new XML(new File(fileName)) ;
    LoadService.cacheResource(xml, fileName) ;
    return xml ;
  }
  
  //  Temporary member lists, to be discarded once setup is complete-
  private List <XML>
    childList = new List <XML> () ;
  private List <String>
    attributeList = new List <String> (),
    valueList = new List <String> () ;
  
  /**  Constructs a new XML node from the given text file.
   */
  private XML(File xmlF) {
    try {
      XML current = this ;
      boolean
        rTag = false,   //reading an opening tag.
        rAtt = false,   //reading a tag or attribute name.
        rVal = false,   //reading an attribute value.
        rCon = false ;  //reading content between open and closing tags.
      int
        cRead = 0,  //index for  start of content reading.
        aRead = 0,  //attribute reading...
        vRead = 0,  //value reading...
        index,      //current index in file.
        length ;    //total length of file.
      
      FileInputStream fR = new FileInputStream(xmlF) ;
      byte
        chars[] = new byte[length = (int)xmlF.length()],
        read ;
      fR.read(chars) ;
      
      for (index = 0 ; index < length ; index++) {
        read = chars[index] ;
        if (Character.isWhitespace((char)read)) read = ' ' ;
        //  If you're reading a tag or value:
        if (rTag) {
          //  If you're reading an attribute value:
          if (rVal) {
            if (read == '"') {
              current.valueList.addLast(readS(chars, vRead, index)) ;
              ///I.say("\n  adding value: " + current.valueList.last().refers) ;
              rVal = false ;
            }
            continue ;
          }
          //  If you're reading an attribute or name tag:
          if (rAtt) {
            switch(read) {
              
              case('='):
                current.attributeList.addLast(readS(chars, aRead, index)) ;
                ///I.say(
                ///  "\n  adding attribute: " + current.attributeList.last().refers
                ///  + " (No. " + current.attributeList.size() + ")") ;
                rAtt = false ;
                break ;
              
              case('>'):
              case(' '):
                current.tag = readS(chars, aRead, index) ;
                ///I.say("\n  setting tag: " + current.tag) ;
                rAtt = false ;
                break ;
            }
            if (rAtt) continue ;
          }
          //  Otherwise:
          switch(read) {
            
            case('"'):
              rVal = true ;
              vRead = index + 1 ;
              break ;
            
            case('>'):
              if (chars[index - 1] == '/') {
                //this is a closed tag, so the xml block ends here.
                rTag = false ;
                ///I.say("\nclosed tag (" + current.tag + "). Going back to parent-") ;
                current = current.parent ;
              }
              if (rCon = rTag) cRead = index + 1 ;
              //this was an opening tag, so new content should be read.
              rTag = false ;
              break ;
            
            case('='):
            case('/'):
            case(' '):
              //ignore these characters.
              break ;
            
            default:
              //anything else would begin an attribute in a tag.
              //I.say("\nopening tag: ") ;
              rAtt = true ;
              aRead = index ;
              break ;
          }
          
          if (rTag) continue ;
        }
        
        if (read == '<') {
          //  An opening/closing tag begins...
          rTag = true ;
          
          if (rCon) {
            //  Test.report("adding content.") ;
            current.content = readS(chars, cRead, index) ;
            rCon = false ;
          }
          
          if (chars[index + 1] == '/') {
            //...this is a closing tag, so the xml block ends here.
            rTag = false ;
            ///I.say("\nend xml block (" + current.tag + "). Going back to parent-") ;
            current = current.parent ;
          }
          else {
            ///I.say("\nnew xml block:") ;
            //a new xml block starts here.
            rVal = rAtt = false ;
            XML xml = new XML() ;
            xml.indexAsChild = current.childList.size() ;
            xml.parent = current ;
            current.childList.addLast(xml) ;
            current = xml ;
          }
        }
      }
      ///I.say("\n\n___xxxBEGIN XML COMPILATIONxxx___") ;
      compile() ;
      ///I.say("\n___xxxEND OF XML COMPILATIONxxx___\n\n") ;
    }
    catch(IOException e) {
      I.say("" + e) ;
    }
  }
  
  //  Simple helper method for reading a String between start and end indices:
  final private String readS(final byte chars[], final int s, final int e) {
    return new String(chars, s, e - s) ;
  }
	
  
  private XML() {}
  
  private static int indent = 0 ;
  final static boolean print = false ;
  
  /**  Transforms the temporary member lists into proper arrays.
    */
  final private void compile() {
    children = (XML[]) childList.toArray(XML.class) ;
    attributes = (String[]) attributeList.toArray(String.class) ;
    values = (String[]) valueList.toArray(String.class) ;
    if (children == null)
      children = new XML[0] ;
    if ((attributes == null) || (values == null))
      attributes = values = new String[0] ;
    if (print) {
      byte iB[] = new byte[indent] ;
      for (int n = indent ; n-- > 0 ;) iB[n] = ' ' ;
      String iS = new String(iB) ;
      I.say("" + iS + "tag name: " + tag) ;
      for (int n = 0 ; n < values.length ; n++)
        I.say("" + iS + "att/val pair: " + attributes[n] + " " + values[n]) ;
      I.say("" + iS + "tag content: " + content) ;
    }
    indent++ ;
    for (XML child : children) child.compile() ;
    indent-- ; 
  }
}


