/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx;

import com.farao_community.farao.data.crac_file.xlsx.model.TimesSeries;
import com.farao_community.farao.data.crac_file.xlsx.handler.EntityHandler;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * Main API
 */
public class ExcelReader {

    protected ExcelReader() {
    }

    public static <T> ReaderBuilder of(Class<T> clazz) {
        return new ReaderBuilder<>(clazz);
    }

    public static final class ReaderBuilder<T> {
        private final Class<T> clazz;
        private InputStream inputStream;
        private String sheetName;
        private boolean skipHeaderRow = false;
        private TimesSeries timesSeries;

        public ReaderBuilder(Class<T> clazz) {
            this.clazz = clazz;
        }

        public ReaderBuilder from(InputStream inputStream) {
            Objects.requireNonNull(inputStream);
            this.inputStream = inputStream;
            return this;
        }

        public ReaderBuilder sheet(String sheetName) {
            Objects.requireNonNull(sheetName);
            this.sheetName = sheetName;
            return this;
        }

        public ReaderBuilder skipHeaderRow(boolean value) {
            this.skipHeaderRow = value;
            return this;
        }

        public ReaderBuilder timesSeries(TimesSeries timesSeries) {
            this.timesSeries = timesSeries;
            return this;
        }

        public <T> List<T> list() {
            EntityHandler<T> entityHandler = EntityHandler.builder().clazz((Class) clazz).sheetName(sheetName).skipHeaderRow(skipHeaderRow).timesSeries(timesSeries).build();
            entityHandler.process(inputStream);
            return entityHandler.readAsList();
        }
    }
}
