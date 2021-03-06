/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



public class Audit extends Plan {
  
  
  /**  Data fields, constructors and save/load functions-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  public static enum Type {
    TYPE_OFFICIAL ,
    TYPE_AMATEUR  ,
    TYPE_EXTORTION,
  }
  final static int
    STAGE_EVAL   = -1,
    STAGE_AUDIT  =  0,
    STAGE_REPORT =  1,
    STAGE_DONE   =  2;
  
  final static int
    RATE_DIVISOR  = 25,
    MAX_VISITS    = 5 ;
  
  
  final Type type;
  final Venue reportAt;
  
  private int stage = STAGE_EVAL;
  private Property audited;
  public int checkBonus = 0;
  private int numVisits = 0;
  
  private Tally <Property>
    perVenue = new Tally();
  private float
    expenses ,
    income   ,
    taxesPaid,
    wagesPaid,
    embezzled,
    totalSum ;
  
  
  private Audit(Actor actor, Property toAudit, Venue reportAt, Type type) {
    super(actor, toAudit, MOTIVE_JOB, NO_HARM);
    this.type     = type;
    this.audited  = toAudit;
    this.reportAt = reportAt;
  }
  
  
  public Audit(Session s) throws Exception {
    super(s);
    type       = (Type ) s.loadEnum(Type.values());
    reportAt   = (Venue) s.loadObject();
    
    stage      = s.loadInt();
    audited    = (Venue) s.loadObject();
    checkBonus = s.loadInt();
    numVisits  = s.loadInt();
    
    for (int n = s.loadInt(); n-- > 0;) {
      final Property v = (Property) s.loadObject();
      final float p = s.loadFloat();
      perVenue.add(p, v);
    }
    
    expenses   = s.loadFloat();
    income     = s.loadFloat();
    taxesPaid  = s.loadFloat();
    wagesPaid  = s.loadFloat();
    embezzled  = s.loadFloat();
    totalSum   = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveEnum  (type      );
    s.saveObject(reportAt  );
    
    s.saveInt   (stage     );
    s.saveObject(audited   );
    s.saveInt   (checkBonus);
    s.saveInt   (numVisits );
    
    s.saveInt(perVenue.size());
    for (Property v : perVenue.keys()) {
      s.saveObject(v);
      s.saveFloat(perVenue.valueFor(v));
    }
    
    s.saveFloat (expenses  );
    s.saveFloat (income    );
    s.saveFloat (taxesPaid );
    s.saveFloat (wagesPaid );
    s.saveFloat (embezzled );
    s.saveFloat (totalSum  );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Audit(other, audited, reportAt, type);
  }
  
  
  
  /**  Detecting corruption-
    */
  //  TODO:  Your minister for finance (or analyst) could do this as well, once
  //  reports arrive back at the bastion.  Of course, they can also be bribed...
  
