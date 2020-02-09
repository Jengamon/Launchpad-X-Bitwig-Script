var colors = {
  "000000": 0,
  "ff5706": 0x54,
  "d99d10": 0x3D,
  "545454": 0x75,
  "7a7a7a": 0x76,
  "c9c9c9": 0x77,
  "8689ac": 0x74,
  "a37943": 0x3D,
  "c69f70": 0x7E,
  "00a694": 0x41,
  "5761c6": 0x2D,
  "848ae0": 0x2C,
  "9549cb": 0x36,
  "bc76f0": 0x35,
  "0099d9": 0x27,
  "44c8ff": 0x25,
  "43d2b9": 0x21,
  "009d47": 0x1B,
  "3ebb62": 0x19,
  "d93871": 0x39,
  "e16691": 0x38,
  "d92e24": 0x6A,
  "ec6157": 0x6B,
  "ff833e": 0x6C,
  "e4b74e": 0x3E,
  "739814": 0x13,
  "a0c04c": 0x11,
  "808080": 0x1,
}

function find_novation_color(red, green, blue) {
  let redc = ('0' + Math.round(255 * red).toString(16)).substr(-2);
  let greenc = ('0' + Math.round(255 * green).toString(16)).substr(-2);
  let bluec = ('0' + Math.round(255 * blue).toString(16)).substr(-2);

  // println(`color attempted ${redc}${greenc}${bluec}`);
  let key = `${redc}${greenc}${bluec}`;
  if(colors[key] != undefined) {
    return colors[key];
  } else {
    println(`Didn't find color ${redc}${greenc}${bluec}, defaulting to index 1.`);
    return 1;
  }
}
