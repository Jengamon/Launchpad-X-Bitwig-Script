var session, modes, drumpad_mode, mode_index;
var on_session = true;
var arranger_device, arranger_track;
var clip_launcher_view;
var recording_active, ra_behavior;
var follow_pref, mode_double_pref;

function init() {
  let preferences = host.getPreferences();
  let swap_on_boot = preferences.getBooleanSetting("Swap to Session on Boot?", "Behavior", true);

  // Transport access
  transport = host.createTransport();
  session = new Session(swap_on_boot.get());
  session.setCallbacks(onMidi0, onSysex0, onMidi1, onSysex1);

  on_session = swap_on_boot.get();

  // Initalize helper objects
  arranger_track = host.createCursorTrack(8, 0);
  arranger_device = arranger_track.createCursorDevice("Primary", "Primary Instrument", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);
  arranger_track.hasNext().markInterested();
  arranger_track.trackType().markInterested();
  follow_pref = preferences.getBooleanSetting("Follow Selection?", "Behavior", true);
  ra_behavior = preferences.getEnumSetting("Record Button Behavior", "Behavior", ["Toggle Launcher Overdub", "Cycle Selection"], "Toggle Launcher Overdub");
  mode_double_pref = preferences.getEnumSetting("On Mixer Mode Button Double Press", "Behavior", ["Do Nothing", "Do Action"], "Do Action");
  clip_launcher_view = new ClipLauncherView();

  // Initialize modes
  modes = [new SessionViewMode(), new MixerMode()];
  mode_index = 0;
  if(on_session) {
    modes[mode_index].onActivate(session);
  } else {
    // Light CC 99 with the default color (white)
    session.sendMidi(0xB0, 99, 1);
  }

  // Depending on whether the first device is a Drum Machine, switch between
  // Simple Drum Mode and normal Notes mode
  arranger_device.hasDrumPads().addValueObserver((hdp) => {
    if(hdp) {
      session.sendSysex("0F 01");
    } else {
      session.sendSysex("0F 00");
    }
  });

  // Record button drawing
  recording_active = false;
  transport.isClipLauncherOverdubEnabled().addValueObserver((re) => {
    recording_active = re;
    host.requestFlush();
  });

  // Initalize drum pad mode
  drumpad_mode = new DrumPadMode();

  // Flush our init state.
  host.requestFlush();

  // TODO: Perform further initialization here.
  println("Launchpad X initialized!");
}

// Called when a short MIDI message is received on MIDI input port 0.
function onMidi0(status, data1, data2) {
  let blocks = [95, 96, 97, 98];
  if(status == 0xB0 && data2 > 0 && blocks.indexOf(data1) != -1) {
    switch(data1) {
      case 95:
        // Switch session mode if we were already on the session mode
        if(on_session) {
          modes[mode_index].onDeactivate(session);
          mode_index = (mode_index + 1) % modes.length;
        }
        modes[mode_index].onActivate(session);
        on_session = true;
        break;
      case 96: // Notes was pressed
      case 97: // Custom was pressed
        // Other modes, so switch on_session to false
        on_session = false;
        modes[mode_index].onLeave(session);
        break;
      case 98: // Record was pressed (cycle selected track if on, else turn on)
        switch(ra_behavior.get()) {
          case "Toggle Launcher Overdub":
            transport.isClipLauncherOverdubEnabled().toggle();
            break;
          case "Cycle Selection":
            if(arranger_track.hasNext().get()) {
              arranger_track.selectNext();
            } else {
              arranger_track.selectFirst();
            }
            break;
          default:
            host.errorln(`Unhandled option ${ra_behavior.get()}`)
        }
        break;
      default: // Noop
        break;
    }
  } else {
    // Run mode code
    switch(status) {
      case 0xB0:
        if(!on_session) {
          drumpad_mode.onMidiIn(session, status, data1, data2);
        } else {
          modes[mode_index].onMidiIn(session, status, data1, data2);
        }
        break;
      case 0x98:
      case 0xA8:
        drumpad_mode.onMidiIn(session, status, data1, data2);
        break;
      default:
        modes[mode_index].onMidiIn(session, status, data1, data2);
        break;
    }
  }

  host.requestFlush();
}

// Called when a MIDI sysex message is received on MIDI input port 0.
function onSysex0(data) {
  let sysex = session.parseSysex(data);
  // printSysex(data);
  // println(`PARSED = ${JSON.stringify(sysex)} ${sysex.command.hexByteAt(0)}`);
  let drumpad_commands = [0x0f, 0x13, 0x16];
  // println(`${drumpad_commands.indexOf(sysex.command.hexByteAt(0)) != -1} ${sysex.command}`);
  if(drumpad_commands.indexOf(sysex.command.hexByteAt(0)) != -1) {
    drumpad_mode.onSysexIn(session, sysex);
  } else {
    modes[mode_index].onSysexIn(session, sysex);
  }
   host.requestFlush();
}

// Called when a short MIDI message is received on MIDI input port 1.
function onMidi1(status, data1, data2) {
   // If we supported scrolling text, we would turn it off here.
   // The notes and stuff are forwarded to Bitwig
}

// Called when a MIDI sysex message is received on MIDI input port 1.
// Thus is most probably entirely pointless...
function onSysex1(data) {
  print(`Sysex 1: ${data}`);
}

function flush() {
  if(!on_session) {
    drumpad_mode.redraw(session);
  } else {
    modes[mode_index].redraw(session);
  }

  // Record button drawing
  if(recording_active) {
    modes[mode_index].drawCCSolid(session, 98, 5);
  } else {
    modes[mode_index].drawCCSolid(session, 98, 7);
  }

  session.forceSend();
}

function exit() {
  session.shutdown();
  session = null;
  drumpad_mode = null;
  modes = [];
  mode_index = -1;
  arranger_device = null;
  arranger_track = null;
  recording_active = false;
}
