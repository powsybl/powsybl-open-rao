/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.service;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.xlsx.ExcelReader;
import com.farao_community.farao.data.crac_file.xlsx.model.ContingencyElementXlsx;
import com.farao_community.farao.data.crac_file.xlsx.model.MonitoredBranchXlsx;
import io.vavr.control.Validation;
import org.apache.poi.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class CracFileScanner {

    public void checkCracFileSheet(InputStream inputStream) {
        byte[] bytes = new byte[0];
        try {
            bytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new FaraoException(e);
        }

        Validation.valid(ExcelReader.of(MonitoredBranchXlsx.class)
                .from(new ByteArrayInputStream(bytes))
                .skipHeaderRow(true)
                .sheet("Branch_CBCO")
                .list());

        Validation.valid(ExcelReader.of(ContingencyElementXlsx.class)
                .from(new ByteArrayInputStream(bytes))
                .skipHeaderRow(true)
                .sheet("Branch_CO")
                .list());
    }
}
