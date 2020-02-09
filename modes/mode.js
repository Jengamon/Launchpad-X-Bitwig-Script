// Defines the abstract interface for modes, that each mode can redefine.
function Mode() {
  throw new Error("Should be overriden");
}

Mode.prototype.initialize = function() {
  this.except = [];
  this.cc_except = [];
};

Mode.prototype.activeColor = function() { return 0x1; }
Mode.prototype.onMidiIn = function(session, status, data1, data2) {};
Mode.prototype.onSysexIn = function(session, sysex) {};
Mode.prototype.redraw = function(session) {};
Mode.prototype.onActivate = function(session) {
  // Set current mode indicator light to proper color
  session.sendMidi(0xB0, 99, this.activeColor());
};
Mode.prototype.onDeactivate = function(session) {
};
Mode.prototype.onLeave = function(session) {
  // Set current mode indicator light to 1
  session.sendMidi(0xB0, 99, 1);
};

// Utilities

Mode.prototype.drawCCSolid = function(session, id, color) {
  session.sendMidi(0xB0, id, color);
  this.cc_except.push(id);
};

Mode.prototype.drawCCFlash = function(session, id, color) {
  session.sendMidi(0xB1, id, color);
  this.cc_except.push(id);
};

Mode.prototype.drawCCPulse = function(session, id, color) {
  session.sendMidi(0xB2, id, color);
  this.cc_except.push(id);
};

Mode.prototype.drawSolid = function(session, row, col, color) {
  session.sendMidi(0x90, this.getPadNumber(row, col), color);
  this.except.push(this.getPadNumber(row, col));
};

Mode.prototype.drawFlash = function(session, row, col, color) {
  session.sendMidi(0x91, this.getPadNumber(row, col), color);
  this.except.push(this.getPadNumber(row, col));
};

Mode.prototype.drawPulse = function(session, row, col, color) {
  session.sendMidi(0x92, this.getPadNumber(row, col), color);
  this.except.push(this.getPadNumber(row, col));
};

Mode.prototype.operateControls = function(data1) {
  switch(data1) {
    case 91:
      clip_launcher_view.view.sceneBank().scrollPosition().inc(-1);
      break;
    case 92:
      clip_launcher_view.view.sceneBank().scrollPosition().inc(1);
      break;
    case 93:
      clip_launcher_view.view.channelScrollPosition().inc(-1);
      break;
    case 94:
      clip_launcher_view.view.channelScrollPosition().inc(1);
      break;
    default:
      host.errorln(`Unknown Control ${data1}`)
  }
};

Mode.prototype.launchPad = function(data1) {
  let track = parseInt((''+data1)[1], 10) - 1;
  let scene = 8 - parseInt((''+data1)[0], 10);
  let pad = clip_launcher_view.view.getItemAt(track).clipLauncherSlotBank().getItemAt(scene);
  pad.launch();
};

let row_offsets = [80, 70, 60, 50, 40, 30, 20, 10];

Mode.prototype.getPadNumber = function(row, col) {
  return row_offsets[row] + col + 1;
};

// Generates a list of all the pads on the controller
Mode.prototype._allPads = function() {
  let list = [];
  for(let i = 0; i < 8; i++) {
    for(let j = 0; j < 8; j++) {
      list.push(this.getPadNumber(i, j));
    }
  }
  return list;
}

Mode.prototype.clearPad = function(session, pad) {
  session.sendMidi(0x90, pad, 0);
  session.sendMidi(0x91, pad, 0);
  session.sendMidi(0x92, pad, 0);
};

Mode.prototype.clearCCPad = function(session, pad) {
  session.sendMidi(0xB0, pad, 0);
  session.sendMidi(0xB1, pad, 0);
  session.sendMidi(0xB2, pad, 0);
};

Mode.prototype.clearUnused = function(session, ex) {
  let except = this.except;
  this.except = [];
  // Clear any pads and cols NOT on the list
  let all_pads = this._allPads();
  for(let i = 0; i < all_pads.length; i++) {
    let ei = except.indexOf(all_pads[i]);
    if(ei == -1) {
      this.clearPad(session, all_pads[i]);
    }
  }

  except = this.cc_except;
  this.cc_except = [];
  let all_cc = [91, 92, 93, 94, 89, 79, 69, 59, 49, 39, 29, 19];
  for(let i = 0; i < all_cc.length; i++) {
    let ei = except.indexOf(all_cc[i]);
    if(ei == -1) {
      this.clearCCPad(session, all_cc[i]);
    }
  }
};
