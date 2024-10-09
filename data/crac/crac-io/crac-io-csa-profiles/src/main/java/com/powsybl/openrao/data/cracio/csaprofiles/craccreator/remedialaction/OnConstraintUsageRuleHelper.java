/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.remedialaction;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.ElementCombinationConstraintKind;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElement;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ContingencyWithRemedialAction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
public final class OnConstraintUsageRuleHelper {

    private OnConstraintUsageRuleHelper() {
    }

    public static Set<Cnec> getImportedCnecFromAssessedElementId(String assessedElementId, Crac crac, Set<ElementaryCreationContext> cnecCreationContexts) {
        return cnecCreationContexts.stream().filter(context -> context.isImported() && assessedElementId.equals(context.getNativeObjectId())).map(context -> crac.getCnec(context.getCreatedObjectId())).collect(Collectors.toSet());
    }

    public static Set<Cnec> getCnecsBuiltFromAssessedElementsCombinableWithRemedialActions(Crac crac, Set<ElementaryCreationContext> cnecCreationContexts, Set<AssessedElement> nativeAssessedElements) {
        Set<Cnec> cnecsCombinableWithRemedialActions = new HashSet<>();
        nativeAssessedElements.stream().filter(AssessedElement::isCombinableWithRemedialAction).forEach(nativeAssessedElement -> cnecsCombinableWithRemedialActions.addAll(getImportedCnecFromAssessedElementId(nativeAssessedElement.mrid(), crac, cnecCreationContexts)));
        return cnecsCombinableWithRemedialActions;
    }

    public static Set<Cnec> filterCnecsThatHaveGivenContingencies(Set<Cnec> cnecs, Set<String> contingenciesIds) {
        return cnecs.stream().filter(cnec -> cnec.getState().getContingency().isPresent() && contingenciesIds.contains(cnec.getState().getContingency().get().getId())).collect(Collectors.toSet());
    }

    public static Map<String, AssociationStatus> processCnecsLinkedToRemedialAction(Crac crac, String remedialActionId, Set<AssessedElement> nativeAssessedElements, Set<AssessedElementWithRemedialAction> linkedAssessedElementWithRemedialActions, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, Set<ElementaryCreationContext> cnecCreationContexts) {
        Map<String, AssociationStatus> contingencyStatusMap = OnContingencyStateUsageRuleHelper.processContingenciesLinkedToRemedialAction(crac, remedialActionId, linkedContingencyWithRemedialActions);
        Map<String, AssociationStatus> cnecStatusMap = new HashMap<>();
        Set<Cnec> cnecsCombinableWithRemedialAction = getCnecsBuiltFromAssessedElementsCombinableWithRemedialActions(crac, cnecCreationContexts, nativeAssessedElements);
        Map<String, ElementCombinationConstraintKind> validContingenciesUsageRules = contingencyStatusMap.entrySet().stream().filter(entry -> entry.getValue().isValid()).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().elementCombinationConstraintKind()));

        for (AssessedElementWithRemedialAction nativeAssessedElementWithRemedialAction : linkedAssessedElementWithRemedialActions) {
            Set<Cnec> cnecs = contingencyStatusMap.isEmpty() ? getImportedCnecFromAssessedElementId(nativeAssessedElementWithRemedialAction.assessedElement(), crac, cnecCreationContexts) : filterCnecsThatHaveGivenContingencies(getImportedCnecFromAssessedElementId(nativeAssessedElementWithRemedialAction.assessedElement(), crac, cnecCreationContexts), validContingenciesUsageRules.keySet());

            if (cnecStatusMap.containsKey(nativeAssessedElementWithRemedialAction.assessedElement()) || cnecs.stream().anyMatch(cnec -> cnecStatusMap.containsKey(cnec.getId()))) {
                cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because this assessed element has several conflictual links to the remedial action.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
                cnecs.stream().map(Cnec::getId).forEach(cnecStatusMap::remove);
                continue;
            }

            cnecsCombinableWithRemedialAction.removeAll(cnecs);

            if (cnecs.isEmpty()) {
                cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because no CNEC was imported by Open RAO from this assessed element.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
                continue;
            }

            if (!nativeAssessedElementWithRemedialAction.normalEnabled()) {
                cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because the association is disabled.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
                continue;
            }

            if (!ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeAssessedElementWithRemedialAction.combinationConstraintKind()) && !ElementCombinationConstraintKind.CONSIDERED.toString().equals(nativeAssessedElementWithRemedialAction.combinationConstraintKind())) {
                cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because of an illegal combinationConstraintKind.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
                continue;
            }

            ElementCombinationConstraintKind combinationConstraintKind = ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeAssessedElementWithRemedialAction.combinationConstraintKind()) ? ElementCombinationConstraintKind.INCLUDED : ElementCombinationConstraintKind.CONSIDERED;

            cnecs.forEach(
                cnec -> {
                    if (cnec.getState().getContingency().isPresent()) {
                        Contingency contingency = cnec.getState().getContingency().get();
                        if (validContingenciesUsageRules.containsKey(contingency.getId()) && combinationConstraintKind != validContingenciesUsageRules.get(contingency.getId())) {
                            cnecStatusMap.put(cnec.getId(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with CNEC %s ignored because the combinationConstraintKinds between of the AssessedElementWithRemedialAction for assessed element %s and the ContingencyWithRemedialAction for contingency %s are different.".formatted(remedialActionId, cnec.getId(), nativeAssessedElementWithRemedialAction.assessedElement(), contingency.getId())));
                            return;
                        }
                    }
                    cnecStatusMap.put(cnec.getId(), new AssociationStatus(true, combinationConstraintKind, ""));
                }
            );
        }

        // Add CNECs built from AssessedElements which are combinable with remedial actions
        if (contingencyStatusMap.isEmpty()) {
            // The remedial action is not associated to contingencies
            cnecsCombinableWithRemedialAction.forEach(cnec -> cnecStatusMap.put(cnec.getId(), new AssociationStatus(true, ElementCombinationConstraintKind.CONSIDERED, "")));
        } else {
            // The remedial action is associated to contingencies (valid or not)
            filterCnecsThatHaveGivenContingencies(cnecsCombinableWithRemedialAction, validContingenciesUsageRules.keySet()).forEach(cnec -> cnecStatusMap.put(cnec.getId(), new AssociationStatus(true, validContingenciesUsageRules.get(cnec.getState().getContingency().orElseThrow().getId()), "")));
        }

        return cnecStatusMap;
    }
}
