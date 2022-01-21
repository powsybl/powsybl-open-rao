/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MaxMinMarginParameters {
    private final double pstPenaltyCost;
    private final double hvdcPenaltyCost;
    private final double injectionPenaltyCost;

    public MaxMinMarginParameters(double pstPenaltyCost, double hvdcPenaltyCost, double injectionPenaltyCost) {
        this.pstPenaltyCost = pstPenaltyCost;
        this.hvdcPenaltyCost = hvdcPenaltyCost;
        this.injectionPenaltyCost = injectionPenaltyCost;
    }

    public final double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public final double getHvdcPenaltyCost() {
        return hvdcPenaltyCost;
    }

    public final double getInjectionPenaltyCost() {
        return injectionPenaltyCost;
    }
}
