package io.github.jengamon.novation.internal;

import com.bitwig.extension.controller.api.ControllerHost;

import java.io.OutputStream;

public class HostErrorOutputStream extends OutputStream {
    private final ControllerHost mHost;
    private String mBuffer = "";


    public HostErrorOutputStream(ControllerHost host) {
        mHost = host;
    }

    @Override
    public void write(int b) {
        switch((char)b) {
            case '\n':
                mHost.errorln(mBuffer);
                mBuffer = "";
                break;
            default:
                mBuffer += (char) b;
        }
    }
}
