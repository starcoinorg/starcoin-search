# starcoin scan-api doc
Library supports all available StarcoinScan API calls for all available Starcoin Networks for
explorer.starcoin.org

## Version: 1.0

**Contact information:**  
author  
ssyuan  
suoyuan@gmail.com  

### /v2/block/info/{network}/hash/{hash}

#### GET
##### Summary

get block_info by hash

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| hash | path | hash | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/block/{network}/

#### GET
##### Summary

get block by ID

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| id | query | id | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/block/{network}/hash/{hash}

#### GET
##### Summary

get block by hash

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| hash | path | hash | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/block/{network}/height/{height}

#### GET
##### Summary

get block by height

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| height | path | height | Yes | long |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/block/{network}/page/{page}

#### GET
##### Summary

get block list

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/block/{network}/start_height/

#### GET
##### Summary

get block list by start height

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| start_height | query | start_height | No | long |
| page | query | page | No | integer |
| count | query | count | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/block/{network}/uncle/hash/{hash}

#### GET
##### Summary

get uncle block by hash

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| hash | path | hash | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/block/{network}/uncle/height/{height}

#### GET
##### Summary

get uncle block by height

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| height | path | height | Yes | long |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/block/{network}/uncle/page/{page}

#### GET
##### Summary

get uncle block list

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |
| total | query | total | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/token/{network}/holders/page/{page}

#### GET
##### Summary

get token holders list

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |
| token_type | query | token_type | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/token/{network}/info/{token}

#### GET
##### Summary

get token aggregate info

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| token | path | token | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/token/{network}/market_cap/{token}

#### GET
##### Summary

get token market cap

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| token | path | token | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/token/{network}/stats/{page}

#### GET
##### Summary

get token aggregate stat list

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/address/{network}/{address}/page/{page}

#### GET
##### Summary

get transaction list of page range by address

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| address | path | address | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |
| txn_type | query | txn_type | No | integer |
| with_event | query | with_event | No | boolean |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/nft/{network}/page/{page}

#### GET
##### Summary

get nft transaction list of page range by address

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| page | path | page | Yes | integer |
| start_time | query | start_time | No | long |
| address | query | address | No | string |
| count | query | count | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/pending_txn/get/{network}/{id}

#### GET
##### Summary

get pending transaction by ID

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| id | path | id | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/pending_txns/{network}/page/{page}

#### GET
##### Summary

get pending transaction list

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |
| start_height | query | start_height | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/byAddress/{address}

#### GET
##### Summary

get transaction list by address

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| address | path | address | Yes | string |
| count | query | count | No | integer |
| txn_type | query | txn_type | No | integer |
| with_event | query | with_event | No | boolean |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/byBlock/{block_hash}

#### GET
##### Summary

get transaction by block

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| block_hash | path | block_hash | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/byBlockHeight/{block_height}

#### GET
##### Summary

get transaction by block height

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| block_height | path | block_height | Yes | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/events/byTag/{tag_name}/page/{page}

#### GET
##### Summary

get transaction events by tag

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| tag_name | path | tag_name | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/hash/{hash}

#### GET
##### Summary

get transaction by hash

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| hash | path | hash | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/page/{page}

#### GET
##### Summary

get transaction list

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |
| start_height | query | start_height | No | integer |
| txn_type | query | txn_type | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/start_time/

#### GET
##### Summary

get transaction list by start time

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| start_time | query | start_time | No | long |
| page | query | page | No | integer |
| count | query | count | No | integer |
| txn_type | query | txn_type | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/transfer/byTag/{tag_name}/page/{page}

#### GET
##### Summary

get transfer by token

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| tag_name | path | tag_name | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/transfer/conversations/page/{page}

#### GET
##### Summary

get transfer by sender and receiver

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| receiver | query | receiver | Yes | string |
| sender | query | sender | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/transfer/count/byTag/{tag_name}

#### GET
##### Summary

get transfers count by token

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| tag_name | path | tag_name | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/transfer/receiver/{receiver}/page/{page}

#### GET
##### Summary

get transfer by receiver

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| receiver | path | receiver | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/transfer/sender/{sender}/page/{page}

#### GET
##### Summary

get transfer by sender

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| sender | path | sender | Yes | string |
| page | path | page | Yes | integer |
| count | query | count | No | integer |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/transaction/{network}/{id}

#### GET
##### Summary

get transaction by ID

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| network | path | network | Yes | string |
| id | path | id | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/apikey/add/{app_name}

#### GET
##### Summary

add api key of dapp

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| address | query | address | Yes | string |
| app_name | path | app_name | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/apikey/list/

