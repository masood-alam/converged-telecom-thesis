/*
		Copyright (C) Dialogic Corporation 1999-2006. All Rights Reserved.

 Name:          mtr_main.c

 Description:   Console command line interface to mtr.

 Functions:     main()

 -----  ---------  -----  ---------------------------------------------
 Issue    Date      By                     Changes
 -----  ---------  -----  ---------------------------------------------
   A     10-Mar-99  SFP   - Initial code.
   1     16-Feb-00  HJM   - Added option to disable trace.
   2     31-Jul-01  JER   - Changed default module ID to be 0x2d.
   3     10-Aug-01  JER   - Added handling for SEND-ROUTING-INFO-FOR-GPRS
   			    and SEND-IMSI.
   4    20-Jan-06   TBl   - Include reference to Intel Corporation in file header
   5    13-Dec-06   ML    - Change to use of Dialogic Corporation copyright.
 */

#include <stdio.h>
#include <string.h>

#include "system.h"
#include "ss7_inc.h"
#include "map_inc.h"
#include "strtonum.h"

#ifdef LINT_ARGS
  static int read_cli_parameters(int argc, char *argv[], int *arg_index);
  static void show_syntax(void);
  static int read_option(char *arg);

  extern int mtr_ent(u8 mtr_mod_id,  u8 mtr_ssn, u8 mtr_map_id, u8 trace);
#else
  static int read_cli_parameters();
  static void show_syntax();
  static int read_option();

  extern int mtr_ent();
#endif

#define CLI_EXIT_REQ            (-1)    /* Option requires immediate exit */
#define CLI_UNRECON_OPTION      (-2)    /* Unrecognised option */
#define CLI_RANGE_ERR           (-3)    /* Option value is out of range */

/*
 * Default values for MTR's command line options:
 */
#define DEFAULT_MODULE_ID        (0x2d)
#define DEFAULT_SSN             (0x06)
#define DEFAULT_MAP_ID           (MAP_TASK_ID)

static u8  mtr_mod_id;
static u8  mtr_ssn;			/* The sub-system number to be used by MTR */
static u8  mtr_map_id;
static u8  mtr_trace;

static char *program;

/*
 * Main function for MAP Test Utility (MTR):
 */
int main(argc, argv)
  int argc;
  char *argv[];
{
  int failed_arg;
  int cli_error;

  mtr_mod_id = DEFAULT_MODULE_ID;
  mtr_ssn = DEFAULT_SSN;
  mtr_map_id = DEFAULT_MAP_ID;
  mtr_trace = 1;

  program = argv[0];

  if ((cli_error = read_cli_parameters(argc, argv, &failed_arg)) != 0)
  {
    switch (cli_error)
    {
      case CLI_UNRECON_OPTION :
        fprintf(stderr, "%s: Unrecognised option : %s\n", program, argv[failed_arg]);
        show_syntax();
        break;

      case CLI_RANGE_ERR :
        fprintf(stderr, "%s: Parameter range error : %s\n", program, argv[failed_arg]);
        show_syntax();
        break;

      default :
        break;
    }
  }
  else
    mtr_ent(mtr_mod_id, mtr_ssn, mtr_map_id, mtr_trace);

  return(0);
}


/*
 * show_syntax()
 */
static void show_syntax()
{
  fprintf(stderr,
	"Syntax: %s [-m -u]\n", program);
  fprintf(stderr,
    "  -m  : HLR's module ID (default=0x%02x)\n", DEFAULT_MODULE_ID);
  fprintf(stderr,
	"  -u  : MAP module ID (default=0x%02x)\n", DEFAULT_MAP_ID);
  fprintf(stderr,
    "  -t  : Trace disabled\n");
}

/*
 * Read in command line options and set the system variables accordingly.
 *
 * Returns 0 on success; on error returns non-zero and
 * writes the parameter index which caused the failure
 * to the variable arg_index.
 */
static int read_cli_parameters(argc, argv, arg_index)
  int argc;             /* Number of arguments */
  char *argv[];         /* Array of argument pointers */
  int *arg_index;       /* Used to return parameter index on error */
{
  int error;
  int i;

  for (i=1; i < argc; i++)
  {
    if ((error = read_option(argv[i])) != 0)
    {
      *arg_index = i;
      return(error);
    }
  }
  return(0);
}

/*
 * Read a command line parameter and check syntax.
 *
 * Returns 0 on success or error code on failure.
 */
static int read_option(arg)
  char *arg;            /* Pointer to the parameter */
{
  u32 temp_u32;

  if (arg[0] != '-')
    return(CLI_UNRECON_OPTION);

  switch (arg[1])
  {
    case 'h' :
    case 'H' :
    case '?' :
    case 'v' :
      show_syntax();
      return(CLI_EXIT_REQ);

    case 'm' :
      if (!strtonum(&temp_u32, &arg[2]))
        return(CLI_RANGE_ERR);
      mtr_mod_id = (u8)temp_u32;
      break;

    case 'u' :
      if (!strtonum(&temp_u32, &arg[2]))
        return(CLI_RANGE_ERR);
      mtr_map_id = (u8)temp_u32;
      break;

    case 't' :
      mtr_trace = 0;
      break;
  }
  return(0);
}
