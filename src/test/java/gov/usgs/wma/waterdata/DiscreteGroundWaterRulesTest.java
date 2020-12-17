package gov.usgs.wma.waterdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DiscreteGroundWaterRulesTest {
	@MockBean
	@Qualifier("amazonSNS")
	public AmazonSNS snsClient;

	@Autowired
	private DiscreteGroundWaterRules rules;
	DiscreteGroundWater dga;

	@BeforeEach
	void setUp() {
		dga = new DiscreteGroundWater();
	}

	@Test
	void recognizedFieldVisitCommentsShouldParseToCorrectDateTimeAccuracyCode() {

		dga.setFieldVisitComments(DateTimeAccuracy.HOUR.getMatchString());

		rules.apply(dga);

		assertEquals(DateTimeAccuracy.HOUR.getCode(), dga.getDateTimeAccuracyCode());
		assertEquals(DateTimeAccuracy.HOUR.getText(), dga.getDateTimeAccuracyText());
	}

	@Test
	void unrecognizedFieldVisitCommentsShouldParseToCorrectDateTimeAccuracyCode() {
		dga.setFieldVisitComments("XYZ");

		rules.apply(dga);

		assertEquals(DateTimeAccuracy.MINUTE.getCode(), dga.getDateTimeAccuracyCode());
		assertEquals(DateTimeAccuracy.MINUTE.getText(), dga.getDateTimeAccuracyText());
	}

	@Test
	void emptyFieldVisitCommentsShouldParseToMINUTEDateTimeAccuracyCode() {
		dga.setFieldVisitComments("");

		rules.apply(dga);

		assertEquals(DateTimeAccuracy.MINUTE.getCode(), dga.getDateTimeAccuracyCode());
		assertEquals(DateTimeAccuracy.MINUTE.getText(), dga.getDateTimeAccuracyText());
	}

	@Test
	void nullFieldVisitCommentsShouldParseToMINUTEDateTimeAccuracyCode() {

		dga.setFieldVisitComments(null);

		rules.apply(dga);
		assertEquals(DateTimeAccuracy.MINUTE.getCode(), dga.getDateTimeAccuracyCode());
		assertEquals(DateTimeAccuracy.MINUTE.getText(), dga.getDateTimeAccuracyText());
	}

	@Test
	void trimmedSingleValuedResultQualifiersShouldBeUnmodified() {

		String qualStr = "[\"abc\"]";

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertEquals(qualStr, dga.getReadingQualifiers());
	}

	@Test
	void trimmedDoubleValuedResultQualifiersShouldBeUnmodified() {

		String qualStr = "[\"abc\",\"def\"]";

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertEquals(qualStr, dga.getReadingQualifiers());
	}

	@Test
	void untrimmedSingleValuedResultQualifiersShouldBeTrimmed() {

		String qualStr = "[\"   abc   \"]";

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertEquals("[\"abc\"]", dga.getReadingQualifiers());
	}

	@Test
	void untrimmedDoubleValuedResultQualifiersShouldBeTrimmed() {

		String qualStr = "[\"   abc   \",\"   def   \"]";

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertEquals("[\"abc\",\"def\"]", dga.getReadingQualifiers());
	}

	@Test
	void meaninglessWhitespaceIsIgnoredForSingleValuedResultQualifiers() {

		String qualStr = "[   \"abc\"   ]";

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertEquals("[\"abc\"]", dga.getReadingQualifiers());
	}

	@Test
	void meaninglessWhitespaceIsIgnoredForDoubleValuedResultQualifiers() {

		String qualStr = "[   \"abc\"   ,   \"def\"   ]";

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertEquals("[\"abc\",\"def\"]", dga.getReadingQualifiers());
	}

	@Test
	void nullValuedResultQualifiersRemainNull() {

		String qualStr = null;

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertNull(dga.getReadingQualifiers());
	}

	@Test
	void emptyValuedResultQualifiersIsConvertedToNull() {

		String qualStr = "";

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertNull(dga.getReadingQualifiers());
	}

	@Test
	void emptyArrayResultQualifiersIsConvertedToNull() {

		String qualStr = "[]";

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertNull(dga.getReadingQualifiers());
	}

	@Test
	void whitespaceResultQualifiersAreConvertedToNull() {

		String qualStr = "  \t  ";

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertNull(dga.getReadingQualifiers());
	}

	@Test
	void whitespaceValuesInResultQualifiersAreSkipped() {

		String qualStr = "[   \"\",  \"abc\"   ,   \"\"   ]";

		dga.setReadingQualifiers(qualStr);

		rules.apply(dga);

		assertEquals("[\"abc\"]", dga.getReadingQualifiers());
	}

	@Test
	void NonJsonResultQualifiersThrowsRuntimeException() {

		String qualStr = "I am not Json";

		dga.setReadingQualifiers(qualStr);

		RuntimeException e = assertThrows(RuntimeException.class, () -> {
			rules.apply(dga);
			
		});
		String topicArn = "testArn";
		String message="ERROR: IOException applying groundwater rules: Unrecognized token 'I': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')\n";
		String newQualstr =" at [Source: (String)\"I am not Json\"; line: 1, column: 2]I am not Json";
		PublishRequest request = new PublishRequest(topicArn, message  + newQualstr);
		Mockito.verify(snsClient, Mockito.times(1)).publish(request);
	}


}