  //  TODO:  Only make this crime catch-able through another auditor's checks-
  //         (which is what makes a high accounting skill valuable.)
  public static boolean checkForEmbezzlement(
    Behaviour doing, Actor audits, boolean casual, Action taken
  ) {
    if (! (doing instanceof Audit)) return false;
    final Audit suspect = (Audit) doing;
    if (suspect.embezzled <= 0) return false;
    
    final boolean seen =
      suspect.actor().actionInProgress() &&
      audits.senses.awareOf(suspect.actor());
    if (casual && ! seen) return false;
    
    float DC = casual ? 20 : (seen ? 5 : 10);
    DC -= suspect.embezzled / 10f;
    if (audits.skills.test(ACCOUNTING, DC, 1.0f, taken)) return true;
    
    return false;
  }
  
  
  public static boolean payIncomeTax(Actor actor, Venue home) {
    if (actor.base().isPrimal() || actor.mind.vocation().defaultSalary == 0) {
      return false;
    }
    
    float baseSumDue = Nums.min(actor.gear.allCredits(), actor.gear.unTaxed());
    if (baseSumDue <= 0) return false;
    
    final int standing     = actor.mind.vocation().standing;
    final int percent      = Backgrounds.DEFAULT_TAX_PERCENTS[standing];
    final int upgradeLevel = HoldingUpgrades.upgradeLevelOf(home);
    final int homeTaxBonus = Backgrounds.HOUSE_LEVEL_TAX_BONUS[upgradeLevel];
    
    final Profile p = actor.base().profiles.profileFor(actor);
    final float daysTax    = p.daysSinceSinceTaxAssessed();
    final float actorPays  = baseSumDue * percent / 100f;
    final float taxBonus   = homeTaxBonus * daysTax / Backgrounds.NUM_DAYS_PAY;
    
    actor.gear.incCredits(0 - actorPays);
    home.stocks.incCredits(actorPays + taxBonus);
    actor.gear.taxDone();
    p.clearTax();
    return true;
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
  
  
  public static Audit nextAmateurAudit(Actor actor) {
    final Property work = actor.mind.work();
    if (! (work instanceof Venue)) return null;
    return new Audit(actor, work, (Venue) work, Type.TYPE_AMATEUR);
  }
  
  
  public static Audit nextOfficialAudit(Actor actor, Venue... venues) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nFinding next venue to audit for "+actor);
    
    final Venue work = (Venue) actor.mind.work();
    if (work == null) return null;
    
    final Stage world = actor.world();
    final Batch <Venue> batch = new Batch <Venue> ();
    
    if (venues == null || venues.length == 0) {
      world.presences.sampleFromMap(work, world, 10, batch, work.base());
      batch.add(work);
      for (Target t : actor.senses.awareOf()) {
        if (! (t instanceof Venue && t.base() == work.base())) continue;
        batch.add((Venue) t);
      }
    }
    else Visit.appendTo(batch, venues);
    
    final Pick <Venue> pick = new Pick <Venue> (null, 0);
    for (Venue v : batch) {
      if (PlanUtils.competition(Audit.class, v, actor) > 0) continue;
      final float cash = v.inventory().unTaxed();
      float rating = (Nums.abs(cash) + Nums.max(0, cash)) / RATE_DIVISOR;
      rating *= 2 / (2 + PlanUtils.homeDistanceFactor(actor, v));
      pick.compare(v, rating);
      if (report) {
        I.say("  Rating for "+v+" is "+rating);
        I.say("    Current balance: "+v.inventory().allCredits());
        I.say("    Untaxed:         "+v.inventory().unTaxed());
      }
    }
    if (pick.result() == null) return null;
    return new Audit(actor, pick.result(), work, Type.TYPE_OFFICIAL);
  }
  
  
  public static Audit nextExtortionAudit(Actor actor, Venue venue) {
    final Venue work = (Venue) actor.mind.work();
    return new Audit(actor, work, venue, Type.TYPE_EXTORTION);
  }
  
  
  public static Venue nearestAdmin(Actor actor) {
    final Presences p = actor.world().presences;
    for (Object t : p.matchesNear(SERVICE_ADMIN, actor, -1)) {
      final Venue v = (Venue) t;
      if (v.base() == actor.base()) return v;
    }
    return null;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor && hasBegun();
    if (report) {
      I.say("\nGetting priority for audit of "+audited+" by "+actor);
    }
    
    float credits = Nums.abs(totalSum);
    if (audited != null) credits += Nums.abs(audited.inventory().unTaxed());
    float modifier = Nums.clamp(credits / RATE_DIVISOR, -CASUAL, PARAMOUNT);
    
    if (type == Type.TYPE_EXTORTION) {
      if (credits <= 0) return 0;
      modifier *= 2;
      modifier -= ROUTINE;
    }
    
    final float priority = (ROUTINE + modifier) / 2;
    if (report) I.say("  Final priority: "+priority);
    return priority;
  }
  
  

  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next audit step: "+actor);
    
    if (stage == STAGE_EVAL) {
      if (
        audited == null && type == Type.TYPE_OFFICIAL &&
        numVisits < MAX_VISITS
      ) {
        final Audit next = nextOfficialAudit(actor);
        audited = next == null ? null : next.audited;
      }
      if (report) I.say("  Finding next venue to audit: "+audited);
      stage = (audited == null) ? STAGE_REPORT : STAGE_AUDIT;
    }
    
    if (stage == STAGE_AUDIT) {
      if (report) {
        I.say("  Will perform audit.");
        I.say("  Current credits: "+audited.inventory().allCredits());
      }
      final Action audit = new Action(
        actor, audited,
        this, "actionAudit",
        Action.TALK, "Auditing "+audited
      );
      final Boarding canBoard[] = audited.canBoard();
      if (canBoard == null || canBoard.length == 0) {
        audit.setMoveTarget(Spacing.nearestOpenTile(audited, actor));
      }
      return audit;
    }
    
