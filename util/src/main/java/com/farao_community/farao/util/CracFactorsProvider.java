package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.SensitivityFunction;
import com.powsybl.sensitivity.SensitivityVariable;
import com.powsybl.sensitivity.factors.BranchFlowPerInjectionIncrease;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.InjectionIncrease;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CracFactorsProvider implements SensitivityFactorsProvider {
    private final Crac crac;

    public CracFactorsProvider(Crac crac) {
        this.crac = crac;
    }

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();
        List<SensitivityVariable> sensitivityVariables = crac.getRangeActions().stream()
                .map(ra -> rangeActionToSensitivityVariables(network, ra))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Case no RangeAction is provided, we still want to get reference flows
        if (sensitivityVariables.isEmpty()) {
            sensitivityVariables.add(defaultSensitivityVariable(network));
        }

        List<SensitivityFunction> sensitivityFunctions = crac.getCnecs().stream()
                .map(cnec -> cnecToSensitivityFunction(network, cnec))
                .collect(Collectors.toList());

        sensitivityFunctions.forEach(fun -> sensitivityVariables.forEach(var -> factors.add(sensitivityFactorMapping(fun, var))));
        return factors;
    }

    private SensitivityVariable defaultSensitivityVariable(Network network) {
        Generator genKept = network.getGeneratorStream()
                .filter(this::willBeKeptInSensi)
                .findFirst()
                .orElseThrow(() -> new FaraoException(String.format("Unable to create sensitivity factors for CRAC '%s'. Did not find any generator in network '%s'.", crac.getId(), network.getId())));
        return new InjectionIncrease(genKept.getId(), genKept.getName(), genKept.getId());
    }

    private boolean willBeKeptInSensi(Generator gen) {
        return gen.getTerminal().isConnected() && gen.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
    }

    private SensitivityFunction cnecToSensitivityFunction(Network network, Cnec cnec) {
        String id = cnec.getId();
        String name = cnec.getName();
        String branchId = cnec.getNetworkElement().getId();
        Identifiable networkIdentifiable = network.getIdentifiable(branchId);
        if (networkIdentifiable instanceof Branch) {
            return new BranchFlow(id, name, branchId);
        } else {
            throw new FaraoException("Unable to create sensitivity funtion for " + id);
        }
    }

    private List<SensitivityVariable> rangeActionToSensitivityVariables(Network network, RangeAction rangeAction) {
        Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
        return networkElements.stream()
                .map(el -> networkElementToSensitivityVariable(network, el))
                .collect(Collectors.toList());
    }

    private SensitivityVariable networkElementToSensitivityVariable(Network network, NetworkElement networkElement) {
        String elementId = networkElement.getId();
        Identifiable networkIdentifiable = network.getIdentifiable(elementId);
        if (networkIdentifiable instanceof TwoWindingsTransformer) {
            return new PhaseTapChangerAngle(elementId, elementId, elementId);
        } else {
            throw new FaraoException("Unable to create sensitivity variable for " + elementId);
        }
    }

    private SensitivityFactor sensitivityFactorMapping(SensitivityFunction function, SensitivityVariable variable) {
        if (function instanceof BranchFlow) {
            if (variable instanceof PhaseTapChangerAngle) {
                return new BranchFlowPerPSTAngle((BranchFlow) function, (PhaseTapChangerAngle) variable);
            } else if (variable instanceof InjectionIncrease) {
                return new BranchFlowPerInjectionIncrease((BranchFlow) function, (InjectionIncrease) variable);
            } else {
                throw new FaraoException("Unable to create sensitivity factor for function of type " + function.getClass().getTypeName() + " and variable of type " + variable.getClass().getTypeName());
            }
        } else {
            throw new FaraoException("Unable to create sensitivity factor for function of type " + function.getClass().getTypeName() + " and variable of type " + variable.getClass().getTypeName());
        }
    }
}
