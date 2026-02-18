/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.fbconstraint;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.io.commons.RaUsageLimitsAdder;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzerProperties;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.CriticalBranchType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.IndependantComplexVariant;
import com.powsybl.openrao.virtualhubs.HvdcConverter;
import com.powsybl.openrao.virtualhubs.HvdcLine;
import com.powsybl.openrao.virtualhubs.InternalHvdc;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.isEmpty;
import static com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS;
import static com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzerProperties.SuffixMatchPriority.NAME_BEFORE_ORDERCODE;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class FbConstraintCracCreator {

    private static void addFbContraintInstants(Crac crac) {
        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("curative", InstantKind.CURATIVE);
    }

    FbConstraintCreationContext createCrac(FlowBasedConstraintDocument fbConstraintDocument, Network network, CracCreationParameters cracCreatorParameters) {
        FbConstraintCracCreationParameters fbConstraintCracCreationParameters = cracCreatorParameters.getExtension(FbConstraintCracCreationParameters.class);
        OffsetDateTime offsetDateTime = null;
        List<InternalHvdc> internalHvdcs = new ArrayList<>();
        if (fbConstraintCracCreationParameters != null) {
            offsetDateTime = fbConstraintCracCreationParameters.getTimestamp();
            internalHvdcs = fbConstraintCracCreationParameters.getInternalHvdcs();
        }
        FbConstraintCreationContext creationContext = new FbConstraintCreationContext(offsetDateTime, network.getNameOrId());
        Crac crac = cracCreatorParameters.getCracFactory().create(fbConstraintDocument.getDocumentIdentification().getV(), fbConstraintDocument.getDocumentIdentification().getV(), offsetDateTime);
        addFbContraintInstants(crac);
        RaUsageLimitsAdder.addRaUsageLimits(crac, cracCreatorParameters);

        // check timestamp
        if (!checkTimeStamp(offsetDateTime, fbConstraintDocument.getConstraintTimeInterval().getV(), creationContext)) {
            return creationContext.creationFailure();
        }

        // Check for UCTE network
        if (!network.getSourceFormat().equals("UCTE")) {
            creationContext.getCreationReport().error("FlowBasedConstraintDocument CRAC creation is only possible with a UCTE network");
            return creationContext.creationFailure();
        }

        UcteNetworkAnalyzer ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(COMPLETE_WITH_WILDCARDS, NAME_BEFORE_ORDERCODE));

        // Store all Outages while reading CriticalBranches and ComplexVariants
        List<OutageReader> outageReaders = new ArrayList<>();

        // read Critical Branches information
        readCriticalBranches(fbConstraintDocument, offsetDateTime, crac, creationContext, ucteNetworkAnalyzer, outageReaders, cracCreatorParameters.getDefaultMonitoredSides());

        // read Complex Variants information
        readComplexVariants(fbConstraintDocument, offsetDateTime, crac, creationContext, ucteNetworkAnalyzer, outageReaders, internalHvdcs);

        // logs
        creationContext.buildCreationReport();
        return creationContext.creationSucess(crac);
    }

    private void createContingencies(Crac crac, List<OutageReader> outageReaders) {
        outageReaders.forEach(or -> or.addContingency(crac));
    }

    private void readCriticalBranches(FlowBasedConstraintDocument fbConstraintDocument, OffsetDateTime offsetDateTime, Crac crac, FbConstraintCreationContext creationContext, UcteNetworkAnalyzer ucteNetworkAnalyzer, List<OutageReader> outageReaders, Set<TwoSides> defaultMonitoredSides) {
        List<CriticalBranchType> criticalBranchForTimeStamp = selectCriticalBranchesForTimeStamp(fbConstraintDocument, offsetDateTime);

        if (!isEmpty(criticalBranchForTimeStamp)) {
            List<CriticalBranchReader> criticalBranchReaders = criticalBranchForTimeStamp.stream()
                .map(cb -> new CriticalBranchReader(cb, ucteNetworkAnalyzer, defaultMonitoredSides))
                .toList();

            outageReaders.addAll(criticalBranchReaders.stream()
                .filter(CriticalBranchReader::isCriticialBranchValid)
                .filter(cbr -> !cbr.isBaseCase())
                .map(CriticalBranchReader::getOutageReader)
                .toList());

            createContingencies(crac, outageReaders);
            createCnecs(crac, criticalBranchReaders, creationContext);

        } else {
            creationContext.getCreationReport().warn("the flow-based constraint document does not contain any critical branch for the requested timestamp");
        }
        createCnecTimestampFilteringInformation(fbConstraintDocument, offsetDateTime, creationContext);
    }

    private void createCnecs(Crac crac, List<CriticalBranchReader> criticalBranchReaders, FbConstraintCreationContext creationContext) {
        criticalBranchReaders.forEach(criticalBranchReader -> {
            creationContext.addCriticalBranchCreationContext(new CriticalBranchCreationContext(criticalBranchReader, crac));
            if (criticalBranchReader.isCriticialBranchValid()) {
                criticalBranchReader.addCnecs(crac);
            }
        });
    }

    private void createCnecTimestampFilteringInformation(FlowBasedConstraintDocument fbConstraintDocument, OffsetDateTime timestamp, FbConstraintCreationContext creationContext) {
        fbConstraintDocument.getCriticalBranches().getCriticalBranch().stream()
            .filter(criticalBranch -> !isInTimeInterval(timestamp, criticalBranch.getTimeInterval().getV()))
            .filter(criticalBranch -> creationContext.getBranchCnecCreationContext(criticalBranch.getId()) == null)
            .forEach(criticalBranch -> creationContext.addCriticalBranchCreationContext(
                CriticalBranchCreationContext.notImported(criticalBranch.getId(), ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "CriticalBranch is not valid for the requested timestamp")
            ));
    }

    private void readComplexVariants(final FlowBasedConstraintDocument fbConstraintDocument,
                                     final OffsetDateTime offsetDateTime,
                                     final Crac crac,
                                     final FbConstraintCreationContext creationContext,
                                     final UcteNetworkAnalyzer ucteNetworkAnalyzer,
                                     final List<OutageReader> outageReaders,
                                     final List<InternalHvdc> internalHvdcs) {
        if (Objects.isNull(fbConstraintDocument.getComplexVariants())
            || Objects.isNull(fbConstraintDocument.getComplexVariants().getComplexVariant())
            || fbConstraintDocument.getComplexVariants().getComplexVariant().isEmpty()) {
            creationContext.getCreationReport().warn("the flow-based constraint document does not contain any complex variant");
        } else {
            final List<IndependantComplexVariant> remedialActionsForTimestamp = selectRemedialActionsForTimestamp(fbConstraintDocument, offsetDateTime);
            if (!isEmpty(remedialActionsForTimestamp)) {
                createRemedialActions(crac, ucteNetworkAnalyzer, remedialActionsForTimestamp, outageReaders, creationContext, internalHvdcs);
            } else {
                creationContext.getCreationReport().warn("the flow-based constraint document does not contain any complex variant for the requested timestamp");
            }
            createRaTimestampFilteringInformation(fbConstraintDocument, offsetDateTime, creationContext);
        }
    }

    private void createRemedialActions(final Crac crac,
                                       final UcteNetworkAnalyzer ucteNetworkAnalyzer,
                                       final List<IndependantComplexVariant> independantComplexVariants,
                                       final List<OutageReader> outageReaders,
                                       final FbConstraintCreationContext creationContext,
                                       final List<InternalHvdc> internalHvdcs) {
        final Set<String> coIds = outageReaders.stream()
            .filter(OutageReader::isOutageValid)
            .map(oR -> oR.getOutage().getId())
            .collect(Collectors.toSet());

        final List<ComplexVariantReader> complexVariantReaders = independantComplexVariants.stream()
            .map(icv -> new ComplexVariantReader(icv, ucteNetworkAnalyzer, coIds))
            .toList();

        ComplexVariantCrossCompatibility.checkAndInvalidate(complexVariantReaders);

        final Map<Optional<ActionType>, List<ComplexVariantReader>> complexVariantReadersGroupedByType = complexVariantReaders.stream()
            .collect(Collectors.groupingBy(cvr -> Optional.ofNullable(cvr.getType())));

        addPstAndTopoRemedialActionsToCrac(complexVariantReadersGroupedByType.get(Optional.<ActionType>empty()), crac, creationContext);
        addPstAndTopoRemedialActionsToCrac(complexVariantReadersGroupedByType.get(Optional.of(ActionType.PST)), crac, creationContext);
        addPstAndTopoRemedialActionsToCrac(complexVariantReadersGroupedByType.get(Optional.of(ActionType.TOPO)), crac, creationContext);
        addHvdcRemedialActionsToCrac(internalHvdcs, complexVariantReadersGroupedByType.get(Optional.of(ActionType.HVDC)), crac, ucteNetworkAnalyzer, creationContext);
    }

    private static void addPstAndTopoRemedialActionsToCrac(final List<ComplexVariantReader> complexVariantReaders,
                                                           final Crac crac,
                                                           final FbConstraintCreationContext creationContext) {
        if (null == complexVariantReaders) {
            // complexVariantReaders can be null when there is no ComplexVariantReader matching the specified ActionType
            return;
        }

        complexVariantReaders.forEach(cvr -> {
            if (cvr.isComplexVariantValid()) {
                cvr.addRemedialAction(crac);
            }
            creationContext.addComplexVariantCreationContext(cvr.getComplexVariantCreationContext());
        });
    }

    private static void addHvdcRemedialActionsToCrac(final List<InternalHvdc> internalHvdcs,
                                                     final List<ComplexVariantReader> complexVariantReaders,
                                                     final Crac crac,
                                                     final UcteNetworkAnalyzer ucteNetworkAnalyzer,
                                                     final FbConstraintCreationContext creationContext) {
        if (null == complexVariantReaders) {
            // complexVariantReaders can be null when there is no ComplexVariantReader matching the specified ActionType
            return;
        }

        final List<HvdcLine> hvdcLines = internalHvdcs.stream()
            .map(InternalHvdc::lines)
            .flatMap(List::stream)
            .toList();

        final Map<String, String> nodeToStationMap = internalHvdcs.stream()
            .map(InternalHvdc::converters)
            .flatMap(List::stream)
            .collect(Collectors.toMap(HvdcConverter::node, HvdcConverter::station));

        final Map<String, ComplexVariantReader> complexVariantReadersMappedByName = complexVariantReaders.stream()
            .collect(Collectors.toMap(
                cvr -> cvr.getActionReaders().getFirst().getNetworkElementId(),
                cvr -> cvr));

        for (final HvdcLine hvdcLine : hvdcLines) {
            final HvdcLineRemedialActionAdder hvdcLineRemedialActionAdder = new HvdcLineRemedialActionAdder(hvdcLine, ucteNetworkAnalyzer, complexVariantReadersMappedByName, nodeToStationMap);
            hvdcLineRemedialActionAdder.add(crac, creationContext);
        }
        checkInvalidInjectionRangeActionsFromCrac(crac, creationContext);
    }

    private static void checkInvalidInjectionRangeActionsFromCrac(final Crac crac, final FbConstraintCreationContext creationContext) {
        final Map<Optional<String>, List<InjectionRangeAction>> injectionRangeActionsByGroupId = crac.getInjectionRangeActions().stream()
            .collect(Collectors.groupingBy(InjectionRangeAction::getGroupId));

        injectionRangeActionsByGroupId.values().stream()
            .filter(raList -> raList.size() > 1) // On n'a besoin de regarder que les parades qui ont le même groupId, donc les raList qui contiennent plus d'un élément dans la value
            .filter(raList -> injectionRangeActionsHaveInconsistentUsageRules(raList, creationContext))
            .forEach(raList -> {
                // TODO To remove invalid range actions, use crac.removeInjectionRangeAction for each range action in raList
            });
    }

    private static boolean injectionRangeActionHasPreventiveUsageRule(final InjectionRangeAction injectionRangeAction) {
        return injectionRangeAction.getUsageRules().stream()
            .anyMatch(usageRule -> usageRule.getInstant().isPreventive());
    }

    private static boolean injectionRangeActionHasCurativeUsageRule(final InjectionRangeAction injectionRangeAction) {
        return injectionRangeAction.getUsageRules().stream()
            .anyMatch(usageRule -> usageRule.getInstant().isCurative());
    }

    private static boolean injectionRangeActionsHaveInconsistentUsageRules(final List<InjectionRangeAction> injectionRangeActions,
                                                                           final FbConstraintCreationContext creationContext) {

        return injectionRangeActionsHaveInconsistentUsageRulesForInstant(injectionRangeActions,
                creationContext,
                FbConstraintCracCreator::injectionRangeActionHasPreventiveUsageRule,
                "preventive")
            && injectionRangeActionsHaveInconsistentUsageRulesForInstant(injectionRangeActions,
                creationContext,
                FbConstraintCracCreator::injectionRangeActionHasCurativeUsageRule,
                "curative");
        // TODO Should we add a check on afterCoList for curative instant?
    }

    private static boolean injectionRangeActionsHaveInconsistentUsageRulesForInstant(final List<InjectionRangeAction> injectionRangeActions,
                                                                                     final FbConstraintCreationContext creationContext,
                                                                                     final Predicate<InjectionRangeAction> usageRulesPredicate,
                                                                                     final String instant) {
        final boolean allInjectionRangeActionsHaveSameUsageRuleForInstant = injectionRangeActions.stream()
            .allMatch(ira -> usageRulesPredicate.test(ira) == usageRulesPredicate.test(injectionRangeActions.getFirst()));

        if (!allInjectionRangeActionsHaveSameUsageRuleForInstant) {
            final String injectionRangeActionIds = injectionRangeActions.stream().map(InjectionRangeAction::getId).collect(Collectors.joining(", "));
            creationContext.getCreationReport().warn("inconsistent " + instant + " usage rules for injection range actions " + injectionRangeActionIds);
            return true;
        }
        return false;
    }

    private void createRaTimestampFilteringInformation(FlowBasedConstraintDocument fbConstraintDocument, OffsetDateTime timestamp, FbConstraintCreationContext creationContext) {
        fbConstraintDocument.getComplexVariants().getComplexVariant().stream()
            .filter(complexVariant -> !isInTimeInterval(timestamp, complexVariant.getTimeInterval().getV()))
            .filter(complexVariant -> creationContext.getRemedialActionCreationContext(complexVariant.getId()) == null)
            .forEach(complexVariant -> creationContext.addComplexVariantCreationContext(
                new StandardElementaryCreationContext(complexVariant.getId(), null, null, ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "ComplexVariant is not valid for the requested timestamp", false)
            ));
    }

    private List<CriticalBranchType> selectCriticalBranchesForTimeStamp(FlowBasedConstraintDocument document, OffsetDateTime timestamp) {
        if (timestamp == null) {
            return document.getCriticalBranches().getCriticalBranch();
        }

        List<CriticalBranchType> selectedCriticalBranches = new ArrayList<>();

        document.getCriticalBranches().getCriticalBranch().forEach(criticalBranch -> {
            // Select valid critical branches
            if (isInTimeInterval(timestamp, criticalBranch.getTimeInterval().getV())) {
                selectedCriticalBranches.add(criticalBranch);
            }
        });

        return selectedCriticalBranches;
    }

    private List<IndependantComplexVariant> selectRemedialActionsForTimestamp(FlowBasedConstraintDocument document, OffsetDateTime timestamp) {
        if (timestamp == null) {
            return document.getComplexVariants().getComplexVariant();
        }

        List<IndependantComplexVariant> selectedRemedialActions = new ArrayList<>();

        document.getComplexVariants().getComplexVariant().forEach(complexVariant -> {
            // Select valid critical branches
            if (isInTimeInterval(timestamp, complexVariant.getTimeInterval().getV())) {
                selectedRemedialActions.add(complexVariant);
            }
        });

        return selectedRemedialActions;
    }

    private boolean checkTimeStamp(OffsetDateTime offsetDateTime, String fbConstraintDocumentTimeInterval, FbConstraintCreationContext creationContext) {
        if (Objects.isNull(offsetDateTime)) {
            creationContext.getCreationReport().error("when creating a CRAC from a flow-based constraint, timestamp must be non-null");
            return false;
        }

        if (!isInTimeInterval(offsetDateTime, fbConstraintDocumentTimeInterval)) {
            creationContext.getCreationReport().error(String.format("timestamp %s is not in the time interval of the flow-based constraint document: %s", offsetDateTime.toString(), fbConstraintDocumentTimeInterval));
            return false;
        }

        return true;
    }

    private boolean isInTimeInterval(OffsetDateTime offsetDateTime, String timeInterval) {
        String[] timeIntervals = timeInterval.split("/");
        OffsetDateTime startTimeBranch = OffsetDateTime.parse(timeIntervals[0]);
        OffsetDateTime endTimeBranch = OffsetDateTime.parse(timeIntervals[1]);
        // Select valid critical branches
        return !offsetDateTime.isBefore(startTimeBranch) && offsetDateTime.isBefore(endTimeBranch);
    }
}
