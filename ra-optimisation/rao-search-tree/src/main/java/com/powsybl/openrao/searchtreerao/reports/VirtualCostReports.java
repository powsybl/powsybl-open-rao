/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.Leaf;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;

import java.util.Locale;
import java.util.Objects;

import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static java.lang.String.format;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class VirtualCostReports {
    private static final double EPSILON = 1e-6;
    private static final int NUMBER_LOGGED_VIRTUAL_COSTLY_ELEMENTS = 10;

    private VirtualCostReports() {
        // Utility class should not be instantiated
    }

    /**
     * This method logs information about positive virtual costs
     */
    public static void reportVirtualCostInformation(final ReportNode parentNode,
                                                    final boolean verbose,
                                                    final Leaf leaf,
                                                    final Unit unit,
                                                    final double previousDepthOptimalLeafFunctionalCost,
                                                    final SearchTreeParameters parameters,
                                                    final boolean optimized) {
        leaf.getVirtualCostNames().stream()
            .filter(virtualCostName -> leaf.getVirtualCost(virtualCostName) > EPSILON)
            .forEach(virtualCostName -> reportVirtualCostDetails(parentNode, verbose, leaf, virtualCostName, unit, previousDepthOptimalLeafFunctionalCost, parameters, optimized));
    }

    /**
     * If stop criterion could have been reached without the given virtual cost, this method logs a message, in order
     * to inform the user that the given network action was rejected because of a virtual cost
     * (message is not logged if it has already been logged at previous depth)
     * In all cases, this method also logs most costly elements for given virtual cost
     */
    static void reportVirtualCostDetails(final ReportNode parentNode,
                                         final boolean verbose,
                                         final Leaf leaf,
                                         final String virtualCostName,
                                         final Unit unit,
                                         final double previousDepthOptimalLeafFunctionalCost,
                                         final SearchTreeParameters parameters,
                                         final boolean optimized) {
        boolean updatedVerbose = verbose;
        if (!SearchTree.costSatisfiesStopCriterion(leaf.getCost(), parameters)
            && SearchTree.costSatisfiesStopCriterion(leaf.getCost() - leaf.getVirtualCost(virtualCostName), parameters)
            && (leaf.isRoot() || !SearchTree.costSatisfiesStopCriterion(previousDepthOptimalLeafFunctionalCost, parameters))) {
            // Stop criterion would have been reached without virtual cost, for the first time at this depth
            // and for the given leaf
            final String template = optimized
                ? "openrao.searchtreerao.reportStopCriterionCouldHaveBeenReachedWithoutVirtualCostOptimized"
                : "openrao.searchtreerao.reportStopCriterionCouldHaveBeenReachedWithoutVirtualCost";
            final String prefix = optimized ? "Optimized " : "";

            parentNode.newReportNode()
                .withMessageTemplate(template)
                .withUntypedValue("leaf", leaf.getIdentifier())
                .withUntypedValue("virtualCostName", virtualCostName)
                .withSeverity(INFO_SEVERITY)
                .add();

            BUSINESS_LOGS.info("{}{}, stop criterion could have been reached without \"{}\" virtual cost", prefix, leaf.getIdentifier(), virtualCostName);
            // Promote detailed logs about costly elements to BUSINESS_LOGS
            updatedVerbose = true;
        }
        reportVirtualCostlyElements(parentNode, updatedVerbose, leaf, virtualCostName, unit, optimized);
    }

    static void reportVirtualCostlyElements(final ReportNode parentNode,
                                            final boolean verbose,
                                            final Leaf leaf,
                                            final String virtualCostName,
                                            final Unit unit,
                                            final boolean optimized) {
        int i = 1;
        for (FlowCnec flowCnec : leaf.getCostlyElements(virtualCostName, NUMBER_LOGGED_VIRTUAL_COSTLY_ELEMENTS)) {
            final TwoSides limitingSide = leaf.getMargin(flowCnec, TwoSides.ONE, unit) < leaf.getMargin(flowCnec, TwoSides.TWO, unit) ? TwoSides.ONE : TwoSides.TWO;
            final double flow = leaf.getFlow(flowCnec, limitingSide, unit);
            final Double limitingThreshold = flow >= 0
                ? flowCnec.getUpperBound(limitingSide, unit).orElse(flowCnec.getLowerBound(limitingSide, unit).orElse(Double.NaN))
                : flowCnec.getLowerBound(limitingSide, unit).orElse(flowCnec.getUpperBound(limitingSide, unit).orElse(Double.NaN));

            final VirtualCostlyElementRecord element = new VirtualCostlyElementRecord(leaf.getIdentifier(),
                virtualCostName,
                i,
                flow, unit,
                limitingThreshold, unit,
                leaf.getMargin(flowCnec, limitingSide, unit), unit,
                flowCnec.getNetworkElement().getId(), flowCnec.getState().getId(),
                flowCnec.getId(), flowCnec.getName());
            reportVirtualCostlyElement(parentNode, verbose, element, optimized);
            i++;
        }
    }

    static void reportVirtualCostlyElement(final ReportNode parentNode,
                                           final boolean verbose,
                                           final VirtualCostlyElementRecord element,
                                           final boolean optimized) {
        final OpenRaoLogger logger = verbose ? BUSINESS_LOGS : TECHNICAL_LOGS;
        final TypedValue severity = verbose ? INFO_SEVERITY : TRACE_SEVERITY;

        final String template = optimized ? "openrao.searchtreerao.reportVirtualCostlyElementOptimized" : "openrao.searchtreerao.reportVirtualCostlyElement";
        final String prefix = optimized ? "Optimized " : "";

        final String costlyElementIndex = format(Locale.ENGLISH, "%02d", element.costlyElementIndex());
        final String flow = format(Locale.ENGLISH, "%.2f", element.flow());
        final String threshold = format(Locale.ENGLISH, "%.2f", element.limitingThreshold());
        final String margin = format(Locale.ENGLISH, "%.2f", element.leafMargin());

        parentNode.newReportNode()
            .withMessageTemplate(template)
            .withUntypedValue("leafId", element.leafId())
            .withUntypedValue("virtualCostName", element.virtualCostName())
            .withUntypedValue("costlyElementIndex", costlyElementIndex)
            .withUntypedValue("flow", flow)
            .withUntypedValue("flowUnit", Objects.toString(element.flowUnit()))
            .withUntypedValue("threshold", threshold)
            .withUntypedValue("thresholdUnit", Objects.toString(element.limitingThresholdUnit()))
            .withUntypedValue("margin", margin)
            .withUntypedValue("marginUnit", Objects.toString(element.leafMarginUnit()))
            .withUntypedValue("networkElement", element.networkElementId())
            .withUntypedValue("state", element.stateId())
            .withUntypedValue("cnecId", element.flowCnecId())
            .withUntypedValue("cnecName", element.flowCnecName())
            .withSeverity(severity)
            .add();

        logger.info("{}{}, limiting \"{}\" constraint #{}: flow = {} {}, threshold = {} {}, margin = {} {}, element {} at state {}, CNEC ID = \"{}\", CNEC name = \"{}\"",
            prefix,
            element.leafId(),
            element.virtualCostName(),
            costlyElementIndex,
            flow,
            element.flowUnit(),
            threshold,
            element.limitingThresholdUnit(),
            margin,
            element.leafMarginUnit(),
            element.networkElementId(),
            element.stateId(),
            element.flowCnecId(),
            element.flowCnecName());
    }

    public record VirtualCostlyElementRecord(String leafId,
                                             String virtualCostName,
                                             int costlyElementIndex,
                                             double flow,
                                             Unit flowUnit,
                                             double limitingThreshold,
                                             Unit limitingThresholdUnit,
                                             double leafMargin,
                                             Unit leafMarginUnit,
                                             String networkElementId,
                                             String stateId,
                                             String flowCnecId,
                                             String flowCnecName) {
    }
}