    if (stage == STAGE_REPORT) {
      if (report) I.say("  Reporting earnings at: "+reportAt);
      final Action files = new Action(
        actor, reportAt,
        this, "actionFileReport",
        Action.TALK, "Filing Report"
      );
      return files;
    }
    return null;
  }
  
  
  public boolean actionAudit(Actor actor, Property audited) {
    performAudit();
    numVisits++;
    
    if (type == Type.TYPE_AMATEUR) {
      actor.gear.incCredits(embezzled);
      stage = STAGE_DONE;
    }
    else {
      final float balance = audited.inventory().allCredits();
      audited.inventory().incCredits(0 - balance);
      audited.inventory().taxDone();
      stage = (audited == reportAt) ? STAGE_REPORT : STAGE_EVAL;
      this.audited = null;
    }
    return true;
  }
  
  
  public boolean actionFileReport(Actor actor, Venue office) {
    final Base base = office.base();
    actor.gear.incCredits(embezzled);
    base.finance.incCredits(    income   , BaseFinance.SOURCE_BIZ_IN );
    base.finance.incCredits(0 - expenses , BaseFinance.SOURCE_BIZ_OUT);
    base.finance.incCredits(    taxesPaid, BaseFinance.SOURCE_TAXES  );
    base.finance.incCredits(0 - wagesPaid, BaseFinance.SOURCE_WAGES  );
    base.finance.recordVenueBalances(perVenue);
    this.totalSum = 0;
    stage = STAGE_DONE;
    return true;
  }
  
  
  private float performAudit() {
    final boolean report = stepsVerbose && (
      I.talkAbout == actor || I.talkAbout == audited
    );
    if (audited == null) return 0;
    final Base base = audited.base();
    if (report) I.say("\n"+actor+" auditing "+audited);
    //
    //  Our first step is to get the total sum of wages to pay to each worker
    //  associated with this venue.
    final int numW = audited.staff().workers().size();
    final Profile profiles[] = new Profile[numW];
    final float salaries[] = new float[numW];
    float sumWages = 0, sumSalaries = 0;
    int i = 0;
    
    for (Actor works : audited.staff().workers()) {
      final Profile p = base.profiles.profileFor(works);
      profiles[i] = p;
      final float
        salary      = p.salary(),
        payInterval = p.daysSinceWageAssessed(),
        wages       = salary * payInterval / Backgrounds.NUM_DAYS_PAY;
      
      if (report) {
        I.say("    "+works+" is due: "+wages+" over "+payInterval+" days");
        I.say("    Salary: "+salary);
        I.add("  Wages already due: "+p.wagesDue());
      }
      salaries[i++] = salary;
      p.incWagesDue(wages);
      sumWages    += wages ;
      sumSalaries += salary;
    }
    //
    //  Employees are also entitled to a share of any surplus generated by the
    //  business.  However, this split is NOT recorded as wages for accounting
    //  purposes- it simply reduces the balance of profit.
    float balance = audited.inventory().allCredits();
    final boolean isPrivate = audited.owningTier() == Owner.TIER_PRIVATE;
    if (report) {
      I.say("  "+actor+" auditing "+audited+", initial balance: "+balance);
      I.say("  Sum of salaries is: "+sumSalaries);
    }
    i = 0;
    if (balance > 0 && sumSalaries > 0) for (Profile p : profiles) {
      float split = salaries[i++] * balance / sumSalaries;
      split *= Backgrounds.DEFAULT_SURPLUS_PERCENT / 100f;
      if (report) I.say("  "+p.actor+" getting profit bonus: "+split);
      p.incWagesDue(split);
      balance -= split;
    }
    //
    //  Any balance remaining is recorded as 'business income' (or taxation in
    //  the case of housing), while any deficit is recorded as 'business
    //  expense' (or extra wages in the case of housing- e.g, for servants.)
    if (balance >= 0) {
      //  TODO:  Insert the embezzle check earlier?
      if (Rand.num() > (actor.traits.traitLevel(LOYAL) + 1) / 2f) {
        float taken = balance * Backgrounds.DEFAULT_EMBEZZLE_PERCENT / 100f;
        this.embezzled += taken;
        balance -= taken;
      }
      if (isPrivate) this.taxesPaid += balance;
      else this.income += balance;
    }
    else {
      if (isPrivate) this.wagesPaid += 0 - balance;
      else this.expenses += 0 - balance;
    }
    //
    //  TODO:  Leave some cash in reserve for purchases, based on salary size?
    //  Otherwise, we tally up the total and adjust cash reserves.
    this.wagesPaid += sumWages;
    balance -= sumWages;
    this.totalSum += balance;
    this.perVenue.add(balance, audited);
    return balance;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (stage == STAGE_AUDIT && audited != null) {
      d.append("Auditing: ");
      d.append(audited);
      d.append(" (Balance "+(int) totalSum+")");
    }
    else if (stage == STAGE_REPORT) {
      d.append("Filing a financial report at ");
      d.append(reportAt);
      d.append(" (Balance "+(int) totalSum+")");
    }
    else d.append("Auditing "+audited);
  }
}



