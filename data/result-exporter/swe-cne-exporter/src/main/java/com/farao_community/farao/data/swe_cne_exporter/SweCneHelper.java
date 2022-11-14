/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.data.cne_exporter_commons.CneExporterParameters;
import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SweCneHelper extends CneHelper {
    private AngleMonitoringResult angleMonitoringResult;

    public SweCneHelper(Crac crac, Network network, RaoResult raoResult, AngleMonitoringResult angleMonitoringResult, RaoParameters raoParameters, CneExporterParameters exporterParameters) {
        super(crac, network, raoResult, raoParameters, exporterParameters);
        this.angleMonitoringResult = angleMonitoringResult;
    }

    public AngleMonitoringResult getAngleMonitoringResult() {
        return angleMonitoringResult;
    }
}
