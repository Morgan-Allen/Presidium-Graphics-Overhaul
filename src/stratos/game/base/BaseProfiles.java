


package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.util.*;
import static stratos.game.base.LawUtils.*;



public class BaseProfiles {
  
  
  /**  Data fields, construction and save/load methods:
    */
  final public Base base;
  Table <Actor, Profile> allProfiles = new Table <Actor, Profile> (1000);
  Table <String, Integer> allSettings = new Table <String, Integer> (100);
  
  
  public BaseProfiles(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final Profile p = Profile.loadProfile(s);
      allProfiles.put(p.actor, p);
    }
    for (int n = s.loadInt(); n-- > 0;) {
      final String key = s.loadString();
      final int value = s.loadInt();
      allSettings.put(key, value);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(allProfiles.size());
    for (Profile p : allProfiles.values()) {
      Profile.saveProfile(p, s);
    }
    s.saveInt(allSettings.size());
    for (String key : allSettings.keySet()) {
      s.saveString(key);
      s.saveInt(allSettings.get(key));
    }
  }
  
  
  
  /**  Citizen profiles, including crime and punishment:
    */
  public Profile profileFor(Actor actor) {
    Profile match = allProfiles.get(actor);
    if (match == null) {
      match = new Profile(actor, this);
      allProfiles.put(actor, match);
    }
    return match;
  }
  
  
  public Summons sentenceFor(Actor actor) {
    final Profile match = allProfiles.get(actor);
    if (match == null) return null;
    return match.openSentence();
  }
  
  
  public List <Crime> crimesBy(Actor actor) {
    final Profile match = allProfiles.get(actor);
    if (match == null) return NO_CRIMES;
    return match.offences;
  }
  
  
  
  /**  General base-wide settings, such as legislation, import quotas, or tax
    *  rates:
    */
  public void assertSetting(String key, int value) {
    allSettings.put(key, value);
  }
  
  
  public int querySetting(String key) {
    final Integer value = allSettings.get(key);
    return value == null ? 0 : (int) value;
  }
  
  
  public int querySetting(String key, int defaultValue) {
    final Integer value = allSettings.get(key);
    if (value == null) {
      assertSetting(key, defaultValue);
      return defaultValue;
    }
    return (int) value;
  }
}














