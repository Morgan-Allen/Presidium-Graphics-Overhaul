


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;




public class Performance extends Recreation {
  
  
  /**  Data fields, setup and save/load functions-
    */
  final static String SONG_NAMES[] = {
    "Red Planet Blues, by Khal Segin & Tolev Zaller",
    "It's Full Of Stars, by D. B. Unterhaussen",
    "Take The Sky From Me, by Wedon the Elder",
    "Men Are From Asra Novi, by The Ryod Sisters",
    "Ode To A Hrexxen Gorn, by Ultimex 1450",
    "Geodesic Dome Science Rap, by Sarles Matson",
    "Stuck In The Lagrange Point With You, by Eniud Yi",
    "Untranslatable Feelings, by Strain Variant Beta-7J",
    "A Credit For Your Engram, by Tobul Masri Mark IV",
    "Where Everyone Knows Your Scent Signature, by The Imperatrix",
    "101111-00879938-11AA9191-101, by Lucinda O",
    "Pi Is The Loneliest Number, by Marec 'Irrational' Bel",
    "Zakharov And MG Go Go Go, by Natalya Morgan-Skye",
    "Procyon Nerve-Wipe Hymn, Traditional",
    "ALL HAIL THE MESMERFROG, by ALL HAIL THE MESMERFROG",
    "The Very Best of Mandolin Hero 2047 Karaoke"
  };
  final static String EROTICS_NAMES[] = {
    "Private Dance"
  };
  
  final static String MEDIA_NAMES[] = {
    "Centripetal Velocity: Mean Streets",
    "Citizen Taygeta, Part I & II",
    "Citizen Taygeta, Part III & IV",
    "The Krech and the Consort",
    "Kohan Nees Katzi, a Tragedy in 3 Acts",
    "The Sidereal Rites of Inobe"
  };
  final static String LARP_NAMES[] = {
    "Planets & Wormholes: 18th Edition",
    "Mandolin Hero: Sex, Sun & Soma",
    "My Life With The Overmind",
    "Psychic Raptor: Adventures in the 21st Century",
    "The Sleepy Village Xenomorph",
    "Burning Empires Historical Re-enactment"
  };
  final static String SPORT_NAMES[] = {
    "Dony Hoc's Ultra VR Surfing",
    "Zero G Kinetic Dance Workout",
    "Relativistic Track & Field",
    "Neutronium Endurance Lifting",
    "Battle Royale Magneto-Chess",
    "Team Terminator Tag Tournament"
  };
  final static String MEDITATE_NAMES[] = {
    "Meditation"
  };
  
  final static String[][] ALL_PERFORM_NAMES = {
    SONG_NAMES,
    EROTICS_NAMES,
    MEDIA_NAMES,
    LARP_NAMES,
    SPORT_NAMES,
    MEDITATE_NAMES
  };
  final static Skill PERFORM_SKILLS[][] = {
    { MUSIC_AND_SONG, EROTICS    },
    { EROTICS       , ATHLETICS  },
    { MASQUERADE, GRAPHIC_DESIGN },
    { MASQUERADE, HANDICRAFTS    },
    { ATHLETICS      , MUSCULAR  , IMMUNE   , MOTOR },
    { BODY_MEDITATION, PERCEPT, COGNITION, NERVE   }
  };
  
  final static String EFFECT_DESC[] = {
    "Terrible",
    "Poor",
    "Fair",
    "Good",
    "Excellent",
    "Rapturous",
  };
  
  private static boolean verbose = false;
  
  
  
  final Actor client;
  public int checkBonus;
  private float performValue = -1;
  private float timeSpent = 0;
  
  private int actID = -1;
  private Performance lead = null;
  
  
  public Performance(Actor actor, Boarding venue, int type, Actor client) {
    super(actor, venue, type);
    this.client = client;
  }
  
  
  public Performance(Session s) throws Exception {
    super(s);
    client = (Actor) s.loadObject();
    checkBonus = s.loadInt();
    performValue = s.loadFloat();
    timeSpent = s.loadFloat();
    actID = s.loadInt();
    lead = (Performance) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(client);
    s.saveInt(checkBonus);
    s.saveFloat(performValue);
    s.saveFloat(timeSpent);
    s.saveInt(actID);
    s.saveObject(lead);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Performance(other, venue, type, client);
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false;
    final Performance oP = (Performance) p;
    return oP.type == this.type && oP.client == this.client;
  }
  
  
  
