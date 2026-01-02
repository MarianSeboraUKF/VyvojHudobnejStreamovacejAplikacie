package sk.ukf.wavvy;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PlayCountRepository {
    private static final String PREFS = "wavvy_prefs";
    private static final String KEY_COUNTS = "play_counts_json";
    private static final Gson gson = new Gson();
    private static Map<String, Integer> getMap(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_COUNTS, "{}");

        Type type = new TypeToken<HashMap<String, Integer>>() {}.getType();
        Map<String, Integer> map = gson.fromJson(json, type);
        if (map == null) map = new HashMap<>();
        return map;
    }
    private static void saveMap(Context ctx, Map<String, Integer> map) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_COUNTS, gson.toJson(map)).apply();
    }
    public static int getCount(Context ctx, int audioResId) {
        Map<String, Integer> map = getMap(ctx);
        String key = String.valueOf(audioResId);
        Integer v = map.get(key);
        return (v == null) ? 0 : v;
    }
    public static void increment(Context ctx, int audioResId) {
        Map<String, Integer> map = getMap(ctx);
        String key = String.valueOf(audioResId);

        int current = 0;
        if (map.containsKey(key) && map.get(key) != null) {
            current = map.get(key);
        }
        map.put(key, current + 1);
        saveMap(ctx, map);
    }
}