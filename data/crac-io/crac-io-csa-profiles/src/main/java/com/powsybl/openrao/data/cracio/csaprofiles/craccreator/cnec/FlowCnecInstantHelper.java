package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.cnec;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.LoadingLimits;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracio.csaprofiles.parameters.CsaCracCreationParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class FlowCnecInstantHelper {
    private final CsaCracCreationParameters csaCracCreationParameters;
    private final List<Instant> instants;

    public FlowCnecInstantHelper(CsaCracCreationParameters csaCracCreationParameters, List<Instant> instants) {
        this.csaCracCreationParameters = csaCracCreationParameters;
        this.instants = new ArrayList<>(instants);
        checkCraApplicationWindowMap();
    }

    // CSA CRAC Creation Parameters checking

    private void checkCraApplicationWindowMap() {
        Map<String, Integer> curativeBatchPostOutageTimeMap = csaCracCreationParameters.getCurativeBatchPostOutageTime();
        List<Instant> curativeInstants = instants.stream().filter(Instant::isCurative).sorted(Instant::compareTo).toList();
        for (Instant curativeInstant : curativeInstants) {
            if (!curativeBatchPostOutageTimeMap.containsKey(curativeInstant.getId())) {
                throw new OpenRaoException("curative-batch-post-outage-time map is missing \"" + curativeInstant.getId() + "\" key.");
            }
        }
        for (int instantIndex = 0; instantIndex < curativeInstants.size() - 1; instantIndex++) {
            if (curativeBatchPostOutageTimeMap.get(curativeInstants.get(instantIndex).getId()) >= curativeBatchPostOutageTimeMap.get(curativeInstants.get(instantIndex + 1).getId())) {
                throw new OpenRaoException("The TATL acceptable duration for %s cannot be longer than the acceptable duration for %s.".formatted(curativeInstants.get(instantIndex).getId(), curativeInstants.get(instantIndex + 1).getId()));
            }
        }
    }

    // Instant to limits mapping

    Set<Integer> getAllTatlDurationsOnSide(Branch<?> branch, TwoSides side) {
        return branch.getCurrentLimits(side).map(limits -> limits.getTemporaryLimits().stream().map(LoadingLimits.TemporaryLimit::getAcceptableDuration).collect(Collectors.toSet())).orElseGet(Set::of);
    }

    public Map<String, Integer> mapPostContingencyInstantsAndLimitDurations(Branch<?> branch, TwoSides side, String tso) {
        Map<String, Integer> instantToLimit = new HashMap<>();
        Map<String, Integer> curativeBatchPostOutageTimeMap = csaCracCreationParameters.getCurativeBatchPostOutageTime();
        boolean doNotUsePatlInFinalState = csaCracCreationParameters.getTsosWhichDoNotUsePatlInFinalState().contains(tso);
        Set<Integer> tatlDurations = getAllTatlDurationsOnSide(branch, side);
        // raise exception if a TSO not using the PATL has no TATL either
        // associate instant to TATL duration, or Integer.MAX_VALUE if PATL
        int longestDuration = doNotUsePatlInFinalState ? tatlDurations.stream().max(Integer::compareTo).orElse(Integer.MAX_VALUE) : Integer.MAX_VALUE; // longest TATL duration or infinite (PATL)
        Instant outageInstant = instants.stream().filter(Instant::isOutage).findFirst().get();
        Optional<Instant> autoInstant = instants.stream().filter(Instant::isAuto).findFirst();
        List<Instant> curativeInstants = instants.stream().filter(Instant::isCurative).sorted(Instant::compareTo).toList();
        instantToLimit.put(outageInstant.getId(), tatlDurations.stream().filter(tatlDuration -> tatlDuration >= 0 && tatlDuration < curativeBatchPostOutageTimeMap.get(curativeInstants.get(0).getId())).max(Integer::compareTo).orElse(getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, 0, longestDuration)));
        autoInstant.ifPresent(instant -> instantToLimit.put(instant.getId(), getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, curativeBatchPostOutageTimeMap.get(curativeInstants.get(0).getId()), longestDuration)));
        for (int instantIndex = 0; instantIndex < curativeInstants.size() - 1; instantIndex++) {
            instantToLimit.put(curativeInstants.get(instantIndex).getId(), getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, curativeBatchPostOutageTimeMap.get(curativeInstants.get(instantIndex + 1).getId()), longestDuration));
        }
        instantToLimit.put(curativeInstants.get(curativeInstants.size() - 1).getId(), longestDuration);
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
