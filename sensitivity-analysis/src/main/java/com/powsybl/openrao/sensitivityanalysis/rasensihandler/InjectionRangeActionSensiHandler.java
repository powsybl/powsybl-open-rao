/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis.rasensihandler;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.WeightedSensitivityVariable;

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
    public double getSensitivityOnFlow(FlowCnec cnec, TwoSides side, SystematicSensitivityResult sensitivityResult) {
        return sensitivityResult.getSensitivityOnFlow(getPositiveGlskMapId(), cnec, side) * getKeySum(getPositiveGlskMap())
                - sensitivityResult.getSensitivityOnFlow(getNegativeGlskMapId(), cnec, side) * getKeySum(getNegativeGlskMap());
    }

    @Override
    public void checkConsistency(Network network) {
        injectionRangeAction.getInjectionDistributionKeys().forEach((k, v) -> {
            Identifiable<?> identifiable = network.getIdentifiable(k.getId());
            if (!(identifiable instanceof Generator || identifiable instanceof Load)) {
                throw new OpenRaoException(String.format("Unable to create sensitivity variable for InjectionRangeAction %s, on element %s", injectionRangeAction.getId(), k.getId()));
            }
        });
    }

    public Map<String, Float> getPositiveGlskMap() {
        return injectionRangeAction.getInjectionDistributionKeys().entrySet()
                .stream().filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(e -> e.getKey().getId(), e -> e.getValue().floatValue()));
    }

    public Map<String, Float> getNegativeGlskMap() {
        return injectionRangeAction.getInjectionDistributionKeys().entrySet()
                .stream().filter(e -> e.getValue() < 0)
                .collect(Collectors.toMap(e -> e.getKey().getId(), e -> -e.getValue().floatValue()));
    }

    public List<WeightedSensitivityVariable> rescaleGlskMap(Map<String, Float> glskMap) {
        float keySum = (float) getKeySum(glskMap);
        if (keySum != 0) {
            glskMap.entrySet().forEach(e -> e.setValue(e.getValue() / keySum));
        }
        return glskMap.entrySet().stream().map(e -> new WeightedSensitivityVariable(e.getKey(), e.getValue())).toList();
    }

    private double getKeySum(Map<String, Float> glskMap) {
        return glskMap.values().stream()
                .mapToDouble(v -> v)
                .sum();
    }

    public String getPositiveGlskMapId() {
        return injectionRangeAction.getId() + POSITIVE_GLSK_SUFFIX;
    }

    public String getNegativeGlskMapId() {
        return injectionRangeAction.getId() + NEGATIVE_GLSK_SUFFIX;
    }
}
