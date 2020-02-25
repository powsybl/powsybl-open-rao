package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Cnec;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CnecJsonModule extends SimpleModule {

    public CnecJsonModule() {
        addDeserializer(Cnec.class, new CnecDeserializer());
        addSerializer(Cnec.class, new CnecSerializer());
    }
}
