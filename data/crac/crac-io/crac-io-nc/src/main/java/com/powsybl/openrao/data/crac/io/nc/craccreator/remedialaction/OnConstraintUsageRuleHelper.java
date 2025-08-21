/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.openrao.data.crac.io.nc.objects.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.crac.io.nc.objects.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;

import java.util.HashMap;
import java.util.Map;
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
        Set<String> validLinkedContingencies = contingencyStatusMap.entrySet().stream().filter(entry -> entry.getValue().isValid()).map(Map.Entry::getKey).collect(Collectors.toSet());

        for (AssessedElementWithRemedialAction nativeAssessedElementWithRemedialAction : linkedAssessedElementWithRemedialActions) {
            Set<Cnec> cnecs = contingencyStatusMap.isEmpty() ? getImportedCnecFromAssessedElementId(nativeAssessedElementWithRemedialAction.assessedElement(), crac, cnecCreationContexts) : filterCnecsThatHaveGivenContingencies(getImportedCnecFromAssessedElementId(nativeAssessedElementWithRemedialAction.assessedElement(), crac, cnecCreationContexts), validLinkedContingencies);

            if (isAssociationInvalid(remedialActionId, cnecStatusMap, nativeAssessedElementWithRemedialAction, cnecs)) {
                continue;
            }

            cnecs.forEach(cnec -> cnecStatusMap.put(cnec.getId(), new AssociationStatus(true, "")));
        }

        return cnecStatusMap;
    }

    private static boolean isAssociationInvalid(String remedialActionId, Map<String, AssociationStatus> cnecStatusMap, AssessedElementWithRemedialAction nativeAssessedElementWithRemedialAction, Set<Cnec> cnecs) {
        if (cnecStatusMap.containsKey(nativeAssessedElementWithRemedialAction.assessedElement()) || cnecs.stream().anyMatch(cnec -> cnecStatusMap.containsKey(cnec.getId()))) {
            cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because this assessed element has several conflictual links to the remedial action.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
            cnecs.stream().map(Cnec::getId).forEach(cnecStatusMap::remove);
            return true;
        }

        if (cnecs.isEmpty()) {
            cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because no CNEC was imported by Open RAO from this assessed element.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
            return true;
        }

        if (!nativeAssessedElementWithRemedialAction.normalEnabled()) {
            cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because the association is disabled.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
            return true;
        }

        if (!nativeAssessedElementWithRemedialAction.isIncluded()) {
            cnecStatusMap.put(nativeAssessedElementWithRemedialAction.assessedElement(), new AssociationStatus(false, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because only included combinationConstraintKinds are supported.".formatted(remedialActionId, nativeAssessedElementWithRemedialAction.assessedElement())));
            return true;
        }
        return false;
    }
}
