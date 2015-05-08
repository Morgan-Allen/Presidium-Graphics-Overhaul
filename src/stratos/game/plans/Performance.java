/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Performance extends Recreation {
  
  
  /**  Data fields, setup and save/load functions-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  public static interface Theatre {
    String[] namesForPerformance(int type);
  }
  
  final static String EFFECT_DESC[] = {
    "Terrible",
    "Poor",
    "Fair",
    "Good",
    "Excellent",
    "Rapturous",
  };
  final static String DEFAULT_NAMES[] = {
    "Song", "Erotics", "Media Display", "LARP", "Sport", "Meditation"
  };
  final static Skill PERFORM_SKILLS[][] = {
    { MUSIC_AND_SONG, EROTICS    },
    { EROTICS       , ATHLETICS  },
    { MASQUERADE, GRAPHIC_DESIGN },
    { MASQUERADE, HANDICRAFTS    },
    { ATHLETICS      , MUSCULAR, IMMUNE   , MOTOR },
    { BODY_MEDITATION, PERCEPT , COGNITION, NERVE }
  };
  
  
  final Actor client;
  public int checkBonus;
  private float performValue = -1;
  private float timeSpent = 0;
  
  private int actID = -1;
  private Performance lead = null;
  
  
  public Performance(
    Actor actor, Venue venue, int type, Actor client, float cost
  ) {
    super(actor, venue, type, cost);
    this.client = client;
  }
  
  
  public Performance(Session s) throws Exception {
    super(s);
    client       = (Actor) s.loadObject();
    checkBonus   = s.loadInt();
    performValue = s.loadFloat();
    timeSpent    = s.loadFloat();
    actID        = s.loadInt();
    lead         = (Performance) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(client      );
    s.saveInt   (checkBonus  );
    s.saveFloat (performValue);
    s.saveFloat (timeSpent   );
    s.saveInt   (actID       );
    s.saveObject(lead        );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Performance(other, venue, type, client, cost);
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    final Performance oP = (Performance) p;
    return oP.type == this.type && oP.client == this.client;
  }
  
  
  
  /**  Helper methods-
    */
  private void findLead() {
    if (lead != null) return;
    
    for (Mobile m : venue.inside()) if (m instanceof Actor) {
      final Performance match = (Performance) ((Actor) m).matchFor(this);
      if (match == null || match.finished()) continue;
      lead = match;
      actID = lead.actID;
      performValue = lead.performValue + Rand.range(-2, 2);
      return;
    }
    lead = this;
    
    if (venue instanceof Theatre) {
      final String names[] = ((Theatre) venue).namesForPerformance(type);
      actID = names == null ? -1 : Rand.index(names.length);
    }
    
    performValue = 5 + Rand.range(-2, 2);
  }
  
  
  public float performValue() {
    return performValue;
  }
  
  
  public String performDesc() {
    findLead();
    if (actID == -1) {
      return DEFAULT_NAMES[type];
    }
    else {
      final String names[] = ((Theatre) venue).namesForPerformance(type);
      return names[actID];
    }
  }
  
  
  public static String qualityDesc(float performValue) {
    String desc = EFFECT_DESC[Nums.clamp((int) (performValue / 2), 5)];
    return desc+" reception.";
  }
  
  
  
  /**  Static helper methods-
    */
  public static float performValueFor(Venue venue, Recreation r) {
    float value = 0, count = 0;
    for (Performance p : performancesMatching(venue, r)) {
      if (p.client == r.actor()) value += p.performValue * 5;
      else value += p.performValue;
      count++;
    }
    if (count == 0) return -1;
    return (value + 1) / (1 + count);
  }
  
  
  public static Batch <Performance> performancesMatching(
    Venue venue, Recreation r
  ) {
    final Batch <Performance> at = new Batch <Performance> ();
    for (Actor a : venue.staff.visitors()) {
      final Performance p = (Performance) a.matchFor(Performance.class, true);
      if (p == null || (r.type != TYPE_ANY && p.type != r.type)) continue;
      if (p.client != null && p.client != r.actor()) continue;
      at.add(p);
    }
    return at;
  }
  
  
  public static Batch <Actor> audienceFor(Venue venue, int performType) {
    final Batch <Actor> audience = new Batch <Actor> ();
    for (Actor a : venue.staff.visitors()) {
      final Recreation r = (Recreation) a.matchFor(Recreation.class, true);
      if (r != null && r.type == performType) audience.add(a);
    }
    return audience;
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Trait BASE_TRAITS[] = { OUTGOING, CREATIVE };
  
  public float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor && hasBegun();
    if (expired()) {
      if (report) {
        I.say("\nPerformance expired, ID: "+hashCode());
        if (lead != this) {
          I.say("  Lead ID: "+lead.hashCode());
          I.say("  Lead finished? "+lead.finished());
        }
        I.say("  Time spent: "+timeSpent);
      }
      return 0;
    }
    
    final float priority = priorityForActorWith(
      actor, venue,
      CASUAL, NO_MODIFIER,
      MILD_HELP, MILD_COOPERATION, NO_FAIL_RISK,
      PERFORM_SKILLS[type], BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    return priority;
  }
  
  
  private boolean expired() {
    findLead();
    if (lead != this && lead.finished()) return true;
    return timeSpent > PERFORM_TIME;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor && hasBegun();
    if (report) I.say("\nGetting next performance step for "+actor);
    
    if (expired()) {
      if (report) I.say("  Performance has expired!");
      return null;
    }
    
    if (client != null) {
      final Recreation r = new Recreation(client, venue, type, cost);
      if (client.matchFor(r) == null) {
        if (report) I.say("  Attending to client: "+client);
        
        final Action attend = new Action(
          actor, venue,
          this, "actionAttend",
          Action.TALK, "Attending"
        );
        return attend;
      }
    }
    
    if (report) I.say("  Playing new chord...");
    final Action perform = new Action(
      actor, venue,
      this, "actionPerform",
      Action.TALK, "Performing"
    );
    return perform;
  }
  
  
  public boolean actionAttend(Actor actor, Venue venue) {
    final Recreation r = new Recreation(client, venue, type, cost);
    if (client.mind.mustIgnore(r)) {
      interrupt(INTERRUPT_CANCEL);
      return false;
    }
    client.mind.assignBehaviour(r);
    return true;
  }
  
  
  public boolean actionPerform(Actor actor, Venue venue) {
    //
    //  Firstly, calculate the effectiveness of the actor's own performance-
    float effect = 0;
    for (Skill s : PERFORM_SKILLS[type]) {
      final int DC = s == PERFORM_SKILLS[type][0] ? MODERATE_DC : ROUTINE_DC;
      final boolean success = actor.skills.test(s, DC - checkBonus, 1);
      if (success) effect++;
      else if (Rand.yes()) effect--;
    }
    performValue = Nums.clamp(performValue + effect, 0, 10);
    timeSpent += (1 + Rand.index(2)) / 1.5f;
    //
    //  The actor may be entitled to some of the benefits of recreation in the
    //  process, assuming they have a decent relationship with the audience-
    final Actor notice;
    if (client != null) notice = client;
    else {
      final Batch <Actor> audience = audienceFor(venue, type);
      notice = (Actor) Rand.pickFrom(audience);
    }
    if (notice != null && Rand.num() < actor.relations.valueFor(notice)) {
      Recreation r = (Recreation) notice.matchFor(Recreation.class, true);
      if (r == null) return true;
      final float interval = 1f / ENJOY_TIME;
      float comfort = Recreation.rateComfort(venue, notice, r);
      comfort += notice.health.moraleLevel();
      comfort *= interval;
      actor.health.adjustMorale(comfort);
    }
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    d.append("Performing "+performDesc());
    if (client != null) { d.append(" for "); d.append(client); }
    else { d.append(" at "); d.append(venue); }
  }
  
  
  public static void describe(
    Description d, String label, int type, Venue venue
  ) {
    final Recreation r = new Recreation(null, venue, type, -1);
    final Performance p = performancesMatching(venue, r).first();
    
    d.append("\n"+label+"\n  ");
    if (p == null) d.append("No performance.");
    else {
      d.append(p.performDesc());
      d.append("\n  ");
      final Batch <Actor> audience = audienceFor(venue, p.type);
      if (audience.size() > 0) {
        final float reception = performValueFor(venue, r);
        d.append(Performance.qualityDesc(reception));
      }
      else d.append("No audience.");
    }
  }
}


