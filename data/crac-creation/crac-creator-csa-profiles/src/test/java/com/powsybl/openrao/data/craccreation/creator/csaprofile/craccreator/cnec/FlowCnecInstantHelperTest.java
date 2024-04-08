package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters.CsaCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlowCnecInstantHelperTest {
    @Test
    void checkCracCreationParametersWithoutCsaExtension() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new FlowCnecInstantHelper().checkCracCreationParameters(new CracCreationParameters()));
        assertEquals("No CsaCracCreatorParameters extension provided.", exception.getMessage());
    }

    @Test
    void checkCracCreationParametersWithMissingTso() {
        CracCreationParameters parameters = new CracCreationParameters();
        CsaCracCreationParameters csaParameters = new CsaCracCreationParameters();
        csaParameters.setUsePatlInFinalState(Map.of("REE", false, "REN", true, "ELIA", true));
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new FlowCnecInstantHelper().checkCracCreationParameters(parameters));
        assertEquals("use-patl-in-final-state map is missing \"RTE\" key.", exception.getMessage());
    }

    @Test
    void checkCracCreationParametersWithMissingInstant() {
        CracCreationParameters parameters = new CracCreationParameters();
        CsaCracCreationParameters csaParameters = new CsaCracCreationParameters();
        csaParameters.setCraApplicationWindow(Map.of("curative 1", 300, "curative 2", 600, "preventive", 0));
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new FlowCnecInstantHelper().checkCracCreationParameters(parameters));
        assertEquals("cra-application-window map is missing \"curative 3\" key.", exception.getMessage());
    }

    @Test
    void checkCracCreationParametersWithCurative1LongerThanCurative2() {
        CracCreationParameters parameters = new CracCreationParameters();
        CsaCracCreationParameters csaParameters = new CsaCracCreationParameters();
        csaParameters.setCraApplicationWindow(Map.of("curative 1", 600, "curative 2", 300, "curative 3", 1200));
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new FlowCnecInstantHelper().checkCracCreationParameters(parameters));
        assertEquals("The TATL acceptable duration for curative 1 cannot be longer than the acceptable duration for curative 2.", exception.getMessage());
    }

    @Test
    void checkCracCreationParametersWithCurative2LongerThanCurative3() {
        CracCreationParameters parameters = new CracCreationParameters();
        CsaCracCreationParameters csaParameters = new CsaCracCreationParameters();
        csaParameters.setCraApplicationWindow(Map.of("curative 1", 300, "curative 2", 1200, "curative 3", 600));
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new FlowCnecInstantHelper().checkCracCreationParameters(parameters));
        assertEquals("The TATL acceptable duration for curative 2 cannot be longer than the acceptable duration for curative 3.", exception.getMessage());
    }
}
