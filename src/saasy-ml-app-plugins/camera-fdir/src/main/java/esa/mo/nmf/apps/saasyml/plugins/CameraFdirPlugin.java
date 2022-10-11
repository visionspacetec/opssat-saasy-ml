package esa.mo.nmf.apps.saasyml.plugins;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class CameraFdirPlugin extends Plugin {

    public CameraFdirPlugin(PluginWrapper wrapper) {
        super(wrapper);

        // you can use "wrapper" to have access to the plugin context (plugin manager, descriptor, ...)
    }

    @Override
    public void start() {
        System.out.println("CameraFdirPlugin.start()");
    }

    @Override
    public void stop() {
        System.out.println("CameraFdirPlugin.stop()");
    }
    
    @Override
    public void delete() {
        System.out.println("CameraFdirPlugin.delete()");
    }
}
