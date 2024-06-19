package org.starcoin.scan.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.starcoin.scan.ScanApplication;
import org.starcoin.scan.service.vo.DIBlocksAndEdgesAndHeightGroupsVo;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ScanApplication.class)
@TestPropertySource(locations = "classpath:application.properties")
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
