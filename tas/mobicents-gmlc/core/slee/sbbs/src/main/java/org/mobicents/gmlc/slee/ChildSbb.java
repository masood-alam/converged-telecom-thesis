package org.mobicents.gmlc.slee;

import java.io.IOException;
import java.io.PrintWriter;

import javax.slee.ActivityContextInterface;
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.SbbLocalObject;
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.TimerID;
import javax.slee.facilities.TimerOptions;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

//import javax.naming.Context;

//import javax.naming.InitialContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javax.slee.ActivityEndEvent;
import javax.slee.SbbID;
import javax.slee.EventContext;


import net.java.slee.resource.http.HttpServletRaActivityContextInterfaceFactory;
import net.java.slee.resource.http.HttpServletRaSbbInterface;
import net.java.slee.resource.http.HttpSessionActivity;
import net.java.slee.resource.http.events.HttpServletRequestEvent;

import net.java.slee.resource.http.HttpServletRequestActivity;

import org.apache.log4j.Logger;
import javax.slee.nullactivity.NullActivity;
import org.mobicents.slee.SbbContextExt;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.sccp.parameter.EncodingScheme;

import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;

import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;

// -what?
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;



import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;

import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle0001;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle0010;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle0011;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle0100;
import org.mobicents.protocols.ss7.sccp.impl.parameter.GlobalTitle0001Impl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.GlobalTitle0011Impl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.GlobalTitle0100Impl;


import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;

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

import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle0100;
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

public abstract class ChildSbb implements Sbb, ChildInterface {

	private AddressString serviceCenterAddress;
	private SccpAddress serviceCenterSCCPAddress = null;

	// 3.0
	protected ParameterFactory sccpParameterFact;

	protected Tracer logger;
  protected SbbContextExt sbbContext;
	
	private TimerFacility timerFacility = null;

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;

	protected static final ResourceAdaptorTypeID mapRATypeID = new ResourceAdaptorTypeID("MAPResourceAdaptorType",
			"org.mobicents", "2.0");
	protected static final String mapRaLink = "MAPRA";

	
	public void onTimerEvent(TimerEvent event, ActivityContextInterface aci) {

		if (this.logger.isWarningEnabled()) {
			this.logger.warning(String.format("onTimerEvent()") );
		}

	}

	


	public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci) {
		logger.info("Got an activity end for activity " + aci.getActivity());
	}

	private final String getSbbId() {
		SbbID sbbId = this.sbbContext.getSbb();
		return sbbId.toString();
	}
	
	/* MAP Component Events */
	
	
public void onInvokeTimeout(InvokeTimeout evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onInvokeTimeout" + evt);
		}
	}

	
	/**
	 * Dialog Events
	 */

	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogDelimiter=" + evt);
		}
	}

	public void onDialogAccept(DialogAccept evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogAccept" + evt);
		}
	}

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogReject" + evt);
		}
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogUserAbort" + evt);
		}
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogProviderAbort" + evt);
		}
	}

	public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogClose" + evt);
		}
	}

	public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogNotice" + evt);
		}
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogTimeout" + evt);
		}
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogRequest" + evt);
		}
	}

	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Rx :  onErrorComponent " + event + " Dialog=" + event.getMAPDialog());
		}
		// if (mapErrorMessage.isEmAbsentSubscriberSM()) {
		// this.sendReportSMDeliveryStatusRequest(SMDeliveryOutcome.absentSubscriber);
		// }

//		SmsEvent original = this.getOriginalSmsEvent();

//		if (original != null) {
//			if (original.getSms().getDestSystemId() != null) {
//				this.sendFailureDeliverSmToEsms(original);
//			}
//		}
	}


	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onRejectComponent" + event);

//		SmsEvent original = this.getOriginalSmsEvent();