#### GET
##### Summary

get user api keys

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| address | query | address | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/apikey/remove

#### GET
##### Summary

remove api key

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| app_key | query | app_key | Yes | string |
| address | query | address | Yes | string |
| sign | query | sign | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/apikey/update/{app_name}

#### GET
##### Summary

update app name

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| app_name | path | app_name | Yes | string |
| app_key | query | app_key | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/code/

#### GET
##### Summary

get code

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| opt | query | opt | Yes | integer |
| address | query | address | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/destroy/{address}

#### GET
##### Summary

delete user

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| address | path | address | Yes | string |
| sign | query | sign | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/login/{address}/

#### GET
##### Summary

login by address

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| address | path | address | Yes | string |
| sign | query | sign | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/logout/{address}/

#### GET
##### Summary

logout

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| address | path | address | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/show/{address}

#### GET
##### Summary

show user info

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| address | path | address | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/update/address/{new}

#### GET
##### Summary

update wallet address

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| new | path | new | Yes | string |
| old | query | old | Yes | string |
| sign | query | sign | Yes | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### /v2/user/update/{address}

#### GET
##### Summary

update user profile info

##### Parameters

| Name | Located in | Description | Required | Schema |
| ---- | ---------- | ----------- | -------- | ---- |
| address | path | address | Yes | string |
| mobile | query | mobile | No | string |
| email | query | email | No | string |
| avatar | query | avatar | No | string |
| twitter | query | twitter | No | string |
| discord | query | discord | No | string |
| telegram | query | telegram | No | string |
| domain | query | domain | No | string |
| blog | query | blog | No | string |
| profile | query | profile | No | string |

##### Responses

| Code | Description |
| ---- | ----------- |
| 200 | OK |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |

### Models

#### ApiKey

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| apiKey | string |  | No |
| appName | string |  | No |
| createTime | dateTime |  | No |
| id | long |  | No |
| userId | long |  | No |
| valid | boolean |  | No |

#### Authenticator

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| ed25519 | object |  | No |

#### Block

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| blockMetadata | object |  | No |
| body | object |  | No |
| header | object |  | No |
| id | string |  | No |
| transactionList | [ object ] |  | No |
| uncles | [ object ] |  | No |

#### BlockBody

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| userTransactions | [ object ] |  | No |

#### BlockHeader

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| author | string |  | No |
| authorAuthKey | string |  | No |
| blockAccumulatorRoot | string |  | No |
| blockHash | string |  | No |
| bodyHash | string |  | No |
| chainId | integer |  | No |
| difficulty | long |  | No |
| difficultyHexStr | string |  | No |
| extra | string |  | No |
| gasUsed | long |  | No |
| height | long |  | No |
| id | string |  | No |
| nonce | long |  | No |
| parentHash | string |  | No |
| stateRoot | string |  | No |
| timestamp | long |  | No |
| txnAccumulatorRoot | string |  | No |

#### BlockInfoEntity

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| blockAccumulatorInfo | string |  | No |
| blockHash | string |  | No |
| blockNumber | long |  | No |
| totalDifficulty | string |  | No |
| txnAccumulatorInfo | string |  | No |

#### BlockMetadata

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| author | string |  | No |
| authorAuthKey | string |  | No |
| chainId | string |  | No |
| number | string |  | No |
| parentGasUsed | long |  | No |
| parentHash | string |  | No |
| timestamp | long |  | No |
| uncles | string |  | No |

#### Ed25519

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| publicKey | string |  | No |
| signature | string |  | No |

#### Event

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| blockHash | string |  | No |
| blockNumber | string |  | No |
| data | string |  | No |
| decodeEventData | string |  | No |
| eventKey | string |  | No |
| eventSeqNumber | string |  | No |
| id | string |  | No |
| transactionGlobalIndex | long |  | No |
| transactionHash | string |  | No |
| transactionIndex | integer |  | No |
| typeTag | string |  | No |

