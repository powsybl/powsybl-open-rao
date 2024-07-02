/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;

/**
 * This abstract implementation uses a Scalable to apply redispatching
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public abstract class AbstractRedispatchAction implements RedispatchAction {
    protected void apply(Network network, double powerToRedispatch, Scalable scalable, ReportNode reportNode) {
        double redispatchedPower = scalable.scale(network, powerToRedispatch);
        if (Math.abs(redispatchedPower - powerToRedispatch) > 1) {
            AngleMonitoringReports.reportRedispatchingFailed(reportNode, powerToRedispatch, redispatchedPower);
        }
    }
}
