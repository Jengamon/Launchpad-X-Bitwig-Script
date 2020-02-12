package io.github.jengamon.novation;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.HostErrorOutputStream;
import io.github.jengamon.novation.internal.HostOutputStream;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.mode.*;
import io.github.jengamon.novation.state.ArrowValue;
import io.github.jengamon.novation.state.LaunchpadXState;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LaunchpadXExtension extends ControllerExtension
{
   private Transport mTransport;
   private Session mSession;
   private HardwareSurface mSurface;
   private RangedValue mBPM;
   private LaunchpadXState mState;

   private AbstractMode mSessionMode;
   private AbstractMode mNoteMode;
   private Mode mCurrentMode = null;

   private CursorTrack mCursorTrack;
   private CursorDevice mCursorDevice;

   protected LaunchpadXExtension(final LaunchpadXExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      // Replace System.out and System.err with ones that should actually work
      System.setOut(new PrintStream(new HostOutputStream(host)));
      System.setErr(new PrintStream(new HostErrorOutputStream(host)));

      Preferences preferences = host.getPreferences();
      BooleanValue swap_on_boot = preferences.getBooleanSetting("Swap to Session on Boot?", "Behavior", true);
      BooleanValue follow_pref = preferences.getBooleanSetting("Follow Selection?", "Behavior", true);
      BooleanValue send_track_notes = preferences.getBooleanSetting("Send Track Notes?", "Behavior", true);
      EnumValue record_mode = preferences.getEnumSetting("Record Mode", "Behavior", new String[]{"Clip Launcher", "Global"}, "Clip Launcher");
      EnumValue ra_behavior = preferences.getEnumSetting("Record Button Behavior", "Behavior", new String[]{"Toggle Record", "Cycle Selection"}, "Toggle Record");
      EnumValue mode_double_pref = preferences.getEnumSetting("On Mixer Mode Button Double Press", "Behavior", new String[]{"Do Nothing", "Do Action"}, "Do Nothing");

      mTransport = host.createTransport();
      mSession = new Session(host);
      // Keep track of the bpm manually
      mBPM = mTransport.tempo().modulatedValue();
      mBPM.markInterested();

      mCursorTrack = host.createCursorTrack(8, 0);
      mCursorDevice = mCursorTrack.createCursorDevice("Primary", "Primary Instrument", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);

      mSurface = host.createHardwareSurface();
      initSurface(host);
      host.requestFlush();

      HardwareActionBindable mSessionAction = host.createAction(() -> {
         mSession.sendSysex("00 00");
         mCurrentMode = Mode.SESSION;
//         mState.clear();
         mState.sessionButton().light().setColor(Color.fromHex("50505000"));
         mState.noteButton().light().setColor(Color.nullColor());
         host.requestFlush();
//         mSurface.invalidateHardwareOutputState();
      }, () -> "Switch mode to Session View");

      HardwareActionBindable mNoteAction = host.createAction(() -> {
         mSession.sendSysex("00 01");
         mCurrentMode = Mode.NOTE;
//         mState.clear();
         mState.noteButton().light().setColor(Color.fromHex("50505000"));
         mState.sessionButton().light().setColor(Color.nullColor());
         host.requestFlush();
      }, () -> "Switch mode to Note View");

      initModes(host);

      mSessionAction.addBinding(mState.sessionButton().button().pressedAction());
      mNoteAction.addBinding(mState.noteButton().button().pressedAction());

      if(swap_on_boot.get()) {
         mSessionAction.invoke();
      }

//      mSession.setMidiCallback(ChannelType.DAW, msg -> onMidi0(msg));
//      mSession.setMidiCallback(ChannelType.CUSTOM, msg -> onMidi1(msg));
      mSession.setSysexCallback(ChannelType.DAW, data -> onSysex0(data));
//      mSession.setSysexCallback(ChannelType.CUSTOM, data -> onSysex1(data));

      // TODO: Perform your driver initialization here.
      // For now just show a popup notification for verification that it is running.
      System.out.println("Launchpad X Initialized");
   }

   private void initSurface(ControllerHost host) {
      mSurface.setPhysicalSize(241, 241);
      mState = new LaunchpadXState(host, mSurface, mSession, mBPM); // Initialize controller state

//      MidiIn dawIn = mSession.midiIn(ChannelType.DAW);

//      Function<ArrowValue, BooleanValueChangedCallback> arrowObserver = arrowValue -> pressed -> {
//         if(pressed) {
//            host.requestFlush();
//         }
//      };

      // Connect the state to our buttons
//      mState.arrows()[0].button().isPressed().addValueObserver(val -> host.requestFlush());
//      mState.arrows()[1].button().isPressed().addValueObserver(val -> host.requestFlush());
//      mState.arrows()[2].button().isPressed().addValueObserver(val -> host.requestFlush());
//      mState.arrows()[3].button().isPressed().addValueObserver(val -> host.requestFlush());
   }

   private void initModes(ControllerHost host) {
      mSessionMode = new SessionMode();
      mNoteMode = new NoteMode(mCursorDevice);

//      Runnable refresh = () -> host.requestFlush();
      Function<Mode, Supplier<Boolean>> isMode = mode -> () -> mode.equals(mCurrentMode);

      mSessionMode.onInitialize(host, mSession, mState, isMode.apply(Mode.SESSION));
      mNoteMode.onInitialize(host, mSession, mState, isMode.apply(Mode.NOTE));
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
      // TODO: Implement your MIDI input handling code here.
   }

   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex0(final String data)
   {
      byte[] sysex = Utils.parseSysex(data);
      System.out.println(Arrays.toString(sysex));
   }
   
//   /** Called when we receive short MIDI message on port 1. */
//   private void onMidi1(ShortMidiMessage msg)
//   {
//   }
//
//   /** Called when we receive sysex MIDI message on port 1. */
//   private void onSysex1(final String data)
//   {
//   }
}
