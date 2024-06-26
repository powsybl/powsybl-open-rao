package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.FRM;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.RELIABILITY_MARGIN;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.getPrimaryVersionNumber;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.getSubVersionNumber;

public final class CnecDeserializerUtils {
    private CnecDeserializerUtils() {
    }

    public static void checkFrm(String version) {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 3) {
            throw new OpenRaoException(String.format("Unexpected field for version %s : %s", version, FRM));
        }
    }

    public static void checkReliabilityMargin(String version) {
        //"frm" renamed to "reliabilityMargin" in 1.4 and abandoned in 1.5
        int primaryVersionNumber = getPrimaryVersionNumber(version);
        int subVersionNumber = getSubVersionNumber(version);
        if (primaryVersionNumber <= 1 && subVersionNumber <= 3 || primaryVersionNumber >= 2 && subVersionNumber >= 5) {
            throw new OpenRaoException(String.format("Unexpected field for version %s : %s", version, RELIABILITY_MARGIN));
        }
    }
}
