load("./drum_pad/pad_helper.js");

const RECORDING_COLOR = 0x5;
const NORMAL_COLOR = 79;

// Manages drumpad messages and if the cursor device is a drum pad.
function DrumPadMode() {
  Mode.prototype.initialize(this);

  // println(arranger_device);
  this.dp_bank = arranger_device.createDrumPadBank(64);
  this.dp_bank.setIndication(true);
  this.dp_bank.channelScrollPosition().markInterested();
  this.dp_bank.hasSoloedPads().markInterested();
  arranger_track.playingNotes().markInterested();

  // Add arranger track note forwarding
  let dpm = this;
  let lighted = [];
  arranger_track.playingNotes().addValueObserver((note_array) => {
    // Convert note_array into a more reasonable form
    let notes = [];

    // If we are configure to not send the track's current notes, don't send them.
    if(!send_track_notes.get()) {
      return;
    }

    let pert = [];

    for(let i = 0; i < note_array.length; i++) {
      let note = note_array[i];
      notes[note.pitch()] = note.velocity();
      pert.push(note.pitch());
    }

    let new_lighted = [];

    for(let i = 0; i < pert.length; i++) {
      let ni = pert[i];
      if(lighted.indexOf(ni) != -1 || notes[ni] != undefined) {
        session.custom_out.sendMidi(0x90 | dpm.midi_channel, ni, notes[ni]);
        new_lighted.push(ni);
      }
    }

    for(let i = 0; i < lighted.length; i++) {
      let ni = lighted[i];
      if(new_lighted.indexOf(ni) == -1) {
        session.custom_out.sendMidi(0x80 | dpm.midi_channel, ni, 0);
      }
    }

    lighted = new_lighted;
  });

  this.helpers = [];
  for(let i = 0; i < 4; i++) {
    this.helpers.push(new PadHelper(this, 16 * i));
  }

  // this.pressed = {};

  this.midi_channel = 0;

  session.sendSysex("16");
}

DrumPadMode.prototype = Object.create(Mode.prototype);

DrumPadMode.prototype.drawSolid = function(session, pad, color) {
  session.sendMidi(0x98, pad, color);
  this.except.push(pad);
};

DrumPadMode.prototype.drawFlash = function(session, pad, color) {
  session.sendMidi(0x99, pad, color);
  this.except.push(pad);
};

DrumPadMode.prototype.drawPulse = function(session, pad, color) {
  session.sendMidi(0x9A, pad, color);
  this.except.push(pad);
};

DrumPadMode.prototype._allPads = function() {
  let list = [];
  for(let i = 36; i < 100; i++) {
    list.push(i);
  }
  return list;
};

DrumPadMode.prototype.clearPad = function(session, pad) {
  session.sendMidi(0x98, pad, 0);
  session.sendMidi(0x99, pad, 0);
  session.sendMidi(0x9A, pad, 0);
};

DrumPadMode.prototype.toNote = function(pad) {
  return this.dp_bank.channelScrollPosition().get() + (pad - 36);
}

DrumPadMode.prototype.onMidiIn = function(session, status, data1, data2) {
  if(status != 0xB0) {
    // We do have to convert from pad number to note number.
    let nn = this.toNote(data1);
    //let midi_command = status ^ 0x8;
    // We add 12 because at scroll position 0, we are at MIDI note C1
    // println(`[DPM] ${nn} ${(status ^ 0x8 | this.midi_channel).toString(16)} ${data2}`)
    // session.sendMidi(0x9F, 60, data2);
    // Only send notes on valid pads
    let valid = false;
    let id = (data1 - 36);
    let helper = this.helpers[Math.floor(id / 16)];
    let hid = id - helper.offset;
    if(helper.isValid(hid)) {
      session.note_input.sendRawMidiEvent(status ^ 0x8 | this.midi_channel, nn, data2);
      host.requestFlush();
    }
    // this.pressed[data1] = data2 > 0;
  } else {
    // An arrow was pressed! Adjust the scroll window.
    // But keep the 64 notes in view (the position cannot increase above E3 == 62)
    let key = data1 - 91;
    if(data2 > 0) {
      switch(key) {
        case 0:
          if(this.canIncreaseBy(16)) {
            this.dp_bank.channelScrollPosition().inc(16);
          }
          break;
        case 1:
          if(this.canDecreaseBy(16)) {
            this.dp_bank.channelScrollPosition().inc(-16);
          }
          break;
        case 2:
          if(this.canDecreaseBy(4)) {
            this.dp_bank.channelScrollPosition().inc(-4);
          }
          break;
        case 3:
          if(this.canIncreaseBy(4)) {
            this.dp_bank.channelScrollPosition().inc(4);
          }
          break;
        default:
          println(`Unhandled key ${key} (${key + 91})`);
          break;
      }
    }
  }

  session.sendSysex("16");
};

DrumPadMode.prototype.canIncreaseBy = function(by) {
  return (this.dp_bank.channelScrollPosition().get() + by) <= 66;
}

DrumPadMode.prototype.canDecreaseBy = function(by) {
  return (this.dp_bank.channelScrollPosition().get() - by) >= 0;
}

DrumPadMode.prototype.onSysexIn = function(session, sysex) {
  let command = sysex.command.hexByteAt(0);
  switch(command) {
    case 0x16: // Note mode config
      // Set the channel properly
      this.midi_channel = sysex.data.hexByteAt(3);
      break;
    default:
      // If we don't know the command, then the session mode should.
      println(`[DPM] Unknown SYSEX command ${command.toString(16)}: ${sysex.data}`);
      break;
  }
};

DrumPadMode.prototype.redraw = function(session) {
  let anySoloed = this.dp_bank.hasSoloedPads().get();
  for(let i = 0; i < 4; i++) {
    let helper = this.helpers[i];
    let except = [];
    for(let j = 0; j < 16; j++) {
      let pad = helper.getPadNumber(j);

      if((/*this.pressed[pad] ||*/ arranger_track.playingNotes().isNotePlaying(this.toNote(pad))) && helper.isValid(j, anySoloed)) {
        if(recording_active) {
          this.drawSolid(session, pad, RECORDING_COLOR);
        } else {
          this.drawSolid(session, pad, NORMAL_COLOR);
        }
        except.push(j);
      }
    }
    helper.draw(session, this, except, anySoloed);
  }

  if(this.canIncreaseBy(16)) {
    this.drawCCSolid(session, 91, 13);
  }
  if(this.canIncreaseBy(4)) {
    this.drawCCSolid(session, 94, 13);
  }
  if(this.canDecreaseBy(16)) {
    this.drawCCSolid(session, 92, 13);
  }
  if(this.canDecreaseBy(4)) {
    this.drawCCSolid(session, 93, 13);
  }

  this.clearUnused(session);
};
