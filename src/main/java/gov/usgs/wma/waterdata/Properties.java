package gov.usgs.wma.waterdata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="properties")
public class Properties {
	String tier;
	String bucket;
	String region;
	String snsTopicArn;

	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getSnsTopicArn() {
		return snsTopicArn;
	}

	public void setSnsTopicArn(String snsTopicArn) {
		this.snsTopicArn = snsTopicArn;
	}
}
