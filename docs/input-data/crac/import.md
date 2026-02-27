# CRAC import & export

## Introduction

The [OpenRAO CRAC object model](json.md) can be directly imported and exported using the CRAC API.  

The JSON format - also called OpenRAO internal format - is a raw image of the CRAC object model of OpenRAO. It is 
particularly suited to exchange a CRAC java object through files, for instance to exchange CRAC data between 
microservices or Kubernetes pods of an application. It has an importer and an exporter. The complete round-trip 
(java object → export → json file → import → java object) has been designed so that the CRAC at the 
beginning of the chain is exactly the same as the one at the end of the chain.  

Examples of JSON formats are given on this [page](json.md).  

Examples of uses of CRAC import/export are given below:  

~~~java
// import a CRAC from a file or an input stream
Crac crac = Crac.read("crac.json", new FileInputStream(new File("/tmp/crac.json")), network, new CracCreationParameters());

// export a CRAC in JSON to a file or output stream
crac.write("JSON", Paths.get("/tmp/crac.json"));
~~~

The formats currently supported by OpenRAO are:
- [OpenRAO JSON format](json.md) (import & export)
- [FlowBasedConstraint document](fbconstraint.md), also known as Merged-CB, CBCORA or F301 (import only)
- [CSE CRAC](cse.md) (import only)
- [CIM CRAC](cim.md) (import only)
- [CSA PROFILES CRAC](nc.md) (import only)
- Also, an automatic [CRAC generator](crac-generator.md) from a network file.

## Versioning of internal JSON CRAC files

Json files and json importer/exporter are versioned.  
The version number does not correspond to the version number of powsybl-open-rao. The version only increases when a 
modification is made within the JSON importer / exporter.  
- The number version of the json files corresponds to the number of version of the exporter by which it has been exported.
- A Json file can be imported by powsybl-open-rao only if the versions of the file and the importer are compatible (see below)
- A Json file cannot be exported to an older version of the format  


| File version (example) | Importer version (example) | Is compatible? | Explanation                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|------------------------|----------------------------|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1.0                    | 1.0                        | YES            |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| 1.0                    | 1.1                        | YES            | The importer is compatible with all the previous versions of the json file, **given that the first number of the version does not change**!                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| 1.1                    | 1.0                        | NO[^1]         | The importer is *a priori* not compatible with newer versions of the file.  <br> For instance, if a json CRAC is generated with powsybl-open-rao 5.3.0 (importer version = 1.3) and read with powsybl-open-rao 5.1.0 (importer version = 1.2), the importer should not work. <br> However, the import is not systematically rejected. It might even work in some situation. <br> For instance, in the example above, if a 1.1 crac does not contain the feature specific to its version, the newly introduced switchPair elementary action, it will still be importable by the 1.0 importer. |
| 1.0                    | 2.0                        | NO             | compatibility is not ensured anymore when first version number change                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 2.0                    | 1.0                        | NO             | compatibility is not ensured anymore when first version number change                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |

[^1]: might work in some situations

## CracCreationParameters and CracCreationContext

The [OpenRAO CRAC object model](json.md) is not a bijection ("one-to-one" mapping) of existing "business" (or "native") formats:  
- The same business objects can be modelled differently in native formats, and in OpenRAO. For example, an [OpenRAO CNEC](json.md#cnecs) 
  can be modelled as an implicit coupling of a line and a contingency, defined independently in the native format.
- The perimeter of the "CRAC" business object is not rigid. [OpenRAO's CRAC object](json.md) offers the possibility to model 
  more business objects than most native formats. For instance, it can model [remedial action usage limits](json.md#ras-usage-limitations), 
  which is currently not supported by any native format.

To handle this complex mapping, extra objects have been created:  
- The CRAC importers return a [CracCreationContext](creation-context.md) object, that contains:
    - the created OpenRAO CRAC object
    - additional information which explains how the initial format has been mapped into the OpenRAO format. This 
      mapping is often not straightforward (see below). The CracCreationContext enables to keep in memory a link 
      between the native CRAC and the OpenRAO CRAC objects, in order to allow business assessment of the RAO's results.
- The CRAC importers can process a [CracCreationParameters](creation-parameters.md) object, that allows the user to add to 
  the OpenRAO CRAC object information that is not possible to model in the native format (see examples [here](creation-parameters.md)).

## Importing a CRAC object from a file
The user can import a CRAC object from a file using one of two static methods of the Crac interface.  
It is necessary to add the needed format importers (`Importer` implementation) to the run-time dependencies.

~~~java
// If you need access to the CracCreationContext:
CracCreationContext cracCreationContext = Crac.readWithContext(filename, inputStream, network, cracCreationParameters);
Crac crac = cracCreationContext.getCrac();

// If you don't need the CracCreationContext and only care about the Crac:
Crac crac = Crac.read(filename, inputStream, network, cracCreationParameters)
~~~
Where:
- **filename** is the name of the native or json CRAC file (useful to start guessing the file's format).
- **inputStream** is the stream with the contents of the CRAC file.
- **network** is the PowSyBl network object that OpenRAO shall use to interpret the CRAC file.
- **cracCreationParameters** is the [CracCreationParameters](creation-parameters.md) object that allows adding context 
  and data to the CRAC object creation.

## Exporting a CRAC object to a file
The user can use the following method in order to write the OpenRAO CRAC object to a file:

~~~java
Crac crac = ...
crac.write(format, outputStream);
~~~
Where:
- **format** is a String indicating the format of the export (currently, only "JSON" is supported)
- **outputStream** is the stream that should be written to

## Implementing new CRAC file formats
Developers are welcome to add CRAC importers/exporters to handle their native formats, by implementing the`Importer`/
`Exporter` interface of the CRAC I/O module, and add the implementations to their run-time dependencies.
There is no obligation to publish the resulting code.  
You can find inspiration in existing CRAC importers' code on our [GitHub](https://github.com/powsybl/powsybl-open-rao).  
You should also get familiar with our java [CRAC creation API](json.md).  
