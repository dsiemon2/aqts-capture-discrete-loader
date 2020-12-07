package gov.usgs.wma.waterdata;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.Topic;



/**
 * Applies business rules to a domain object.
 * It is assumed that this object can be modified.
 */
public class DiscreteGroundWaterRules {
	@Autowired
	protected SnsUtil snsUtil;
	@Autowired
	protected Properties properties;
	//Threadsafe factory
	private static final JsonFactory jsonFactory = new JsonFactory();
	

	/**
	 * Apply business rules to a DiscreteGroundWater, modifying it in place.
	 * @param domObj
	 */
	public void apply(DiscreteGroundWater domObj) {
		String mess = "";
		//Rule:  The DateTime accuracy is read from a standardized format in the field_visit_comment.
		//If the unrecognized or unspecified, it is assumed to be MINUTE.
		//Ref:  https://internal.cida.usgs.gov/jira/browse/IOW-558 (read from comment)
		//Ref:  https://internal.cida.usgs.gov/jira/browse/IOW-652 (default to MINUTE)
		{
			DateTimeAccuracy dta = DateTimeAccuracy.parse(domObj.getFieldVisitComments());

			if (dta.isReal()) {

				domObj.setDateTimeAccuracyCode(dta.getCode());
				domObj.setDateTimeAccuracyText(dta.getText());
			} else {

				dta = DateTimeAccuracy.MINUTE;
				domObj.setDateTimeAccuracyCode(dta.getCode());
				domObj.setDateTimeAccuracyText(dta.getText());
			}
		}

		//Data Quality:  The Reading Qualifiers are a list encoded in JSON, but sometimes individual values are not trimmed.
		{
			String qualStr = domObj.getReadingQualifiers();

			qualStr = StringUtils.trimWhitespace(qualStr);

			if (! StringUtils.isEmpty(qualStr)) {
				try {

					List<String> vals = new ArrayList();

					JsonParser parser = jsonFactory.createParser(qualStr);

					while (! parser.isClosed()) {

						JsonToken token = parser.nextToken();

						if (token != null && token.isScalarValue()) {
							String val = StringUtils.trimWhitespace(parser.getValueAsString());

							if (!StringUtils.isEmpty(val)) {
								vals.add(StringUtils.trimWhitespace(parser.getValueAsString()));
							}
						}
					}

					if (vals.size() > 0) {
						StringWriter writer = new StringWriter();

						JsonGenerator gen = jsonFactory.createGenerator(writer);

						gen.writeArray(vals.toArray(new String[vals.size()]), 0, vals.size());
						gen.flush();

						qualStr = writer.toString();

					} else {
						qualStr = null;
					}

					domObj.setReadingQualifiers(qualStr);
				} catch (IOException e) {
					mess = "empty RDB file created.";
					snsUtil.publishSNSMessage("ERROR: " + mess + qualStr);
					throw new RuntimeException(e);
				}
			} else {
				domObj.setReadingQualifiers(null);
			}

		}
	}

	
}
