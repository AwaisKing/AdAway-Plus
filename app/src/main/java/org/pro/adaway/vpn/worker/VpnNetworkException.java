package org.pro.adaway.vpn.worker;

public class VpnNetworkException extends Exception {
    private static final long serialVersionUID = -5734051054215564675L;

    public VpnNetworkException(final String s) {
        super(s);
    }

    public VpnNetworkException(final String s, final Throwable t) {
        super(s, t);
    }
}