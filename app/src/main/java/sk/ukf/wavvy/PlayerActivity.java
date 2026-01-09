package sk.ukf.wavvy;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import sk.ukf.wavvy.model.Song;

public class PlayerActivity extends AppCompatActivity {
    public static final String EXTRA_QUEUE_AUDIO_IDS = "queue_audio_ids";
    public static final String EXTRA_QUEUE_INDEX = "queue_index";
    private ExoPlayer player;
    private ImageButton btnBack, btnMore;
    private ImageButton btnShuffle, btnPrev, btnPlayPause, btnNext, btnRepeat;
    private SeekBar seekBar;
    private TextView tvCurrentTime, tvTotalTime;
    private TextView tvSongTitle, tvSongArtist;
    private ImageView ivCover;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;
    private int[] originalQueue = null;
    private int[] activeQueue = null;
    private int queueIndex = 0;
    private int currentAudioResId = 0;
    private boolean countedThisTrack = false;
    private boolean shuffleEnabled = false;
    private enum RepeatMode { OFF, ONE, ALL }
    private RepeatMode repeatMode = RepeatMode.OFF;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean resumeOnFocusGain = false;
    private final float normalPlayerVolume = 1.0f;
    private TextView tvPlaybackStatus;
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener =
            focusChange -> {
                if (player == null) return;

                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        player.setVolume(normalPlayerVolume);
                        if (resumeOnFocusGain) {
                            resumeOnFocusGain = false;
                            player.play();
                        }
                        updatePlayPauseIcon();
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS:
                        resumeOnFocusGain = false;
                        if (player.isPlaying()) player.pause();
                        abandonAudioFocus();
                        updatePlayPauseIcon();
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        resumeOnFocusGain = player.isPlaying();
                        if (player.isPlaying()) player.pause();
                        updatePlayPauseIcon();
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        player.setVolume(0.2f);
                        break;
                }
            };
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
        WindowInsetsControllerCompat insets =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insets.setAppearanceLightStatusBars(false);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setWillPauseWhenDucked(false)
                .build();

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
        tvPlaybackStatus = findViewById(R.id.tvPlaybackStatus);

        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);

        btnBack.setOnClickListener(v -> finish());
        btnMore.setOnClickListener(v -> Toast.makeText(this, "More Soon", Toast.LENGTH_SHORT).show());

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
                    onTrackEnded();
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
        btnNext.setOnClickListener(v -> playNext(true));
        btnPrev.setOnClickListener(v -> playPrev());
        btnShuffle.setOnClickListener(v -> toggleShuffle());
        btnRepeat.setOnClickListener(v -> cycleRepeatMode());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (player != null) player.seekTo(sb.getProgress());
            }
        });
        initQueueFromIntent();
        updateShuffleUi();
        updateRepeatUi();
        updatePlaybackStatusText();
        loadCurrentTrack(false);
        updatePlayPauseIcon();
        updateNavButtons();
    }
    private void initQueueFromIntent() {
        int[] ids = getIntent().getIntArrayExtra(EXTRA_QUEUE_AUDIO_IDS);
        int idx = getIntent().getIntExtra(EXTRA_QUEUE_INDEX, 0);

        if (ids == null || ids.length == 0) {
            int singleAudio = getIntent().getIntExtra("audioResId", R.raw.test_track);
            ids = new int[]{ singleAudio };
            idx = 0;
        }

        if (idx < 0) idx = 0;
        if (idx >= ids.length) idx = ids.length - 1;

        originalQueue = ids.clone();
        activeQueue = ids.clone();
        queueIndex = idx;
    }
    private void loadCurrentTrack(boolean autoPlay) {
        if (activeQueue == null || activeQueue.length == 0) return;

        int audioResId = activeQueue[queueIndex];
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

        if (autoPlay) {
            if (requestAudioFocus()) player.play();
            else Toast.makeText(this, "Nepodarilo sa získať audio focus.", Toast.LENGTH_SHORT).show();
        }

        NowPlayingRepository.saveNowPlaying(
                this,
                currentAudioResId,
                activeQueue,
                queueIndex
        );
        updateNavButtons();
        updatePlayPauseIcon();
    }
    private void onTrackEnded() {
        if (player == null) return;

        // Repeat ONE: stále točíme tú istú skladbu
        if (repeatMode == RepeatMode.ONE) {
            player.seekTo(0);
            player.play();
            return;
        }

        // Skús ísť na ďalšiu skladbu
        boolean moved = playNext(true);

        // Ak sa nepodarilo (boli sme na konci queue)
        if (!moved) {
            if (repeatMode == RepeatMode.ALL && activeQueue != null && activeQueue.length > 0) {
                // playNext() pri ALL už vie wrapnúť, ale sem sa dostaneme len ak len 1 track
                player.seekTo(0);
                player.play();
            } else {
                // Repeat OFF (alebo queue 1 track) -> stop
                stopPlayback();
            }
        }
    }
    private void togglePlayPause() {
        if (player == null) return;

        if (player.isPlaying()) {
            player.pause();
            abandonAudioFocus();
        } else {
            if (requestAudioFocus()) player.play();
            else Toast.makeText(this, "Niečo sa nepodarilo :/", Toast.LENGTH_SHORT).show();
        }
        updatePlayPauseIcon();
    }
    private void stopPlayback() {
        if (player == null) return;

        player.pause();
        player.seekTo(0);

        abandonAudioFocus();

        seekBar.setProgress(0);
        tvCurrentTime.setText("00:00");

        countedThisTrack = false;

        updatePlayPauseIcon();
        updateNavButtons();
    }
    private boolean playNext(boolean autoPlay) {
        if (activeQueue == null || activeQueue.length == 0) return false;

        if (queueIndex < activeQueue.length - 1) {
            queueIndex++;
            loadCurrentTrack(autoPlay);
            return true;
        }

        if (repeatMode == RepeatMode.ALL && activeQueue.length > 1) {
            queueIndex = 0;
            loadCurrentTrack(autoPlay);
            return true;
        }
        return false;
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

        if (activeQueue == null || activeQueue.length == 0) return;

        if (queueIndex > 0) {
            queueIndex--;
            loadCurrentTrack(true);
            return;
        }

        if (repeatMode == RepeatMode.ALL && activeQueue.length > 1) {
            queueIndex = activeQueue.length - 1;
            loadCurrentTrack(true);
        }
    }
    private void toggleShuffle() {
        if (originalQueue == null || originalQueue.length <= 1) {
            Toast.makeText(this, "Shuffle nie je dostupný", Toast.LENGTH_SHORT).show();
            return;
        }

        shuffleEnabled = !shuffleEnabled;
        int keepAudioId = currentAudioResId;

        if (shuffleEnabled) {
            activeQueue = buildShuffledQueueKeepingCurrent(originalQueue, keepAudioId);
            queueIndex = 0;
        } else {
            activeQueue = originalQueue.clone();
            queueIndex = indexOf(activeQueue, keepAudioId);
        }
        updateShuffleUi();
        updateNavButtons();
        updatePlaybackStatusText();
    }
    private int[] buildShuffledQueueKeepingCurrent(int[] source, int currentId) {
        if (source == null || source.length == 0) return source;

        // Rozdelíme: current track + zvyšok
        ArrayList<Integer> rest = new ArrayList<>();
        boolean foundCurrent = false;

        for (int id : source) {
            if (id == currentId && !foundCurrent) {
                foundCurrent = true;
            } else {
                rest.add(id);
            }
        }

        // Ak current track v source nebol, tak len shuffle celé
        if (!foundCurrent) {
            ArrayList<Integer> all = new ArrayList<>();
            for (int id : source) all.add(id);
            Collections.shuffle(all, new Random(System.nanoTime()));
            int[] out = new int[all.size()];
            for (int i = 0; i < all.size(); i++) out[i] = all.get(i);
            return out;
        }

        // Shuffle zvyšku
        Collections.shuffle(rest, new Random(System.nanoTime()));

        // Výsledok: current ide na index 0, zvyšok za ním
        int[] out = new int[source.length];
        out[0] = currentId;

        for (int i = 0; i < rest.size(); i++) {
            out[i + 1] = rest.get(i);
        }

        return out;
    }
    private int indexOf(int[] arr, int id) {
        if (arr == null) return 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == id) return i;
        }
        return 0;
    }
    private void updateShuffleUi() {
        if (shuffleEnabled) tintOn(btnShuffle);
        else tintOff(btnShuffle);
    }
    private void cycleRepeatMode() {
        if (repeatMode == RepeatMode.OFF) repeatMode = RepeatMode.ONE;
        else if (repeatMode == RepeatMode.ONE) repeatMode = RepeatMode.ALL;
        else repeatMode = RepeatMode.OFF;
        updateRepeatUi();
        updateNavButtons();
        updatePlaybackStatusText();
    }
    private void updateRepeatUi() {
        if (repeatMode == RepeatMode.OFF) {
            btnRepeat.setImageResource(R.drawable.ic_repeat);
            tintOff(btnRepeat);
            return;
        }

        if (repeatMode == RepeatMode.ONE) {
            btnRepeat.setImageResource(R.drawable.ic_repeat_one);
        } else {
            btnRepeat.setImageResource(R.drawable.ic_repeat);
        }
        tintOn(btnRepeat);
    }
    private void updatePlaybackStatusText() {
        if (tvPlaybackStatus == null) return;

        String shuffle = shuffleEnabled ? "Shuffle ON" : "Shuffle OFF";

        String repeat;
        if (repeatMode == RepeatMode.OFF) repeat = "Repeat OFF";
        else if (repeatMode == RepeatMode.ONE) repeat = "Repeat ONE";
        else repeat = "Repeat ALL";

        tvPlaybackStatus.setText(shuffle + " • " + repeat);

        boolean anyOn = shuffleEnabled || repeatMode != RepeatMode.OFF;
        tvPlaybackStatus.setTextColor(ContextCompat.getColor(
                this,
                anyOn ? R.color.accent : R.color.textSecondary
        ));
    }
    private void updatePlayPauseIcon() {
        if (player == null) return;
        btnPlayPause.setImageResource(player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }
    private void updateNavButtons() {
        boolean hasQueue = activeQueue != null && activeQueue.length > 1;
        boolean prevEnabled = hasQueue && (queueIndex > 0 || repeatMode == RepeatMode.ALL);
        btnPrev.setEnabled(prevEnabled);

        boolean nextEnabled = hasQueue && (queueIndex < activeQueue.length - 1 || repeatMode == RepeatMode.ALL);
        btnNext.setEnabled(nextEnabled);

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
    private void tintOn(ImageButton btn) {
        btn.setColorFilter(ContextCompat.getColor(this, R.color.accent));
        btn.setAlpha(1.0f);
    }
    private void tintOff(ImageButton btn) {
        btn.setColorFilter(ContextCompat.getColor(this, R.color.textSecondary));
        btn.setAlpha(0.55f);
    }
    private boolean requestAudioFocus() {
        if (audioManager == null || audioFocusRequest == null) return true;
        int result = audioManager.requestAudioFocus(audioFocusRequest);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }
    private void abandonAudioFocus() {
        if (audioManager == null || audioFocusRequest == null) return;
        audioManager.abandonAudioFocusRequest(audioFocusRequest);
        resumeOnFocusGain = false;
        if (player != null) player.setVolume(normalPlayerVolume);
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
        abandonAudioFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(progressUpdater);
        abandonAudioFocus();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}