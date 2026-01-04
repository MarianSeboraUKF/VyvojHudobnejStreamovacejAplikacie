package sk.ukf.wavvy;

import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;
import sk.ukf.wavvy.model.Song;

public class PlayerLauncher {
    private PlayerLauncher() {}
    public static void openQueue(Context ctx, ArrayList<Song> list, int clickedAudioResId) {
        if (ctx == null || list == null || list.isEmpty()) return;

        int[] ids = new int[list.size()];
        int index = 0;

        for (int i = 0; i < list.size(); i++) {
            int id = list.get(i).getAudioResId();
            ids[i] = id;
            if (id == clickedAudioResId) index = i;
        }

        Intent intent = new Intent(ctx, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_QUEUE_AUDIO_IDS, ids);
        intent.putExtra(PlayerActivity.EXTRA_QUEUE_INDEX, index);
        ctx.startActivity(intent);
    }
    public static void openQueue(Context ctx, ArrayList<Song> list, Song clicked) {
        if (clicked == null) return;
        openQueue(ctx, list, clicked.getAudioResId());
    }
}