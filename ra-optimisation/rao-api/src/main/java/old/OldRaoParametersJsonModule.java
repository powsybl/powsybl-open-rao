/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package old;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class OldRaoParametersJsonModule extends SimpleModule {

    public OldRaoParametersJsonModule() {
        addDeserializer(OldRaoParameters.class, new OldRaoParametersDeserializer());
        addSerializer(OldRaoParameters.class, new OldRaoParametersSerializer());
    }
}
