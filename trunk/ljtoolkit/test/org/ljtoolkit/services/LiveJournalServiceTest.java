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

package org.ljtoolkit.services;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.ljtoolkit.utils.LJResourceBundle;
import org.testng.Assert;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

public class LiveJournalServiceTest {
	private LiveJournalService service = LiveJournalService.getInstance();
	private LJResourceBundle bundle = LJResourceBundle.getInstance("org.ljtoolkit.services");
	
	@BeforeGroups(groups = "service")
	public void setup() {
		// setup the user
		User user = new User();
		user.setUsername(bundle.getResource("LiveJournalServiceTest.test.username"));
		user.setPassword(bundle.getResource("LiveJournalServiceTest.test.password"));
		service.setUser(user);		
	}
	
	@Test(groups = "service")
	public void getEventsTest() {	
		GetEventParams params = new GetEventParams();
		params.setSelectType(SelectType.LAST_N);
		
		List<Event> events = null;
		
		try {
			events = service.getEvents(params);
		} catch (LiveJournalServiceException e) {
			System.out.println("Error trying to fetch events, cause: " + e.getMessage());
		}
		
		Assert.assertNotNull(events);
		Assert.assertTrue(events.size() > 0);
		
		for(Event event : events) {
			System.out.println("Event id: [" + event.getId() + "] Subject: [" + event.getSubject() + "]");
		}
	}
	
	@Test(groups = "service")
	public void postEventTest() {
		TransformEventParams params = new TransformEventParams();
		
		params.setSubject("This is a test post to Live Journal");
		params.setTimestamp(new Date());
		params.setLineEndings(BaseEventParams.PC_LINE_ENDING);
		params.setSecurity(EventSecurity.PRIVATE);
		params.setEvent("This is a test event\n with a line ending! And a Tag");
		
		PostEventMetadata meta = new PostEventMetadata();
		Map<String, String> props = new HashMap<String, String>();
		props.put("taglist", "test-tag");
		meta.setPropsMap(props);
		
		params.setMetadata(meta);
		
		int eventId = 0;
		try {
			eventId = service.postEvent(params);
		} catch (LiveJournalServiceException e) {
			System.out.println("Error trying to post event, cause: " + e.getMessage());
		}
		
		Assert.assertTrue(eventId != 0);
		
		System.out.println("Returned eventId is " + eventId);
	}

	@Test(groups = "service")
	public void fullCycleTest() {
		int eventId = postEvent();
		getEvent(eventId);
		editEvent(eventId);
		deleteEvent(eventId);
	}

	private int postEvent() {
		TransformEventParams params = new TransformEventParams();
		
		buildEvent(params);
		
		int eventId = 0;
		try {
			eventId = service.postEvent(params);
		} catch (LiveJournalServiceException e) {
			System.out.println("Error trying to post event, cause: " + e.getMessage());
		}
		
		Assert.assertTrue(eventId != 0);		
		System.out.println("Returned eventId is " + eventId);	
		return eventId;
	}
	
	private void buildEvent(TransformEventParams params) {
		// setup the essentials
		params.setSubject("This is a test event post to Live Journal");
		params.setTimestamp(new Date());
		params.setLineEndings(BaseEventParams.PC_LINE_ENDING);
		params.setSecurity(EventSecurity.PRIVATE);
		params.setAction(EventAction.POST);
		
		// create the content
		StringBuffer eventContent = new StringBuffer();
		eventContent.append("This is test event...\n");
		eventContent.append("There are line endings in this post...\n");
		eventContent.append("And there are tags too!");
		params.setEvent(eventContent.toString());
		
		// create a tag
		PostEventMetadata meta = new PostEventMetadata();
		Map<String, String> props = new HashMap<String, String>();
		props.put("taglist", "foo-tag,bar-tag");
		meta.setPropsMap(props);		
		params.setMetadata(meta);
	}
	
	private void getEvent(int eventId) {
		GetEventParams params = new GetEventParams();
		params.setSelectType(SelectType.ONE);
		params.setItemId(eventId);
		
		List<Event> events = null;
		
		try {
			events = service.getEvents(params);
		} catch (LiveJournalServiceException e) {
			System.out.println("Error trying to fetch events, cause: " + e.getMessage());
		}
		
		Assert.assertNotNull(events);
		Assert.assertTrue(events.size() == 1);
		
		for(Event event : events) {
			Assert.assertTrue(event.getSubject().equalsIgnoreCase("This is a test event post to Live Journal"));
			Assert.assertTrue(event.getId() == eventId);
			System.out.println("Event id: [" + event.getId() + "] Subject: [" + event.getSubject() + "]");
		}
		
	}
	
	private void editEvent(int eventId) {
		TransformEventParams params = new TransformEventParams();
		params.setItemId(eventId);
		
		rebuildEvent(params);
		
		try {
			service.editEvent(params);
		} catch (LiveJournalServiceException e) {
			System.out.println("Error trying to post event, cause: " + e.getMessage());
		}		
	}
	
	private void rebuildEvent(TransformEventParams params) {
		// setup the essentials
		params.setSubject("This is a edit test event post to Live Journal");
		params.setTimestamp(new Date());
		params.setLineEndings(BaseEventParams.PC_LINE_ENDING);
		params.setSecurity(EventSecurity.PRIVATE);
		params.setAction(EventAction.EDIT);
		
		// create the content
		StringBuffer eventContent = new StringBuffer();
		eventContent.append("This is test edit event...\n");
		eventContent.append("There are line endings in this edit...\n");
		eventContent.append("And there are tags too!");
		params.setEvent(eventContent.toString());
		
		// create a tag
		PostEventMetadata meta = new PostEventMetadata();
		Map<String, String> props = new HashMap<String, String>();
		props.put("taglist", "edit-tag");
		meta.setPropsMap(props);		
		params.setMetadata(meta);	
	}
	
	private void deleteEvent(int eventId) {
		TransformEventParams params = new TransformEventParams();
		params.setItemId(eventId);
		params.setEvent("");
		params.setAction(EventAction.DELETE);
		
		try {
			service.editEvent(params);
		} catch (LiveJournalServiceException e) {
			System.out.println("Error trying to post event, cause: " + e.getMessage());
		}				
	}	
}