#### JSONResult

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| body | object |  | No |
| statusCode | string | _Enum:_ `"ACCEPTED"`, `"ALREADY_REPORTED"`, `"BAD_GATEWAY"`, `"BAD_REQUEST"`, `"BANDWIDTH_LIMIT_EXCEEDED"`, `"CHECKPOINT"`, `"CONFLICT"`, `"CONTINUE"`, `"CREATED"`, `"DESTINATION_LOCKED"`, `"EXPECTATION_FAILED"`, `"FAILED_DEPENDENCY"`, `"FORBIDDEN"`, `"FOUND"`, `"GATEWAY_TIMEOUT"`, `"GONE"`, `"HTTP_VERSION_NOT_SUPPORTED"`, `"IM_USED"`, `"INSUFFICIENT_SPACE_ON_RESOURCE"`, `"INSUFFICIENT_STORAGE"`, `"INTERNAL_SERVER_ERROR"`, `"I_AM_A_TEAPOT"`, `"LENGTH_REQUIRED"`, `"LOCKED"`, `"LOOP_DETECTED"`, `"METHOD_FAILURE"`, `"METHOD_NOT_ALLOWED"`, `"MOVED_PERMANENTLY"`, `"MOVED_TEMPORARILY"`, `"MULTIPLE_CHOICES"`, `"MULTI_STATUS"`, `"NETWORK_AUTHENTICATION_REQUIRED"`, `"NON_AUTHORITATIVE_INFORMATION"`, `"NOT_ACCEPTABLE"`, `"NOT_EXTENDED"`, `"NOT_FOUND"`, `"NOT_IMPLEMENTED"`, `"NOT_MODIFIED"`, `"NO_CONTENT"`, `"OK"`, `"PARTIAL_CONTENT"`, `"PAYLOAD_TOO_LARGE"`, `"PAYMENT_REQUIRED"`, `"PERMANENT_REDIRECT"`, `"PRECONDITION_FAILED"`, `"PRECONDITION_REQUIRED"`, `"PROCESSING"`, `"PROXY_AUTHENTICATION_REQUIRED"`, `"REQUESTED_RANGE_NOT_SATISFIABLE"`, `"REQUEST_ENTITY_TOO_LARGE"`, `"REQUEST_HEADER_FIELDS_TOO_LARGE"`, `"REQUEST_TIMEOUT"`, `"REQUEST_URI_TOO_LONG"`, `"RESET_CONTENT"`, `"SEE_OTHER"`, `"SERVICE_UNAVAILABLE"`, `"SWITCHING_PROTOCOLS"`, `"TEMPORARY_REDIRECT"`, `"TOO_EARLY"`, `"TOO_MANY_REQUESTS"`, `"UNAUTHORIZED"`, `"UNAVAILABLE_FOR_LEGAL_REASONS"`, `"UNPROCESSABLE_ENTITY"`, `"UNSUPPORTED_MEDIA_TYPE"`, `"UPGRADE_REQUIRED"`, `"URI_TOO_LONG"`, `"USE_PROXY"`, `"VARIANT_ALSO_NEGOTIATES"` | No |
| statusCodeValue | integer |  | No |

#### JSONResult«List«ApiKey»»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| body | object |  | No |
| statusCode | string | _Enum:_ `"ACCEPTED"`, `"ALREADY_REPORTED"`, `"BAD_GATEWAY"`, `"BAD_REQUEST"`, `"BANDWIDTH_LIMIT_EXCEEDED"`, `"CHECKPOINT"`, `"CONFLICT"`, `"CONTINUE"`, `"CREATED"`, `"DESTINATION_LOCKED"`, `"EXPECTATION_FAILED"`, `"FAILED_DEPENDENCY"`, `"FORBIDDEN"`, `"FOUND"`, `"GATEWAY_TIMEOUT"`, `"GONE"`, `"HTTP_VERSION_NOT_SUPPORTED"`, `"IM_USED"`, `"INSUFFICIENT_SPACE_ON_RESOURCE"`, `"INSUFFICIENT_STORAGE"`, `"INTERNAL_SERVER_ERROR"`, `"I_AM_A_TEAPOT"`, `"LENGTH_REQUIRED"`, `"LOCKED"`, `"LOOP_DETECTED"`, `"METHOD_FAILURE"`, `"METHOD_NOT_ALLOWED"`, `"MOVED_PERMANENTLY"`, `"MOVED_TEMPORARILY"`, `"MULTIPLE_CHOICES"`, `"MULTI_STATUS"`, `"NETWORK_AUTHENTICATION_REQUIRED"`, `"NON_AUTHORITATIVE_INFORMATION"`, `"NOT_ACCEPTABLE"`, `"NOT_EXTENDED"`, `"NOT_FOUND"`, `"NOT_IMPLEMENTED"`, `"NOT_MODIFIED"`, `"NO_CONTENT"`, `"OK"`, `"PARTIAL_CONTENT"`, `"PAYLOAD_TOO_LARGE"`, `"PAYMENT_REQUIRED"`, `"PERMANENT_REDIRECT"`, `"PRECONDITION_FAILED"`, `"PRECONDITION_REQUIRED"`, `"PROCESSING"`, `"PROXY_AUTHENTICATION_REQUIRED"`, `"REQUESTED_RANGE_NOT_SATISFIABLE"`, `"REQUEST_ENTITY_TOO_LARGE"`, `"REQUEST_HEADER_FIELDS_TOO_LARGE"`, `"REQUEST_TIMEOUT"`, `"REQUEST_URI_TOO_LONG"`, `"RESET_CONTENT"`, `"SEE_OTHER"`, `"SERVICE_UNAVAILABLE"`, `"SWITCHING_PROTOCOLS"`, `"TEMPORARY_REDIRECT"`, `"TOO_EARLY"`, `"TOO_MANY_REQUESTS"`, `"UNAUTHORIZED"`, `"UNAVAILABLE_FOR_LEGAL_REASONS"`, `"UNPROCESSABLE_ENTITY"`, `"UNSUPPORTED_MEDIA_TYPE"`, `"UPGRADE_REQUIRED"`, `"URI_TOO_LONG"`, `"USE_PROXY"`, `"VARIANT_ALSO_NEGOTIATES"` | No |
| statusCodeValue | integer |  | No |

