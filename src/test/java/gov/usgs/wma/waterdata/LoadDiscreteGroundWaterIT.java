package gov.usgs.wma.waterdata;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.amazonaws.services.sns.AmazonSNS;
import com.github.springtestdbunit.annotation.DatabaseSetup;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.MOCK,
               classes={
                       DBTestConfig.class,
                       LoadDiscreteGroundWater.class,
                       TransformDao.class,
                       ObservationDao.class,
                       DiscreteGroundWaterRules.class,
                       DiscreteGroundWaterRowMapper.class,
                       SnSUtil.class,
                       Properties.class,
                       SpringConfig.class,
                       AmazonSNS.class})
@DatabaseSetup(
		connection="transform",
		value="classpath:/testData/transformDb/")
@DatabaseSetup(
		connection="observation",
		value="classpath:/testData/lookups/")
@DatabaseSetup(
		connection="observation",
		value="classpath:/testData/sitefile/")
@ActiveProfiles("it")
public class LoadDiscreteGroundWaterIT extends BaseTestDao {
	@MockBean
	@Qualifier("amazonSNS")
	public AmazonSNS snsClient;

	@Autowired
	public DiscreteGroundWaterRules discreteGroundWaterRules;

	@Autowired
	public LoadDiscreteGroundWater loadDiscreteGroundWater;

	@Test
	@DatabaseSetup(
			connection="observation",
			value="classpath:/testResult/existingData/")
	@ExpectedDatabase(
			connection="observation",
			value="classpath:/testResult/afterInsert/",
			assertionMode= DatabaseAssertionMode.NON_STRICT_UNORDERED)
	public void testInsertNewData() {
		request.setLocationIdentifier(LOCATION_IDENTIFIER_1);
		request.setMonitoringLocationIdentifier(MONITORING_LOCATION_IDENTIFIER_1);
		ResultObject result = loadDiscreteGroundWater.processRequest(request);
		assertEquals(MONITORING_LOCATION_IDENTIFIER_1, result.getMonitoringLocationIdentifier());
		assertEquals(4, result.getInsertCount());
		assertEquals(0, result.getDeleteCount());
	}

	@Test
	@DatabaseSetup(
			connection="observation",
			value="classpath:/testResult/afterInsert/")
	@ExpectedDatabase(
			value="classpath:/testResult/afterInsert/",
			assertionMode= DatabaseAssertionMode.NON_STRICT_UNORDERED,
			connection="observation")
	public void testReplaceExistingData() {
		request.setLocationIdentifier(LOCATION_IDENTIFIER_1);
		request.setMonitoringLocationIdentifier(MONITORING_LOCATION_IDENTIFIER_1);
		ResultObject result = loadDiscreteGroundWater.processRequest(request);
		assertEquals(MONITORING_LOCATION_IDENTIFIER_1, result.getMonitoringLocationIdentifier());
		assertEquals(4, result.getInsertCount());
		assertEquals(4, result.getDeleteCount());
	}

	@Test
	@DatabaseSetup(
			connection="observation",
			value="classpath:/testData/discreteGWAqtsDbWithExtraRecord/")
	@ExpectedDatabase(
			value="classpath:/testResult/afterInsert/",
			assertionMode= DatabaseAssertionMode.NON_STRICT_UNORDERED,
			connection="observation")
	public void testDeleteRecordsNolongerAssociatedWithMonitoringLocation() {
		request.setLocationIdentifier(LOCATION_IDENTIFIER_1);
		request.setMonitoringLocationIdentifier(MONITORING_LOCATION_IDENTIFIER_1);
		ResultObject result = loadDiscreteGroundWater.processRequest(request);
		assertEquals(MONITORING_LOCATION_IDENTIFIER_1, result.getMonitoringLocationIdentifier());
		assertEquals(4, result.getInsertCount());
		assertEquals(4, result.getDeleteCount());
	}

	@Test
	@DatabaseSetup(
			connection="observation",
			value="classpath:/testResult/empty/")
	@ExpectedDatabase(
			connection="observation",
			value="classpath:/testResult/empty/",
			assertionMode= DatabaseAssertionMode.NON_STRICT_UNORDERED)
	public void testNoRecordsFoundInTransformDB() {
		request.setLocationIdentifier(BAD_LOCATION_IDENTIFIER);
		request.setMonitoringLocationIdentifier(BAD_MONITORING_LOCATION_IDENTIFIER);
		ResultObject result = loadDiscreteGroundWater.processRequest(request);
		assertEquals(BAD_MONITORING_LOCATION_IDENTIFIER, result.getMonitoringLocationIdentifier());
		assertEquals(0, result.getInsertCount());
		assertEquals(0, result.getDeleteCount());

		assertDoesNotThrow(() -> {
			loadDiscreteGroundWater.apply(request);
		}, "should not have thrown an exception but did");
	}

	@Test
	// Records no longer present in the transform db, delete should still happen
	@DatabaseSetup(
			connection="transform",
			value="classpath:/testData/transformDbRecordsNoLongerPresent/")
	// Records are still present here in the observations db
	@DatabaseSetup(
			connection="observation",
			value="classpath:/testResult/afterInsert/")
	// So now they should be deleted and nothing inserted
	@ExpectedDatabase(
			value="classpath:/testResult/noMorePublicRecordsSoMustDelete/",
			assertionMode= DatabaseAssertionMode.NON_STRICT_UNORDERED,
			connection="observation")
	public void testRecordsNoLongerInTransformDbSoDeleteInObservationsDb() {
		request.setLocationIdentifier(LOCATION_IDENTIFIER_1);
		request.setMonitoringLocationIdentifier(MONITORING_LOCATION_IDENTIFIER_1);
		ResultObject result = loadDiscreteGroundWater.processRequest(request);
		assertEquals(MONITORING_LOCATION_IDENTIFIER_1, result.getMonitoringLocationIdentifier());
		assertEquals(0, result.getInsertCount());
		assertEquals(4, result.getDeleteCount());
	}
}
