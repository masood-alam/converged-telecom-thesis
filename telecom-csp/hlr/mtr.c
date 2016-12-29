/*
		Copyright (C) Dialogic Corporation 1999-2006. All Rights Reserved.

 Name:          mtr.c

 Description:   Simple responder for MTU (MAP test utility)
                This program responds to an incoming dialogue received
                from the Intel MAP module.

                The program receives:
                	MAP-OPEN-IND
                        service indication
                        MAP-DELIMITER-IND

                and it responds with:
                	MAP-OPEN-RSP
                        service response
                        MAP-CLOSE-IND

                The following services are handled:
                	MAP-FORWARD-SHORT-MESSAGE
                	MAP-SEND-IMSI
                	MAP-SEND-ROUTING-INFO-FOR-GPRS

 Functions:     main

 -----  ---------  -----  ---------------------------------------------
 Issue    Date      By                     Changes
 -----  ---------  -----  ---------------------------------------------
   A    11-Mar-99   SFP   - Initial code.
   1    16-Feb-00   HJM   - Support for multiple dialogues
                          - Recovers and prints out short message
   2    03-May-00   JER   - Corrected problem with instance.
   3    10-Aug-01   JER   - Added handling for SEND-ROUTING-INFO-FOR-GPRS
                            and SEND-IMSI.
   4    20-Jan-06   TBl   - Include reference to Intel Corporation in file header
   5    13-Dec-06   ML    - Change to use of Dialogic Corporation copyright.
 */

#include <stdio.h>
#include <string.h>

#include "system.h"
#include "msg.h"
#include "sysgct.h"

#include "scp_inc.h"  // masood
#include "tcp_inc.h"  // masood
#include "ss7_inc.h"  // masood
#include "map_inc.h"
#include "mtr.h"
#include "pack.h"


/*
 * Prototypes for local functions:
 */
#ifdef LINT_ARGS
  static int init_resources(void);
  static u16 MTU_def_alph_to_str(u8 *da_octs, u16 da_olen, u16 da_num,
                                 char *ascii_str, u16 max_strlen);
  static int print_sh_msg(MSG *m);
  static dlg_info *get_dialogue_info(u16 dlg_id);
  static int MTR_trace_msg(char *prefix, MSG *m);
  static int MTR_send_msg(u16 instance, MSG *m);
  static int MTR_n_state_req( ssn, format_id, cong_level);
  static int MTR_process_map_msg(MSG *m);
  static int MTR_send_OpenResponse(u16 mtr_map_inst, u16 dlg_id, u8 result);
  static int MTR_ForwardSMResponse(u16 mtr_map_inst, u16 dlg_id, u8 invoke_id);
  static int MTR_SendImsiResponse(u16 mtr_map_inst, u16 dlg_id, u8 invoke_id);
  static int MTR_SendRtgInfoGprsResponse(u16 mtr_map_inst, u16 dlg_id, u8 invoke_id);
  static int MTR_SendRtgInfoSmResponse(u16 mtr_map_inst, u16 dlg_id, u8 invoke_id);
  static int MTR_SendATIResponse(u16 mtr_map_inst, u16 dlg_id, u8 invoke_id);
  static int MTR_send_MapClose(u16 mtr_map_inst, u16 dlg_id, u8 method);
  static int MTR_send_Abort(u16 mtr_map_inst, u16 dlg_id, u8 reason);
  static int MTR_get_invoke_id(u8 *pptr, u16 plen);
  static int MTR_get_applic_context(u8 *pptr, u16 plen, u8 *dst, u16 dstlen);
  static int MTR_get_sh_msg(u8 *pptr, u16 plen, u8 *dst, u16 dstlen);
#else
  static int init_resources();
  static u16 MTU_def_alph_to_str();
  static int print_sh_msg();
  static dlg_info *get_dialogue_info();
  static int MTR_trace_msg();
  static int MTR_send_msg();
  static int MTR_process_map_msg();
  static int MTR_send_OpenRsponse();
  static int MTR_ForwardSMResponse();
  static int MTR_SendImsiResponse();
  static int MTR_SendRtgInfoGprsResponse();
  static int MTR_SendRtgInfoSmResponse();
  static int MTR_SendATIResponse();
  static int MTR_send_MapClose();
  static int MTR_send_Abort();
  static int MTR_get_invoke_id();
  static int MTR_get_applic_context();
  static int MTR_get_sh_msg();
#endif


  /*
 * Some useful macros. used in for example send_uis message
 */

#define NO_RESPONSE             (0)
#define RESPONSE(mod_id)        (1 << ((mod_id) & 0x0f))


  // these macros are also duplicated from TTU module to provide uis
#define MTR_user_in_service(ssn)        MTR_n_state_req((ssn), 1, 0)
#define MTR_user_out_of_service(ssn)    MTR_n_state_req((ssn), 2, 0)
#define MTR_user_congestion(ssn, cong)  MTR_n_state_req((ssn), 7, (cong))


/*
 * Static data:
 */
static dlg_info dialogue_info[MAX_NUM_DLGS];	/* Dialog_info */
static u8 mtr_mod_id;                           /* Module id of this task */
static u8 mtr_map_id;                           /* Module id for all MAP requests */
static u8 mtr_trace;                            /* Controls trace requirements */


/*
 * MTU_def_alph_to_str()
 * Returns the number of ascii characters formatted into the
 * ascii string. Returns zero if this could not be done.
 */
