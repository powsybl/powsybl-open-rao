/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.CnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MeasurementCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreationContext;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.swe_cne_exporter.xsd.*;
import com.powsybl.iidm.network.Branch;

import java.util.*;

import static com.farao_community.farao.data.cne_exporter_commons.CneConstants.*;

/**
 * Generates MonitoredSeries for SWE CNE format
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SweMonitoredSeriesCreator {
    private final CneHelper cneHelper;
    private final CimCracCreationContext cracCreationContext;
    private Map<Contingency, Map<MonitoredSeriesCreationContext, Set<CnecCreationContext>>> cnecCreationContextsMap;

    public SweMonitoredSeriesCreator(CneHelper cneHelper, CimCracCreationContext cracCreationContext) {
        this.cneHelper = cneHelper;
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
        Crac crac = cneHelper.getCrac();
        cracCreationContext.getMonitoredSeriesCreationContexts().values().stream()
            .filter(MonitoredSeriesCreationContext::isImported).forEach(
                monitoredSeriesCreationContext -> monitoredSeriesCreationContext.getMeasurementCreationContexts().stream()
                    .filter(MeasurementCreationContext::isImported).forEach(
                        measurementCreationContext -> measurementCreationContext.getCnecCreationContexts().values().stream()
                            .filter(CnecCreationContext::isImported).forEach(
                                cnecCreationContext -> {
                                    FlowCnec cnec = crac.getFlowCnec(cnecCreationContext.getCreatedCnecId());
                                    Contingency contingency = cnec.getState().getContingency().orElse(null);
                                    cnecCreationContextsMap.computeIfAbsent(contingency, c -> new TreeMap<>(Comparator.comparing(MonitoredSeriesCreationContext::getNativeId)));
                                    cnecCreationContextsMap.get(contingency).computeIfAbsent(monitoredSeriesCreationContext, cc -> new TreeSet<>(Comparator.comparing(CnecCreationContext::getCreatedCnecId)));
                                    cnecCreationContextsMap.get(contingency).get(monitoredSeriesCreationContext).add(cnecCreationContext);
                                }
                            )
                    )
        );
    }

    public List<MonitoredSeries> generateMonitoredSeries(Contingency contingency) {
        List<MonitoredSeries> monitoredSeriesList = new ArrayList<>();
        cnecCreationContextsMap.get(contingency).forEach(
            (monitoredSeriesCC, cnecCCSet) -> monitoredSeriesList.addAll(generateMonitoredSeries(monitoredSeriesCC, cnecCCSet))
        );
        return monitoredSeriesList;
    }

    private List<MonitoredSeries> generateMonitoredSeries(MonitoredSeriesCreationContext monitoredSeriesCreationContext, Set<CnecCreationContext> cnecCreationContexts) {
        RaoResult raoResult = cneHelper.getRaoResult();
        Crac crac = cneHelper.getCrac();
        Map<Integer, MonitoredSeries> monitoredSeriesPerFlowValue = new LinkedHashMap<>();
        cnecCreationContexts.forEach(cnecCreationContext -> {
            FlowCnec cnec = crac.getFlowCnec(cnecCreationContext.getCreatedCnecId());
            int roundedFlow = (int) Math.round(raoResult.getFlow(OptimizationState.AFTER_CRA, cnec, Unit.AMPERE));
            if (monitoredSeriesPerFlowValue.containsKey(roundedFlow)) {
                mergeSeries(monitoredSeriesPerFlowValue.get(roundedFlow), cnec);
            } else {
                monitoredSeriesPerFlowValue.put(roundedFlow, generateMonitoredSeriesFromScratch(monitoredSeriesCreationContext, cnec));
            }
        });
        return new ArrayList<>(monitoredSeriesPerFlowValue.values());
    }

    private void mergeSeries(MonitoredSeries monitoredSeries, FlowCnec cnec) {
        Analog threshold = new Analog();
        threshold.setMeasurementType(getThresholdMeasurementType(cnec));
        threshold.setUnitSymbol(AMP_UNIT_SYMBOL);
        float roundedThreshold = Math.round(Math.min(
            Math.abs(cnec.getLowerBound(Side.LEFT, Unit.AMPERE).orElse(Double.POSITIVE_INFINITY)),
            Math.abs(cnec.getUpperBound(Side.LEFT, Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY))));
        threshold.setPositiveFlowIn(cneHelper.getRaoResult().getFlow(OptimizationState.AFTER_CRA, cnec, Unit.AMPERE) >= 0 ?
            DIRECT_POSITIVE_FLOW_IN : OPPOSITE_POSITIVE_FLOW_IN);
        threshold.setAnalogValuesValue(Math.abs(roundedThreshold));

        monitoredSeries.getRegisteredResource().get(0).getMeasurements().add(threshold);
    }

    private MonitoredSeries generateMonitoredSeriesFromScratch(MonitoredSeriesCreationContext monitoredSeriesCreationContext, FlowCnec cnec) {
        MonitoredSeries monitoredSeries = new MonitoredSeries();
        monitoredSeries.setMRID(monitoredSeriesCreationContext.getNativeId());
        monitoredSeries.setName(monitoredSeriesCreationContext.getNativeName());
        MonitoredRegisteredResource registeredResource = new MonitoredRegisteredResource();
        registeredResource.setMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, monitoredSeriesCreationContext.getNativeResourceId()));
        registeredResource.setName(monitoredSeriesCreationContext.getNativeResourceName());
        Branch<?> branch = cneHelper.getNetwork().getBranch(cnec.getNetworkElement().getId());
        registeredResource.setInAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, branch.getTerminal1().getVoltageLevel().getId()));
        registeredResource.setOutAggregateNodeMRID(SweCneUtil.createResourceIDString(A02_CODING_SCHEME, branch.getTerminal2().getVoltageLevel().getId()));

        Analog flow = new Analog();
        flow.setMeasurementType(FLOW_MEASUREMENT_TYPE);
        flow.setUnitSymbol(AMP_UNIT_SYMBOL);
        float roundedFlow = Math.round(cneHelper.getRaoResult().getFlow(OptimizationState.AFTER_CRA, cnec, Unit.AMPERE));
        flow.setPositiveFlowIn(roundedFlow >= 0 ? DIRECT_POSITIVE_FLOW_IN : OPPOSITE_POSITIVE_FLOW_IN);
        flow.setAnalogValuesValue(Math.abs(roundedFlow));
        registeredResource.getMeasurements().add(flow);

        Analog threshold = new Analog();
        threshold.setMeasurementType(getThresholdMeasurementType(cnec));
        threshold.setUnitSymbol(AMP_UNIT_SYMBOL);
        float roundedThreshold = Math.round(Math.min(
            Math.abs(cnec.getLowerBound(Side.LEFT, Unit.AMPERE).orElse(Double.POSITIVE_INFINITY)),
            Math.abs(cnec.getUpperBound(Side.LEFT, Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY))));
        threshold.setPositiveFlowIn(roundedFlow >= 0 ? DIRECT_POSITIVE_FLOW_IN : OPPOSITE_POSITIVE_FLOW_IN);
        threshold.setAnalogValuesValue(Math.abs(roundedThreshold));
        registeredResource.getMeasurements().add(threshold);

        monitoredSeries.getRegisteredResource().add(registeredResource);
        return monitoredSeries;
    }

    private String getThresholdMeasurementType(FlowCnec cnec) {
        switch (cnec.getState().getInstant()) {
            case PREVENTIVE:
                return PATL_MEASUREMENT_TYPE;
            case OUTAGE:
                return TATL_MEASUREMENT_TYPE;
            case AUTO:
                return AUTO_MEASUREMENT_TYPE;
            case CURATIVE:
                return CURATIVE_MEASUREMENT_TYPE;
            default:
                throw new FaraoException(String.format("Unexpected instant: %s", cnec.getState().getInstant().toString()));
        }
    }
}
