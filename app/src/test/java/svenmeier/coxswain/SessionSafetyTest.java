package svenmeier.coxswain;
import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.lang.reflect.*;
import propoid.db.Repository;
import svenmeier.coxswain.gym.*;
import static org.junit.Assert.*;
/** Regression coverage for session transitions, interval boundaries and portable display preferences. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class SessionSafetyTest {
 Context context; Gym gym;
 @Before public void setup() throws Exception {
  context=RuntimeEnvironment.getApplication(); context.deleteDatabase("gym");
  Constructor<Gym> ctor=Gym.class.getDeclaredConstructor(Context.class);ctor.setAccessible(true);
  gym=ctor.newInstance(context);gym.initialize();
 }
 @After public void cleanup() throws Exception {
  Field f=Gym.class.getDeclaredField("repository");f.setAccessible(true);((Repository)f.get(gym)).close();context.deleteDatabase("gym");
 }
 Measurement m(int seconds,int meters) { Measurement m=new Measurement();m.setDuration(seconds);m.setDistance(meters);return m; }
 @Test public void replacementPreservesOldRowInHistoryAndBackup() {
  gym.select(Program.meters("Old row",5000,Difficulty.MEDIUM));gym.onMeasured(m(60,250));
  Workout old=gym.current;
  gym.startFreeRow();
  assertNull(gym.current);
  assertEquals(WorkoutStatus.ENDED_EARLY,old.status.get());
  assertEquals(1,gym.getAllWorkouts().count());
  assertTrue(gym.createBackup().contains("Old row"));
 }
 @Test public void delayedSamplesKeepPrescribedIntervalBoundaries() {
  Program p=Program.minutes("Intervals",1,Difficulty.MEDIUM);
  p.getSegment(0).setDuration(10);p.addSegment(new Segment(Difficulty.REST).setDuration(5));
  p.addSegment(new Segment(Difficulty.MEDIUM).setDuration(10));
  gym.select(p);
  gym.onMeasured(m(11,110));
  assertEquals(Difficulty.REST,gym.progress.segment.difficulty.get());
  gym.onMeasured(m(15,110));
  assertEquals(Difficulty.MEDIUM,gym.progress.segment.difficulty.get());
  assertEquals(Event.PROGRAM_FINISHED,gym.onMeasured(m(25,200)));
  Workout completed = gym.complete();
  assertEquals(20, completed.planActiveSeconds.get().intValue());
 }
 @Test public void delayedPacketCanCrossSeveralTimedSegments() {
  Program p=Program.minutes("Short intervals",1,Difficulty.MEDIUM);
  p.getSegment(0).setDuration(5);p.addSegment(new Segment(Difficulty.REST).setDuration(5));
  p.addSegment(new Segment(Difficulty.MEDIUM).setDuration(5));
  gym.select(p);
  assertEquals(Event.PROGRAM_FINISHED,gym.onMeasured(m(15,150)));
  Workout result=gym.complete();
  assertEquals(10,result.planActiveSeconds.get().intValue());
  assertEquals(100,result.planActiveDistance.get().intValue());
 }
 @Test public void statisticsMaterializeDatabaseSamplesBeforeMultiplePasses() {
  gym.select(Program.meters("Statistics",100,Difficulty.MEDIUM));
  gym.onMeasured(m(10,100)); Workout row=gym.complete();
  row.planActiveSeconds.set(null); row.planActiveDistance.set(null); row.planActiveStrokes.set(null);
  gym.mergeWorkout(row);
  WorkoutStatistics statistics=new WorkoutStatistics(row,gym.getSnapshots(row).list());
  assertEquals(100,statistics.getWorkMeters().intValue());
  assertEquals(10,statistics.getWorkSeconds().intValue());
  assertEquals(100,gym.activeDistance(row));
 }
 @Test public void backupRestoresLiveRowLayout() {
  android.content.SharedPreferences layout=context.getSharedPreferences("live_row_display",Context.MODE_PRIVATE);
  layout.edit().putString("metric_bindings","POWER,DISTANCE,SPLIT,STROKE_RATE,DURATION,PULSE").commit();
  String backup=gym.createBackup();layout.edit().clear().commit();gym.restoreBackup(backup);
  assertEquals("POWER,DISTANCE,SPLIT,STROKE_RATE,DURATION,PULSE",layout.getString("metric_bindings",null));
 }
}
