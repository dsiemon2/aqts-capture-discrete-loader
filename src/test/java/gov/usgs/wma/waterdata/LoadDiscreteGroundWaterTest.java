package gov.usgs.wma.waterdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.amazonaws.services.sns.AmazonSNS;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
public class LoadDiscreteGroundWaterTest {
	@MockBean
	@Qualifier("amazonSNS")
	public AmazonSNS snsClient;
	@Autowired
	protected SnSUtil snsUtil;
	@MockBean
	private ObservationDao observationDao;
	@MockBean
	private TransformDao transformDao;

	private LoadDiscreteGroundWater loadDiscreteGroundWater;
	private RequestObject request;
	private List<DiscreteGroundWater> genericDiscreteGroundWaterList;
	private DiscreteGroundWater genericDiscreteGroundWater1;
	private DiscreteGroundWater genericDiscreteGroundWater2;

	@BeforeEach
	public void setupLoadDiscreteGroundWater() {
		loadDiscreteGroundWater = new LoadDiscreteGroundWater(transformDao, observationDao);
		request = new RequestObject();
		request.setLocationIdentifier(BaseTestDao.LOCATION_IDENTIFIER_1);
		request.setMonitoringLocationIdentifier(BaseTestDao.MONITORING_LOCATION_IDENTIFIER_1);
		genericDiscreteGroundWaterList = new ArrayList<>();
		genericDiscreteGroundWater1 = new DiscreteGroundWater();
		genericDiscreteGroundWater2 = new DiscreteGroundWater();
	}

	@Test
	public void testNoRecordsFound() {
		// no gw levels found
		when(transformDao.getDiscreteGroundWater(anyString())).thenReturn(genericDiscreteGroundWaterList);
		ResultObject result = loadDiscreteGroundWater.processRequest(request);

		assertNotNull(result);
		assertEquals(0, result.getInsertCount());
		assertDoesNotThrow(() -> {
			loadDiscreteGroundWater.apply(request);
		}, "should not have thrown an exception but did");
	}

	@Test
	public void testNullLocationIdentifier() {
		assertThrows(IllegalArgumentException.class, () -> {
			ResultObject result = loadDiscreteGroundWater.processRequest(new RequestObject());
		});
	}

	@Test
	public void testFoundGenericFailedInsert() {
		genericDiscreteGroundWaterList.add(genericDiscreteGroundWater1);
		genericDiscreteGroundWaterList.add(genericDiscreteGroundWater2);
		when(transformDao.getDiscreteGroundWater(anyString())).thenReturn(genericDiscreteGroundWaterList);
		// delete succeeds
		when(observationDao.deleteDiscreteGroundWater(anyString())).thenReturn(2);
		// insert fails
		when(observationDao.insertDiscreteGroundWater(anyList())).thenReturn(0);

		assertThrows(RuntimeException.class, () -> {
			loadDiscreteGroundWater.processRequest(request);
		}, "should have thrown an exception but did not");
		assertThrows(RuntimeException.class, () -> {
			loadDiscreteGroundWater.apply(request);
		}, "should have thrown an exception but did not");
	}

	@Test
	public void testFoundGeneric() {
		genericDiscreteGroundWaterList.add(genericDiscreteGroundWater1);
		genericDiscreteGroundWaterList.add(genericDiscreteGroundWater2);
		// 2 gw levels returned
		when(transformDao.getDiscreteGroundWater(anyString())).thenReturn(genericDiscreteGroundWaterList);
		// delete succeeds
		when(observationDao.deleteDiscreteGroundWater(anyString())).thenReturn(2);
		// insert succeeds
		when(observationDao.insertDiscreteGroundWater(anyList())).thenReturn(2);
		ResultObject result = loadDiscreteGroundWater.apply(request);

		assertNotNull(result);
		assertEquals(genericDiscreteGroundWaterList.size(), result.getInsertCount());
	}

	@Test
	public void testFoundGenericNewRecords() {
		genericDiscreteGroundWaterList.add(genericDiscreteGroundWater1);
		genericDiscreteGroundWaterList.add(genericDiscreteGroundWater2);
		// 2 gw levels returned
		when(transformDao.getDiscreteGroundWater(anyString())).thenReturn(genericDiscreteGroundWaterList);
		// nothing to delete
		when(observationDao.deleteDiscreteGroundWater(anyString())).thenReturn(0);
		// insert succeeds
		when(observationDao.insertDiscreteGroundWater(anyList())).thenReturn(2);
		ResultObject result = loadDiscreteGroundWater.apply(request);

		assertNotNull(result);
		assertEquals(genericDiscreteGroundWaterList.size(), result.getInsertCount());
	}
}