#### JSONResult«UserInfo»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| body | object |  | No |
| statusCode | string | _Enum:_ `"ACCEPTED"`, `"ALREADY_REPORTED"`, `"BAD_GATEWAY"`, `"BAD_REQUEST"`, `"BANDWIDTH_LIMIT_EXCEEDED"`, `"CHECKPOINT"`, `"CONFLICT"`, `"CONTINUE"`, `"CREATED"`, `"DESTINATION_LOCKED"`, `"EXPECTATION_FAILED"`, `"FAILED_DEPENDENCY"`, `"FORBIDDEN"`, `"FOUND"`, `"GATEWAY_TIMEOUT"`, `"GONE"`, `"HTTP_VERSION_NOT_SUPPORTED"`, `"IM_USED"`, `"INSUFFICIENT_SPACE_ON_RESOURCE"`, `"INSUFFICIENT_STORAGE"`, `"INTERNAL_SERVER_ERROR"`, `"I_AM_A_TEAPOT"`, `"LENGTH_REQUIRED"`, `"LOCKED"`, `"LOOP_DETECTED"`, `"METHOD_FAILURE"`, `"METHOD_NOT_ALLOWED"`, `"MOVED_PERMANENTLY"`, `"MOVED_TEMPORARILY"`, `"MULTIPLE_CHOICES"`, `"MULTI_STATUS"`, `"NETWORK_AUTHENTICATION_REQUIRED"`, `"NON_AUTHORITATIVE_INFORMATION"`, `"NOT_ACCEPTABLE"`, `"NOT_EXTENDED"`, `"NOT_FOUND"`, `"NOT_IMPLEMENTED"`, `"NOT_MODIFIED"`, `"NO_CONTENT"`, `"OK"`, `"PARTIAL_CONTENT"`, `"PAYLOAD_TOO_LARGE"`, `"PAYMENT_REQUIRED"`, `"PERMANENT_REDIRECT"`, `"PRECONDITION_FAILED"`, `"PRECONDITION_REQUIRED"`, `"PROCESSING"`, `"PROXY_AUTHENTICATION_REQUIRED"`, `"REQUESTED_RANGE_NOT_SATISFIABLE"`, `"REQUEST_ENTITY_TOO_LARGE"`, `"REQUEST_HEADER_FIELDS_TOO_LARGE"`, `"REQUEST_TIMEOUT"`, `"REQUEST_URI_TOO_LONG"`, `"RESET_CONTENT"`, `"SEE_OTHER"`, `"SERVICE_UNAVAILABLE"`, `"SWITCHING_PROTOCOLS"`, `"TEMPORARY_REDIRECT"`, `"TOO_EARLY"`, `"TOO_MANY_REQUESTS"`, `"UNAUTHORIZED"`, `"UNAVAILABLE_FOR_LEGAL_REASONS"`, `"UNPROCESSABLE_ENTITY"`, `"UNSUPPORTED_MEDIA_TYPE"`, `"UPGRADE_REQUIRED"`, `"URI_TOO_LONG"`, `"USE_PROXY"`, `"VARIANT_ALSO_NEGOTIATES"` | No |
| statusCodeValue | integer |  | No |

#### PendingTransaction

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| authenticator | object |  | No |
| id | string |  | No |
| rawTransaction | object |  | No |
| timestamp | long |  | No |
| transactionHash | string |  | No |

