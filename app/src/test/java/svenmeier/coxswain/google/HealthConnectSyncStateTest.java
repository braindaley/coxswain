package svenmeier.coxswain.google;
import android.content.Context;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import svenmeier.coxswain.gym.Workout;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class HealthConnectSyncStateTest {
 @Test public void additionalPermissionsAllowBackfillAfterPartialSync() {
  Context context=RuntimeEnvironment.getApplication();
  Workout workout=new Workout();workout.start.set(123L);
  String exercise="android.permission.health.WRITE_EXERCISE";
  String power="android.permission.health.WRITE_POWER";
  android.content.SharedPreferences prefs=context.getSharedPreferences("health_connect_exports",Context.MODE_PRIVATE);
  prefs.edit().putBoolean("123",true).remove("permissions:123").commit();
  // Old boolean markers cannot prove that all newly granted metric types were sent.
  assertTrue(HealthConnectExport.needsExport(context,workout,Set.of(exercise,power)));
  prefs.edit().putStringSet("permissions:123",Set.of(exercise)).commit();
  assertFalse(HealthConnectExport.needsExport(context,workout,Set.of(exercise)));
  assertTrue(HealthConnectExport.needsExport(context,workout,Set.of(exercise,power)));
  prefs.edit().putStringSet("permissions:123",Set.of(exercise,power)).commit();
  assertFalse(HealthConnectExport.needsExport(context,workout,Set.of(exercise,power)));
  prefs.edit().remove("123").remove("permissions:123").commit();
 }
}