u16 MTU_def_alph_to_str(da_octs, da_olen, da_num, ascii_str, max_strlen)
  u8   *da_octs;      /* u8 array from which the deft alph chars are recoverd */
  u16   da_olen;      /* The formatted octet length of da_octs  */
  u16   da_num;       /* The number of formatted characters in the array */
  char *ascii_str;    /* location into which chars are written */
  u16   max_strlen;   /* The max space available for the ascii_str */
{
  char *start_ascii_str;   /* The first char */
  u16  i;                  /* The bit position along the da_octs */

  start_ascii_str = ascii_str;

  if ((da_olen * 8) > ((max_strlen + 1) * 7))
    return(0);

  if (  ( (da_num * 7)  >   (da_olen      * 8) )
     || ( (da_num * 7) <= ( (da_olen - 1) * 8) ) )
  {
    /*
     * The number of digits does not agree with the size of the string
     */
    return (0);
  }
  else
  {
    for (i=0; i < da_num; i++)
    {
      *ascii_str++ = DEF2ASCII(unpackbits(da_octs, i * 7, 7));
    }

    *ascii_str++ = '\0';

    return((u16)(ascii_str - start_ascii_str));
  }
}

/*
 * print_sh_msg()
 *
 * prints a received short message
 *
 * Always returns zero
 *
 */
int print_sh_msg(m)
  MSG *m;
{
  u8 *pptr;                 	/* Parameter pointer */
  u16 plen;                 	/* length of primitive data */
  u16 msg_len;                  /* Number of characters in short message */
  u8  raw_SM[MAX_SM_SIZE];   	/* Buffer for holding raw SM */
  char ascii_SM[MAX_SM_SIZE];	/* Buffer for holding ascii SM */

  pptr = get_param(m);
  plen = MTR_get_sh_msg(pptr, m->len, raw_SM, MAX_SM_SIZE);
  plen -= SIZE_UI_HEADER;
  msg_len = raw_SM[SIZE_UI_HEADER - 1];
  MTU_def_alph_to_str(raw_SM + SIZE_UI_HEADER, plen, msg_len,
                      ascii_SM, MAX_SM_SIZE);
  printf("HLR Rx: Short Message User Information:\n");
  printf("HLR Rx: %s\n",ascii_SM);
  return(0);
}

int  MTR_config_map( u8 mtr_mod_id, u8 mtr_map_id);

/*
 * mtr_ent
 *
 * Waits in a continuous loop responding to any received
 * forward SM request with a forward SM response.
 *
 * Never returns.
 */
int mtr_ent(mtr_id, ssn, map_id, trace)
  u8 mtr_id;   /* Module id for this task */
  u8 ssn;      /* Subsystem of MTR */
  u8 map_id;   /* Module ID for MAP */
  u8 trace;    /* Trace requirements */
{
  HDR *h;		/* received message */
  MSG *m;		/* received message */

  mtr_mod_id = mtr_id;
  mtr_map_id = map_id;
  mtr_trace  = trace;

  /*
   * Print banner so we know what's running.
   */
  printf("HLR MAP Test Application VectraCom 2011.\n");
  printf("===============================================================================\n\n");
  printf("HLR mod ID - 0x%02x; MAP module Id 0x%x\n", mtr_mod_id, mtr_map_id);
  if ( mtr_trace == 0 )
    printf(" Tracing disabled.\n\n");

  init_resources();


 /*
   * Tell SCCP that this sub-system
   * is now in service:
   */
  MTR_user_in_service(ssn);
  /*
   * Now enter main loop, receiving messages as they
   * become available and processing accordingly.
   */

 MTR_config_map( mtr_mod_id, mtr_map_id);


  while (1)
  {
    /*
     * GCT_receive will attempt to receive messages
     * from the task's message queue and block until
     * a message is ready.
     */
    if ((h = GCT_receive(mtr_mod_id)) != 0)
    {
      m = (MSG *)h;
      MTR_trace_msg("HLR Rx:", m);
      switch (m->hdr.type)
      {
        case MAP_MSG_DLG_IND:
        case MAP_MSG_SRV_IND:
          MTR_process_map_msg(m);
        break;
      }

      /*
       * Once we have finished processing the message
       * it must be released to the pool of messages.
       */
      relm(h);
    }
  }
  return(0);
}

/*
 * Get Dialogue Info
 *
 * Returns pointer to dialogue info or 0 on error.
 */
dlg_info *get_dialogue_info(dlg_id)
  u16 dlg_id;               /* Dlg ID of the incoming message 0x800a perhaps */
{
  u16 dlg_ref;              /* Internal Dlg Ref, 0x000a perhaps */

  if (!(dlg_id & 0x8000) )
  {
    if (mtr_trace)
      printf("HLR Rx: Bad dialogue id: Outgoing dialogue id, dlg_id == %x\n",dlg_id);
    return 0;
  }
  else
  {
    dlg_ref = dlg_id & 0x7FFF;
  }

  if ( dlg_ref >= MAX_NUM_DLGS )
  {
    if (mtr_trace)
      printf("HLR Rx: Bad dialogue id: Out of range dialogue, dlg_id == %x\n",dlg_id);
    return 0;
  }
  return &dialogue_info[dlg_ref];
}


/*
 * MTR_process_map_msg
 *
 * Processes a received MAP primitive message.
 *
 * Always returns zero.
 */
