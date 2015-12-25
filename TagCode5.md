# The undocumented tag code '5' explained #

The PSPP's excellent documentation on Portable file format is surely the best source of
information for it. However, at the time of writing this (14 Jan 2013),
the documentation is still missing one bit of information.

The documentation [A.6 Variable Count Record](http://www.gnu.org/software/pspp/pspp-dev/html_node/Variable-Count-Record.html#Variable-Count-Record)
says that

"The purpose of the second is unknown; it contains the value 161 in all portable
files examined so far."

Please notice that value 161 (dec) is '5B' in base-30. This is not a single value,
but instead a tag code '5' plus an integer 'B' (11 dec).

The integer stored with tag code '5' expresses the _precision_ used while serializing
floating point data into Portable file. The precision is the number of trigesimal
digits allowed at most. So, the value 'B' means that every base-30 number may
have at most 11 trigesimals. The sign, the trigesimal point and the exponent
don't count.

Most versions of SPSS use precision=11, but some older ones used less trigesimal
numbers. One SPSS Portable file that I was able to find to demonstrate this is
in [this](http://www.u.arizona.edu/~norrande/data.html) web site.

The direct to link to the portable file is
[here](http://www.u.arizona.edu/~norrande/STOPIN.POR). Inspection of the file
reveals that this file has been created with "SPSS for Unix, Release 6.1 (AIX 3.2)".
The file doesn't have value '5B' after the variable count record, so the PSPP people
surely haven't examined this file yet. The tag code '5' has value '9' which means
the precision=9. Inspection of the data contained in the file reveals that this is
the case: all values have at most 9 trigesimal digits.

I have notified pspp-dev@gnu.org about this tag code, so it'll probably be included there soon.