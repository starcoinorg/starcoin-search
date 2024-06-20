package org.starcoin.scan.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.starcoin.scan.ScanApplication;
import org.starcoin.scan.service.vo.DIBlocksAndEdgesAndHeightGroupsVo;

import java.io.IOException;

@SpringBootTest(classes = ScanApplication.class)
@TestPropertySource(locations = "classpath:application-unittest.properties")
public class DagInspectServiceTest {

    final static String TEST_NETWORK = "halley";

    @Autowired
    DagInspectorService dagInspectorService;

    @Test
    public void testGetBlocksAndEdgesAndHeightGroups() throws IOException {
        DIBlocksAndEdgesAndHeightGroupsVo vo = dagInspectorService.getBlocksAndEdgesAndHeightGroups(TEST_NETWORK, 10000L, 10050L);
        Assertions.assertNotEquals(0, vo.getBlocks().size());
        Assertions.assertNotEquals(0, vo.getEdges().size());
        Assertions.assertNotEquals(0, vo.getHeightGroups().size());
    }

    @Test
    public void testGetBlockHash() throws IOException {
        DIBlocksAndEdgesAndHeightGroupsVo vo = dagInspectorService.getBlockHash(
                TEST_NETWORK,
                "0x93a4fc71929be2e435efe682d02260f8dd46824fe90e926e3b3ea5839f31e67c",
                10
        );
        Assertions.assertNotEquals(0, vo.getBlocks().size());
        Assertions.assertNotEquals(0, vo.getEdges().size());
        Assertions.assertNotEquals(0, vo.getHeightGroups().size());
        System.out.println(vo);
    }

    @Test
    public void testGetBlockDAAScore() throws IOException {
        DIBlocksAndEdgesAndHeightGroupsVo vo = dagInspectorService.getBlockDAAScore(TEST_NETWORK, 10409, 1);
        Assertions.assertNotEquals(0, vo.getBlocks().size());
        Assertions.assertNotEquals(0, vo.getEdges().size());
        Assertions.assertNotEquals(0, vo.getHeightGroups().size());
        System.out.println(vo);
    }


    @Test
    public void testGetHead() throws IOException {
        DIBlocksAndEdgesAndHeightGroupsVo vo = dagInspectorService.getHead(TEST_NETWORK, 100);
        Assertions.assertNotEquals(0, vo.getBlocks().size());
        Assertions.assertNotEquals(0, vo.getEdges().size());
        Assertions.assertNotEquals(0, vo.getHeightGroups().size());
        System.out.println(vo);
    }
}
