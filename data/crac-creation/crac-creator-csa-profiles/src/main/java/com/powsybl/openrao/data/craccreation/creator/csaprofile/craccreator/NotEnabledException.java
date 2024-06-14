package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;

public class NotEnabledException extends OpenRaoImportException {

    public NotEnabledException(ImportStatus importStatus, String msg) {
        super(importStatus, msg);
    }
}
