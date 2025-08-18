/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.idcc.core;

import com.powsybl.openrao.commons.OpenRaoException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class JaxbUtil {

    private JaxbUtil() {
        throw new AssertionError("Utility class should not be constructed");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxbUtil.class);

    public static <T> void writeInFile(Class<T> clazz, T type, String filePath) {
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            FileOutputStream fos = new FileOutputStream(filePath);
            marshaller.marshal(type, fos);
        } catch (JAXBException e) {
            String errorMessage = String.format("Error occurred when writing content of object of type %s to bytes", clazz.getName());
            LOGGER.error(errorMessage);
            throw new OpenRaoException(errorMessage, e);
        } catch (FileNotFoundException e) {
            throw new OpenRaoException(e);
        }
    }

}