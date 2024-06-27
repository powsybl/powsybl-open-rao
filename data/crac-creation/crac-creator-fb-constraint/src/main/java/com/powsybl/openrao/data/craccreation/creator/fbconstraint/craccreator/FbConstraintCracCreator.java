/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.fbconstraint.craccreator;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreationReport;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreator;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.fbconstraint.xsd.CriticalBranchType;
import com.powsybl.openrao.data.craccreation.creator.fbconstraint.FbConstraint;
import com.powsybl.openrao.data.craccreation.creator.fbconstraint.xsd.FlowBasedConstraintDocument;
import com.powsybl.openrao.data.craccreation.creator.fbconstraint.xsd.IndependantComplexVariant;
import com.powsybl.openrao.data.craccreation.util.RaUsageLimitsAdder;
import com.powsybl.openrao.data.craccreation.util.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.craccreation.util.ucte.UcteNetworkAnalyzerProperties;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.craccreation.util.ucte.UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS;
import static com.google.common.collect.Iterables.isEmpty;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class FbConstraintCracCreator implements CracCreator<FbConstraint, FbConstraintCreationContext> {

    private static void addFbContraintInstants(Crac crac) {
        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("curative", InstantKind.CURATIVE);
    }

    @Override
    public String getNativeCracFormat() {
        return "FlowBasedConstraintDocument";
    }

    @Override
    public FbConstraintCreationContext createCrac(FbConstraint fbConstraintDocument, Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreatorParameters, ReportNode reportNode) {
        ReportNode fbConstraintCracCreatorReportNode = FbConstraintReports.reportFbConstraintCracCreator(reportNode);
        ReportNode fbConstraintCracCreationContextReportNode = FbConstraintReports.reportFbConstraintCracCreationContext(fbConstraintCracCreatorReportNode);
        FbConstraintCreationContext creationContext = new FbConstraintCreationContext(offsetDateTime, network.getNameOrId(), fbConstraintCracCreationContextReportNode);
        Crac crac = cracCreatorParameters.getCracFactory().create(fbConstraintDocument.getDocument().getDocumentIdentification().getV(), fbConstraintCracCreatorReportNode);
        addFbContraintInstants(crac);
        RaUsageLimitsAdder.addRaUsageLimits(crac, cracCreatorParameters);

        // check timestamp
        if (!checkTimeStamp(offsetDateTime, fbConstraintDocument.getDocument().getConstraintTimeInterval().getV(), creationContext, fbConstraintCracCreatorReportNode)) {
            return creationContext.creationFailure();
        }

        // Check for UCTE network
        if (!network.getSourceFormat().equals("UCTE")) {
            CracCreationReport.error("FlowBasedConstraintDocument CRAC creation is only possible with a UCTE network", fbConstraintCracCreatorReportNode);
            return creationContext.creationFailure();
        }

        UcteNetworkAnalyzer ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(COMPLETE_WITH_WILDCARDS));

        // Store all Outages while reading CriticalBranches and ComplexVariants
        List<OutageReader> outageReaders = new ArrayList<>();

        // read Critical Branches information
        readCriticalBranches(fbConstraintDocument, offsetDateTime, crac, creationContext, ucteNetworkAnalyzer, outageReaders, cracCreatorParameters.getDefaultMonitoredSides(), fbConstraintCracCreatorReportNode);

        // read Complex Variants information
        readComplexVariants(fbConstraintDocument, offsetDateTime, crac, creationContext, ucteNetworkAnalyzer, outageReaders, fbConstraintCracCreatorReportNode);

        // logs
        creationContext.buildCreationReport();
        return creationContext.creationSucess(crac);
    }

    private void createContingencies(Crac crac, List<OutageReader> outageReaders) {
        outageReaders.forEach(or -> or.addContingency(crac));
    }

    private void readCriticalBranches(FbConstraint fbConstraintDocument, OffsetDateTime offsetDateTime, Crac crac, FbConstraintCreationContext creationContext, UcteNetworkAnalyzer ucteNetworkAnalyzer, List<OutageReader> outageReaders, Set<TwoSides> defaultMonitoredSides, ReportNode reportNode) {
        List<CriticalBranchType> criticalBranchForTimeStamp = selectCriticalBranchesForTimeStamp(fbConstraintDocument.getDocument(), offsetDateTime);

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
            CracCreationReport.warn("the flow-based constraint document does not contain any critical branch for the requested timestamp", reportNode);
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

    private void createCnecTimestampFilteringInformation(FbConstraint fbConstraintDocument, OffsetDateTime timestamp, FbConstraintCreationContext creationContext) {
        fbConstraintDocument.getDocument().getCriticalBranches().getCriticalBranch().stream()
            .filter(criticalBranch -> !isInTimeInterval(timestamp, criticalBranch.getTimeInterval().getV()))
            .filter(criticalBranch -> creationContext.getBranchCnecCreationContext(criticalBranch.getId()) == null)
            .forEach(criticalBranch -> creationContext.addCriticalBranchCreationContext(
                CriticalBranchCreationContext.notImported(criticalBranch.getId(), ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, "CriticalBranch is not valid for the requested timestamp")
            ));
    }

    private void readComplexVariants(FbConstraint fbConstraintDocument, OffsetDateTime offsetDateTime, Crac crac, FbConstraintCreationContext creationContext, UcteNetworkAnalyzer ucteNetworkAnalyzer, List<OutageReader> outageReaders, ReportNode reportNode) {
        if (Objects.isNull(fbConstraintDocument.getDocument().getComplexVariants())
            || Objects.isNull(fbConstraintDocument.getDocument().getComplexVariants().getComplexVariant())
            || fbConstraintDocument.getDocument().getComplexVariants().getComplexVariant().isEmpty()) {
            CracCreationReport.warn("the flow-based constraint document does not contain any complex variant", reportNode);
        } else {
            List<IndependantComplexVariant> remedialActionForTimeStamp = selectRemedialActionsForTimeStamp(fbConstraintDocument.getDocument(), offsetDateTime);
            if (!isEmpty(remedialActionForTimeStamp)) {
                createRemedialAction(crac, ucteNetworkAnalyzer, remedialActionForTimeStamp, outageReaders, creationContext);
            } else {
                CracCreationReport.warn("the flow-based constraint document does not contain any complex variant for the requested timestamp", reportNode);
            }
            createRaTimestampFilteringInformation(fbConstraintDocument, offsetDateTime, creationContext);
        }
    }

    private void createRemedialAction(Crac crac, UcteNetworkAnalyzer ucteNetworkAnalyzer, List<IndependantComplexVariant> independantComplexVariants, List<OutageReader> outageReaders, FbConstraintCreationContext creationContext) {

        Set<String> coIds = outageReaders.stream().filter(OutageReader::isOutageValid).map(oR -> oR.getOutage().getId()).collect(Collectors.toSet());

        List<ComplexVariantReader> complexVariantReaders = independantComplexVariants.stream()
            .map(icv -> new ComplexVariantReader(icv, ucteNetworkAnalyzer, coIds))
            .toList();

        ComplexVariantCrossCompatibility.checkAndInvalidate(complexVariantReaders);

        complexVariantReaders.forEach(cvr -> {
            if (cvr.isComplexVariantValid()) {
                cvr.addRemedialAction(crac);
            }
            creationContext.addComplexVariantCreationContext(cvr.getComplexVariantCreationContext());
        });
    }

    private void createRaTimestampFilteringInformation(FbConstraint fbConstraintDocument, OffsetDateTime timestamp, FbConstraintCreationContext creationContext) {
        fbConstraintDocument.getDocument().getComplexVariants().getComplexVariant().stream()
             .filter(complexVariant -> !isInTimeInterval(timestamp, complexVariant.getTimeInterval().getV()))
            .filter(complexVariant -> creationContext.getRemedialActionCreationContext(complexVariant.getId()) == null)
            .forEach(complexVariant -> creationContext.addComplexVariantCreationContext(
                new ComplexVariantCreationContext(complexVariant.getId(), ImportStatus.NOT_FOR_REQUESTED_TIMESTAMP, null, "ComplexVariant is not valid for the requested timestamp")
            ));
    }

    private List<CriticalBranchType> selectCriticalBranchesForTimeStamp(FlowBasedConstraintDocument document, OffsetDateTime timestamp) {

        if (timestamp == null) {
            return document.getCriticalBranches().getCriticalBranch();
        } else {
            List<CriticalBranchType> selectedCriticalBranches = new ArrayList<>();

            document.getCriticalBranches().getCriticalBranch().forEach(criticalBranch -> {
                // Select valid critical branches
                if (isInTimeInterval(timestamp, criticalBranch.getTimeInterval().getV())) {
                    selectedCriticalBranches.add(criticalBranch);
                }
            });
            return selectedCriticalBranches;
        }
    }

    private List<IndependantComplexVariant> selectRemedialActionsForTimeStamp(FlowBasedConstraintDocument document, OffsetDateTime timestamp) {

        if (timestamp == null) {
            return document.getComplexVariants().getComplexVariant();
        } else {
            List<IndependantComplexVariant> selectedRemedialActions = new ArrayList<>();

            document.getComplexVariants().getComplexVariant().forEach(complexVariant -> {
                // Select valid critical branches
                if (isInTimeInterval(timestamp, complexVariant.getTimeInterval().getV())) {
                    selectedRemedialActions.add(complexVariant);
                }
            });
            return selectedRemedialActions;
        }
    }

    private boolean checkTimeStamp(OffsetDateTime offsetDateTime, String fbConstraintDocumentTimeInterval, FbConstraintCreationContext creationContext, ReportNode reportNode) {
        if (Objects.isNull(offsetDateTime)) {
            CracCreationReport.error("when creating a CRAC from a flow-based constraint, timestamp must be non-null", reportNode);
            return false;
        }

        if (!isInTimeInterval(offsetDateTime, fbConstraintDocumentTimeInterval)) {
            CracCreationReport.error(String.format("timestamp %s is not in the time interval of the flow-based constraint document: %s", offsetDateTime.toString(), fbConstraintDocumentTimeInterval), reportNode);
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
