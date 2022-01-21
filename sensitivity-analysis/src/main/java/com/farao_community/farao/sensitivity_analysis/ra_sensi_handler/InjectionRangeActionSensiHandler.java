/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis.ra_sensi_handler;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariable;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionRangeActionSensiHandler implements RangeActionSensiHandler {

    private static final String POSITIVE_GLSK_SUFFIX = "-positiveInjections";
    private static final String NEGATIVE_GLSK_SUFFIX = "-negativeInjections";
    private final InjectionRangeAction injectionRangeAction;

    public InjectionRangeActionSensiHandler(InjectionRangeAction injectionRangeAction) {
        this.injectionRangeAction = injectionRangeAction;
    }

    @Override
    public List<SensitivityVariable> rangeActionToSensitivityVariable() {
        List<SensitivityVariable> sensitivityVariables = new ArrayList<>();

        /*
         separate injection distribution keys of the RangeAction into two GlskMap:
         - one with the positive injection variations
         - one with the negative injection variations

         Rescale the GlskMaps so that their sum is equal to 1
         */

        Map<String, Float> positiveGlskMap = getPositiveGlskMap();
        Map<String, Float> negativeGlskMap = getNegativeGlskMap();

        if (!positiveGlskMap.isEmpty()) {
            rescaleGlskMap(positiveGlskMap);
            sensitivityVariables.add(new LinearGlsk(getPositiveGlskMapId(), getPositiveGlskMapId(), positiveGlskMap));
        }

        if (!negativeGlskMap.isEmpty()) {
            rescaleGlskMap(negativeGlskMap);
            sensitivityVariables.add(new LinearGlsk(getNegativeGlskMapId(), getNegativeGlskMapId(), negativeGlskMap));
        }

        return sensitivityVariables;
    }

    @Override
    public double getSensitivityOnFlow(FlowCnec cnec, SystematicSensitivityResult sensitivityResult) {
        return sensitivityResult.getSensitivityOnFlow(getPositiveGlskMapId(), cnec) * getKeySum(getPositiveGlskMap())
                - sensitivityResult.getSensitivityOnFlow(getNegativeGlskMapId(), cnec) * getKeySum(getNegativeGlskMap());
    }

    @Override
    public void checkConsistency(Network network) {
        injectionRangeAction.getInjectionDistributionKeys().forEach((k, v) -> {
            Identifiable<?> identifiable = network.getIdentifiable(k.getId());
            if (!(identifiable instanceof Generator || identifiable instanceof Load)) {
                throw new SensitivityAnalysisException(String.format("Unable to create sensitivity variable for InjectionRangeAction %s, on element %s", injectionRangeAction.getId(), k.getId()));
            }
        });
    }

    private Map<String, Float> getPositiveGlskMap() {
        return injectionRangeAction.getInjectionDistributionKeys().entrySet()
                .stream().filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(e -> e.getKey().getId(), e -> e.getValue().floatValue()));
    }

    private Map<String, Float> getNegativeGlskMap() {
        return injectionRangeAction.getInjectionDistributionKeys().entrySet()
                .stream().filter(e -> e.getValue() < 0)
                .collect(Collectors.toMap(e -> e.getKey().getId(), e -> -e.getValue().floatValue()));
    }

    private void rescaleGlskMap(Map<String, Float> glskMap) {
        float keySum = (float) getKeySum(glskMap);
        if (keySum != 0) {
            glskMap.entrySet().forEach(e -> e.setValue(e.getValue() / keySum));
        }
    }

    private double getKeySum(Map<String, Float> glskMap) {
        return glskMap.values().stream()
                .mapToDouble(v -> v)
                .sum();
    }

    private String getPositiveGlskMapId() {
        return injectionRangeAction.getId() + POSITIVE_GLSK_SUFFIX;
    }

    private String getNegativeGlskMapId() {
        return injectionRangeAction.getId() + NEGATIVE_GLSK_SUFFIX;
    }
}
