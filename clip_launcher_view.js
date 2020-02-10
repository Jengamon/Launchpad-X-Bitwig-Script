// An object that simplies drawing the current state of the clip launcher.
// As a side effect, holds the current track view

// BOTTOM ROW MODES
const BRM_NORMAL = 0;
const BRM_STOP = 1;
const BRM_MUTE = 2;
const BRM_SOLO = 3;
const BRM_RECORD = 4;

// COLORS
const PLAY_COLOR = 0x1A;
const RECORD_COLOR = 0x5;
const ARM_COLOR = 0x79;

const STOPPED_COLOR = 0x79;
const PLAYING_COLOR = 0x78;

const SOLO_ON_COLOR = 0x7C;
const SOLO_OFF_COLOR = 0x7D;

const MUTE_ON_COLOR = 0x54;
const MUTE_OFF_COLOR = 0x53;

function TrackState() {
  this.clip_color = [0, 0, 0, 0, 0, 0, 0, 0];
  this.clip_content = [false, false, false, false, false, false, false, false];
  this.clip_state = [0, 0, 0, 0, 0, 0, 0, 0];
  this.clip_queued = [false, false, false, false, false, false, false, false];

  this.solo = false;
  this.mute = false;
  this.stopped = false;
  this.exists = false;
  this.armed = false;
}

function ClipLauncherView() {
  this.view = host.createTrackBank(8, 0, 8, false);

  let clv = this;

  this.track_pos = 0;
  this.max_tracks = 0;
  this.scene_pos = 0;
  this.max_scenes = 0;
  this.view.channelScrollPosition().addValueObserver((csp) => {
    let scsp = Math.max(Math.min(clv.max_tracks - 8, csp), 0);
    if(scsp != csp) {
      clv.view.channelScrollPosition().set(scsp);
    } else {
      clv.track_pos = scsp;
    }
  });
  this.view.channelCount().addValueObserver((cc) => { clv.max_tracks = cc; });
  this.view.sceneBank().scrollPosition().addValueObserver((sbci) => { clv.scene_pos = sbci; });
  this.view.sceneBank().itemCount().addValueObserver((sbic) => { clv.max_scenes = sbic; });

  // Follow the main selected track
  arranger_track.position().addValueObserver((pos) => {
    let psp;
    if(follow_pref.get()) {
      clv.view.scrollIntoView(pos);
    }
  });

  // Set indications...
  this.view.sceneBank().setIndication(true);
  for(let i = 0; i < 8; i++) {
    this.view.getItemAt(i).clipLauncherSlotBank().setIndication(true);
  }

  this.track_states = [];

  for(let i = 0; i < 8; i++) {
    let ts = new TrackState();
    let track = this.view.getItemAt(i);
    let clsb = track.clipLauncherSlotBank();
    clsb.addPlaybackStateObserver((s, ps, q) => {
      ts.clip_state[s] = ps;
      ts.clip_queued[s] = q;
      host.requestFlush();
    });
    for(let j = 0; j < 8; j++) {
      clsb.getItemAt(j).hasContent().addValueObserver((exists) => {
        // println(`J == ${j} for track ${i} ${exists}`)
        ts.clip_content[j] = exists;
        host.requestFlush();
      });
    }
    clsb.addColorObserver((s, r, g, b) => {
      let color = find_novation_color(r, g, b);
      ts.clip_color[s] = color;
      host.requestFlush();
    });

    track.solo().addValueObserver((solo) => { ts.solo = solo; host.requestFlush(); });
    track.mute().addValueObserver((mute) => { ts.mute = mute; host.requestFlush(); });
    track.arm().addValueObserver((arm) => { ts.armed = arm; host.requestFlush(); });
    track.isStopped().addValueObserver((isStop) => { ts.stopped = isStop; host.requestFlush(); });
    track.exists().addValueObserver((exists) => { ts.exists = exists; host.requestFlush(); });

    this.track_states.push(ts);
  }
}

