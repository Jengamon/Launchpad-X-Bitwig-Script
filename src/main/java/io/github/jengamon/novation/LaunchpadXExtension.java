package io.github.jengamon.novation;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.HostErrorOutputStream;
import io.github.jengamon.novation.internal.HostOutputStream;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.modes.AbstractMode;
import io.github.jengamon.novation.modes.DrumPadMode;
import io.github.jengamon.novation.modes.SessionMode;
import io.github.jengamon.novation.modes.mixer.*;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.state.PadLightState;

import java.io.PrintStream;
import java.util.ArrayList;
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
      BooleanValue mPulseSessionPads = prefs.getBooleanSetting("Pulse Session Scene Pads?", "Behavior", false);
//      BooleanValue mFollowCursorTrack = prefs.getBooleanSetting("Follow Cursor Track?", "Behavior", true);
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
      TrackBank mSessionTrackBank = host.createTrackBank(8, 0, 8);
      mSessionTrackBank.setSkipDisabledItems(true);

      mViewableBanks.addValueObserver(vb -> {
         mSessionTrackBank.sceneBank().setIndication(vb);
         for(int i = 0; i < mSessionTrackBank.getCapacityOfBank(); i++) {
            mSessionTrackBank.getItemAt(i).clipLauncherSlotBank().setIndication(vb);
         }
      });

      // Create surface buttons and their lights
      mSurface.setPhysicalSize(241, 241);
      mLSurface = new LaunchpadXSurface(host, mSession, mSurface);
      mMachine = new ModeMachine(mSession);
      mMachine.register(Mode.SESSION, new SessionMode(mSessionTrackBank, mTransport, mLSurface, host, mPulseSessionPads));
      mMachine.register(Mode.DRUM, new DrumPadMode(host, mSession, mLSurface, mCursorDevice));
      mMachine.register(Mode.UNKNOWN, new AbstractMode() {
         @Override
         public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
            return new ArrayList<>();
         }
      });

      // Mixer modes
      AtomicReference<Mode> mixerMode = new AtomicReference<>(Mode.MIXER_VOLUME);
      mMachine.register(Mode.MIXER_VOLUME, new VolumeMixer(mixerMode, host, mTransport, mLSurface, mSessionTrackBank));
      mMachine.register(Mode.MIXER_PAN, new PanMixer(mixerMode, host, mTransport, mLSurface, mSessionTrackBank));
      mMachine.register(Mode.MIXER_SEND, new SendMixer(mixerMode, host, mTransport, mLSurface, mCursorTrack));
      mMachine.register(Mode.MIXER_CONTROLS, new ControlsMixer(mixerMode, host, mTransport, mLSurface, mCursorDevice));
      mMachine.register(Mode.MIXER_STOP, new StopClipMixer(mixerMode, host, mTransport, mLSurface, mSessionTrackBank));
      mMachine.register(Mode.MIXER_MUTE, new MuteMixer(mixerMode, host, mTransport, mLSurface, mSessionTrackBank));
      mMachine.register(Mode.MIXER_SOLO, new SoloMixer(mixerMode, host, mTransport, mLSurface, mSessionTrackBank));
      mMachine.register(Mode.MIXER_ARM, new RecordArmMixer(mixerMode, host, mTransport, mLSurface, mSessionTrackBank));
//      MidiIn dawIn = mSession.midiIn(ChannelType.DAW);

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
      MultiStateHardwareLight recordLight = mLSurface.record().light();
      BooleanValue arrangerRecord = mTransport.isArrangerRecordEnabled();
      BooleanValue clipLauncherOverdub = mTransport.isClipLauncherOverdubEnabled();
      arrangerRecord.addValueObserver(are -> {
         if(are || clipLauncherOverdub.get()) {
            recordLight.state().setValue(PadLightState.solidLight(5));
         } else {
            recordLight.state().setValue(PadLightState.solidLight(7));
         }
      });
      clipLauncherOverdub.addValueObserver(ode -> {
         if(ode || arrangerRecord.get()) {
            recordLight.state().setValue(PadLightState.solidLight(5));
         } else {
            recordLight.state().setValue(PadLightState.solidLight(7));
         }
      });

      mLSurface.novation().light().state().setValue(PadLightState.solidLight(3));

      AtomicReference<Mode> lastSessionMode = new AtomicReference<>(Mode.SESSION);
      HardwareActionBindable mSessionAction = host.createAction(() -> {
         switch(mMachine.mode()) {
            case SESSION:
               lastSessionMode.set(mixerMode.get());
               mMachine.setMode(mLSurface, mixerMode.get());
               break;
            case MIXER_VOLUME:
            case MIXER_PAN:
            case MIXER_SEND:
            case MIXER_CONTROLS:
            case MIXER_STOP:
            case MIXER_MUTE:
            case MIXER_SOLO:
            case MIXER_ARM:
               lastSessionMode.set(Mode.SESSION);
               mMachine.setMode(mLSurface, Mode.SESSION);
               break;
            case DRUM:
            case UNKNOWN:
               mMachine.setMode(mLSurface, lastSessionMode.get());
               break;
            default:
               throw new RuntimeException("Unknown mode " + mMachine.mode());
         }
      }, () -> "Press Session View");

      HardwareActionBindable mNoteAction = host.createAction(() -> {
         Mode om = mMachine.mode();
         if(om != Mode.DRUM && om != Mode.UNKNOWN) {
            lastSessionMode.set(om);
         }
         mSession.sendSysex("00 01");
         mMachine.setMode(mLSurface, Mode.DRUM);
      }, () -> "Press Note View");

      HardwareActionBindable mCustomAction = host.createAction(() -> {
         Mode om = mMachine.mode();
         if(om != Mode.DRUM && om != Mode.UNKNOWN) {
            lastSessionMode.set(om);
         }
         mMachine.setMode(mLSurface, Mode.UNKNOWN);
      }, () -> "Press Custom View");

      if(mSwapOnBoot.get()) {
         mSessionAction.invoke();
      } else {
         mMachine.setMode(mLSurface, Mode.DRUM);
      }

      mSessionAction.addBinding(mLSurface.session().button().pressedAction());
      mNoteAction.addBinding(mLSurface.note().button().pressedAction());
      mCustomAction.addBinding(mLSurface.custom().button().pressedAction());

      mSession.setMidiCallback(ChannelType.DAW, this::onMidi0);
      mSession.setSysexCallback(ChannelType.DAW, this::onSysex0);
      mSession.setMidiCallback(ChannelType.CUSTOM, this::onMidi1);

      System.out.println("Launchpad X Initialized");

      host.requestFlush();
   }

   @Override
   public void exit()
   {
      mSession.shutdown();
      System.out.println("Launchpad X Exited");
   }

   @Override
   public void flush()
   {
      mSurface.updateHardware();
   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(ShortMidiMessage msg)
   {
//      mSurface.invalidateHardwareOutputState();
//      System.out.println(msg);
   }

   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex0(final String data)
   {
      byte[] sysex = Utils.parseSysex(data);
      mMachine.sendSysex(sysex);
      mSurface.invalidateHardwareOutputState();
   }
   
   /** Called when we receive short MIDI message on port 1. */
   private void onMidi1(ShortMidiMessage msg)
   {
      // System.out.println("C: " + Utils.toHexString((byte)msg.getStatusByte()) + Utils.toHexString((byte)msg.getData1()) + Utils.toHexString((byte)msg.getData2()));
   }

//   /** Called when we receive sysex MIDI message on port 1. */
//   private void onSysex1(final String data)
//   {
//   }
}
