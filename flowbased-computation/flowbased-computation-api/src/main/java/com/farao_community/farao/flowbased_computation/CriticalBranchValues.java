/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

/**
 * Critical branches description
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */

public class CriticalBranchValues {
    private final String id;
    private final String name;
    private final double uNominal; // V
    private final double iMax; // A
    private final double powerFlow; //MW
    private final String direction; // DIRECT or OPPOSITE

    /**
     * Constructor
     *
     * @param id Id of the branch
     * @param name Name of the branch
     * @param uNominal nominal power on the branch
     * @param iMax current on the branch
     * @param direction the direction of the power flow
     */
    public CriticalBranchValues(String id, String name, double uNominal, double iMax, String direction) {
        this.id = id;
        this.name = name;
        this.uNominal = uNominal;
        this.iMax = iMax;
        this.powerFlow = uNominal * iMax * Math.sqrt(3.0) / 1000.0;
        this.direction = direction;
    }

    /**
     * Get the Id of the branch
     *
     * @return the Id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the name of the branch
     *
     * @return the name of the branch
     */
    public String getName() {
        return name;
    }

    /**
     * Get the value of the nominal power
     *
     * @return the value of the nominal power
     */
    public double getuNominal() {
        return uNominal;
    }

    /**
     * Get the value of the current
     *
     * @return the value of the current
     */
    public double getiMax() {
        return iMax;
    }


    /**
     * Get the value of the power flow
     *
     * @return the value of the power flow
     */
    public double getPowerFlow() {
        return powerFlow;
    }


    /**
     * Get the direction of the flow
     *
     * @return the direction
     */
    public String getDirection() {
        return direction;
    }
}
