package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.parameters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CsaCracCreationParametersTest {
    @Test
    void setAttributes() {
        CsaCracCreationParameters parameters = new CsaCracCreationParameters();
        assertEquals("CsaCracCreatorParameters", parameters.getName());
        assertFalse(parameters.getUseCnecGeographicalFilter());
        parameters.setUseCnecGeographicalFilter(true);
        assertTrue(parameters.getUseCnecGeographicalFilter());
    }
}
