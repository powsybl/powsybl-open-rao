/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.nc;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.io.Exporter;
import com.powsybl.openrao.data.raoresult.io.nc.profiles.RemedialActionScheduleProfile;
import org.apache.commons.lang3.NotImplementedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;

/**
 * RAO Result exporter in ENTSO-E's standard Network Code profiles.
 * The Remedial Action Schedule (RAS) profile is always exported.
 * Other profiles may be exported if stated in the export properties.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(Exporter.class)
public class NcExporter implements Exporter {
    @Override
    public String getFormat() {
        return "NC";
    }

    @Override
    public Set<String> getRequiredProperties() {
        return Set.of();
    }

    @Override
    public Class<? extends CracCreationContext> getCracCreationContextClass() {
        return null;
    }

    @Override
    public void exportData(RaoResult raoResult, CracCreationContext cracCreationContext, Properties properties, OutputStream outputStream) {
        if (cracCreationContext instanceof NcCracCreationContext ncCracCreationContext) {
            OffsetDateTime timeStamp = ncCracCreationContext.getTimeStamp();
            new RemedialActionScheduleProfile().fill(timeStamp, raoResult, ncCracCreationContext).write(outputStream);
        } else {
            throw new OpenRaoException("CRAC Creation Context is not NC-compliant.");
        }
    }

    @Override
    public void exportData(RaoResult raoResult, Crac crac, Properties properties, OutputStream outputStream) {
        throw new NotImplementedException("CracCreationContext is required for NC export.");
    }
}
