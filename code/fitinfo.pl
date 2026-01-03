#!/usr/bin/perl -w

#### fitinfo.pl

#### Usage: fitinfo.pl {FIT file} {FIT file}...
#### Shows a summary of an exercise activity by parsing the 'session' data
####   record from a Garmin activity FIT file.

#### This utility depends on Garmin's FitCSVTool.jar, and requires a suitable
####   Java JVM

#### Kevin Boone, January 2026

#### This code is for demonstration purposes, and is not expected to be of
####   production quality. You may do with it as you wish but there is,
####   of course, no warranty implied.

use strict;
use POSIX; # for floor()

my $FITCSVTOOL = "/usr/lib/FitCSVTool.jar"; # Indicate location of Garmin's JAR

# toepoch
# Convert Garmin's FIT timestamp to a Unix epoch time
sub toepoch ($)
  {
  # Garmin epoch is 00:00 UTC Dec 31 1989, or 631065600
  return $_[0] + 631065600;
  }

# tohm
# Convert a time in seconds to hours:minutes
sub tohm ($)
  {
  my $sec = $_[0];
  my $hr = floor ($sec / 3600); 
  my $min = floor (($sec - ($hr * 3600))/60);
  return "${hr}h:${min}m";
  }

# tolocaltime
# Display a Garmin FIT timestamp as local time
sub tolocaltime ($)
  {
  my $epoch_date = toepoch ($_[0]);
  return localtime ($epoch_date); 
  }

# parse_session
# Parse the session record from the FIT file, which should be passed as
#  an array of string tokens, as read from the relevant line in the
#  CSV file.
sub parse_session (@)
  {
  my $args = $_[0];
  for (my $i = 0; $i < scalar (@$args); $i++)
    {
    my $key = @$args[$i];
    if ($key eq "start_time")
      {
      my $start_time_local = tolocaltime (eval(@$args[$i+1]));
      print ("Start time: $start_time_local\n");
      }
    if ($key eq "total_elapsed_time")
      {
      my $duration = tohm (eval(@$args[$i+1]));
      print ("Duration: $duration\n");
      }
    if ($key eq "sport_profile_name")
      {
      my $type = @$args[$i+1];
      print ("Type: $type\n");
      }
    if ($key eq "total_strides")
      {
      my $steps = eval(@$args[$i+1]) * 2;
      print ("Steps: $steps\n");
      }
    if ($key eq "total_distance")
      {
      my $distance = eval(@$args[$i+1]);
      if ($distance != 0)
        {
        $distance /= 1000;
        printf ("Distance: %.2f km\n", ${distance});
        }
      }
    if ($key eq "avg_heart_rate")
      {
      my $avg_heart_rate = eval(@$args[$i+1]);
      print ("Average HR: $avg_heart_rate\n");
      }
    if ($key eq "max_heart_rate")
      {
      my $max_heart_rate = eval(@$args[$i+1]);
      print ("Max HR: $max_heart_rate\n");
      }
    }
  }

# do_file
# Process and display the summary from a single FIT file
sub do_file ($)
  {
  my $file = $_[0];
  # Note that we put the converted CSV file in the same directory
  #  as the FIT file, so the directory must be writable
  my $converted_file = "$file.csv";
  # Run Garmin's Java utility, to convert the FIT file into CSV
  # Ideally, we need better error reporting here. I'm suppressing output from
  #   Garmin's utility, because it's so noisy.
  system ("java -jar $FITCSVTOOL -b $file $converted_file > /dev/null");
  open IN, "<$converted_file" or die "Can't convert FIT file $file\n";
  my $done = 0;
  # Read the CSV line by line, until we reach the session record
  while (<IN>)
    {
    my @args = split (",", $_);
    if ($args[0] eq "Data" and $args[2] eq "session")
      {
      parse_session (\@args);
      print "\n";
      $done = 1;
      last;
      }
    }
  close IN;
  if (!$done)
    {
    # Warn if there was no session record -- not all FIT files have one.
    printf 
      ("FIT file did not contain a 'session' record. Is it the right type?\n");
    }
  unlink ($converted_file); # Remove the CSV file
  }

# Start here

if (@ARGV < 1)
  {
  die "Usage: fitinfo.pl {FIT files...}\n";
  }

# Check we can run java -- the Garmin FIT converter is a Java program.
if (system ("which java > /dev/null") != 0)
  {
  die "Can't run Java JVM. Sorry.\n";
  }
  

# Loop through the files on the command line, processing each one.
foreach my $file (@ARGV)
  {
  do_file ($file);
  }


