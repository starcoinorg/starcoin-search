package org.starcoin.indexer.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.Block;
import org.starcoin.bean.BlockHeader;
import org.starcoin.bean.BlockOffset;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IndexerHandleTest {

    @Mock
    private ElasticSearchHandler elasticSearchHandler;

    @Mock
    private TransactionRPCClient transactionRPCClient;

    @Mock
    private BlockRPCClient blockRPCClient;

    @InjectMocks
    private IndexerHandle indexerHandle;

    private Block mockBlock;
    private BlockHeader mockHeader;
    private BlockOffset mockOffset;

    @Before
    public void setUp() {
        // Setup mock block and header
        mockHeader = new BlockHeader();
        mockHeader.setHeight(100L);
        mockHeader.setBlockHash("blockHash");
        mockHeader.setParentHash("parentHash");

        mockBlock = new Block();
        mockBlock.setHeader(mockHeader);

        mockOffset = new BlockOffset(100L, "blockHash");

        // Set network parameter
        ReflectionTestUtils.setField(indexerHandle, "network", "main");
        ReflectionTestUtils.setField(indexerHandle, "bulkSize", 10L);
    }

    @Test
    public void testNormalFlow() throws JSONRPC2SessionException, JsonProcessingException {
        // Setup
        when(elasticSearchHandler.getRemoteOffset()).thenReturn(mockOffset);
        when(blockRPCClient.getBlockByHeight(anyLong())).thenReturn(mockBlock);
        when(blockRPCClient.getChainHeader()).thenReturn(mockHeader);

        // Execute
        indexerHandle.initOffset();
        indexerHandle.executeInternal(null);

        // Verify
        verify(elasticSearchHandler, times(2)).getRemoteOffset();
        verify(blockRPCClient, times(1)).getBlockByHeight(100L);
        
        // Capture and verify the offset being set
        ArgumentCaptor<BlockOffset> offsetCaptor = ArgumentCaptor.forClass(BlockOffset.class);
        verify(elasticSearchHandler, times(1)).setRemoteOffset(offsetCaptor.capture());
        BlockOffset capturedOffset = offsetCaptor.getValue();
        assertEquals(100L, capturedOffset.getBlockHeight());
        assertEquals("blockHash", capturedOffset.getBlockHash());
    }

    @Test
    public void testResetToGenesis() throws JSONRPC2SessionException {
        // Setup - simulate null offset
        when(elasticSearchHandler.getRemoteOffset()).thenReturn(null);
        when(blockRPCClient.getBlockByHeight(0L)).thenReturn(mockBlock);

        // Execute
        indexerHandle.initOffset();

        // Verify
        verify(elasticSearchHandler, times(1)).getRemoteOffset();
        verify(blockRPCClient, times(1)).getBlockByHeight(0L);
        
        // Capture and verify the offset being set
        ArgumentCaptor<BlockOffset> offsetCaptor = ArgumentCaptor.forClass(BlockOffset.class);
        verify(elasticSearchHandler, times(1)).setRemoteOffset(offsetCaptor.capture());
        BlockOffset capturedOffset = offsetCaptor.getValue();
        assertEquals(0L, capturedOffset.getBlockHeight());
        assertEquals("blockHash", capturedOffset.getBlockHash());
    }

    @Test
    public void testForkHandling() throws JSONRPC2SessionException, JsonProcessingException {
        // Setup
        BlockOffset currentOffset = new BlockOffset(100L, "oldHash");
        BlockHeader chainHeader = new BlockHeader();
        chainHeader.setHeight(101L);
        
        // Create a new block that will cause a fork
        Block newBlock = new Block();
        BlockHeader newHeader = new BlockHeader();
        newHeader.setHeight(101L);
        newHeader.setParentHash("differentParentHash"); // Different from current parent hash
        newBlock.setHeader(newHeader);

        // Create a fork block that will be found during rollback
        Block forkBlock = new Block();
        BlockHeader forkHeader = new BlockHeader();
        forkHeader.setHeight(100L);
        forkHeader.setBlockHash("oldHash");
        forkHeader.setParentHash("parentHash");
        forkBlock.setHeader(forkHeader);

        // Create a master block that will be found during rollback
        Block masterBlock = new Block();
        BlockHeader masterHeader = new BlockHeader();
        masterHeader.setHeight(99L);
        masterHeader.setBlockHash("parentHash");
        masterHeader.setParentHash("grandParentHash");
        masterBlock.setHeader(masterHeader);

        // Setup mock behavior
        when(elasticSearchHandler.getRemoteOffset()).thenReturn(currentOffset);
        when(blockRPCClient.getBlockByHeight(100L)).thenReturn(forkBlock);
        when(blockRPCClient.getBlockByHeight(101L)).thenReturn(newBlock);
        when(blockRPCClient.getBlockByHeight(99L)).thenReturn(masterBlock);
        when(blockRPCClient.getChainHeader()).thenReturn(chainHeader);
        when(elasticSearchHandler.getBlockContent(eq("oldHash"))).thenReturn(forkBlock);
        when(elasticSearchHandler.getBlockContent(eq("parentHash"))).thenReturn(masterBlock);
        when(blockRPCClient.getBlockByHash(eq("oldHash"))).thenReturn(forkBlock);
        when(blockRPCClient.getBlockByHash(eq("parentHash"))).thenReturn(masterBlock);

        // Execute
        indexerHandle.initOffset();
        indexerHandle.executeInternal(null);

        // Verify
        verify(elasticSearchHandler, times(2)).getRemoteOffset();
        
        // Capture and verify the forked block being updated
        ArgumentCaptor<Block> forkedBlockCaptor = ArgumentCaptor.forClass(Block.class);
        verify(elasticSearchHandler, times(1)).bulkForkedUpdate(forkedBlockCaptor.capture());
        Block capturedForkedBlock = forkedBlockCaptor.getValue();
        assertNotNull(capturedForkedBlock);
        assertEquals(100L, capturedForkedBlock.getHeader().getHeight());
        assertEquals("oldHash", capturedForkedBlock.getHeader().getBlockHash());
        assertEquals("parentHash", capturedForkedBlock.getHeader().getParentHash());
        
        // Capture and verify the offset being set
        ArgumentCaptor<BlockOffset> offsetCaptor = ArgumentCaptor.forClass(BlockOffset.class);
        verify(elasticSearchHandler, times(1)).setRemoteOffset(offsetCaptor.capture());
        BlockOffset capturedOffset = offsetCaptor.getValue();
        assertEquals(100L, capturedOffset.getBlockHeight());
        assertEquals("oldHash", capturedOffset.getBlockHash());
    }

    @Test
    public void testNullBlockHandling() throws JSONRPC2SessionException {
        // Setup
        when(elasticSearchHandler.getRemoteOffset()).thenReturn(mockOffset);
        when(blockRPCClient.getBlockByHeight(anyLong())).thenReturn(null);

        // Execute
        indexerHandle.initOffset();

        // Verify
        verify(elasticSearchHandler, times(1)).getRemoteOffset();
        verify(blockRPCClient, times(1)).getBlockByHeight(100L);
        // Should not update offset when block is null
        verify(elasticSearchHandler, never()).setRemoteOffset(any(BlockOffset.class));
    }

    @Test
    public void testRPCExceptionHandling() throws JSONRPC2SessionException {
        // Setup
        when(elasticSearchHandler.getRemoteOffset()).thenReturn(mockOffset);
        JSONRPC2SessionException expectedException = new JSONRPC2SessionException("RPC Error");
        when(blockRPCClient.getBlockByHeight(anyLong())).thenThrow(expectedException);

        // Execute and verify exception
        JSONRPC2SessionException thrown = assertThrows(
            JSONRPC2SessionException.class,
            () -> indexerHandle.initOffset()
        );

        // Verify exception details
        assertEquals("RPC Error", thrown.getMessage());
        
        // Verify interactions
        verify(elasticSearchHandler, times(1)).getRemoteOffset();
        verify(blockRPCClient, times(1)).getBlockByHeight(100L);
        // Should not update offset when RPC exception occurs
        verify(elasticSearchHandler, never()).setRemoteOffset(any(BlockOffset.class));
    }

    @Test
    public void testMainNetworkNullOffsetHandling() throws JSONRPC2SessionException {
        // Setup
        ReflectionTestUtils.setField(indexerHandle, "network", "main");
        when(elasticSearchHandler.getRemoteOffset()).thenReturn(null);
        when(blockRPCClient.getBlockByHeight(0L)).thenReturn(mockBlock);

        // Execute
        indexerHandle.initOffset();
        indexerHandle.executeInternal(null);

        // Verify
        verify(elasticSearchHandler, times(2)).getRemoteOffset();
        verify(blockRPCClient, times(1)).getBlockByHeight(0L);
        verify(elasticSearchHandler, times(1)).setRemoteOffset(any(BlockOffset.class));
    }

    @Test
    public void testNonMainNetworkNullOffsetHandling() throws JSONRPC2SessionException {
        // Setup
        ReflectionTestUtils.setField(indexerHandle, "network", "test");
        when(elasticSearchHandler.getRemoteOffset()).thenReturn(null);
        when(blockRPCClient.getBlockByHeight(0L)).thenReturn(mockBlock);

        // Execute
        indexerHandle.initOffset();
        indexerHandle.executeInternal(null);

        // Verify
        verify(elasticSearchHandler, times(2)).getRemoteOffset();
        verify(blockRPCClient, times(1)).getBlockByHeight(0L);
        verify(elasticSearchHandler, times(1)).setRemoteOffset(any(BlockOffset.class));
    }
} 