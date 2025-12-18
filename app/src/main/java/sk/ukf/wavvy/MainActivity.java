package sk.ukf.wavvy;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

public class MainActivity extends AppCompatActivity {
    private ExoPlayer player;
    private Button btnPlayPause;
    private Button btnStop;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnStop = findViewById(R.id.btnStop);

        player = new ExoPlayer.Builder(this).build();

        MediaItem mediaItem = MediaItem.fromUri(
                "android.resource://" + getPackageName() + "/" + R.raw.test_track
        );
        player.setMediaItem(mediaItem);
        player.prepare();

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnStop.setOnClickListener(v -> stopPlayback());

        updateButtonText();
    }
    private void togglePlayPause() {
        if (player == null) return;

        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
        updateButtonText();
    }
    private void stopPlayback() {
        if (player == null) return;
        player.pause();
        player.seekTo(0);
        updateButtonText();
    }
    private void updateButtonText() {
        if (player == null) return;
        btnPlayPause.setText(player.isPlaying() ? "Pause" : "Play");
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}