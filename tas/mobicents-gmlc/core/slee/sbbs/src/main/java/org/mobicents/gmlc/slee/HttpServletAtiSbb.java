package org.mobicents.gmlc.slee;

import java.io.IOException;
import java.io.PrintWriter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.slee.ActivityContextInterface;
import javax.slee.ChildRelation;

import javax.slee.ActivityEndEvent;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.SbbID;
import javax.slee.SbbLocalObject;
import javax.slee.EventContext;


import net.java.slee.resource.http.HttpServletRaActivityContextInterfaceFactory;
import net.java.slee.resource.http.HttpServletRaSbbInterface;
import net.java.slee.resource.http.HttpSessionActivity;
import net.java.slee.resource.http.events.HttpServletRequestEvent;

import org.apache.log4j.Logger;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivity;
import org.mobicents.slee.SbbContextExt;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;

import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;

import org.mobicents.protocols.ss7.sccp.parameter.EncodingScheme;


import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.USSDString;
import org.mobicents.protocols.ss7.map.api.service.supplementary.MAPDialogSupplementary;

import org.mobicents.protocols.ss7.map.api.service.supplementary.ProcessUnstructuredSSRequest;

import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSRequest;

import org.mobicents.protocols.ss7.map.api.service.supplementary.UnstructuredSSResponse;

//Masood
import org.mobicents.protocols.ss7.map.api.datacoding.CBSDataCodingScheme;
import org.mobicents.protocols.ss7.map.datacoding.CBSDataCodingSchemeImpl;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRelease;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.InvokeTimeout;
import org.mobicents.slee.resource.map.events.RejectComponent;


import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;

import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

import org.mobicents.protocols.ss7.map.api.primitives.SubscriberIdentity;

import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.RequestedInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.LocationInformation;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberState;
import org.mobicents.protocols.ss7.map.api.primitives.CellGlobalIdOrServiceAreaIdOrLAI;
import org.mobicents.protocols.ss7.map.api.primitives.CellGlobalIdOrServiceAreaIdFixedLength;

public abstract class HttpServletAtiSbb implements Sbb, ParentInterface {
	// Keep timeout for event suspend to be maximum
	// in milli seconds  1000*60*3 = 3 minutes
	private static final int EVENT_SUSPEND_TIMEOUT = 1000 * 10;

	private AddressString serviceCenterAddress;
	private SccpAddress serviceCenterSCCPAddress = null;

//	private MAPApplicationContext atiMAPApplicationContext = null;

	protected Tracer logger;
  	protected SbbContextExt sbbContext;
	//private HttpServletRaSbbInterface httpServletRaSbbInterface;

//	private HttpServletRaActivityContextInterfaceFactory httpServletRaActivityContextInterfaceFactory;
//	protected MAPContextInterfaceFactory mapAcif;
//	protected MAPProvider mapProvider;
//	protected MAPParameterFactory mapParameterFactory;

	private HttpServletRaSbbInterface httpProvider;
	private HttpServletRaActivityContextInterfaceFactory httpServletRaActivityContextInterfaceFactory;

	private static final ResourceAdaptorTypeID httpRATypeID = new ResourceAdaptorTypeID("HttpServletResourceAdaptorType", "org.mobicents", "1.0");
	private static final String httpRaLink = "HttpServletRA";

	/** Creates a new instance of CallSbb */
	public HttpServletAtiSbb() {
	}


	public void onSessionGet(HttpServletRequestEvent event, ActivityContextInterface aci){
		try{
			System.out.println("Session is active");
			

			HttpServletResponse response = event.getResponse();
			PrintWriter w = null;

			w = response.getWriter();
			w.print("onSessionGet OK! Served by SBB = " + getSbbId());
			w.flush();
			response.flushBuffer();
			
		} catch(Exception e){
			e.printStackTrace();
		}
	}

 
  
	public void onGet(HttpServletRequestEvent event, ActivityContextInterface aci, EventContext eventContext) {
	
	
	 // ev = event;
		// Reduce the events pending to be fired on this ACI
		ParentActivityContextInterface parentSbbActivityContextInterface = this.asSbbActivityContextInterface(aci);
		int pendingEventsOnNullActivity = parentSbbActivityContextInterface.getPendingEventsOnNullActivity();

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received Http GET. pendingEventsOnNullActivity=" + pendingEventsOnNullActivity + " event= "
					+ event + "this=" + this);
		}

		pendingEventsOnNullActivity = pendingEventsOnNullActivity - 1;
		parentSbbActivityContextInterface.setPendingEventsOnNullActivity(pendingEventsOnNullActivity);

//		aci.detach(this.sbbContext.getSbbLocalObject());

		// if you attached this , on resume, the GET message will be delivered to next SBB instance !!!
//		parentSbbActivityContextInterface.attach(this.sbbContext.getSbbLocalObject());

		// Suspend the delivery of event till unsuspended by other
		// event-handlers
		eventContext.suspendDelivery(EVENT_SUSPEND_TIMEOUT);
	this.setNullActivityEventContext(eventContext);

