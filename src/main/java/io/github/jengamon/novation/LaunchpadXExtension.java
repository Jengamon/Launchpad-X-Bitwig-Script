package io.github.jengamon.novation;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.HostErrorOutputStream;
import io.github.jengamon.novation.internal.HostOutputStream;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.modes.AbstractMode;
import io.github.jengamon.novation.reactive.modes.DrumPadMode;
import io.github.jengamon.novation.reactive.modes.SessionMode;
import io.github.jengamon.novation.reactive.modes.mixer.*;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.ihls.BasicColor;

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

      BooleanSyncWrapper pulseSessionPads = new BooleanSyncWrapper(mPulseSessionPads, mSurface, host);

      // Create surface buttons and their lights
      mSurface.setPhysicalSize(241, 241);
      mLSurface = new LaunchpadXSurface(host, mSession, mSurface);
      mMachine = new ModeMachine(mSession);
      mMachine.register(Mode.SESSION, new SessionMode(mSessionTrackBank, mTransport, mSurface, host, pulseSessionPads));
      mMachine.register(Mode.DRUM, new DrumPadMode(host, mSession, mSurface, mCursorDevice));
      mMachine.register(Mode.UNKNOWN, new AbstractMode() {
         @Override
         public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
            return new ArrayList<>();
         }
      });

      // Mixer modes
      AtomicReference<Mode> mixerMode = new AtomicReference<>(Mode.MIXER_VOLUME);
      mMachine.register(Mode.MIXER_VOLUME, new VolumeMixer(mMachine, mixerMode, host, mTransport, mLSurface, mSurface, mCursorTrack, mSessionTrackBank));
      mMachine.register(Mode.MIXER_PAN, new PanMixer(mMachine, mixerMode, host, mTransport, mLSurface, mSurface, mCursorTrack, mSessionTrackBank));
      mMachine.register(Mode.MIXER_SEND, new SendMixer(mMachine, mixerMode, host, mTransport, mLSurface, mSurface, mCursorTrack));
      mMachine.register(Mode.MIXER_CONTROLS, new ControlsMixer(mMachine, mixerMode, host, mTransport, mLSurface, mSurface, mCursorTrack));
      mMachine.register(Mode.MIXER_STOP, new StopClipMixer(mMachine, mixerMode, host, mTransport, mLSurface, mSurface, mCursorTrack, mSessionTrackBank));
      mMachine.register(Mode.MIXER_MUTE, new MuteMixer(mMachine, mixerMode, host, mTransport, mLSurface, mSurface, mCursorTrack, mSessionTrackBank));
      mMachine.register(Mode.MIXER_SOLO, new SoloMixer(mMachine, mixerMode, host, mTransport, mLSurface, mSurface, mCursorTrack, mSessionTrackBank));
      mMachine.register(Mode.MIXER_ARM, new RecordArmMixer(mMachine, mixerMode, host, mTransport, mLSurface, mSurface, mCursorTrack, mSessionTrackBank));
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
      int nid = mLSurface.novation().id();
      mLSurface.novation().light().state().setValue(new BasicColor(new ColorTag(0xff, 0xff, 0xff), 0xB0, new int[]{0}, nid));

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
         host.requestFlush();
      }, () -> "Press Session View");

      HardwareActionBindable mNoteAction = host.createAction(() -> {
         Mode om = mMachine.mode();
         if(om != Mode.DRUM && om != Mode.UNKNOWN) {
            lastSessionMode.set(om);
         }
         mSession.sendSysex("00 01");
         mMachine.setMode(mLSurface, Mode.DRUM);
         host.requestFlush();
      }, () -> "Press Note View");

      HardwareActionBindable mCustomAction = host.createAction(() -> {
         Mode om = mMachine.mode();
         if(om != Mode.DRUM && om != Mode.UNKNOWN) {
            lastSessionMode.set(om);
         }
         mMachine.setMode(mLSurface, Mode.UNKNOWN);
         host.requestFlush();
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
