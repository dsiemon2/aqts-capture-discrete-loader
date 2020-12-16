package gov.usgs.wma.waterdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class TransformDao {
	private static final Logger LOG = LoggerFactory.getLogger(TransformDao.class);

	@Autowired
	private DiscreteGroundWaterRowMapper discreteGroundWaterRowMapper;

	@Autowired
	@Qualifier("jdbcTemplateTransform")
	protected JdbcTemplate jdbcTemplate;

	@Value("classpath:sql/getDiscreteGroundWater.sql")
	protected Resource selectQuery;

	public List<DiscreteGroundWater> getDiscreteGroundWater(String locationIdentifier) {
		List<DiscreteGroundWater> rtn = Collections.emptyList();
		try {
			String sql = new String(FileCopyUtils.copyToByteArray(selectQuery.getInputStream()));
			rtn = jdbcTemplate.query(
					sql,
					discreteGroundWaterRowMapper,
					locationIdentifier
					);
		} catch (IOException e) {
			LOG.error("Unable to get SQL statement", e);
			throw new RuntimeException(e);
		}
		return rtn;
	}
}
