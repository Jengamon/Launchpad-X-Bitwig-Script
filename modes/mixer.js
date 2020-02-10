// A mode that controls a mixer bank

// MIXER Modes
const MODE_VOLUME = 0;
const MODE_PAN = 1;
const MODE_SEND_A = 2; // Sends mode
const MODE_SEND_B = 3; // Controls mode
const MODE_STOP_CLIP = 4;
const MODE_MUTE = 5;
const MODE_SOLO = 6;
const MODE_RECORD_ARM = 7;
const MODE_PADS = [89, 79, 69, 59, 49, 39, 29, 19];
const FADER_BI = [false, true, false, false];
const EPSILON = 0.05;

const CC_MAPS = [
  [21, 22, 23, 24, 25, 26, 27, 28],
  [29, 30, 31, 32, 33, 34, 35, 36],
  [37, 38, 39, 40, 41, 42, 43, 44],
  [45, 46, 47, 48, 49, 50, 51, 52],
];

function MixerMode() {
  Mode.prototype.initialize.call(this);
  this.mode = 0; // Default to mixer volume mode
  this.controls = arranger_device.createCursorRemoteControlsPage(8);
  this.vertical = false;
  this.fader_colors = [0, 0, 0, 0, 0, 0, 0, 0]; // Just set it's color to 0 if it isn't useful...
  this.send_exists = [false, false, false, false, false, false, false, false];
  this.fader_values = [
    [0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0],
  ];
  this.ignore_flag = [
    [false, false, false, false, false, false, false, false],
    [false, false, false, false, false, false, false, false],
    [false, false, false, false, false, false, false, false],
    [false, false, false, false, false, false, false, false],
  ];
  this.position = [0, 0, 0, 0];
  this.max_position = [0, 0, 0, 0];

  // Setup callbacks
  let mm = this;

  let update_mode_value = (mode, i, value) => {
    mm.fader_values[mode][i] = value;
    if(!mm.ignore_flag[mode][i]) {
      mm.sendValues(session);
      host.requestFlush();
    }
    // If the delta is large enough, stop ignoring sending it.
    if(Math.abs(mm.fader_values[mode][i] - value) > EPSILON && mm.ignore_flag[mode][i]) {
      mm.ignore_flag[mode][i] = false;
    }
  }

  for(let i = 0; i < 8; i++) {
    let param = this.controls.getParameter(i);
    param.setIndication(true);
    param.value().addValueObserver((control) => update_mode_value(MODE_SEND_B, i, control));
    this.controls.selectedPageIndex().addValueObserver((csp) => {
      mm.position[MODE_SEND_B] = csp;
      host.requestFlush();
    });
    this.controls.pageCount().addValueObserver((cic) => {
      mm.max_position[MODE_SEND_B] = cic;
      host.requestFlush();
    });
  }

  let sendBank = arranger_track.sendBank();
  sendBank.scrollPosition().addValueObserver((sbsp) => {
    mm.position[MODE_SEND_A] = sbsp;
    mm.setupFaders(session);
    host.requestFlush();
  });
  sendBank.itemCount().addValueObserver((sbic) => {
    mm.max_position[MODE_SEND_A] = sbic;
    mm.setupFaders(session);
    host.requestFlush();
  });

  for(let send = 0; send < 8; send++) {
    let s = sendBank.getItemAt(send);
    s.exists().addValueObserver((se) => {
      // println(`${se} ${send}`);
      mm.send_exists[send] = se;
      mm.setupFaders(session);
      host.requestFlush();
    });
    // Send mode callback
    s.value().addValueObserver((sv) => update_mode_value(MODE_SEND_A, send, sv));
  }

  for(let track = 0; track < 8; track++) {
    let t = clip_launcher_view.view.getItemAt(track);
    clip_launcher_view.view.channelScrollPosition().addValueObserver((csp) => {
      mm.position[MODE_VOLUME] = csp;
      mm.position[MODE_PAN] = csp;
      host.requestFlush();
    });
    clip_launcher_view.view.channelCount().addValueObserver((cic) => {
      mm.max_position[MODE_VOLUME] = cic;
      mm.max_position[MODE_PAN] = cic;
      host.requestFlush();
    });
    // Color callback
    t.color().addValueObserver((r, g, b) => {
      let color = find_novation_color(r, g, b);
      switch(mm.mode) {
        case MODE_VOLUME:
        case MODE_PAN:
          mm.fader_colors[track] = color;
          mm.sendColors(session);
          host.requestFlush();
          break;
        default:
          break;
      }
    });

    // Pan mode callback
    t.pan().value().addValueObserver((pan) => update_mode_value(MODE_PAN, track, pan));

    // Volume mode callback
    t.volume().value().addValueObserver((volume) => update_mode_value(MODE_VOLUME, track, volume));
  }
}

