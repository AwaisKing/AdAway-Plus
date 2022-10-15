package org.pro.adaway.model.adblocking;

/**
 * This enum represents the ad blocking methods.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public enum AdBlockMethod {
    /**
     * Not defined ad block method.
     */
    UNDEFINED(0),
    /**
     * The system hosts file ad block method.
     */
    ROOT(1),
    /**
     * The VPN based ad block method.
     */
    VPN(2);

    private final int code;

    AdBlockMethod(final int code) {
        this.code = code;
    }

    public static AdBlockMethod fromCode(final int code) {
        for (final AdBlockMethod method : AdBlockMethod.values())
            if (method.code == code) return method;
        return UNDEFINED;
    }

    public int toCode() {
        return this.code;
    }
}
