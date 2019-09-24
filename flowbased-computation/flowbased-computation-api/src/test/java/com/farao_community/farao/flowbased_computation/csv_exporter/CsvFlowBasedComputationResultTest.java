package com.farao_community.farao.flowbased_computation.csv_exporter;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.flowbased_computation.json.JsonFlowBasedComputationResult;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static junit.framework.TestCase.assertEquals;

public class CsvFlowBasedComputationResultTest {

    private FlowBasedComputationResult results;
    private CracFile cracFile;
    private String expectedResult;

    @Before
    public void setUp() throws IOException {
        // get results
        InputStream isRes = getClass().getResourceAsStream("/CsvExportTestOutputFlowBased.json");
        results = JsonFlowBasedComputationResult.read(isRes);

        // get cracFile
        InputStream isCrac = getClass().getResourceAsStream("/CsvExportTestCracDataFlowBased.json");
        cracFile = JsonCracFile.read(isCrac);

        //get expected result
        expectedResult = new String(Files.toByteArray(new File(getClass().getResource("/CsvExportTestExpectedResults.csv").getFile())));
    }

    @Test
    public void testExportCsvOk() {
        OutputStream os = new ByteArrayOutputStream();
        CsvFlowBasedComputationResult.write(results, cracFile, os);
        assertEquals(normalizeLineEnds(expectedResult), normalizeLineEnds(os.toString()));
    }

    private String normalizeLineEnds(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }

}
