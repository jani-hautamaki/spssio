# Introduction #

This page illustrates the problem of unstable POR files due to base-30 digits used for the data.

# Round-trip #

Steps to replicate the situation:

  1. Select transform data / compute variable
  1. Enter expression 188696690000000025 / 10000000000000000000
  1. Compute (result is approx. 0.018869669)
  1. Save as System file (SAV) to get the exact IEEE 64-bit floating point representation.
  1. Then save as Portable file (POR).
  1. Load the saved Portabe file, and save it as another Portable file with a new file name.
  1. Load the newest Portable file and save it as System file.

The expression `188696690000000025 / 10000000000000000000` results in number approximately `0.018869669`. This has to be done with the expression, because SPSS doesn't allow the user to enter numbers with more than 15 decimals.

The result is then saved into a System file. It will be saved as IEEE 64-bit floating-point **`27`**` 23 f7 0c 92 52 93 3f` (hex). When the same data is then saved as Portable file, the floating point value is conerted into base-30 representation with precision=11 giving `GTECSL0R001-C`.

When the Portable file containing the value `GTECSL0R001-C` is then loaded, the value `GTECSL0R001-C` is read and it is converted into IEEE 64-bit floating-point **`26`**` 23 f7 0c 92 52 93 3f`. This value, in turn, is converted into base-30 representation `GTECSL0QTTT-C` when the file is again saved into Portable file format.

Summary:

  1. **`27`**` 23 f7 0c 92 52 93 3f` converts into `GTECSL0R001-C`.
  1. `GTECSL0R001-C` converts back to **`26`**` 23 f7 0c 92 52 93 3f`
  1. **`26`**` 23 f7 0c 92 52 93 3f` converts into `GTECSL0QTTT-C`.

Conclusion: it is not possible to reliably round-trip base-30 digits through other representations. Therefore the data from a Portable file should be saved into a CSV file primarily in its original base-30 representation.

# TODO #

Hopefully this page will be elaborated later on.