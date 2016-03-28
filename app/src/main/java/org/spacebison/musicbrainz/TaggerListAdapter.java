package org.spacebison.musicbrainz;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.spacebison.musicbrainz.UntaggedListAdapter.UntaggedRelease;
import org.spacebison.musicbrainz.UntaggedListAdapter.UntaggedTrack;
import org.spacebison.musicbrainz.api.Medium;
import org.spacebison.musicbrainz.api.Release;
import org.spacebison.musicbrainz.api.Track;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 09.03.16.
 */
public class TaggerListAdapter extends RecyclerView.Adapter<TaggerListAdapter.ParentViewHolder> {
    private static final String TAG = "UntaggedListAdapter";
    private final OrderedHashSet<ReleaseTag> mReleaseTags = new OrderedHashSet<>();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final LinkedList<ChildAdapter> mChildAdapters = new LinkedList<>();
    private final RecyclerViewAdapterNotifier mNotifier = new RecyclerViewAdapterNotifier(this);
    private OnTrackTagClickListener mOnTrackTagClickListener;
    private OnTrackTagLongClickListener mOnTrackTagLongClickListener;
    private OnReleaseTagClickListener mOnReleaseTagClickListener;
    private OnReleaseTagLongClickListener mOnReleaseTagLongClickListener;

