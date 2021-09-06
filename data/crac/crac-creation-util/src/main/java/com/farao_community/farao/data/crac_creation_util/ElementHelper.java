package com.farao_community.farao.data.crac_creation_util;

public interface ElementHelper {

    /**
     * Returns a boolean indicating whether or not the element is considered valid
     * in the network
     */
    boolean isValid();

    /**
     * If the element is not valid, returns the reason why it is considered invalid
     */
    String getInvalidReason();

    /**
     * If the element is valid, returns its corresponding id in the PowSyBl Network
     */
    String getIdInNetwork();

}
