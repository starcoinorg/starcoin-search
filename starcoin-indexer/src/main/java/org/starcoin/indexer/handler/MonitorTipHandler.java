package org.starcoin.indexer.handler;

import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Service;
import org.starcoin.api.BlockRPCClient;
import org.starcoin.bean.BlockOffset;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

@Service
public class MonitorTipHandler extends QuartzJobBean {

    static final long MAX_DIFF = 2000;

    static final String FROM = "nkysggsy@gmail.com";
    static final String TO = "nkysggsy@gmail.com";
    static final String SUBJECT = "starcoin-search monitor tip";


    private static final Logger logger = LoggerFactory.getLogger(MonitorTipHandler.class);

    @Autowired
    private ElasticSearchHandler elasticSearchHandler;

    @Autowired
    private BlockRPCClient blockRPCClient;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            long currentMasterNumber =  blockRPCClient.getChainHeader().getHeight();
            BlockOffset remoteBlockOffset = elasticSearchHandler.getRemoteOffset();
            logger.info("current master offset {} remote offset: {}", currentMasterNumber, remoteBlockOffset);
            if (remoteBlockOffset == null) {
                logger.warn("offset must not null, please check blocks.mapping!!");
                return;
            }
            long elasticHeight = remoteBlockOffset.getBlockHeight();
            // if (currentMasterNumber > elasticHeight + MAX_DIFF) {
            logger.info("current master offset {} remote offset: {}", currentMasterNumber, elasticHeight);
            sendEmail(currentMasterNumber, elasticHeight);
        } catch (JSONRPC2SessionException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendEmail(long masterHeight, long elasticHeight) {
        // Construct an object to contain the recipient address.
        Destination destination = new Destination().withToAddresses(new String[]{TO});

        // Create the subject and body of the message.
        Content subject = new Content().withData(SUBJECT);
        String str = String.format("masterHeight %d, elasticHeight %d", masterHeight,elasticHeight);
        Content textBody = new Content().withData(str);
        Body body = new Body().withText(textBody);

        // Create a message with the specified subject and body.
        Message message = new Message().withSubject(subject).withBody(body);

        // Assemble the email.
        SendEmailRequest request = new SendEmailRequest().withSource(FROM).withDestination(destination).withMessage(message);

        try {
            logger.info("Attempting to send an email through Amazon SES by using the AWS SDK for Java...");

            /*
             * The ProfileCredentialsProvider will return your [default]
             * credential profile by reading from the credentials file located at
             * (~/.aws/credentials).
             *
             * TransferManager manages a pool of threads, so we create a
             * single instance and share it throughout our application.
             */
            AWSCredentials credentials = null;
            try {
                credentials = new ProfileCredentialsProvider().getCredentials();
            } catch (Exception e) {
                throw new AmazonClientException(
                        "Cannot load the credentials from the credential profiles file. " +
                                "Please make sure that your credentials file is at the correct " +
                                "location (~/.aws/credentials), and is in valid format.",
                        e);
            }

            // Instantiate an Amazon SES client, which will make the service call with the supplied AWS credentials.
            // Choose the AWS region of the Amazon SES endpoint you want to connect to. Note that your production
            // access status, sending limits, and Amazon SES identity-related settings are specific to a given
            // AWS region, so be sure to select an AWS region in which you set up Amazon SES. Here, we are using
            // the Asia Pacific (Tokyo) region. For a complete list, see http://docs.aws.amazon.com/ses/latest/DeveloperGuide/regions.html
            AmazonSimpleEmailService client =
                    AmazonSimpleEmailServiceClientBuilder.standard()
                            .withCredentials(new AWSStaticCredentialsProvider(credentials))
                            .withRegion(Regions.AP_NORTHEAST_1)
                            .build();

            // Send the email.
            client.sendEmail(request);
            logger.info("Email sent!");

        } catch (Exception ex) {
            logger.info("The email was not sent.");
            logger.info("Error message: " + ex.getMessage());
        }
    }
}
