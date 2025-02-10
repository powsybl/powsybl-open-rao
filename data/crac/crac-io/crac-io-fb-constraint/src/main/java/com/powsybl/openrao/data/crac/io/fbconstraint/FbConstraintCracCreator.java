/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.fbconstraint;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.CriticalBranchType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.IndependantComplexVariant;
import com.powsybl.openrao.data.crac.io.commons.RaUsageLimitsAdder;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzerProperties;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.isEmpty;
import static com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class FbConstraintCracCreator {

    private static void addFbContraintInstants(Crac crac) {
        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("curative", InstantKind.CURATIVE);
    }

    FbConstraintCreationContext createCrac(FlowBasedConstraintDocument fbConstraintDocument, Network network, CracCreationParameters cracCreatorParameters) {
        FbConstraintCracCreationParameters fbConstraintCracCreationParameters = cracCreatorParameters.getExtension(FbConstraintCracCreationParameters.class);
        OffsetDateTime offsetDateTime = fbConstraintCracCreationParameters.getTimestamp();
        FbConstraintCreationContext creationContext = new FbConstraintCreationContext(offsetDateTime, network.getNameOrId());
        Crac crac = cracCreatorParameters.getCracFactory().create(fbConstraintDocument.getDocumentIdentification().getV());
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

        UcteNetworkAnalyzer ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(COMPLETE_WITH_WILDCARDS));

        // Store all Outages while reading CriticalBranches and ComplexVariants
        List<OutageReader> outageReaders = new ArrayList<>();

        // read Critical Branches information
        readCriticalBranches(fbConstraintDocument, offsetDateTime, crac, creationContext, ucteNetworkAnalyzer, outageReaders, cracCreatorParameters.getDefaultMonitoredSides());

        // read Complex Variants information
        readComplexVariants(fbConstraintDocument, offsetDateTime, crac, creationContext, ucteNetworkAnalyzer, outageReaders);

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

    private void readComplexVariants(FlowBasedConstraintDocument fbConstraintDocument, OffsetDateTime offsetDateTime, Crac crac, FbConstraintCreationContext creationContext, UcteNetworkAnalyzer ucteNetworkAnalyzer, List<OutageReader> outageReaders) {
        if (Objects.isNull(fbConstraintDocument.getComplexVariants())
            || Objects.isNull(fbConstraintDocument.getComplexVariants().getComplexVariant())
            || fbConstraintDocument.getComplexVariants().getComplexVariant().isEmpty()) {
            creationContext.getCreationReport().warn("the flow-based constraint document does not contain any complex variant");
        } else {
            List<IndependantComplexVariant> remedialActionForTimeStamp = selectRemedialActionsForTimeStamp(fbConstraintDocument, offsetDateTime);
            if (!isEmpty(remedialActionForTimeStamp)) {
                createRemedialAction(crac, ucteNetworkAnalyzer, remedialActionForTimeStamp, outageReaders, creationContext);
            } else {
                creationContext.getCreationReport().warn("the flow-based constraint document does not contain any complex variant for the requested timestamp");
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
