package org.mobicents.gmlc.slee;

import java.io.IOException;
import java.io.PrintWriter;

//import javax.naming.Context;
//import javax.naming.InitialContext;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;
import javax.slee.ActivityContextInterface;
import javax.slee.ActivityEndEvent;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.resource.ResourceAdaptorTypeID;

import javax.slee.SbbID;
import javax.slee.SbbLocalObject;
import javax.slee.EventContext;

import javax.slee.ChildRelation;


import net.java.slee.resource.http.HttpServletRaActivityContextInterfaceFactory;
import net.java.slee.resource.http.HttpServletRaSbbInterface;
import net.java.slee.resource.http.HttpSessionActivity;
import net.java.slee.resource.http.events.HttpServletRequestEvent;

import org.apache.log4j.Logger;
import javax.slee.facilities.Tracer;
import org.mobicents.slee.SbbContextExt;


import javax.slee.ActivityContextInterface;
//import org.mobicents.atigateway.slee.services.http.server.events.AtiEvent;

//import org.mobicents.atigateway.domain.library.Ati;

import javax.slee.nullactivity.NullActivity;


import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;

import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;

import org.mobicents.protocols.ss7.sccp.parameter.EncodingScheme;

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
import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;

import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;

import org.mobicents.protocols.ss7.map.api.primitives.SubscriberIdentity;
//import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.RequestedInfo;


import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.RequestedInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberInfo;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.LocationInformation;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.SubscriberState;
import org.mobicents.protocols.ss7.map.api.primitives.CellGlobalIdOrServiceAreaIdOrLAI;
import org.mobicents.protocols.ss7.map.api.primitives.CellGlobalIdOrServiceAreaIdFixedLength;


public abstract class AtiSbb extends ChildSbb {
	private static final int EVENT_SUSPEND_TIMEOUT = 1000 *30  ;
	
		private MAPApplicationContext atiMAPApplicationContext = null;


  public AtiSbb() {
  }



//	public void setSbbContext(SbbContext context) {
	
//	super.setSbbContext(context);
//	}

	
//	public void unsetSbbContext() {
//		this.sbbContext = null;
//		super.unsetSbbContext();
//	}

	

	// 28-7-16, Disabled activityend/activity end	
//	public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci) {
//		logger.info("Got an activity end for activity " + aci.getActivity());
//	}



	private final String getSbbId() {
		SbbID sbbId = this.sbbContext.getSbb();
		return sbbId.toString();
	}

	/**
	 * SBB Local Object Methods
	 * 
	 * @throws MAPException
	 */



 
/**
	 * CMPs
	 */
	public abstract void setMsisdn(String msisdn);

	public abstract String getMsisdn();




	public abstract ChildRelation getHttpServletAtiSbb();

  /* SBB Local Object Methods */
  
