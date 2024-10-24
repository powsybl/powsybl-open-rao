/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons;

import com.powsybl.iidm.network.TwoSides;

public interface CnecElementHelper extends ElementHelper {

    /**
     * If the CNEC element is valid, returns a boolean indicating whether or not the element is
     * inverted in the network, compared to the orientation originally used in the constructor
     * of the helper
     */
    boolean isInvertedInNetwork();

    /**
     * If the CNEC element is valid, returns the nominal voltage on a given side of the element
     * The side corresponds to the side of the element in the network, which might be inverted
     * (see isInvertedInNetwork()).
     */
    double getNominalVoltage(TwoSides side);

    /**
     * If the CNEC element is valid, returns the current limit on a given side of the Branch.
     * The side corresponds to the side of the branch in the network, which might be inverted
     * (see isInvertedInNetwork()).
     */
    double getCurrentLimit(TwoSides side);

    /**
     * If the CNEC element is valid, returns a boolean indicating whether or not the element is
     * the half-line of a tie-line
     */
    boolean isHalfLine();

    /**
     * If the CNEC element is a valid half-line, returns a boolean indicating which half of the
     * tie-line is actually targeted by the CNEC definition
     */
    TwoSides getHalfLineSide();
}
