/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreator;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.CriticalBranchType;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.FbConstraint;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.FlowBasedConstraintDocument;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.IndependantComplexVariant;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzerProperties;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS;
import static com.google.common.collect.Iterables.isEmpty;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class FbConstraintCracCreator implements CracCreator<FbConstraint, FbConstraintCreationContext> {

    @Override
    public String getNativeCracFormat() {
        return "FlowBasedConstraintDocument";
    }

    @Override
    public FbConstraintCreationContext createCrac(FbConstraint fbConstraintDocument, Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreatorParameters) {
        FbConstraintCreationContext creationContext = new FbConstraintCreationContext(offsetDateTime, network.getNameOrId());
        Crac crac = cracCreatorParameters.getCracFactory().create(fbConstraintDocument.getDocument().getDocumentIdentification().getV());

        // check timestamp
        if (!checkTimeStamp(offsetDateTime, fbConstraintDocument.getDocument().getConstraintTimeInterval().getV(), creationContext)) {
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
        readCriticalBranches(fbConstraintDocument, offsetDateTime, crac, creationContext, ucteNetworkAnalyzer, outageReaders);

        // read Complex Variants information
        readComplexVariants(fbConstraintDocument, offsetDateTime, crac, creationContext, ucteNetworkAnalyzer, outageReaders);

        // logs
        creationContext.buildCreationReport();
        return creationContext.creationSucess(crac);
    }

    private void createContingencies(Crac crac, List<OutageReader> outageReaders) {
        outageReaders.forEach(or -> or.addContingency(crac));
    }

    private void readCriticalBranches(FbConstraint fbConstraintDocument, OffsetDateTime offsetDateTime, Crac crac, FbConstraintCreationContext creationContext, UcteNetworkAnalyzer ucteNetworkAnalyzer, List<OutageReader> outageReaders) {
        List<CriticalBranchType> criticalBranchForTimeStamp = selectCriticalBranchesForTimeStamp(fbConstraintDocument.getDocument(), offsetDateTime);

        if (!isEmpty(criticalBranchForTimeStamp)) {
            List<CriticalBranchReader> criticalBranchReaders = criticalBranchForTimeStamp.stream()
                .map(cb -> new CriticalBranchReader(cb, ucteNetworkAnalyzer))
                .collect(Collectors.toList());

            outageReaders.addAll(criticalBranchReaders.stream()
                .filter(CriticalBranchReader::isCriticialBranchValid)
                .filter(cbr -> !cbr.isBaseCase())
                .map(CriticalBranchReader::getOutageReader)
                .collect(Collectors.toList()));

            createContingencies(crac, outageReaders);
            createCnecs(crac, criticalBranchReaders, creationContext);

        } else {
            creationContext.getCreationReport().warn("the flow-based constraint document does not contain any critical branch for the requested timestamp");
        }
        createCnecTimestampFilteringInformation(fbConstraintDocument, offsetDateTime, creationContext);
    }

    private void createCnecs(Crac crac, List<CriticalBranchReader> criticalBranchReaders, FbConstraintCreationContext creationContext) {
        criticalBranchReaders.forEach(criticalBranchReader -> {
            creationContext.addCriticalBranchCreationContext(new CriticalBranchCreationContext(criticalBranchReader));
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

    private void readComplexVariants(FbConstraint fbConstraintDocument, OffsetDateTime offsetDateTime, Crac crac, FbConstraintCreationContext creationContext, UcteNetworkAnalyzer ucteNetworkAnalyzer, List<OutageReader> outageReaders) {
        if (Objects.isNull(fbConstraintDocument.getDocument().getComplexVariants())
            || Objects.isNull(fbConstraintDocument.getDocument().getComplexVariants().getComplexVariant())
            || fbConstraintDocument.getDocument().getComplexVariants().getComplexVariant().isEmpty()) {
            creationContext.getCreationReport().warn("the flow-based constraint document does not contain any complex variant");
        } else {
            List<IndependantComplexVariant> remedialActionForTimeStamp = selectRemedialActionsForTimeStamp(fbConstraintDocument.getDocument(), offsetDateTime);
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
            .collect(Collectors.toList());

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
