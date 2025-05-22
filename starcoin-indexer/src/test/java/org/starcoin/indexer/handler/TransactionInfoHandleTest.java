package org.starcoin.indexer.handler;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.Transaction;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionInfoHandleTest {

    private RestHighLevelClient realElasticsearchClient;
    private TransactionRPCClient transactionRPCClient;
    private TransactionInfoHandle transactionInfoHandle;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        // Create real Elasticsearch client with authentication
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        // Get credentials from environment variables or properties file
        String username = System.getenv("ES_USERNAME") != null ? System.getenv("ES_USERNAME") : "elastic";
        String password = System.getenv("ES_PASSWORD") != null ? System.getenv("ES_PASSWORD") : "testpassword";
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        // Add request interceptor to log raw request data
        HttpRequestInterceptor requestInterceptor = (request, context) -> {
            System.out.println("=== ES Request Details ===");
            System.out.println("Method: " + request.getRequestLine().getMethod());
            System.out.println("URI: " + request.getRequestLine().getUri());
            System.out.println("Headers: " + request.getAllHeaders());
            
            if (request instanceof org.apache.http.HttpEntityEnclosingRequest) {
                org.apache.http.HttpEntityEnclosingRequest entityRequest = (org.apache.http.HttpEntityEnclosingRequest) request;
                HttpEntity entity = entityRequest.getEntity();
                if (entity != null) {
                    String requestBody = EntityUtils.toString(entity);
                    System.out.println("Request Body: " + requestBody);
                    
                    // Create new entity with application/json content type
                    StringEntity newEntity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    entityRequest.setEntity(newEntity);
                }
            }
            System.out.println("=======================");
        };

        RestClientBuilder builder = RestClient.builder(
            new HttpHost("localhost", 9200, "http"))
            .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                @Override
                public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                    return httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .addInterceptorFirst(requestInterceptor);
                }
            });
        
        realElasticsearchClient = new RestHighLevelClient(builder);
        
        // Initialize TransactionInfoHandle with real ES client
        transactionInfoHandle = new TransactionInfoHandle(realElasticsearchClient);

        transactionRPCClient = new TransactionRPCClient(new URL("http://main.seed.starcoin.org:9850"));
        ReflectionTestUtils.setField(transactionInfoHandle, "transactionRPCClient", transactionRPCClient);
        ReflectionTestUtils.setField(transactionInfoHandle, "globalIndex", 25349659L);
        ReflectionTestUtils.setField(transactionInfoHandle, "network", "main.0727");
    }

    @AfterEach
    void tearDown() throws IOException {
        realElasticsearchClient.close();
    }

    @Test
    void testDebugSingleTransaction() {
        // Get transaction data for specific index
        List<Transaction> transactions = Assertions.assertDoesNotThrow(
            () -> transactionRPCClient.getTransactionInfos(25349659L, true, 1),
            "Failed to get transaction info from RPC"
        );
        
        assertFalse(transactions.isEmpty(), "Transaction list should not be empty");

        Transaction transaction = transactions.get(0);
        System.out.println("Transaction Hash: " + transaction.getTransactionHash());
        System.out.println("Global Index: " + transaction.getTransactionGlobalIndex());

        BulkRequest bulkRequest = new BulkRequest();
        transactionInfoHandle.handleSingleTransaction(transaction, bulkRequest);

        // Test with real ES client
        BulkResponse realResponse = Assertions.assertDoesNotThrow(
            () -> realElasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT),
            "Failed to execute bulk request to Elasticsearch"
        );

        if (realResponse.hasFailures()) {
            StringBuilder errorMessage = new StringBuilder("Bulk operation failed:\n");
            for (BulkItemResponse item : realResponse.getItems()) {
                if (item.isFailed()) {
                    errorMessage.append(String.format(
                        "Index: %s, Type: %s, Id: %s, Failure: %s\n",
                        item.getIndex(),
                        item.getType(),
                        item.getId(),
                        item.getFailureMessage()
                    ));
                }
            }
            fail(errorMessage.toString());
        }
    }
} 