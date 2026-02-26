/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class RaoResultArchiveManager {
    private static final String INDIVIDUAL_RAO_RESULT_NAME_TEMPLATE_PROPERTY = "time-coupled-rao-result.export.filename-template";
    private static final String TIME_COUPLED_RAO_RESULT_SUMMARY_FILENAME_PROPERTY = "time-coupled-rao-result.export.summary-filename";
    private static final String TIME_COUPLED_RAO_RESULT_PREVENTIVE_ONLY = "time-coupled-rao-result.export.preventive-only";
    private static final String DEFAULT_INDIVIDUAL_RAO_RESULT_NAME_TEMPLATE = "'raoResult_'yyyyMMddHHmm'.json'";
    private static final String DEFAULT_TIME_COUPLED_RAO_RESULT_SUMMARY_FILENAME = "timeCoupledRaoSummary.json";

    private RaoResultArchiveManager() {
    }

    // default export format is JSON, see later to set export format
    public static void exportAndZipResults(ZipOutputStream zipOutputStream, TimeCoupledRaoResult timeCoupledRaoResult, TemporalData<Crac> cracs, Properties properties) throws IOException {
        String jsonFileNameTemplate = getIndividualRaoResultFilenameTemplate(properties);
        String summaryFilename = getSummaryFilename(properties);
        for (OffsetDateTime timestamp : timeCoupledRaoResult.getTimestamps()) {
            addRaoResultToZipArchive(timestamp, zipOutputStream, timeCoupledRaoResult.getIndividualRaoResult(timestamp), cracs.getData(timestamp).orElseThrow(), properties, jsonFileNameTemplate);
        }
        List<Instant> instants = cracs.getDataPerTimestamp().values().iterator().next().getSortedInstants();
        addSummaryToZipArchive(zipOutputStream, timeCoupledRaoResult, summaryFilename, jsonFileNameTemplate, instants, exportOnlyPreventiveResults(properties));
        zipOutputStream.close();
    }

    private static void addRaoResultToZipArchive(OffsetDateTime timestamp,
                                                 ZipOutputStream zipOutputStream,
                                                 RaoResult raoResult,
                                                 Crac crac,
                                                 Properties properties,
                                                 String jsonFileNameTemplate) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        raoResult.write("JSON", crac, properties, byteArrayOutputStream);
        addEntryToZipArchive(timestamp.format(DateTimeFormatter.ofPattern(jsonFileNameTemplate)), zipOutputStream, byteArrayOutputStream);
        byteArrayOutputStream.close();
    }

    private static void addSummaryToZipArchive(ZipOutputStream zipOutputStream,
                                               TimeCoupledRaoResult timeCoupledRaoResult,
                                               String summaryFilename,
                                               String jsonFileNameTemplate,
                                               List<Instant> instants,
                                               boolean preventiveOnly) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectMapper objectMapper = JsonUtil.createObjectMapper();
            SimpleModule module = new JsonTimeCoupledRaoResultSerializerModule(jsonFileNameTemplate, preventiveOnly ? List.of(instants.getFirst()) : instants);
            objectMapper.registerModule(module);
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(byteArrayOutputStream, timeCoupledRaoResult);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        addEntryToZipArchive(summaryFilename, zipOutputStream, byteArrayOutputStream);
        byteArrayOutputStream.close();
    }

    private static void addEntryToZipArchive(String entryName, ZipOutputStream zipOutputStream, ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zipOutputStream.putNextEntry(entry);
        addOutputStreamContentToZipAndClose(zipOutputStream, byteArrayOutputStream);
        byteArrayOutputStream.close();
        zipOutputStream.closeEntry();
    }

    private static void addOutputStreamContentToZipAndClose(ZipOutputStream zipOutputStream, ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        byte[] bytes = new byte[1024];
        int length;
        InputStream is = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        while ((length = is.read(bytes)) >= 0) {
            zipOutputStream.write(bytes, 0, length);
        }
        is.close();
    }

    private static String getSummaryFilename(Properties properties) {
        return properties.getProperty(TIME_COUPLED_RAO_RESULT_SUMMARY_FILENAME_PROPERTY, DEFAULT_TIME_COUPLED_RAO_RESULT_SUMMARY_FILENAME);
    }

    private static String getIndividualRaoResultFilenameTemplate(Properties properties) {
        return properties.getProperty(INDIVIDUAL_RAO_RESULT_NAME_TEMPLATE_PROPERTY, DEFAULT_INDIVIDUAL_RAO_RESULT_NAME_TEMPLATE);
    }

    private static boolean exportOnlyPreventiveResults(Properties properties) {
        return Boolean.parseBoolean(properties.getProperty(TIME_COUPLED_RAO_RESULT_PREVENTIVE_ONLY));
    }
}
