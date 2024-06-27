/*============================================================================
  zom.c

  (c)2013 Kevin Boone
  Distributed under the terms of the GNU Public Licence, version 2
============================================================================*/

#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>
#include <string.h>
#include <ctype.h>
#include <getopt.h>

#ifndef BOOL
typedef int BOOL;
#endif
#ifndef TRUE
#define TRUE 1
#endif
#ifndef FALSE
#define FALSE 0
#endif


#define DEFAULT_H0      1000.0
#define DEFAULT_Z0      50.0
#define DEFAULT_PERIODS 20
#define DEFAULT_BETA    0.0001 
#define DEFAULT_ALPHA   1.0 
#define DEFAULT_PI	0.0
#define DEFAULT_DELTA	0.0
#define DEFAULT_R       0 
#define DEFAULT_GAMMA   0 


/*============================================================================
  show_version 
============================================================================*/
void show_version (void)
  {
  printf ("%s version %s\n", NAME, VERSION);
  printf ("Copyright (c)2013 Kevin Boone\n");
  printf ("Freely distrbutable under the terms of the GNU Public Licence");
  printf ("\n");
  }


/*============================================================================
  show_usage
============================================================================*/
void show_usage (const char *argv0, FILE *out)
  {
  fprintf (out, "Usage: %s [options]\n", 
    argv0);
  fprintf (out, "Options:\n");
  fprintf (out, "  -a                debraining parameter alpha\n");
  fprintf (out, "  -b                zombie vigour parameter beta\n");
  fprintf (out, 
    "  -d                human natural death rate parameter delta\n");
  fprintf (out, "  -g                zombie fatality parameter gamma\n");
  fprintf (out, "  -h                Show this message\n");
  fprintf (out, "  -H                Initial number of humans\n");
  fprintf (out, "  -p                human birth rate parameter pi\n");
  fprintf (out, "  -r                retaliation delay\n");
  fprintf (out, "  -t                Time periods to run\n");
  fprintf (out, "  -v                Show version\n");
  fprintf (out, "  -w                Show winner only\n");
  fprintf (out, "  -Z                Initial number of zombies\n");
  fprintf (out, "For more information, see http://www.kevinboone.net/zom.html");
  }


/*============================================================================
  run_model
  Work out the next values of H and L from the curent values, and
  the values of the model parameters 
============================================================================*/
void run_model (double H_old, double Z_old, double *H_new, double *Z_new, 
    double beta, double alpha, double pi, double delta, double gamma)
  {
  double zom_attacks = beta * H_old * Z_old;

  double new_zoms = zom_attacks * (1.0 - gamma);
  double debrained_zoms = alpha * H_old;

  double new_humans = pi;
  double removed_humans = delta * H_old + zom_attacks;

  *H_new = H_old - new_zoms + new_humans - removed_humans;
  *Z_new = Z_old + new_zoms - debrained_zoms;

  // Populations cannot fall below one 
  if (*H_new < 1) *H_new = 0;
  if (*Z_new < 1) *Z_new = 0;
  }


/*============================================================================
  parse_number 
============================================================================*/
BOOL parse_number (const char *s_num, double *result)
  {
  if (sscanf (s_num, "%lf", result) == 1) return TRUE;
  return FALSE;
  }


/*============================================================================
  main
============================================================================*/
int main (int argc, char **argv)
  {
  BOOL usage = FALSE;
  BOOL version = FALSE;
  BOOL winner_only = FALSE;
  int periods = DEFAULT_PERIODS; // Time units to run
  int t = 0; // Time in periods
  int r = DEFAULT_R; // Retaliation delay in periods
  double H_0 = DEFAULT_H0; // Initial human population
  double Z_0 = DEFAULT_Z0; // Initial zombie population
  double beta = DEFAULT_BETA; // Zombifications per zom per human per time unit
  double alpha = DEFAULT_ALPHA; // Debrainings per zom per human per time unit
  double pi = DEFAULT_PI; // human birth rate per time unit
  double delta = DEFAULT_DELTA; // human death rate per time unit
  double gamma = DEFAULT_GAMMA; // zombie attack fatality factor 
  
  // Parse command line
  int c;
  while ((c = getopt (argc, argv, "hvwt:H:Z:a:b:p:d:r:g:")) != -1)
  switch (c)
    {
    case 'h':
      usage = TRUE;
      break; 

    case 'v':
      version = TRUE;
      break;

    case 'w':
      winner_only = TRUE;
      break;

    case 't':
      periods = atoi (optarg);
      if (periods == 0)
        {
        fprintf (stderr,"Invalid value '%s' for time periods\n", optarg);
        exit (-1);
        }
       break;

    case 'H':
      if (!parse_number (optarg, &H_0))
        {
        fprintf (stderr,"Parameter H must be a number\n");
        exit (-1);
        }
       break;

    case 'Z':
      if (!parse_number (optarg, &Z_0))
        {
        fprintf (stderr,"Parameter Z must be a number\n");
        exit (-1);
        }
       break;

    case 'a':
      if (!parse_number (optarg, &alpha))
        {
        fprintf (stderr,"Parameter a (alpha) must be a number\n");
        exit (-1);
        }
       break;

    case 'b':
      if (!parse_number (optarg, &beta))
        {
        fprintf (stderr,"Parameter b (beta) must be a number\n");
        exit (-1);
        }
       break;

    case 'p':
      if (!parse_number (optarg, &pi))
        {
        fprintf (stderr,"Parameter p (pi) must be a number\n");
        exit (-1);
        }
       break;

    case 'd':
      if (!parse_number (optarg, &delta))
        {
        fprintf (stderr,"Parameter d (delta) must be a number\n");
        exit (-1);
        }
       break;

    case 'g':
      if (!parse_number (optarg, &gamma))
        {
        fprintf (stderr,"Parameter g (gamma) must be a number\n");
        exit (-1);
        }
       break;

    case 'r':
      {
      double temp = 0;
      if (!parse_number (optarg, &temp))
        {
        fprintf (stderr,"Parameter d (delta) must be a number\n");
        exit (-1);
        }
      r = (int)temp;
      break;
      }
    default:
      show_usage (argv[0], stderr);
      exit(-1);
    }

  if (usage)
    {
    show_usage (argv[0], stdout); 
    exit(0);
    }

  if (version)
    {
    show_version (); 
    exit(0);
    }
  
  double Z = Z_0; // Zombie population at time t
  double H = H_0; // Human population at time t 

  BOOL done = FALSE;

  //printf ("Time Humans Zombies");

  for (t = 0; t < periods && !done; t++)
    {
    if (!winner_only)
      printf ("%d %lG %lG\n", t, H, Z);
    if (t >= r)
      run_model (H, Z, &H, &Z, beta, alpha, pi, delta, gamma); 
    else
      run_model (H, Z, &H, &Z, beta, 0, pi, delta, gamma); 
    if (Z < Z_0 / 1000)
      {
      if (winner_only)
        {
        printf ("*** Result: Humans win at t=%d\n", t);
        done = TRUE;
        }
      }
    else if (H < H_0 / 1000)
      {
      if (winner_only)
        {
        printf ("*** Result: Zombies win at t=%d\n", t);
        done = TRUE;
        }
      }
    }

  return 0;
  printf ("*** Result: Neither humans nor zombies win\n");
  }