//		if (original != null) {
//			if (original.getSms().getDestSystemId() != null) {
//				this.sendFailureDeliverSmToEsms(original);
//			}
//		}
	}


	public void onDialogRelease(DialogRelease evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			// TODO : Should be fine
			this.logger.info("Rx :  DialogRelease " + evt);
		}

	// Since AtiSbb is not suspending, only ParentActivityContextInterface is important (needs to resume and EndActvity in case of errors)
	
   logger.info("this.getNullActivityEventContext()=" + this.getNullActivityEventContext() );
		if (this.getNullActivityEventContext() != null)
		{
		 AtiActivityContextInterface atiSbbActivityContextInterface = this.asSbbActivityContextInterface(this
				.getNullActivityEventContext().getActivityContextInterface());
		 this.resumeNullActivityEventDelivery(atiSbbActivityContextInterface, this.getNullActivityEventContext());
		}
	}

	/*
	
	*/
		protected SccpAddress getServiceCenterSccpAddress() {
		if (this.serviceCenterSCCPAddress == null) {
			  	//GlobalTitle gtHLR = GlobalTitle.getInstance(0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL, "923330055101");
//      	EncodingScheme es = new BCDEvenEncodingScheme();
//			GlobalTitle gt = new GlobalTitle0100Impl( "923330055101", 0, es, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL);
		GlobalTitle gt = sccpParameterFact.createGlobalTitle("923330055101", 0, NumberingPlan.ISDN_TELEPHONY, null,
				NatureOfAddress.INTERNATIONAL);

	//		this.serviceCenterSCCPAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, 0x93);
		this.serviceCenterSCCPAddress = sccpParameterFact.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, 0x93);

		}
		return this.serviceCenterSCCPAddress;
	}
	
	
	protected AddressString getServiceCenterAddressString() {

		if (this.serviceCenterAddress == null) {
			//this.logger.info("smscPropertiesManagement.getServiceCenterGt()="+smscPropertiesManagement.getServiceCenterGt() );
			this.serviceCenterAddress = this.mapParameterFactory.createAddressString(
					AddressNature.international_number,
					org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "923330055101");
		}
		return this.serviceCenterAddress;
	}
	
	
   
   	public void sbbActivate() {
		// TODO Auto-generated method stub

	}

	public void sbbCreate() throws CreateException {
		// TODO Auto-generated method stub

	}

	public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {

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
			this.logger = this.sbbContext.getTracer("Child" + getClass().getName() );
		try {
		//	Context ctx = (Context) new InitialContext().lookup("java:comp/env");
		//	this.mapAcif = (MAPContextInterfaceFactory) ctx.lookup("slee/resources/map/2.0/acifactory");
		//	this.mapProvider = (MAPProvider) ctx.lookup("slee/resources/map/2.0/provider");
		//	this.mapParameterFactory = this.mapProvider.getMAPParameterFactory();
			this.mapAcif = (MAPContextInterfaceFactory) this.sbbContext.getActivityContextInterfaceFactory(mapRATypeID);
			this.mapProvider = (MAPProvider) this.sbbContext.getResourceAdaptorInterface(mapRATypeID, mapRaLink);
			this.mapParameterFactory = this.mapProvider.getMAPParameterFactory();
			this.timerFacility = this.sbbContext.getTimerFacility();

			// 3.0
			this.sccpParameterFact = new ParameterFactoryImpl();

		} catch (Exception e) {
			logger.info("setSbbContext failed ", e);
		}
	}

	
	public void unsetSbbContext() {
		// TODO Auto-generated method stub
	  this.sbbContext = null;
	  this.logger = null;
	}


 
   
	
	/**
	 * CMPs
	 */
	public abstract void setNullActivityEventContext(EventContext eventContext);

	public abstract EventContext getNullActivityEventContext();

	/**
	 * Sbb ACI
	 */
	public abstract AtiActivityContextInterface asSbbActivityContextInterface(ActivityContextInterface aci);

	
	protected void resumeNullActivityEventDelivery(AtiActivityContextInterface atiSbbActivityContextInterface,
			EventContext nullActivityEventContext) {
		if (atiSbbActivityContextInterface.getPendingEventsOnNullActivity() == 0) {
			// If no more events pending, lets end NullActivity
			// nullActivityEventContext.getActivityContextInterface().getActivity() returns HttpServletRequestActivity
			// and not NullActivty which was using in case of custom Events
//			NullActivity nullActivity = (NullActivity) nullActivityEventContext.getActivityContextInterface()
	//				.getActivity();
		//	nullActivity.endActivity();
	//		HttpServletRequestActivity httpActivity = (HttpServletRequestActivity) nullActivityEventContext.getActivityContextInterface()
	//				.getActivity();
	//		httpActivity.endActivity();

	//		if (logger.isInfoEnabled()) {
	//			this.logger.info(String.format("No more events to be fired on NullActivity=%s:  Ended", nullActivity));
	//		}
		}
		// Resume delivery for rest of the SMS's for this MSISDN
		if (nullActivityEventContext.isSuspended()) {
			nullActivityEventContext.resumeDelivery();
		}
	}





}
