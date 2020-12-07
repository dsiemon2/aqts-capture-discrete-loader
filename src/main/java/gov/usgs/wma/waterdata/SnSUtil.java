package gov.usgs.wma.waterdata;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.DecryptionFailureException;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.InternalServiceErrorException;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.amazonaws.services.secretsmanager.model.InvalidRequestException;
import com.amazonaws.services.secretsmanager.model.InvalidParameterException;

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
	//private final String snsTopicName;
	private Properties properties;

	@Autowired
	SnSUtil(Properties properties) {
		this.properties = properties;
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
				snsClient.publish(request);
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

	private Topic getSNSTopic() {
		Topic snsTopic = null;
		String tier = properties == null ? null : properties.getTier();
		String mess = "";

		if (StringUtils.hasText(tier)) {
			try {
				String topicName = String.format("%s-%s-topic", TOPIC_BASE_NAME, tier);
				ListTopicsRequest request = new ListTopicsRequest();

				ListTopicsResult result = snsClient.listTopics(request);
				for (Topic topic : result.getTopics()) {
					String arn = topic.getTopicArn();
					System.err.println("ARN = " + arn);
					if (arn != null && arn.contains(topicName)) {
						snsTopic = topic;
						break;
					}
				}

				if (snsTopic == null) {
					System.err.println("Error initializing SNS logging: SNS topic not found: " + topicName);
				}
			} catch (Exception e) {
				System.err.println("Error getting SNS topic: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			// Todo: use logging framework
			mess = properties == null ? "properties component not available" : "tier property not set";
			System.err.print("Error initializing SNS logging: " + mess);
		}

		return snsTopic;
	}

    private Topic getSecret(String appState) {

		String secretName = "SNS-WARNING-TOPIC-" + appState;
		String region = "us-west-2";

		// Create a Secrets Manager client
		AWSSecretsManager client  = AWSSecretsManagerClientBuilder.standard()
										.withRegion(region)
										.build();
		
		// In this sample we only handle the specific exceptions for the 'GetSecretValue' API.
		// See https://docs.aws.amazon.com/secretsmanager/latest/astatepireference/API_GetSecretValue.html
		// We rethrow the exception by default.
		
		String secret, decodedBinarySecret;
		Topic SecretArn = null;
		secret="";
		GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
						.withSecretId(secretName);
		GetSecretValueResult getSecretValueResult = null;

		getSecretValueResult = client.getSecretValue(getSecretValueRequest);
		

		// Decrypts secret using the associated KMS CMK.
		// Depending on whether the secret is a string or binary, one of these fields will be populated.
		if (getSecretValueResult.getSecretString() != null) {
			secret = getSecretValueResult.getSecretString();
			SecretArn.setTopicArn(secret);
		}
		else {
			decodedBinarySecret = new String(Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());
		}
		
		return SecretArn;
	}

}