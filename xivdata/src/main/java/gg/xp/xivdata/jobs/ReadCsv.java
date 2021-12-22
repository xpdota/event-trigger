package gg.xp.xivdata.jobs;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public final class ReadCsv {
	private ReadCsv() {
	}

	public static List<String[]> cellsFromResource(String resourcePath) {
		List<String[]> arrays;
		try (CSVReader csvReader = new CSVReader(new InputStreamReader(ReadCsv.class.getResourceAsStream(resourcePath)))) {
			arrays = csvReader.readAll();
		}
		catch (IOException | CsvException e) {
			throw new RuntimeException(e);
		}
		return arrays;
	}

	public static List<String[]> cellsFromFile(File file) {
		List<String[]> arrays;
		try (CSVReader csvReader = new CSVReader(new FileReader(file))) {
			arrays = csvReader.readAll();
		}
		catch (IOException | CsvException e) {
			throw new RuntimeException(e);
		}
		return arrays;
	}
}
