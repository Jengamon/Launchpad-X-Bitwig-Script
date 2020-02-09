// Manages a 16-by-16 grid of pads in drum mode
function PadHelper(dp, offset) {
  // this.drumpad_mode = dp;
  this.offset = offset;
  this.colors = [
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0
  ];
  this.content = [
    false, false, false, false,
    false, false, false, false,
    false, false, false, false,
    false, false, false, false,
  ];
  this.solo = [
    false, false, false, false,
    false, false, false, false,
    false, false, false, false,
    false, false, false, false,
  ];
  this.mute = [
    false, false, false, false,
    false, false, false, false,
    false, false, false, false,
    false, false, false, false,
  ];

  // Setup callbacks as proper.
  let ph = this;
  for(let i = 0; i < 16; i++) {
    let index = offset + i;
    let item = dp.dp_bank.getItemAt(index);
    item.color().addValueObserver((r, g, b) => {
      let ci = find_novation_color(r, g, b);
      ph.colors[i] = ci;
    });
    item.solo().addValueObserver((solo) => {
      ph.solo[i] = solo;
    });
    item.mute().addValueObserver((mute) => {
      ph.mute[i] = mute;
    });
    item.exists().addValueObserver((exists) => {
      ph.content[i] = exists;
    });
  }
}

PadHelper.prototype.getPadNumber = function(index) {
  return 36 + this.offset + index;
};

PadHelper.prototype.anySoloed = function() {
  return this.solo.indexOf(true) != -1;
}

PadHelper.prototype.isValid = function(i, anySoloed) {
  return this.content[i] && (!anySoloed || this.solo[i]) && !this.mute[i];
}

PadHelper.prototype.draw = function(session, mode, ex, anySoloed) {
  let except = ex || [];
  for(let i = 0; i < 16; i++) {
    let pad = this.getPadNumber(i);
    if(this.isValid(i, anySoloed) && except.indexOf(i) == -1) {
      mode.drawSolid(session, pad, this.colors[i]);
    }
  }
};
