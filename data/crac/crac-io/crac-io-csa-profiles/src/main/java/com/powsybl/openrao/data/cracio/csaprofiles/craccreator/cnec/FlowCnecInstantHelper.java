package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.cnec;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.LoadingLimits;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracio.csaprofiles.parameters.CsaCracCreationParameters;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class FlowCnecInstantHelper {
    private final CsaCracCreationParameters csaCracCreationParameters;
    private final Crac crac;

    public FlowCnecInstantHelper(CsaCracCreationParameters csaCracCreationParameters, Crac crac) {
        this.csaCracCreationParameters = csaCracCreationParameters;
        this.crac = crac;
        checkCraApplicationWindowMap();
    }

    // CSA CRAC Creation Parameters checking

    private void checkCraApplicationWindowMap() {
        List<Pair<String, Integer>> curativeInstantsData = csaCracCreationParameters.getCurativeInstants();
        List<Instant> curativeInstants = crac.getInstants(InstantKind.CURATIVE).stream().toList();
        for (Instant curativeInstant : curativeInstants) {
            if (curativeInstantsData.stream().noneMatch(instantData -> curativeInstant.getId().equals(instantData.getLeft()))) {
                throw new OpenRaoException("curative-instants is missing \"" + curativeInstant.getId() + "\" instant.");
            }
        }
        for (int instantIndex = 0; instantIndex < curativeInstantsData.size() - 1; instantIndex++) {
            if (curativeInstantsData.get(instantIndex).getRight() >= curativeInstantsData.get(instantIndex + 1).getRight()) {
                throw new OpenRaoException("The TATL acceptable duration for %s cannot be longer than the acceptable duration for %s.".formatted(curativeInstantsData.get(instantIndex).getLeft(), curativeInstantsData.get(instantIndex + 1).getLeft()));
            }
        }
    }

    // Instant to limits mapping

    Set<Integer> getAllTatlDurationsOnSide(Branch<?> branch, TwoSides side) {
        return branch.getCurrentLimits(side).map(limits -> limits.getTemporaryLimits().stream().map(LoadingLimits.TemporaryLimit::getAcceptableDuration).collect(Collectors.toSet())).orElseGet(Set::of);
    }

    public Map<String, Integer> mapPostContingencyInstantsAndLimitDurations(Branch<?> branch, TwoSides side, String tso) {
        Map<String, Integer> instantToLimit = new HashMap<>();
        List<Pair<String, Integer>> curativeInstantsData = csaCracCreationParameters.getCurativeInstants();
        boolean doNotUsePatlInFinalState = csaCracCreationParameters.getTsosWhichDoNotUsePatlInFinalState().contains(tso);
        Set<Integer> tatlDurations = getAllTatlDurationsOnSide(branch, side);
        // raise exception if a TSO not using the PATL has no TATL either
        // associate instant to TATL duration, or Integer.MAX_VALUE if PATL
        int longestDuration = doNotUsePatlInFinalState ? tatlDurations.stream().max(Integer::compareTo).orElse(Integer.MAX_VALUE) : Integer.MAX_VALUE; // longest TATL duration or infinite (PATL)
        // TODO: check with TSOs if auto-instant-application-time should not be used here instead
        instantToLimit.put(crac.getInstant(InstantKind.OUTAGE).getId(), tatlDurations.stream().filter(tatlDuration -> tatlDuration >= 0 && tatlDuration < curativeInstantsData.get(0).getRight()).max(Integer::compareTo).orElse(getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, 0, longestDuration)));
        instantToLimit.put(crac.getInstant(InstantKind.AUTO).getId(), getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, curativeInstantsData.get(0).getRight(), longestDuration));
        for (int instantIndex = 0; instantIndex < curativeInstantsData.size() - 1; instantIndex++) {
            instantToLimit.put(curativeInstantsData.get(instantIndex).getLeft(), getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, curativeInstantsData.get(instantIndex + 1).getRight(), longestDuration));
        }
        instantToLimit.put(curativeInstantsData.get(curativeInstantsData.size() - 1).getLeft(), longestDuration);
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
