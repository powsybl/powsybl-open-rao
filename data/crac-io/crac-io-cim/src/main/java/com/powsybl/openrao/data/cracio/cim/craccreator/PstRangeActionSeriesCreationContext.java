package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;

import java.util.Set;

public final class PstRangeActionSeriesCreationContext extends RemedialActionSeriesCreationContext {
    private final String networkElementNativeMrid;
    private final String networkElementNativeName;

    private PstRangeActionSeriesCreationContext(String nativeId, Set<String> createdIds, String networkElementNativeMrid, String networkElementNativeName,
                                                ImportStatus importStatus, boolean isAltered, boolean isInverted, String importStatusDetail) {
        super(nativeId, createdIds, importStatus, isAltered, isInverted, importStatusDetail);
        this.networkElementNativeMrid = networkElementNativeMrid;
        this.networkElementNativeName = networkElementNativeName;
    }

    public static PstRangeActionSeriesCreationContext notImported(String nativeId, ImportStatus importStatus, String importStatusDetail) {
        return new PstRangeActionSeriesCreationContext(nativeId, Set.of(nativeId), null, null, importStatus, false, false, importStatusDetail);
    }

    public static PstRangeActionSeriesCreationContext imported(String nativeId, String networkElementNativeMrid, String networkElementNativeName, boolean isAltered, String importStatusDetail) {
        return new PstRangeActionSeriesCreationContext(nativeId, Set.of(nativeId), networkElementNativeMrid, networkElementNativeName, ImportStatus.IMPORTED, isAltered, false, importStatusDetail);
    }

    public String getNetworkElementNativeMrid() {
        return networkElementNativeMrid;
    }

    public String getNetworkElementNativeName() {
        return networkElementNativeName;
    }
}
