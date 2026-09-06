package svenmeier.coxswain.google;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Required by Health Connect to show permissions rationale.
 */
public class HealthConnectPermissionsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This is a minimal implementation. Usually, you'd show a rationale here.
        finish();
    }
}
