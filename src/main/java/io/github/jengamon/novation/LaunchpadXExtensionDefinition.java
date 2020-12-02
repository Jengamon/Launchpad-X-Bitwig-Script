package io.github.jengamon.novation;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class LaunchpadXExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("54555913-b867-4c61-8866-5e79ca63aa88");

   public LaunchpadXExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "Launchpad X";
   }

   @Override
   public String getAuthor()
   {
      return "Jengamon";
   }

   @Override
   public String getVersion()
   {
      return "1.2";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Novation";
   }

   @Override
   public String getHardwareModel()
   {
      return "Launchpad X";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 12;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 2;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 2;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      if (platformType == PlatformType.WINDOWS)
      {
         list.add(new String[]{"LPX MIDI", "MIDIIN2 (LPX MIDI)"}, new String[]{"LPX MIDI", "MIDIOUT2 (LPX MIDI)"});
      }
      else if (platformType == PlatformType.MAC)
      {
         // TODO: Find a good guess for the Mac names.
      }
      else if (platformType == PlatformType.LINUX)
      {
         // TODO Find better guess. Get a Linux.
         list.add(new String[]{"Launchpad X MIDI 1", "Launchpad X MIDI 2"}, new String[]{"Launchpad X MIDI 1", "Launchpad X MIDI 2"});
      }
   }

   @Override
   public LaunchpadXExtension createInstance(final ControllerHost host)
   {
      return new LaunchpadXExtension(this, host);
   }
}
