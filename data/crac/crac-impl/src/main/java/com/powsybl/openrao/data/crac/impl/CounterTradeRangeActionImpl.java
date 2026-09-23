/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.GeneratorActionBuilder;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public class CounterTradeRangeActionImpl extends AbstractRangeAction<CounterTradeRangeAction>
        implements CounterTradeRangeAction {

    private final String exportingArea;
    private final String importingArea;
    private final String area;
    private final List<StandardRange> ranges;
    private final Double initialSetpoint;
    private Map<String, Map<String, Double>> glsk;

    CounterTradeRangeActionImpl(String id, String name, String operator, String groupId, Set<UsageRule> usageRules,
                                List<StandardRange> ranges, Double initialSetpoint, Integer speed,
                                Double activationCost, Map<VariationDirection, Double> variationCosts,
                                String exportingArea, String importingArea) {
        super(id, name, operator, usageRules, groupId, speed, activationCost, variationCosts);
        this.ranges = ranges;
        this.initialSetpoint = initialSetpoint;
        this.exportingArea = exportingArea;
        this.importingArea = importingArea;
        this.area = null;
        this.glsk = null;
    }

    CounterTradeRangeActionImpl(String id, String name, String operator, String groupId, Set<UsageRule> usageRules,
                                List<StandardRange> ranges, Double initialSetpoint, Integer speed,
                                Double activationCost, Map<VariationDirection, Double> variationCosts,
                                String area) {
        super(id, name, operator, usageRules, groupId, speed, activationCost, variationCosts);
        this.ranges = ranges;
        this.initialSetpoint = initialSetpoint;
        this.exportingArea = "null";
        this.importingArea = "null";
        this.area = area;
        this.glsk = null;
    }

    @Override
    public List<StandardRange> getRanges() {
        return ranges;
    }

    @Override
    public double getMinAdmissibleSetpoint(double previousInstantSetPoint) {
        return StandardRangeActionUtils.getMinAdmissibleSetpoint(previousInstantSetPoint, ranges, initialSetpoint);
    }

    @Override
    public double getMaxAdmissibleSetpoint(double previousInstantSetPoint) {
        return StandardRangeActionUtils.getMaxAdmissibleSetpoint(previousInstantSetPoint, ranges, initialSetpoint);
    }

    @Override
    public Double getInitialSetpoint() {
        return initialSetpoint;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.emptySet();
        // modified instead the method: concatenateNetworkElementsIds.
    }

    @Override
    public String getExportingArea() {
        return exportingArea;
    }

    @Override
    public String getImportingArea() {
        return importingArea;
    }

    @Override
    public String getArea() {
        return area;
    }

    @Override
    public void apply(Network network, double setpoint) {
        Map<String, Double> currentArea = glsk.get(area);
        currentArea.forEach((key, value) -> applyCT(network, key, setpoint * value));
    }

    public void applyCT(Network network, String generatorId, double targetSetpoint) {
        Generator generator = network.getGenerator(generatorId);
        if (generator != null) {
            new GeneratorActionBuilder().withId("id")
                                        .withGeneratorId(generatorId)
                                        .withActivePowerRelativeValue(false)
                                        .withActivePowerValue(generator.getTargetP() + targetSetpoint)
                                        .build()
                                        .toModification()
                                        .apply(network, true, ReportNode.NO_OP);
            return;
        }
        // TODO do the same with loads (see: injection range actions)

        if (network.getIdentifiable(generatorId) == null) {
            throw new OpenRaoException(String.format("Injection %s not found in network", generatorId));
        } else {
            throw new OpenRaoException(
                    String.format("%s refers to an object of the network which is not an handled Injection (not a Load, not a Generator)",
                                  generatorId));
        }
    }

    @Override
    public double getCurrentSetpoint(Network network) {
        return 0;
        // TODO: need to change this or not??
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        if (this.exportingArea != null && this.importingArea != null) {
            return this.exportingArea.equals(((CounterTradeRangeAction) o).getExportingArea())
                    && this.importingArea.equals(((CounterTradeRangeAction) o).getImportingArea())
                    && this.ranges.equals(((CounterTradeRangeAction) o).getRanges());
        } else if (this.area != null) {
            return this.area.equals(((CounterTradeRangeAction) o).getArea())
                    && this.ranges.equals(((CounterTradeRangeAction) o).getRanges());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        for (StandardRange range : ranges) {
            hashCode += 31 * range.hashCode();
        }
        hashCode += 31 * exportingArea.hashCode() + 63 * importingArea.hashCode();
        return hashCode;
    }

    @Override
    public void setGlsk(ZonalData<SensitivityVariableSet> zonalGlsk) {
        this.glsk = new HashMap<>();
        this.glsk.put(getArea(), new HashMap<>());

        zonalGlsk.getData(getArea())
                 .getVariablesById()
                 .forEach((id, variable) -> glsk.get(getArea()).put(id, variable.getWeight()));
    }

    public Map<String, Map<String, Double>> getGlsk() {
        return glsk;
    }
}
