/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.cnec;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.LoadingLimits;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class FlowCnecInstantHelper {
    private final NcCracCreationParameters ncCracCreationParameters;
    private final Crac crac;

    public FlowCnecInstantHelper(NcCracCreationParameters ncCracCreationParameters, Crac crac) {
        this.ncCracCreationParameters = ncCracCreationParameters;
        this.crac = crac;
    }

    // Instant to limits mapping

    Set<Integer> getAllTatlDurationsOnSide(Branch<?> branch, TwoSides side) {
        return branch.getCurrentLimits(side).map(limits -> limits.getTemporaryLimits().stream().map(LoadingLimits.TemporaryLimit::getAcceptableDuration).collect(Collectors.toSet())).orElseGet(Set::of);
    }

    public Map<String, Integer> mapPostContingencyInstantsAndLimitDurations(Branch<?> branch, TwoSides side, String tso) {
        Map<String, Integer> instantToLimit = new HashMap<>();
        Map<String, Integer> curativeInstantsMap = ncCracCreationParameters.getCurativeInstants();
        List<String> sortedCurativeInstants = curativeInstantsMap.entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue)).map(Map.Entry::getKey).toList();
        boolean doNotUsePatlInFinalState = ncCracCreationParameters.getTsosWhichDoNotUsePatlInFinalState().contains(tso);
        Set<Integer> tatlDurations = getAllTatlDurationsOnSide(branch, side);
        // raise exception if a TSO not using the PATL has no TATL either
        // associate instant to TATL duration, or Integer.MAX_VALUE if PATL
        int longestDuration = doNotUsePatlInFinalState ? tatlDurations.stream().max(Integer::compareTo).orElse(Integer.MAX_VALUE) : Integer.MAX_VALUE; // longest TATL duration or infinite (PATL)
        instantToLimit.put(crac.getInstant(InstantKind.OUTAGE).getId(), tatlDurations.stream().filter(tatlDuration -> tatlDuration >= 0 && tatlDuration < curativeInstantsMap.get(sortedCurativeInstants.get(0))).max(Integer::compareTo).orElse(getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, 0, longestDuration)));
        instantToLimit.put(crac.getInstant(InstantKind.AUTO).getId(), getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, curativeInstantsMap.get(sortedCurativeInstants.get(0)), longestDuration));
        for (int instantIndex = 0; instantIndex < sortedCurativeInstants.size() - 1; instantIndex++) {
            instantToLimit.put(sortedCurativeInstants.get(instantIndex), getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, curativeInstantsMap.get(sortedCurativeInstants.get(instantIndex + 1)), longestDuration));
        }
        instantToLimit.put(sortedCurativeInstants.get(curativeInstantsMap.size() - 1), longestDuration);
        return instantToLimit;
    }

    private int getShortestTatlWithDurationGreaterThanOrReturn(Set<Integer> tatlDurations, int duration, int longestDuration) {
        return tatlDurations.stream().filter(tatlDuration -> tatlDuration >= duration).min(Integer::compareTo).orElse(longestDuration);
    }

    // Retrieve instant from limit duration

    public Set<String> getPostContingencyInstantsAssociatedToLimitDuration(Map<String, Integer> mapInstantsAndLimits, int limitDuration) {
        // if limitDuration is not a key of the map, take closest greater duration
        int durationThreshold = mapInstantsAndLimits.containsValue(limitDuration) ? limitDuration : mapInstantsAndLimits.values().stream().filter(duration -> duration > limitDuration).min(Integer::compareTo).orElse(Integer.MAX_VALUE);
        return mapInstantsAndLimits.entrySet().stream().filter(entry -> entry.getValue() == durationThreshold).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public Set<String> getPostContingencyInstantsAssociatedToPatl(Map<String, Integer> mapInstantsAndLimits) {
        return getPostContingencyInstantsAssociatedToLimitDuration(mapInstantsAndLimits, Integer.MAX_VALUE);
    }

}
