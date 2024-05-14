/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.refprog.referenceprogram;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class ReferenceProgramBuilder {
    private ReferenceProgramBuilder() {

    }

    private static void computeRefFlowOnCurrentNetwork(Network network, String loadFlowProvider, LoadFlowParameters loadFlowParameters, ReportNode reportNode) {
        String errorMsg = "LoadFlow could not be computed. The ReferenceProgram will be built without a prior LoadFlow computation";
        try {
            // we need this separate load flow to get reference flow on cnec.
            // because reference flow from sensi is not yet fully implemented in powsybl
            LoadFlowResult loadFlowResult = LoadFlow.find(loadFlowProvider).run(network, network.getVariantManager().getWorkingVariantId(), LocalComputationManager.getDefault(), loadFlowParameters, reportNode);
            if (!loadFlowResult.isOk()) {
                Reports.reportLoadflowUnsecure(reportNode);
            }
        } catch (PowsyblException e) {
            Reports.reportLoadflowException(reportNode, e.getMessage());
        }
    }

    public static ReferenceProgram buildReferenceProgram(Network network, String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        return buildReferenceProgram(network, loadFlowProvider, loadFlowParameters, ReportNode.NO_OP);
    }

    public static ReferenceProgram buildReferenceProgram(Network network, String loadFlowProvider, LoadFlowParameters loadFlowParameters, ReportNode reportNode) {
        computeRefFlowOnCurrentNetwork(network, loadFlowProvider, loadFlowParameters, reportNode);
        Map<EICode, Double> netPositions = (new CountryNetPositionComputation(network)).getNetPositions();
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();

        // warning: only the net positions are properly filled. With the use of the "null" in the
        // construction of the ReferenceExchangeData below, the zone to zone exchanges cannot be
        // retrieved from the ReferenceProgram.
        netPositions.forEach((referenceProgramArea, flow) -> referenceExchangeDataList.add(new ReferenceExchangeData(referenceProgramArea, null, flow)));
        return new ReferenceProgram(referenceExchangeDataList);
    }
}
