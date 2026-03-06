/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.fbconstraint;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.api.StandardElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.ActionsSetType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.IndependantComplexVariant;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzer;

import java.util.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class ComplexVariantReader {

    private final IndependantComplexVariant complexVariant;

    private List<ActionReader> actionReaders;
    private List<String> afterCoList;
    private ElementaryCreationContext complexVariantCreationContext;
    private ActionTypeEnum type;

    private ImportStatus importStatus;
    private String importStatusDetail;

    boolean isComplexVariantValid() {
        return importStatus.equals(ImportStatus.IMPORTED);
    }

    IndependantComplexVariant getComplexVariant() {
        return complexVariant;
    }

    List<String> getAfterCoList() {
        return afterCoList;
    }

    ComplexVariantReader(IndependantComplexVariant complexVariant, UcteNetworkAnalyzer ucteNetworkAnalyzer, Set<String> validCoIds) {
        this.complexVariant = complexVariant;

        interpretWithNetwork(ucteNetworkAnalyzer);
        if (isComplexVariantValid()) {
            interpretUsageRules(validCoIds);
        }
    }

    void addRemedialAction(Crac crac) {
        if (type.equals(ActionTypeEnum.PST)) {
            addPstRemedialAction(crac);
        } else if (type.equals(ActionTypeEnum.TOPO)) {
            addTopologicalRemedialAction(crac);
        }
        // InjectionRemedialAction for HVDC type are created in FbConstraintCracCreator with HvdcLineRemedialActionAdder
        // because one remedial action is composed of two complex variants and can therefore not be built in a single ComplexVariantReader
    }

    private void addPstRemedialAction(final Crac crac) {
        final PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId(complexVariant.getId())
            .withName(complexVariant.getName())
            .withOperator(complexVariant.getTsoOrigin());
        final ActionReader actionReader = actionReaders.getFirst();
        actionReader.addAction(pstRangeActionAdder);
        addUsageRules(pstRangeActionAdder, crac);
        pstRangeActionAdder.add();
        complexVariantCreationContext = PstComplexVariantCreationContext.imported(
            complexVariant.getId(),
            actionReader.getNativeNetworkElementId(),
            getCreatedRaId(),
            actionReader.isInverted(),
            actionReader.getInversionMessage()
        );
    }

    private void addTopologicalRemedialAction(final Crac crac) {
        final NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withId(complexVariant.getId())
            .withName(complexVariant.getName())
            .withOperator(complexVariant.getTsoOrigin());
        actionReaders.forEach(action -> action.addAction(networkActionAdder, complexVariant.getId()));
        addUsageRules(networkActionAdder, crac);
        networkActionAdder.add();
    }

    ActionTypeEnum getType() {
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

    void invalidateOnInvalidGenerator(final ImportStatus status, final String detail) {
        if (!status.equals(ImportStatus.IMPORTED)) {
            this.importStatus = status;
            this.importStatusDetail = detail;
        }
    }

    void invalidateOnInconsistencyOnState(final String state) {
        this.importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
        this.importStatusDetail = "Invalid because other ComplexVariant has opposite activation rule on " + state + " state";
    }

    private void interpretWithNetwork(UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.importStatus = ImportStatus.IMPORTED;

        if (complexVariant.getActionsSet().size() != 1) {
            this.importStatus = ImportStatus.INCOMPLETE_DATA;
            this.importStatusDetail = String.format("complex variant %s was removed as it should contain one and only one actionSet", complexVariant.getId());
            return;
        }

        // interpret actions
        actionReaders = complexVariant.getActionsSet().getFirst().getAction().stream()
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

        if (actionReaders.stream().anyMatch(actionReader -> actionReader.getType().equals(ActionTypeEnum.PST))) {
            if (actionReaders.size() > 1) {
                this.importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
                this.importStatusDetail = String.format("complex variant %s was removed as it contains several actions which are not topological actions", complexVariant.getId());
            } else {
                this.type = ActionTypeEnum.PST;
            }
        } else if (actionReaders.stream().anyMatch(actionReader -> actionReader.getType().equals(ActionTypeEnum.HVDC))) {
            if (actionReaders.size() > 1) {
                this.importStatus = ImportStatus.INCONSISTENCY_IN_DATA;
                this.importStatusDetail = String.format("complex variant %s was removed as it contains several actions", complexVariant.getId());
            } else {
                this.type = ActionTypeEnum.HVDC;
            }
        } else {
            this.type = ActionTypeEnum.TOPO;
        }
    }

    private void interpretUsageRules(final Set<String> validCoIds) {
        final ActionsSetType actionsSet = complexVariant.getActionsSet().getFirst();

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

    private void addUsageRules(final RemedialActionAdder<?> remedialActionAdder, final Crac crac) {
        final ActionsSetType actionsSetType = complexVariant.getActionsSet().getFirst();

        if (actionsSetType.isPreventive()) {
            remedialActionAdder.newOnInstantUsageRule()
                .withInstant(crac.getPreventiveInstant().getId())
                .add();
        }

        if (actionsSetType.isCurative() && !Objects.isNull(afterCoList)) {
            for (String co : afterCoList) {
                remedialActionAdder.newOnContingencyStateUsageRule()
                    .withContingency(co)
                    .withInstant(crac.getInstant(InstantKind.CURATIVE).getId())
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
