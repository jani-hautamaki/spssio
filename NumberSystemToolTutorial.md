# Introduction #

Background information about positional number systems can be found from [Wikipedia](http://en.wikipedia.org/wiki/Numeral_system). In this page, a _number system_ is a synonym for positional base-_b_ numeral system.

**Fact: SPSS Portable file format uses a base-30 number system (0-9, and then A-S for numbers 10-29) for integers and doubles.**

The base-30 number system is also known as the _trigesimal number system_.

This representation causes serious difficulties with respect to keeping the data unmodified at bit-level while serializing and deserializing a data file (see [round-tripping base-30 numbers](RoundTripBase30Digits.md)).

The sole purpose of the SPSS Portable file format was, at least officially(?), to enable data exchange between mainframes operating on different platforms (like VAX, DEC, Unix and so on). However, from the contemporary perspective, you just have to wonder whether the real purpose was to vendor lock the users to a certain product. At least the author has never ever heard of any other product using base-30 representation for numbers (excluding the ones which are trying to imitate SPSS, eg. this project, and PSPP).

The primary motivation for creating `NumberSystemTool`, known hereafter as `numsystool` was to enable studying how the author's algorithms for number serialization and deserialization performed, especially when compared to SPSS, and to the ones provided by Java.

These two routines, serialization and deserialization, are highly critical to any program using data from SPSS  Portable files. Their importance cannot be emphasized enough. To be clear, the author's implementations of these routines are found in the following source code files:

  * Deserialization (`String`-> `double`): [NumberParser.java](http://code.google.com/p/spssio/source/browse/src/java/spssio/util/NumberParser.java)
  * Serialization (`double` -> `String`): [NumberFormatter.java](http://code.google.com/p/spssio/source/browse/src/java/spssio/util/NumberFormatter.java)

Because these routines are at the heart of a program working with SPSS Portable file format, these two source code files have an exceptionally high quality expectations. They are, hopefully, state-of-the-art.

# Starting the program #

First, you need to append the PATH environment variable with the `bin` directory. Once you have done that, starting the program is easy. Just execute a command...

```
numsystool
```

When the program starts, it outputs some basic information regarding the data types (which depend on the platform). For instance, the biggest and smallest values available for the primitive data type `double` are outputted in the beginning. These limit values are explained in detail later.

After all the outputted information there should be a prompt, and after that the cursor, waiting for input:
```
tool b=10 m=double>
```

# Basic operating principle #

The basic operating principle of the program is:

  1. Read a number from stdin (from `String` to `double`)
  1. Write the same number to stdout (from `double` to `String`)

Put put concisely, the operation is: read-write of the same number. The program can, therefore, be used to "round-trip" numbers through the author's code.

Reading operation involves turning a `String` into a `double`. This is known as the **deserialization**. Writing operation, in turn, involves turning a `double` (or an `int`) to a String. This is known as the **serialization**.

Both of aspects of the read-write operation can be configured to a high degree. Most importantly, the routines used for deserialization and serialization can be selected, that is, the user can choose whether to use the author's implementations or the ones provided by Java run-time libraries. The user can also choose the number system which is being used. Furthermore, input and output number systems can be chosen separately from each other, if so desired.

Once the program has been fired up, the prompt tells details about the things used for INPUT, that is, for deserializing numbers. Specifically, the prompt tells what is the base of the input number system (in the beginning b=10, ie. decimals), and which routine is used for deserialization (in the beginning it is `tool`, which means that the author's code (the code contained in the tool itself) is used for deserialization.

To see the basic operation of the program, type, for instance, some integer number and press enter. Here is an example run for the number "`1234567`":

```
tool b=10 m=double> 1234567
                    dec: 1234567.0   (Double.toString)
                    raw: 4132d68700000000   (doubleToRawLongBits)

                    1234567
                    ^ tool b=10 prec=16 m=double
```

The fields of the output are:

  * **`dec: ...`** field is outputted always. It is the result of `Double.toString()` operation (as indicated clearly in the parenthesis) applied to the result of the deserialization routine (remember, the deserialization routine was the one which converts `String`s to `double`s).
  * **`raw: ...`** field is outputted always. It is the result of the `Double.double.ToRawLongBits()` operation applied to the result of the deserialization routine. Here it is important to know that the result is 64-bit representation according to the IEEE 754 floating point "double format" bit layout. This is sometimes very useful for debugging the details. More information regarding the operation can be found from the API documenation, [here](http://docs.oracle.com/javase/7/docs/api/java/lang/Double.html#doubleToRawLongBits%28double%29)
  * **`1234567`** is the OUTPUT of the selected serialization routine.
  * **`^ ...`** field shows details regarding the current serialization routine. Circumflex was chosen to remind that the details shown apply to the field above.

To get a short summary of the available commands, type `\h`:
```
tool b=10 m=double> \h
```

All the commands start with a backslash (`\`). All input is treated as a number unless the input begins with a backslash OR contains multiple words (separated by spaces).

From here on, the prompt is not shown on the most command listings. Only the commands that are typed in, and the explicit output caused by the commands are shown.

# Command: `\base` #

The first command which is of immediate interest is the command which is used to set the base for input and/or the output number system. For instance, to convert decimal numbers to hexadecimal numbers, the input number system base should be set to 10, and the output number system base should be set to 16. Concisely: input radix is 10, output radix is 16. To achieve this, the bases are set separately by the following two commands:

```
\base in 10
\base out 16
```

After setting the base, the user is informed about the current status of the input and number system settings. The last output should be:

```
Output base set
Input base:       10
Output base:      16
Output precision: 14
```

Here, the program tells that the input number system is base-10 system, and the output is base-16 system. The "digits" used for the number system are automatically set so that:

  * digits 0-9: the characters **0-9** are used,
  * digits 10-35: the upper-case characters **A-Z** are used,
  * digits 36-61: the lower-case characerts **a-z** are used,
  * digit 62: the characters **`+`**,
  * digit 63: the characters **`/`**.

If you happen to need custom digits for some reason, see `\digits` command. This command enables you to use, for instance, the letters A-J instead of the numbers 0-9.

Okay, now we have set manually the input to base-10 and the output to base-16. For instance, the decimal number "1000" corresponds to the hexadecimal number "3E8". Lets try it out by typing "`1000`" to the prompt:

```
tool b=10 m=double> 1000
                    dec: 1000.0   (Double.toString)
                    raw: 408f400000000000   (doubleToRawLongBits)

                    3E8
                    ^ tool b=16 prec=14 m=double
```

The result is "`3E8`" as expected. It is obvious that the author's code is working correctly on simple cases like such as this one. Another magical decimal number is "`100`" which all geeks should know by heart to be `64` in hexadecimal. Lets verify this with the tool:

```
tool b=10 m=double> 100
                    dec: 100.0   (Double.toString)
                    raw: 4059000000000000   (doubleToRawLongBits)

                    64
                    ^ tool b=16 prec=14 m=double
```

Just as it should be. Lets set the bases of both input and output number system back to 10, ie. decimals. Setting both input and output at the same time can be done by omitting the number system specification (in/out) from the command:
```
\base 10
```

Now the base used for both input and output number system is set back to 10.

# Command: `\precision` #

The number of digits present in the significand (alias mantissa) is controlled by a setting called _output precision_. As the name suggest, this settings affects only the serialization procedure.

For instance, to limit the number of digits in the significand down to 3, execute a command:

```
\precision 3
```

The effects of this limited precision can be witnessed by typing in a large decimal number. For instance, number 1234567890:

```
tool b=10 m=double> 1234567890
                    dec: 1.23456789E9   (Double.toString)
                    raw: 41d26580b4800000   (doubleToRawLongBits)

                    123+7
                    ^ tool b=10 prec=3 m=double
```

The outputted number, "`123+7`", has three digits (123) in the significand, and a number 7 as its exponent. This number equals to 1 230 000 000, and it is a number with three digits in the significand closest to the original input number 1 234 567 890.

When a number is shortened in the way shown here, its precision is cut to a lesser degree than would be possible with the internal data type. This corresponds to _rounding_ the number to a closest number with three digits in the significand. Therefore, the underlying procedure must take care of the rounding appropriately. A naive approach would just cut the significand to the desired length instead of properly rounding the number.

Lets verify that the underlying procedure actually employs a proper rounding to the number. A good test number is, for instance, 1236567890, in which the fourth number, number 4, has been replaced with number 6, which should be rounded upwards. Type in the number:

```
tool b=10 m=double> 1236567890
                    dec: 1.23656789E9   (Double.toString)
                    raw: 41d26d21d4800000   (doubleToRawLongBits)

                    124+7
                    ^ tool b=10 prec=3 m=double
```

The number was rounded correctly upwards. The number 1 236 567 890 beacme 1 240 000 000, which is the number with three digits in the significand closest to the input number.

**NOTE:** The data type used internally has a major impact on the highest available precision.

To see this, consider a typical 64-bit IEEE double precision floating point. The mantissa (ie. significand) has 52 bits stored explicitly, and 1 bit stored implicitly. Therefore, the IEEE double precision floating point has 53-bit mantissa altogether. In other words, the `double`'s significand has 53 binary digits. Consequently, the biggest number expressible in the `double`'s significand is 2<sup>53</sup>.

The question is now: how many base-`b` digits is used (needed) by the biggest number expressible in the `double`'s significand? The answer determines the highest available precision for a procedure which uses `double` data type internally and base-`b` system for output.

For base-10 numbers, the highest one digit number is 9 which is 10<sup>1</sup> minus one. The highest two digit number is 99 which is 10<sup>2</sup> minus one. There is a pattern here, obviously. Generalizing: given base-`b` number system, the highest number expressible with `k` digits is `b`<sup>k</sup> minus one. If a number is equal to or greather than this value, then its expression in this number system requires more than `k` digits.

Turning the question around, how many base-`b` digits is needed by a certain value `x`? The objective is then to find such an integer `k` that `b`<sup>k</sup> is greater than `x`, but `b`<sup>(k-1)</sup> is still less than or equal to `x`. Put concisely: given `x`, find an integer `k` such that

```
b^(k-1) <= x < b^k
```

The answer is given by taking base-`b` logarithm of the given `x`, and rounding it upwards to the next integer. For those who can't remember their logarithms: changing the base of the logarithm can be accomplished with the equation `log(value)/log(new_base)`.

The answer to the original question (how many base-`b` digits is needed by the biggest number expressible in the `double`'s 53-bit significand) is, therefore, given by taking base-`b` logarithm of the number 2^53.

For instance, given base-10 output system and `double` data type, the highest available precision is then

```
ln(2^53) / ln(10) = 53*ln(2)/ln(10) = 15.955
```

The result can be interpreted as follows: 15 digit positions use the whole range of the digits (10 digits), but the last digit position uses only 9.55 digits. The result is therefore rounded upwards, because even though you are not using the whole range of the digits available, you're still using a whole digit position. Thus, the highest available precision for base-10 digits with `double` data type is 16 digits in the significand in the best case, and 15 digits in the worst case.

# Command: `\context` #

The data types used internally by the author's serialization and deserialization routines can be configured with the `\context` command. The command allows the user to switch between `double` and `BigDecimal` data types. This is a classical trade-off between accuary and speed. The primitive data type `double` provides speed with less accuracy, and the class `BigDecimal` provides accuracy with less speed.

When using `BigDecimal`s one is required to have a `MathContext` object which determines some parameters related to calculations, such as the rounding mode used and the precision. The most basic choices used in Java programs are pre-defined `MathContext` objects: `DECIMAL32`, `DECIMAL64`, and `DECIMAL128`. The number at the end of the each name corresponds to the bits available for the data type. The more there are bits available, the higher the achieved precision.

**NOTE:** Currently, only the deserialization (input) routine supports `BigDecimal`s. Consequently, setting a `MathContext` for the serialization (output) routine may result in unspecified behaviour.

An inconvenient fact about binary numbers is that there are numbers which cannot be represented exactly in binary floating-point, no matter what the precision is. For instance, one such a number is 1/3. The consequence is that any floating-point trying to represent 1/3 is inaccurate, and therefore doomed to start to detoriate at some point. Hence, this number, 0.33333...,  is a very good value for testing the limits of precision.

To demonstrate the limitations of the `double` data type lets set the input data type to `BigDecimal` and by using `DECIMAL128` as the `MathContext` object for input. This is achieved with the command

```
\context in 128
```

(More generally, to set `DECIMAL<num>` as the input `MathContext`, the command is "`\context in <num>`", where `<num>` is either 32, 64, or 128)

The prompt should change. After executing the command the new prompt should be:

```
tool b=10 m=128>
```

Now there is enough precision available for the input values. Well, at least much more than there is precision available for the output, which is still using the 64-bit `double` data type.

Next, lets set the output precision to some value bigger than the highest available precision. The `MathContex` object `DECIMAL128` should, according to [javadoc](http://docs.oracle.com/javase/7/docs/api/java/math/MathContext.html#DECIMAL128), provide at least 34 digits. That is probably a bit too much for our purposes, so lets settle to 24 digits, which 10 digits more than should be possible with `double`. Setting the precision:

```
\precision 24
```

So now the input is using `DECIMAL128` MathContext, and the output is still using 64-bit `double`, but will output 24 digits in the significand which is 10 digits more than the underlying data type should be capable of. Everything is set. Lets see what happens with the test number, 1/3, with 34 digits after the point:

```
tool b=10 m=128> 0.33333333333333333333333333333333
                 dec: 0.3333333333333333   (Double.toString)
                 raw: 3fd5555555555555   (doubleToRawLongBits)

                 .333333333333333303727386
                 ^ tool b=10 prec=24 m=double
```

Taking the result into a closer inspection. Lets enumerate the digits of the result to see the position where it starts to detoriate.

```
Result:   .333333333333333303727386
Digit:     123456789012345678901234567890
                    1         2
```

The result starts to detoriate after the 16th digit! The detoriation starts exactly at the predicted position. Pretty cool, huh?

Next, we might study the effects when the deserialization routine used for the input data is changed.

# Command: `\in` #

The user might not trust to a 3rd party code for converting input
strings into numeric data types (deserialization), but wants to
see how the serialization code performs in isolation of other 3rd
party code.  In that case, the deserialization code used to convert
the input string into a numeric data type (ie. `Double`) is changed
from the author's code to some time-honoured method such as the ones
provided by the Java API (eg. `Double.parseDouble`).

The deserialization algorithm can be chosen with the command "`\in`".
It takes an argument, which specifies the algorithm that is going
to be used for the deserialization. General format of the command is
then

```
\in <name>
```

where `<name>` is the name of the deserialization algorithm. Possible values for the argument are shown by the help command, "`\h`". The following values are currently available:

  * `java`: Uses the method `Double.parseDouble(String)`.
  * `float`: Uses the method `Float.parseFloat(String)`.
  * `tool`: Uses the author's code, ie. the method `NumberParser.parseDouble(String)`.
  * `raw`: Uses the method `Double.longBitsToDouble(Long)`.
  * `reshape`: Uses the author's code, the method `NumberFormatter.reformat(String)`. The input string is _reshaped_ into a new `String` with less precision without going through `String->double->String`. This is an advanced feature which is discussed later on.

To demonstrate further the effects of the internal data type, lets select the method `Float.parseFloat()` as the deserialization algorithm. This is done by executing the command

```
\in float
```

Now, the settings are as follows. Output precision has been set to 24,
which means that the author's algorithm will produce 24 digits for
the mantissa, and the algorithm used for converting `String` into
a numeric data type is `Float.parseFloat()`. Obviously the deserialization
is limited to the `float`.

The 32-bit IEEE single precision floating-point, also known as `float`
in Java, has a mantissa with 23 bits stored explicitly, and 1 bit stored
implicitly. Therefore, it has 24-bit mantisa altogether. Given a base-10
output system, and 24-bit mantissa of `float` data type, the highest
available precision is then

```
ln(2^24) / ln(10) = 24*ln(2)/ln(10) = 7.225
```

which is rounded upwards. The highest precision available for
`float` data type is therefore 8 digits in a base-10 number system
in the best case, and 7 digits in the worst case.

Lets see how the theory corresponds withthe reality.
Enter our favourite test number (one third, 1/3, with 24 digits
after the decimal point), and see the results.

```
float b=10/fixed> 0.33333333333333333333333333333333
                  dec: 0.33333334   (Float.toString)
                  raw: 3eaaaaab   (floatToRawLongBits)

                  .333333343267440795898438
                  ^ tool b=10 prec=24 m=double
```

Taking the result into a closer inspection. Lets enumerate the digits
of the result to see the position where it starts to detoriate.

```
Result:   .333333343267440795898438
Digit:     123456789012345678901234567890
                    1         2
```

The result starts to detoriate after the 7th digit. This time
the best-case scenario of 8 digits didn't happen, but the worst-case
precision of 7 digits is, of course, always guaranteed.

Switch back to the author's deserialization method.

```
\in tool
```

Enter a number, and press enter. You will see in the output
the "raw: ..." field. This field gives a hexadecimal representation
of the raw `double` (or the raw `float`) after it has been deserialized
from the input string using the chosen deserialization method.
The decimal serialization can be misleading, and therefore it is
sometimes desirable to see the resulting variable with such a bit-level
accuracy.

Enter the number "0.1":
```
tool b=10 m=double> 0.1
                    dec: 0.1   (Double.toString)
                    raw: 3fb999999999999a   (doubleToRawLongBits)

                    .1
                    ^ tool b=10 prec=24 m=double
```

In other words, the decimal number "0.1" is represented in the computer's
memory as a 8-byte value `3fb999999999999a`. Now, lets reverse
the process. We would like to be able to enter this raw 8-byte value
and to be able to get "0.1" as the result. This is achieved by
changing the deserialization method to "raw":
```
\in raw
```

Now the program accepts raw hexadecimal representations of `double`s.
They have to be exactly 16 hexadecimal digits long. Lets enter
the value "`3fb999999999999a`":
```
raw ieee64> 3fb999999999999a
            dec: 0.1   (Double.toString)
            raw: 3fb999999999999a   (doubleToRawLongBits)

            .1
            ^ tool b=10 prec=24 m=double
```
The program serialized this `double` into a string "`.1`", which was
the well-anticipated result. An intriguing question arises, what is
the closest value to this one representable in the `double` data type?
Lets increase the hexadecimal representation just by one (the last "`a`"
becomes "`b`"), and see what happens:

```
raw ieee64> 3fb999999999999b
            dec: 0.10000000000000002   (Double.toString)
            raw: 3fb999999999999b   (doubleToRawLongBits)

            .10000000000000002220446
            ^ tool b=10 prec=24 m=double
```

So this number, .10000000000000002220446, is the closest `double`
next to 0.1. Or is it, really? Repeat the process: increase
the hexadecimal value inputted by one (this time the last "`b`"
becomes "`c`"). This time something rather surprising happens:

```
raw ieee64> 3fb999999999999c
            dec: 0.10000000000000003   (Double.toString)
            raw: 3fb999999999999c   (doubleToRawLongBits)

            .10000000000000002220446
            ^ tool b=10 prec=24 m=double
```

Summarizing these two results: there are two different `double` values, `3fb999999999999b` and `3fb999999999999c`, which both resulted in the same decimal representation when using the tool's own serialization method. Nevertheless, Java's own `Double.toString()` was able to produce different serializations. What is going on here?

Variables are typically kept in the computer's memory most of the time. A variable is moved into a register when it is needed in some operation such as division or multiplication. The operation usually stores the result in a register, often overwriting an input value. After the operation, the program typically moves the result back into a variable kept in memory.

The tool's serialization algorithm is internally using `double` data type for its calculations. The `double` data type is 64 bits wide. However, the FPU registers in x86 architecture are wider. In fact, even the legacy registers ST(0)-ST(7) are 80 bits wide. They have 16 extra bits compared to `double` data type. The following fact is established.

_Moving FPU register contents to memory is most often a **narrowing** conversion_.

Here comes the tricky part: a sophisticated compiler may optimize a tight loop in such a way that some variables are kept in registers during the whole loop, and the registers' values are moved back into the memory only after the loop. The intermediate results during the loop have higher precision than the final result stored into a variable after the loop.

Optimization may have significant effects on the results. This, however, is difficult to show with Java, since there is not much control over the byte code generation. Instead, good old C code and a proper compiler can be used to show the effects of the optimization.

gcc compiler, not too old, is needed for the following code snippet, and an x86 platform. The details of the compiler and the platform used for the demonstration are shown below.

Compiler:
```
gcc --version
gcc (GCC) 4.4.7 20120313 (Red Hat 4.4.7-3)
```

Platform:
```
uname -a
Linux domain.name.here 2.6.32-358.23.2.el6.i686 #1 SMP Sat Sep 14 05:33:24 EDT 2013 i686 i686 i386 GNU/Linux
```

Here's the code snippet,
```
/*
 * (C) 2013 Jani Hautamaki
 * Demonstrates the effects of -ffloat-store optimization.
 *
 * Compile three different versions:
 * gcc precision.c -o a.out
 * gcc -O2 precision.c -o b.out
 * gcc -O2 -ffloat-store precision.c -o c.out
 *
 * You can turn off a specific optimization to recover
 * the result of the unoptimized version:
 */

#include <stdio.h>

/*
Compile with
   gcc test.c
or with
    gcc -O2 test3.c
and turn off the specific optimization trick:
    gcc -O2 -ffloat-store precision.c
*/

void printf_double(double d_fraction) {
    int i_integral;
    int i;
    char buffer[128];

    for (i = 0; i < 24; i++) {
        i_integral = (int) d_fraction;
        d_fraction = (d_fraction - (double)(i_integral)) * 10.0;
        buffer[i] = '0' + i_integral;
    }

    buffer[i] = '\0';
    printf("%c.%s\n", buffer[0], &buffer[1]);
}
int main(void) {
    /* union is used to avoid breaking strict aliasing rule */
    union {
       double as_double;
       unsigned int as_uint_array[2];
    } data;

    data.as_uint_array[1] = 0x3fb99999;
    data.as_uint_array[0] = 0x9999999c;

    printf("%.24f\n", data.as_double);
    printf_double(data.as_double);


    return 0;
}
```

Compile three versions:
```
gcc precision.c -o a.out
gcc -O2 precision.c -o b.out
gcc -O2 -ffloat-store precision.c -o c.out
```

Here the exceprt from the man page regarding `-ffloat-store` flag: "Do not store floating point variables in registers, and inhibit other options that might change whether a floating point value is taken from a register or memory."

  * Unoptimized version (`a.out`): gives the same result as `spssio`'s serialization algorithm.
  * Optimized version (`b.out`): gives the same result as Java's `Double.toString()` method.
  * Optimized version with -ffloat-store (`c.out`): gives the same result as the unoptimized version. This verifies that the difference in results is due to juggling floating point variables between memory and registers.

As a final verification use `BigDecimal`s in the serialization algorithm instead of `double`s. This is achieved by setting up `MathContext` for the output with the following command:

```
raw ieee64> \context out 128
```

Lets re-examine the serialization algorithm output for the raw ieee64 values `3fb999999999999b` and `3fb999999999999c` to see if they are different this time.

First value:
```
raw ieee64> 3fb999999999999b
            dec: 0.10000000000000002   (Double.toString)
            raw: 3fb999999999999b   (doubleToRawLongBits)

            .100000000000000019428903
            ^ tool b=10 prec=24 m=128
```

Second value:
```
raw ieee64> 3fb999999999999c
            dec: 0.10000000000000003   (Double.toString)
            raw: 3fb999999999999c   (doubleToRawLongBits)

            .100000000000000033306691
            ^ tool b=10 prec=24 m=128
```

Now, the `spssio`'s serialization routine is able to produce differing results for differing double values. However, this ability comes with a price. Working with `BigDecimal`s requires more computations than working with `double`s. Again, it is the classical trade-off between accuracy and speed.

(To be continued...)