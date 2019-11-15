/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
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
