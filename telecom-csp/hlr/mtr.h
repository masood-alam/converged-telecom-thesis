/*
		Copyright (C) Dialogic Corporation 1999-2006. All Rights Reserved.

 Name:          mtr.h

 Description:   Include file for MTR utility

 -----  ---------  -----  ---------------------------------------------
 Issue    Date      By                     Changes
 -----  ---------  -----  ---------------------------------------------
   1    16-Feb-00   HJM   - Initial code.
   2    10-Aug-01   JER   - Added handling for SEND-ROUTING-INFO-FOR-GPRS
   			    and SEND-IMSI.
   3    13-Dec-06   ML    - Change to use of Dialogic Corporation copyright.                            
*/


/*
 * General definitions
 */
#define MAX_NUM_DLGS                (1024)

/*
 * MTR state definitions
 */
#define MTR_S_NULL                  (0)   /* Idle */
#define MTR_S_WAIT_FOR_SRV_PRIM     (1)   /* Wait for fwd SMS indication */
#define MTR_S_WAIT_DELIMITER        (2)   /* Wait for MAP Delimiter Ind */

#define MAX_PARAM_LEN               (320)
#define MTR_MAX_AC_LEN              (255) /* max length of app context */
#define MAX_SM_SIZE                 (200) /* max length of short message */
#define SIZE_UI_HEADER              (18)  /* Short Message User Info header */


/*
 * Local macros.
 */
#define NO_RESPONSE                 (0)
#define BIN2CH(b)                   ((char)(((b)<10)?'0'+(b):'a'-10+(b)))


/*
 * Alphabet conversion.
 * def_alph   ascii
 * 0x27 '->' (=39)
 *
 *
 * Any ascii characters without default alphabet symbols are replaced with
 * a '@'. N.B. Default alphabet 0x00 is a '@'.
 */
static u8 default_alphabet_to_ascii[0x80]= {
/*0   1   2   3   4   5   6   7   8   9   a   b   c   d   e   f */
 '@','£','$','@','@','@','@','@','@','@', 10,'@', 13,'@','@','@', /* 0x00 */
 '@','_','@','@','@','@','@','@','@','@','@', 32,'@','@','@','@', /* 0x10 */
 ' ','!','"','#','@','%','&', 39,'(',')','*','+',',','-','.','/', /* 0x20 */
 '0','1','2','3','4','5','6','7','8','9',':',';','<','=','>','?', /* 0x30 */
 '@','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O', /* 0x40 */
 'P','Q','R','S','T','U','V','W','X','Y','Z','@','@','@','@','@', /* 0x50 */
 '@','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o', /* 0x60 */
 'p','q','r','s','t','u','v','w','x','y','z','@','@','@','@','@'  /* 0x70 */
};

#define DEF2ASCII(def_alp) (default_alphabet_to_ascii[def_alp])


/*
 * Structure Definitions. Note that this structure is simplified because MTR
 * only allows one service primitive per dialogue so storage is only needed
 * for one service primitive type and one invoke ID.
 */

typedef struct dlg_info {
    u8  state;				/* state */
    u8  ptype;				/* service primitive type */
    u16 invoke_id;			/* invoke ID */
    u16 map_inst;			/* instance of MAP module */
    u8  ac_len;				/* Length of application context */
    u8  app_context[MTR_MAX_AC_LEN];	/* Saved application context */
}dlg_info;

/* EOF */
