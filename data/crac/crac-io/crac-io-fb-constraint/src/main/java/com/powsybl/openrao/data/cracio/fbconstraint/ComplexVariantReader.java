/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.fbconstraint;

import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.cracio.fbconstraint.xsd.ActionsSetType;
import com.powsybl.openrao.data.cracio.fbconstraint.xsd.IndependantComplexVariant;
import com.powsybl.openrao.data.cracio.commons.ucte.UcteNetworkAnalyzer;

import java.util.*;

import static com.powsybl.openrao.data.cracapi.usagerule.UsageMethod.AVAILABLE;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class ComplexVariantReader {

    private final IndependantComplexVariant complexVariant;

    private List<ActionReader> actionReaders;
    private List<String> afterCoList;
    private ElementaryCreationContext complexVariantCreationContext;
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
            addUsageRules(pstRangeActionAdder, crac);
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
            actionReaders.forEach(action -> action.addAction(networkActionAdder, complexVariant.getId()));
            addUsageRules(networkActionAdder, crac);
            networkActionAdder.add();
        }
    }

    ActionReader.Type getType() {
        return type;
    }

    List<ActionReader> getActionReaders() {
        return actionReaders;
    }

    ElementaryCreationContext getComplexVariantCreationContext() {
        if (complexVariantCreationContext == null) {
            complexVariantCreationContext = new StandardElementaryCreationContext(complexVariant.getId(), null, getCreatedRaId(), importStatus, importStatusDetail, false);
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
                .toList();

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

            afterCoList = actionsSet.getAfterCOList().getAfterCOId().stream().filter(validCoIds::contains).toList();
            if (afterCoList.isEmpty()) {
                this.importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
                this.importStatusDetail = String.format("complex variant %s was removed as all its 'afterCO' are invalid", complexVariant.getId());
            }
        }
    }

    private void addUsageRules(RemedialActionAdder<?> remedialActionAdder, Crac crac) {
        ActionsSetType actionsSetType = complexVariant.getActionsSet().get(0);

        if (actionsSetType.isPreventive()) {
            remedialActionAdder.newOnInstantUsageRule()
                    .withInstant(crac.getPreventiveInstant().getId())
                    .withUsageMethod(AVAILABLE)
                    .add();
        }

        if (actionsSetType.isCurative() && !Objects.isNull(afterCoList)) {
            for (String co : afterCoList) {
                remedialActionAdder.newOnContingencyStateUsageRule()
                        .withContingency(co)
                        .withInstant(crac.getInstant(InstantKind.CURATIVE).getId())
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
