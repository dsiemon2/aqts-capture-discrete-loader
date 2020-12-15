package gov.usgs.wma.waterdata;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;


@Configuration
public class SpringConfig {
	@Bean
	@Qualifier("AmazonSNS")
    public AmazonSNS snsClient() {
		return AmazonSNSClientBuilder.defaultClient();
	}
}
