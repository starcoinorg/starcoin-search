package org.starcoin.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.io.File;

@Configuration
class ElasticsearchConfiguration {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.protocol}")
    private String protocol;

    @Value("${elasticsearch.username}")
    private String userName;

    @Value("${jasypt.encryptor.password}")
    private String password;

    @Value("${elasticsearch.connTimeout}")
    private int connTimeout;

    @Value("${elasticsearch.socketTimeout}")
    private int socketTimeout;

    @Value("${elasticsearch.connectionRequestTimeout}")
    private int connectionRequestTimeout;

    @Value("${elasticsearch.truststore.path:}")
    private String truststorePath;

    @Value("${elasticsearch.truststore.password:changeit}")
    private String truststorePassword;

    @Bean(destroyMethod = "close", name = "client")
    public RestHighLevelClient initRestClient() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(userName, password));
        
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, protocol))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    
                    // Configure SSL if using HTTPS
                    if ("https".equalsIgnoreCase(protocol)) {
                        try {
                            SSLContext sslContext;
                            if (truststorePath != null && !truststorePath.isEmpty() && new File(truststorePath).exists()) {
                                // Use custom truststore
                                sslContext = SSLContexts.custom()
                                        .loadTrustMaterial(new File(truststorePath), truststorePassword.toCharArray())
                                        .build();
                            } else {
                                // Use default SSL context (for development/testing)
                                sslContext = SSLContexts.createDefault();
                            }
                            httpClientBuilder.setSSLContext(sslContext);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to configure SSL context", e);
                        }
                    }
                    
                    return httpClientBuilder;
                })
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(connTimeout)
                        .setSocketTimeout(socketTimeout)
                        .setConnectionRequestTimeout(connectionRequestTimeout));
        return new RestHighLevelClient(builder);
    }
}
