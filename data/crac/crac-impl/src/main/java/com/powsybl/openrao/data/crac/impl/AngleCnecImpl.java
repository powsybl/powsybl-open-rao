/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.threshold.Threshold;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AngleCnecImpl extends AbstractCnec<AngleCnec> implements AngleCnec {
    private final Set<Threshold> thresholds;
    private final NetworkElement exportingNetworkElement;
    private final NetworkElement importingNetworkElement;

    AngleCnecImpl(String id,
                 String name,
                 NetworkElement exportingNetworkElement,
                 NetworkElement importingNetworkElement,
                 String operator,
                 String border,
                 State state,
                 boolean optimized,
                 boolean monitored,
                  Set<Threshold> thresholds,
                 double reliabilityMargin) {
        super(id, name, Set.of(exportingNetworkElement, importingNetworkElement), operator, border, state, optimized, monitored, reliabilityMargin);
        this.thresholds = thresholds;
        this.exportingNetworkElement = exportingNetworkElement;
        this.importingNetworkElement = importingNetworkElement;
    }

    @Override
    public NetworkElement getExportingNetworkElement() {
        return exportingNetworkElement;
    }

    @Override
    public NetworkElement getImportingNetworkElement() {
        return importingNetworkElement;
    }

    @Override
    public Set<Threshold> getThresholds() {
        return thresholds;
    }

    @Override
    public Optional<Double> getLowerBound(Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(PhysicalParameter.ANGLE);
        Set<Threshold> limitingThresholds = thresholds.stream()
            .filter(Threshold::limitsByMin)
            .collect(Collectors.toSet());

        if (!limitingThresholds.isEmpty()) {
            double lowerBound = Double.NEGATIVE_INFINITY;
            for (Threshold threshold : limitingThresholds) {
                double currentBound = threshold.min().orElseThrow() + reliabilityMargin;
                if (currentBound > lowerBound) {
                    lowerBound = currentBound;
                }
            }
            return Optional.of(lowerBound);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Double> getUpperBound(Unit requestedUnit) {
        requestedUnit.checkPhysicalParameter(PhysicalParameter.ANGLE);
        Set<Threshold> limitingThresholds = thresholds.stream()
            .filter(Threshold::limitsByMax)
            .collect(Collectors.toSet());
        if (!limitingThresholds.isEmpty()) {
            double upperBound = Double.POSITIVE_INFINITY;
            for (Threshold threshold : limitingThresholds) {
                double currentBound = threshold.max().orElseThrow() - reliabilityMargin;
                if (currentBound < upperBound) {
                    upperBound = currentBound;
                }
            }
            return Optional.of(upperBound);
        }
        return Optional.empty();
    }

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.ANGLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AngleCnecImpl cnec = (AngleCnecImpl) o;
        return super.equals(cnec)
            && thresholds.equals(cnec.getThresholds())
            && exportingNetworkElement.equals(cnec.getExportingNetworkElement())
            && importingNetworkElement.equals(cnec.getImportingNetworkElement());
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode = 31 * hashCode + thresholds.hashCode();
        hashCode = 31 * hashCode + exportingNetworkElement.hashCode();
        hashCode = 31 * hashCode + importingNetworkElement.hashCode();
        return hashCode;
    }
}
