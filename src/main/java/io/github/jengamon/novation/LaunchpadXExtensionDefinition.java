package io.github.jengamon.novation;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchpadXExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("54555913-b867-4c61-8866-5e79ca63aa88");

   public LaunchpadXExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "Launchpad X (API 10)";
   }

   @Override
   public String getAuthor()
   {
      return "Jengamon";
   }

   @Override
   public String getVersion()
   {
      return "0.5-java";
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
      return 10;
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
         // TODO: Set the correct names of the ports for auto detection on Windows platform here
         // and uncomment this when port names are correct.
         // list.add(new String[]{"Input Port 0", "Input Port 1"}, new String[]{"Output Port 0", "Output Port 1"});
      }
      else if (platformType == PlatformType.LINUX)
      {
         // TODO: Set the correct names of the ports for auto detection on Windows platform here
         // and uncomment this when port names are correct.
         // list.add(new String[]{"Input Port 0", "Input Port 1"}, new String[]{"Output Port 0", "Output Port 1"});
      }
   }

   @Override
   public LaunchpadXExtension createInstance(final ControllerHost host)
   {
      return new LaunchpadXExtension(this, host);
   }
}