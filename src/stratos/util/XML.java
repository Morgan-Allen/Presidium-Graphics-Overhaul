/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.util;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.*;

import java.io.*;

import stratos.graphics.common.*;
import stratos.start.Assets;



/**  In essence, an easier-going XML file/node, which constructs a hierarchy of
  *  tags, attributes, arguments and content from a given .xml file.
  */
public class XML {
  
  private static boolean
    verbose      = false,
    extraVerbose = false;
  
  public static void main(String args[]) {
    verbose = true;
    XML.load("media/Help/TutorialScript.xml");
  }
  
  
  protected XML
    parent,
    children[];
  protected int
    indexAsChild = -1;
  protected String
    tag,
    content,
    attributes[],
    values[];
  
  public String tag()       { return tag; }
  public String content()   { return content; }
  public int numChildren()  { return children.length; }
  public int indexAsChild() { return indexAsChild; }
  public XML child(int n)   { return children[n]; }
  public XML[] children()   { return children; }
  public XML parent()       { return parent; }
  
  
  /**  Returns the first child matching the given tag.
    */
  public XML child(String tag) {
    for (XML child : children) if (child.tag.equals(tag)) return child;
    return null;
  }

  /**  Returns an array of all children matching the given tag.
    */
  public XML[] allChildrenMatching(String tag) {
    Stack <XML> matches = new Stack <XML> ();
    for (XML child : children)
      if (child.tag.equals(tag)) matches.addLast(child);
    return (matches.size() > 0) ?
      (XML[]) matches.toArray(XML.class) :
      new XML[0];
  }
  
  /**  Returns the first child whose named attribute matches the given value.
    */
  public XML matchChildValue(String att, String value) {
    for (XML child : childList)
      if (child.value(att).equals(value))
        return child;
    return null;
  }
  
  
  /**  Returns this node's value for the given attribute (if present- null
    *  otherwise.)
    */
  public String value(String label) {
    for (int n = values.length; n-- > 0;)
      if (attributes[n].equals(label)) return values[n];
    return null;
  }
  
  
  public boolean getBool(String label) {
    final String val = value(label);
    return (val == null) ? false : Boolean.parseBoolean(val);
  }
  
  
  public float getFloat(String label) {
    final String val = value(label);
    return (val == null) ? 1 : Float.parseFloat(val);
  }
  
  
  public int getInt(String label) {
    return (int) getFloat(label);
  }
  
  
  
  /**  Returns the XML node constructed from the file with the given name.
    */
  public static XML load(String fileName) {
    Object cached = Assets.getResource(fileName);
    if (cached != null) return (XML) cached;
    final XML xml = new XML(fileName);
    Assets.cacheResource(xml, fileName);
    return xml;
  }
  
  
  //  Temporary member lists, to be discarded once setup is complete-
  private List <XML>
    childList = new List <XML> ();
  private List <String>
    attributeList = new List <String> (),
    valueList     = new List <String> ();
  
