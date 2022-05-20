/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.contingency.CimContingencyCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.ContingencySeries;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.MonitoredSeries;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CimCracUtils {
    private CimCracUtils() { }

    public static Optional<Contingency> getContingencyFromCrac(ContingencySeries cimContingency, CimCracCreationContext cracCreationContext) {
        CimContingencyCreationContext ccc = cracCreationContext.getContingencyCreationContext(cimContingency.getMRID());
        if (ccc == null) {
            return Optional.empty();
        }
        String createdContingencyId = ccc.getCreatedContingencyId();
        Contingency contingency = cracCreationContext.getCrac().getContingency(createdContingencyId);
        return Objects.isNull(contingency) ? Optional.empty() : Optional.of(contingency);
    }

    public static Set<FlowCnec> getFlowCnecsFromCrac(MonitoredSeries monitoredSeries, CimCracCreationContext cracCreationContext) {
        MonitoredSeriesCreationContext mscc = cracCreationContext.getMonitoredSeriesCreationContext(monitoredSeries.getMRID());
        if (mscc == null) {
            return new HashSet<>();
        }
        return mscc.getCreatedCnecIds().stream().map(cnecId -> cracCreationContext.getCrac().getFlowCnec(cnecId)).collect(Collectors.toSet());
    }

    public static Set<FlowCnec> getFlowCnecsFromCrac(String zoneId, CimCracCreationContext cracCreationContext) {
        Objects.requireNonNull(zoneId);
        return cracCreationContext.getMonitoredSeriesCreationContexts().values().stream()
            .filter(mscc -> zoneId.equals(mscc.getInZoneId()) || zoneId.equals(mscc.getOutZoneId()))
            .map(MonitoredSeriesCreationContext::getCreatedCnecIds)
            .flatMap(Set::stream)
            .map(cnecId -> cracCreationContext.getCrac().getFlowCnec(cnecId))
            .collect(Collectors.toSet());
    }

}
