package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.cnec;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.CurrentLimits;
import com.powsybl.iidm.network.LoadingLimits;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracio.csaprofiles.parameters.CsaCracCreationParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowCnecInstantHelperTest {

    private LoadingLimits.TemporaryLimit tatl0;
    private LoadingLimits.TemporaryLimit tatl182;
    private LoadingLimits.TemporaryLimit tatl300;
    private LoadingLimits.TemporaryLimit tatl600;
    private LoadingLimits.TemporaryLimit tatl900;
    private LoadingLimits.TemporaryLimit tatl1200;
    private CracCreationParameters parameters;
    private CsaCracCreationParameters csaParameters;
    private FlowCnecInstantHelper helper;

    @BeforeEach
    void setUp() {
        initTatls();
        initCracCreationParameters();
        helper = new FlowCnecInstantHelper(parameters);
    }

    @Test
    void getTsosWhichDoNotUsePatlInFinalState() {
        assertEquals(Set.of("REE"), helper.getTsosWhichDoNotUsePatlInFinalState());
    }

    @Test
    void checkCracCreationParametersWithoutCsaExtension() {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new FlowCnecInstantHelper(cracCreationParameters));
        assertEquals("No CsaCracCreatorParameters extension provided.", exception.getMessage());
    }

    @Test
    void checkCracCreationParametersWithMissingTso() {
        csaParameters.setUsePatlInFinalState(Map.of("REE", false, "REN", true, "ELIA", true));
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new FlowCnecInstantHelper(parameters));
        assertEquals("use-patl-in-final-state map is missing \"RTE\" key.", exception.getMessage());
    }

    @Test
    void checkCracCreationParametersWithMissingInstant() {
        csaParameters.setCraApplicationWindow(Map.of("curative 1", 300, "curative 2", 600, "preventive", 0));
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new FlowCnecInstantHelper(parameters));
        assertEquals("cra-application-window map is missing \"curative 3\" key.", exception.getMessage());
    }

    @Test
    void checkCracCreationParametersWithCurative1LongerThanCurative2() {
        csaParameters.setCraApplicationWindow(Map.of("curative 1", 600, "curative 2", 300, "curative 3", 1200));
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new FlowCnecInstantHelper(parameters));
        assertEquals("The TATL acceptable duration for curative 1 cannot be longer than the acceptable duration for curative 2.", exception.getMessage());
    }

    @Test
    void checkCracCreationParametersWithCurative2LongerThanCurative3() {
        csaParameters.setCraApplicationWindow(Map.of("curative 1", 300, "curative 2", 1200, "curative 3", 600));
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new FlowCnecInstantHelper(parameters));
        assertEquals("The TATL acceptable duration for curative 2 cannot be longer than the acceptable duration for curative 3.", exception.getMessage());
    }

    @Test
    void getAllTatlDurationsOnSide() {
        Branch<?> branch = Mockito.mock(Branch.class);

        // No TATL on side ONE / TATLs on side TWO
        CurrentLimits currentLimitsRight = Mockito.mock(CurrentLimits.class);
        Mockito.when(currentLimitsRight.getTemporaryLimits()).thenReturn(mockBranchTatls(true, true, true, true, true, true));
        Mockito.when(branch.getCurrentLimits(TwoSides.ONE)).thenReturn(Optional.empty());
        Mockito.when(branch.getCurrentLimits(TwoSides.TWO)).thenReturn(Optional.of(currentLimitsRight));

        assertTrue(helper.getAllTatlDurationsOnSide(branch, TwoSides.ONE).isEmpty());
        assertEquals(Set.of(0, 182, 300, 600, 900, 1200), helper.getAllTatlDurationsOnSide(branch, TwoSides.TWO));
    }

    @Test
    void mapPostContingencyInstantsAndLimitDurationsAndUsePatlInFinalState() {
        assertEquals(buildInstantDurationMap(182, 300, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, true, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, true, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, true, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, true, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 900, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, false, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 900, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, false, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 1200, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, false, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, false, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, true, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, true, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, true, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, true, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 900, 900, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, false, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 900, 900, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, false, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 1200, 1200, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, false, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, false, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 300, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, true, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 300, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, true, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 300, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, true, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 300, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, true, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 300, 900, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, false, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 300, 900, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, false, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 300, 1200, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, false, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 300, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, false, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 600, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, true, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 600, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, true, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 600, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, true, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 600, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, true, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 900, 900, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, false, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 900, 900, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, false, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, 1200, 1200, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, false, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, false, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, true, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, true, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, true, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, true, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 900, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, false, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 900, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, false, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, 1200, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, false, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 300, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, false, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, true, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, true, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, true, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, true, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 900, 900, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, false, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 900, 900, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, false, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, 1200, 1200, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, false, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(182, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, false, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(300, 300, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, true, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(300, 300, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, true, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(300, 300, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, true, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(300, 300, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, true, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(300, 300, 900, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, false, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(300, 300, 900, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, false, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(300, 300, 1200, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, false, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(300, 300, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, false, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(600, 600, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, true, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(600, 600, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, true, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(600, 600, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, true, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(600, 600, 600, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, true, false, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(900, 900, 900, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, false, true, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(900, 900, 900, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, false, true, false), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(1200, 1200, 1200, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, false, false, true), TwoSides.TWO, "RTE"));
        assertEquals(buildInstantDurationMap(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, false, false, false), TwoSides.TWO, "RTE"));
    }

    @Test
    void mapPostContingencyInstantsAndLimitDurationsAndDoNotUsePatlInFinalState() {
        assertEquals(buildInstantDurationMap(182, 300, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, true, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, true, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, true, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, 600, 600), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, true, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 900, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, false, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 900, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, false, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 1200, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, false, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 300, 300, 300), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, true, false, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, true, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, true, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, true, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 600, 600), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, true, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 900, 900, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, false, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 900, 900, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, false, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 1200, 1200, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, false, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 182, 182, 182, 182), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, true, false, false, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 300, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, true, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 300, 600, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, true, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 300, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, true, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 300, 600, 600, 600), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, true, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 300, 900, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, false, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 300, 900, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, false, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 300, 1200, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, false, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 300, 300, 300, 300), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, true, false, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 600, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, true, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 600, 600, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, true, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 600, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, true, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 600, 600, 600, 600), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, true, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 900, 900, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, false, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 900, 900, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, false, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 1200, 1200, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, false, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(0, 0, 0, 0, 0), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, true, false, false, false, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, true, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, true, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, true, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 600, 600, 600), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, true, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 900, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, false, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 900, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, false, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 1200, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, false, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 300, 300, 300, 300), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, true, false, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, true, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, true, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, true, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 600, 600, 600, 600), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, true, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 900, 900, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, false, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 900, 900, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, false, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 1200, 1200, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, false, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(182, 182, 182, 182, 182), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, false, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(300, 300, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, true, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(300, 300, 600, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, true, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(300, 300, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, true, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(300, 300, 600, 600, 600), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, true, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(300, 300, 900, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, false, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(300, 300, 900, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, false, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(300, 300, 1200, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, false, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(300, 300, 300, 300, 300), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, false, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(600, 600, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, true, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(600, 600, 600, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, true, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(600, 600, 600, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, true, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(600, 600, 600, 600, 600), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, true, false, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(900, 900, 900, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, false, true, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(900, 900, 900, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, false, true, false), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(1200, 1200, 1200, 1200, 1200), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, false, false, true), TwoSides.TWO, "REE"));
        assertEquals(buildInstantDurationMap(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, false, false, false), TwoSides.TWO, "REE"));
    }

    @Test
    void tatlToInstantMapWithRealData() {
        // REE
        Branch<?> reeBranch = createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, false, true, false);
        assertEquals(buildInstantDurationMap(900, 900, 900, 900, 900), helper.mapPostContingencyInstantsAndLimitDurations(reeBranch, TwoSides.TWO, "REE"));
        // REN
        Branch<?> renBranch = createBranchWithTatlsOnSide(TwoSides.TWO, false, false, false, false, true, false);
        assertEquals(buildInstantDurationMap(900, 900, 900, Integer.MAX_VALUE, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(renBranch, TwoSides.TWO, "REN"));
        // RTE Type 1
        Branch<?> rteBranchType1 = createBranchWithTatlsOnSide(TwoSides.TWO, false, true, false, true, false, true);
        assertEquals(buildInstantDurationMap(182, 600, 600, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(rteBranchType1, TwoSides.TWO, "RTE"));
        // RTE Type 2
        Branch<?> rteBranchType2 = createBranchWithTatlsOnSide(TwoSides.TWO, false, false, true, false, false, true);
        assertEquals(buildInstantDurationMap(300, 300, 1200, 1200, Integer.MAX_VALUE), helper.mapPostContingencyInstantsAndLimitDurations(rteBranchType2, TwoSides.TWO, "RTE"));
    }

    @Test
    void getPostContingencyInstantsAssociatedToLimitsOnRealData() {
        // REE
        Map<String, Integer> reeInstantToDurationMap = buildInstantDurationMap(900, 900, 900, 900, 900);
        assertEquals(Set.of("outage", "auto", "curative 1", "curative 2", "curative 3"), helper.getPostContingencyInstantsAssociatedToLimitDuration(reeInstantToDurationMap, 900));
        assertTrue(helper.getPostContingencyInstantsAssociatedToPatl(reeInstantToDurationMap).isEmpty());
        // REN
        Map<String, Integer> renInstantToDurationMap = buildInstantDurationMap(900, 900, 900, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Set.of("outage", "auto", "curative 1"), helper.getPostContingencyInstantsAssociatedToLimitDuration(renInstantToDurationMap, 900));
        assertEquals(Set.of("curative 2", "curative 3"), helper.getPostContingencyInstantsAssociatedToPatl(renInstantToDurationMap));
        // RTE Type 1
        Map<String, Integer> rteType1InstantToDurationMap = buildInstantDurationMap(182, 600, 600, 1200, Integer.MAX_VALUE);
        assertEquals(Set.of("outage"), helper.getPostContingencyInstantsAssociatedToLimitDuration(rteType1InstantToDurationMap, 0));
        assertEquals(Set.of("outage"), helper.getPostContingencyInstantsAssociatedToLimitDuration(rteType1InstantToDurationMap, 182));
        assertEquals(Set.of("auto", "curative 1"), helper.getPostContingencyInstantsAssociatedToLimitDuration(rteType1InstantToDurationMap, 600));
        assertEquals(Set.of("curative 2"), helper.getPostContingencyInstantsAssociatedToLimitDuration(rteType1InstantToDurationMap, 1200));
        assertEquals(Set.of("curative 3"), helper.getPostContingencyInstantsAssociatedToPatl(rteType1InstantToDurationMap));
        // RTE Type 2
        Map<String, Integer> rteType2InstantToDurationMap = buildInstantDurationMap(300, 300, 1200, 1200, Integer.MAX_VALUE);
        assertEquals(Set.of("outage", "auto"), helper.getPostContingencyInstantsAssociatedToLimitDuration(rteType2InstantToDurationMap, 300));
        assertEquals(Set.of("curative 1", "curative 2"), helper.getPostContingencyInstantsAssociatedToLimitDuration(rteType2InstantToDurationMap, 1200));
        assertEquals(Set.of("curative 3"), helper.getPostContingencyInstantsAssociatedToPatl(rteType2InstantToDurationMap));
    }

    private void initCracCreationParameters() {
        parameters = new CracCreationParameters();
        csaParameters = new CsaCracCreationParameters();
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);
    }

    private void initTatls() {
        tatl0 = Mockito.mock(LoadingLimits.TemporaryLimit.class);
        Mockito.when(tatl0.getAcceptableDuration()).thenReturn(0);
        tatl182 = Mockito.mock(LoadingLimits.TemporaryLimit.class);
        Mockito.when(tatl182.getAcceptableDuration()).thenReturn(182);
        tatl300 = Mockito.mock(LoadingLimits.TemporaryLimit.class);
        Mockito.when(tatl300.getAcceptableDuration()).thenReturn(300);
        tatl600 = Mockito.mock(LoadingLimits.TemporaryLimit.class);
        Mockito.when(tatl600.getAcceptableDuration()).thenReturn(600);
        tatl900 = Mockito.mock(LoadingLimits.TemporaryLimit.class);
        Mockito.when(tatl900.getAcceptableDuration()).thenReturn(900);
        tatl1200 = Mockito.mock(LoadingLimits.TemporaryLimit.class);
        Mockito.when(tatl1200.getAcceptableDuration()).thenReturn(1200);
    }

    private Collection<LoadingLimits.TemporaryLimit> mockBranchTatls(boolean useTatl0, boolean useTatl182, boolean useTatl300, boolean useTatl600, boolean useTatl900, boolean useTatl1200) {
        Collection<LoadingLimits.TemporaryLimit> tatls = new HashSet<>();
        if (useTatl0) {
            tatls.add(tatl0);
        }
        if (useTatl182) {
            tatls.add(tatl182);
        }
        if (useTatl300) {
            tatls.add(tatl300);
        }
        if (useTatl600) {
            tatls.add(tatl600);
        }
        if (useTatl900) {
            tatls.add(tatl900);
        }
        if (useTatl1200) {
            tatls.add(tatl1200);
        }
        return tatls;
    }

    private Branch<?> createBranchWithTatlsOnSide(TwoSides side, boolean useTatl0, boolean useTatl182, boolean useTatl300, boolean useTatl600, boolean useTatl900, boolean useTatl1200) {
        Branch<?> branch = Mockito.mock(Branch.class);
        CurrentLimits currentLimits = Mockito.mock(CurrentLimits.class);
        Mockito.when(currentLimits.getTemporaryLimits()).thenReturn(mockBranchTatls(useTatl0, useTatl182, useTatl300, useTatl600, useTatl900, useTatl1200));
        Mockito.when(branch.getCurrentLimits(side)).thenReturn(Optional.of(currentLimits));
        Mockito.when(branch.getId()).thenReturn("branchId");
        return branch;
    }

    private Map<String, Integer> buildInstantDurationMap(int outageDuration, int autoDuration, int curative1Duration, int curative2Duration, int curative3Duration) {
        return Map.of("outage", outageDuration, "auto", autoDuration, "curative 1", curative1Duration, "curative 2", curative2Duration, "curative 3", curative3Duration);
    }
}
