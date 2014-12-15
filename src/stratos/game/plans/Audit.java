/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.politic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Audit extends Plan {
  
  
  /**  Data fields, constructors and save/load functions-
    */
  public static enum Type {
    TYPE_OFFICIAL,
    TYPE_AMATEUR ,
    TYPE_EXTORTION
  }
  final static int
    STAGE_EVAL   = -1,
    STAGE_AUDIT  =  0,
    STAGE_REPORT =  1,
    STAGE_DONE   =  2;
  
  private static boolean verbose = false;
  
  
  private Type type;
  private int stage = STAGE_EVAL;
  
  private Venue audited;
  private float balance = 0;
  public int checkBonus = 0;
  
  
  public Audit(Actor actor, Venue toAudit, Type type) {
    super(actor, toAudit, true, NO_HARM);
    this.type    = type;
    this.audited = toAudit;
  }
  
  
  public Audit(Session s) throws Exception {
    super(s);
    type       = (Type) s.loadEnum(Type.values());
    stage      = s.loadInt();
    audited    = (Venue) s.loadObject();
    balance    = s.loadFloat();
    checkBonus = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveEnum  (type      );
    s.saveInt   (stage     );
    s.saveObject(audited   );
    s.saveFloat (balance   );
    s.saveInt   (checkBonus);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Audit(other, audited, type);
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public static Audit nextOfficialAudit(Actor actor) {
    
    final Stage world = actor.world();
    final Venue work = (Venue) actor.mind.work();
    final Batch <Venue> batch = new Batch <Venue> ();
    world.presences.sampleFromMap(work, world, 10, batch, work.base());
    
    final Pick <Venue> pick = new Pick <Venue> ();
    for (Venue v : batch) {
      float rating = Nums.abs(v.inventory().credits()) / 100f;
      rating -= Plan.rangePenalty(v, actor);
      rating -= Plan.competition(Audit.class, v, actor);
      pick.compare(v, rating);
    }
    
    if (pick.result() == null) return null;
    return new Audit(actor, pick.result(), Type.TYPE_OFFICIAL);
  }
  
  
  public static Venue nearestAdmin(Actor actor) {
    final Presences p = actor.world().presences;
    for (Object t : p.matchesNear(SERVICE_ADMIN, actor, -1)) {
      final Venue v = (Venue) t;
      if (v.base() == actor.base()) return v;
    }
    return null;
  }
  
  
  protected float getPriority() {
    final float credits = audited.inventory().credits();
    float modifier = Nums.clamp(credits / 100, -ROUTINE, ROUTINE);
    
    if (type == Type.TYPE_EXTORTION) {
      if (credits <= 0) return 0;
      modifier *= 2;
      modifier -= ROUTINE;
    }
    return (ROUTINE + modifier) / 2;
  }
  
  

  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("Getting next audit step... "+this.hashCode());
    if (report) I.say("Stage was: "+stage);
    
    if (stage == STAGE_EVAL) {
      if (audited == null && Rand.num() > (Nums.abs(balance) / 1000f)) {
        final Behaviour next = actor.mind.work().jobFor(actor);
        if (next instanceof Audit) audited = ((Audit) next).audited;
      }
      stage = (audited == null) ? STAGE_REPORT : STAGE_AUDIT;
    }
    if (report) I.say("Stage is now: "+stage+", audited: "+audited);
    
    if (stage == STAGE_AUDIT) {
      final Action audit = new Action(
        actor, audited,
        this, "actionAudit",
        Action.TALK, "Auditing "+audited
      );
      final Tile e = audited.mainEntrance();
      if (e == null || e.blocked()) {
        audit.setMoveTarget(Spacing.nearestOpenTile(audited.origin(), actor));
      }
      return audit;
    }
    if (stage == STAGE_REPORT) {
      final Action files = new Action(
        actor, actor.mind.work(),
        this, "actionFileReport",
        Action.TALK, "Filing Report"
      );
      return files;
    }
    return null;
  }
  
  
  public boolean actionAudit(Actor actor, Venue audited) {
    final float balance = auditForBalance(actor, audited, true);
    this.balance += balance;
    stage = STAGE_EVAL;
    this.audited = null;
    return true;
  }
  
  
  public boolean actionFileReport(Actor actor, Venue office) {
    fileEarnings(actor, office, balance);
    stage = STAGE_DONE;
    return true;
  }
  
  
  public static void fileEarnings(Actor actor, Venue office, float balance) {
    final Base base = office.base();
    
    //  TODO:  The source-listing needs to be made a lot more specific!
    if (balance > 0) {
      base.finance.incCredits(balance, BaseFinance.SOURCE_TAXES);
      office.chat.addPhrase((int) balance+" credits in profit");
    }
    if (balance < 0) {
      base.finance.incCredits(balance, BaseFinance.SOURCE_WAGES);
      office.chat.addPhrase((int) (0 - balance)+" credits in debt");
    }
  }
  
  
  public static float auditForBalance(
    Actor audits, Venue venue, boolean deductSums
  ) {
    final boolean report = verbose && I.talkAbout == audits;
    if (venue.base() == null) return 0;
    if (report) I.say("\n+"+audits+" auditing "+venue);
    
    float sumWages = 0, sumSalaries = 0;//, sumSplits = 0;
    
    final BaseProfiles BP = venue.base().profiles;
    final int numW = venue.personnel.workers().size();
    final Profile profiles[] = new Profile[numW];
    final float salaries[] = new float[numW];
    
    int i = 0; for (Actor works : venue.personnel.workers()) {
      final Profile p = BP.profileFor(works);
      profiles[i] = p;
      //final int KR = BP.querySetting(AuditOffice.KEY_RELIEF);
      
      final float
        salary = p.salary(),
        //relief = AuditOffice.RELIEF_AMOUNTS[KR],
        payInterval = p.daysSincePayment(venue.world()),
        wages = (salary / Backgrounds.NUM_DAYS_PAY) * payInterval;
      
      if (report) {
        I.say("  "+works+" is due: "+wages+" over "+payInterval+" days");
        I.say("  Salary: "+salary);
        I.say("  Wages already due: "+p.paymentDue());
      }
      
      salaries[i++] = salary;
      p.incPaymentDue(wages);
      sumWages += wages;
      sumSalaries += salary;
    }

    final float surplus = venue.stocks.credits();
    
    if (verbose && I.talkAbout == audits) {
      I.say(audits+" auditing "+venue+", surplus: "+surplus);
      I.say("Sum of salaries is: "+sumSalaries);
    }
    
    i = 0;
    if (surplus > 0 && sumSalaries > 0) for (Profile p : profiles) {
      float split = salaries[i++] * surplus / sumSalaries;
      split *= Backgrounds.DEFAULT_SURPLUS_PERCENT / 100f;
      if (report) I.say("  "+p.actor+" getting profit bonus: "+split);
      p.incPaymentDue(split);
      sumWages += split;
    }

    if (report) I.say("  Balance is now: "+venue.stocks.credits());
    
    if (deductSums) {
      venue.stocks.incCredits(0 - sumWages);
      final float balance = venue.stocks.credits();
      venue.stocks.incCredits(0 - balance);
      venue.stocks.taxDone();
      return balance;
    }
    else {
      return venue.stocks.credits() - sumWages;
    }
  }
  
  
  public static float taxesDue(Actor actor) {
    if (actor.base().primal) return 0;
    //  TODO:  factor in social class in some way.
    //final int bracket = actor.vocation().standing;
    final int percent = Backgrounds.DEFAULT_TAX_PERCENT;
    
    float afterSaving = actor.gear.credits();
    if (afterSaving < 0) return 0;
    afterSaving = Nums.min(afterSaving, actor.gear.unTaxed());
    return afterSaving * percent / 100f;
  }
  
  
  public static float propertyValue(Venue venue) {
    float value = 0;
    if (venue instanceof Holding) {
      final Holding home = (Holding) venue;
      value += home.upgradeLevel() * 25;
      value += home.structure.buildCost();
    }
    return value;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (stage == STAGE_AUDIT && audited != null) {
      d.append("Auditing: ");
      d.append(audited);
      d.append(" (Balance "+(int) balance+")");
    }
    else if (stage == STAGE_REPORT) {
      d.append("Filing a financial report at ");
      d.append(actor.mind.work());
      d.append(" (Balance "+(int) balance+")");
    }
    else d.append("Auditing "+audited);
  }
}






    
    //  TODO:  Allow qualified auditors to perform automatic currency
    //  adjustments as part of this behaviour!
    
    /*
    final float balance = venue.stocks.credits();
    venue.stocks.incCredits(0 - Count.max(balance, sumWages));
    venue.stocks.taxDone();
    I.sayAbout(venue, "Balance is now: "+venue.stocks.credits());
    return balance;
    //*/
    //  TODO:  Restore this in some form later.
    /*
    float
      balance = venue.stocks.credits(),
      waste = (Rand.num() + base.crimeLevel()) / 2f;
    
    final float honesty =
      (audits.traits.traitLevel(HONOURABLE) / 2f) -
      (audits.traits.traitLevel(ACQUISITIVE) * waste);
    
    if (Rand.num() > (honesty + 1f) / 2) {
      waste *= 1.5f;
      final float bribe = BASE_BRIBE_SIZE * waste;
      balance -= bribe;
      audits.gear.incCredits(bribe);
    }
    else {
      if (audits.skills.test(ACCOUNTING, 15, 5)) waste  = 0;
      if (audits.skills.test(ACCOUNTING, 5 , 5)) waste /= 2;
    }
    final int
      profit = (int) (balance / (1 + waste)),
      losses = (int) (balance * (1 + waste));
    
    
    if (profit > 0) {
      venue.stocks.incCredits(0 - profit);
      venue.stocks.taxDone();
      return profit;
    }
    if (losses < 0) {
      venue.stocks.incCredits(0 - losses);
      venue.stocks.taxDone();
      return losses;
    }
    venue.stocks.taxDone();
    return 0;
    //*/





