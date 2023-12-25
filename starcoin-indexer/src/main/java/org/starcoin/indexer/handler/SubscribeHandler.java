package org.starcoin.indexer.handler;

import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starcoin.api.StarcoinSubscriber;
import org.starcoin.api.TransactionRPCClient;
import org.starcoin.bean.PendingTransaction;
import org.starcoin.bean.PendingTransactionNotification;
import org.web3j.protocol.websocket.WebSocketService;
import org.starcoin.jsonrpc.client.JSONRPC2SessionException;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;

public class SubscribeHandler implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(SubscribeHandler.class);

    private String seedHost;
    private ElasticSearchHandler elasticSearchHandler;

    public SubscribeHandler(String seedHost, ElasticSearchHandler elasticSearchHandler) {
        this.seedHost = seedHost;
        this.elasticSearchHandler = elasticSearchHandler;
    }

    @Override
    public void run() {
        try {
            WebSocketService service = new WebSocketService("ws://" + seedHost + ":9870", true);
            service.connect();
            StarcoinSubscriber subscriber = new StarcoinSubscriber(service);
            Flowable<PendingTransactionNotification> flowableTxns = subscriber.newPendingTransactionsNotifications();
            TransactionRPCClient rpc = new TransactionRPCClient(new URL("http://" + seedHost + ":9850"));

            for (PendingTransactionNotification notifications : flowableTxns.blockingIterable()) {
                for (String notification : notifications.getParams().getResult()) {
                    logger.info("notification: {}", notification);
                    PendingTransaction transaction = rpc.getPendingTransaction(notification);
                    elasticSearchHandler.savePendingTransaction(transaction);
                }
            }
        } catch (ConnectException | MalformedURLException | JSONRPC2SessionException e) {
            logger.error("handle subscribe exception:", e);
        }
    }
}
