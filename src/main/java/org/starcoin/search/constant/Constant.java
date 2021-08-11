package org.starcoin.search.constant;

public class Constant {
    public static final int ELASTICSEARCH_MAX_HITS = 10000;

    public static final String BLOCK_IDS_INDEX = "block_ids";
    public static final String BLOCK_CONTENT_INDEX = "blocks";
    public static final String TRANSACTION_INDEX = "txn_infos";
    public static final String UNCLE_BLOCK_INDEX = "uncle_blocks";
    public static final String PENDING_TXN_INDEX = "pending_txns";
    public static final String EVENT_INDEX = "txn_events";
    public static final String TRANSFER_INDEX = "transfer";
    public static final String ADDRESS_INDEX = "address_holder";
    public static final String TRANSFER_JOURNAL_INDEX = "transfer_journal";
    public static final String MARKET_CAP_INDEX = "market_cap";


    public static final String EVENT_FILTER_ADDRESS = "0x00000000000000000000000000000001";
    public static final String EVENT_FILTER__MODULE = "Account";
    public static final String DEPOSIT_EVENT = "DepositEvent";
    public static final String WITHDRAW_EVENT = "WithdrawEvent";
}
