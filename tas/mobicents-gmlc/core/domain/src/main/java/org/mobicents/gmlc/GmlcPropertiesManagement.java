/**
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.gmlc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

/**
 * @author amit bhayani
 * 
 */
public class GmlcPropertiesManagement implements GmlcPropertiesManagementMBean {

	private static final Logger logger = Logger.getLogger(GmlcPropertiesManagement.class);

	private static final String SC_GT = "scgt";
	private static final String SC_SSN = "scssn";

	private String serviceCenterGt = null;
	private int serviceCenterSsn = -1;


	protected static final String NO_ROUTING_RULE_CONFIGURED_ERROR_MESSAGE = "noroutingruleconfigerrmssg";

	protected static final String SERVER_ERROR_MESSAGE = "servererrmssg";

	protected static final String DIALOG_TIMEOUT_ERROR_MESSAGE = "dialogtimeouterrmssg";

	protected static final String DIALOG_TIMEOUT = "dialogtimeout";

	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";
	private static final XMLBinding binding = new XMLBinding();
	private static final String PERSIST_FILE_NAME = "gmlcproperties.xml";

	private static GmlcPropertiesManagement instance;

	private final String name;

	private String persistDir = null;

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private String noRoutingRuleConfiguredMessage;
	private String serverErrorMessage;
	private String dialogTimeoutErrorMessage;

	public String getServiceCenterGt() {
		return serviceCenterGt;
	}

	public void setServiceCenterGt(String serviceCenterGt) {
		this.serviceCenterGt = serviceCenterGt;
		this.store();
	}

	public int getServiceCenterSsn() {
		return serviceCenterSsn;
	}

	public void setServiceCenterSsn(int serviceCenterSsn) {
		this.serviceCenterSsn = serviceCenterSsn;
		this.store();
	}
	/**
	 * Dialog time out in milliseconds. Once HTTP request is sent, it expects
	 * back response in dialogTimeout milli seconds.
	 */
	private long dialogTimeout = 25000;

	private GmlcPropertiesManagement(String name) {
		this.name = name;
		binding.setClassAttribute(CLASS_ATTRIBUTE);
	}

	protected static GmlcPropertiesManagement getInstance(String name) {
		if (instance == null) {
			instance = new GmlcPropertiesManagement(name);
		}
		return instance;
	}

	public static GmlcPropertiesManagement getInstance() {
		return instance;
	}

	public String getName() {
		return name;
	}

	public String getPersistDir() {
		return persistDir;
	}

	public void setPersistDir(String persistDir) {
		this.persistDir = persistDir;
	}

//	@Override
//	public String getNoRoutingRuleConfiguredMessage() {
//		return this.noRoutingRuleConfiguredMessage;
//	}

//	@Override
//	public void setNoRoutingRuleConfiguredMessage(String noRoutingRuleConfiguredMessage) {
//		this.noRoutingRuleConfiguredMessage = noRoutingRuleConfiguredMessage;
//		this.store();
//	}

//	@Override
//	public String getServerErrorMessage() {
//		return this.serverErrorMessage;
//	}

//	@Override
//	public void setServerErrorMessage(String serverErrorMessage) {
//		this.serverErrorMessage = serverErrorMessage;
//		this.store();
//	}

	@Override
	public String getDialogTimeoutErrorMessage() {
		return this.dialogTimeoutErrorMessage;
	}

	@Override
	public void setDialogTimeoutErrorMessage(String dialogTimeoutErrorMessage) {
		this.dialogTimeoutErrorMessage = dialogTimeoutErrorMessage;
		this.store();
	}

	@Override
	public long getDialogTimeout() {
		return dialogTimeout;
	}

	@Override
	public void setDialogTimeout(long dialogTimeout) {
		this.dialogTimeout = dialogTimeout;
		this.store();
	}

	public void start() throws Exception {

		this.persistFile.clear();

		if (persistDir != null) {
			this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
					.append(PERSIST_FILE_NAME);
		} else {
			persistFile
					.append(System.getProperty(GmlcManagement.USSD_PERSIST_DIR_KEY,
							System.getProperty(GmlcManagement.USER_DIR_KEY))).append(File.separator).append(this.name)
					.append("_").append(PERSIST_FILE_NAME);
		}

		logger.info(String.format("Loading GMLC Properties from %s", persistFile.toString()));

		try {
			this.load();
		} catch (FileNotFoundException e) {
			logger.warn(String.format("Failed to load the GMLC configuration file. \n%s", e.getMessage()));
		}

	}

	public void stop() throws Exception {
		this.store();
	}

	/**
	 * Persist
	 */
	public void store() {

		// TODO : Should we keep reference to Objects rather than recreating
		// everytime?
		try {
			XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
			writer.setBinding(binding);
			// Enables cross-references.
			// writer.setReferenceResolver(new XMLReferenceResolver());
			writer.setIndentation(TAB_INDENT);

			writer.write(this.serviceCenterGt, SC_GT, String.class);
			writer.write(this.serviceCenterSsn, SC_SSN, Integer.class);

//			writer.write(this.noRoutingRuleConfiguredMessage, NO_ROUTING_RULE_CONFIGURED_ERROR_MESSAGE, String.class);
//			writer.write(this.serverErrorMessage, SERVER_ERROR_MESSAGE, String.class);
			writer.write(this.dialogTimeoutErrorMessage, DIALOG_TIMEOUT_ERROR_MESSAGE, String.class);
			writer.write(this.dialogTimeout, DIALOG_TIMEOUT, Long.class);

			writer.close();
		} catch (Exception e) {
			logger.error("Error while persisting the Rule state in file", e);
		}
	}

	/**
	 * Load and create LinkSets and Link from persisted file
	 * 
	 * @throws Exception
	 */
	public void load() throws FileNotFoundException {

		XMLObjectReader reader = null;
		try {
			reader = XMLObjectReader.newInstance(new FileInputStream(persistFile.toString()));

			reader.setBinding(binding);
			this.serviceCenterGt = reader.read(SC_GT, String.class);
			this.serviceCenterSsn = reader.read(SC_SSN, Integer.class);
			
	//		this.noRoutingRuleConfiguredMessage = reader.read(NO_ROUTING_RULE_CONFIGURED_ERROR_MESSAGE, String.class);
	//		this.serverErrorMessage = reader.read(SERVER_ERROR_MESSAGE, String.class);
			this.dialogTimeoutErrorMessage = reader.read(DIALOG_TIMEOUT_ERROR_MESSAGE, String.class);
			this.dialogTimeout = reader.read(DIALOG_TIMEOUT, Long.class);

			reader.close();
		} catch (XMLStreamException ex) {
			// this.logger.info(
			// "Error while re-creating Linksets from persisted file", ex);
		}
	}

}
