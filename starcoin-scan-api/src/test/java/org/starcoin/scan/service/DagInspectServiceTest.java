package org.starcoin.scan.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.starcoin.scan.service.vo.DIBlocksAndEdgesAndHeightGroupsVo;

public class DagInspectServiceTest {

    final static String TEST_NETWORK = "halley";

    @Autowired
    DagInspectorService dagInspectorService;

    @Test
    public void testGetBlocksAndEdgesAndHeightGroups() {
        DIBlocksAndEdgesAndHeightGroupsVo vo = dagInspectorService.getBlocksAndEdgesAndHeightGroups(TEST_NETWORK, 0L, 100L);
        Assertions.assertNotEquals(0, vo.getBlocks().size());
        Assertions.assertNotEquals(0, vo.getEdges().size());
        Assertions.assertNotEquals(0, vo.getHeightGroups().size());
    }
}