//	 System.out.println(eventContext);
   String msisdn =	event.getRequest().getParameter("msisdn");
   logger.info("msisdn="+msisdn);
 
	
   try {
   ChildRelation relation = this.getAtiSbb();
 //  System.out.println("relation="+ relation.create());
   ChildSbbLocalObject child = (ChildSbbLocalObject) relation.create();
   child.setupAnyTimeInterrogationRequest(msisdn, this.getNullActivityEventContext());
   forwardEvent(child, aci);
  } catch (Exception e) {
 	  logger.severe("Unable to create AtiSbb", e);
 	  //	ParentActivityContextInterface parentSbbActivityContextInterface = this.asSbbActivityContextInterface(this
		//			.getNullActivityEventContext().getActivityContextInterface());
			this.resumeNullActivityEventDelivery(parentSbbActivityContextInterface, this.getNullActivityEventContext());
  }
	}

	private void forwardEvent(SbbLocalObject child, ActivityContextInterface aci) {
		try {
			aci.attach(child);
			aci.detach(sbbContext.getSbbLocalObject());
		} catch (Exception e) {
			logger.severe("Unexpected error in forwardEvent: ", e);
		}
	}

	// 28-7-16 Disabled activityend/activity end
//	public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci) {
//		logger.info("Got an activity end for activity " + aci.getActivity());
//	}

	private final String getSbbId() {
		SbbID sbbId = this.sbbContext.getSbb();
		return sbbId.toString();
	}
	
	

	/**
	 * Dialog Events
	 */


   
	public void sbbActivate() {
		// TODO Auto-generated method stub

	}


	public void sbbCreate() throws CreateException {
		// TODO Auto-generated method stub

	}

	
	public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
		// TODO Auto-generated method stub

	}

	
	public void sbbLoad() {
		// TODO Auto-generated method stub

	}

	
	public void sbbPassivate() {
		// TODO Auto-generated method stub

	}

	
	public void sbbPostCreate() throws CreateException {
		// TODO Auto-generated method stub

	}

	
	public void sbbRemove() {
		// TODO Auto-generated method stub

	}

	
	public void sbbRolledBack(RolledBackContext arg0) {
		// TODO Auto-generated method stub

	}

	
	public void sbbStore() {
		// TODO Auto-generated method stub

	}

	
	public void setSbbContext(SbbContext context) {
			this.sbbContext = (SbbContextExt) context;
			this.logger = this.sbbContext.getTracer("HttpServletAtiSbb");
		try {

			httpServletRaActivityContextInterfaceFactory = (HttpServletRaActivityContextInterfaceFactory)this.sbbContext
				.getActivityContextInterfaceFactory(httpRATypeID);
			this.httpProvider = (HttpServletRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(httpRATypeID, httpRaLink);		

		} catch (Exception e) {
			logger.info("setSbbContext failed ", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.slee.Sbb#unsetSbbContext()
	 */
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}


	
	/**
	 * CMPs
	 */
	public abstract void setNullActivityEventContext(EventContext eventContext);

	public abstract EventContext getNullActivityEventContext();

	/**
	 * Sbb ACI
	 */
	public abstract ParentActivityContextInterface asSbbActivityContextInterface(ActivityContextInterface aci);

	/**
	 * Get HttpServlet child SBB
	 * 
	 * @return
	 */
	public abstract ChildRelation getAtiSbb();
	
 
	
		protected void resumeNullActivityEventDelivery(ParentActivityContextInterface parentSbbActivityContextInterface,
			EventContext nullActivityEventContext) {
		if (parentSbbActivityContextInterface.getPendingEventsOnNullActivity() == 0) {
			// If no more events pending, lets end NullActivity
			NullActivity nullActivity = (NullActivity) nullActivityEventContext.getActivityContextInterface()
					.getActivity();
			nullActivity.endActivity();
			if (logger.isInfoEnabled()) {
				this.logger.info(String.format("No more events to be fired on NullActivity=%s:  Ended", nullActivity));
			}
		}
		// Resume delivery for rest of the SMS's for this MSISDN
		if (nullActivityEventContext.isSuspended()) {
			nullActivityEventContext.resumeDelivery();
		}
	}



  
 	@Override
 	public void setupAnyTimeInterrogationResponse(String msisdn, String cgi, EventContext nullActivityEventContext) {
 		this.logger.info("Received ati response msisdn=" + msisdn + " cgi = " + cgi);
 		
 		
 		this.logger.info("getNullActivityEventContext() = " + this.getNullActivityEventContext() );
 		this.logger.info("nullActivityEventContext = " + nullActivityEventContext );
 		
 		HttpServletRequestEvent ev=null;
 		if (nullActivityEventContext != null)
 		{
 			  ev = (HttpServletRequestEvent) nullActivityEventContext.getEvent();
 				try{
			    HttpServletResponse response = ev.getResponse();
					PrintWriter w = null;

					w = response.getWriter();
			//		w.print("onSessionGet OK! Served by SBB = " + getSbbId());
					w.print("ATI("+msisdn + ") = " + cgi);
					w.flush();
					response.flushBuffer();		
		    } catch(Exception e){
			    e.printStackTrace();
		    }


		   if (nullActivityEventContext.isSuspended()) {
				nullActivityEventContext.resumeDelivery();
	  	}
		 }
		 
		 

 		
 	} 

}
