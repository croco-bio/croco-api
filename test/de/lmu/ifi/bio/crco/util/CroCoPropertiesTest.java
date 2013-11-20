package de.lmu.ifi.bio.crco.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class CroCoPropertiesTest {

	@Test
	public void test() throws Exception {
		CroCoProperties props = CroCoProperties.getInstance();
	///	System.out.println(props.getProperties());
		System.out.println(props.getProperties("de.lmu.ifi.bio.crco.connector.LocalService."));
	}

}
