After completing the RAO, the user can export the [`RaoResult`](/output-data/rao-result.md) object as a CORE CNE file using the `write` method with the `"CORE CNE""` format:

~~~java
// RaoResult interface
public void write(CracCreationContext cracCreationContext, Properties properties, OutputStream outputStream)
~~~

With:
- **raoResult**: the [RaoResult](/output-data/rao-result.md) object containing selected remedial actions and flow results.
- **cracCreationContext**: the [CracCreationContext object](/input-data/crac/creation-context.md) generated during 
  [CRAC creation](/input-data/crac/import.md). CORE CNE export only handles [UcteCracCreationContext](/input-data/crac/creation-context.md#ucte-implementation) 
  subtype, because it follows the UCTE conventions.
- **properties**: a specific object that te user should define, containing meta-information that will be written 
  in the header of the CNE file as well as relevant RAO parameters:
  - **`"document-id"`**: document ID to be written in "mRID" field
  - **`"revision-number"`**: integer to be written in "revisionNumber" field
  - **`"domain-id"`**: domain ID to be written in "domain.mRID" field (usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/))
  - **`"process-type"`**: the ENTSO-E code of the process type, to be written in "process.processType" field:
    - **A48**: Day-ahead capacity determination, used for CORE region
    - ~~**Z01**~~: Day-ahead capacity determination, used for SWE region (so don't use it here)
  - **`"sender-id"`**: ID of the sender of the CNE document, to be written in "sender_MarketParticipant.mRID" field 
    (usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/))
  - **`"sender-role"`**: ENTSO-E code defining the role of the sender of the CNE document, to be written in 
    "sender_MarketParticipant.marketRole.type" field:
    - **A04**: system operator
    - **A36**: capacity coordinator
    - **A44**: regional security coordinator
  - **`"receiver-id"`**: ID of the receiver of the CNE document, to be written in "receiver_MarketParticipant.mRID" field 
    (usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/))
  - **`"receiver-role"`**: ENTSO-E code defining the role of the receiver of the CNE document, to be written in
    "receiver_MarketParticipant.marketRole.type" field. Same value options as senderRole.
  - **`"time-interval"`**: time interval of document applicability, to be written in "time_Period.timeInterval" field. It should 
    be formatted as follows: "YYYY-MM-DDTHH:MMZ/YYYY-MM-DDTHH:MMZ" (start date / end date).
  - **`"objective-function-type"`** (optional, default is `"max-min-relative-margin-in-megawatt"`, should match the input RaoParameters)
  - **`"with-loop-flows"`** (optional, default is `"false"`, should match the input RaoParameters)
  - **`"mnec-acceptable-margin-diminution"`** (optional, default is `"0"`, should match the input RaoParameters)

Here is a complete example:

~~~java
// Fetch input data (network) and parameters
Network network = ...
RaoParameters raoParameters = ...
// Create CRAC
CracCreationContext cracCreationContext = CracCreators.createCrac(...);
Crac crac = cracCreationContext.getCrac();
// Run RAO
RaoResult raoResult = Rao.find(...).run(...)
// Set CNE header parameters
Properties properties = new Properties();
properties.setProperty("document-id", "DOCUMENT_ID");
properties.setProperty("revision-number", "1");
properties.setProperty("domain-id", "DOMAIN_ID");
properties.setProperty("process-type", "A48"); // DAY_AHEAD_CC
properties.setProperty("sender-id", "SENDER_ID");
properties.setProperty("sender-role", "A44"); // REGIONAL_SECURITY_COORDINATOR
properties.setProperty("receiver-id", "RECEIVER_ID");
properties.setProperty("receiver-role", "A36"); // CAPACITY_COORDINATOR
properties.setProperty("time-interval", "2021-10-30T22:00Z/2021-10-31T23:00Z");
// Set RaoParameters in properties
switch (raoParameters.getObjectiveFunctionParameters().getType()) {
    case MAX_MIN_RELATIVE_MARGIN_IN_AMPERE -> properties.setProperty("objective-function-type", "max-min-relative-margin-in-ampere");
    case MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT -> properties.setProperty("objective-function-type", "max-min-relative-margin-in-megawatt");
    case MAX_MIN_MARGIN_IN_AMPERE -> properties.setProperty("objective-function-type", "max-min-margin-in-ampere");
    case MAX_MIN_MARGIN_IN_MEGAWATT -> properties.setProperty("objective-function-type", "max-min-margin-in-megawatt");
}
if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
    properties.setProperty("with-loop-flows", "true");
}
if (raoParameters.hasExtension(MnecParametersExtension.class)) {
    properties.setProperty("mnec-acceptable-margin-diminution", String.valueOf(raoParameters.getExtension(MnecParametersExtension.class).getAcceptableMarginDecrease()));
}
// Export CNE to output stream
OutputStream os = ...
raoResult.write("CORE CNE", cracCreationContext, properties, os);
~~~
