package sk.ukf.wavvy;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import sk.ukf.wavvy.model.Song;

public class PlayerActivity extends AppCompatActivity {
    public static final String EXTRA_QUEUE_AUDIO_IDS = "queue_audio_ids";
    public static final String EXTRA_QUEUE_INDEX = "queue_index";
    private ExoPlayer player;
    private ImageButton btnBack;
    private ImageButton btnMore;
    private ImageButton btnShuffle;
    private ImageButton btnPrev;
    private ImageButton btnPlayPause;
    private ImageButton btnNext;
    private ImageButton btnRepeat;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private TextView tvSongTitle;
    private TextView tvSongArtist;
    private ImageView ivCover;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;
    private int[] queueAudioIds = null;
    private int queueIndex = 0;
    private int currentAudioResId = 0;
    private boolean countedThisTrack = false;
    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            if (player != null && !isUserSeeking) {
                long pos = player.getCurrentPosition();
                seekBar.setProgress((int) pos);
                tvCurrentTime.setText(formatTime(pos));
            }
            uiHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat insets = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insets.setAppearanceLightStatusBars(false);

        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);

        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvSongArtist = findViewById(R.id.tvSongArtist);
        ivCover = findViewById(R.id.ivCover);

        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);

        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);

        btnBack.setOnClickListener(v -> finish());
        btnMore.setOnClickListener(v ->
                Toast.makeText(this, "More Soon", Toast.LENGTH_SHORT).show()
        );

        player = new ExoPlayer.Builder(this).build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long dur = player.getDuration();
                    if (dur > 0) {
                        seekBar.setMax((int) dur);
                        tvTotalTime.setText(formatTime(dur));
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    if (!playNextInternal(true)) stopPlayback();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcon();

                if (isPlaying && !countedThisTrack && currentAudioResId != 0) {
                    PlayCountRepository.increment(PlayerActivity.this, currentAudioResId);
                    countedThisTrack = true;
                }
            }
        });

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> playNextInternal(true));
        btnPrev.setOnClickListener(v -> playPrev());

        btnShuffle.setOnClickListener(v -> Toast.makeText(this, "Shuffle Soon", Toast.LENGTH_SHORT).show());
        btnRepeat.setOnClickListener(v -> Toast.makeText(this, "Repeat Soon", Toast.LENGTH_SHORT).show());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                isUserSeeking = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (player != null) player.seekTo(sb.getProgress());
            }
        });

        initQueueFromIntent();
        loadCurrentTrack(false);
        updatePlayPauseIcon();
        updateNavButtons();
    }
    private void initQueueFromIntent() {
        queueAudioIds = getIntent().getIntArrayExtra(EXTRA_QUEUE_AUDIO_IDS);
        queueIndex = getIntent().getIntExtra(EXTRA_QUEUE_INDEX, 0);

        if (queueAudioIds == null || queueAudioIds.length == 0) {
            int singleAudio = getIntent().getIntExtra("audioResId", R.raw.test_track);
            queueAudioIds = new int[]{ singleAudio };
            queueIndex = 0;
        }

        if (queueIndex < 0) queueIndex = 0;
        if (queueIndex >= queueAudioIds.length) queueIndex = queueAudioIds.length - 1;
    }
    private void loadCurrentTrack(boolean autoPlay) {
        int audioResId = queueAudioIds[queueIndex];
        currentAudioResId = audioResId;

        countedThisTrack = false;
        seekBar.setProgress(0);
        tvCurrentTime.setText("00:00");
        tvTotalTime.setText("00:00");

        Song s = SongRepository.findByAudioResId(audioResId);
        if (s != null) {
            tvSongTitle.setText(s.getTitle());
            tvSongArtist.setText(s.getArtist());
            ivCover.setImageResource(s.getCoverResId());
        }

        MediaItem mediaItem = MediaItem.fromUri("android.resource://" + getPackageName() + "/" + audioResId);
        player.setMediaItem(mediaItem);
        player.prepare();

        if (autoPlay) player.play();

        updateNavButtons();
        updatePlayPauseIcon();
    }
    private void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) player.pause();
        else player.play();
        updatePlayPauseIcon();
    }
    private void stopPlayback() {
        if (player == null) return;

        player.pause();
        player.seekTo(0);

        seekBar.setProgress(0);
        tvCurrentTime.setText("00:00");

        countedThisTrack = false;

        updatePlayPauseIcon();
        updateNavButtons();
    }
    private boolean playNextInternal(boolean autoPlay) {
        if (queueAudioIds == null || queueAudioIds.length == 0) return false;
        if (queueIndex + 1 >= queueAudioIds.length) return false;

        queueIndex++;
        loadCurrentTrack(autoPlay);
        return true;
    }
    private void playPrev() {
        if (player == null) return;

        long pos = player.getCurrentPosition();
        if (pos > 3000) {
            player.seekTo(0);
            seekBar.setProgress(0);
            tvCurrentTime.setText("00:00");
            return;
        }

        if (queueAudioIds == null || queueAudioIds.length == 0) return;
        if (queueIndex - 1 < 0) return;

        queueIndex--;
        loadCurrentTrack(true);
    }
    private void updatePlayPauseIcon() {
        if (player == null) return;
        btnPlayPause.setImageResource(player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }
    private void updateNavButtons() {
        boolean hasQueue = queueAudioIds != null && queueAudioIds.length > 1;
        btnPrev.setEnabled(hasQueue && queueIndex > 0);
        btnNext.setEnabled(hasQueue && queueIndex < queueAudioIds.length - 1);

        float disabledAlpha = 0.35f;
        btnPrev.setAlpha(btnPrev.isEnabled() ? 1f : disabledAlpha);
        btnNext.setAlpha(btnNext.isEnabled() ? 1f : disabledAlpha);
    }
    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onStart() {
        super.onStart();
        uiHandler.post(progressUpdater);
    }

    @Override
    protected void onStop() {
        super.onStop();
        uiHandler.removeCallbacks(progressUpdater);
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(progressUpdater);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}