static int MTR_process_map_msg(m)
  MSG *m;                       /* Received message */
{
  u16  dlg_id;                  /* Dialogue id */
  u8   ptype;                   /* Parameter Type */
  u8   *pptr;                   /* Parameter Pointer */
  dlg_info *dlg_info;    	/* State info for dialogue */
  u8   send_abort;              /* Set if abort to be generated */
  int  invoke_id;               /* Invoke id of received srv req */

  pptr = get_param(m);
  ptype = *pptr;

  dlg_id = m->hdr.id;
  send_abort = 0;

  /*
   * Get state information associated with this dialogue
   */
  dlg_info = get_dialogue_info(dlg_id);

  if (dlg_info == 0)
    return 0;

  switch (dlg_info->state)
  {
    case MTR_S_NULL :
      /*
       * Null state.
       */
      switch (m->hdr.type)
      {
        case MAP_MSG_DLG_IND :
          switch (ptype)
          {
            case MAPDT_OPEN_IND :
              /*
               * Open indication indicates that a request to open a new
               * dialogue has been received
               */
              if ( mtr_trace)
                printf("HLR Rx: Received Open Indication\n");

              /*
               * Save application context and MAP instance
               * We don't do actually do anything further with it though.
               */
              dlg_info->map_inst = (u16)GCT_get_instance((HDR*)m);
              dlg_info->ac_len =(u8)MTR_get_applic_context(pptr, m->len,
                                                           dlg_info->app_context,
                                                           MTR_MAX_AC_LEN);
              if (dlg_info->ac_len != 0)
              {
                /*
                 * Respond to the OPEN_IND with OPEN_RSP and wait for the
                 * service indication
                 */
                MTR_send_OpenResponse(dlg_info->map_inst, dlg_id, MAPRS_DLG_ACC);
                dlg_info->state = MTR_S_WAIT_FOR_SRV_PRIM;
              }
              else
              {
                /*
                 * We do not have a proper Application Context - abort
                 * the dialogue
                 */
                printf("not a proper ac- abort\n");
                send_abort = 1;
              }
              break;

            default :
              /*
               * Unexpected event - Abort the dialogue.
               */
                printf("unexpected event- abort\n");
              send_abort = 1;
              break;
          }
          break;

        default :
          /*
           * Unexpected event - Abort the dialogue.
           */
           printf(" unexected event 1 - abort\n");
          send_abort = 1;
          break;
      }
      break;

    case MTR_S_WAIT_FOR_SRV_PRIM :
      /*
       * Waiting for service primitive
       */
          //printf("HLR Rx: Waiting for SRV_PRIM :???");
      switch (m->hdr.type)
      {
        case MAP_MSG_SRV_IND :
          /*
           * Service primitive indication
           */
          switch (ptype)
          {
            /* Masood blocked for HLR case MAPST_FWD_SM_IND :*/
            case MAPST_SEND_IMSI_IND :
            case MAPST_SND_RTIGPRS_IND :
			case MAPST_SND_RTISM_IND :		/* Added by Masood */
			case MAPST_ANYTIME_INT_IND :   /* Added by Masood */
              if (mtr_trace)
              {
                switch (ptype)
                {
                  //case MAPST_FWD_SM_IND :
                  //  printf("HLR Rx: Received Forward Short Message Indication\n");
                  //  break;
                  case MAPST_SEND_IMSI_IND :
                    printf("HLR Rx: Received Send IMSI Indication\n");
                    break;
                  case MAPST_SND_RTIGPRS_IND :
                    printf("HLR Rx: Received Send Routing Info for GPRS Indication\n");
                    break;
                  case MAPST_SND_RTISM_IND :
                    printf("HLR Rx: Received Send Routing Info for SM Indication\n");
                    break;
                  case MAPST_ANYTIME_INT_IND :
                  	printf("HLR Rx: Received AnyTimeInterrogation indication\n");
                  	break;
                }
              }

              /*
               * Recover invoke id. The invoke id is used
               * when sending the Forward short message response.
               */
              invoke_id = MTR_get_invoke_id(get_param(m), m->len);

              /*
               * If recovery of the invoke id succeeded, save invoke id and
               * primitive type and change state to wait for the delimiter.
               */
              if (invoke_id != -1)
              {
                dlg_info->invoke_id = (u8)invoke_id;
                dlg_info->ptype = ptype;

                if ((mtr_trace) && (ptype == MAPST_FWD_SM_IND))
                  print_sh_msg(m);

                dlg_info->state = MTR_S_WAIT_DELIMITER;
                break;
              }
              else
              {
                printf("HLR RX: No invoke ID included in the message\n");
              }
              break;

            default :

                printf("HLR RX: Unknown primitive %x in the message\n", ptype);
              send_abort = 1;
              break;
          }
          break;

        case MAP_MSG_DLG_IND :
          /*
           * Dialogue indication - we were not expecting this!
           */
          switch (ptype)
          {
            case MAPDT_NOTICE_IND :
              /*
               * MAP-NOTICE-IND indicates some kind of error. Close the
               * dialogue and idle the state machine.
               */
              if (mtr_trace)
                printf("HLR Rx: Received Notice Indication\n");
              /*
               * Now send Map Close and go to idle state.
               */
              MTR_send_MapClose(dlg_info->map_inst, dlg_id, MAPRM_normal_release);
              dlg_info->state = MTR_S_NULL;
              send_abort = 0;
              break;

            default :
              /*
               * Unexpected event - Abort the dialogue.
               */
               printf(" again unexpected event - abort (indication ptype=%x\n", ptype);
              send_abort = 1;
              break;
          }
          break;

        default :
          /*
           * Unexpected event - Abort the dialogue.
           */
           printf ("again again unexpected event - abort\n");
          send_abort = 1;
          break;
      }
      break;

    case MTR_S_WAIT_DELIMITER :
      /*
       * Wait for delimiter.
       */
      switch (m->hdr.type)
      {
        case MAP_MSG_DLG_IND :

          switch (ptype)
          {
            case MAPDT_DELIMITER_IND :
              /*
               * Delimiter indication received. Now send the appropriate
               * response depending on the service primitive that was received.
               */
              if (mtr_trace)
                printf("HLR Rx: Received delimiter Indication\n");

              switch (dlg_info->ptype)
              {
            //    case MAPST_FWD_SM_IND :
              //    MTR_ForwardSMResponse(dlg_info->map_inst, dlg_id,
                //                        dlg_info->invoke_id);
                //  break;
                case MAPST_SEND_IMSI_IND :
                  			MTR_SendImsiResponse(dlg_info->map_inst, dlg_id,
                                       dlg_info->invoke_id);
                  			break;
                case MAPST_SND_RTIGPRS_IND :
                  				MTR_SendRtgInfoGprsResponse(dlg_info->map_inst, dlg_id,
                                              dlg_info->invoke_id);

                  				break;
                case MAPST_SND_RTISM_IND :
                  				MTR_SendRtgInfoSmResponse(dlg_info->map_inst, dlg_id,
                                              dlg_info->invoke_id);
                  				break;
								case MAPST_ANYTIME_INT_IND:
													// we should check requested info parameter
													MTR_SendATIResponse(dlg_info->map_inst, dlg_id, dlg_info->invoke_id);

                  				break;
              }

              MTR_send_MapClose(dlg_info->map_inst, dlg_id, MAPRM_normal_release);
              dlg_info->state = MTR_S_NULL;
              send_abort = 0;
              break;

            default :
              /*
               * Unexpected event - Abort the dialogue
               */
			 printf("HLR Rx: Unexpected event!!!");

              send_abort = 1;
              break;
          }
          break;

        default :
          /*
           * Unexpected event - Abort the dialogue
           */
			 printf("HLR Rx: Unexpected event!!! Again");
          send_abort = 1;
          break;
      }
      break;
  }
  /*
   * If an error or unexpected event has been encountered, send abort and
   * return to the idle state.
   */
  if (send_abort)
  {
    MTR_send_Abort(dlg_info->map_inst, dlg_id, MAPUR_procedure_error);
    dlg_info->state = MTR_S_NULL;
  }
  return(0);
}


