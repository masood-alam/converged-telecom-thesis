/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.gmlc;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
//import org.mobicents.smsc.smpp.SmppEncoding;
//import org.mobicents.smsc.smpp.SmppOamMessages;
import org.mobicents.ss7.management.console.ShellExecutor;

/**
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public class GmlcShellExecutor implements ShellExecutor {

	private static final Logger logger = Logger.getLogger(GmlcShellExecutor.class);

	private GmlcManagement gmlcManagement;

  private static GmlcPropertiesManagement gmlcPropertiesManagement;

	public GmlcShellExecutor() {

	}

	public void start() throws Exception {
		gmlcPropertiesManagement = GmlcPropertiesManagement.getInstance(this.getGmlcManagement().getName());
		if (logger.isInfoEnabled()) {
			logger.info("Started GmlcShellExecutor " + this.getGmlcManagement().getName());
		}
	}

	/**
	 * @return the m3uaManagement
	 */
	public GmlcManagement getGmlcManagement() {
		return gmlcManagement;
	}

	/**
	 * @param m3uaManagement
	 *            the m3uaManagement to set
	 */
	public void setGmlcManagement(GmlcManagement gmlcManagement) {
		this.gmlcManagement = gmlcManagement;
	}


	
	@Override
	public String execute(String[] args) {
		try {
			if (args.length < 2 || args.length > 50) {
				// any command will have atleast 3 args
				return GmlcOAMMessages.INVALID_COMMAND;
			}

			if (args[1] == null) {
				return GmlcOAMMessages.INVALID_COMMAND;
			}
       
			 if (args[1].equals("set")) {
				return this.manageSet(args);
			} else if (args[1].equals("get")) {
				return this.manageGet(args);
			}

            return GmlcOAMMessages.INVALID_COMMAND;
		} catch (Throwable e) {
			logger.error(String.format("Error while executing comand %s", Arrays.toString(args)), e);
			return e.toString();
		}
	}

	private String manageSet(String[] options) throws Exception {
		if (options.length < 4) {
			return GmlcOAMMessages.INVALID_COMMAND;
		}

		String parName = options[2].toLowerCase();
        try {

		if (parName.equals("scgt")) {
			gmlcPropertiesManagement.setServiceCenterGt(options[3]);
		} else if (parName.equals("scssn")) {
			int val = Integer.parseInt(options[3]);
			gmlcPropertiesManagement.setServiceCenterSsn(val);
		} else {
				return GmlcOAMMessages.INVALID_COMMAND;
			}
		} catch (IllegalArgumentException e) {
			return String.format(GmlcOAMMessages.ILLEGAL_ARGUMENT, parName, e.getMessage());
		}

		return GmlcOAMMessages.PARAMETER_SUCCESSFULLY_SET;
	}

	private String manageRemove(String[] options) throws Exception {
		if (options.length < 3) {
			return GmlcOAMMessages.INVALID_COMMAND;
		}

		String parName = options[2].toLowerCase();
	//	try {
	//		if (parName.equals("esmedefaultcluster")) {
	//			smscPropertiesManagement.setEsmeDefaultClusterName(null);

//			} else {
	//			return SMSCOAMMessages.INVALID_COMMAND;
	//		}
	//	} catch (IllegalArgumentException e) {
	//		return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, parName, e.getMessage());
	//	}

		return GmlcOAMMessages.PARAMETER_SUCCESSFULLY_REMOVED;
	}

	private String manageGet(String[] options) throws Exception {
		if (options.length == 3) {
			String parName = options[2].toLowerCase();

			StringBuilder sb = new StringBuilder();
			sb.append(options[2]);
			sb.append(" = ");
			if (parName.equals("scgt")) {
				sb.append(gmlcPropertiesManagement.getServiceCenterGt());
			} else if (parName.equals("scssn")) {
				sb.append(gmlcPropertiesManagement.getServiceCenterSsn());
			} else {
				return GmlcOAMMessages.INVALID_COMMAND;
			}

			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();

			sb.append("scgt = ");
			sb.append(gmlcPropertiesManagement.getServiceCenterGt());
			sb.append("\n");

			sb.append("scssn = ");
			sb.append(gmlcPropertiesManagement.getServiceCenterSsn());
			sb.append("\n");
	

			return sb.toString();
		}
	}


	@Override
	public boolean handles(String command) {
		return "gmlc".equals(command);
	}

//	public static void main(String[] args) throws Exception {
//		String command = "smsc mapcache get 1234567";
//		SMSCShellExecutor exec = new SMSCShellExecutor();
//		exec.getMapVersionCache(command.split(" "));
//	}

}

