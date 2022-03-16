-- # block_info
CREATE SCHEMA IF NOT EXISTS barnard;

CREATE TABLE IF NOT EXISTS barnard.block_info
(
    block_hash character varying(66) COLLATE pg_catalog."default" NOT NULL,
    block_number bigint                                           NOT NULL,
    total_difficulty character varying(50)                        NOT NULL,
    block_accumulator_info text                                   NOT NULL,
    txn_accumulator_info text                                     NOT NULL,
    CONSTRAINT block_info_pkey PRIMARY KEY (block_hash)
    );

CREATE SCHEMA IF NOT EXISTS main;

CREATE TABLE IF NOT EXISTS main.block_info
(
    block_hash character varying(66) COLLATE pg_catalog."default" NOT NULL,
    block_number bigint                                           NOT NULL,
    total_difficulty character varying(50)                        NOT NULL,
    block_accumulator_info text                                   NOT NULL,
    txn_accumulator_info text                                     NOT NULL,
    CONSTRAINT block_info_pkey PRIMARY KEY (block_hash)
    );

CREATE SCHEMA IF NOT EXISTS halley;

CREATE TABLE IF NOT EXISTS halley.block_info
(
    block_hash character varying(66) COLLATE pg_catalog."default" NOT NULL,
    block_number bigint                                           NOT NULL,
    total_difficulty character varying(50)                        NOT NULL,
    block_accumulator_info text                                   NOT NULL,
    txn_accumulator_info text                                     NOT NULL,
    CONSTRAINT block_info_pkey PRIMARY KEY (block_hash)
    );