/******************************************************************************
 *
 * Functions to send primitive requests to the MAP module
 *
 ******************************************************************************/

/*
 * MTR_send_OpenResponse
 *
 * Sends an open response to MAP
 *
 * Returns zero or -1 on error.
 */
static int MTR_send_OpenResponse(instance, dlg_id, result)
  u16 instance;        /* Destination instance */
  u16 dlg_id;          /* Dialogue id */
  u8  result;          /* Result (accepted/rejected) */
{
  MSG  *m;                      /* Pointer to message to transmit */
  u8   *pptr;                   /* Pointer to a parameter */
  dlg_info *dlg_info;    	/* Pointer to dialogue state information */

  /*
   * Get the dialogue information associated with the dlg_id
   */
  dlg_info = get_dialogue_info(dlg_id);
  if (dlg_info == 0)
    return (-1);

  if (mtr_trace)
    printf("HLR Tx: Sending Open Response\n");

  /*
   * Allocate a message (MSG) to send:
   */
  if ((m = getm((u16)MAP_MSG_DLG_REQ, dlg_id,
                     NO_RESPONSE, (u16)(7 + dlg_info->ac_len))) != 0)
  {
    m->hdr.src = mtr_mod_id;
    m->hdr.dst = mtr_map_id;
    /*
     * Format the parameter area of the message
     *
     * Primitive type   = Open response
     * Parameter name   = result_tag
     * Parameter length = 1
     * Parameter value  = result
     * Parameter name   = applic_context_tag
     * parameter length = len
     * parameter data   = applic_context
     * EOC_tag
     */
    pptr = get_param(m);
    pptr[0] = MAPDT_OPEN_RSP;
    pptr[1] = MAPPN_result;
    pptr[2] = 0x01;
    pptr[3] = result;
    pptr[4] = MAPPN_applic_context;
    pptr[5] = (u8)dlg_info->ac_len;
    memcpy((void*)(pptr+6), (void*)dlg_info->app_context, dlg_info->ac_len);
    pptr[6+dlg_info->ac_len] = 0x00;

    /*
     * Now send the message
     */
    MTR_send_msg(instance, m);
  }
  return(0);
}

/*
 * MTR_ForwardSMResponse
 *
 * Sends a forward short message response to MAP.
 *
 * Always returns zero.
 */
static int MTR_ForwardSMResponse(instance, dlg_id, invoke_id)
  u16 instance;        /* Destination instance */
  u16 dlg_id;          /* Dialogue id */
  u8  invoke_id;       /* Invoke_id */
{
  MSG  *m;                      /* Pointer to message to transmit */
  u8   *pptr;                   /* Pointer to a parameter */
  dlg_info *dlg_info;     	/* Pointer to dialogue state information */

  /*
   *  Get the dialogue information associated with the dlg_id
   */
  dlg_info = get_dialogue_info(dlg_id);
  if (dlg_info == 0)
    return (-1);

  if (mtr_trace)
    printf("HLR Tx: Sending Forward SM Response\n\r");

  /*
   * Allocate a message (MSG) to send:
   */
  if ((m = getm((u16)MAP_MSG_SRV_REQ, dlg_id, NO_RESPONSE, 5)) != 0)
  {
    m->hdr.src = mtr_mod_id;
    m->hdr.dst = mtr_map_id;

    /*
     * Format the parameter area of the message
     *
     * Primitive type   = Forward SM response
     * Parameter name   = invoke ID
     * Parameter length = 1
     * Parameter value  = invoke ID
     * Parameter name   = terminator
     */
    pptr = get_param(m);
    pptr[0] = MAPST_FWD_SM_RSP;
    pptr[1] = MAPPN_invoke_id;
    pptr[2] = 0x01;
    pptr[3] = invoke_id;
    pptr[4] = 0x00;

    /*
     * Now send the message
     */
    MTR_send_msg(instance, m);
  }
  return(0);
}

