/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.swe;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CnecCreationContext;
import com.powsybl.openrao.data.crac.io.cim.craccreator.MeasurementCreationContext;
import com.powsybl.openrao.data.crac.io.cim.craccreator.MonitoredSeriesCreationContext;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.Analog;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.MonitoredRegisteredResource;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.MonitoredSeries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.A02_CODING_SCHEME;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.AMP_UNIT_SYMBOL;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.AUTO_MEASUREMENT_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.CURATIVE_MEASUREMENT_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.DIRECT_POSITIVE_FLOW_IN;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.FLOW_MEASUREMENT_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.OPPOSITE_POSITIVE_FLOW_IN;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.PATL_MEASUREMENT_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.TATL_MEASUREMENT_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.swe.SweCneUtil.DEFAULT_DECIMALS_FOR_ROUNDING_FLOWS;
import static com.powsybl.openrao.data.raoresult.io.cne.swe.SweCneUtil.DEFAULT_DECIMALS_FOR_ROUNDING_THRESHOLDS;

/**
 * Generates MonitoredSeries for SWE CNE format
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SweMonitoredSeriesCreator {
    private final SweCneHelper sweCneHelper;
    private final CimCracCreationContext cracCreationContext;
    private Map<Contingency, Map<MonitoredSeriesCreationContext, Set<CnecCreationContext>>> cnecCreationContextsMap;

    public SweMonitoredSeriesCreator(SweCneHelper sweCneHelper, CimCracCreationContext cracCreationContext) {
        this.sweCneHelper = sweCneHelper;
        this.cracCreationContext = cracCreationContext;
        prepareMap();
    }

    private void prepareMap() {
        cnecCreationContextsMap = new TreeMap<>((c1, c2) -> {
            if (Objects.isNull(c1) && Objects.isNull(c2)) {
                return 0;
            } else if (Objects.isNull(c1)) {
                return -1;
            } else if (Objects.isNull(c2)) {
                return 1;
            } else {
                return c1.getId().compareTo(c2.getId());
            }
        });
        Crac crac = sweCneHelper.getCrac();
        cracCreationContext.getMonitoredSeriesCreationContexts().values().stream()
            .filter(MonitoredSeriesCreationContext::isImported).forEach(
                monitoredSeriesCreationContext -> monitoredSeriesCreationContext.getMeasurementCreationContexts().stream()
                    .filter(MeasurementCreationContext::isImported).forEach(
                        measurementCreationContext -> measurementCreationContext.getCnecCreationContexts().values().stream()
                            .filter(CnecCreationContext::isImported).forEach(
                                cnecCreationContext -> {
                                    FlowCnec cnec = crac.getFlowCnec(cnecCreationContext.getCreatedCnecId());
                                    Contingency contingency = cnec.getState().getContingency().orElse(null);
                                    cnecCreationContextsMap.computeIfAbsent(contingency, c ->
                                        new TreeMap<>(Comparator.comparing(MonitoredSeriesCreationContext::getNativeId)));
                                    cnecCreationContextsMap.get(contingency).computeIfAbsent(monitoredSeriesCreationContext, cc ->
                                        new TreeSet<>(Comparator.comparing(CnecCreationContext::getCreatedCnecId)));
                                    cnecCreationContextsMap.get(contingency).get(monitoredSeriesCreationContext).add(cnecCreationContext);
                                })));
    }

    public List<MonitoredSeries> generateMonitoredSeries(Contingency contingency) {
        if (!cnecCreationContextsMap.containsKey(contingency)) {
            return Collections.emptyList();
        }
        List<MonitoredSeries> monitoredSeriesList = new ArrayList<>();
        boolean includeMeasurements = !sweCneHelper.isContingencyDivergent(contingency);
        cnecCreationContextsMap.get(contingency).forEach((monitoredSeriesCC, cnecCCSet) ->
            monitoredSeriesList.addAll(generateMonitoredSeries(monitoredSeriesCC, cnecCCSet, includeMeasurements))
        );
        return monitoredSeriesList;
    }

    private Pair<Double, Double> getCnecLimitingFlowAndThreshold(Instant optimizedInstant, FlowCnec cnec) {
        double flow = 0.0;
        double threshold = 0.0;
        double margin = Double.POSITIVE_INFINITY;
        for (TwoSides side : cnec.getMonitoredSides()) {
            double flowOnSide = sweCneHelper.getRaoResult().getFlow(optimizedInstant, cnec, side, Unit.AMPERE);
            if (Double.isNaN(flowOnSide)) {
                continue;
            }
            double marginOnLowerBound = flowOnSide - cnec.getLowerBound(side, Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY);
            if (marginOnLowerBound < margin) {
                margin = marginOnLowerBound;
                flow = flowOnSide;
                threshold = cnec.getLowerBound(side, Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY);
            }
            double marginOnUpperBound = cnec.getUpperBound(side, Unit.AMPERE).orElse(Double.POSITIVE_INFINITY) - flowOnSide;
            if (marginOnUpperBound < margin) {
                margin = marginOnUpperBound;
                flow = flowOnSide;
                threshold = cnec.getUpperBound(side, Unit.AMPERE).orElse(Double.POSITIVE_INFINITY);
            }
        }
        return Pair.of(
            roundValueBasedOnMargin(flow, margin, DEFAULT_DECIMALS_FOR_ROUNDING_FLOWS).doubleValue(),
            roundValueBasedOnMargin(threshold, margin, DEFAULT_DECIMALS_FOR_ROUNDING_THRESHOLDS).doubleValue()
        );
    }

    private List<MonitoredSeries> generateMonitoredSeries(MonitoredSeriesCreationContext monitoredSeriesCreationContext,
                                                          Set<CnecCreationContext> cnecCreationContexts,
                                                          boolean includeMeasurements) {
        Crac crac = sweCneHelper.getCrac();
        Map<Double, MonitoredSeries> monitoredSeriesPerFlowValue = new LinkedHashMap<>();
        cnecCreationContexts.forEach(cnecCreationContext -> {
            FlowCnec cnec = crac.getFlowCnec(cnecCreationContext.getCreatedCnecId());
            Pair<Double, Double> flowAndThreshold = getCnecLimitingFlowAndThreshold(cnec.getState().getInstant(), cnec);
            double flowValue = flowAndThreshold.getLeft();
            if (monitoredSeriesPerFlowValue.containsKey(flowValue) && includeMeasurements) {
                mergeSeries(monitoredSeriesPerFlowValue.get(flowValue), cnec, flowAndThreshold.getRight());
            } else if (!monitoredSeriesPerFlowValue.containsKey(flowValue)) {
                monitoredSeriesPerFlowValue.put(flowValue, generateMonitoredSeriesFromScratch(monitoredSeriesCreationContext, cnec, includeMeasurements));
            }
        });
        return new ArrayList<>(monitoredSeriesPerFlowValue.values());
    }

    private void mergeSeries(MonitoredSeries monitoredSeries, FlowCnec cnec, double thresholdValue) {
        Analog threshold = new Analog();
        threshold.setMeasurementType(getThresholdMeasurementType(cnec));
        threshold.setUnitSymbol(AMP_UNIT_SYMBOL);
        threshold.setPositiveFlowIn(thresholdValue >= 0 ? DIRECT_POSITIVE_FLOW_IN : OPPOSITE_POSITIVE_FLOW_IN);
        threshold.setAnalogValuesValue((float) Math.abs(thresholdValue));

        monitoredSeries.getRegisteredResource().get(0).getMeasurements().add(threshold);
    }

    private MonitoredSeries generateMonitoredSeriesFromScratch(MonitoredSeriesCreationContext monitoredSeriesCreationContext, FlowCnec cnec, boolean includeMeasurements) {
        MonitoredSeries monitoredSeries = new MonitoredSeries();
        monitoredSeries.setMRID(monitoredSeriesCreationContext.getNativeId());
        monitoredSeries.setName(monitoredSeriesCreationContext.getNativeName());
        MonitoredRegisteredResource registeredResource = new MonitoredRegisteredResource();
        registeredResource.setMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, monitoredSeriesCreationContext.getNativeResourceId()));
        registeredResource.setName(monitoredSeriesCreationContext.getNativeResourceName());
        setInOutAggregateNodes(cnec.getNetworkElement().getId(), monitoredSeriesCreationContext.getNativeId(), registeredResource);

        if (includeMeasurements) {
            Pair<Double, Double> flowAndThreshold = getCnecLimitingFlowAndThreshold(cnec.getState().getInstant(), cnec);

            Analog flow = new Analog();
            flow.setMeasurementType(FLOW_MEASUREMENT_TYPE);
            flow.setUnitSymbol(AMP_UNIT_SYMBOL);
            double flowValue = flowAndThreshold.getLeft();
            flow.setPositiveFlowIn(flowValue >= 0 ? DIRECT_POSITIVE_FLOW_IN : OPPOSITE_POSITIVE_FLOW_IN);
            flow.setAnalogValuesValue((float) Math.abs(flowValue));
            registeredResource.getMeasurements().add(flow);

            Analog threshold = new Analog();
            threshold.setMeasurementType(getThresholdMeasurementType(cnec));
            threshold.setUnitSymbol(AMP_UNIT_SYMBOL);
            double thresholdValue = flowAndThreshold.getRight();
            threshold.setPositiveFlowIn(thresholdValue >= 0 ? DIRECT_POSITIVE_FLOW_IN : OPPOSITE_POSITIVE_FLOW_IN);
            threshold.setAnalogValuesValue((float) Math.abs(thresholdValue));
            registeredResource.getMeasurements().add(threshold);
        }

        monitoredSeries.getRegisteredResource().add(registeredResource);
        return monitoredSeries;
    }

    void setInOutAggregateNodes(String networkElementId, String monitoredSeriesId, MonitoredRegisteredResource registeredResource) {
        Branch<?> branch = cracCreationContext.getNetworkBranches().get(networkElementId);
        if (branch instanceof TieLine tieLine && tieLine.getDanglingLine1().hasProperty("CGMES.TopologicalNode_Boundary")) {
            Country cnecOperatorCountry = SweCneUtil.getOperatorCountry(monitoredSeriesId.substring(0, 3));
            String xNodeMRId = tieLine.getDanglingLine1().getProperty("CGMES.TopologicalNode_Boundary");
            if (SweCneUtil.getBranchCountry(branch, TwoSides.ONE).equals(cnecOperatorCountry)) {
                registeredResource.setInAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, branch.getTerminal1().getVoltageLevel().getId()));
                registeredResource.setOutAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, xNodeMRId));
            } else {
                registeredResource.setInAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, xNodeMRId));
                registeredResource.setOutAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, branch.getTerminal2().getVoltageLevel().getId()));
            }
        } else {
            registeredResource.setInAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, branch.getTerminal1().getVoltageLevel().getId()));
            registeredResource.setOutAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, branch.getTerminal2().getVoltageLevel().getId()));
        }
    }

    private String getThresholdMeasurementType(FlowCnec cnec) {
        switch (cnec.getState().getInstant().getKind()) {
            case PREVENTIVE:
                return PATL_MEASUREMENT_TYPE;
            case OUTAGE:
                return TATL_MEASUREMENT_TYPE;
            case AUTO:
                return AUTO_MEASUREMENT_TYPE;
            case CURATIVE:
                return CURATIVE_MEASUREMENT_TYPE;
            default:
                throw new OpenRaoException(String.format("Unexpected instant: %s", cnec.getState().getInstant().toString()));
        }
    }
}
