/**  
  *  Written by Jaroslaw Jedynak
  *  
  *  Feel free to poke around for non-commercial purposes.
  */
package stratos.test;
import stratos.start.*;
import stratos.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;



public class AutomatedTestRunner {
  
  
  static Stack<TestCase> testCases;
  static TestCase currentTest;
  static AutomatedScenario currentScenario;
  static FileWriter log;
  
  
  public static void setupAutomatedTest(Stack<TestCase> testCases, String logFileName) {
    try {
      log = new FileWriter(logFileName, true);
    } catch (IOException e) {
      I.complain(e.getMessage());
    }

    AutomatedTestRunner.testCases = testCases;
    moveToNextTestCase();
  }
  
  
  private static void moveToNextTestCase() {
    currentTest = testCases.removeFirst();

    if (currentTest == null) {
      logToFile("Testing finished");
      PlayLoop.exitLoop();
      return;
    } else {
      logToFile("Moving to next test");
    }

    currentScenario = currentTest.initScenario();
    PlayLoop.setupAndLoop(currentScenario);
  }
  
  
  private static String getCurrentTimeStamp() {
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
    Date now = new Date();
    String strDate = sdfDate.format(now);
    return strDate;
  }
  
  
  private static void logToFile(String message) {
    try {
      I.say(message);
      log.write(getCurrentTimeStamp() + " " + message + "\n");
      log.flush();
    } catch (IOException e) {
      I.complain(e.getMessage());
    }
  }
  
  
  public static void testSucceeded(TestCase caseRun) {
    logToFile("Test succeeded: "+caseRun.caseName);
    moveToNextTestCase();
  }
  

  public static void testFailed(TestCase caseRun, String reason) {
    logToFile("Test failed: "+caseRun.caseName+" Reason: "+reason);
    moveToNextTestCase();
  }
}
