package io.github.jengamon.novation.internal;

import com.bitwig.extension.controller.api.ControllerHost;

import java.io.OutputStream;

/**
 * Buffers output for the ControllerHost, so we can use System.out.println...
 * @author Jengamon
 */
public class HostOutputStream extends OutputStream {
    private final ControllerHost mHost;
    private String mBuffer = "";


    public HostOutputStream(ControllerHost host) {
        mHost = host;
    }

    @Override
    public void write(int b) {
        switch((char)b) {
            case '\n':
                mHost.println(mBuffer);
                mBuffer = "";
                break;
            default:
                mBuffer += (char) b;
        }
    }
}
