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
import static org.junit.Assert.fail;

public class CsvFlowBasedComputationResultTest {

    private CracFile cracFile;

    @Before
    public void setUp()  {
        // get cracFile
        InputStream isCrac = getClass().getResourceAsStream("/CsvExportTestCracDataFlowBased.json");
        cracFile = JsonCracFile.read(isCrac);
    }

    @Test
    public void testExportCsvOk() throws IOException {

        // get flow-based computation results
        InputStream isRes = getClass().getResourceAsStream("/CsvExportTestOutputFlowBased.json");
        FlowBasedComputationResult results = JsonFlowBasedComputationResult.read(isRes);

        //get expected csv export
        String expectedResult = new String(Files.toByteArray(new File(getClass().getResource("/CsvExportTestExpectedResults.csv").getFile())));

        OutputStream os = new ByteArrayOutputStream();
        CsvFlowBasedComputationResult.write(results, cracFile, os);
        assertEquals(normalizeLineEnds(expectedResult), normalizeLineEnds(os.toString()));
    }

    @Test
    public void testExportCsvBranchNotFound() {
        // get flow-based computation results
        InputStream isRes = getClass().getResourceAsStream("/CsvExportTestOutputFlowBasedMissingBranch.json");
        FlowBasedComputationResult results = JsonFlowBasedComputationResult.read(isRes);

        OutputStream os = new ByteArrayOutputStream();
        try {
            CsvFlowBasedComputationResult.write(results, cracFile, os);
            fail();
        } catch (IllegalArgumentException e) {
            // should throw
        }
    }

    @Test
    public void testExportCsvPtdfNotFound() {
        // get flow-based computation results
        InputStream isRes = getClass().getResourceAsStream("/CsvExportTestOutputFlowBasedMissingPtdf.json");
        FlowBasedComputationResult results = JsonFlowBasedComputationResult.read(isRes);

        OutputStream os = new ByteArrayOutputStream();
        try {
            CsvFlowBasedComputationResult.write(results, cracFile, os);
            fail();
        } catch (IllegalArgumentException e) {
            // should throw
        }
    }

    private String normalizeLineEnds(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }

}
