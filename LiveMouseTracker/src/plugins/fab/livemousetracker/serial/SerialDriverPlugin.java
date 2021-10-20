package plugins.fab.livemousetracker.serial;

import icy.plugin.PluginLoader;
import icy.plugin.abstract_.Plugin;

public class SerialDriverPlugin extends Plugin {

	public SerialDriverPlugin() {
		super();
		System.out.println("Serial driver plugin launched.");
		PluginLoader.waitWhileLoading();

		try
		{
			prepareLibrary("jssc");
		}
		catch (UnsatisfiedLinkError e1)
		{
	        	System.out.println("Warning: " + e1.getMessage());
	        	System.out.println("Couldn't prepare library...");
		}

		try
		{
			// load native library
			System.loadLibrary("jssc");
		}
		catch (UnsatisfiedLinkError e1)
		{
	        System.out.println("Warning: " + e1.getMessage());
	        System.out.println("Alternate method...");

	        try
			{
				loadLibrary("jssc");
			}
			catch (UnsatisfiedLinkError e2)
			{

			    if (e2.getMessage().contains("already loaded"))
			        System.out.println("Warning: " + e2.getMessage());
			    else
			        throw e2;
			}
	    }
		System.out.println("Serial driver plugin finished.");
	}
}

/*
try
{
    // load native librarie
    if (SystemUtil.is32bits())
    {
    	System.out.println("32 bits system not supported.");
    	return null;
//        plugin.prepareLibrary("ufdw_j4k_32bit");
//        plugin.prepareLibrary("ufdw_j4k2_32bit");
    }
    else
    {
        plugin.prepareLibrary("ufdw_j4k_64bit");
        plugin.prepareLibrary("ufdw_j4k2_64bit");
    }
}
catch (UnsatisfiedLinkError e)
{
    // just show a warning
    if (e.getMessage().contains("already loaded"))
        System.out.println("Warning: " + e.getMessage());
    else
        throw e;
}
*/