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

-- # user info
CREATE SCHEMA starcoin_user;

CREATE TABLE IF NOT EXISTS starcoin_user.user_info
(
    user_id         SERIAL      NOT NULL,
    wallet_addr     character varying(50) COLLATE pg_catalog."default" NOT NULL,
    is_valid        boolean     NOT NULL DEFAULT true,
    e_mail          character varying(50),
    mobile          character varying(20),
    user_grade      smallint    NOT NULL,
    avatar          character varying(50),
    twitter_name    character varying(50),
    discord_name    character varying(50),
    telegram_name   character varying(50),
    domain_name     character varying(50),
    blog_addr       character varying(50),
    profile         character varying(200),
    create_time     DATE        NOT NULL DEFAULT now(),
    last_login      DATE,
    CONSTRAINT user_info_pkey PRIMARY KEY (user_id)
    );


CREATE TABLE IF NOT EXISTS starcoin_user.api_keys
(
    key_id      SERIAL  NOT NULL,
    user_id     bigint  NOT NULL,
    app_name    character varying(30) NOT NULL,
    api_key     character varying(50) NOT NULL,
    is_valid    boolean NOT NULL,
    create_time DATE NOT NULL DEFAULT now(),
    CONSTRAINT api_key_pkey PRIMARY KEY (key_id)
    );

CREATE TABLE IF NOT EXISTS starcoin_user.rate_limit
(
    rl_id       BIGSERIAL  NOT NULL,
    key_id      bigint     NOT NULL,
    rate_limit  integer    NOT NULL,
    create_time DATE NOT NULL DEFAULT now(),
    last_update DATE,
    CONSTRAINT rate_limit_pkey PRIMARY KEY (rl_id)
    );