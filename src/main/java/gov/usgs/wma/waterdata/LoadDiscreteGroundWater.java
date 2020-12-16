package gov.usgs.wma.waterdata;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LoadDiscreteGroundWater implements Function<RequestObject, ResultObject> {

	private static final Logger LOG = LoggerFactory.getLogger(LoadDiscreteGroundWater.class);

	public static final String INSERT_SUCCEEDED_MESSAGE = "Successfully update discrete gw levels for monitoring location identifier: %s.  Inserted %s rows, deleted %s rows";
	public static final String INSERT_FAILED_MESSAGE = "Selected row count: %s and inserted row count: %s differ, insert failed for monitoring location identifier: %s";

	private final TransformDao transformDao;
	private final ObservationDao observationDao;

	@Autowired
	public LoadDiscreteGroundWater(TransformDao transformDao, ObservationDao observationDao) {
		this.transformDao = transformDao;
		this.observationDao = observationDao;
	}

	@Override
	public  ResultObject apply(RequestObject request) {
		return processRequest(request);
	}

	@Transactional
	protected ResultObject processRequest(RequestObject request) {

		String locationIdentifier = request.getLocationIdentifier();
		String monitoringLocationIdentifier = request.getMonitoringLocationIdentifier();

		LOG.debug("Begin processing request for locationIdentifier: {}, monitoringLocationIdentifier: {}", locationIdentifier, monitoringLocationIdentifier);

		if (locationIdentifier == null || monitoringLocationIdentifier == null) {
			throw new IllegalArgumentException("Neither the locationIdentifier nor monitoringLocationIdentifier can be null");
		}

		// Get all the records for this aqts location identifier from the transform db
		List<DiscreteGroundWater> discreteGroundWaterList = transformDao.getDiscreteGroundWater(locationIdentifier);

		// Update the observations db
		ResultObject result = updateDiscreteGroundWaterIntoObservationDb(discreteGroundWaterList, monitoringLocationIdentifier);

		LOG.debug(String.format(INSERT_SUCCEEDED_MESSAGE, result.getMonitoringLocationIdentifier(), result.getInsertCount(), result.getDeleteCount()));

		return result;
	}

	@Transactional
	public ResultObject updateDiscreteGroundWaterIntoObservationDb(List<DiscreteGroundWater> discreteGroundWaterList, String monitoringLocationIdentifier) {

		ResultObject result = new ResultObject();
		result.setMonitoringLocationIdentifier(monitoringLocationIdentifier);

		if (discreteGroundWaterList.size() > 0) {
			// Do the delete and the insert if we get discrete gw levels from the transform db
			result.setDeleteCount(
					observationDao.deleteDiscreteGroundWater(monitoringLocationIdentifier)
			);
			result.setInsertCount(
					observationDao.insertDiscreteGroundWater(discreteGroundWaterList)
			);
		} else {
			// Just do the delete if we get no discrete gw levels from the transform db
			// This happens if discrete gw levels are no longer public
			result.setDeleteCount(
					observationDao.deleteDiscreteGroundWater(monitoringLocationIdentifier)
			);
			result.setInsertCount(0);
		}


		// If the number of discrete gw levels that were inserted differs from number of discrete gw levels we attempted
		// to insert, then throw an error. This would be a rare case.
		if (result.getInsertCount() != discreteGroundWaterList.size()) {
			String failMessageInsertFailed = String.format(
					INSERT_FAILED_MESSAGE,
					discreteGroundWaterList.size(),
					result.getInsertCount(),
					monitoringLocationIdentifier);
			throw new RuntimeException(failMessageInsertFailed);
		}

		return result;
	}
}
