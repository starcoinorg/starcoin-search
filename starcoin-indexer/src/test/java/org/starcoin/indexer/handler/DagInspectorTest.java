package org.starcoin.indexer.handler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.starcoin.bean.Block;
import org.starcoin.bean.BlockHeader;
import org.starcoin.indexer.test.IndexerLogicBaseTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class DagInspectorTest extends IndexerLogicBaseTest {
    @Autowired
    private DagInspectorIndexerHandler dagInspectorHandler;

    Block new_test_block(String block_hash, Long height, String parent_hash, List<String> parents_hash) {
        Block block = new Block();
        BlockHeader header = new BlockHeader();
        header.setHeight(height);
        header.setBlockHash(block_hash);
        header.setParentHash(parent_hash);
        header.setParentsHash(new Vector<>(parents_hash));
        block.setHeader(header);
        return block;
    }

    /**
     * Test of testUpsertFromBlocks method, of class DagInspector.
     */
    @Test
    public void testTestUpsertFromBlocks() throws Exception {

        // Initialize blocks
        // Block 1 (Genesis)
        // Block 1 <- Block 2
        // Block 1 <- Block 3
        // Block 2 <- Block 4
        // Block 3 <- Block 4
        // Block 4 <- Block 5
        // Block 2 <- Block 6
        // Block 3 <- Block 6
        List<Block> blockList = new ArrayList<>();
        blockList.add(new_test_block("block_hash_1", 1L, "", new Vector<>()));
        blockList.add(new_test_block("block_hash_2", 2L, "block_hash_1", List.of("block_hash_1")));
        blockList.add(new_test_block("block_hash_3", 2L, "block_hash_1", List.of("block_hash_1")));
        blockList.add(new_test_block("block_hash_4", 3L, "block_hash_3", List.of("block_hash_3")));
        blockList.add(new_test_block("block_hash_5", 4L, "block_hash_4", List.of("block_hash_4")));
        blockList.add(new_test_block("block_hash_6", 5L, "block_hash_2", List.of("block_hash_2", "block_hash_3")));
        dagInspectorHandler.upsertDagInfoFromBlocks(blockList);
    }


}
