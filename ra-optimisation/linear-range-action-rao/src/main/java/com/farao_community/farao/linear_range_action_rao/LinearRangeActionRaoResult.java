/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_range_action_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.powsybl.commons.extensions.AbstractExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRangeActionRaoResult extends AbstractExtension<RaoComputationResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearRangeActionRaoResult.class);

    public enum SecurityStatus {
        SECURED,
        UNSECURED,
        UNKNOWN
    }

    private SecurityStatus securityStatus;

    private double minMargin;

    @Override
    public String getName() {
        return "LinearRangeActionRaoResult";
    }

    public LinearRangeActionRaoResult() {
        this.securityStatus = SecurityStatus.UNKNOWN;
        this.minMargin = Double.MAX_VALUE;
    }

    public LinearRangeActionRaoResult(SecurityStatus securityStatus) {
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
            LOGGER.info("Updated LinearRangeActionRaoResult to: mininum margin = {}, security status: {}",
                    this.minMargin,
                    this.securityStatus);
        }
    }

    public double getMinMargin() {
        return minMargin;
    }

    public void setMinMargin(double minMargin) {
        this.minMargin = minMargin;
    }

}
