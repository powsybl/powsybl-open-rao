/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.ActionsSetType;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.IndependantComplexVariant;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_api.usage_rule.UsageMethod.AVAILABLE;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class ComplexVariantReader {

    private final IndependantComplexVariant complexVariant;

    private List<ActionReader> actionReaders;
    private List<String> afterCoList;
    private ComplexVariantCreationContext complexVariantCreationContext;
    private ActionReader.Type type;

    private ImportStatus importStatus;
    private String importStatusDetail;

    boolean isComplexVariantValid() {
        return importStatus.equals(ImportStatus.IMPORTED);
    }

    IndependantComplexVariant getComplexVariant() {
        return complexVariant;
    }

    ComplexVariantReader(IndependantComplexVariant complexVariant, UcteNetworkAnalyzer ucteNetworkAnalyzer, Set<String> validCoIds) {
        this.complexVariant = complexVariant;

        interpretWithNetwork(ucteNetworkAnalyzer);
        if (isComplexVariantValid()) {
            interpretUsageRules(validCoIds);
        }
    }

    void addRemedialAction(Crac crac) {
        if (type.equals(ActionReader.Type.PST)) {
            PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
                    .withId(complexVariant.getId())
                    .withName(complexVariant.getName())
                    .withOperator(complexVariant.getTsoOrigin());
            actionReaders.get(0).addAction(pstRangeActionAdder);
            addUsageRules(pstRangeActionAdder);
            pstRangeActionAdder.add();
            complexVariantCreationContext = PstComplexVariantCreationContext.imported(
                    complexVariant.getId(),
                    actionReaders.get(0).getNativeNetworkElementId(),
                    getCreatedRaId(),
                    actionReaders.get(0).isInverted(),
                    actionReaders.get(0).getInversionMessage()
            );
        } else {
            NetworkActionAdder networkActionAdder = crac.newNetworkAction()
                    .withId(complexVariant.getId())
                    .withName(complexVariant.getName())
                    .withOperator(complexVariant.getTsoOrigin());
            actionReaders.forEach(action -> action.addAction(networkActionAdder));
            addUsageRules(networkActionAdder);
            networkActionAdder.add();
        }
    }

    ActionReader.Type getType() {
        return type;
    }

    List<ActionReader> getActionReaders() {
        return actionReaders;
    }

    ComplexVariantCreationContext getComplexVariantCreationContext() {
        if (complexVariantCreationContext == null) {
            complexVariantCreationContext = new ComplexVariantCreationContext(complexVariant.getId(), importStatus, getCreatedRaId(), importStatusDetail);
        }
        return complexVariantCreationContext;
    }

    void invalidateOnIncompatibilityWithOtherVariants() {
        this.importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
        this.importStatusDetail = "Invalid because other ComplexVariant(s) is(are) defined on same branch and perimeter";
    }

    private void interpretWithNetwork(UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.importStatus = ImportStatus.IMPORTED;

        if (complexVariant.getActionsSet().size() != 1) {
            this.importStatus = ImportStatus.INCOMPLETE_DATA;
            this.importStatusDetail = String.format("complex variant %s was removed as it should contain one and only one actionSet", complexVariant.getId());
            return;
        }

        // interpret actions
        actionReaders = complexVariant.getActionsSet().get(0).getAction().stream()
                .map(actionType -> new ActionReader(actionType, ucteNetworkAnalyzer))
                .collect(Collectors.toList());

        Optional<ActionReader> invalidAction = actionReaders.stream().filter(actionReader -> !actionReader.isActionValid()).findAny();

        if (invalidAction.isPresent()) {
            this.importStatus = ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK;
            this.importStatusDetail = String.format("complex variant %s was removed as %s", complexVariant.getId(), invalidAction.get().getInvalidActionReason());
            return;
        }

        if (actionReaders.isEmpty()) {
            this.importStatus = ImportStatus.INCOMPLETE_DATA;
            this.importStatusDetail = String.format("complex variant %s was removed as it must contain at least one action", complexVariant.getId());
            return;
        }

        if (actionReaders.stream().anyMatch(actionReader -> actionReader.getType().equals(ActionReader.Type.PST))) {
            if (actionReaders.size() > 1) {
                this.importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
                this.importStatusDetail = String.format("complex variant %s was removed as it contains several actions which are not topological actions", complexVariant.getId());
            } else {
                this.type = ActionReader.Type.PST;
            }
        } else {
            this.type = ActionReader.Type.TOPO;
        }
    }

    private void interpretUsageRules(Set<String> validCoIds) {

        ActionsSetType actionsSet = complexVariant.getActionsSet().get(0);

        if (actionsSet.isCurative() && actionsSet.isPreventive()) {
            this.importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
            this.importStatusDetail = String.format("complex variant %s was removed as it cannot be preventive and curative", complexVariant.getId());
            return;
        }

        if (actionsSet.isCurative() || Boolean.TRUE.equals(actionsSet.isEnforced())) {
            if (Objects.isNull(actionsSet.getAfterCOList()) || Objects.isNull(actionsSet.getAfterCOList().getAfterCOId()) || actionsSet.getAfterCOList().getAfterCOId().isEmpty()) {
                this.importStatus = ImportStatus.INCOMPLETE_DATA;
                this.importStatusDetail = String.format("complex variant %s was removed as its 'afterCOList' is empty", complexVariant.getId());
                return;
            }

            afterCoList = actionsSet.getAfterCOList().getAfterCOId().stream().filter(validCoIds::contains).collect(Collectors.toList());
            if (afterCoList.isEmpty()) {
                this.importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
                this.importStatusDetail = String.format("complex variant %s was removed as all its 'afterCO' are invalid", complexVariant.getId());
            }
        }
    }

    private void addUsageRules(RemedialActionAdder<?> remedialActionAdder) {
        ActionsSetType actionsSetType = complexVariant.getActionsSet().get(0);

        if (actionsSetType.isPreventive()) {
            remedialActionAdder.newFreeToUseUsageRule()
                    .withInstant(Instant.PREVENTIVE)
                    .withUsageMethod(AVAILABLE)
                    .add();
        }

        if (actionsSetType.isCurative() && !Objects.isNull(afterCoList)) {
            for (String co : afterCoList) {
                remedialActionAdder.newOnStateUsageRule()
                        .withContingency(co)
                        .withInstant(Instant.CURATIVE)
                        .withUsageMethod(AVAILABLE)
                        .add();
            }
        }
    }

    private String getCreatedRaId() {
        if (isComplexVariantValid()) {
            return complexVariant.getId();
        } else {
            return null;
        }
    }
}
