package io.github.jengamon.novation.reactive.atomics;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.ObjectArrayValue;
import com.bitwig.extension.controller.api.PlayingNote;

public class NotesSyncWrapper extends ObjectArraySyncWrapper<PlayingNote> {
    public NotesSyncWrapper(ObjectArrayValue<PlayingNote> array, HardwareSurface surf, ControllerHost host) {
        super(array, surf, host);
    }

    public boolean isNotePlaying(int note)
    {
        for(int i = 0; i < length(); i++) {
            PlayingNote playingNote = get(i);
            assert playingNote != null;
            if(playingNote.pitch() == note)
                return true;
        }
        return false;
    }
}
