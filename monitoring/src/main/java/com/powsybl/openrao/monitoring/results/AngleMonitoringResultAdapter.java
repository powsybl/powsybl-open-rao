/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.impl.AngleCnecValue;
import com.powsybl.openrao.data.raoresult.api.extension.AngleResult;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class AngleMonitoringResultAdapter {
    private AngleMonitoringResultAdapter() {
    }

    public static AngleResult convertToAngleExtension(MonitoringResult angleMonitoringResult) {
        AngleResult angleExtension = new AngleResult();
        angleMonitoringResult.getCnecResults().forEach(angleResult -> angleExtension.addMeasurement(((AngleCnecValue) angleResult.getValue()).value(), angleResult.getCnec().getState().getInstant(), (AngleCnec) angleResult.getCnec(), Unit.DEGREE));
        return angleExtension;
    }
}