ClipLauncherView.prototype.generate = function(row, col, session, mode) {
  let ts = this.track_states[col];
  let exists = ts.clip_content[row];
  let color = ts.clip_color[row];
  let queued = ts.clip_queued[row];
  let state = ts.clip_state[row];
  let armed = ts.armed;
  // println(`CLV ${row} ${col} ${exists} ${color} ${queued} ${armed}`)
  if(exists) {
    switch(state) {
      case 0:
        mode.drawSolid(session, row, col, color);
        if(queued) {
          mode.drawFlash(session, row, col, 0x05);
        }
        break;
      case 1:
        mode.drawSolid(session, row, col, color);
        if(queued) {
          mode.drawFlash(session, row, col, PLAY_COLOR);
        } else {
          mode.drawPulse(session, row, col, color);
        }
        break;
      case 2:
        mode.drawSolid(session, row, col, color);
        if(queued) {
          mode.drawPulse(session, row, col, RECORD_COLOR);
        } else {
          mode.drawFlash(session, row, col, RECORD_COLOR);
        }
        break;
      default:
        host.errorln(`Invalid pad state: ${row} ${col} ${state}`);
    }
  } else {
    if(armed) {
      mode.drawSolid(session, row, col, ARM_COLOR);
    }

    switch(state) {
      case 0: // stopped
      case 1: // playing
        // Impossible or trivial. Do nothing.
        break;
      case 2: // recording
        if(queued) {
          mode.drawPulse(session, row, col, RECORD_COLOR);
        } else {
          mode.drawFlash(session, row, col, RECORD_COLOR);
        }
        break;
      default:
        host.errorln(`Invalid pad state: ${row} ${col} ${state}`);
    }
  }
}

// Expects a Session, then a Mode object
// Produces the Mode object calls necessary to draw the current CL state
ClipLauncherView.prototype.draw = function(session, mode, _brm, controls) {
  let brm = _brm || BRM_NORMAL;
  for(let i = 0; i < 7; i++) {
    for(let j = 0; j < this.track_states.length; j++) {
      this.generate(i, j, session, mode);
    }
  }

  if(controls) {
    if(this.track_pos > 0) {
      mode.drawCCSolid(session, 93, 0x54);
    }
    if(this.track_pos + 8 < this.max_tracks) {
      mode.drawCCSolid(session, 94, 0x54);
    }
    if(this.scene_pos > 0) {
      mode.drawCCSolid(session, 91, 0x54);
    }
    if(this.scene_pos + 8 < this.max_scenes) {
      mode.drawCCSolid(session, 92, 0x54);
    }
  }

  // For the final row, switch it based off of the brm
  switch(brm) {
    case BRM_NORMAL:
      for(let j = 0; j < this.track_states.length; j++) {
        this.generate(7, j, session, mode);
      }
      break;
    case BRM_STOP:
      for(let j = 0; j < this.track_states.length; j++) {
        let ts = this.track_states[j];
        if(ts.exists) {
          if(ts.stopped) {
            mode.drawSolid(session, 7, j, STOPPED_COLOR);
          } else {
            mode.drawSolid(session, 7, j, PLAYING_COLOR);
          }
        }
      }
      break;
    case BRM_MUTE:
      for(let j = 0; j < this.track_states.length; j++) {
        let ts = this.track_states[j];
        if(ts.exists) {
          if(ts.mute) {
            mode.drawSolid(session, 7, j, MUTE_ON_COLOR);
          } else {
            mode.drawSolid(session, 7, j, MUTE_OFF_COLOR);
          }
        }
      }
      break;
    case BRM_SOLO:
      for(let j = 0; j < this.track_states.length; j++) {
        let ts = this.track_states[j];
        if(ts.exists) {
          if(ts.solo) {
            mode.drawSolid(session, 7, j, SOLO_ON_COLOR);
          } else {
            mode.drawSolid(session, 7, j, SOLO_OFF_COLOR);
          }
        }
      }
      break;
    case BRM_RECORD:
      for(let j = 0; j < this.track_states.length; j++) {
        let ts = this.track_states[j];
        if(ts.exists) {
          if(ts.armed) {
            mode.drawSolid(session, 7, j, PLAYING_COLOR);
          } else {
            mode.drawSolid(session, 7, j, STOPPED_COLOR);
          }
        }
      }
      break;
    default:
      host.errorln(`Unknown BOTTOM_ROW_MODE: ${brm}`);
      break;
  }
}
