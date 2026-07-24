/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.sensitivityanalysis.rasensihandler.CounterTradeRangeActionSensiHandler;
import com.powsybl.openrao.sensitivityanalysis.rasensihandler.InjectionRangeActionSensiHandler;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.SensitivityVariableType;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class RangeActionSensitivityProvider extends LoadflowProvider {
    private final Set<RangeAction<?>> rangeActions;
    private final Map<String, SensitivityVariableSet> glsks;

    RangeActionSensitivityProvider(Set<RangeAction<?>> rangeActions, Set<FlowCnec> cnecs, Set<Unit> units) {
        super(cnecs, units);
        this.rangeActions = rangeActions;
        glsks = new HashMap<>();
    }

    @Override
    public List<SensitivityFactor> getBasecaseFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();

        if (afterContingencyOnly) {
            return factors;
        }

        Map<String, SensitivityVariableType> sensitivityVariables = new HashMap<>();
        Set<String> glskIds = new HashSet<>();
        fillSensitivityVariablesAndGlskIds(network, sensitivityVariables, glskIds);

        List<Pair<String, SensitivityFunctionType>> sensitivityFunctions = getSensitivityFunctions(network, null);

        //According to ContingencyContext doc, contingencyId should be null for preContingency context
        ContingencyContext preContingencyContext = new ContingencyContext(null, ContingencyContextType.NONE);
        sensitivityFunctions.forEach(function ->
            sensitivityVariables.forEach((key, value) -> factors.add(new SensitivityFactor(function.getValue(), function.getKey(), value, key,
                glskIds.contains(key), preContingencyContext)))
        );
        return factors;
    }

    @Override
    public List<SensitivityFactor> getContingencyFactors(Network network, List<Contingency> contingencies) {
        List<SensitivityFactor> factors = new ArrayList<>();
        for (Contingency contingency : contingencies) {
            String contingencyId = contingency.getId();
            Map<String, SensitivityVariableType> sensitivityVariables = new HashMap<>();
            Set<String> glskIds = new HashSet<>();
            fillSensitivityVariablesAndGlskIds(network, sensitivityVariables, glskIds);

            List<Pair<String, SensitivityFunctionType>> sensitivityFunctions = getSensitivityFunctions(network, contingencyId);

            //According to ContingencyContext doc, contingencyId should be null for preContingency context
            ContingencyContext contingencyContext = new ContingencyContext(contingencyId, ContingencyContextType.SPECIFIC);
            sensitivityFunctions.forEach(function ->
                sensitivityVariables.forEach((key, value) -> factors.add(new SensitivityFactor(function.getValue(), function.getKey(), value, key,
                    glskIds.contains(key), contingencyContext)))
            );
        }
        return factors;
    }

    private void fillSensitivityVariablesAndGlskIds(Network network, Map<String, SensitivityVariableType> sensitivityVariables, Set<String> glskIds) {
        for (RangeAction<?> ra : rangeActions) {
            if (ra instanceof PstRangeAction pstRangeAction) {
                sensitivityVariables.put(pstRangeAction.getNetworkElement().getId(), SensitivityVariableType.TRANSFORMER_PHASE);
            } else if (ra instanceof HvdcRangeAction hvdcRangeAction) {
                HvdcLine hvdcLine = network.getHvdcLine(hvdcRangeAction.getNetworkElement().getId());
                if (hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class) != null && hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled()) {
                    continue;
                }
                sensitivityVariables.put(hvdcRangeAction.getNetworkElement().getId(), SensitivityVariableType.HVDC_LINE_ACTIVE_POWER);
            } else if (ra instanceof InjectionRangeAction injectionRangeAction) {
                createPositiveAndNegativeGlsks(injectionRangeAction, sensitivityVariables, glskIds);
            } else if (ra instanceof CounterTradeRangeAction counterTradeRangeAction) {
                createCounterTradeGlsks(network, counterTradeRangeAction, sensitivityVariables, glskIds);
            } else {
                throw new OpenRaoException(String.format("Range action type of %s not implemented yet", ra.getId()));
            }
        }

        // Case no RangeAction is provided, we still want to get reference flows
        if (sensitivityVariables.isEmpty()) {
            addDefaultSensitivityVariable(network, sensitivityVariables);
        }
    }

    public Map<String, Map<String, Double>> getGlsk(Network network) {
        Map<String, Map<String, Double>> allGlsks =
                network.getAreaStream().collect(Collectors.toMap(Identifiable::getId, area -> new HashMap<>()));

        network.getGeneratorStream()
               .filter(g -> g.getTerminal().isConnected())
               .forEach(generator -> generator.getTerminal()
                                              .getVoltageLevel()
                                              .getAreasStream()
                                              .forEach(area -> allGlsks.get(area.getId())
                                                                       .put(generator.getId(),
                                                                            generator.getTargetP())));

        allGlsks.forEach((area, glsk) -> {
            double glskSum = glsk.values().stream().mapToDouble(factor -> factor).sum();
            if (glskSum == 0.0) {
                glsk.forEach((key, value) -> glsk.put(key, 1.0 / glsk.size()));
            } else {
                glsk.forEach((key, value) -> glsk.put(key, value / glskSum));
            }
        });
        return allGlsks;
    }

    private Map<String, Float> getPositiveGlskMap(Map<String, Double> glsk) {
        return glsk.entrySet()
                .stream().filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().floatValue()));
    }

    private Map<String, Float> getNegativeGlskMap(Map<String, Double> glsk) {
        return glsk.entrySet()
                .stream().filter(e -> e.getValue() < 0)
                .collect(Collectors.toMap(Entry::getKey, e -> -e.getValue().floatValue()));
    }

    private void createCounterTradeGlsks(Network network, CounterTradeRangeAction counterTradeRangeAction,
                                         Map<String, SensitivityVariableType> sensitivityVariables,
                                         Set<String> glskIds) {
        CounterTradeRangeActionSensiHandler handler = new CounterTradeRangeActionSensiHandler(counterTradeRangeAction);
        var glsk = getGlsk(network);
        var exporting = glsk.get(counterTradeRangeAction.getExportingArea());
        var importing = glsk.get(counterTradeRangeAction.getImportingArea());

        var positiveGlskMap = getPositiveGlskMap(exporting);
        positiveGlskMap.putAll(getNegativeGlskMap(importing));

        if (!positiveGlskMap.isEmpty()) {
            List<WeightedSensitivityVariable> positiveGlsk = positiveGlskMap.entrySet().stream().map(e -> new WeightedSensitivityVariable(e.getKey(), e.getValue())).toList();
            String positiveGlskMapId = handler.getPositiveGlskMapId();
            sensitivityVariables.put(positiveGlskMapId, SensitivityVariableType.INJECTION_ACTIVE_POWER);
            glsks.putIfAbsent(positiveGlskMapId, new SensitivityVariableSet(positiveGlskMapId, positiveGlsk));
            glskIds.add(positiveGlskMapId);

        }

        var negativeGlskMap = getNegativeGlskMap(exporting);
        negativeGlskMap.putAll(getPositiveGlskMap(importing));
        if (!negativeGlskMap.isEmpty()) {
            List<WeightedSensitivityVariable> negativeGlsk = negativeGlskMap.entrySet().stream().map(e -> new WeightedSensitivityVariable(e.getKey(), e.getValue())).toList();
            String negativeGlskMapId = handler.getNegativeGlskMapId();
            sensitivityVariables.put(negativeGlskMapId, SensitivityVariableType.INJECTION_ACTIVE_POWER);
            glsks.putIfAbsent(negativeGlskMapId, new SensitivityVariableSet(negativeGlskMapId, negativeGlsk));
            glskIds.add(negativeGlskMapId);

        }
    }

    private void createPositiveAndNegativeGlsks(InjectionRangeAction rangeAction, Map<String, SensitivityVariableType> sensitivityVariables, Set<String> glskIds) {
        InjectionRangeActionSensiHandler injectionRangeActionSensiHandler = new InjectionRangeActionSensiHandler(rangeAction);
        Map<String, Float> positiveGlskMap = injectionRangeActionSensiHandler.getPositiveGlskMap();
        Map<String, Float> negativeGlskMap = injectionRangeActionSensiHandler.getNegativeGlskMap();

        if (!positiveGlskMap.isEmpty()) {
            List<WeightedSensitivityVariable> positiveGlsk = injectionRangeActionSensiHandler.rescaleGlskMap(positiveGlskMap);
            sensitivityVariables.put(injectionRangeActionSensiHandler.getPositiveGlskMapId(), SensitivityVariableType.INJECTION_ACTIVE_POWER);
            glsks.putIfAbsent(injectionRangeActionSensiHandler.getPositiveGlskMapId(), new SensitivityVariableSet(injectionRangeActionSensiHandler.getPositiveGlskMapId(), positiveGlsk));
            glskIds.add(injectionRangeActionSensiHandler.getPositiveGlskMapId());

        }

        if (!negativeGlskMap.isEmpty()) {
            List<WeightedSensitivityVariable> negativeGlsk = injectionRangeActionSensiHandler.rescaleGlskMap(negativeGlskMap);
            sensitivityVariables.put(injectionRangeActionSensiHandler.getNegativeGlskMapId(), SensitivityVariableType.INJECTION_ACTIVE_POWER);
            glsks.putIfAbsent(injectionRangeActionSensiHandler.getNegativeGlskMapId(), new SensitivityVariableSet(injectionRangeActionSensiHandler.getNegativeGlskMapId(), negativeGlsk));
            glskIds.add(injectionRangeActionSensiHandler.getNegativeGlskMapId());
        }
    }

    @Override
    public List<SensitivityVariableSet> getVariableSets() {
        return new ArrayList<>(glsks.values());
    }

    @Override
    public Map<String, HvdcRangeAction> getHvdcs() {
        return rangeActions.stream()
            .filter(HvdcRangeAction.class::isInstance)
            .collect(Collectors.toMap(
                rangeAction -> ((HvdcRangeAction) rangeAction).getNetworkElement().getId(),
                HvdcRangeAction.class::cast,
                (existing, replacement) -> existing));
    }
}
