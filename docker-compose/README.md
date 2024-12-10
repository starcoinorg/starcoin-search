# How to build local environment

## 1. Install "Docker" and "Docker Compose"
## 2. Run the command to start the database and components
```bash
docker-compose up
```
## 3. Start starcoin-index project

### Config the startup environment variable
```dotenv
HOSTS=localhost  
NETWORK=halley # select which network to scan
BG_TASK_JOBS=dag_inspector
TXN_OFFSET=0
BULK_SIZE=100
STARCOIN_ES_PWD=
STARCOIN_ES_URL=localhost
STARCOIN_ES_PROTOCOL=http
STARCOIN_ES_PORT=9200
STARCOIN_ES_USER=
SWAP_API_URL=https://swap-api.starswap.xyz
SWAP_CONTRACT_ADDR=0x8c109349c6bd91411d6bc962e080c4a3
DS_URL=jdbc:postgresql://localhost/starcoin
DB_SCHEMA=halley
DB_USER_NAME=starcoin
DB_PWD=starcoin
PROGRAM_ARGS=
# auto_repair 9411700
```

### Configuration Elasticsearch template
[IMPORTANT!!] Make sure your template has added to Elastic search service before add data, including component template and index template to ES.
Following file: [[es_pipeline.scripts](..%2Fkube%2Fmappings%2Fes_pipeline.scripts)]

1. Open the 'Kibana' site has been started in the docker-compose environment, usually the url is http://localhost:5601 
2. Navigate to 'Dev Tools'
3. Follow the instructions in the file of giving above to add the template to ES

### Add SQL tables for network
[IMPORTANT!!] Add the [tables](../starcoin-indexer/deploy/create_table.sql) for the network you want to scan, including main, barnard, halley, etc.

## 4. Start starcoin-scan-api project

### Config the startup enviroment variable
```dotenv
STARCOIN_ES_URL=localhost
STARCOIN_ES_PROTOCOL=http
STARCOIN_ES_PORT=9200
STARCOIN_ES_USER=
STARCOIN_ES_INDEX_VERSION=
STARCOIN_ES_PWD=
MAIN_DS_URL=jdbc:postgresql://localhost/starcoin?currentSchema=main
BARNARD_DS_URL=jdbc:postgresql://localhost/starcoin?currentSchema=barnard
HALLEY_DS_URL=jdbc:postgresql://localhost/starcoin?currentSchema=halley
DS_URL=jdbc:postgresql://localhost/starcoin
STARCOIN_USER_DS_URL="jdbc:postgresql://localhost/starcoin?currentSchema=starcoin_user"
DB_USER_NAME=starcoin
DB_PWD=starcoin
```

### Add SQL tables for network
[IMPORTANT!!] Add the [tables](../starcoin-scan-api/deploy/create_table.sql) for the network you want to scan, including main, barnard, halley, etc.
