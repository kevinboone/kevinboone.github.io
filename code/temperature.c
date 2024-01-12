/*===========================================================================

  temperature.c

  A simple program to demonstrate 

  Placed in the public domain by Kevin Boone, November 2020.

  Wire an NTC thermistor so it makes the upper limb of a potential divider,
  with a resistor of the thermistor's nominal value in the lower limb. 

  Note that the accuracy of the calculation does depend on matching the
  fixed resistor in the potential divider to the nominal resistance of
  the thermistor. The fixed resistor could, in fact, be a trimmer or,
  more likely, a small-valued trimmer in series with a larger fixed 
  resistor. In practice, the fixed resistance will vary a little with 
  temperature, so the accuracy will not be perfect.

  We assume that, when the thermistor is shorted, so that the analog input
  is at the reference voltage, the ADC reads its
  maximum value -- 255 for an 8-bit ADC. It doesn't matter what the actual
  reference voltage is, so long as it causes the ADC to read maxium.

===========================================================================*/
 
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <linux/i2c.h>
#include <linux/i2c-dev.h>
#include <math.h> 

// I2C properties
#define I2C_DEV "/dev/i2c-1"
#define I2C_ADDR 0x4B

// Absolute zero, in Celsius
#define T_ZERO - 273.15

// Thermistor properties

// Base temperature of the thermistor, in Celsius. This is the temperature at
//  which it will take its nominal resistance.
#define T_BASE 25.0
// Temperature coefficient of resistance of the thermistor.
#define BETA 3950.0

// Base temperature of the thermistor in absolute units.
#define T_BASE_K (T_BASE - T_ZERO)

// Note that the nominal value of the thermistor does not feature in the
//  calculation, because we're using it in a potential divider with a fixed
//  resistor whose value matches the nominal value.

// Value that the ADC chip reads when the analogue input is at maximum --
//   that is, with the thermistor shorted
#define MAX_ADC 255

int main (int argc, char **argv)
  {
  int i2c = open (I2C_DEV, O_RDWR);
  if (i2c >= 0)
    {
    printf ("i2c open\n");
    if (ioctl (i2c, I2C_SLAVE, I2C_ADDR) >= 0)
      {
      printf ("Device address set\n");

      // Form the command byte, which will be the same for each cycle

      int single_ended = 0x80; // bit 7 set for single-ended
      int dac_on_ref_off = 0x04;// bits 2-3 -- DAC on, ref off 
      int channel = 0x000; // bits 4-6 contain the channel number to sample

      int cmd = single_ended | dac_on_ref_off | channel;       

      while (1)
        {
	// Write the command byte 
	write (i2c, &cmd, 1);
	unsigned char data[1];
	
	// Read the data byte
	read (i2c, &data, 1);

	// A is the proportion that the measured value makes of
	//  the reference value
        double A = data[0] / (double)MAX_ADC;
	
	// Define L just to make the final calculation less ugly
        double L = log ((1 - A) / A);
        //Final result is T, in Celsius
	double T = 1.0 / (L / BETA + 1 / (T_BASE_K)) + T_ZERO;

	printf ("Temp is %g Celsius\n", T);

	usleep (1000000); // Delay 1 sec -- temp does change that quickly
	}
      }
    else
      {
      fprintf (stderr, "Can't open i2c %s\n", I2C_DEV);
      }

    close (i2c);
    }
  else
    {
    fprintf (stderr, "Can't set device address %04x\n", I2C_ADDR);
    }
  return 0;
  }

