package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.openrao.data.crac.api.io.utils.BufferSize;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import java.io.File;
import java.net.URISyntaxException;

public class TestBase {

    public File getResourceAsFile(String file) {
        try {
            var url = getClass().getResource(file);
            if (url == null) {
                throw new RuntimeException("Resource not found on classpath: " + file);
            }
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error loading resource: " + file, e);
        }
    }

    public SafeFileReader getResourceAsReader(String file) {
        return SafeFileReader.create(getResourceAsFile(file), BufferSize.SMALL);
    }

}
