package io.github.jengamon.novation.reactive.modes.session;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.Track;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MixerPadAction extends SessionPadAction {
    private AtomicBoolean mMixerMode;

    public MixerPadAction(int x, int y, AtomicReference<SessionPadMode> padMode, ClipLauncherSlot slot, Track track, BooleanSyncWrapper isTrackEnabled, AtomicBoolean mixerMode) {
        super(x, y, padMode, slot, track, isTrackEnabled);
        mMixerMode = mixerMode;
    }

    @Override
    public void run() {
        // Only run this action if we aren't in a "mixer mode"
        if(!mMixerMode.get()) {
            super.run();
        }
    }
}
