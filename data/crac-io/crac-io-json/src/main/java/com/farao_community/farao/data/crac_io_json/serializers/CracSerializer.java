package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationNames.*;

public class CracSerializer {

    public static String serializeCrac(Crac crac) {
        return buildJsonCrac(crac).toString(1);
    }

    private static JSONObject buildJsonCrac(Crac crac) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(NAME, crac.getName());
        jsonObject.put(NETWORK_ELEMENTS, serializeNetworkElements(crac));
        jsonObject.put(FLOW_CNECS, serializeFlowCnecs(crac));
        return jsonObject;
    }

    private static JSONArray serializeNetworkElements(Crac crac) {
        // cheat : using crac.getNetworkElements() for now, which may become invisible
        JSONArray array = new JSONArray();
        crac.getNetworkElements().forEach(networkElement -> array.put(serializeNetworkElement(networkElement)));
        return array;
    }

    private static JSONObject serializeNetworkElement(NetworkElement networkElement) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ID, networkElement.getId());
        jsonObject.put(NAME, networkElement.getName());
        return jsonObject;
    }

    private static JSONArray serializeFlowCnecs(Crac crac) {
        JSONArray array = new JSONArray();
        crac.getFlowCnecs().forEach(flowCnec -> array.put(serializeFlowCnec(flowCnec)));
        return array;
    }

    private static JSONObject serializeFlowCnec(FlowCnec flowCnec) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ID, flowCnec.getId());
        jsonObject.put(NAME, flowCnec.getName());
        jsonObject.put(NETWORK_ELEMENT_ID, flowCnec.getNetworkElement().getId());
        jsonObject.put(OPERATOR, flowCnec.getOperator());
        jsonObject.put(INSTANT, serializeInstant(flowCnec.getState().getInstant()));
        if (flowCnec.getState().getContingency().isPresent()) {
            jsonObject.put(CONTINGENCY_ID, flowCnec.getState().getContingency().orElseThrow().getId());
        }
        jsonObject.put(OPTIMIZED, flowCnec.isOptimized());
        jsonObject.put(MONITORED, flowCnec.isMonitored());
        jsonObject.put(FRM, flowCnec.getReliabilityMargin());
        jsonObject.put(THRESHOLDS, serializeThresholds(flowCnec));
        return jsonObject;
    }

    private static String serializeInstant(Instant instant) {
        switch (instant) {
            case PREVENTIVE:
                return PREVENTIVE_INSTANT;
            case OUTAGE:
                return OUTAGE_INSTANT;
            case AUTO:
                return  AUTO_INSTANT;
            case CURATIVE:
                return CURATIVE_INSTANT;
            default:
                throw new FaraoException(String.format("Unsupported instant %s", instant));
        }
    }

    private static JSONObject serializeThresholds(FlowCnec flowCnec) {
        return null; // todo
    }
}
