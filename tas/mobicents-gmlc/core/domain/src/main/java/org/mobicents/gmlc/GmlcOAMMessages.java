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

/**
 * 
 * @author Amit Bhayani
 * 
 */
public interface GmlcOAMMessages {

	/**
	 * Pre defined messages
	 */
	public static final String INVALID_COMMAND = "Invalid Command";

	public static final String ILLEGAL_ARGUMENT = "Illegal argument %s: %s";

	public static final String PARAMETER_SUCCESSFULLY_SET = "Parameter has been successfully set";

	public static final String PARAMETER_SUCCESSFULLY_REMOVED = "Parameter has been successfully removed";



	/**
	 * Generic constants
	 */
	public static final String TAB = "        ";

	public static final String NEW_LINE = "\n";

	public static final String COMMA = ",";

	/**
	 * Show command specific constants
	 */

	public static final String SHOW_STARTED = " started=";


}
