/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.idcc.core;

import com.powsybl.openrao.commons.OpenRaoException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.OffsetDateTime;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class XmlOutputsUtil {

    private XmlOutputsUtil() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static XMLGregorianCalendar getXMLGregorianCurrentTime() {
        XMLGregorianCalendar xmlGregorianCalendar;
        try {
            xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(OffsetDateTime.now().toString());
            xmlGregorianCalendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
            xmlGregorianCalendar.setTimezone(0);
        } catch (DatatypeConfigurationException e) {
            throw new OpenRaoException("Impossible to create XmlGregorianCalendar current time", e);
        }
        return xmlGregorianCalendar;
    }
}
