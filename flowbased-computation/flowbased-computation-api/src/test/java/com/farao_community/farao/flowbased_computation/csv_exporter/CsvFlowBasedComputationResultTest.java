package com.farao_community.farao.flowbased_computation.csv_exporter;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.flowbased_computation.json.JsonFlowBasedComputationResult;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class CsvFlowBasedComputationResultTest {

    private FlowBasedComputationResult results;
    private CracFile cracFile;

    @Before
    public void setUp() {
        // get results
        InputStream isRes = getClass().getResourceAsStream("/outputflowbased");
        results = JsonFlowBasedComputationResult.read(isRes);

        // get cracFile
        InputStream isCrac = getClass().getResourceAsStream("/cracDataFlowBased.json");
        cracFile = JsonCracFile.read(isCrac);
    }

    @Test
    public void testExportCsvOk() {
        OutputStream os = new ByteArrayOutputStream();
        CsvFlowBasedComputationResult.write(results, cracFile, os);

        System.out.println("fin");
    }
}
