package org.tudo.sse.semver;

class SemanticVersionParser {

    static SemanticVersionNumber parseNumber(String value) throws SemanticVersionParsingException {
        if(value.isBlank()) throw new SemanticVersionParsingException(value, "Semantic version cannot be empty", 0);

        ParsingState state = ParsingState.NUMBER;
        int parsingPosition = 0;
        StringBuilder currentValue = new StringBuilder();

        int currNumberPartIdx = 0;
        int[] numberParts = new int[] {-1, 0, 0};

        String preRelease = null;
        String buildMetadata = null;

        final char[] characters = value.toCharArray();

        for(char currentChar: characters) {

            switch(state){
                case NUMBER:
                    if(currentChar != '.' && currentChar != '-' && currentChar != '+') {
                        currentValue.append(currentChar);
                    } else {
                        if(currentValue.length() == 0)
                            throw new SemanticVersionParsingException(value, "Numeric identifier must have at least one digit", parsingPosition);

                        final String numValue = currentValue.toString();
                        currentValue.setLength(0);

                        if(!isNumericIdentifier(numValue))
                            throw new SemanticVersionParsingException(value, "Not a valid numeric identifier", parsingPosition);

                        numberParts[currNumberPartIdx] = asInt(numValue, value, parsingPosition);

                        if(currentChar == '.'){
                            if(currNumberPartIdx == 2)
                                throw new SemanticVersionParsingException(value, "Semantic version cannot have more than three parts", parsingPosition);

                            currNumberPartIdx += 1;
                        } else if(currentChar == '-'){
                            state = ParsingState.PRERELEASE;
                        } else {
                            state = ParsingState.BUILDMETADATA;
                        }
                    }
                    break;

                    case PRERELEASE:
                        if(currentChar != '+'){
                            currentValue.append(currentChar);
                        } else {
                            final String preReleaseValue = currentValue.toString();
                            currentValue.setLength(0);

                            if(!isValidPreRelease(preReleaseValue))
                                throw new SemanticVersionParsingException(value, "Not a valid prerelease identifier", parsingPosition);

                            preRelease = preReleaseValue;

                            state = ParsingState.BUILDMETADATA;
                        }
                        break;
                    case BUILDMETADATA:
                        currentValue.append(currentChar);

                        break;
            }

            parsingPosition += 1;
        }

        String remaining = currentValue.toString();

        switch(state){
            case NUMBER:
                numberParts[currNumberPartIdx] = asInt(remaining, value, parsingPosition);
                break;
            case PRERELEASE:
                if(!isValidPreRelease(remaining))
                    throw new SemanticVersionParsingException(value, "Not a valid prerelease identifier", parsingPosition);

                preRelease = remaining;
                break;
            case BUILDMETADATA:
                if(!isValidBuild(remaining))
                    throw new SemanticVersionParsingException(value, "Not a valid build identifier", parsingPosition);

                buildMetadata = remaining;
        }

        return new SemanticVersionNumber(numberParts[0], numberParts[1], numberParts[2], preRelease, buildMetadata);
    }

    private static boolean isValidBuild(String value){
        if(value.isBlank()) return false;

        final String[] parts = value.split("\\.");

        for(String part: parts){
            if(!isAlphanumericIdentifier(part) && !isDigits(part))
                return false;
        }

        return true;
    }

    private static boolean isValidPreRelease(String value){
        if(value.isBlank()) return false;

        final String[] parts = value.split("\\.");

        for(String part: parts){
            if(!isAlphanumericIdentifier(part) && !isNumericIdentifier(part))
                return false;
        }

        return true;
    }

    private static boolean isAlphanumericIdentifier(String value) {
        if(value.isEmpty()) return false;

        boolean hasNonDigit = false;

        for(char c : value.toCharArray()) {
            if(isNonDigit(c)) hasNonDigit = true;

            if(!isIdentifierCharacter(c)) return false;
        }

        return hasNonDigit;
    }

    private static boolean isNumericIdentifier(String value){
        if(value.isEmpty()) return false;
        if(value.equals("0")) return true;

        char[] chars = value.toCharArray();

        if(!isPositiveDigit(chars[0])) return false;

        for(int i = 1; i < chars.length; i++){
            if(!isDigit(chars[i])) return false;
        }

        return true;
    }

    private static boolean isDigits(String value){
        if(value.isEmpty()) return false;
        for(char c : value.toCharArray()){
            if(!isDigit(c)) return false;
        }
        return true;
    }

    private static boolean isIdentifierCharacter(char c){
        return isDigit(c) || isNonDigit(c);
    }

    private static boolean isNonDigit(char c){
        return c == '-' || isLetter(c);
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isPositiveDigit(char c){
        return (c >= '1' && c <= '9');
    }

    private static boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    private static int asInt(String value, String number, int pos) throws SemanticVersionParsingException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfx) {
            throw new SemanticVersionParsingException(number, "Expected an integer", pos);
        }
    }

    private enum ParsingState {
        NUMBER,
        PRERELEASE,
        BUILDMETADATA
    }
}
