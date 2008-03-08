/*
 * Copyright 2008 Troy Bourdon
 * 
 * This file is part of LJToolkit.
 *
 * LJToolkit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LJToolkit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with LJToolkit.  If not, see <http://www.gnu.org/licenses/>.    
 */

package org.ljtoolkit.client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ljtoolkit.data.Event;
import org.ljtoolkit.data.PostEventMetadata;
import org.ljtoolkit.data.User;
import org.ljtoolkit.enums.EventAction;
import org.ljtoolkit.enums.EventSecurity;
import org.ljtoolkit.enums.SelectType;
import org.ljtoolkit.exceptions.LiveJournalServiceException;
import org.ljtoolkit.params.BaseEventParams;
import org.ljtoolkit.params.GetEventParams;
import org.ljtoolkit.params.TransformEventParams;
import org.ljtoolkit.services.LiveJournalService;
import org.ljtoolkit.utils.LJResourceBundle;

/**
 * The purpose of this class is to provide services to the {@link org.ljtoolkit.client.LJClient} class.
 * The LJClient class serves as the driver for by parsing the command line and in turn
 * calling on the services of this class.
 * 
 * @author Troy Bourdon
 * @see LJClient
 *
 */
public class LJClientService {
	private static Log logger = LogFactory.getLog(LJClientService.class);
	private static LJResourceBundle bundle = LJResourceBundle.getInstance("org.ljtoolkit.client");
	
	/**
	 * Method called by the LJClient's main method to dispatch the desired action.
	 * 
	 * @param cmdLine
	 */
	public static void dispatchAction(LJClientCmdLine cmdLine) {
		if(cmdLine.getAction().equals(LJClientAction.ADD)){ addEvent(cmdLine); }
		if(cmdLine.getAction().equals(LJClientAction.UPDATE)){ editEvent(cmdLine); }
		if(cmdLine.getAction().equals(LJClientAction.DELETE)){ deleteEvent(cmdLine); }
		if(cmdLine.getAction().equals(LJClientAction.LIST)){ listEvents(cmdLine); }
	}
	
	/**
	 * Method to post an event to Live Journal.
	 * 
	 * @param cmdLine
	 */
	public static void addEvent(LJClientCmdLine cmdLine) {
		LiveJournalService service = LiveJournalService.getInstance();
		TransformEventParams params = new TransformEventParams();
		
		if(!setupUserAndParseEventFile(service, params, cmdLine)) {	return; }
		
		// set timestamp
		params.setTimestamp(new Date());
		
		if(params.getSubject() != null && params.getEvent() != null) {
			int eventId = 0;
			try {
				eventId = service.postEvent(params);
			} catch (LiveJournalServiceException e) {
				outputError(bundle.getResource("add.event.error.msg", e.getMessage()), e);
			}
			
			assert(eventId != 0);	
			String status = bundle.getResource("add.event.status.msg", eventId);
			System.out.println(buildStatusMessage(status));			
		} else {
			outputError(bundle.getResource("add.event.error.unknown"));
		}
	}

	/**
	 * Method to edit an event to Live Journal.
	 * 
	 * @param cmdLine
	 */
	private static void editEvent(LJClientCmdLine cmdLine) {
		LiveJournalService service = LiveJournalService.getInstance();
		TransformEventParams params = new TransformEventParams();
		
		if(!setupUserAndParseEventFile(service, params, cmdLine)) {	return; }
		
		// set the timestamp and itemId before editing
		params.setTimestamp(new Date());
		params.setAction(EventAction.EDIT);
		params.setItemId(Integer.valueOf(cmdLine.getId()).intValue());
		
		if(params.getSubject() != null && params.getEvent() != null) {
			try {
				service.editEvent(params);
			} catch (LiveJournalServiceException e) {
				outputError(bundle.getResource("edit.event.error.msg", e.getMessage()), e);
			}
			
			String status = bundle.getResource("edit.event.status.msg");
			System.out.println(buildStatusMessage(status));			
		} else {
			outputError(bundle.getResource("edit.event.error.unknown"));
		}		
	}

	/**
	 * Method to delete an event from Live Journal
	 * 
	 * @param cmdLine
	 */
	private static void deleteEvent(LJClientCmdLine cmdLine) {
		LiveJournalService service = LiveJournalService.getInstance();
		TransformEventParams params = new TransformEventParams();
			
		userSetup(service, cmdLine);
		
		// set the itemId before editing
		params.setItemId(Integer.valueOf(cmdLine.getId()).intValue());
		params.setAction(EventAction.DELETE);
		
		try {
			service.editEvent(params);
		} catch (LiveJournalServiceException e) {
			outputError(bundle.getResource("delete.event.error.msg", e.getMessage()), e);
		}
		
		String status = bundle.getResource("delete.event.status.msg", params.getItemId());
		System.out.println(buildStatusMessage(status));					
	}

	/**
	 * Method to list the user's events from Live Journal
	 * 
	 * @param cmdLine
	 */
	private static void listEvents(LJClientCmdLine cmdLine) {
		LiveJournalService service = LiveJournalService.getInstance();
		GetEventParams params = new GetEventParams();
		List<Event> events = null;
						
		userSetup(service, cmdLine);
		params.setSelectType(SelectType.LAST_N);
		params.setHowMany(50);
		params.setTruncate(10);
		params.setLineEndings(BaseEventParams.PC_LINE_ENDING);
		
		try {
			events = service.getEvents(params);
			buildListEventsOutput(events);
		} catch (LiveJournalServiceException e) {
			outputError(bundle.getResource("list.event.error.msg", e.getMessage()), e);
		}
	}

