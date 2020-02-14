package io.github.jengamon.novation;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.HostErrorOutputStream;
import io.github.jengamon.novation.internal.HostOutputStream;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.modes.DrumPadMode;
import io.github.jengamon.novation.reactive.modes.MixerMode;
import io.github.jengamon.novation.reactive.modes.SessionMode;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
      BooleanValue mSwapOnBoot = prefs.getBooleanSetting("Swap to Session on Boot?", "Behavior", true);
      BooleanValue mTrackUploadValues = prefs.getBooleanSetting("Upload Track Notes?", "Midi Behavior", true);
      BooleanValue mFollowCursorTrack = prefs.getBooleanSetting("Follow Cursor Track?", "Behavior", true);
      BooleanValue mPulseSessionPads = prefs.getBooleanSetting("Pulse Session Scene Pads?", "Behavior", false);
      BooleanValue mViewableBanks = prefs.getBooleanSetting("Viewable Bank?", "Behavior", true);
      EnumValue mRecordLevel = prefs.getEnumSetting("Record Level", "Record Button", new String[]{"Global", "Clip Launcher"}, "Clip Launcher");
      EnumValue mRecordAction = prefs.getEnumSetting("Record Action", "Record Button", new String[]{"Toggle Record", "Cycle Tracks"}, "Toggle Record");

      // Replace System.out and System.err with ones that should actually work
      System.setOut(new PrintStream(new HostOutputStream(host)));
      System.setErr(new PrintStream(new HostErrorOutputStream(host)));

      // Create the requisite state objects
      mSession = new Session(host);
      mSurface = host.createHardwareSurface();
      Transport mTransport = host.createTransport();
      CursorTrack mCursorTrack = host.createCursorTrack(8, 0);
      CursorDevice mCursorDevice = mCursorTrack.createCursorDevice("Primary", "Primary Instrument", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);
      TrackBank mSessionTrackBank = host.createTrackBank(8, 0, 8, true);
//      TrackBank mMixerTrackBank = host.createTrackBank(8, 8, 0, false);

      // Setup track bank following
      SettableIntegerValue stbpos = mSessionTrackBank.scrollPosition();
//      SettableIntegerValue mtbpos = mMixerTrackBank.scrollPosition();
      IntegerValue stbcc = mSessionTrackBank.channelCount();
//      IntegerValue mtbcc = mMixerTrackBank.channelCount();
      stbpos.markInterested(); stbcc.markInterested();
