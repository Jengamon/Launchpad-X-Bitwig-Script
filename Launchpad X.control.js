loadAPI(10);

// Remove this if you want to be able to use deprecated methods without causing script to stop.
// This is useful during development.
host.setShouldFailOnDeprecatedUse(true);

host.defineController("Novation", "Launchpad X", "1.0", "92a7c5e4-47ca-4219-b9ea-5899cedbe1a8", "Jengamon");

host.defineMidiPorts(2, 2);

if (host.platformIsWindows())
{
   // TODO: Set the correct names of the ports for auto detection on Windows platform here
   // and uncomment this when port names are correct.
   host.addDeviceNameBasedDiscoveryPair(["LPX MIDI", "MIDIIN2 (LPX MIDI)"], ["LPX MIDI", "MIDIOUT2 (LPX MIDI)"]);
}
else if (host.platformIsMac())
{
   // TODO: Set the correct names of the ports for auto detection on Mac OSX platform here
   // and uncomment this when port names are correct.
   // host.addDeviceNameBasedDiscoveryPair(["Input Port 0", "Input Port 1"], ["Output Port 0", "Output Port 1"]);
}
else if (host.platformIsLinux())
{
   // TODO: Set the correct names of the ports for auto detection on Linux platform here
   // and uncomment this when port names are correct.
   // host.addDeviceNameBasedDiscoveryPair(["Input Port 0", "Input Port 1"], ["Output Port 0", "Output Port 1"]);
}

// Color support
load("./find_color.js");

// Clip Launcher view support
load("./clip_launcher_view.js");

// Modes
load("./modes/mode.js");
load("./modes/session_view.js");
load("./modes/mixer.js");

// DrumPad Mode
load("./drum_pad.js");

// Session (I/O)
load("./session.js");

// Driver main code
load("./driver.js");
