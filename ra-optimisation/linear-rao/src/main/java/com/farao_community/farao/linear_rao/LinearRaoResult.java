/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRaoResult extends AbstractExtension<RaoComputationResult> {

    public enum SecurityStatus {
        SECURED,
        UNSECURED,
        UNKNOWN
    }

    private SecurityStatus securityStatus;

    private double minMargin;

    @Override
    public String getName() {
        return "LinearRaoResult";
    }

    public LinearRaoResult() {
        this.securityStatus = SecurityStatus.UNKNOWN;
        this.minMargin = Double.MAX_VALUE;
    }

    public LinearRaoResult(SecurityStatus securityStatus) {
        this.securityStatus = securityStatus;
        this.minMargin = Double.MAX_VALUE;
    }

    public SecurityStatus getSecurityStatus() {
        return securityStatus;
    }

    public void setSecurityStatus(SecurityStatus securityStatus) {
        this.securityStatus = securityStatus;
    }

    public void updateResult(double margin) {
        if (margin < this.minMargin) {
            this.minMargin = margin;
            if (minMargin < 0.0) {
                this.setSecurityStatus(SecurityStatus.UNSECURED);
            }
        }
    }

    public double getMinMargin() {
        return minMargin;
    }

    public void setMinMargin(double minMargin) {
        this.minMargin = minMargin;
    }

}
