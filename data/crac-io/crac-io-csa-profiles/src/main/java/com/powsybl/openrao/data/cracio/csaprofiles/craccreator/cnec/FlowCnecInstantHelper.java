package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.cnec;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.LoadingLimits;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracio.csaprofiles.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class FlowCnecInstantHelper {

    private final Set<String> tsosWhichDoNotUsePatlInFinalState;
    private final int curative1InstantDuration;
    private final int curative2InstantDuration;
    private final int curative3InstantDuration;

    private final Set<String> tsos = Set.of("REE", "REN", "RTE");
    private final Set<String> instants = Set.of(CsaProfileConstants.CURATIVE_1_INSTANT, CsaProfileConstants.CURATIVE_2_INSTANT, CsaProfileConstants.CURATIVE_3_INSTANT);

    public FlowCnecInstantHelper(CracCreationParameters parameters) {
        CsaCracCreationParameters csaParameters = parameters.getExtension(CsaCracCreationParameters.class);
        checkCsaExtension(csaParameters);
        checkUsePatlInFinalStateMap(csaParameters);
        checkCraApplicationWindowMap(csaParameters);
        tsosWhichDoNotUsePatlInFinalState = csaParameters.getUsePatlInFinalState().entrySet().stream().filter(entry -> !entry.getValue()).map(Map.Entry::getKey).collect(Collectors.toSet());
        curative1InstantDuration = csaParameters.getCraApplicationWindow().get(CsaProfileConstants.CURATIVE_1_INSTANT);
        curative2InstantDuration = csaParameters.getCraApplicationWindow().get(CsaProfileConstants.CURATIVE_2_INSTANT);
        curative3InstantDuration = csaParameters.getCraApplicationWindow().get(CsaProfileConstants.CURATIVE_3_INSTANT);
    }

    public Set<String> getTsosWhichDoNotUsePatlInFinalState() {
        return tsosWhichDoNotUsePatlInFinalState;
    }

    // CSA CRAC Creation Parameters checking

    private static void checkCsaExtension(CsaCracCreationParameters csaParameters) {
        if (csaParameters == null) {
            throw new OpenRaoException("No CsaCracCreatorParameters extension provided.");
        }
    }

    private void checkUsePatlInFinalStateMap(CsaCracCreationParameters csaParameters) {
        Map<String, Boolean> usePatlInFinalState = csaParameters.getUsePatlInFinalState();
        for (String tso : tsos) {
            if (!usePatlInFinalState.containsKey(tso)) {
                throw new OpenRaoException("use-patl-in-final-state map is missing \"" + tso + "\" key.");
            }
        }
    }

    private void checkCraApplicationWindowMap(CsaCracCreationParameters csaParameters) {
        Map<String, Integer> craApplicationWindow = csaParameters.getCraApplicationWindow();
        for (String instant : instants) {
            if (!craApplicationWindow.containsKey(instant)) {
                throw new OpenRaoException("cra-application-window map is missing \"" + instant + "\" key.");
            }
        }
        if (craApplicationWindow.get(CsaProfileConstants.CURATIVE_1_INSTANT) >= craApplicationWindow.get(CsaProfileConstants.CURATIVE_2_INSTANT)) {
            throw new OpenRaoException("The TATL acceptable duration for %s cannot be longer than the acceptable duration for %s.".formatted(CsaProfileConstants.CURATIVE_1_INSTANT, CsaProfileConstants.CURATIVE_2_INSTANT));
        }
        if (craApplicationWindow.get(CsaProfileConstants.CURATIVE_2_INSTANT) >= craApplicationWindow.get(CsaProfileConstants.CURATIVE_3_INSTANT)) {
            throw new OpenRaoException("The TATL acceptable duration for %s cannot be longer than the acceptable duration for %s.".formatted(CsaProfileConstants.CURATIVE_2_INSTANT, CsaProfileConstants.CURATIVE_3_INSTANT));
        }
    }

    // Instant to limits mapping

    Set<Integer> getAllTatlDurationsOnSide(Branch<?> branch, TwoSides side) {
        return branch.getCurrentLimits(side).map(limits -> limits.getTemporaryLimits().stream().map(LoadingLimits.TemporaryLimit::getAcceptableDuration).collect(Collectors.toSet())).orElseGet(Set::of);
    }

    public Map<String, Integer> mapPostContingencyInstantsAndLimitDurations(Branch<?> branch, TwoSides side, String tso) {
        Map<String, Integer> instantToLimit = new HashMap<>();
        boolean doNotUsePatlInFinalState = tsosWhichDoNotUsePatlInFinalState.contains(tso);
        Set<Integer> tatlDurations = getAllTatlDurationsOnSide(branch, side);
        // raise exception if a TSO not using the PATL has no TATL either
        // associate instant to TATL duration, or Integer.MAX_VALUE if PATL
        int longestDuration = doNotUsePatlInFinalState ? tatlDurations.stream().max(Integer::compareTo).orElse(Integer.MAX_VALUE) : Integer.MAX_VALUE; // longest TATL duration or infinite (PATL)
        instantToLimit.put(CsaProfileConstants.OUTAGE_INSTANT, tatlDurations.stream().filter(tatlDuration -> tatlDuration >= 0 && tatlDuration < curative1InstantDuration).max(Integer::compareTo).orElse(getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, 0, longestDuration)));
        instantToLimit.put(CsaProfileConstants.AUTO_INSTANT, getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, curative1InstantDuration, longestDuration));
        instantToLimit.put(CsaProfileConstants.CURATIVE_1_INSTANT, getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, curative2InstantDuration, longestDuration));
        instantToLimit.put(CsaProfileConstants.CURATIVE_2_INSTANT, getShortestTatlWithDurationGreaterThanOrReturn(tatlDurations, curative3InstantDuration, longestDuration));
        instantToLimit.put(CsaProfileConstants.CURATIVE_3_INSTANT, longestDuration);
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
