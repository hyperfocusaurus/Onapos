package onapos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class CollectionFile {
	private File onDisk;
	private Collection collection;
	public CollectionFile() {
		
	}
	
	public CollectionFile(File infile) {
		onDisk = infile;
	}
	
	public CollectionFile(Collection c,File outfile) {
		onDisk = outfile;
		collection = c;
	}

	/**
	 * Provides an interface for writing collections to disk directly
	 * @param c the collection to write
	 */
	public void write(Collection c) {
		collection = c;
		this.write();
	}
	
	/**
	 * writes this file's collection to disk
	 */
	public void write() {
		FileWriter writer;
		BufferedWriter buffedWriter;
		if(!onDisk.exists()) {
			try {
				onDisk.createNewFile();
			} catch (IOException e) {
				System.err.println("WARNING: collection file not saved (could not create file): "+onDisk.getName());
			}
		}
		if(!onDisk.canWrite()) {
			System.err.println("WARNING: collection file not saved (could not open file for writing): "+onDisk.getName());
			return;
		}
		if(onDisk.isDirectory()) {
			System.err.println("WARNING: collection file not saved (tried to save as directory)");
			return;
		}
		try {
			writer = new FileWriter(onDisk);
			buffedWriter = new BufferedWriter(writer);
		} catch (IOException e) {
			System.err.println("WARNING: file deleted before we could write to it: "+onDisk.getName());
			return;
		}
		try {
			buffedWriter.write("name:"+collection.getName());
			buffedWriter.newLine();
			buffedWriter.write("type:"+collection.getType());
			buffedWriter.newLine();
			HashMap<String,Property> properties = collection.getProperties();
			for(Entry<String,Property> entry : properties.entrySet()) {
				buffedWriter.write("field:");
				buffedWriter.write(entry.getKey());
				buffedWriter.append(",");
				buffedWriter.write(entry.getValue().getTypeAsString());
				buffedWriter.newLine();
			}
			ArrayList<Item> items = collection.getItems();
			for(Item i : items) {
				buffedWriter.write("item {");
				buffedWriter.newLine();
				for(Entry<String,Property> p : i.getProperties().entrySet()) {
					buffedWriter.write(p.getKey());
					buffedWriter.append(":");
					try {
						switch(p.getValue().getType()) {
							case STRING:
								buffedWriter.write(p.getValue().getString());
								break;
							case INTEGER:
								buffedWriter.write(new Integer(p.getValue().getInt()).toString());
								break;
							case DOUBLE:
								buffedWriter.write(new Double(p.getValue().getDouble()).toString());
								break;
							case DATE:
								buffedWriter.write(p.getValue().getDate().toString());
								break;
							case BOOLEAN:
								if(p.getValue().getBoolean())
									buffedWriter.write("yes");
								else
									buffedWriter.write("no");
								break;
							default:
								System.err.println("WARNING: saving naked property (may not get loaded): "+p.getKey());
								break;
						}
						buffedWriter.newLine();
					} catch (PropertyException e) {
						System.err.println("Unreachable code, but the compiler don't care none anyhow!");
						System.exit(42);
					}
				}
				buffedWriter.append("}");
				buffedWriter.newLine();
			}
			buffedWriter.flush();
		} catch(IOException e) {
			System.err.println("WARNING: file may be corrupted (IOException encountered while writing)");
			e.printStackTrace(System.err);
		}
	}
	
	public Collection read() {
		FileReader reader;
		BufferedReader buffedReader;
		if(!onDisk.canRead()) {
			System.err.println("WARNING: collection file could not be read: "+onDisk.getName());
			return null;
		}
		if(onDisk.isDirectory()) {
			System.err.println("WARNING: tried to load a directory as a collection!");
			return null;
		}
		try {
			reader = new FileReader(onDisk);
			buffedReader = new BufferedReader(reader);
		} catch(FileNotFoundException e) {
			System.err.println("WARNING: file deleted before we could read it: "+onDisk.getName());
			return null;
		}
		try {
			String curLine;
			String collectionName = "Untitled Collection";
			String collectionType = "Generic";
			ArrayList<String> propertyNames = new ArrayList<String>();
			ArrayList<String> propertyTypes = new ArrayList<String>();
			ArrayList<Item> items = new ArrayList<Item>();
			while((curLine = buffedReader.readLine()) != null) {
				if(curLine.trim().startsWith("name:")) {
					collectionName = curLine.substring(curLine.indexOf(':')+1);
				}
				if(curLine.trim().startsWith("type:")) {
					collectionType = curLine.substring(curLine.indexOf(':')+1);
				}
				if(curLine.trim().startsWith("field:")) {
					propertyNames.add(curLine.trim().substring(curLine.trim().indexOf(':')+1,curLine.trim().indexOf(',')));
					propertyTypes.add(curLine.trim().substring(curLine.trim().indexOf(',')+1));
				}
				if(curLine.trim().startsWith("item")) {
					Item curItem = new Item();
					boolean foundLastBracket = false;
					while(curLine != null && foundLastBracket == false) {
						curLine = buffedReader.readLine();
						if(curLine.contains("}") && (curLine.indexOf("}") == 0 || curLine.charAt(curLine.indexOf("}")-1)!='\\')) {
							foundLastBracket = true;
						}
						for(String propertyName : propertyNames) {
							if(curLine.trim().startsWith(propertyName)) {
								curItem.addProperty(propertyName, new Property(curLine.substring(curLine.indexOf(":")+1)));
							}
						}
					}
					items.add(curItem);
				}
			}
			collection = new Collection(collectionName,collectionType);
			collection.addItems(items);
			return collection;
		} catch(IOException e) {
			System.err.println("WARNING: exception occurred while reading file:"+onDisk.getName());
			e.printStackTrace(System.err);
			return null;
		}
	}
}