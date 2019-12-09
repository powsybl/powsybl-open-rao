package com.farao_community.farao.closed_optimisation_rao.json;

import com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoParameters;
import com.farao_community.farao.ra_optimisation.RaoComputationParameters;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationParameters;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class JsonClosedOptimisationRaoParametersTest {
    private RaoComputationParameters parameters;

    @Before
    public void setUp() {
        parameters = new RaoComputationParameters();
        ClosedOptimisationRaoParameters closedParameters = new ClosedOptimisationRaoParameters();
        closedParameters.setNumberOfParallelThreads(12);
        closedParameters.setRelativeMipGap(0.25);
        closedParameters.setSolverType("TEST");
        closedParameters.setMaxTimeInSeconds(3600);
        parameters.addExtension(ClosedOptimisationRaoParameters.class, closedParameters);
    }

    @Test
    public void shouldImportAValidParametersFileAndHaveCorrectsValues() {
        RaoComputationParameters importedParameters = JsonRaoComputationParameters.read(getClass().getResourceAsStream("/json/validParameters.json"));
        ClosedOptimisationRaoParameters closedImportedParameters = importedParameters.getExtension(ClosedOptimisationRaoParameters.class);
        assertThat(closedImportedParameters, is(notNullValue()));
        assertThat(closedImportedParameters.getNumberOfParallelThreads(), is(equalTo(12)));
        assertThat(closedImportedParameters.getRelativeMipGap(), is(equalTo(0.25)));
        assertThat(closedImportedParameters.getSolverType(), is(equalTo("TEST")));
        assertThat(closedImportedParameters.getMaxTimeInSeconds(), is(closeTo(3600, 1e-3)));
    }

    @Test
    public void shouldExportParametersInAValidFormat() throws IOException {
        String expectedParameters;
        try (InputStream is = getClass().getResourceAsStream("/json/validParameters.json")) {
            expectedParameters = IOUtils.toString(is, "UTF-8");
        }
        String actualParameters;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonRaoComputationParameters.write(parameters, baos);
            actualParameters = new String(baos.toByteArray());
        }
        assertThat(actualParameters, is(equalTo(expectedParameters)));
    }
}