/*
 * MTR_SendRtgInfoGprsResponse
 *
 * Sends a send routing info for GPRS response to MAP.
 *
 * Always returns zero.
 */
static int MTR_SendRtgInfoGprsResponse(instance, dlg_id, invoke_id)
  u16 instance;        /* Destination instance */
  u16 dlg_id;          /* Dialogue id */
  u8  invoke_id;       /* Invoke_id */
{
  MSG  *m;                      /* Pointer to message to transmit */
  u8   *pptr;                   /* Pointer to a parameter */
  dlg_info *dlg_info;     	/* Pointer to dialogue state information */

  /*
   *  Get the dialogue information associated with the dlg_id
   */
  dlg_info = get_dialogue_info(dlg_id);
  if (dlg_info == 0)
    return (-1);

  if (mtr_trace)
    printf("HLR Tx: Sending Send Routing Info for GPRS Response\n\r");

  /*
   * Allocate a message (MSG) to send:
   */
  if ((m = getm((u16)MAP_MSG_SRV_REQ, dlg_id, NO_RESPONSE, 12)) != 0)
  {
    m->hdr.src = mtr_mod_id;
    m->hdr.dst = mtr_map_id;

    /*
     * Format the parameter area of the message
     *
     * Primitive type   = Send Routing Info for GPRS response
     *
     * Parameter name   = invoke ID
     * Parameter length = 1
     * Parameter value  = invoke ID
     *
     * Parameter name = SGSN address
     * Parameter length = 4
     * Parameter value:
     *   1st octet:  address type = IPv4; addres length = 4
     *   remaining octets = 193.195.185.113
     *
     * Parameter name   = terminator
     */
    pptr = get_param(m);
    pptr[0] = MAPST_SND_RTIGPRS_RSP;
    pptr[1] = MAPPN_invoke_id;
    pptr[2] = 0x01;
    pptr[3] = invoke_id;
    pptr[4] = MAPPN_sgsn_address;
    pptr[5] = 5;
    pptr[6] = 4;
    pptr[7] = 193;
    pptr[8] = 195;
    pptr[9] = 185;
    pptr[10] = 113;
    pptr[11] = 0x00;

    /*
     * Now send the message
     */
    MTR_send_msg(instance, m);
  }
  return(0);
}



/*
 * Added by Masood
 * MTR_SendRtgInfoSmResponse
 *
 * Sends a send routing info for SM response to MAP.
 *
 * Always returns zero.
 */
static int MTR_SendRtgInfoSmResponse(instance, dlg_id, invoke_id)
  u16 instance;        /* Destination instance */
  u16 dlg_id;          /* Dialogue id */
  u8  invoke_id;       /* Invoke_id */
{
  MSG  *m;                      /* Pointer to message to transmit */
  u8   *pptr;                   /* Pointer to a parameter */
  dlg_info *dlg_info;     	/* Pointer to dialogue state information */

  /*
   *  Get the dialogue information associated with the dlg_id
   */
  dlg_info = get_dialogue_info(dlg_id);
  if (dlg_info == 0)
    return (-1);

  if (mtr_trace)
    printf("HLR Tx: Sending Send Routing Info for SM Response\n\r");

  /*
   * Allocate a message (MSG) to send:
   */
  if ((m = getm((u16)MAP_MSG_SRV_REQ, dlg_id, NO_RESPONSE, 23)) != 0)
  {
    m->hdr.src = mtr_mod_id;
    m->hdr.dst = mtr_map_id;

    /*
     * Format the parameter area of the message
     *
     * Primitive type   = Send Routing Info for GPRS response
     *
     * Parameter name   = invoke ID
     * Parameter length = 1
     * Parameter value  = invoke ID
     *
     * Parameter name = SGSN address
     * Parameter length = 4
     * Parameter value:
     *   1st octet:  address type = IPv4; addres length = 4
     *   remaining octets = 193.195.185.113
     *
     * Parameter name   = terminator
     */
    pptr = get_param(m);
    pptr[0] = MAPST_SND_RTISM_RSP;
    pptr[1] = MAPPN_invoke_id;
    pptr[2] = 0x01;
    pptr[3] = invoke_id;
    

    pptr[4] = MAPPN_imsi;
    pptr[5] = 7;
    pptr[6] = 0x06;
    pptr[7] = 0x08;
    pptr[8] = 0x62;
    pptr[9] = 0x87;
    pptr[10] = 0x00;
    pptr[11] = 0x40;
    pptr[12] = 0x45;
	

	pptr[13] = MAPPN_msc_num;
    pptr[14] = 7;
    pptr[15] = 0x91;
    pptr[16] = 0x29;
    pptr[17] = 0x79;
    pptr[18] = 0x52;
    pptr[19] = 0x57;
    pptr[20] = 0x21;
    pptr[21] = 0xf5;
    pptr[22] = 0x00;

    /*
     * Now send the message
     */
    MTR_send_msg(instance, m);
  }
  return(0);
}


