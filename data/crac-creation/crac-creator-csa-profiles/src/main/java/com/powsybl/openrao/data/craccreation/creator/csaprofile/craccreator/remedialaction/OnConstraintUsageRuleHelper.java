/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.COMBINATION_CONSTRAINT_KIND;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.REQUEST_ASSESSED_ELEMENT;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
public final class OnConstraintUsageRuleHelper {

    private OnConstraintUsageRuleHelper() {
    }

    public static Set<Cnec> getImportedCnecFromAssessedElementId(String assessedElementId, Crac crac, Set<CsaProfileElementaryCreationContext> cnecCreationContexts) {
        return cnecCreationContexts.stream().filter(context -> context.isImported() && assessedElementId.equals(context.getNativeId())).map(context -> crac.getCnec(context.getElementId())).collect(Collectors.toSet());
    }

    public static Set<Cnec> getCnecsBuiltFromAssessedElementsCombinableWithRemedialActions(Crac crac, Set<CsaProfileElementaryCreationContext> cnecCreationContexts, PropertyBags assessedElementPropertyBags) {
        Set<Cnec> cnecsCombinableWithRemedialActions = new HashSet<>();
        assessedElementPropertyBags.stream().filter(propertyBag -> Boolean.parseBoolean(propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_REMEDIAL_ACTION))).forEach(propertyBag -> cnecsCombinableWithRemedialActions.addAll(getImportedCnecFromAssessedElementId(propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT), crac, cnecCreationContexts)));
        return cnecsCombinableWithRemedialActions;
    }

    public static Set<Cnec> filterCnecsThatHaveGivenContingencies(Set<Cnec> cnecs, Set<String> contingenciesIds) {
        return cnecs.stream().filter(cnec -> cnec.getState().getContingency().isPresent() && contingenciesIds.contains(cnec.getState().getContingency().get().getId())).collect(Collectors.toSet());
    }

    public static Map<String, AssociationStatus> processCnecsLinkedToRemedialAction(Crac crac, String remedialActionId, PropertyBags assessedElementPropertyBags, Set<PropertyBag> linkedAssessedElementWithRemedialActions, Set<PropertyBag> linkedContingencyWithRemedialActions, Set<CsaProfileElementaryCreationContext> cnecCreationContexts) {
        Map<String, AssociationStatus> contingencyStatusMap = OnContingencyStateUsageRuleHelper.processContingenciesLinkedToRemedialAction(crac, remedialActionId, linkedContingencyWithRemedialActions);
        Map<String, AssociationStatus> cnecStatusMap = new HashMap<>();
        Set<Cnec> cnecsCombinableWithRemedialAction = getCnecsBuiltFromAssessedElementsCombinableWithRemedialActions(crac, cnecCreationContexts, assessedElementPropertyBags);
        Map<String, CsaProfileConstants.ElementCombinationConstraintKind> validContingenciesUsageRules = contingencyStatusMap.entrySet().stream().filter(entry -> entry.getValue().isValid()).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().elementCombinationConstraintKind()));

        for (PropertyBag assessedElementWithRemedialActionPropertyBag : linkedAssessedElementWithRemedialActions) {
            String assessedElementId = assessedElementWithRemedialActionPropertyBag.getId(REQUEST_ASSESSED_ELEMENT);
            Set<Cnec> cnecs = contingencyStatusMap.isEmpty() ? getImportedCnecFromAssessedElementId(assessedElementId, crac, cnecCreationContexts) : filterCnecsThatHaveGivenContingencies(getImportedCnecFromAssessedElementId(assessedElementId, crac, cnecCreationContexts), validContingenciesUsageRules.keySet());

            if (cnecStatusMap.containsKey(assessedElementId) || cnecs.stream().anyMatch(cnec -> cnecStatusMap.containsKey(cnec.getId()))) {
                cnecStatusMap.put(assessedElementId, new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because this assessed element has several conflictual links to the remedial action.".formatted(remedialActionId, assessedElementId)));
                cnecs.stream().map(Cnec::getId).forEach(cnecStatusMap::remove);
                continue;
            }

            String combinationConstraintKindStr = assessedElementWithRemedialActionPropertyBag.get(COMBINATION_CONSTRAINT_KIND);
            Optional<String> normalEnabledOpt = Optional.ofNullable(assessedElementWithRemedialActionPropertyBag.get(CsaProfileConstants.NORMAL_ENABLED));

            cnecsCombinableWithRemedialAction.removeAll(cnecs);

            if (cnecs.isEmpty()) {
                cnecStatusMap.put(assessedElementId, new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because no CNEC was imported by Open RAO from this assessed element.".formatted(remedialActionId, assessedElementId)));
                continue;
            }

            if (normalEnabledOpt.isPresent() && !Boolean.parseBoolean(normalEnabledOpt.get())) {
                cnecStatusMap.put(assessedElementId, new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because the association is disabled.".formatted(remedialActionId, assessedElementId)));
                continue;
            }

            if (!combinationConstraintKindStr.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString()) && !combinationConstraintKindStr.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
                cnecStatusMap.put(assessedElementId, new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with assessed element %s ignored because of an illegal combinationConstraintKind.".formatted(remedialActionId, assessedElementId)));
                continue;
            }

            CsaProfileConstants.ElementCombinationConstraintKind combinationConstraintKind = CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(combinationConstraintKindStr) ? CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED : CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED;

            cnecs.forEach(
                cnec -> {
                    if (cnec.getState().getContingency().isPresent()) {
                        Contingency contingency = cnec.getState().getContingency().get();
                        if (validContingenciesUsageRules.containsKey(contingency.getId()) && combinationConstraintKind != validContingenciesUsageRules.get(contingency.getId())) {
                            cnecStatusMap.put(cnec.getId(), new AssociationStatus(false, null, "OnConstraint usage rule for remedial action %s with CNEC %s ignored because the combinationConstraintKinds between of the AssessedElementWithRemedialAction for assessed element %s and the ContingencyWithRemedialAction for contingency %s are different.".formatted(remedialActionId, cnec.getId(), assessedElementId, contingency.getId())));
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
            cnecsCombinableWithRemedialAction.forEach(cnec -> cnecStatusMap.put(cnec.getId(), new AssociationStatus(true, CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED, "")));
        } else {
            // The remedial action is associated to contingencies (valid or not)
            filterCnecsThatHaveGivenContingencies(cnecsCombinableWithRemedialAction, validContingenciesUsageRules.keySet()).forEach(cnec -> cnecStatusMap.put(cnec.getId(), new AssociationStatus(true, validContingenciesUsageRules.get(cnec.getState().getContingency().orElseThrow().getId()), "")));
        }

        return cnecStatusMap;
    }
}
