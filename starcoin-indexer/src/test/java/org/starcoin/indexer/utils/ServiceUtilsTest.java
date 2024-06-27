package org.starcoin.indexer.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.Block;
import org.starcoin.bean.BlockHeader;
import org.starcoin.indexer.handler.ServiceUtils;
import org.starcoin.indexer.test.IndexerLogicBaseTest;
import org.starcoin.utils.ExceptionWrap;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceUtilsTest extends IndexerLogicBaseTest {

    @Autowired
    private TransactionRPCClient transactionRPCClient;

    @Test
    void testFetchTransactionsForBlock() {
        List<String> blockHashes = Arrays.asList(
                "0x6590d6769e9837e5fbc116daf53efa809f996e16cf55d4a713695094ac79aada",
                "0x6b7ca9e9a1aa3f414caab285f214179108d5cf3fc2864efd9e7b5ed4461e7e80",
                "0x042218a4750e696942fab2a875479a77f7c3c48049acb400a80c77b24aa06d36",
                "0x721c15b57a22687dd90a439d85918f92f49937eef48c7c7afb72a7da43845589",
                "0x9a68fd546aec8fdad955c0626cb9b20be121f932bb69f8a523f029248b125f51",
                "0x73c0d5b604e130dedf0ecb77102e91de521bf38097b80d3e64c7ed58bc85c232",
                "0x9161c9b51a7817cc2a18c190c99b40e96cb7cf4666ad63a7ffa33b2671c3aa31",
                "0x020aadcabfbd57cc4c9059d16a91f8bc944b6c5c110d4fc72e6c346658035ecc"
        );
        List<Block> blocks = blockHashes.stream().map(ExceptionWrap.wrap(blockHash -> {
            Block block = new Block();
            BlockHeader header = new BlockHeader();
            header.setBlockHash(blockHash);
            block.setHeader(header);
            ServiceUtils.fetchTransactionsForBlock(transactionRPCClient, block);
            System.out.println("Block: " + blockHash);
            return block;
        })).collect(Collectors.toList());
        blocks.forEach(System.out::println);

    }
}
