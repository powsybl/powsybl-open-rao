/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.afs.crac_file.xlsx;

import com.powsybl.afs.local.storage.LocalFile;
import com.powsybl.afs.storage.NodeGenericMetadata;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.timeseries.DoubleArrayChunk;
import com.powsybl.timeseries.StringArrayChunk;
import com.powsybl.timeseries.TimeSeriesMetadata;


import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AfsXlsxCracLocalFile implements LocalFile {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.AfsCracLocalFile");

    private final Path file;

    public AfsXlsxCracLocalFile(Path file) {
        this.file = file;
    }

    @Override
    public String getPseudoClass() {
        return AfsXlsxCracFile.PSEUDO_CLASS;
    }

    @Override
    public String getDescription() {
        return RESOURCE_BUNDLE.getString("CracFile");
    }

    @Override
    public NodeGenericMetadata getGenericMetadata() {
        return new NodeGenericMetadata();
    }

    @Override
    public Optional<InputStream> readBinaryData(String s) {
        try {
            return Optional.of(Files.newInputStream(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean dataExists(String s) {
        throw new AssertionError();
    }

    @Override
    public Set<String> getDataNames() {
        throw new AssertionError();
    }

    @Override
    public Set<String> getTimeSeriesNames() {
        throw new AssertionError();
    }

    @Override
    public boolean timeSeriesExists(String s) {
        throw new AssertionError();
    }

    @Override
    public List<TimeSeriesMetadata> getTimeSeriesMetadata(Set<String> set) {
        throw new AssertionError();
    }

    @Override
    public Set<Integer> getTimeSeriesDataVersions() {
        throw new AssertionError();
    }

    @Override
    public Set<Integer> getTimeSeriesDataVersions(String s) {
        throw new AssertionError();
    }

    @Override
    public Map<String, List<DoubleArrayChunk>> getDoubleTimeSeriesData(Set<String> set, int i) {
        throw new AssertionError();
    }

    @Override
    public Map<String, List<StringArrayChunk>> getStringTimeSeriesData(Set<String> set, int i) {
        throw new AssertionError();
    }

    @Override
    public String getName() {
        return DataSourceUtil.getBaseName(file);
    }

    @Override
    public Optional<Path> getParentPath() {
        return Optional.ofNullable(file.getParent());
    }
}
