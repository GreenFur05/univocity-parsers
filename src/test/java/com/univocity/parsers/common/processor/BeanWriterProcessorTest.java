/*******************************************************************************
 * Copyright 2014 Univocity Software Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.univocity.parsers.common.processor;

import com.univocity.parsers.annotations.*;
import com.univocity.parsers.common.*;
import com.univocity.parsers.conversions.*;
import com.univocity.parsers.csv.*;
import com.univocity.parsers.examples.Car;
import org.testng.annotations.*;

import java.io.StringWriter;
import java.math.*;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.*;

public class BeanWriterProcessorTest {

	static class TestBean {

		@Parsed(defaultNullRead = "0")
		Integer quantity;

		@Trim
		@LowerCase
		@Parsed(index = 4)
		String commts;

		@Parsed(field = "amount")
		BigDecimal amnt;

		@Trim
		@LowerCase
		@BooleanString(falseStrings = {"no", "n", "null"}, trueStrings = {"yes", "y"})
		@Parsed
		Boolean pending;
	}

	private final NormalizedString[] headers = NormalizedString.toArray("date,amount,quantity,pending,comments".split(","));

	@Test
	public void testAnnotatedBeanProcessor() {
		BeanWriterProcessor<TestBean> processor = new BeanWriterProcessor<TestBean>(TestBean.class);
		processor.convertAll(Conversions.toNull("?"));

		processor.initialize();

		Object[] row;

		TestBean bean1 = new TestBean();
		bean1.amnt = new BigDecimal("555.999");
		bean1.commts = null;
		bean1.pending = true;
		bean1.quantity = 1;

		row = processor.write(bean1, headers, null);

		assertEquals(row[0], "?"); // date not mapped in bean
		assertEquals(row[1], "555.999");
		assertEquals(row[2], "1");
		assertEquals(row[3], "yes");
		assertEquals(row[4], "?");

		TestBean bean2 = new TestBean();
		bean2.amnt = null;
		bean2.quantity = 0;
		bean2.pending = false;
		bean2.commts = " something ";

		row = processor.write(bean2, headers, null);

		assertEquals(row[0], "?"); // date not mapped in bean
		assertEquals(row[1], "?");
		assertEquals(row[2], "0");
		assertEquals(row[3], "no");
		assertEquals(row[4], "something"); // trimmed
	}

	@Test
	public void testRepeatedIndexInAnnotation() {
		BeanWriterProcessor<AnnotatedBeanProcessorTest.Data> rowProcessor = new BeanWriterProcessor<AnnotatedBeanProcessorTest.Data>(AnnotatedBeanProcessorTest.Data.class);
		CsvWriterSettings settings = new CsvWriterSettings();
		settings.setRowWriterProcessor(rowProcessor);

		try {
			new CsvWriter(settings);
			fail("Expecting validation error on duplicate field");
		} catch(Exception e){
			assertTrue(e.getMessage().startsWith("Duplicate field index '1' found in attribute"));
		}
	}

	@Test
	public void testBeanColumnReorderingEnabled() {
		StringWriter output = new StringWriter();

		CsvWriterSettings settings = new CsvWriterSettings();
		settings.setRowWriterProcessor(new BeanWriterProcessor<Car>(Car.class));
		settings.setHeaderWritingEnabled(true);

		settings.setColumnReorderingEnabled(true);
		settings.selectFields("model", "year", "price", "model", "year", "year");

		CsvWriter writer = new CsvWriter(output, settings);
		writer.writeHeaders();


		Car car = new Car();
		Set<String> s = new HashSet<String>();
		s.add("Test");
		s.add("Description");

		car.setYear(2001);
		car.setDescription(s);
		car.setMake("2001");
		car.setModel("U");
		car.setPrice(new BigDecimal("100.00"));
		writer.processRecord(car);

		car.setYear(2002);
		car.setDescription(s);
		car.setMake("2002Make");
		car.setModel("2002Model");
		car.setPrice(new BigDecimal("200.00"));
		writer.processRecord(car);

		car.setYear(2003);
		car.setDescription(s);
		car.setMake("2003Make");
		car.setModel("2003Model");
		car.setPrice(new BigDecimal("300.00"));
		writer.processRecord(car);

		writer.close();

		System.out.println(output.toString());
	}

}
