package org.icemoon.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import com.jme3.asset.AssetManager;

import icemoon.iceloader.AbstractConfiguration;

public class Abilities extends AbstractConfiguration<LinkedHashMap<Integer, Ability>> {
	private static final Logger LOG = Logger.getLogger(Abilities.class.getName());

	private final static int maxColumns = 29;
	private final static int minColumns = 28;

	private String colDelimiter = "\t";

	public Abilities(String assetPath) {
		this(null, assetPath);
	}

	public Abilities(AssetManager assetManager, String assetPath) {
		super(new LinkedHashMap<Integer, Ability>(), assetManager);
		this.assetPath = assetPath;
		if (assetManager != null) {
			loadConfigurationAsset();
		}
	}

	@Override
	protected void load(InputStream in, LinkedHashMap<Integer, Ability> backingObject) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		try {
			String line = null;
			int lineNo = 0;
			long startLine = -1;
			while ((line = reader.readLine()) != null) {
				if (startLine == -1) {
					startLine = lineNo;
				}
				line = line.trim().replace("\r", "");
				if (isComment(line))
					continue;
				String comment = null;
				String[] row = line.split(colDelimiter);
				if (row.length < minColumns || row.length > maxColumns)
					LOG.severe(
							String.format("Line %s has more or less than the min (%d) or max (d) number of colums (%d)",
									lineNo, minColumns, maxColumns, row.length));
				for (int i = 0; i < row.length; i++) {
					if (row[i].startsWith("\""))
						row[i] = row[i].substring(1);
					if (row[i].endsWith("\""))
						row[i] = row[i].substring(0, row[i].length() - 1);
				}
				try {
					Ability a = new Ability();
					a.set(row, comment);
					backingObject.put(a.getAbilityId(), a);
				} catch (IndexOutOfBoundsException ioobe) {
					LOG.severe(String.format("Line %d (of %d for %d) failed to parse. %s", lineNo, row.length,
							maxColumns, ioobe.getMessage()));
				}
				lineNo++;
			}
		} finally {
			reader.close();
		}

	}

	protected boolean isComment(String lastLine) {
		return lastLine.startsWith("#");
	}
}
