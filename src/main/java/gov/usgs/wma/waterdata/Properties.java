package gov.usgs.wma.waterdata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="properties")
public class Properties {
	String snsTopicArn;

	public String getSnsTopicArn() {
		return snsTopicArn;
	}

	public void setSnsTopicArn(String snsTopicArn) {
		this.snsTopicArn = snsTopicArn;
	}
}
