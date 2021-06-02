class Homework {
    static String generateTokens(String s) {
        StringBuilder tokenSequence = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isDigit(c)) {
                for (int j = i; j < s.length(); j++) {
                    if (!Character.isDigit(c))
                        tokenSequence.append(c);

                    i++;
                }
            }

            else if (Character.isLetter(c)) {
                for (int j = i; j < s.length(); j++) {
                    if (!Character.isLetterOrDigit(c))
                        tokenSequence.append("VAR");

                    i++;
                }
            }

            else {
                switch (c) {
                    case '(':
                        tokenSequence.append("LPAR");
                        break;
    
                    case ')':
                        tokenSequence.append("RPAR");
                        break;
    
                    case '*':
                        tokenSequence.append("MUL");
                        break;
    
                    case '\\':
                        tokenSequence.append("DIV");
                        break;
    
                    case '+':
                        tokenSequence.append("PLUS");
                        break;
    
                    case '-':
                        tokenSequence.append("SUB");
                        break;
    
                    case '=':
                        tokenSequence.append("EQ");
                        break;
    
                    case ';':
                        tokenSequence.append("SMICOLON");
                        break;
                }
            }

            tokenSequence.append(" ");
        }

        return tokenSequence.toString();
    }

    public static void main(String[] args) {
        String s = "foo = (1 + 2) * 3 / 5;";

        String tokenSequence = generateTokens(s);

        System.out.println(tokenSequence);
    }
}