  	@Override
	public void setupAnyTimeInterrogationRequest(String msisdn,	EventContext nullActivityEventContext) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received setupAnyTimeInterrogationRequestIndication msisdn= " + msisdn
					+ " nullActivityEventContext" + nullActivityEventContext);
		}



		this.setNullActivityEventContext(nullActivityEventContext);
		this.setMsisdn(msisdn);
		
		this.sendATI(msisdn, this.getATIMAPApplicationContext());

	}


	private MAPApplicationContext getATIMAPApplicationContext() {
		if (this.atiMAPApplicationContext == null) {
			this.atiMAPApplicationContext = MAPApplicationContext.getInstance(
					MAPApplicationContextName.anyTimeEnquiryContext, MAPApplicationContextVersion.version3);
		}
		return this.atiMAPApplicationContext;
	}

	private void sendATI(String destinationAddress, MAPApplicationContext mapApplicationContext) {
		// Send out ATI

   //  logger.info("sendATI():");
	//	this.logger.info("destinationAddress="+destinationAddress+"mapApplicationContext="+mapApplicationContext);
		//MAPDialogSms mapDialogSms = null;
		MAPDialogMobility mapDialog = null;
		
			try {
			// 1. Create Dialog first and add the SRI request to it
			mapDialog = this.setupAnyTimeInterrogationRequestIndication(destinationAddress, mapApplicationContext);

			// 2. Create the ACI and attach this SBB
			ActivityContextInterface atiDialogACI = this.mapAcif.getActivityContextInterface(mapDialog);
			atiDialogACI.attach(this.sbbContext.getSbbLocalObject());

			// 3. Finally send the request
			mapDialog.send();
		} catch (MAPException e) {
			logger.severe("Error while trying to send SendAnyTimeInterrogationRequest", e);
			// something horrible, release MAPDialog and free resources

			if (mapDialog != null) {
				mapDialog.release();
			}


			// TODO : Take care of error condition
		}
		
   }	
	
	
	
		
		private SccpAddress convertAddressFieldToSCCPAddress(String address) {
//		GlobalTitle gt = new GlobalTitle0100Impl(address, 0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL );
		GlobalTitle gt = sccpParameterFact.createGlobalTitle(address, 0, NumberingPlan.ISDN_TELEPHONY, null,
				NatureOfAddress.INTERNATIONAL);

		//return new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt, 6);
		return sccpParameterFact.createSccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, gt, 0, 6);
	}

	
		private MAPDialogMobility setupAnyTimeInterrogationRequestIndication(String destinationAddress,
			MAPApplicationContext mapApplicationContext) throws MAPException {
		// this.mapParameterFactory.creat

	//	logger.info("setupAnyTimeInterrogationRequestIndication():");
		SccpAddress destinationReference = this.convertAddressFieldToSCCPAddress(destinationAddress);

//		this.logger.info("destinationReference="+destinationReference);
		MAPDialogMobility mapDialog = this.mapProvider.getMAPServiceMobility().createNewDialog(mapApplicationContext,
				this.getServiceCenterSccpAddress(), null, destinationReference, null);

//		mapDialogSms.addSendRoutingInfoForSMRequest(this.getCalledPartyISDNAddressString(destinationAddress), true,
	//			this.getServiceCenterAddressString(), null, false, null, null, null);
			SubscriberIdentity si;
        	ISDNAddressString msisdn;
        	ISDNAddressString gsmscfaddress;
    //    	createRequestedInfo(boolean locationInformation, boolean subscriberState, MAPExtensionContainer extensionContainer, boolean currentLocation, DomainType requestedDomain, boolean imei,
      //      boolean msClassmark, boolean mnpRequestedInfo)
        	RequestedInfo inf = this.mapProvider.getMAPParameterFactory().createRequestedInfo(true, true, null, false, null, false, false, false);
        	
        	msisdn = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
        			AddressNature.international_number, 
        			org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, destinationAddress);
        	si = this.mapProvider.getMAPParameterFactory().createSubscriberIdentity(msisdn);
        	
       	gsmscfaddress = this.mapProvider.getMAPParameterFactory().createISDNAddressString(
        			AddressNature.international_number, 
        			org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, "923009224350");

	 		mapDialog.addAnyTimeInterrogationRequest(si, inf, gsmscfaddress, null);
		return mapDialog;
	}
	
		public void onAnyTimeInterrogationResponse(	AnyTimeInterrogationResponse evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(String.format("Received AnyTimeInterrogationResponse "+evt));
		}
		
		SubscriberInfo si=null;
		LocationInformation li=null;
		SubscriberState ss=null;
		CellGlobalIdOrServiceAreaIdOrLAI cgi_sai_lai=null;
		CellGlobalIdOrServiceAreaIdFixedLength cgi_sai_fixedlength=null;
		String cgi="not-available";
	//	try {
		 si = evt.getSubscriberInfo();
		 {
		  if (si != null)
		 		li = si.getLocationInformation();
		 		ss = si.getSubscriberState();
		 	} 
		 if (li != null)
		 		cgi_sai_lai = li.getCellGlobalIdOrServiceAreaIdOrLAI();
  //	 } catch (MAPException e) {
   //  	logger.severe("Error while getting SubscriberInfo ", e);
   //  }
     
     if (cgi_sai_lai != null)
     		cgi_sai_fixedlength = cgi_sai_lai.getCellGlobalIdOrServiceAreaIdFixedLength();
     		
     if (cgi_sai_fixedlength != null)
     {
     	try {
     	 cgi = String.format("%3d%02d%05d%4d", cgi_sai_fixedlength.getMCC(),cgi_sai_fixedlength.getMNC(),cgi_sai_fixedlength.getLac(),cgi_sai_fixedlength.getCellIdOrServiceAreaCode());
 		//	  logger.info("cgid="+cgi);
     	} catch (MAPException e) {
     		logger.severe("Unable to get cgid ", e);
     	}
     }
//     logger.info("locationInformation="+li);

 // in case of any error, use this code
// 	AtiActivityContextInterface atiSbbActivityContextInterface = this.asSbbActivityContextInterface(this
//					.getNullActivityEventContext().getActivityContextInterface());
//			this.resumeNullActivityEventDelivery(atiSbbActivityContextInterface, this.getNullActivityEventContext());
 
		// lets detach so we don't get onDialogRelease() which will start
		// delivering SMS waiting in queue for same MSISDN
	aci.detach(this.sbbContext.getSbbLocalObject());
    ParentSbbLocalObject parent = null;
	
	  try {
   ChildRelation relation = this.getHttpServletAtiSbb();
 //  System.out.println("relation="+ relation.create());
   parent = (ParentSbbLocalObject) relation.create();
   parent.setupAnyTimeInterrogationResponse(this.getMsisdn(), cgi, this.getNullActivityEventContext());
   forwardEvent(parent, aci);
  } catch (Exception e) {
 	  logger.severe("Unable to create ParentSbb", e);
  }

	}
	
	private void forwardEvent(SbbLocalObject child, ActivityContextInterface aci) {
		try {
			aci.attach(child);
		//	aci.detach(sbbContext.getSbbLocalObject());
		} catch (Exception e) {
			logger.severe("Unexpected error in forwardEvent: ", e);
		}
	}

   
   

}
