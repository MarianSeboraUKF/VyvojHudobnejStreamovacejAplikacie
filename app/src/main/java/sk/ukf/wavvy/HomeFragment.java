package sk.ukf.wavvy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Button btnOpenPlayer = view.findViewById(R.id.btnOpenPlayer);
        btnOpenPlayer.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            startActivity(intent);
        });

        return view;
    }
}