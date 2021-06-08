package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.contingency.*;
import com.powsybl.iidm.network.*;

import java.util.List;
import java.util.stream.Collectors;

final class SensitivityAnalysisUtil {

    private SensitivityAnalysisUtil() { }

    static Contingency convertCracContingencyToPowsybl(com.farao_community.farao.data.crac_api.Contingency cracContingency, Network network) {
        String id = cracContingency.getId();
        List<ContingencyElement> contingencyElements = cracContingency.getNetworkElements().stream()
            .map(element -> convertCracContingencyElementToPowsybl(element, network))
            .collect(Collectors.toList());
        return new Contingency(id, contingencyElements);
    }

    private static ContingencyElement convertCracContingencyElementToPowsybl(NetworkElement cracContingencyElement, Network network) {
        String elementId = cracContingencyElement.getId();
        Identifiable<?> networkIdentifiable = network.getIdentifiable(elementId);
        if (networkIdentifiable instanceof Branch) {
            return new BranchContingency(elementId);
        } else if (networkIdentifiable instanceof Generator) {
            return new GeneratorContingency(elementId);
        } else if (networkIdentifiable instanceof HvdcLine) {
            return new HvdcLineContingency(elementId);
        } else if (networkIdentifiable instanceof BusbarSection) {
            return new BusbarSectionContingency(elementId);
        } else if (networkIdentifiable instanceof DanglingLine) {
            return new DanglingLineContingency(elementId);
        } else {
            throw new SensitivityAnalysisException("Unable to apply contingency element " + elementId);
        }
    }
}
