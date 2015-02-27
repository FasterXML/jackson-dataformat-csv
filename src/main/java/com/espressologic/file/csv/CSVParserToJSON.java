package com.espressologic.file.csv;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;

public class CSVParserToJSON {
	
	private String keyAttrName = "idvendor_pricelist";
	private String altKeyAttrName = "vendorID";
	//col1 = origColName col2 = replacement column
	private Map<String,String> mappedColumns = new HashMap<String,String>();

	/*
	 * if(row.processCSVFlag && row.content !== nulll){ 
	 *  var csv =  new com.espressologic.file.csv.CSVParserToJSON("keycolumn");
	 *  var result = csv.CSVParserToJSON.convertFileToJSON(row.idvendor_pricelist , row.vendorID,row.content);
	 *  var json = JSON.parse(result); log.debug(result); }
	 *  
	 *  //can also pass HashMap<String,String> map with oldColName,newColName in constructor
	 *  new CSVParserToJSON("keycolumn", map);
	 */
	public CSVParserToJSON(){
		
	}
	public CSVParserToJSON(String keyName){
		this.keyAttrName = keyName;
	}
	public CSVParserToJSON(String keyName,String altKeyName){
		this.keyAttrName = keyName;
		this.altKeyAttrName = altKeyName;
	}
	public CSVParserToJSON(String keyName,Map<String,String> columnMap){
		this.keyAttrName = keyName;
		this.setMappedColumns(columnMap);
	}
	public CSVParserToJSON(String keyName,String altKeyName,Map<String,String> columnMap){
		this.keyAttrName = keyName;
		this.altKeyAttrName = altKeyName;
		this.setMappedColumns(columnMap);
	}

	public static void main(String[] args) {
		File file = new File("ScanPower Vega.csv");//JarrowFormulas.csv");
		
		byte[] bFile = new byte[(int) file.length()];
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			fileInputStream.read(bFile);
			fileInputStream.close();

			for (int i = 0; i < bFile.length; i++) {
				// System.out.print((char) bFile[i]);
			}
			CSVParserToJSON csv = new CSVParserToJSON("idvendor_pricelist","vendorid");
			csv.addColumnMap("upc", "UPC");
			String str = csv.convertFileToJSON(4,2, bFile);
			System.out.println(str);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public String convertFileToJSON(String keyName, int idvendor_pricelist, int vendorID, byte[] bytes){
		keyAttrName = keyName;
		return convertFileToJSON( idvendor_pricelist,  vendorID,bytes);
	}

	public String convertFileToJSON(int idKey1, int idKey2 , byte[] bytes) {

		CsvMapper mapper = new CsvMapper();
		// important: we need "array wrapping" (see next section) here:
		mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
		// need to create a record [{ vendorID, row, columnName, value}]
		MappingIterator<String[]> it;
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			it = mapper.reader(String[].class).readValues(bis);
			String[] row = null;
			String[] columnHeader = null;

			int rownum = 0;
			String sep = "";
			while (it.hasNext()) {
				row = it.next();
				// and voila, column values in an array. Works with Lists as
				if (rownum == 0) {
					// this is our first row so skip it
					columnHeader = row.clone();
					
				} else {
					String cs = ",";

					for (int i = 0; i < row.length; i++) {
						//skip blank and null rows
						if(row[i] != null && !"".equals(row[i])){
							sb.append(sep);
							sb.append("{");
							format(sb, "\""+keyAttrName+"\":",
									String.valueOf(idKey1));
							sb.append(cs);
							format(sb, "\"row\":", String.valueOf(rownum));
							sb.append(cs);
							format(sb, "\"columnName\":", quote(replaceColumnName(columnHeader[i])));
							sb.append(cs);
							format(sb, "\"value\":", quote(row[i]));
							if(idKey2 > 0){
								sb.append(cs);
								format(sb, "\""+altKeyAttrName+"\":", String.valueOf(idKey2));
							}
							sb.append("}");
							sep = ",";
						}
					}
				}
				rownum++;
			}

		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sb.append("]");
		return sb.toString();
	}

	private String replaceColumnName(String currColName) {
		String response = currColName;
		//use the map to find and swap the name
		for(String mappedColName : mappedColumns.keySet()){
			if(response.equalsIgnoreCase(mappedColName)){
				response = mappedColumns.get(mappedColName);
				break;
			}
		}
		return response;
	}
	private String quote(String value) {
		String val = (value == null)?"":value.replaceAll("\"", "'");
		return "\"" + val + "\"";
	}

	private void format(StringBuffer sb, String key, String value) {
		sb.append(key);
		sb.append(value);
	}
	public String getKeyAttrName() {
		return keyAttrName;
	}
	public void setKeyAttrName(String keyAttrName) {
		this.keyAttrName = keyAttrName;
	}
	public String getAltKeyAttrName() {
		return altKeyAttrName;
	}
	public void setAltKeyAttrName(String altKeyAttrName) {
		this.altKeyAttrName = altKeyAttrName;
	}
	public Map<String, String> getMappedColumns() {
		return mappedColumns;
	}
	public void setMappedColumns(Map<String, String> mappedColumns) {
		this.mappedColumns = mappedColumns;
	}
	public void addColumnMap(String oldColName,String newColName){
		this.mappedColumns.put(oldColName, newColName);
	}
}