//      mtbpos.markInterested(); mtbcc.markInterested();
      AtomicBoolean followCursor = new AtomicBoolean(false);
      mFollowCursorTrack.addValueObserver(followCursor::set);
      mCursorTrack.position().addValueObserver(new_pos -> {
         if(followCursor.get()) {
            // Check if it is in bounds
            int stbposition = stbpos.get();
//            int mtbposition = mtbpos.get();
            boolean stbib = new_pos > stbposition && new_pos < (stbposition + 8);
//            boolean mtbib = new_pos > mtbposition && new_pos < (mtbposition + 8);
            System.out.println(stbcc.get() + " "  + mSessionTrackBank.getSizeOfBank());
            if(!stbib) {
               int nstbposition = Math.max(0, Math.min(stbcc.get() - mSessionTrackBank.getCapacityOfBank(), new_pos));
               stbpos.set(nstbposition);
            }
//            if(!mtbib) {
//               int nmtbposition = Math.max(0, Math.min(mtbcc.get() - mMixerTrackBank.getCapacityOfBank(), new_pos));
//               mtbpos.set(nmtbposition);
//            }
         }
      });

      mViewableBanks.addValueObserver(vb -> {
         mSessionTrackBank.sceneBank().setIndication(vb);
         for(int i = 0; i < mSessionTrackBank.getCapacityOfBank(); i++) {
            mSessionTrackBank.getItemAt(i).clipLauncherSlotBank().setIndication(vb);
         }
      });

      // Set up note uploading
      mCursorTrack.createLauncherCursorClip(0, 0).addNoteStepObserver(note -> {
         if(!mTrackUploadValues.get()) return;
         switch(note.state()) {
            case NoteOn:
               mSession.midiOut(ChannelType.CUSTOM).sendMidi(0x90 | note.channel(), note.y(), (int) note.velocity() * 127);
               break;
            case NoteSustain:
               mSession.midiOut(ChannelType.CUSTOM).sendMidi(0xA0 | note.channel(), note.y(), (int) note.velocity() * 127);
               break;
            case Empty:
               mSession.midiOut(ChannelType.CUSTOM).sendMidi(0x80 | note.channel(), note.y(), (int) note.velocity() * 127);
               break;
         }
      });

      BooleanSyncWrapper pulseSessionPads = new BooleanSyncWrapper(mPulseSessionPads, mSurface, host);

      // Create surface buttons and their lights
      mSurface.setPhysicalSize(241, 241);
      mLSurface = new LaunchpadXSurface(host, mSession, mSurface);
      mMachine = new ModeMachine();
      mMachine.register(Mode.SESSION, new SessionMode(mSessionTrackBank, mTransport, mSurface, host, pulseSessionPads));
      mMachine.register(Mode.DRUM, new DrumPadMode(host, mSession, mSurface, mCursorDevice));
      mMachine.register(Mode.MIXER, new MixerMode(host, mTransport, mLSurface, mSession, mSurface, mCursorTrack, mCursorDevice, mSessionTrackBank));

      MidiIn dawIn = mSession.midiIn(ChannelType.DAW);

      // Select record button behavior and light it accordingly
      mCursorTrack.hasNext().markInterested();
      AtomicBoolean recordActionToggle = new AtomicBoolean(false);
      AtomicBoolean recordLevelGlobal = new AtomicBoolean(false);
      mRecordAction.addValueObserver(val -> recordActionToggle.set(val.equals("Toggle Record")));
      mRecordLevel.addValueObserver(val -> recordLevelGlobal.set(val.equals("Global")));
      Runnable selectAction = () -> {
         if(recordActionToggle.get()) {
            if(recordLevelGlobal.get()) {
               mTransport.isArrangerRecordEnabled().toggle();
            } else {
               mTransport.isClipLauncherOverdubEnabled().toggle();
            }
         } else {
            if(mCursorTrack.hasNext().get()) {
               mCursorTrack.selectNext();
            } else {
               mCursorTrack.selectFirst();
            }
         }
         host.requestFlush();
      };

      HardwareActionBindable recordState = host.createAction(selectAction, () -> "Press Record Button");
      mLSurface.record().button().pressedAction().setBinding(recordState);

      AtomicBoolean recordEnabled = new AtomicBoolean(false);
      AtomicBoolean overdubEnabled = new AtomicBoolean(false);
      mTransport.isArrangerRecordEnabled().addValueObserver(are -> {
         mSurface.invalidateHardwareOutputState();
         recordEnabled.set(are);
      });
      mTransport.isClipLauncherOverdubEnabled().addValueObserver(ode -> {
         mSurface.invalidateHardwareOutputState();
         overdubEnabled.set(ode);
      });
      SessionSendableLightState recordLightState = new SessionSendableLightState() {
         ColorTag getLightState() {
            if(recordEnabled.get() || overdubEnabled.get()) {
               return new ColorTag(255, 97, 97);
            } else {
               return new ColorTag(179, 97, 97);
            }
         }

         @Override
         public HardwareLightVisualState getVisualState() {
            return HardwareLightVisualState.createForColor(getLightState().toBitwigColor());
         }

         @Override
         public boolean equals(Object obj) {
            return false;
         }

         @Override
         public void send(Session session) {
            session.sendMidi(0xB0, 98, getLightState().selectNovationColor());
         }
      };
      mLSurface.record().light().state().setValue(recordLightState);

      AtomicReference<Mode> lastSessionMode = new AtomicReference<>(Mode.SESSION);
      HardwareActionBindable mSessionAction = host.createAction(() -> {
         switch(mMachine.mode()) {
            case SESSION:
               lastSessionMode.set(Mode.MIXER);
               mMachine.setMode(mLSurface, Mode.MIXER);
               break;
            case MIXER:
               mSession.sendSysex("00 00");
               lastSessionMode.set(Mode.SESSION);
               mMachine.setMode(mLSurface, Mode.SESSION);
               break;
            case DRUM:
            case UNKNOWN:
               mMachine.setMode(mLSurface, lastSessionMode.get());
               if(lastSessionMode.get() == Mode.SESSION) {
                  mSession.sendSysex("00 00");
               }
               break;
            default:
               throw new RuntimeException("Unknown mode " + mMachine.mode());
         }
         host.requestFlush();
      }, () -> "Press Session View");

      HardwareActionBindable mNoteAction = host.createAction(() -> {
         mSession.sendSysex("00 01");
         mMachine.setMode(mLSurface, Mode.DRUM);
         host.requestFlush();
      }, () -> "Press Note View");

      if(mSwapOnBoot.get()) {
         mSessionAction.invoke();
      } else {
         mMachine.setMode(mLSurface, Mode.DRUM);
      }

      mSessionAction.addBinding(mLSurface.session().button().pressedAction());
      mNoteAction.addBinding(mLSurface.note().button().pressedAction());

//      mSession.sendSysex("00");

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
//      System.out.println(Arrays.toString(sysex));
      mMachine.sendSysex(mSession, sysex);
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
