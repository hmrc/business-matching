#business-matching
=============
[![Build Status](https://travis-ci.org/hmrc/business-matching.svg?branch=master)](https://travis-ci.org/hmrc/business-matching) [ ![Download](https://api.bintray.com/packages/hmrc/releases/business-matching/images/download.svg) ](https://bintray.com/hmrc/releases/business-matching/_latestVersion)

This service privides the ability for agents, organistation or self-assessment individuals to search ETMP for their Business Partner

###Business Lookup
=====

The request must be a valid json using one of the following uris
- `POST    /sa/:gg/business-matching/business-lookup/:utr/:userType:` Self Assessment users should call this
- `POST    /org/:gg/business-matching/business-lookup/:utr/:userType:` Organisations should call this
- `POST    /agent/:gg/business-matching/business-lookup/:utr/:userType:` Agents should call this
- `POST    /org/:gg/business-matching/business-lookup ` Currently only Organisations are supported

Where:

| Parameter | Message                      |
|:--------:|------------------------------|
|    gg    | The Users Government Gateway Id  |
|   utr    | he Unique Tax Reference being looked up |
| userType | Whether this is "sa", "org" or "agent" |


####Example of usage

    POST /agent/123456789/business-matching/business-lookup/:utr/agent

 **Request body**

 ```json
{
  "acknowledgmentReference": "12345678901234123456789098909090",
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
 **Response body**

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
    "eMailAddress": "stephen@manncorpone.co.uk"
  }
}
 ```
####Example of usage

    POST /gg/123456789/business-matching/business-lookup

 **Request body**

 ```json
{
   "MDGHeader":{
      "originatingSystem":"MDTP",
      "requestTimeStamp":"2016-07-13T12:28:50.843Z",
      "correlationId":"fb001dc0-f441-4d25-98e3-e8440b879908"
   },
   "messageTypes":{
      "messageType":"RegisterWithID"
   },
   "registrationDetails":{
      "IDType":"UTR",
      "IDNumber":"123456789",
      "requiresNameMatch":false,
      "isAnAgent":false
   }
}
 ```
 **Response body**

 ```json
{
   "MDGHeader":{
      "status":"OK",
      "processingDate":"2001-12-17T09:30:47.123456Z",
      "returnParameters":[
         {
            "paramName":"SAP_NUMBER",
            "paramValue":"0123456789"
         }
      ]
   },
   "SAFEID":"XE0000123456789",
   "isAnIndividual":true,
   "organisation":{
      "organisationName":"name of the organisation",
      "isAGroup":true,
      "organisationType":"Partnership",
      "code":"0001"
   },
   "address":{
      "addressType":"0001",
      "addressLine1":"21 Emmbrook Lane",
      "addressLine2":"Wokingham",
      "postalCode":"RG41 4RR",
      "countryCode":"GB"
   },
   "contactDetails":{
      "phoneNumber":"011 5666 4444",
      "mobileNo":"011 5666 4444",
      "faxNo":"011 5666 4444",
      "emailAddress":"fred.flintstone@org.co.uk"
   }
}
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
