package gov.usgs.wma.waterdata;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.Topic;
import java.util.Base64;
import org.springframework.stereotype.Component;


/**
 * Manager class for SNS actions.
 */
@Component
public class SnSUtil {
	private static final String appState = "TEST";
	private static final String TOPIC_BASE_NAME = "SNS-WARNING-";
	private static final String TOPIC_NAME = "SNS-WARNING-TOPIC-" + appState;
	private final AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();
	private final Topic snsTopic;
	//private static final Logger logger = LoggerFactory.getLogger(LoadDiscreteGroundWater.class);
	//private final String snsTopicName;


	SnSUtil() {
		//this.snsTopic = getSNSTopic();
		this.snsTopic = getSecret(TOPIC_NAME);
		
	}

	/**
	 * Simple helper method to send a message to the etl discrete groundwater rdb
	 * SNS topic. Exceptions are caught and logged to standard error, so that the rdb
	 * processing continues.
	 *
	 * @param mess The message to place in the SNS topic.
	 */
	public void publishSNSMessage(String mess) {
		if (snsTopic != null) {
			try {
				PublishRequest request = new PublishRequest(snsTopic.getTopicArn(), mess);
				System.out.println("returned arn: " + snsTopic.getTopicArn());
				PublishResult publishResult = snsClient.publish(request);
				System.out.println(publishResult.toString());
				System.out.println("INFO: Message published to SNS: " + mess);
			} catch (Exception e) {
				System.err.print("Error publishing message to SNS topic: " + e.getMessage());
				System.err.print("Message to have been sent: " + mess);
				e.printStackTrace();
			}
		} else {
			System.err.print("Error SNS logging not initialized, message to have been sent: " + mess);
		}
	}

    private Topic getSecret(String topicName) {
		String secretName = topicName;
		String region = "us-west-2";
		System.out.println("Entered Get Secret");
		// Create a Secrets Manager client
		AWSSecretsManager client  = AWSSecretsManagerClientBuilder.standard()
										.withRegion(region)
										.build();
		
		// In this sample we only handle the specific exceptions for the 'GetSecretValue' API.
		// See https://docs.aws.amazon.com/secretsmanager/latest/astatepireference/API_GetSecretValue.html
		// We rethrow the exception by default.
		
		String secret, decodedBinarySecret;
		Topic secretArn = null;
		Topic snsTopic = null;
		secret="";
		GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
						.withSecretId(secretName);
		GetSecretValueResult getSecretValueResult = null;

		getSecretValueResult = client.getSecretValue(getSecretValueRequest);
		

		// Decrypts secret using the associated KMS CMK.
		// Depending on whether the secret is a string or binary, one of these fields will be populated.
		if (getSecretValueResult.getSecretString() != null) {
			secret = getSecretValueResult.getSecretString();
			System.out.println("Get Secret return" + secret);
			//logger.debug("Get Secret return" + secret);
			snsTopic= new Topic();
			snsTopic.setTopicArn(secret);
			System.err.println("Get Secret return: " + secret);
		}
		else {
			decodedBinarySecret = new String(Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());
		}
		
		return snsTopic;
	}

}