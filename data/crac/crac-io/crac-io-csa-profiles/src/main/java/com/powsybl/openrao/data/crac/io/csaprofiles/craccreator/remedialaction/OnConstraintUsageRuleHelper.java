/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.remedialaction;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.io.csaprofiles.craccreator.constants.ElementCombinationConstraintKind;
import com.powsybl.openrao.data.crac.io.csaprofiles.nc.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.crac.io.csaprofiles.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class OnConstraintUsageRuleHelper {

    private OnConstraintUsageRuleHelper() {
    }

    public static Set<Cnec> getImportedCnecFromAssessedElementId(String assessedElementId, Crac crac, Set<ElementaryCreationContext> cnecCreationContexts) {
        return cnecCreationContexts.stream().filter(context -> context.isImported() && assessedElementId.equals(context.getNativeObjectId())).map(context -> crac.getCnec(context.getCreatedObjectId())).collect(Collectors.toSet());
    }

    public static Set<Cnec> filterCnecsThatHaveGivenContingencies(Set<Cnec> cnecs, Set<String> contingenciesIds) {
        return cnecs.stream().filter(cnec -> cnec.getState().getContingency().isPresent() && contingenciesIds.contains(cnec.getState().getContingency().get().getId())).collect(Collectors.toSet());
    }

    public static Map<String, AssociationStatus> processCnecsLinkedToRemedialAction(Crac crac, String remedialActionId, Set<AssessedElementWithRemedialAction> linkedAssessedElementWithRemedialActions, Set<ContingencyWithRemedialAction> linkedContingencyWithRemedialActions, Set<ElementaryCreationContext> cnecCreationContexts) {
        Map<String, AssociationStatus> contingencyStatusMap = OnContingencyStateUsageRuleHelper.processContingenciesLinkedToRemedialAction(crac, remedialActionId, linkedContingencyWithRemedialActions);
        Map<String, AssociationStatus> cnecStatusMap = new HashMap<>();
        Map<String, ElementCombinationConstraintKind> validContingenciesUsageRules = contingencyStatusMap.entrySet().stream().filter(entry -> entry.getValue().isValid()).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().elementCombinationConstraintKind()));

        for (AssessedElementWithRemedialAction nativeAssessedElementWithRemedialAction : linkedAssessedElementWithRemedialActions) {
            Set<Cnec> cnecs = contingencyStatusMap.isEmpty() ? getImportedCnecFromAssessedElementId(nativeAssessedElementWithRemedialAction.assessedElement(), crac, cnecCreationContexts) : filterCnecsThatHaveGivenContingencies(getImportedCnecFromAssessedElementId(nativeAssessedElementWithRemedialAction.assessedElement(), crac, cnecCreationContexts), validContingenciesUsageRules.keySet());

            if (isAssociationInvalid(remedialActionId, cnecStatusMap, nativeAssessedElementWithRemedialAction, cnecs)) {
                continue;
            }

            ElementCombinationConstraintKind combinationConstraintKind = ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeAssessedElementWithRemedialAction.combinationConstraintKind()) ? ElementCombinationConstraintKind.INCLUDED : ElementCombinationConstraintKind.CONSIDERED;
            cnecs.forEach(cnec -> handleLinkedContingencies(remedialActionId, cnecStatusMap, validContingenciesUsageRules, nativeAssessedElementWithRemedialAction, combinationConstraintKind, cnec));
        }

        return cnecStatusMap;
    }

    private static boolean isAssociationInvalid(String remedialActionId, Map<String, AssociationStatus> cnecStatusMap, AssessedElementWithRemedialAction nativeAssessedElementWithRemedialAction, Set<Cnec> cnecs) {
        if (cnecStatusMap.containsKey(nativeAssessedElementWithRemedialAction.assessedElement()) || cnecs.stream().anyMatch(cnec -> cnecStatusMap.containsKey(cnec.getId()))) {
            cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because this assessed element has several conflictual links to the remedial action.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
            cnecs.stream().map(Cnec::getId).forEach(cnecStatusMap::remove);
            return true;
        }

        if (cnecs.isEmpty()) {
            cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because no CNEC was imported by Open RAO from this assessed element.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
            return true;
        }

        if (!nativeAssessedElementWithRemedialAction.normalEnabled()) {
            cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because the association is disabled.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
            return true;
        }

        if (!ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeAssessedElementWithRemedialAction.combinationConstraintKind()) && !ElementCombinationConstraintKind.CONSIDERED.toString().equals(nativeAssessedElementWithRemedialAction.combinationConstraintKind())) {
            cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because of an illegal combinationConstraintKind.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
            return true;
        }
        return false;
    }

    private static void handleLinkedContingencies(String remedialActionId, Map<String, AssociationStatus> cnecStatusMap, Map<String, ElementCombinationConstraintKind> validContingenciesUsageRules, AssessedElementWithRemedialAction nativeAssessedElementWithRemedialAction, ElementCombinationConstraintKind combinationConstraintKind, Cnec cnec) {
        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) {
            Contingency contingency = optionalContingency.get();
            if (validContingenciesUsageRules.containsKey(contingency.getId()) && combinationConstraintKind != validContingenciesUsageRules.get(contingency.getId())) {
                cnecStatusMap.put(cnec.getId(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with CNEC %s ignored because the combinationConstraintKinds between of the AssessedElementWithRemedialAction for assessed element %s and the ContingencyWithRemedialAction for contingency %s are different.".formatted(remedialActionId, cnec.getId(), nativeAssessedElementWithRemedialAction.assessedElement(), contingency.getId())));
                return;
            }
        }
        cnecStatusMap.put(cnec.getId(), new AssociationStatus(true, combinationConstraintKind, ""));
    }
}
