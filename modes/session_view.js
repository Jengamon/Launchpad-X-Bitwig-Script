// A Session view mode.
function SessionViewMode() {
  Mode.prototype.initialize.call(this);
}

SessionViewMode.prototype = Object.create(Mode.prototype);

SessionViewMode.prototype.activeColor = function() { return 0x54; }

SessionViewMode.prototype.redraw = function(session) {
  // Draw the entire clip launcher view.
  clip_launcher_view.draw(session, this, BRM_NORMAL, true);

  this.clearUnused(session);
};

SessionViewMode.prototype.onMidiIn = function(session, status, data1, data2) {
  if(data2 > 0 && status == 0x90) { // On pad press
    // println(`[SVM] ${status.toString(16)} ${data1} ${data2} ${scene} ${track}`);
    // Launch the requisite clip
    this.launchPad(data1);
  } else if (data2 > 0 && status == 0xB0) { // On control press
    switch(data1) {
        case 89:
          clip_launcher_view.view.sceneBank().launch(0);
          break;
        case 79:
          clip_launcher_view.view.sceneBank().launch(1);
          break;
        case 69:
          clip_launcher_view.view.sceneBank().launch(2);
          break;
        case 59:
          clip_launcher_view.view.sceneBank().launch(3);
          break;
        case 49:
          clip_launcher_view.view.sceneBank().launch(4);
          break;
        case 39:
          clip_launcher_view.view.sceneBank().launch(5);
          break;
        case 29:
          clip_launcher_view.view.sceneBank().launch(6);
          break;
        case 19:
          clip_launcher_view.view.sceneBank().launch(7);
          break;
        default:
          this.operateControls(data1);
    }
  }
};

SessionViewMode.prototype.onSysexIn = function(session, sysex) {
  let command = sysex.command.hexByteAt(0);
  switch(command) {
    case 0x00: // Mode switch
    case 0x0d: // Fader velocity
    case 0x0e: // Prog/Live toggle
      // Just ignore these
      break;
    default:
      println(`[SVM] Unknown SYSEX command ${command.toString(16)}: ${sysex.data}`);
      break;
  }
};
