// Handles custom note forwarding and holds the i/o ports (handles sending sysex and midi)
const SYSEX_HEADER = "f0 00 20 29 02 0c";

function Session() {
  this.daw_in = host.getMidiInPort(0);
  this.daw_out = host.getMidiOutPort(0);

  this.custom_in = host.getMidiInPort(1);
  this.custom_out = host.getMidiOutPort(1);

  // Setup MIDI forwarding from custom inputs to DAW w/o consuming
  let ni = this.custom_in.createNoteInput("", "??????");
  ni.setShouldConsumeEvents(false);
  this.note_input = ni;

  // Switch to Live mode (if not already)
  this.sendSysex("0e 00");
  // Switch on DAW mode (if not already)
  this.sendSysex("10 01");
  // Clear daw mode
  this.sendSysex("12 00 00 00");
  // Swap to session mode
  this.sendSysex("00 00");

  this.forceSend();
}

Session.prototype.setCallbacks = function(dawMidi, dawSysex, custMidi, custSysex) {
  if(dawMidi && typeof dawMidi == "function") {
    this.daw_in.setMidiCallback(dawMidi);
  }

  if(dawSysex && typeof dawSysex == "function") {
    this.daw_in.setSysexCallback(dawSysex);
  }

  if(custMidi && typeof custMidi == "function") {
    this.custom_in.setMidiCallback(custMidi);
  }

  if(custSysex && typeof custSysex == "function") {
    this.custom_in.setSysexCallback(custSysex);
  }
};

Session.prototype.sendSysex = function(data) {
  // println("Sending to machine " + `${SYSEX_HEADER} ${data} f7`);
  this.daw_out.sendSysex(`${SYSEX_HEADER} ${data} f7`);
};

Session.prototype.sendMidi = function(status, data1, data2) {
  this.daw_out.sendMidi(status, data1, data2);
};

Session.prototype.parseSysex = function(sysex) {
  // Header length is 12 characters
  let meat = sysex.substr(12, sysex.length - 14);
  return {command: meat.substring(0, 2), data: meat.substring(2)};
};

Session.prototype.shutdown = function() {
  // Return to standalone mode
  this.sendSysex("10 00");
  this.forceSend();
};

// Bitwig has a weird thing where it seems to buffer MIDI/Sysex output,
// so to force it to send messages, we just fill the buffer with trash
Session.prototype.forceSend = function() {
  for(let i = 0; i < 100; i++) {
    this.sendMidi(0, 0, 0);
  }
}