/* Added by Masood */
static int MTR_SendATIResponse(u16 instance, u16 dlg_id, u8 invoke_id)
{
  MSG  *m;                      /* Pointer to message to transmit */
  u8   *pptr;                   /* Pointer to a parameter */
  dlg_info *dlg_info;         /* Pointer to dialogue state information */

  /*
   *  Get the dialogue information associated with the dlg_id
   */
  dlg_info = get_dialogue_info(dlg_id);
  if (dlg_info == 0)
    return (-1);

  if (mtr_trace)
    printf("HLR Tx: Sending Anytime-Interrogation Response\n\r");

  /*
   * Allocate a message (MSG) to send:
   */
  if ((m = getm((u16)MAP_MSG_SRV_REQ, dlg_id, NO_RESPONSE, 17)) != 0)
  {
    m->hdr.src = mtr_mod_id;
    m->hdr.dst = mtr_map_id;

    pptr = get_param(m);
    pptr[0] = MAPST_ANYTIME_INT_RSP;
    pptr[1] = MAPPN_invoke_id;
    pptr[2] = 0x01;
    pptr[3] = invoke_id;
    pptr[4] = MAPPN_cell_id;
    pptr[5] = 7;
    pptr[6 ] = 0x14;  // MCC 1
    pptr[7] = 0xf0;   // MCc 2
    pptr[8 ] = 0x30;  // MNC 
    pptr[9] = 0xe9;    // LAC 1
    pptr[10] = 0xfe;   // LAC 2
    pptr[11] = 0x65;   // cellid 1
    pptr[12] = 0xc8;   // cellid 2
    pptr[13] = MAPPN_sub_state;
    pptr[14] = 0x01;
    pptr[15] = 0x01;  // CAMEL busy
    pptr[16] = 0x00;

/*
    pptr[14] = 0xF0;  //extension
    pptr[15] = 4;     //length of all that follows
    pptr[16] = 1;
    pptr[17] = 1;     //0x0101 = MAPPN_call_forwarding_data
    pptr[18] = 1;  //length
    pptr[19] = 0x22;  //value

    pptr[20] = 0;  //terminator
    */

    /*
     * Now send the message
     */
    MTR_send_msg(instance, m);
  }
  return(0);
}



/*
 * MTR_SendImsiResponse
 *
 * Sends a forward short message response to MAP.
 *
 * Always returns zero.
 */
static int MTR_SendImsiResponse(instance, dlg_id, invoke_id)
  u16 instance;        /* Destination instance */
  u16 dlg_id;          /* Dialogue id */
  u8  invoke_id;       /* Invoke_id */
{
  MSG  *m;                      /* Pointer to message to transmit */
  u8   *pptr;                   /* Pointer to a parameter */
  dlg_info *dlg_info;     	/* Pointer to dialogue state information */

  /*
   *  Get the dialogue information associated with the dlg_id
   */
  dlg_info = get_dialogue_info(dlg_id);
  if (dlg_info == 0)
    return (-1);

  if (mtr_trace)
    printf("HLR Tx: Sending Send IMSI Response\n\r");

  /*
   * Allocate a message (MSG) to send:
   */
  if ((m = getm((u16)MAP_MSG_SRV_REQ, dlg_id, NO_RESPONSE, 14)) != 0)
  {
    m->hdr.src = mtr_mod_id;
    m->hdr.dst = mtr_map_id;

    /*
     * Format the parameter area of the message
     *
     * Primitive type   = send IMSI response
     *
     * Parameter name   = invoke ID
     * Parameter length = 1
     * Parameter value  = invoke ID
     *
     * Primitive name = IMSI
     * Parameter length = 7
     * Parameter value = 60802678000454

     * Parameter name = terminator
     */
    pptr = get_param(m);
    pptr[0] = MAPST_SEND_IMSI_RSP;
    pptr[1] = MAPPN_invoke_id;
    pptr[2] = 0x01;
    pptr[3] = invoke_id;
    pptr[4] = MAPPN_imsi;
    pptr[5] = 7;
    pptr[6] = 0x06;
    pptr[7] = 0x08;
    pptr[8] = 0x62;
    pptr[9] = 0x87;
    pptr[10] = 0x00;
    pptr[11] = 0x40;
    pptr[12] = 0x45;
    pptr[13] = 0x00;

    /*
     * Now send the message
     */
    MTR_send_msg(instance, m);
  }
  return(0);
}

/*
 * MTR_send_MapClose
 *
 * Sends a Close message to MAP.
 *
 * Always returns zero.
 */
static int MTR_send_MapClose(instance, dlg_id, method)
  u16 instance;        /* Destination instance */
  u16 dlg_id;          /* Dialogue id */
  u8  method;          /* Release method */
{
  MSG  *m;                   /* Pointer to message to transmit */
  u8   *pptr;                /* Pointer to a parameter */
  dlg_info *dlg_info;        /* Pointer to dialogue state information */

  /*
   * Get the dialogue information associated with the dlg_id
   */
  dlg_info = get_dialogue_info(dlg_id);
  if (dlg_info == 0)
    return (-1);

  if (mtr_trace)
    printf("HLR Tx: Sending Close Request\n\r");

  /*
   * Allocate a message (MSG) to send:
   */
  if ((m = getm((u16)MAP_MSG_DLG_REQ, dlg_id, NO_RESPONSE, 5)) != 0)
  {
    m->hdr.src = mtr_mod_id;
    m->hdr.dst = mtr_map_id;

    /*
     * Format the parameter area of the message
     *
     * Primitive type   = Close Request
     * Parameter name   = release method tag
     * Parameter length = 1
     * Parameter value  = release method
     * Parameter name   = terminator
     */
    pptr = get_param(m);
    pptr[0] = MAPDT_CLOSE_REQ;
    pptr[1] = MAPPN_release_method;
    pptr[2] = 0x01;
    pptr[3] = method;
    pptr[4] = 0x00;

    /*
     * Now send the message
     */
    MTR_send_msg(dlg_info->map_inst, m);
  }
  return(0);
}

