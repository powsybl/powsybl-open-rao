package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SimpleCracDeserializer extends StdDeserializer<SimpleCrac> {

    public SimpleCracDeserializer() {
        this(null);
    }

    public SimpleCracDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SimpleCrac deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        SimpleCrac simpleCrac = new SimpleCrac(node.get("id").asText(), node.get("name").asText());
        for (Iterator<JsonNode> it = node.get("networkElements").elements(); it.hasNext(); ) {
            JsonNode networkElement = it.next();
            simpleCrac.addNetworkElement(new NetworkElement(networkElement.get("id").asText(), networkElement.get("name").asText()));
        }
        for (Iterator<JsonNode> it = node.get("instants").elements(); it.hasNext(); ) {
            JsonNode instant = it.next();
            simpleCrac.addInstant(new Instant(instant.get("id").asText(), instant.get("seconds").asInt()));
        }
        for (Iterator<JsonNode> it = node.get("contingencies").elements(); it.hasNext(); ) {
            JsonNode contingency = it.next();
            Set<NetworkElement> networkElements = new HashSet<>();
            for (Iterator<JsonNode> itNE = contingency.get("networkElements").elements(); itNE.hasNext(); ) {
                JsonNode networkElement = itNE.next();
                networkElements.add(simpleCrac.getNetworkElement(networkElement.asText()));
            }
            simpleCrac.addContingency(new ComplexContingency(contingency.get("id").asText(), contingency.get("name").asText(), networkElements));
        }
        for (Iterator<JsonNode> it = node.get("states").elements(); it.hasNext(); ) {
            JsonNode state = it.next();
            Optional<Contingency> contingency;
            if (state.get("contingency").isNull()) {
                contingency = Optional.empty();
            } else {
                contingency = Optional.of(simpleCrac.getContingency(state.get("contingency").asText()));
            }
            simpleCrac.addState(new SimpleState(contingency, simpleCrac.getInstant(state.get("instant").asText())));
        }
        for (Iterator<JsonNode> it = node.get("cnecs").elements(); it.hasNext(); ) {
            JsonNode cnec = it.next();
            simpleCrac.addCnec(new SimpleCnec(
                cnec.get("id").asText(),
                cnec.get("name").asText(),
                simpleCrac.getNetworkElement(cnec.get("networkElement").asText()),
                jsonParser.getCodec().treeToValue(cnec.get("threshold"), AbstractThreshold.class),
                simpleCrac.getState(cnec.get("state").asText())
            ));
        }
        for (Iterator<JsonNode> it = node.get("networkActions").elements(); it.hasNext(); ) {
            JsonNode networkAction = it.next();
            switch (networkAction.get("type").asText()) {
                case "complex-network-action":
                    List<UsageRule> usageRules = getUsageRules(simpleCrac, networkAction);
                    Set<AbstractElementaryNetworkAction> abstractElementaryNetworkActions = new HashSet<>();
                    for (Iterator<JsonNode> itElementary = networkAction.get("elementaryNetworkActions").elements(); itElementary.hasNext(); ) {
                        JsonNode elementaryNetworkAction = itElementary.next();
                        abstractElementaryNetworkActions.add(getAbstractElementaryNetworkAction(simpleCrac, elementaryNetworkAction));
                    }
                    simpleCrac.addNetworkAction(new ComplexNetworkAction(
                        networkAction.get("id").asText(),
                        networkAction.get("name").asText(),
                        networkAction.get("operator").asText(),
                        usageRules,
                        abstractElementaryNetworkActions
                    ));
                    break;
                default:
                    simpleCrac.addNetworkAction(getAbstractElementaryNetworkAction(simpleCrac, networkAction));
            }
        }

        return simpleCrac;
    }

    private static AbstractElementaryNetworkAction getAbstractElementaryNetworkAction(SimpleCrac simpleCrac, JsonNode elementaryNetworkActionNode) {
        List<UsageRule> usageRules = getUsageRules(simpleCrac, elementaryNetworkActionNode);
        switch (elementaryNetworkActionNode.get("type").asText()) {
            case "pst-setpoint":
                return new PstSetpoint(
                    elementaryNetworkActionNode.get("id").asText(),
                    elementaryNetworkActionNode.get("name").asText(),
                    elementaryNetworkActionNode.get("operator").asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get("networkElement").asText()),
                    elementaryNetworkActionNode.get("setpoint").asDouble()
                );
            case "hvdc-setpoint":
                return new HvdcSetpoint(
                    elementaryNetworkActionNode.get("id").asText(),
                    elementaryNetworkActionNode.get("name").asText(),
                    elementaryNetworkActionNode.get("operator").asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get("networkElement").asText()),
                    elementaryNetworkActionNode.get("setpoint").asDouble()
                );
            case "injection-setpoint":
                return new InjectionSetpoint(
                    elementaryNetworkActionNode.get("id").asText(),
                    elementaryNetworkActionNode.get("name").asText(),
                    elementaryNetworkActionNode.get("operator").asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get("networkElement").asText()),
                    elementaryNetworkActionNode.get("setpoint").asDouble()
                );
            case "topology":
                return new Topology(
                    elementaryNetworkActionNode.get("id").asText(),
                    elementaryNetworkActionNode.get("name").asText(),
                    elementaryNetworkActionNode.get("operator").asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get("networkElement").asText()),
                    ActionType.valueOf(elementaryNetworkActionNode.get("actionType").asText())
                );
            default:
                throw new FaraoException("Unknown type");
        }
    }

    private static List<UsageRule> getUsageRules(SimpleCrac simpleCrac, JsonNode abstractRemedialActionNode) {
        List<UsageRule> usageRules = new ArrayList<>();
        for (Iterator<JsonNode> itUsageRule = abstractRemedialActionNode.get("usageRules").elements(); itUsageRule.hasNext(); ) {
            JsonNode usageRule = itUsageRule.next();
            switch (usageRule.get("type").asText()) {
                case "free-to-use":
                    usageRules.add(new FreeToUse(UsageMethod.valueOf(usageRule.get("usageMethod").asText()), simpleCrac.getState(usageRule.get("state").asText())));
                    break;
                case "on-constraint":
                    usageRules.add(new OnConstraint(
                        UsageMethod.valueOf(usageRule.get("usageMethod").asText()),
                        simpleCrac.getState(usageRule.get("state").asText()),
                        simpleCrac.getCnec(usageRule.get("cnec").asText())));
                    break;
                case "on-contingency":
                    usageRules.add(new OnContingency(
                        UsageMethod.valueOf(usageRule.get("usageMethod").asText()),
                        simpleCrac.getState(usageRule.get("state").asText()),
                        simpleCrac.getContingency(usageRule.get("contingency").asText())));
                    break;
            }
        }
        return usageRules;
    }
}