    @Override
    public ParentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.item_file_untagged_parent, parent, false);
        return new ParentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ParentViewHolder holder, final int position) {
        final ReleaseTag releaseTag;
        synchronized (mReleaseTags) {
            releaseTag = mReleaseTags.get(position);
        }

        holder.text.setText(releaseTag.release.getArtist_credit().get(0).getName() + " - " + releaseTag.release.getTitle());

        holder.adapter.setRelease(releaseTag);
        holder.adapter.notifyDataSetChanged();

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnReleaseTagClickListener != null) {
                    mOnReleaseTagClickListener.onReleaseTagClick(TaggerListAdapter.this, releaseTag);
                }
            }
        });
        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mOnReleaseTagLongClickListener != null) {
                    mOnReleaseTagLongClickListener.onReleaseTagLongClick(TaggerListAdapter.this, releaseTag);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mReleaseTags.size();
    }

    public List<ReleaseTag> getReleaseTags() {
        return new ArrayList<>(mReleaseTags);
    }

    public void setOnTrackTagClickListener(OnTrackTagClickListener onTrackTagClickListener) {
        mOnTrackTagClickListener = onTrackTagClickListener;
    }

    public void setOnTrackTagLongClickListener(OnTrackTagLongClickListener onTrackTagLongClickListener) {
        mOnTrackTagLongClickListener = onTrackTagLongClickListener;
    }

    public void setOnReleaseTagClickListener(OnReleaseTagClickListener onReleaseTagClickListener) {
        mOnReleaseTagClickListener = onReleaseTagClickListener;
    }

    public void setOnReleaseTagLongClickListener(OnReleaseTagLongClickListener onReleaseTagLongClickListener) {
        mOnReleaseTagLongClickListener = onReleaseTagLongClickListener;
    }

    public void addRelease(Release release) {
        ReleaseTag releaseTag = new ReleaseTag(release);

        if (mReleaseTags.contains(releaseTag)) {
            return;
        }

        mReleaseTags.add(releaseTag);
        mNotifier.notifyItemChanged(mReleaseTags.size() - 1);
    }

    public void setUntaggedRelease(ReleaseTag releaseTag, UntaggedRelease untaggedRelease, List<UntaggedTrack> untaggedTracks) {
        releaseTag.untagged = untaggedRelease;

        LinkedList<Track> tracks = new LinkedList<>();
        for (Medium m : releaseTag.release.getMedia()) {
            tracks.addAll(m.getTracks());
        }

        // TODO: 28.03.16 copmpute scores, assign tracks 

        notifyItemChanged(mReleaseTags.indexOf(releaseTag));
    }

    public void setUntaggedTrack(TrackTag trackTag, UntaggedTrack untaggedTrack) {
        trackTag.untaggedTrack = untaggedTrack;
    }

    public interface OnTrackTagClickListener {
        void onTrackTagClick(TaggerListAdapter adapter, ReleaseTag releaseTag, TrackTag track);
    }

    public interface OnTrackTagLongClickListener {
        void onTrackTagLongClick(TaggerListAdapter adapter, ReleaseTag releaseTag, TrackTag track);
    }

    public interface OnReleaseTagClickListener {
        void onReleaseTagClick(TaggerListAdapter adapter, ReleaseTag release);
    }

    public interface OnReleaseTagLongClickListener {
        void onReleaseTagLongClick(TaggerListAdapter adapter, ReleaseTag release);
    }

    public class ParentViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.root)
        View root;
        @Bind(R.id.text)
        TextView text;
        @Bind(R.id.recycler_view)
        RecyclerView recycler;
        @Bind(R.id.icon_collapse)
        View collapseButton;

        ChildAdapter adapter = new ChildAdapter();

        public ParentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            recycler.setAdapter(adapter);
            recycler.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            collapseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (recycler.getVisibility() == View.VISIBLE) {
                        collapseButton.animate().rotation(180f);
                        ViewCompat.animate(recycler).alpha(0).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                recycler.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        collapseButton.animate().rotation(0f);
                        ViewCompat.animate(recycler).alpha(1).withStartAction(new Runnable() {
                            @Override
                            public void run() {
                                recycler.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            });
        }
    }

    public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {
        ReleaseTag mReleaseTag;
        List<TrackTag> mTrackTags;

        public void setRelease(ReleaseTag releaseTag) {
            mReleaseTag = releaseTag;
            mTrackTags = new LinkedList<>();

            for (Medium m : releaseTag.release.getMedia()) {
                List<Track> tracks = m.getTracks();
                for (Track t : tracks) {
                    mTrackTags.add(new TrackTag(t));
                }
            }
        }

        @Override
        public ChildViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_untagged, parent, false);
            return new ChildViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ChildViewHolder holder, int position) {
            final TrackTag trackTag = mTrackTags.get(position);
            holder.text.setText(trackTag.track.getTitle());

            if (trackTag.untaggedTrack != null) {
                holder.text2.setText(trackTag.untaggedTrack.name);
                holder.text2.setVisibility(View.VISIBLE);
            } else {
                holder.text2.setVisibility(View.GONE);
            }

            holder.root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnTrackTagClickListener != null) {
                        mOnTrackTagClickListener.onTrackTagClick(TaggerListAdapter.this, mReleaseTag, trackTag);
                    }
                }
            });

            holder.root.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnTrackTagLongClickListener != null) {
                        mOnTrackTagLongClickListener.onTrackTagLongClick(TaggerListAdapter.this, mReleaseTag, trackTag);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mTrackTags.size();
        }

        public class ChildViewHolder extends RecyclerView.ViewHolder {
            @Bind(R.id.root)
            ViewGroup root;
            @Bind(R.id.text)
            TextView text;
            @Bind(R.id.text2)
            TextView text2;

            public ChildViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }

    public static class ReleaseTag {
        public UntaggedRelease untagged;
        public Release release;

        public ReleaseTag(Release release) {
            this.release = release;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ReleaseTag releaseTag = (ReleaseTag) o;

            return !(release != null ? !release.equals(releaseTag.release) : releaseTag.release != null);

        }

        @Override
        public int hashCode() {
            return release != null ? release.hashCode() : 0;
        }
    }

    public static class TrackTag {
        public Track track;
        public UntaggedTrack untaggedTrack;

        public TrackTag(Track track) {
            this.track = track;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TrackTag trackTag = (TrackTag) o;

            return !(track != null ? !track.equals(trackTag.track) : trackTag.track != null);

        }

        @Override
        public int hashCode() {
            return track != null ? track.hashCode() : 0;
        }
    }
}
