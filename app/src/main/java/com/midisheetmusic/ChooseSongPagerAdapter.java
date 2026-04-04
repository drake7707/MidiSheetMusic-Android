package com.midisheetmusic;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ChooseSongPagerAdapter extends FragmentStateAdapter {

    public ChooseSongPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1: return new RecentSongsFragment();
            case 2: return new FileBrowserFragment();
            default: return new AllSongsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