#### RawTransaction

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| authenticator | object |  | No |
| chainId | integer |  | No |
| expirationTimestampSecs | string |  | No |
| gasTokenCode | string |  | No |
| gasUnitPrice | string |  | No |
| maxGasAmount | string |  | No |
| payload | string |  | No |
| sender | string |  | No |
| sequenceNumber | string |  | No |
| transactionHash | string |  | No |
| transactionPayload | object |  | No |

#### ResponseMessage

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| data | object |  | No |
| message | string |  | No |
| status | string |  | No |

#### ResponseMessage«List«ApiKey»»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| data | [ object ] |  | No |
| message | string |  | No |
| status | string |  | No |

#### ResponseMessage«UserInfo»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| data | object |  | No |
| message | string |  | No |
| status | string |  | No |

#### Result«Block»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| contents | [ object ] |  | No |
| total | long |  | No |

#### Result«Event»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| contents | [ object ] |  | No |
| total | long |  | No |

#### Result«PendingTransaction»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| contents | [ object ] |  | No |
| total | long |  | No |

#### Result«TokenHolderInfo»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| contents | [ object ] |  | No |
| total | long |  | No |

#### Result«TokenStatisticView»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| contents | [ object ] |  | No |
| total | long |  | No |

#### Result«TokenTransfer»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| contents | [ object ] |  | No |
| total | long |  | No |

#### Result«TransactionWithEvent»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| contents | [ object ] |  | No |
| total | long |  | No |

#### Result«Transfer»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| contents | [ object ] |  | No |
| total | long |  | No |

#### Result«UncleBlock»

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| contents | [ object ] |  | No |
| total | long |  | No |

#### TokenHolderInfo

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| address | string |  | No |
| holdAmount | number (biginteger) |  | No |
| supply | number (biginteger) |  | No |

#### TokenStatisticView

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| addressHolder | long |  | No |
| marketCap | double |  | No |
| marketCapStr | string |  | No |
| typeTag | string |  | No |
| volume | long |  | No |
| volumeStr | string |  | No |

#### TokenTransfer

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| transfers | long |  | No |
| typeTag | string |  | No |

#### Transaction

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| blockHash | string |  | No |
| blockMetadata | object |  | No |
| blockNumber | string |  | No |
| eventRootHash | string |  | No |
| events | [ object ] |  | No |
| gasUsed | string |  | No |
| id | string |  | No |
| stateRootHash | string |  | No |
| status | string |  | No |
| timestamp | long |  | No |
| transactionGlobalIndex | long |  | No |
| transactionHash | string |  | No |
| transactionIndex | integer |  | No |
| transactionType | string | _Enum:_ `"Package"`, `"Script"`, `"ScriptFunction"` | No |
| userTransaction | object |  | No |

#### TransactionPayload

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| TransactionPayload | object |  |  |

#### TransactionWithEvent

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| blockHash | string |  | No |
| blockMetadata | object |  | No |
| blockNumber | string |  | No |
| eventRootHash | string |  | No |
| events | [ object ] |  | No |
| gasUsed | string |  | No |
| id | string |  | No |
| stateRootHash | string |  | No |
| status | string |  | No |
| timestamp | long |  | No |
| transactionGlobalIndex | long |  | No |
| transactionHash | string |  | No |
| transactionIndex | integer |  | No |
| transactionType | string | _Enum:_ `"Package"`, `"Script"`, `"ScriptFunction"` | No |
| userTransaction | object |  | No |

#### Transfer

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| amount | string |  | No |
| amountValue | number (biginteger) |  | No |
| id | string |  | No |
| identifier | string |  | No |
| receiver | string |  | No |
| sender | string |  | No |
| timestamp | long |  | No |
| txnHash | string |  | No |
| typeTag | string |  | No |

#### UncleBlock

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| header | object |  | No |
| id | string |  | No |
| uncleBlockNumber | long |  | No |

#### UserInfo

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| avatar | string |  | No |
| blogAddr | string |  | No |
| createTime | dateTime |  | No |
| discordName | string |  | No |
| domainName | string |  | No |
| eMail | string |  | No |
| id | long |  | No |
| lastLogin | dateTime |  | No |
| mobile | string |  | No |
| profile | string |  | No |
| telegramName | string |  | No |
| twitterName | string |  | No |
| userGrade | string | _Enum:_ `"advance"`, `"free"`, `"professional"`, `"standard"` | No |
| valid | boolean |  | No |
| walletAddr | string |  | No |

#### UserTransaction

| Name | Type | Description | Required |
| ---- | ---- | ----------- | -------- |
| authenticator | object |  | No |
| rawTransaction | object |  | No |
| transactionHash | string |  | No |
