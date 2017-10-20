business-matching
=============
[![Build Status](https://travis-ci.org/hmrc/business-matching.svg?branch=master)](https://travis-ci.org/hmrc/business-matching) [ ![Download](https://api.bintray.com/packages/hmrc/releases/business-matching/images/download.svg) ](https://bintray.com/hmrc/releases/business-matching/_latestVersion)

This service privides the ability for agents, organistation or self-assessment individuals to search (and register) to ETMP. This is Register Once in ROSM pattern.

## Business Lookup APIs

The request must be a valid json using one of the following uris

| PATH | Supported Methods | Description |
|------|-------------------|-------------|
|``` /sa/:gg/business-matching/business-lookup/:utr/:userType``` | POST | Self Assessment users should call this |
|``` /org/:gg/business-matching/business-lookup/:utr/:userType``` | POST | Organisations should call this |
|``` /agent/:gg/business-matching/business-lookup/:utr/:userType``` | POST | Agents should call this |

where,

| Parameter | Message                      |
|:--------:|------------------------------|
|    gg    | unique auth id for clients/agents  |
|   utr    | he Unique Tax Reference being looked up |
| userType | Whether this is "sa", "org" or "agent" |


### Usage

```POST /agent/123456789/business-matching/business-lookup/1111111111/agent```

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
 
This code is open source software licensed under the [Apache 2.0 License].

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0.html
 **Response**
 
 | Status | Message     |
 |-------|-------------|
 | 200   | Ok          |
 | 400   | Bad Request |
 | 404   | Not Found   |
 | 500   | Internal Server Error |
 | 503   | Service Unavailable |

This code is open source software licensed under the [Apache 2.0 License].

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0.html
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

### License


This code is open source software licensed under the [Apache 2.0 License].

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0.html
