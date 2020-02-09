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
  this.fader_colors = [1, 1, 1, 1, 1, 1, 1, 1]; // Just set it's color to 0 if it isn't useful...
  this.fader_values = [
    [10, 10, 10, 10, 10, 10, 10, 10],
    [10, 10, 10, 10, 10, 10, 10, 10],
    [10, 10, 10, 10, 10, 10, 10, 10],
    [10, 10, 10, 10, 10, 10, 10, 10],
  ];
  this.fader_bi = [
    [false, false, false, false, false, false, false, false],
    [true, true, true, true, true, true, true, true],
    [false, false, false, false, false, false, false, false],
    [false, false, false, false, false, false, false, false],
  ];
  this.ignore_flag = [
    [false, false, false, false, false, false, false, false],
    [false, false, false, false, false, false, false, false],
    [false, false, false, false, false, false, false, false],
    [false, false, false, false, false, false, false, false],
  ];
  // Setup callbacks
  let mm = this;

  for(let i = 0; i < 8; i++) {
    let param = this.controls.getParameter(i);
    param.setIndication(true);
    param.value().addValueObserver((control) => {
      if(!mm.ignore_flag[MODE_SEND_B][i]) {
        mm.fader_values[MODE_SEND_B][i] = control * 127;
        mm.sendValues(session);
        host.requestFlush();
      }
      mm.ignore_flag[MODE_SEND_B][i] = false;
    });
  }

  for(let track = 0; track < 8; track++) {
    let t = clip_launcher_view.view.getItemAt(track);
    // track.color().set(0.5, 1, 0.3);
    // Color callback
    t.color().addValueObserver((r, g, b) => {
      let color = find_novation_color(r, g, b);
      switch(mm.mode) {
        case MODE_VOLUME:
        case MODE_PAN:
          mm.fader_colors[track] = color;
          mm.setupFaders(session);
          host.requestFlush();
          break;
        default:
          break;
      }
    });

    // Pan mode callbacks
    t.pan().value().addValueObserver((pan) => {
      if(!mm.ignore_flag[MODE_PAN][track]) {
        mm.fader_values[MODE_PAN][track] = pan * 127;
        mm.sendValues(session);
        host.requestFlush();
      }
      mm.ignore_flag[MODE_PAN][track] = false;
    });
  }
}

MixerMode.prototype = Object.create(Mode.prototype);

MixerMode.prototype.activeColor = function() { return 0x5E; }

MixerMode.prototype.redraw = function(session) {
  // Depending on the current mode, highlight the button with the mode color
  let mode_pad = MODE_PADS[this.mode];
  this.drawCCPulse(session, mode_pad, this.activeColor());

  // Draw arrows depending on the mode and verticalness
  switch(this.mode) {
    case MODE_VOLUME:
    case MODE_PAN:
    case MODE_SEND_A:
    case MODE_SEND_B:

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
            this.fader_values[mode][index] = data2;
            this.uploadValue(mode, index, data2);
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
    println(`[MM] ${status.toString(16)} ${data1} ${data2}`);
    // If it is one of the scene buttons, switch scene modes.
    if(col == 9) {
      this.mode = 8 - row;
      this.switchMode(session);
    } else if (this.mode > MODE_SEND_B) {
      this.operateControls(data1);
    }
  }
};

MixerMode.prototype.uploadValue = function(mode, index, value) {
  switch(mode) {
    case MODE_VOLUME:
      break;
    case MODE_PAN:
      clip_launcher_view.view.getItemAt(index).pan().value().set(value / 127);
      this.ignore_flag[MODE_PAN][index] = true;
      break;
    case MODE_SEND_A:
    case MODE_SEND_B:
    default:
      break;
  }
}

MixerMode.prototype.sendValues = function(session) {
  // Send the current values we have
  for(let i = 0; i < 8; i++) {
    session.sendMidi(0xB4, CC_MAPS[this.mode][i], this.fader_values[this.mode][i]);
  }
}

MixerMode.prototype.setupFaders = function(session) {
  // Setup Faders
  let command = "0100";
  if(this.vertical) {
    command += "00";
  } else {
    command += "01";
  }

  for(let i = 0; i < 8; i++) {
    let colorString = ("0" + this.fader_colors[i].toString(16)).substr(-2);
    let type;
    if(this.fader_bi[this.mode][i]) {
      type = "01";
    } else {
      type = "00";
    }
    let cc = ("0" + CC_MAPS[this.mode][i].toString(16)).substr(-2);
    let index = ("0" + i.toString(16)).substr(-2);
    println(`${index} ${type} ${cc} ${colorString} ${this.fader_colors[i].toString(16)} ${this.fader_colors[i]}`);
    command += (index + type + cc + colorString);
  }

  session.sendSysex(command);
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

    this.sendValues(session);
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
