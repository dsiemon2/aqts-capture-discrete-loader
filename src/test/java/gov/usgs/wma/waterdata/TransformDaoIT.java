package gov.usgs.wma.waterdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.amazonaws.services.sns.AmazonSNS;
import com.github.springtestdbunit.annotation.DatabaseSetup;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.MOCK,
                classes={
                        DBTestConfig.class,
                        TransformDao.class,
                        DiscreteGroundWaterRules.class,
                        DiscreteGroundWaterRowMapper.class,
                        SnSUtil.class,
                        Properties.class,
                        SpringConfig.class})
@DatabaseSetup("classpath:/testData/transformDb/")
@ActiveProfiles("it")
public class TransformDaoIT extends BaseTestDao {
	@MockBean
	@Qualifier("amazonSNS")
	private AmazonSNS snsClient;

	@Autowired
	public TransformDao transformDao;
	List<DiscreteGroundWater> discreteGroundWaterList;

	@BeforeEach
	public void setupTransformDaoIT() {
		discreteGroundWaterList = List.of(
				discreteGroundWater1,
				discreteGroundWater4,
				discreteGroundWater5,
				discreteGroundWater6);
	}

	@Test
	public void testGet() {
		// get new data, return list of discrete gw objects
		List<DiscreteGroundWater> actualData = transformDao.getDiscreteGroundWater(LOCATION_IDENTIFIER_1);
		assertNotNull(actualData);
		assertThat(actualData, containsInAnyOrder(discreteGroundWaterList.toArray()));
	}

	@Test
	public void testNotFound() {
		// try to get data using a bad identifier
		List<DiscreteGroundWater> actualData = transformDao.getDiscreteGroundWater(BAD_LOCATION_IDENTIFIER);
		assertEquals(Collections.emptyList(), actualData);
	}

	@Test
	public void testGetWithNull() {
		// get new data, return list of discrete gw objects
		List<DiscreteGroundWater> actualData = transformDao.getDiscreteGroundWater(null);
		assertEquals(Collections.emptyList(), actualData);
	}
}
