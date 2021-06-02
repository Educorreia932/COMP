2. INT IDENT("a") VIRG IDENT("b") VIRG PVIRG IDENT("b") IGUAL CONST("3") PVIRG IDENT("a") IGUAL IDENT("b") MULT IDENT("c") PVIRG

3. 

### CST

```
Start
  |
  ├── Decl    
  |     |
  |     └─ INT IDENT VIRG IDENT PVIRG
  |         |    |     |    |     |
  |        int   a     ,    b     ;
  |
  ├─ AttribConst
  |      |
  |      └─ IDENT IGUAL CONST PVIRG                  
  |           |     |     |     |
  |           b     =     3     ;
  |
  └─ AttribExpr
         |
         └─ IDENT IGUAL Expr PVIRG 
             |     |     |    |
             a     =     |    ;
                         |
                         └─ IDENT MULT IDENT
                              |     |    |
                              b     *    c
```

### AST

```
Start
  |
  ├── int
  |    |
  |    ├── a
  |    |
  |    ├── b
  |    |
  |    └── c
  |
  ├── =
  |   |
  |   ├── b
  |   |
  |   └── 3
  |
  └── =
      |
      ├── a
      |
      └── *
          |
          ├── b
          |
          └── c
```

4. 3

5. 

|             | INT                                       | IDENT                                     | VIRG | PVIRG | CONST | IGUAL | MULT |
|-------------|-------------------------------------------|-------------------------------------------|------|-------|-------|-------|------|
| Start       | Start → {Decl} {AttribConst} {AttribExpr} | Start → {Decl} {AttribConst} {AttribExpr} |      |       |       |       |      |
| Decl        | Decl → INT IDENT {VIRG IDENT} PVIRG       |                                           |      |       |       |       |      |
| AttribConst |                                           | AttribConst → IDENT IGUAL CONST PVIRG     |      |       |       |       |      |
| AttribExpr  |                                           | AttribExpr → IDENT IGUAL Expr PVIRG       |      |       |       |       |      |
| Expr        |                                           | Expr → IDENT MULT IDENT                   |      |       |       |       |      |

Na tabela LL(1) da gramática, na linha do não-terminal *Start*, existem duas entradas na coluna do terminal *IDENT*, logo a gramática não é LL(1)  

6.

Start → {Decl} {Attrib}
Decl → INT IDENT {VIRG IDENT} PVIRG
Attrib → IDENT IGUAL ConstExpr PVIRG
ConstExpr → CONST | Expr
Expr → IDENT MULT IDENT

7.

|        | INT                                 | IDENT                                      | VIRG | PVIRG | CONST | IGUAL | MULT |
|--------|-------------------------------------|--------------------------------------------|------|-------|-------|-------|------|
| Start  | Start → {Decl} {Attrib}             | Start → {Decl} {Attrib}                    |      |       |       |       |      |
| Decl   | Decl → INT IDENT {VIRG IDENT} PVIRG |                                            |      |       |       |       |      |
| Attrib |                                     | Attrib → IDENT IGUAL (CONST \| Expr) PVIRG |      |       |       |       |      |
| Expr   |                                     | Expr → IDENT MULT IDENT                    |      |       |       |       |      |

Não existem células na tabela com mais do que 1 entrada, como tal a CFG B é LL(1).

8.

```java
Start() {
    if (token == "")
        return true;

    while (token.next()) && Decl()) {}

    while (token.next()) && Attrib()) {}

    return token == NULL;
}

Decl() {
    if (token == INT) {
        token = token.next();

        if (token == IDENT) {
            token = token.next();

            while (token == VIRG && token.next() == IDENT)  
                token = token.next().next();

            if (token == PVIRG) {
                token = token.next();

                return true;
            }
        }
    }

    return false;
}

Attrib() {
    if (token == IDENT) {
        token == token.next();

        if (token == IGUAL) {
            token == token.next();

            if (token == CONST)
                token = token.next();

            else if (!Expr())
                return false;

            if (token == PVIRG) {
                token = token.next();

                return true;
            }
        }
    }

    return false;
}

Expr() {
    if (token == IDENT) {
        token = token.next();

        if (token == MULT) {
            token = token.next();

            if (token == IDENT) {
                token = token.next();

                return true;
            }
        }
    }
}
```

9.

10.

11.