  /**  Constructs a new XML node from the given text file.
   */
  private XML(String xmlF) {
    final boolean report = extraVerbose;
    if (report) I.say("\nLoading XML from path: "+xmlF);
    
    try {
      XML current = this;
      boolean
        readsTag = false,  //reading an opening tag.
        readsAtt = false,  //reading a tag or attribute name.
        readsVal = false,  //reading an attribute value.
        readsCon = false;  //reading content between open and closing tags.
      int
        cRead = 0,  //index for  start of content reading.
        aRead = 0,  //attribute reading...
        vRead = 0,  //value reading...
        index,      //current index in file.
        length;     //total length of file.
      
      final File baseFile = new File(xmlF);
      final FileInputStream fR = new FileInputStream(baseFile);
      byte chars[] = new byte[length = (int) baseFile.length()];
      char read;
      fR.read(chars);
      
      for (index = 0; index < length; index++) {
        read = (char) chars[index];
        if (Character.isWhitespace((char) read)) read = ' ';
        if (report) I.say(" "+read);
        //
        //  If you're reading a tag or value:
        if (readsTag) {
          //
          //  If you're reading an attribute value:
          if (readsVal) {
            if (read == '"') {
              current.valueList.addLast(readS(chars, vRead, index));
              if (report) I.say("  Adding value: "+current.valueList.last());
              readsVal = false;
            }
            continue;
          }
          //  If you're reading an attribute or name tag:
          if (readsAtt) {
            switch(read) {
              case('='):
              case('>'):
              case(' '):
                if (current.tag == null) {
                  current.tag = readS(chars, aRead, index);
                  if (report) I.say("  Setting tag: "+current.tag);
                }
                else {
                  current.attributeList.addLast(readS(chars, aRead, index));
                  if (report) I.say(
                    "  Adding attribute: "+current.attributeList.last()+
                    " (No. "+current.attributeList.size()+")"
                  );
                }
                readsAtt = false;
                break;
            }
            if (readsAtt) continue;
          }
          //  Otherwise:
          switch(read) {
            
            case('"'):
              readsVal = true;
              vRead = index + 1;
              break;
            
            case('>'):
              if (chars[index - 1] == '/') {
                //this is a closed tag, so the xml block ends here.
                readsTag = false;
                if (report) I.say(
                  "  Closed tag ("+current.tag+"). Going back to parent-"
                );
                current = current.parent;
              }
              readsCon = readsTag;
              if (readsCon) cRead = index + 1;
              //this was an opening tag, so new content should be read.
              readsTag = false;
              break;
            
            case('='):
            case('/'):
            case(' '):
              //ignore these characters.
              break;
            
            default:
              //anything else would begin an attribute in a tag.
              //I.say("\nopening tag: ");
              readsAtt = true;
              aRead = index;
              break;
          }
          
          if (readsTag) continue;
        }
        
        if (read == '<') {
          //  An opening/closing tag begins...
          readsTag = true;
          
          if (readsCon) {
            if (report) I.say("  Adding content.");
            current.content = readS(chars, cRead, index);
            readsCon = false;
          }
          
          if (chars[index + 1] == '/') {
            //...this is a closing tag, so the xml block ends here.
            readsTag = false;
            if (report)  I.say(
              "  End xml block ("+current.tag+"). Going back to parent-"
            );
            current = current.parent;
          }
          else {
            if (report) I.say("  New xml block:");
            //a new xml block starts here.
            readsVal = readsAtt = false;
            XML xml = new XML();
            xml.indexAsChild = current.childList.size();
            xml.parent = current;
            current.childList.addLast(xml);
            current = xml;
          }
        }
      }
      if (report) I.say("\n___xxxBEGIN XML COMPILATIONxxx___");
      compile(0);
      fR.close();
      if (report) I.say("___xxxEND OF XML COMPILATIONxxx___\n");
    }
    catch(IOException e) {
      I.say("" + e);
    }
  }
  
  //  Simple helper method for reading a String between start and end indices:
  final private String readS(final byte chars[], final int s, final int e) {
    return new String(chars, s, e - s);
  }
	
  
  private XML() {}
  
  
  
  /**  Transforms the temporary member lists into proper arrays.
    */
  final private void compile(int depth) {
    children   = childList    .toArray(XML   .class);
    attributes = attributeList.toArray(String.class);
    values     = valueList    .toArray(String.class);
    
    if (children == null) {
      children = new XML[0];
    }
    if ((attributes == null) || (values == null)) {
      attributes = values = new String[0];
    }
    if (verbose) {
      final byte iB[] = new byte[depth * 2];
      for (int n = depth * 2; n-- > 0;) iB[n] = ' ';
      final String iS = new String(iB);
      
      I.say("\n"+iS+"Node Tag = \""+tag+"\"");
      for (int n = 0; n < values.length; n++) {
        I.say(iS+"  "+attributes[n]+" = \""+values[n]+"\"");
      }
      if (children.length == 0) I.say(iS+"  Content: "+content);
      else I.say(iS+"  Children: ");
    }
    for (XML child : children) child.compile(depth + 1);
  }
}






