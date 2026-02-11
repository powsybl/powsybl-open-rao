/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.io.Importer;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Imports a CRAC by generating one automatically from a Network file
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(Importer.class)
public class NetworkCracImporter implements Importer {
    @Override
    public String getFormat() {
        return "Network";
    }

    @Override
    public boolean exists(String filename, InputStream inputStream) {
        String extension = FilenameUtils.getExtension(filename);
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("temp", "." + extension);
        } catch (IOException e) {
            return false;
        }
        try {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            // com.powsybl.iidm.network.Importer networkImporter = com.powsybl.iidm.network.Importer.find(new ReadOnlyMemDataSource(tempFile.toString()));
            // return networkImporter != null;
            // TODO avoid reading the network
            Network network = Network.read(tempFile);
            return network != null;
        } catch (IOException | PowsyblException e) {
            return false;
        } finally {
            tempFile.toFile().delete();

        }
    }

    @Override
    public CracCreationContext importData(InputStream inputStream, CracCreationParameters cracCreationParameters, Network network) {
        return new NetworkCracCreator().createCrac(network, cracCreationParameters);
    }
}
