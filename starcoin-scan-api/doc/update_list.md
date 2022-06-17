## StarcoinScan-API Update List

### Update at June 17, 2022
#### update market_cap store type from Long to String
```
token接口增加market_cap_str字段，防止某些语言下原market_cap字段溢出。
影响接口：
/v2/token/{network}/info/{token}
/v2/token/{network}/stats/{page}
建议直接用market_cap_str展示，如果业务涉及计算的话。原来的market_cap字段以后可能会废弃。
```
#### Add volume_str field
```
token接口增加volume_str字段，防止某些语言下原volume字段溢出。
影响接口：
/v2/token/{network}/info/{token}
/v2/token/{network}/stats/{page}
建议直接用volume_str展示，如果业务涉及计算的话。原来的volume_str字段以后可能会废弃。
```
[info](https://api.stcscan.io/swagger-ui/#/token/tokenInfoAggregateUsingGET)
[stats](https://api.stcscan.io/swagger-ui/#/token/getAggregateUsingGET)
