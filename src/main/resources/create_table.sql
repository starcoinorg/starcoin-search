// swap transaction
CREATE TABLE IF NOT EXISTS transaction_payload
(
    transaction_hash character varying
(
    66
) COLLATE pg_catalog."default" NOT NULL,
    json text NOT NULL,
    CONSTRAINT swap_transaction_pkey PRIMARY KEY
(
    transaction_hash
)
    );

// swap transaction
CREATE TABLE IF NOT EXISTS swap_transaction
(
    transaction_hash character varying
(
    66
) COLLATE pg_catalog."default" NOT NULL,
    total_value numeric,
    token_a character varying
(
    512
) COLLATE pg_catalog."default" NOT NULL,
    amount_a numeric NOT NULL,
    token_b character varying
(
    512
) COLLATE pg_catalog."default" NOT NULL,
    amount_b numeric NOT NULL,
    account character varying
(
    34
) COLLATE pg_catalog."default" NOT NULL,
    ts bigint NOT NULL,
    swap_type smallint NOT NULL,
    CONSTRAINT swap_transaction_pkey PRIMARY KEY
(
    transaction_hash
)
    );

// swap天维度汇总统计
CREATE TABLE IF NOT EXISTS swap_day_stat
(
    stat_date time without time zone NOT NULL,
    volume_amount numeric NOT NULL,
    volume numeric,
    tvl_amount numeric NOT NULL,
    tvl numeric,
    CONSTRAINT swap_stat_day_pkey PRIMARY KEY
(
    stat_date
)
    );

CREATE TABLE IF NOT EXISTS token_swap_day_stat
(
    token_name character varying
(
    256
) COLLATE pg_catalog."default" NOT NULL,
    ts time without time zone NOT NULL DEFAULT now
(
),
    volume_amount numeric NOT NULL,
    volume numeric,
    tvl_amount numeric NOT NULL,
    tvl numeric,
    CONSTRAINT token_swap_stat_day_pkey PRIMARY KEY
(
    token_name,
    ts
)
    );

CREATE TABLE IF NOT EXISTS pool_swap_day_stat
(
    first_token_name character varying
(
    256
) COLLATE pg_catalog."default" NOT NULL,
    second_token_name character varying
(
    256
) COLLATE pg_catalog."default" NOT NULL,
    ts time without time zone NOT NULL DEFAULT now
(
),
    volume_amount numeric NOT NULL,
    volume numeric,
    tvl_a_amount numeric NOT NULL,
    tvl_a numeric,
    tvl_b_amount numeric NOT NULL,
    tvl_b numeric,
    CONSTRAINT pool_swap_day_stat_pkey PRIMARY KEY
(
    first_token_name,
    second_token_name,
    ts
)
    );