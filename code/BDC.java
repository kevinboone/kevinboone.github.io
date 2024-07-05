/*
Bullet drop calculation in Java

Copyright (c)2015 Kevin Boone. Distributed under the terms of the GNU Public
Licence V2.0. Please see http://kevinboone.net/zom2.html for an explanation of
the algorithm and parameters.
*/

package net.kevinboone.ballistics.bdc;
import java.util.*;

/** This class plots the trajectory of a bullet when launched from a 
 * rifle at a specific angle and muzzle velocity. It assumes that the
 * bullet weight is known, and that the bullet's coefficient of drag can
 * be determined from G1 ballistic tables. Input parameters are 
 * given at the start of the main() method. */
public class BDC
  {
  /* Some physical and converion constants. */
  private static final double MACH1 = 340.3; // Mach 1 in metres/sec
  private static final double POUNDS_PER_KG = 2.20462; 
  private static final double INCHES_PER_METRE = 39.3701; 
  private static final double g = 9.81; // Gravitaiional accl, m/sec2
  private static final double GRAINS_PER_KG = 15432;
  // A mil (milliradian) is thousandth of a radian
  private static final double MILS_PER_MOA = (1.0/60.0) / 360.0 
      * 2.0 * Math.PI * 1000.0;

  /* Ballistic table G1. Coefficient of drag for a reference bullet
     of 1-inch diameter and 1-pound weight at various speeds, expressed
     as mach numbers. See 
     http://www.jbmballistics.com/ballistics/downloads/text/mcg1.txt */ 
  public static final double CD_G1[][] = 
    {
    {0.00,0.2629},
    {0.05,0.2558},
    {0.10,0.2487},
    {0.15,0.2413},
    {0.20,0.2344},
    {0.25,0.2278},
    {0.30,0.2214},
    {0.35,0.2155},
    {0.40,0.2104},
    {0.45,0.2061},
    {0.50,0.2032},
    {0.55,0.2020},
    {0.60,0.2034},
    {0.70,0.2165},
    {0.725,0.2230},
    {0.75,0.2313},
    {0.775,0.2417},
    {0.80,0.2546},
    {0.825,0.2706},
    {0.85,0.2901},
    {0.875,0.3136},
    {0.90,0.3415},
    {0.925,0.3734},
    {0.95,0.4084},
    {0.975,0.4448},
    {1.0,0.4805},
    {1.025,0.5136},
    {1.05,0.5427},
    {1.075,0.5677},
    {1.10,0.5883},
    {1.125,0.6053},
    {1.15,0.6191},
    {1.20,0.6393},
    {1.25,0.6518},
    {1.30,0.6589},
    {1.35,0.6621},
    {1.40,0.6625},
    {1.45,0.6607},
    {1.50,0.6573},
    {1.55,0.6528},
    {1.60,0.6474},
    {1.65,0.6413},
    {1.70,0.6347},
    {1.75,0.6280},
    {1.80,0.6210},
    {1.85,0.6141},
    {1.90,0.6072},
    {1.95,0.6003},
    {2.00,0.5934},
    {2.05,0.5867},
    {2.10,0.5804},
    {2.15,0.5743},
    {2.20,0.5685},
    {2.25,0.5630},
    {2.30,0.5577},
    {2.35,0.5527},
    {2.40,0.5481},
    {2.45,0.5438},
    {2.50,0.5397},
    {2.60,0.5325},
    {2.70,0.5264},
    {2.80,0.5211},
    {2.90,0.5168},
    {3.00,0.5133},
    {3.10,0.5105},
    {3.20,0.5084},
    {3.30,0.5067},
    {3.40,0.5054},
    {3.50,0.5040},
    {3.60,0.5030},
    {3.70,0.5022},
    {3.80,0.5016},
    {3.90,0.5010},
    {4.00,0.5006},
    {4.20,0.4998},
    {4.40,0.4995},
    {4.60,0.4992},
    {4.80,0.4990},
    {5.00,0.4988}
    };


  /** Helper function to calculate a square. */
  private static double sqr (double x)
    {
    return x * x;
    }


  /** Given three points x0,y0; x1,y1; x2,y2, with a zero crossing assumed to
      lie between x0 and x1, estimate the zero crossing by fitting a quadratic
      to the three points. This is an application of Muller's method --
      see http://mathworld.wolfram.com/MullersMethod.html */ 
  static double getZeroCrossing (double x0, double y0, double x1, double y1,
      double x2, double y2)
    {
    double q = (x2 - x1) / (x1 - x0);
    double A = q*y2 - q*(1+q)*y1+sqr(q)*y0;
    double B = (2*q+1)*y2 - sqr(1+q)*y1 + sqr(q)*y0;
    double C = (1+q)*y2; 

    double D = sqr(B) - 4 * A * C;
    if (D >= 0)
      {
      double d1 = B + Math.sqrt (D);
      double d2 = B - Math.sqrt (D);
      double d = Math.max (d1, d2);
      return x2 - (x2 - x1) * 2.0 * C / d;
      }
    else throw new RuntimeException ("No zero crossing");
    }


  /** Get the drag coefficient from the ballistic table for a specific
      projectile velocity in metres/sec. */
  public static double getCD_mps (double mps)
    {
    return getCD_mach (mps / MACH1);
    }


  /** Get the drag coefficient from the ballistic table for a specific
      projectile velocity in mach number.
      Because CD varies only slightly with velocity (with the possible 
      exception of close to mach-1) we will just use linear
      interpolation in the ballistic table. Probably quadratic would be
      better around mach-1. */
  public static double getCD_mach (double mach)
    {
    double CDTable[][] = CD_G1;

    int i_below = 0;
    int i_above; 
    for (int i = 0; i < CDTable.length; i++)
      {
      if (mach >= CDTable[i][0] && mach < CDTable[i+1][0])
        i_below = i;
      }
    i_above = i_below + 1;

    // Our mach value lies between CD_x0 and CD_x1
    double mach_x0 = CDTable[i_below][0];
    double mach_x1 = CDTable[i_above][0];
    double CD_y0 = CDTable[i_below][1];
    double CD_y1 = CDTable[i_above][1];

    double slope = (CD_y1 - CD_y0) / (mach_x1 - mach_x0);
    double CD = CD_y0 + slope * (mach - mach_x0);

    return CD; 
    }

  /* Calculate the trajectory, as an array of Point objects, for
     the given ballistic and environmental properties. Note 
     that the resulting array will be of a size that cannot be
     determined in advance. */
  public static Point[] getTrajectory (double range, 
      double departureAngleDegrees, double muzzleVelocityMps, 
      double massKg, double bc, double rho, double sightHeightM)
    {
    Vector<Point> points = new Vector<Point>();

    double massPound = massKg * POUNDS_PER_KG;
    double refSecDensity = 1; // l/in2
    double deltaT = 0.005;

    /* Split the initial muzzle velocity into horizontal and vertical
       components, according to the departure angle. */ 
    double departureAngleRadians = departureAngleDegrees 
      / 360.0 * 2.0 * Math.PI;
    double v_x = muzzleVelocityMps * Math.cos (departureAngleRadians);
    double v_y = -muzzleVelocityMps * Math.sin (departureAngleRadians);

    /* s_x and s_y are the coordinates of the projectile, 
       positive _downwards_. */
    double s_x = 0;
    double s_y = 0;
   
    double t = 0; // Time elapsed, in seconds

    /* We can't use a fixed number of iterations based on time, because we
       don't know how long it will take the bullet to reach the required
       range. Instead, iterate until the distance reaches the range, and
       accept that the results will be a variable number of points in 
       space. */
    while (s_x < range)
      {
      /* Horizontal and vertical velocities must be treated as separate
         components, but we still need the axial velocity to work out
         the drag. */
      double v = Math.sqrt (sqr (v_x) + sqr (v_y));

      /* Get the coefficient of drag from the table for this axial velocity. */
      double cd = getCD_mps (v);

      /* Use the drag equation to work out the axial drag on the projectile. */
      double F = 0.5 * rho * sqr(v) * Math.PI / 4 
        / sqr (1.0 * INCHES_PER_METRE) * cd * refSecDensity * massPound / bc;

      //F = 0.5 * rho * sqr(v) * cd / bc;

      /* Split the drag into horizontal and vertical components. Exercise:
         prove that this method gives the same result as F_x=Fsin(theta)
         F_y = Fcos(theta) would give. */
      double F_x = F * v_x / v;
      double F_y = F * v_y / v;

      /* Work out the acceleration at this instant from F=ma, in
         separate horizontal and vertical directions. Gravity applies
         only vertically, of course. */
      double a_x = -F_x / massKg;
      double a_y = -F_y / massKg + g;

      /* Work out the change in velocity resulting from the acceleration. */
      double deltaV_x = a_x * deltaT;
      double deltaV_y = a_y * deltaT;

      double drop = s_y + sightHeightM;

      /* The minutes-of-anlge difference between the scope center and the
         projectile, assuming that, for small theta, theta=sin(theta) */
      double moa = drop / s_x * 360.0 / 2.0 / Math.PI * 60.0;

      /* Store the results for this point in space. */
      Point p = new Point (s_x, drop, v, moa);
      points.add (p);
      
      /* Work out the incremental distances caused by the velocity in
         this instant. */
      double deltaS_x = (v_x + deltaV_x /2) * deltaT;
      double deltaS_y = (v_y + deltaV_y /2) * deltaT;

      /* Finally, adjust the velocity, position, and time according to their
         incremental values, ready for the next pass. */
      v_x += deltaV_x;
      v_y += deltaV_y;
      s_x += deltaS_x;
      s_y += deltaS_y;
      t += deltaT;
      }

    return points.toArray(new Point[0]);
    }


  public static double getZeroDistance (double range, 
      double departureAngleDegrees, double muzzleVelocityMps, 
      double massKg, double bc, double rho, double sightHeightM)
    {
    Point[] trajectory = getTrajectory (range, departureAngleDegrees,
      muzzleVelocityMps, massKg, bc, rho, sightHeightM);

    double distance_0 = 0;
    double distance_1 = 0;
    double distance_2 = 0;
    double drop_0 = 0;
    double drop_1 = 0;
    double drop_2 = 0;
    
    for (int i = 1; i < trajectory.length - 2; i++)
      {
      double distance = trajectory[i].distance;
      double d1 = trajectory[i].drop;
      drop_0 = trajectory[i].drop;
      drop_1 = trajectory[i + 1].drop;

      if (drop_0 <= 0.0 && drop_1 > 0.0) 
        {
        drop_2 = trajectory[i + 2].drop;
        distance_0 = trajectory[i].distance;
        distance_1 = trajectory[i + 1].distance;
        distance_2 = trajectory[i + 2].distance;
        break;
        }
      }

    return getZeroCrossing (distance_0, drop_0, distance_1, drop_1, 
      distance_2, drop_2);
    }



  public static void main (String[] args)
    {
    double range = 400; // Distance over which to plot results
    double departureAngleDegrees = 0.1283; // Angle of elevation of the muzzle 
    double muzzleVelocityMps = 988; // Metres per second
    double massGrains = 55.0; // Mass of the bullet in grains
    double rho = 1.225; // Air desnity in Kb/m3
    double bc = 0.225; // Ballistic coefficient, from manufacturer
    double sightHeightM = 0.01; // Height of the sight above the muzzle
    double massKg = massGrains / GRAINS_PER_KG ;

    /* Work out the distance at which the rifle is zeroed for the
       specified configuration (if there is one). We don't use this
       in later calculations -- but it makes it easier to tweak the
       program parameters to model sighting in at a specific distance. */ 
    double zeroDistance = getZeroDistance (range, departureAngleDegrees,
      muzzleVelocityMps, massKg, bc, rho, sightHeightM);

    System.out.println ("Zero distance: " + zeroDistance + "m");

    Point[] trajectory = getTrajectory (range, departureAngleDegrees,
      muzzleVelocityMps, massKg, bc, rho, sightHeightM);

   System.out.printf ("%s, %s, %s, %s, %s\n", 
      "s_x", "v", "drop", "MOA compensation", "Mils compensation");
    for (int i = 0; i < trajectory.length; i++)
      {
      Point point = trajectory[i];
      System.out.printf ("%2.4f, %2.4f, %2.4f %2.4f, %2.4f\n", 
        point.distance, point.velocity, point.drop, point.moa, 
         point.moa * MILS_PER_MOA); 
      }

    }

  }


/** Class Point represents a point along the projectile's trajectory. */
class Point 
  {
  /** Distance from muzzle in metres. */
  public double distance;
  /** Drop from the muzzle in metres, -ve is upwards. */
  public double drop;
  /** Velocity of the projectile along its direction of travel,
      in metres/second. */
  public double velocity;

  /** Angle for the sight to come _up_ in minutes of angle. */
  public double moa;

  public Point (double distance, double drop, double velocity, double moa)
    {
    this.distance = distance;
    this.drop = drop;
    this.velocity = velocity;
    this.moa = moa;
    }
  }


