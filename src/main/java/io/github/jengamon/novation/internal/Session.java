package io.github.jengamon.novation.internal;

import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.callback.SysexMidiDataReceivedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;

public class Session {
    private final MidiIn dawIn;
    private final MidiOut dawOut;

    private final MidiIn customIn;
    private final MidiOut customOut;

    private final NoteInput noteInput;

    private final static String SYSEX_HEADER = "f0 00 20 29 02 0c";

    public Session(ControllerHost host) {
        dawIn = host.getMidiInPort(0);
        dawOut = host.getMidiOutPort(0);

        customIn = host.getMidiInPort(1);
        customOut = host.getMidiOutPort(1);

        noteInput = customIn.createNoteInput("", "??????");
        noteInput.setShouldConsumeEvents(false);

        // Switch to Live mode (if not already)
        sendSysex("0e 00");
        // Switch on DAW mode (if not already)
        sendSysex("10 01");

//        forceSend();
    }

    public void setMidiCallback(ChannelType type, ShortMidiMessageReceivedCallback clbk) {
        switch(type) {
            case DAW:
                dawIn.setMidiCallback(clbk);
                break;
            case CUSTOM:
                customIn.setMidiCallback(clbk);
                break;
        }
    }

    public void setSysexCallback(ChannelType type, SysexMidiDataReceivedCallback clbk) {
        switch(type) {
            case DAW:
                dawIn.setSysexCallback(clbk);
                break;
            case CUSTOM:
                customIn.setSysexCallback(clbk);
                break;
        }
    }

    public MidiIn midiIn(ChannelType type) {
        switch(type) {
            case DAW:
                return this.dawIn;
            case CUSTOM:
                return this.customIn;
        }
        return null; // Unreachable (hopefully)
    }

    public MidiOut midiOut(ChannelType type) {
        switch(type) {
            case DAW:
                return this.dawOut;
            case CUSTOM:
                return this.customOut;
        }
        return null; // Unreachable (hopefully)
    }

    public NoteInput noteInput() {
        return noteInput;
    }

    public void sendSysex(String message) {
        String sysex = SYSEX_HEADER + " " + message + " f7";
        dawOut.sendSysex(sysex);
    }

    public void sendMidi(int status, int data1, int data2) {
//        if(status != 0) System.out.println(Utils.toHexString((byte)status) + "[" + Utils.toHexString((byte) data1) + " " + Utils.toHexString((byte) data2) + "]");
        dawOut.sendMidi(status, data1, data2);
    }

    public void shutdown() {
        sendSysex("10 00");
//        forceSend();
    }

    @Deprecated
    public void forceSend() {
    }
}