MixerMode.prototype = Object.create(Mode.prototype);

MixerMode.prototype.activeColor = function() { return 0x5E; }

MixerMode.prototype.redraw = function(session) {
  // Depending on the current mode, highlight the button with the mode color
  let mode_pad = MODE_PADS[this.mode];
  this.drawCCPulse(session, mode_pad, this.activeColor());

  // Draw arrows depending on the mode, position, max_position and verticalness (only in fader modes)
  let view_size = (this.mode != MODE_SEND_B ? 8 : 1);
  switch(this.mode) {
    case MODE_VOLUME: // Left and right scroll tracks
    case MODE_PAN: // Up and Down scroll tracks
    case MODE_SEND_A: // Left and Right scroll tracks
    case MODE_SEND_B: // Left and Right switch pages
      if(this.position[this.mode] > 0) {
        if(!this.vertical) {
          this.drawCCSolid(session, 91, this.activeColor());
        } else {
          this.drawCCSolid(session, 93, this.activeColor());
        }
      }
      if(this.position[this.mode] + view_size < this.max_position[this.mode]) {
        if(!this.vertical) {
          this.drawCCSolid(session, 92, this.activeColor());
        } else {
          this.drawCCSolid(session, 94, this.activeColor());
        }
      }
      break;
    default:
      break; // Taken care of in CLVM
  }

  // Draw CLV in proper mode
  let clvm = Math.max(Math.min(this.mode - MODE_SEND_B, BRM_RECORD), BRM_NORMAL);
  clip_launcher_view.draw(session, this, clvm, this.mode > MODE_SEND_B);

  this.clearUnused(session);
};

MixerMode.prototype.onDeactivate = function(session) {
  Mode.prototype.onDeactivate.call(this, session);
  // Switch out from fader mode back to normal session view
  session.sendSysex("00 00");
}

MixerMode.prototype.onMidiIn = function(session, status, data1, data2) {
  if(status == 0xb4) { // Fader change
    switch(this.mode) {
      case MODE_VOLUME:
      case MODE_PAN:
      case MODE_SEND_A:
      case MODE_SEND_B:
        // Receive the proper value.
        for(let mode = 0; mode <= MODE_SEND_B; mode++) {
          let ccs = CC_MAPS[mode];
          let index = ccs.indexOf(data1);
          if(index != -1) {
            let value = data2 / 127;
            if(Math.abs(value - 0.5) <= 0.02) {
              value = 0.5;
            } // Nudge value to .5 if reasonable
            // println(`FADER ${index}: ${value}`)
            this.fader_values[mode][index] = value;
            this.uploadValue(mode, index, value);
          }
        }
        break;
      default:
        // This mode doesn't use this faders, so ignore them.
        break;
    }
  } else if(data2 > 0) { // On press
    let col = parseInt((''+data1)[1], 10);
    let row = parseInt((''+data1)[0], 10);
    // println(`[MM] ${status.toString(16)} ${data1} ${data2}`);
    // If it is one of the scene buttons, switch scene modes.
    let launch = (row != 1 || this.mode <= MODE_SEND_B);
    if(col == 9) {
      this.mode = 8 - row;
      this.switchMode(session);
    //} else if (status == 0xA0) {
      // Ignore aftertouch
    } else if (status == 0x90) {
      switch(this.mode) {
        case MODE_VOLUME:
        case MODE_PAN:
        case MODE_SEND_A:
        case MODE_SEND_B:
          this.launchPad();
          break
        case MODE_STOP_CLIP:
          if(row == 1) {
            clip_launcher_view.view.getItemAt(col - 1).stop();
          } else {
            this.launchPad(data1);
          }
          break;
        case MODE_MUTE:
          if(row == 1) {
            clip_launcher_view.view.getItemAt(col - 1).mute().toggle();
          } else {
            this.launchPad(data1);
          }
          break;
        case MODE_SOLO:
          if(row == 1) {
            clip_launcher_view.view.getItemAt(col - 1).solo().toggle();
          } else {
            this.launchPad(data1);
          }
          break;
        case MODE_RECORD_ARM:
          if(row == 1) {
            clip_launcher_view.view.getItemAt(col - 1).arm().toggle();
          } else {
            this.launchPad(data1);
          }
          break;
        default:
          break;
      }
    } else if (this.mode > MODE_SEND_B) {
      this.operateControls(data1);
    } else {
      let back = (!this.vertical ? data1 == 91 : data1 == 93);
      let forward = (!this.vertical ? data1 == 92 : data1 == 94);
      switch(this.mode) {
        case MODE_VOLUME:
        case MODE_PAN:
          if(back) {
            clip_launcher_view.view.scrollBackwards();
          } else if (forward) {
            clip_launcher_view.view.scrollForwards();
          }
          break;
        case MODE_SEND_A:
          break;
        case MODE_SEND_B:
          if(back) {
            this.controls.selectPreviousPage(false);
          } else if (forward) {
            this.controls.selectNextPage(false);
          }
          break;
        default:
          throw new Error("Unreachable");
      }
    }
  }
};

