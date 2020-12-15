package gov.usgs.wma.waterdata;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


/**
 * Manager class for SNS actions.
 */
@Component
public class SnSUtil {
	private static final String appState = "TEST";
	private static final String TOPIC_BASE_NAME = "SNS-WARNING-";
	private static final String TOPIC_NAME = "SNS-WARNING-TOPIC-" + appState;
	private static final String SNS_TOPIC_ENV = "ATQS_CAPTURE_SNS_LOG_ARN";
	@Autowired
	private Properties properties;
	@Autowired
	private AmazonSNS snsClient;

	SnSUtil(Properties properties) {
		this.properties = properties;
	}

	/**
	 * Simple helper method to send a message to the etl discrete groundwater rdb
	 * SNS topic. Exceptions are caught and logged to standard error, so that the rdb
	 * processing continues.
	 *
	 * @param mess The message to place in the SNS topic.
	 */
	public void publishSNSMessage(String mess) {
		String snsArn = properties.getSnsTopicArn();
	//	String snsArn = System.getenv(SNS_TOPIC_ENV);
		System.out.println("Properties=" + snsArn);
		System.out.println("snsClient=" + snsClient);
		System.out.println("get class: " + snsClient.getClass());
		
		if (StringUtils.hasText(snsArn)) {
			try {
				//snsClient= AmazonSNSClientBuilder.defaultClient();
				PublishRequest request = new PublishRequest(snsArn, mess);
				PublishResult publishResult = snsClient.publish(request);
				//System.out.println(publishResult.toString());
				System.out.println("INFO: Message published to SNS: " + mess);
			} catch (Exception e) {
				System.err.print("Error publishing message to SNS topic: " + e.getMessage());
				System.err.print("Message to have been sent: " + mess);
				e.printStackTrace();
			}
		} else {
			System.err.print("Error SNS Topic not set, message to have been sent: " + mess);
		}
	}


}