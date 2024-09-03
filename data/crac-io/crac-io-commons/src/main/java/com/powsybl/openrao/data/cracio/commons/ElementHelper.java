/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons;

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