/*
 * MTR_send_Abort
 *
 * Sends an abort message to MAP.
 *
 * Always returns zero.
 */
static int MTR_send_Abort(instance, dlg_id, reason)
  u16 instance;		/* Destination instance */
  u16 dlg_id;		/* Dialogue id */
  u8  reason;		/* user reason for abort */
{
  MSG  *m;		/* Pointer to message to transmit */
  u8   *pptr;		/* Pointer to a parameter */
  dlg_info *dlg_info;	/* Pointer to dialogue state information */

  /*
   * Get the dialogue information associated with the dlg_id
   */
  dlg_info = get_dialogue_info(dlg_id);
  if (dlg_info == 0)
    return (-1);

  if (mtr_trace)
    printf("HLR Tx: Sending User Abort Request\n\r");

  /*
   * Allocate a message (MSG) to send:
   */
  if ((m = getm((u16)MAP_MSG_DLG_REQ, dlg_id, NO_RESPONSE, 5)) != 0)
  {
    m->hdr.src = mtr_mod_id;
    m->hdr.dst = mtr_map_id;

    /*
     * Format the parameter area of the message
     *
     * Primitive type   = Close Request
     * Parameter name   = user reason tag
     * Parameter length = 1
     * Parameter value  = reason
     * Parameter name   = terminator
     */
    pptr = get_param(m);
    pptr[0] = MAPDT_U_ABORT_REQ;
    pptr[1] = MAPPN_user_rsn;
    pptr[2] = 0x01;
    pptr[3] = reason;
    pptr[4] = 0x00;

    /*
     * Now send the message
     */
    MTR_send_msg(dlg_info->map_inst, m);
  }
  return(0);
}


/*
 * MTR_send_msg sends a MSG. On failure the
 * message is released and the user notified.
 *
 * Always returns zero.
 */
static int MTR_send_msg(instance, m)
  u16   instance;       /* Destination instance */
  MSG	*m;		/* MSG to send */
{
  GCT_set_instance((unsigned int)instance, (HDR*)m);
  MTR_trace_msg("HLR Tx:", m);

  /*
   * Now try to send the message, if we are successful then we do not need to
   * release the message.  If we are unsuccessful then we do need to release it.
   */

  if (GCT_send(m->hdr.dst, (HDR *)m) != 0)
  {
    if (mtr_trace)
      fprintf(stderr, "*** failed to send message ***\n");
    relm((HDR *)m);
  }
  return(0);
}


/* added by Masood
 * Duplicate of MTR_send_msg()
 * WITHOUT instance parameter (used for send_uis at init)  Masood
 * Always returns zero.
 */
static int MTR_send_msg1(m)
  MSG	*m;		/* MSG to send */
{
  MTR_trace_msg("HLR Tx:", m);

  /*
   * Now try to send the message, if we are successful then we do not need to
   * release the message.  If we are unsuccessful then we do need to release it.
   */

  if (GCT_send(m->hdr.dst, (HDR *)m) != 0)
  {
    if (mtr_trace)
      fprintf(stderr, "*** failed to send message ***\n");
    relm((HDR *)m);
  }
  return(0);
}




/******************************************************************************
 *
 * Functions to recover parameters from received MAP format primitives
 *
 ******************************************************************************/

/*
 * MTR_get_invoke_id
 *
 * recovers the invoke id parameter from a parameter array
 *
 * Returns the recovered value or -1 if not found.
 */
static int MTR_get_invoke_id(pptr, plen)
  u8  *pptr;        /* First byte of received primitive data (type octet) */
  u16 plen;         /* length of primitive data */
{
  int  invoke_id;   /* Recovered invoke_id */
  u8   ptype;       /* Parameter type*/

  /*
   * Skip past primitive type
   */
  pptr++;
  plen --;
  invoke_id = -1;

  while (plen)
  {
    ptype = *pptr++;
    plen = *pptr++;

    if (ptype == MAPPN_invoke_id)
    {
      /*
       * Verify that invoke ID length is 1 octet
       */
      if (plen == 1)
      {
        invoke_id = (int)*pptr;
        break;
      }
    }
    /*
     * Advance to next parameter
     */
    pptr += plen;
  }
  return(invoke_id);
}

/*
 * MTR_get_applic_context
 *
 * Recovers the Application Context parameter from a parameter array
 *
 * Returns the length of parameter data recovered (-1 on failure).
 */
static int MTR_get_applic_context(pptr, plen, dst, dstlen)
  u8  *pptr;	/* First byte of received primitive data (type octet) */
  u16 plen;     /* length of primitive data */
  u8  *dst;     /* Start of destination for recovered ac */
  u16 dstlen;   /* Space available at dst */
{
  u8   ptype;   /* Parameter type */
  int  retval;  /* Return value */

  retval = -1;
  /*
   * Skip past primitive type
   */
  pptr++;
  plen --;

  while (plen)
  {
    ptype = *pptr++;
    plen = *pptr++;

    if (ptype == MAPPN_applic_context)
    {
      /*
       * Verify that there is sufficient space to store the parameter data
       */
      if (plen <= dstlen)
      {
        memcpy((void*)dst, (void*)pptr, plen);
        retval = plen;
        break;
      }
    }
    /*
     * Advance to next parameter
     */
    pptr += plen;
  }
  return(retval);
}

/*
 * MTR_get_sh_msg
 *
 * recovers the short message parameter from a parameter array
 *
 * Returns the length of the recovered data or -1 if error.
 */
