/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileElementaryCreationContext;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class OnConstraintUsageRuleHelper {
    private final Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts;

    private final Set<String> importedCnecsCombinableWithRas = new HashSet<>();
    private final Map<String, Set<String>> excludedCnecsByRemedialAction = new HashMap<>();
    private final Map<String, Set<String>> includedCnecsByRemedialAction = new HashMap<>();
    private final Map<String, Set<String>> consideredCnecsElementsByRemedialAction = new HashMap<>();

    public OnConstraintUsageRuleHelper(Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, PropertyBags assessedElements, PropertyBags assessedElementsWithRemedialAction) {
        this.csaProfileCnecCreationContexts = csaProfileCnecCreationContexts;
        readAssessedElementsCombinableWithRemedialActions(assessedElements);
        readAssessedElementsWithRemedialAction(assessedElementsWithRemedialAction);
    }

    private void readAssessedElementsCombinableWithRemedialActions(PropertyBags assessedElements) {
        List<String> nativeIdsOfCnecsCombinableWithRas = assessedElements.stream()
            .filter(element -> element.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_REMEDIAL_ACTION) != null && Boolean.parseBoolean(element.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_REMEDIAL_ACTION)))
            .map(element -> element.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT)).collect(Collectors.toList());
        Map<String, Set<String>> nativeToFaraoCnecIdsCombinableWithRas = getImportedCnecsNativeIdsToFaraoIds();
        nativeToFaraoCnecIdsCombinableWithRas.keySet().retainAll(nativeIdsOfCnecsCombinableWithRas);
        nativeToFaraoCnecIdsCombinableWithRas.values().stream().flatMap(Set::stream).forEach(importedCnecsCombinableWithRas::add);
    }

    private void readAssessedElementsWithRemedialAction(PropertyBags assessedElementsWithRemedialAction) {
        assessedElementsWithRemedialAction.stream().filter(this::checkNormalEnabled).filter(propertyBag -> getImportedCnecsNativeIdsToFaraoIds().containsKey(propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT))).forEach(propertyBag -> {
            String combinationConstraintKind = propertyBag.get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND);
            String remedialActionId = propertyBag.getId(CsaProfileConstants.REQUEST_REMEDIAL_ACTION);
            String assessedElementId = propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT);

            Set<String> faraoCnecIds = getImportedCnecsNativeIdsToFaraoIds().get(assessedElementId);

            if (combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString())) {
                excludedCnecsByRemedialAction
                    .computeIfAbsent(remedialActionId, k -> new HashSet<>())
                    .addAll(faraoCnecIds);
            } else if (combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString())) {
                includedCnecsByRemedialAction
                    .computeIfAbsent(remedialActionId, k -> new HashSet<>())
                    .addAll(faraoCnecIds);
            } else if (combinationConstraintKind.equals(CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString())) {
                consideredCnecsElementsByRemedialAction
                    .computeIfAbsent(remedialActionId, k -> new HashSet<>())
                    .addAll(faraoCnecIds);
            } else {
                throw new FaraoException(String.format("Unsupported combinationConstraintKind of AssessedElementsWithRemedialAction with id %s, only  ['INCLUDED', 'EXCLUDED', 'CONSIDERED'] are supported ", propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION)));
            }
        });
    }

    private boolean checkNormalEnabled(PropertyBag propertyBag) {
        Optional<String> normalEnabledOpt = Optional.ofNullable(propertyBag.get(CsaProfileConstants.NORMAL_ENABLED));
        return normalEnabledOpt.isEmpty() || Boolean.parseBoolean(normalEnabledOpt.get());
    }

    private Map<String, Set<String>> getImportedCnecsNativeIdsToFaraoIds() {
        return csaProfileCnecCreationContexts.stream()
            .filter(CsaProfileElementaryCreationContext::isImported)
            .collect(Collectors.groupingBy(
                CsaProfileElementaryCreationContext::getNativeId,
                Collectors.mapping(CsaProfileElementaryCreationContext::getElementId, Collectors.toSet())
            ));
    }

    public Set<String> getImportedCnecsCombinableWithRas() {
        return importedCnecsCombinableWithRas;
    }

    public Map<String, Set<String>> getExcludedCnecsByRemedialAction() {
        return this.excludedCnecsByRemedialAction;
    }

    public Map<String, Set<String>> getIncludedCnecsByRemedialAction() {
        return this.includedCnecsByRemedialAction;
    }

    public Map<String, Set<String>> getConsideredCnecsElementsByRemedialAction() {
        return this.consideredCnecsElementsByRemedialAction;
    }
}