	private static boolean setupUserAndParseEventFile(
			LiveJournalService service, TransformEventParams params, LJClientCmdLine cmdLine) {

		userSetup(service, cmdLine);
		
		try {
			parseEventFile(params, cmdLine);
		} catch (FileNotFoundException x) {
			outputError(bundle.getResource("parse.event.find.file.error", cmdLine.getFilePath()), x);
			return false;
		} catch (IOException x) {
			outputError(bundle.getResource("parse.event.close.file.error", cmdLine.getFilePath()), x);
			return false;
		} catch (MalformedPostFileException x) {
			outputError(bundle.getResource("parse.event.malformed.file.error", cmdLine.getFilePath()), x);
			return false;
		}
		
		return true;
	}
	
	private static void userSetup(LiveJournalService service, LJClientCmdLine cmdLine) {
		User user = new User();
		user.setUsername(cmdLine.getUsername());
		user.setPassword(cmdLine.getPassword());
		service.setUser(user);		
	}
	
	private static void parseEventFile(TransformEventParams params, LJClientCmdLine cmdLine)
		throws FileNotFoundException, IOException {
		FileReader file = new FileReader(cmdLine.getFilePath());
		BufferedReader buffer = new BufferedReader(file);
		StringBuffer contentBuffer = new StringBuffer();
		String errMsg = null;
		
		try {
			String line = null;
			int lineNum = 0;
			while((line = buffer.readLine()) != null) {
				String[] strs = null;
				String key = null;
				String value = null;
				
				lineNum++;		
				if(lineNum == 1) {
					strs = StringUtils.split(line, ':');
					if(strs.length > 0)
						key = StringUtils.trim(strs[0]);
					if(strs.length > 1)
						value = StringUtils.trim(strs[1]);
					
					setSubjectParameter(params, key, value);
					continue;
				} else if(lineNum == 2) {
					strs = StringUtils.split(line, ':');
					if(strs.length > 0)
						key = StringUtils.trim(strs[0]);
					if(strs.length > 1)
						value = StringUtils.trim(strs[1]);

					setSecurityParameter(params, key, value);
					continue;
				} else if(lineNum == 3) {
					strs = StringUtils.split(line, ':');
					if(strs.length > 0)
						key = StringUtils.trim(strs[0]);
					if(strs.length > 1)
						value = StringUtils.trim(strs[1]);

					setTagsParameter(params, key, value);
					continue;
				} else {
					if(lineNum > 4) {
						contentBuffer.append(line);
					} 
				}
			}
		} catch (IOException e) {
			outputError(bundle.getResource("parse.event.read.file.error", cmdLine.getFilePath()), e);
		} catch (MalformedPostFileException e) {
			outputError(bundle.getResource("parse.event.malformed.file.error", cmdLine.getFilePath()), e);
		} finally {
			file.close();
			buffer.close();			
		}
				
		params.setEvent(contentBuffer.toString());
	}

	private static void tagsSetup(TransformEventParams params, String tags) {
		PostEventMetadata meta = new PostEventMetadata();
		Map<String, String> props = new HashMap<String, String>();
		props.put("taglist", tags);
		meta.setPropsMap(props);
		
		params.setMetadata(meta);		
	}
	
	private static void setSubjectParameter(TransformEventParams params, String key, String title) 
		throws MalformedPostFileException {
		
		if(key == null) 
			throw new MalformedPostFileException(bundle.getResource("subject.param.no.key"));
		
		if(!key.equalsIgnoreCase("title")) {
			throw new MalformedPostFileException(bundle.getResource("subject.param.no.title.key"));
		} else {
			if(title != null && title.length() > 0)
				params.setSubject(title);
			else
				throw new MalformedPostFileException(bundle.getResource("subject.param.no.title"));
		}			
	}

	private static void setSecurityParameter(TransformEventParams params, String key, String security) 
		throws MalformedPostFileException {
	
		if(key == null) 
			throw new MalformedPostFileException(bundle.getResource("security.param.no.key"));
		
		if(!key.equalsIgnoreCase("security")) {
			throw new MalformedPostFileException(bundle.getResource("security.param.no.key"));
		} else {
			if(security != null && security.length() > 0) {
				if(security.equalsIgnoreCase("public"))
					params.setSecurity(EventSecurity.PUBLIC);
			} 
		}			
	}

	private static void setTagsParameter(TransformEventParams params, String key, String tags) 
		throws MalformedPostFileException {

		if(key == null) 
			throw new MalformedPostFileException(bundle.getResource("tags.param.no.key"));
		
		if(!key.equalsIgnoreCase("tags")) {
			throw new MalformedPostFileException(bundle.getResource("tags.param.no.key"));
		} else {
			if(tags != null && tags.length() > 0) {
				tagsSetup(params, tags);				
			} 
		}			
	}
	
	private static void buildListEventsOutput(List<Event> events) {
		LJClientEventWriter writer = LJClientEventWriter.getInstance();
		writer.outputLiveJournalEvents(events);
	}
	
	private static void outputError(String message, Exception e) {
		logger.error(message, e);
		System.err.println(buildErrorMessage(message));		
	}

	private static void outputError(String message) {
		logger.error(message);		
		System.err.println(buildErrorMessage(message));		
	}

	private static String buildErrorMessage(String error) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("!!! LJClient Error !\n");
		buffer.append("!!!\n");
		buffer.append("!!! " + error + "\n");
		buffer.append("!!!\n");
		
		return buffer.toString();
	}

	private static String buildStatusMessage(String status) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("!!! LJClient Status !\n");
		buffer.append("!!!\n");
		buffer.append("!!! " + status + "\n");
		buffer.append("!!!\n");
		
		return buffer.toString();
	}

}