  /**  Helper methods-
    */
  private void findLead() {
    for (Mobile m : venue.inside()) if (m instanceof Actor) {
      final Performance match = (Performance) ((Actor) m).matchFor(this);
      if (match == null) continue;
      lead = match;
      actID = lead.actID;
      performValue = lead.performValue + Rand.range(-2, 2);
      return;
    }
    lead = this;
    actID = Rand.index(ALL_PERFORM_NAMES[type].length);
    performValue = 5 + Rand.range(-2, 2);
  }
  
  
  public float performValue() {
    return performValue;
  }
  
  
  public String performDesc() {
    if (lead == null) findLead();
    return ALL_PERFORM_NAMES[type][actID];
  }
  
  
  public static String qualityDesc(float performValue) {
    String desc = EFFECT_DESC[Visit.clamp((int) (performValue / 2), 5)];
    return desc+" reception.";
  }
  
  
  
  /**  Static helper methods-
    */
  private boolean matches(Recreation r) {
    final int tA = r.type, tB = this.type;
    if (tA == Recreation.TYPE_ANY || tB == Recreation.TYPE_ANY) return true;
    if (client != null && client != r.actor()) return false;
    return tA == tB;
  }
  
  
  public static float performValueFor(Boarding venue, Recreation r) {
    float value = 0, count = 0;
    for (Performance p : performancesMatching(venue, r)) {
      value += p.performValue;
      count++;
    }
    ///I.say("Value/count: "+value+"/"+count+" "+r.type);
    
    if (count == 0) return -1;
    return (value + 1) / (1 + count);
  }
  
  
  public static Batch <Performance> performancesMatching(
    Boarding venue, Recreation r
  ) {
    final Batch <Performance> at = new Batch <Performance> ();
    final World world = ((Element) venue).world();
    //
    //  TODO:  This will have to match up with visitors instead.
    final Performance match = new Performance(null, venue, r.type, null);
    for (Mobile m : venue.inside()) if (m instanceof Actor) {
      final Actor a = (Actor) m;
      final Performance p = (Performance) a.matchFor(match);
      if (p != null) at.add(p);
    }
    return at;
  }
  
  
  public static Batch <Recreation> audienceFor(Boarding venue, Performance p) {
    final Batch <Recreation> audience = new Batch <Recreation> ();
    for (Mobile m : venue.inside()) if (m instanceof Actor) {
      final Actor a = (Actor) m;
      for (Behaviour b : a.mind.agenda()) if (b instanceof Recreation) {
        final Recreation r = (Recreation) b;
        if (p.matches(r)) { audience.add(r); break; }
      }
    }
    return audience;
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Trait BASE_TRAITS[] = { OUTGOING, CREATIVE };
  
  public float priorityFor(Actor actor) {
    if (expired()) return 0;
    final boolean report = verbose && I.talkAbout == actor;
    
    final float priority = priorityForActorWith(
      actor, venue, CASUAL,
      MILD_HELP, MILD_COOPERATION,
      PERFORM_SKILLS[type], BASE_TRAITS,
      NO_MODIFIER, NORMAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    return priority;
  }
  
  
  private boolean expired() {
    if (lead != this && lead.finished()) return true;
    return timeSpent > (PERFORM_TIME + Rand.index(PERFORM_TIME)) / 2;
  }
  
  
  protected Behaviour getNextStep() {
    if (lead == null) findLead();
    if (expired()) {
      //I.say(actor+" performance has expired...");
      return null;
    }
    if (client != null) {
      final Recreation r = new Recreation(client, venue, type);
      if (client.matchFor(r) == null) {
        final Action attend = new Action(
          actor, venue,
          this, "actionAttend",
          Action.TALK, "Attending"
        );
        return attend;
      }
    }
    final Action perform = new Action(
      actor, venue,
      this, "actionPerform",
      Action.TALK, "Performing"
    );
    return perform;
  }
  
  
  public boolean actionAttend(Actor actor, Venue venue) {
    final Recreation r = new Recreation(client, venue, type);
    if (client.mind.mustIgnore(r)) {
      abortBehaviour();
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
    performValue = Visit.clamp(performValue + effect, 0, 10);
    timeSpent++;
    //
    //  The actor may be entitled to some of the benefits of recreation in the
    //  process, assuming they have a decent relationship with the audience-
    final Recreation r = new Recreation(actor, venue, type);
    final Batch <Recreation> audience = audienceFor(venue, this);
    
    if (audience.size() > 0) {
      final Actor notice = ((Plan) Rand.pickFrom(audience)).actor();
      if (Rand.num() < actor.relations.relationValue(notice)) {
        super.actionRelax(actor, venue);
      }
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
    final Recreation r = new Recreation(null, venue, type);
    final Performance p = performancesMatching(venue, r).first();
    
    d.append("\n"+label+"\n  ");
    if (p == null) d.append("No performance.");
    else {
      d.append(p.performDesc());
      d.append("\n  ");
      final Batch <Recreation> audience = audienceFor(venue, p);
      if (audience.size() > 0) {
        final float reception = performValueFor(venue, r);
        d.append(Performance.qualityDesc(reception));
      }
      else d.append("No audience.");
    }
  }
}


