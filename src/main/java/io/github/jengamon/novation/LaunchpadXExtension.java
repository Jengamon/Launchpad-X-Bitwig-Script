package io.github.jengamon.novation;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.HostErrorOutputStream;
import io.github.jengamon.novation.internal.HostOutputStream;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendable;
import io.github.jengamon.novation.reactive.modes.DrumPadMode;
import io.github.jengamon.novation.reactive.modes.SessionMode;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LaunchpadXExtension extends ControllerExtension
{
   private Session mSession;
   private HardwareSurface mSurface;
   private LaunchpadXSurface mLSurface;
   private ModeMachine mMachine;

   protected LaunchpadXExtension(final LaunchpadXExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      Preferences prefs = host.getPreferences();
      BooleanValue mTrackUploadValues = prefs.getBooleanSetting("Upload Track Notes?", "Behavior", true);
      SettableRangedValue mUploadChannel = prefs.getNumberSetting("Upload Channel", "Behavior", 0.0, 15.0, 1.0, "MIDI", 0.0);

      // Replace System.out and System.err with ones that should actually work
      System.setOut(new PrintStream(new HostOutputStream(host)));
      System.setErr(new PrintStream(new HostErrorOutputStream(host)));

      // Create the requisite state objects
      mSession = new Session(host);
      mSurface = host.createHardwareSurface();
      CursorTrack mCursorTrack = host.createCursorTrack(8, 0);
      CursorDevice mCursorDevice = mCursorTrack.createCursorDevice("Primary", "Primary Instrument", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);

      final List<PlayingNote>[] previous = new List[]{new ArrayList<>()};
      mCursorTrack.playingNotes().addValueObserver(notes -> {
         if(!mTrackUploadValues.get()) return;
         int channel = (int)mUploadChannel.getRaw();
         List<PlayingNote> playingNotes = new ArrayList<>();
         Collections.addAll(playingNotes, notes);
         previous[0].removeAll(playingNotes);
         for(PlayingNote note : previous[0]) {
            mSession.midiOut(ChannelType.CUSTOM).sendMidi(0x80 | channel, note.pitch(), note.velocity());
         }
         for(PlayingNote note : notes) {
            mSession.midiOut(ChannelType.CUSTOM).sendMidi(0x90 | channel, note.pitch(), note.velocity());
         }
         previous[0] = playingNotes;
      });

      // Create surface buttons and their lights
      mSurface.setPhysicalSize(241, 241);
      mLSurface = new LaunchpadXSurface(host, mSession, mSurface);
      mMachine = new ModeMachine();
      mMachine.register(Mode.SESSION, new SessionMode());
      mMachine.register(Mode.DRUM, new DrumPadMode(host, mSession, mSurface, mCursorDevice));

      MidiIn dawIn = mSession.midiIn(ChannelType.DAW);

      HardwareActionBindable mSessionAction = host.createAction(() -> {
         mSession.sendSysex("00 00");
         mMachine.setMode(mLSurface, Mode.SESSION);
         host.requestFlush();
      }, () -> "Press Session View");

      HardwareActionBindable mNoteAction = host.createAction(() -> {
         mSession.sendSysex("00 01");
         mMachine.setMode(mLSurface, Mode.DRUM);
         host.requestFlush();
      }, () -> "Press Note View");

      mSessionAction.invoke();

      mSessionAction.addBinding(mLSurface.session().button().pressedAction());
      mNoteAction.addBinding(mLSurface.note().button().pressedAction());

      mSession.sendSysex("00");

      mSession.setMidiCallback(ChannelType.DAW, data -> onMidi0(data));
      mSession.setSysexCallback(ChannelType.DAW, data -> onSysex0(data));
      mSession.setMidiCallback(ChannelType.CUSTOM, data -> onMidi1(data));

      // TODO: Perform your driver initialization here.
      // For now just show a popup notification for verification that it is running.
      System.out.println("Launchpad X Initialized");
   }

   @Override
   public void exit()
   {
      // TODO: Perform any cleanup once the driver exits
      // For now just show a popup notification for verification that it is no longer running.
      mSession.shutdown();
      System.out.println("Launchpad X Exited");
   }

   @Override
   public void flush()
   {
      mSurface.updateHardware();
      mSession.forceSend();
   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(ShortMidiMessage msg)
   {
      mSurface.invalidateHardwareOutputState();
      System.out.println(msg);
   }

   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex0(final String data)
   {
      byte[] sysex = Utils.parseSysex(data);
      System.out.println(Arrays.toString(sysex));
      mSurface.invalidateHardwareOutputState();
   }
   
   /** Called when we receive short MIDI message on port 1. */
   private void onMidi1(ShortMidiMessage msg)
   {

   }

//   /** Called when we receive sysex MIDI message on port 1. */
//   private void onSysex1(final String data)
//   {
//   }
}