MixerMode.prototype.uploadValue = function(mode, index, value) {
  let track = clip_launcher_view.view.getItemAt(index);
  this.ignore_flag[mode][index] = true;
  switch(mode) {
    case MODE_VOLUME:
      track.volume().value().set(value);
      break;
    case MODE_PAN:
      track.pan().value().set(value);
      break;
    case MODE_SEND_A:
      arranger_track.sendBank().getItemAt(index).value().set(value);
      break;
    case MODE_SEND_B:
      this.controls.getParameter(index).value().set(value);
      break;
    default:
      break;
  }
}

MixerMode.prototype.sendValues = function(session) {
  if(this.mode > MODE_SEND_B) {
    // Don't update if not in a fader mode
    return;
  }
  // Send the current values we have
  for(let i = 0; i < 8; i++) {
    session.sendMidi(0xB4, CC_MAPS[this.mode][i], Math.round(this.fader_values[this.mode][i] * 127));
  }
}

MixerMode.prototype.sendColors = function(session) {
  if(this.mode > MODE_SEND_B) {
    // Don't update if not in a fader mode
    return;
  }
  let colors = this.faderColors();
  // Send the current values we have
  for(let i = 0; i < 8; i++) {
    session.sendMidi(0xB5, CC_MAPS[this.mode][i], colors[i]);
  }
};

MixerMode.prototype.faderColors = function() {
  let colors;
  switch(this.mode) {
    case MODE_VOLUME:
    case MODE_PAN:
      colors = this.fader_colors;
      break;
    case MODE_SEND_A:
      colors = this.send_exists.map((se) => (se ? 0x3 : 0x0));
      break;
    case MODE_SEND_B:
      colors = [5, 84, 13, 21, 29, 37, 53, 57];
      break;
    default:
      break;
  }
  return colors;
};

MixerMode.prototype.setupFaders = function(session) {
  if(this.mode > MODE_SEND_B) {
    // Don't update if not in a fader mode
    return;
  }

  // let colors = this.faderColors();
  // Setup Faders
  let command = "0100";
  if(this.vertical) {
    command += "00";
  } else {
    command += "01";
  }

  for(let i = 0; i < 8; i++) {
    let colorString = "00";
    let type;
    if(FADER_BI[this.mode]) {
      type = "01";
    } else {
      type = "00";
    }
    let cc = ("0" + CC_MAPS[this.mode][i].toString(16)).substr(-2);
    let index = ("0" + i.toString(16)).substr(-2);
    // println(`${index} ${type} ${cc} ${colorString} ${this.fader_colors[i].toString(16)} ${this.fader_colors[i]}`);
    command += (index + type + cc + colorString);
  }

  session.sendSysex(command);
  this.sendValues(session);
  this.sendColors(session);
};

// Switch to fader mode (if not already) if in Volume, Pan, Send, or Controls Mode
MixerMode.prototype.switchMode = function(session) {
  // Update mode characteristics
  switch(this.mode) {
    case MODE_VOLUME:
      this.vertical = true;
      break;
    case MODE_PAN:
      this.vertical = false;
      break;
    case MODE_SEND_A:
      this.vertical = true;
      break;
    case MODE_SEND_B:
      this.vertical = true;
      break;
    default:
      break;
  }

  // Produce commands
  if(this.mode <= MODE_SEND_B) {
    this.setupFaders(session);
    session.sendSysex("00 0D");
  } else {
    session.sendSysex("00 00");
  }
}

MixerMode.prototype.onActivate = function(session) {
  Mode.prototype.onActivate.call(this, session);
  this.switchMode(session);
};

MixerMode.prototype.onSysexIn = function(session, sysex) {
  let command = sysex.command.hexByteAt(0);
  switch(command) {
    case 0x00: // Mode switch
    case 0x0d: // Fader velocity
    case 0x0e: // Prog/Live toggle
      // Just ignore these
      break;
    default:
      println(`[MM] Unknown SYSEX command ${command.toString(16)}: ${sysex.data}`);
      break;
  }
};
