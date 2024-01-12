/*===========================================================================

  ads7830.c

  A simple program to demonstrate polled AD conversion using the
  TI AD7830 chip. I wrote this specifically for the Raspberry Pi 3, but
  it should work on any Linux system with the conventional /dev/i2c-X
  interface.

  Placed in the public domain by Kevin Boone, November 2020.

===========================================================================*/
 
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <linux/i2c.h>
#include <linux/i2c-dev.h>

#define I2C_DEV "/dev/i2c-1"

// Device address is set to 0x48, OR-ed with the address control lines. 
// If these are not connected, the address will be 0x4b (0x48 | 0x03) 
#define I2C_ADDR 0x4B

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
      int dac_on_ref_off = 0x04; // bits 2-3 -- AD on, reference off 
      int channel = 0x000; // bits 4-6 contain the channel number to sample

      int cmd = single_ended | dac_on_ref_off | channel;       

      while (1)
        {
	// Write the command byte 
	write (i2c, &cmd, 1);
	unsigned char data[1];
	// Read the data byte
	read (i2c, &data, 1);
	printf ("%0d\n", data[0]);
	usleep (100000); // Delay 0.1 sec
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

