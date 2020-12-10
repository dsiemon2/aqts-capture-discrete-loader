package gov.usgs.wma.waterdata;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import gov.usgs.wma.waterdata.SnSUtil;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;
/**
 * Applies business rules to a domain object.
 * It is assumed that this object can be modified.
 */
public class DiscreteGroundWaterRules {
	
	private static final JsonFactory jsonFactory = new JsonFactory();
	//Threadsafe factory
	protected SnSUtil snsUtil;
	

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
			mess = "recoverable-data-warnings";
			snsUtil.publishSNSMessage("ERROR: this is a test message");
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
					mess = "recoverable-data-warnings";
					snsUtil.publishSNSMessage("ERROR: " + mess + qualStr);
					throw new RuntimeException(e);
				}
			} else {
				domObj.setReadingQualifiers(null);
			}

		}
	}

	
}
