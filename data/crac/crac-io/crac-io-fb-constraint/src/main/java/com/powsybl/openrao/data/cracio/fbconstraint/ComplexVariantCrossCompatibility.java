/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.fbconstraint;

import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracio.fbconstraint.xsd.ActionsSetType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ComplexVariantCrossCompatibility {

    private static final Comparator<ComplexVariantReader> COMPLEX_VARIANT_PRIORITY_RULE =
            Comparator.nullsLast(Comparator.comparing(ComplexVariantCrossCompatibility::getGroupId))
                    .thenComparing((ComplexVariantReader cvr) -> cvr.getComplexVariant().getId());

    private ComplexVariantCrossCompatibility() {
    }

    static void checkAndInvalidate(List<ComplexVariantReader> complexVariantReaders) {

        int n = complexVariantReaders.size();

        for (int i = 0; i < n - 1; i++) {

            if (!complexVariantReaders.get(i).isComplexVariantValid()) {
                continue;
            }

            List<ComplexVariantReader> overlappingComplexVariants = new ArrayList<>();

            for (int j = i + 1; j < n; j++) {

                if (complexVariantReaders.get(j).isComplexVariantValid()
                        && actionsOverlap(complexVariantReaders.get(i), complexVariantReaders.get(j))
                        && usageRulesOverlap(complexVariantReaders.get(i), complexVariantReaders.get(j))) {
                    overlappingComplexVariants.add(complexVariantReaders.get(j));
                }
            }

            if (!overlappingComplexVariants.isEmpty()) {
                overlappingComplexVariants.add(complexVariantReaders.get(i));
                invalidateAllButOne(overlappingComplexVariants);
            }
        }
    }

    private static boolean actionsOverlap(ComplexVariantReader cvr1, ComplexVariantReader cvr2) {

        if (cvr1.getType() == ActionReader.Type.PST && cvr2.getType() == ActionReader.Type.PST) {

            // PST actions overlap each others if they both act on a same pst
            Set<String> pst1 = cvr1.getActionReaders().stream()
                    .map(ActionReader::getNetworkElementId)
                    .collect(Collectors.toSet());

            Set<String> pst2 = cvr2.getActionReaders().stream()
                    .map(ActionReader::getNetworkElementId)
                    .collect(Collectors.toSet());

            return !Collections.disjoint(pst1, pst2);

        } else if (cvr1.getType() == ActionReader.Type.TOPO && cvr2.getType() == ActionReader.Type.TOPO) {

            // TOPO actions overlap each others if they both act on the same network elements, with the same actions

            Map<String, ActionType> actions1 = new HashMap<>();
            Map<String, ActionType> actions2 = new HashMap<>();

            cvr1.getActionReaders().forEach(ar -> actions1.put(ar.getNetworkElementId(), ar.getActionType()));
            cvr2.getActionReaders().forEach(ar -> actions2.put(ar.getNetworkElementId(), ar.getActionType()));
            return actions1.equals(actions2);

        } else {
            return false;
        }
    }

    private static boolean usageRulesOverlap(ComplexVariantReader cvr1, ComplexVariantReader cvr2) {

        // usageRules overlap each others if they are available on a same state

        ActionsSetType ast1 = cvr1.getComplexVariant().getActionsSet().get(0);
        ActionsSetType ast2 = cvr2.getComplexVariant().getActionsSet().get(0);

        if (ast1.isPreventive() && ast2.isPreventive()) {
            return true;
        }

        return ast1.isCurative() && ast2.isCurative()
            && !Collections.disjoint(ast1.getAfterCOList().getAfterCOId(), ast2.getAfterCOList().getAfterCOId());
    }

    private static void invalidateAllButOne(List<ComplexVariantReader> overlappingComplexVariants) {

        // prioritize the ones with a groupId
        // then take the first in alphabetical order
        overlappingComplexVariants.sort(COMPLEX_VARIANT_PRIORITY_RULE);

        for (int i = 1; i < overlappingComplexVariants.size(); i++) {
            overlappingComplexVariants.get(i).invalidateOnIncompatibilityWithOtherVariants();
        }
    }

    private static String getGroupId(ComplexVariantReader cvr) {
        return cvr.getActionReaders().stream()
                .filter(ar -> ar.getGroupId() != null)
                .map(ActionReader::getGroupId)
                .findAny().orElse("ZZ");
    }
}
