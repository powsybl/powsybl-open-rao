/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.data.cne_exporter_commons.CneExporterParameters;
import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SweCneHelper extends CneHelper {
    private AngleMonitoringResult angleMonitoringResult;
    private Map<Contingency, Boolean> contingencyFailureMap = new HashMap<>();

    public SweCneHelper(Crac crac, Network network, RaoResult raoResult, AngleMonitoringResult angleMonitoringResult, RaoParameters raoParameters, CneExporterParameters exporterParameters) {
        super(crac, network, raoResult, raoParameters, exporterParameters);
        this.angleMonitoringResult = angleMonitoringResult;
        defineContingencyFailureMap();
    }

    public AngleMonitoringResult getAngleMonitoringResult() {
        return angleMonitoringResult;
    }

    public Map<Contingency, Boolean> getContingencyFailureMap() {
        return contingencyFailureMap;
    }

    private void defineContingencyFailureMap() {
        for (Contingency contingency: getCrac().getContingencies()) {
            // Initialize map with default :
            contingencyFailureMap.put(contingency, false);
            if (Objects.nonNull(getRaoResult()) && getCrac().getStates(contingency).stream()
                    .anyMatch(state -> getRaoResult().getComputationStatus(state).equals(ComputationStatus.FAILURE))) {
                contingencyFailureMap.put(contingency, true);
            }
        }
    }

    public boolean isAnyContingencyInFailure() {
        return contingencyFailureMap.values().stream().anyMatch(value -> value.equals(true));
    }
}
