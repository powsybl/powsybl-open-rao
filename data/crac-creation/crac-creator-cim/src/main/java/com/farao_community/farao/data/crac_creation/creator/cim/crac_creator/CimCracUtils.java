/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator;

import com.powsybl.open_rao.data.crac_api.Contingency;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.contingency.CimContingencyCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.cim.xsd.ContingencySeries;
import com.powsybl.open_rao.data.crac_creation.creator.cim.xsd.MonitoredSeries;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CimCracUtils {
    private CimCracUtils() { }

    public static Contingency getContingencyFromCrac(ContingencySeries cimContingency, CimCracCreationContext cracCreationContext) {
        CimContingencyCreationContext ccc = cracCreationContext.getContingencyCreationContextById(cimContingency.getMRID());
        if (ccc == null || !ccc.isImported()) {
            return null;
        }
        return cracCreationContext.getCrac().getContingency(ccc.getCreatedContingencyId());
    }

    public static Set<FlowCnec> getFlowCnecsFromCrac(MonitoredSeries monitoredSeries, CimCracCreationContext cracCreationContext) {
        MonitoredSeriesCreationContext mscc = cracCreationContext.getMonitoredSeriesCreationContext(monitoredSeries.getMRID());
        if (mscc == null) {
            return new HashSet<>();
        }
        return mscc.getCreatedCnecIds().stream().map(cnecId -> cracCreationContext.getCrac().getFlowCnec(cnecId)).collect(Collectors.toSet());
    }

}
