package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters.CsaCracCreationParameters;

import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.CURATIVE_1_INSTANT;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.CURATIVE_2_INSTANT;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.CURATIVE_3_INSTANT;

class FlowCnecInstantHelper {

    private final Set<String> tsos = Set.of("REE", "REN", "RTE");
    private final Set<String> instants = Set.of(CURATIVE_1_INSTANT, CURATIVE_2_INSTANT, CURATIVE_3_INSTANT);

    void checkCracCreationParameters(CracCreationParameters parameters) {
        CsaCracCreationParameters csaParameters = parameters.getExtension(CsaCracCreationParameters.class);
        checkCsaExtension(csaParameters);
        checkUsePatlInFinalStateMap(csaParameters);
        checkCraApplicationWindowMap(csaParameters);
    }

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
        if (craApplicationWindow.get(CURATIVE_1_INSTANT) > craApplicationWindow.get(CURATIVE_2_INSTANT)) {
            throw new OpenRaoException("The TATL acceptable duration for %s cannot be longer than the acceptable duration for %s.".formatted(CURATIVE_1_INSTANT, CURATIVE_2_INSTANT));
        }
        if (craApplicationWindow.get(CURATIVE_2_INSTANT) > craApplicationWindow.get(CURATIVE_3_INSTANT)) {
            throw new OpenRaoException("The TATL acceptable duration for %s cannot be longer than the acceptable duration for %s.".formatted(CURATIVE_2_INSTANT, CURATIVE_3_INSTANT));
        }
    }
}
