package com.farao_community.farao.flowbased_computation.csv_exporter;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class CsvFlowBasedComputationResult {

    private CsvFlowBasedComputationResult() {
        throw new IllegalStateException("Utility class");
    }

    public static void write(FlowBasedComputationResult result, CracFile cracFile, OutputStream os) {

        CsvFlowBasedComputationResultPrinter csvFlowBasedComputationResultPrinter = new CsvFlowBasedComputationResultPrinter(result, cracFile);
        try {
            CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(os), CSVFormat.EXCEL);
            csvFlowBasedComputationResultPrinter.export(csvPrinter);
            csvPrinter.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(FlowBasedComputationResult result, CracFile cracFile, Path csvFile) {
        Objects.requireNonNull(csvFile);
        try (OutputStream outputStream = Files.newOutputStream(csvFile)) {
            write(result, cracFile, outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
