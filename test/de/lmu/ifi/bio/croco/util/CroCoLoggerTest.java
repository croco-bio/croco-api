package de.lmu.ifi.bio.croco.util;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.lmu.ifi.bio.croco.test.UnitTest;

@Category(UnitTest.class)
public class CroCoLoggerTest {

	@Test
	public void test() {
		Logger logger = CroCoLogger.getLogger();
		logger.info("Test");
	}

}
