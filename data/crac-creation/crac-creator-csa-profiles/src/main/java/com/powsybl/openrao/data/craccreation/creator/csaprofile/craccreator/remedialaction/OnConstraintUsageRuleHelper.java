/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileElementaryCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElementWithRemedialAction;

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
    private final Map<String, Set<String>> consideredCnecsByRemedialAction = new HashMap<>();

    public OnConstraintUsageRuleHelper(Set<CsaProfileElementaryCreationContext> csaProfileCnecCreationContexts, Set<AssessedElement> nativeAssessedElements, Set<AssessedElementWithRemedialAction> nativeAssessedElementWithRemedialActions) {
        this.csaProfileCnecCreationContexts = csaProfileCnecCreationContexts;
        readAssessedElementsCombinableWithRemedialActions(nativeAssessedElements);
        readAssessedElementsWithRemedialAction(nativeAssessedElementWithRemedialActions);
    }

    private void readAssessedElementsCombinableWithRemedialActions(Set<AssessedElement> nativeAssessedElements) {
        List<String> nativeIdsOfCnecsCombinableWithRas = nativeAssessedElements.stream()
            .filter(AssessedElement::isCombinableWithRemedialAction)
            .map(AssessedElement::identifier).toList();
        Map<String, Set<String>> nativeToOpenRaoCnecIdsCombinableWithRas = getImportedCnecsNativeIdsToOpenRaoIds();
        nativeToOpenRaoCnecIdsCombinableWithRas.keySet().retainAll(nativeIdsOfCnecsCombinableWithRas);
        nativeToOpenRaoCnecIdsCombinableWithRas.values().stream().flatMap(Set::stream).forEach(importedCnecsCombinableWithRas::add);
    }

    private void readAssessedElementsWithRemedialAction(Set<AssessedElementWithRemedialAction> nativeAssessedElementWithRemedialActions) {
        nativeAssessedElementWithRemedialActions.stream()
            .filter(AssessedElementWithRemedialAction::normalEnabled)
            .filter(nativeAssessedElementWithRemedialAction -> getImportedCnecsNativeIdsToOpenRaoIds().containsKey(nativeAssessedElementWithRemedialAction.assessedElement()))
            .forEach(nativeAssessedElementWithRemedialAction -> {
                Set<String> openRaoCnecIds = getImportedCnecsNativeIdsToOpenRaoIds().get(nativeAssessedElementWithRemedialAction.assessedElement());

                if (CsaProfileConstants.ElementCombinationConstraintKind.EXCLUDED.toString().equals(nativeAssessedElementWithRemedialAction.combinationConstraintKind())) {
                    excludedCnecsByRemedialAction
                        .computeIfAbsent(nativeAssessedElementWithRemedialAction.remedialAction(), k -> new HashSet<>())
                        .addAll(openRaoCnecIds);
                } else if (CsaProfileConstants.ElementCombinationConstraintKind.INCLUDED.toString().equals(nativeAssessedElementWithRemedialAction.combinationConstraintKind())) {
                    includedCnecsByRemedialAction
                        .computeIfAbsent(nativeAssessedElementWithRemedialAction.remedialAction(), k -> new HashSet<>())
                        .addAll(openRaoCnecIds);
                } else if (CsaProfileConstants.ElementCombinationConstraintKind.CONSIDERED.toString().equals(nativeAssessedElementWithRemedialAction.combinationConstraintKind())) {
                    consideredCnecsByRemedialAction
                        .computeIfAbsent(nativeAssessedElementWithRemedialAction.remedialAction(), k -> new HashSet<>())
                        .addAll(openRaoCnecIds);
                } else {
                    throw new OpenRaoException(String.format("Unsupported combinationConstraintKind of AssessedElementsWithRemedialAction with id %s, only  ['INCLUDED', 'EXCLUDED', 'CONSIDERED'] are supported ", nativeAssessedElementWithRemedialAction.identifier()));
                }
            });
    }

    private Map<String, Set<String>> getImportedCnecsNativeIdsToOpenRaoIds() {
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

    public Map<String, Set<String>> getConsideredAndIncludedCnecsByRemedialAction() {
        Map<String, Set<String>> result = new HashMap<>(this.includedCnecsByRemedialAction);
        result.putAll(consideredCnecsByRemedialAction);
        return result;
    }
}
