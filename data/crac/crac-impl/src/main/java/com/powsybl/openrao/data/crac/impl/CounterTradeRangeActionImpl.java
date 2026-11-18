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
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.CounterTradeGeneratorOrdering;
import com.powsybl.openrao.commons.GeneratorOrderingComputer;
import com.powsybl.openrao.commons.GlskGeneratorAllocation;
import com.powsybl.openrao.commons.MeritOrderGlsk;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.ConnectedArea;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public class CounterTradeRangeActionImpl extends AbstractRangeAction<CounterTradeRangeAction> implements CounterTradeRangeAction {

    private final List<StandardRange> ranges;
    private final String area;
    private final List<ConnectedArea> connectedAreas;
    private final Double initialSetpoint;
    private Map<String, Map<String, Double>> glsk;
    private CounterTradeGeneratorOrdering generatorOrdering;

    CounterTradeRangeActionImpl(String id,
                                String name,
                                String operator,
                                String groupId,
                                Set<UsageRule> usageRules,
                                List<StandardRange> ranges,
                                Double initialSetpoint,
                                Integer speed,
                                Double activationCost,
                                Map<VariationDirection, Double> variationCosts,
                                String area,
                                List<ConnectedArea> connectedAreas) {
        super(id, name, operator, usageRules, groupId, speed, activationCost, variationCosts);
        this.ranges = ranges;
        this.initialSetpoint = initialSetpoint;
        this.area = area;
        this.connectedAreas = connectedAreas;
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
    public Set<String> getNetworkElementsIds() {
        return Collections.emptySet();
        // modified instead the method: concatenateNetworkElementsIds.
    }

    @Override
    public String getAreaId() {
        return area;
    }

    @Override
    public List<ConnectedArea> getConnectedAreas() {
        return connectedAreas;
    }

    @Override
    public void apply(Network network, double setpoint) {
        boolean upward = setpoint >= 0;
        if (generatorOrdering == null) {
            throw new OpenRaoException(String.format("Can't apply a counter trade range action on area %s because no ordering has been provided", getAreaId()));
        }
        List<GlskGeneratorAllocation> order = upward ? generatorOrdering.upwardOrder() : generatorOrdering.downwardOrder();

        double[] x = GeneratorOrderingComputer.layerAllocation(order, Math.abs(setpoint));
        Map<String, Double> dispatch;
        if (generatorOrdering.meritOrder()) {
            dispatch = GeneratorOrderingComputer.meritOrderDispatchQuantity(order, x);
        } else {
            dispatch = GeneratorOrderingComputer.computeDispatchQuantity(order, x);
        }

        double sign = upward ? 1.0 : -1.0;
        generatorOrdering.initialTargetP().forEach((generatorId, p0) -> {
            double targetP = p0 + sign * dispatch.getOrDefault(generatorId, 0.0);
            applyCT(network, generatorId, targetP);
        });
    }

    private void applyCT(Network network, String generatorId, double targetSetpoint) {
        Generator generator = network.getGenerator(generatorId);
        if (generator != null) {
            new GeneratorActionBuilder().withId("id")
                                        .withGeneratorId(generatorId)
                                        .withActivePowerRelativeValue(false)
                                        .withActivePowerValue(targetSetpoint)
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
        // TODO: need to change this after application.
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

        return this.ranges.equals(((CounterTradeRangeAction) o).getRanges())
                && this.area.equals(((CounterTradeRangeAction) o).getAreaId())
                && this.connectedAreas.equals(((CounterTradeRangeAction) o).getConnectedAreas());
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        for (StandardRange range : ranges) {
            hashCode += 31 * range.hashCode();
        }
        for (ConnectedArea connectedArea : connectedAreas) {
            hashCode += 31 * connectedArea.hashCode();
        }
        hashCode += 31 * area.hashCode();
        return hashCode;
    }

    @Override
    public void setGlsk(ZonalData<SensitivityVariableSet> zonalGlsk) {
        this.glsk = new HashMap<>();
        this.glsk.put(area, new HashMap<>());

        SensitivityVariableSet areaGlsk = zonalGlsk.getData(area);
        if (areaGlsk == null) {
            throw new OpenRaoException(String.format("No GLSK found for area %s of counter-trading action %s", area, getId()));
        }
        areaGlsk.getVariablesById()
                 .forEach((id, variable) -> glsk.get(area).put(id, variable.getWeight()));
    }

    @Override
    public void setGlsk(Network network) {
        this.glsk = new HashMap<>();
        this.glsk.put(area, computeProportionalShares(network));
    }

    private static double pseudoTargetP(Generator generator) {
        return Math.max(1e-5, Math.abs(generator.getTargetP()));
    }

    private Map<String, Double> computeProportionalShares(Network network) {
        Objects.requireNonNull(network);
        Country country = toCountry(area);

        List<Generator> generators = network.getGeneratorStream()
                .filter(g -> g.getTerminal().getVoltageLevel().getSubstation()
                        .flatMap(Substation::getCountry)
                        .filter(country::equals)
                        .isPresent())
                .filter(g -> g.getTerminal().isConnected()
                        && g.getTerminal().getBusView().getBus() != null
                        && g.getTerminal().getBusView().getBus().isInMainSynchronousComponent())
                .toList();
        double totalP = generators.stream().mapToDouble(CounterTradeRangeActionImpl::pseudoTargetP).sum();

        Map<String, Double> shares = new LinkedHashMap<>();
        generators.forEach(g -> shares.put(g.getId(), pseudoTargetP(g) / totalP));
        return shares;
    }

    private static Country toCountry(String areaId) {
        try {
            return Country.valueOf(areaId);
        } catch (IllegalArgumentException e) {
            throw new OpenRaoException("Area " + areaId + " is not a country code");
        }
    }

    public Map<String, Map<String, Double>> getGlsk() {
        return glsk;
    }

    public void computeGeneratorOrdering(Network network, double tolerance) {
        Map<String, Double> weights = getGlsk().get(area);
        this.generatorOrdering = GeneratorOrderingComputer.computeFromProportional(
                weights,
                network,
                tolerance,
                getMinAdmissibleSetpoint(0),
                getMaxAdmissibleSetpoint(0)
        );
    }

    @Override
    public void computeGeneratorOrdering(MeritOrderGlsk meritOrderGlsk, Network network) {
        if (getMaxAdmissibleSetpoint(0) > 0 && meritOrderGlsk.upward().isEmpty()) {
            BUSINESS_WARNS.warn("Counter trade range action {} allows an increase but its merit order has no upward tranche", getId());
        }
        if (getMinAdmissibleSetpoint(0) < 0 && meritOrderGlsk.downward().isEmpty()) {
            BUSINESS_WARNS.warn("Counter trade range action {} allows a decrease but its merit order has no downward tranche", getId());
        }
        this.generatorOrdering = GeneratorOrderingComputer.computeFromMeritOrder(
                meritOrderGlsk,
                network,
                getMinAdmissibleSetpoint(0),
                getMaxAdmissibleSetpoint(0)
        );
    }

    public CounterTradeGeneratorOrdering getGeneratorOrdering() {
        return generatorOrdering;
    }
}
