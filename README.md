# UUIDP
UUID paranoia

   Generate a hopefully perfectly (paranoid) collision free UUID(P).
   
   A UUIDP consists of: (a) UUID Type 4 identifier (random part); (b) the current time from System.currentTimeMillis() (systematic part 1); (c) the current value from System.nanoTime() (systematic part 2). b and c are xored with XOR_PATTERN and the UUID (a) to look more random, i.e. to hide the leading zeros.
   
   To get a collision you would have to get a collision of two UUID identifiers (1/2^128) at the same wall clock time (extreemly unlikely) at the same time and runtime time (this should be unlikely enough...). UUIDP can be compared and ordered by creation time. Strong integrity protection is provided by an embedded CRC32.
   
