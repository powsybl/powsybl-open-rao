package com.farao_community.farao.data.crac_creation_util;

public interface HvdcElementHelper extends ElementHelper {

    /**
     * If the HVDC element is valid, returns a boolean indicating whether or not the element is
     * inverted in the network, compared to the orientation originally used in the constructor
     * of the helper
     */
    boolean isInvertedInNetwork();

    // if there is a need to: add here getInitialSetpoint(), getMaxActivePower(), getMinActivePower()
}