static int MTR_get_sh_msg(pptr, plen, dst, dstlen)
  u8  *pptr;	/* First byte of received primitive data (type octet) */
  u16 plen;     /* length of primitive data */
  u8  *dst;     /* Start of destination for recovered SM */
  u16 dstlen;   /* Space available at dst */
{
  u8   ptype;   /* Parameter type*/
  int  retval;  /* return value */

  /*
   * Skip past primitive type
   */
  pptr++;
  plen --;
  retval = -1;

  while (plen)
  {
    ptype = *pptr++;
    plen = *pptr++;

    if (ptype == MAPPN_sm_rp_ui)
    {
      /*
       * Verify that Short Message length is no greater than MAX_SM_SIZE
       */
      if (  (plen <= MAX_SM_SIZE)
         && (plen <= dstlen) )
      {
        memcpy((void*)dst, (void*)pptr, plen);
        retval = plen;
        break;
      }
    }
    /*
     * Advance to next parameter
     */
    pptr += plen;
  }
  return(retval);
}

/*
 * MTR_trace_msg
 *
 * Traces (prints) any message as hexadecimal to the console.
 *
 * Always returns zero.
 */
static int MTR_trace_msg(prefix, m)
  char *prefix;
  MSG  *m;               /* received message */
{
  HDR   *h;              /* pointer to message header */
  int   instance;        /* instance of MAP msg received from */
  u16   mlen;            /* length of received message */
  u8    *pptr;           /* pointer to parameter area */

  /*
   * If tracing is disabled then return
   */
  if (mtr_trace == 0)
    return(0);

  h = (HDR*)m;
  instance = GCT_get_instance(h);
  printf("%s I%04x M t%04x i%04x f%02x d%02x s%02x", prefix, instance, h->type,
          h->id, h->src, h->dst, h->status);

  if ((mlen = m->len) > 0)
  {
    if (mlen > MAX_PARAM_LEN)
      mlen = MAX_PARAM_LEN;
    printf(" p");
    pptr = get_param(m);
    while (mlen--)
    {
      printf("%c%c", BIN2CH(*pptr/16), BIN2CH(*pptr%16));
      pptr++;
    }
  }
  printf("\n");
  return(0);
}

/*
 * init_resources
 *
 * Initialises all mtr system resources
 * This includes dialogue state information.
 *
 * Always returns zero
 *
 */
static int init_resources()
{
  int i;    /* for loop index */

  for (i=0; i<MAX_NUM_DLGS; i++)
  {
    dialogue_info[i].state = MTR_S_NULL;
  }
  return (0);
}




/* Imported function for TTU application (by Masood)
 * Function to build and send a local
 * sub-system N-STATE request:
 *
 * Note that the mnemonics are not currently
 * defined!
 */
static int MTR_n_state_req( ssn, format_id, cong_level)
  u8    ssn;            /* local sub-system number */
  u8    format_id;      /* 1=UIS, 2=UOS, 7=Congestion */
  u8    cong_level;     /* congestion level 0, 1, 2 or 3 */
{
  MSG   *m;
  u8    *pptr;

  if ((m = getm(SCP_MSG_SCMG_REQ, ssn, RESPONSE(mtr_mod_id), 8)) != 0)
  {
    m->hdr.src = mtr_mod_id;
    m->hdr.dst = TCP_TASK_ID;
    pptr = get_param(m);
    memset((void *)pptr, 0, m->len);
    rpackbytes(pptr, 0, (u32)SCPMPT_NSTATE_REQ, 1);
    rpackbytes(pptr, 1, (u32)format_id, 1);
    if (format_id == 7)
      rpackbytes(pptr, 5, (u32)cong_level, 1);
    MTR_send_msg1(m);
  }
  return(0);
}


/*
M-I0000-t77e4-i0000-fef-d17-r8000-p
00-cnf-ver
4d-user
14-tcap
ef-mngt
ef-maint
ef-trace
0000-base_usr_ogdlg
8000-base_usr_icdlg
0000-base_tc_ogdlg
8000-base_tc_icdlg
2000-nog_dialogues
2000-nic_dialogues
ffff-num_invokes
00000000-options
0000
00000000000000
00000000000000
*/


int MTR_config_map( u8 usr_id, u8 map_id)
{
  MSG   *m;
  u8    *pptr;

  if ((m = getm(MAP_MSG_CONFIG, 0, RESPONSE(mtr_mod_id), 40)) != 0)
  {
    m->hdr.src = mtr_mod_id;
    m->hdr.dst = map_id;
    pptr = get_param(m);
    memset((void *)pptr, 0, m->len);
    rpackbytes(pptr, 0,  0, 1);
    rpackbytes(pptr, 1,  usr_id, 1);
    rpackbytes(pptr, 2,  TCP_TASK_ID, 1);
    rpackbytes(pptr, 3,  0xef, 1);
    rpackbytes(pptr, 4,  0xef, 1);
    rpackbytes(pptr, 5,  0xef, 1);
    rpackbytes(pptr, 6,  0, 2);
    rpackbytes(pptr, 8,  0x8000, 2);
    rpackbytes(pptr, 10,  0, 2);
    rpackbytes(pptr, 12,  0x8000, 2);
    rpackbytes(pptr, 14,  0x2000, 2);
    rpackbytes(pptr, 16,  0x2000, 2);
    rpackbytes(pptr, 18,  65535, 2);
    rpackbytes(pptr, 20,  0, 4);
    rpackbytes(pptr, 24,  0, 2);
    MTR_send_msg1(m);
  }
  return(0);
}
