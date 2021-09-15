CREATE TABLE IF NOT EXISTS oracle_token_price
(
    token_pair_name character varying(64) COLLATE pg_catalog."default" NOT NULL,
    ts timestamp without time zone NOT NULL DEFAULT now(),
    price numeric NOT NULL,
    decimals integer NOT NULL,
    status smallint NOT NULL,
    txn_hash character(64) COLLATE pg_catalog."default",
    CONSTRAINT pk PRIMARY KEY (token_pair_name, ts)
);

CREATE TABLE IF NOT EXISTS token_swap_stat
(
    token_name character varying(128) COLLATE pg_catalog."default" NOT NULL,
    ts time without time zone NOT NULL  DEFAULT now(),
    volume_amount numeric NOT NULL,
    volume numeric,
    tvl numeric,
    CONSTRAINT token_swap_stat_pkey PRIMARY KEY (token_name, ts)
);

CREATE TABLE IF NOT EXISTS token_pool_swap_stat
(
    first_token_name character varying(128) COLLATE pg_catalog."default" NOT NULL,
    second_token_name character varying(128) COLLATE pg_catalog."default" NOT NULL,
    ts time without time zone NOT NULL  DEFAULT now(),
    volume_amount numeric NOT NULL,
    volume numeric,
    tvl numeric,
    CONSTRAINT token_pool_swap_stat_pkey PRIMARY KEY (first_token_name, second_token_name ,ts)
);