business-matching
=============
[![Build Status](https://travis-ci.org/hmrc/business-matching.svg?branch=master)](https://travis-ci.org/hmrc/business-matching) [ ![Download](https://api.bintray.com/packages/hmrc/releases/business-matching/images/download.svg) ](https://bintray.com/hmrc/releases/business-matching/_latestVersion)

This service provides the ability for agents, organistation or self-assessment individuals to search (and register) to ETMP. This is Register Once in ROSM pattern.

## Business Lookup APIs

The request must be a valid json using one of the following uris

| PATH | Supported Methods | Description |
|------|-------------------|-------------|
|``` /sa/:sa/business-matching/business-lookup/:utr/:userType``` | POST | Self Assessment users should call this |
|``` /org/:org/business-matching/business-lookup/:utr/:userType``` | POST | Organisations should call this |
|``` /agent/:ac/business-matching/business-lookup/:utr/:userType``` | POST | Agents should call this |

where,

| Parameter | Message                      |
|:--------:|------------------------------|
|    sa/org/ac    | unique auth id for clients/agents  |
|   utr    | he Unique Tax Reference being looked up |
| userType | Whether this is "sa", "org" or "agent" |


### Usage

```POST /agent/123456789/business-matching/business-lookup/1111111111/agent```

```POST /org/123477789/business-matching/business-lookup/1111111111/org```

```POST /sa/123890789/business-matching/business-lookup/1111111111/sa```

 **Request body**

```json
{
  "acknowledgementReference": "12345678901234123456789098909090",
  "utr": "1234567890",
  "requiresNameMatch": true,
  "isAnAgent": true,
  "individual": {
    "firstName": "Stephen",
    "lastName": "Wood",
    "dateOfBirth": "1990-04-03"
  }
}
```
 
 **Response**
 
 | Status | Message     |
 |-------|-------------|
 | 200   | Ok          |
 | 400   | Bad Request |
 | 404   | Not Found   |
 | 500   | Internal Server Error |
 | 503   | Service Unavailable |

 ```json
{
  "sapNumber": "1234567890",
  "safeId": "XE0001234567890",
  "agentReferenceNumber": "AARN1234567",
  "isEditable": true,
  "isAnAgent": false,
  "isAnIndividual": true,
  "individual": {
    "firstName": "Stephen",
    "lastName": "Wood",
    "dateOfBirth": "1990-04-03"
  },
  "address": {
    "addressLine1": "100 SuttonStreet",
    "addressLine2": "Wokingham",
    "addressLine3": "Surrey",
    "addressLine4": "London",
    "postalCode": "DH14EJ",
    "countryCode": "GB"
  },
  "contactDetails": {
    "phoneNumber": "01332752856",
    "mobileNumber": "07782565326",
    "faxNumber": "01332754256",
    "emailAddress": "stephen@manncorpone.co.uk"
  }
}
 ```

### All tests and checks

> `sbt runAllChecks`

This is an sbt command alias specific to this project. It will run

- clean
- compile
- unit tests
- integration tests
- and produce a coverage report.

You can view the coverage report in the browser by pasting the generated url.

#### Installing sbt plugin to check for library updates.
To check for dependency updates locally you will need to create this file locally ~/.sbt/1.0/plugins/sbt-updates.sbt
and paste - addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.3") - into the file.
Then run:

> `sbt dependencyUpdates `

To view library update suggestions - this does not cover sbt plugins.
It is not advised to install the plugin for the project.


### License

This code is open source software licensed under the [Apache 2.0 License].

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0.html
