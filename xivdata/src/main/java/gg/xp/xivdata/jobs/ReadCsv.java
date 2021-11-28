package gg.xp.xivdata.jobs;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ReadCsv {
	public static List<String[]> cells(String resourcePath) {
		List<String[]> arrays;
		try (CSVReader csvReader = new CSVReader(new InputStreamReader(ReadCsv.class.getResourceAsStream(resourcePath)))) {
			arrays = csvReader.readAll();
		}
		catch (IOException | CsvException e) {
			throw new RuntimeException(e);
		}
		return arrays;
	}
}
