# Challenge Activity 1 – Parsing and Semantic Analysis

3. `"/""*"[a-zA-Z0-9]*"*""/"`
   
4. `<COMMENT: "/*" (["A"-"Z","a"-"z", "0-9"])* "*/">`
   
5. no value
   
6. To disambiguate between an array assignment and an expression, we need a lookahead of infinite value.
This can be accomplished by using a semantic LOOKAHEAD.

7. k=2
   
8. The construct "int" and the construct "int" "[" "]" have a single common-prefix "int".
In order for the parser to choose the right one, it needs to evaluate 2 tokens at once, that is, LOOKAHEAD(2).

9. [9.pdf](9.pdf)
    
10. 

3) !new int[2];
4) true.brake(1);
5) 9.length;

11.

1) Only allow negating boolean iterals/expressions.
2) Boolean literals shall not have methods.
3) Primitive types, such as integers, can not have attributes.
