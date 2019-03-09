/**
 * @author Petros Soutzis
 */
public class Generate extends AbstractGenerate {

    private int indentationLevel; // This variable indicates the current indentation level

    Generate(){

        this.indentationLevel = 0; // initialise to 0, as it is the start of the program
    }


    /**
     * Indicates that a non-terminal method has been entered.
     * Will print tabs, equal to the level of indentation
     * @param name The name of the non-terminal
     */
    @Override
    public void commenceNonterminal( String name ) {
        for(int i=0; i<indentationLevel; i++)
            System.out.print("\t"); // print a tab 'n' times, where n = level
        super.commenceNonterminal(name);
        this.indentationLevel++;
    } // end of method commenceNonterminal

    /**
     * This method will indicate that a temporary variable V was created
     * @param v The variable to add
     */
    @Override
    public void addVariable(Variable v) {
        for(int i=0; i<indentationLevel; i++)
            System.out.print("\t"); // print a tab 'n' times, where n = level
        super.addVariable(v);
    }

    /**
     * Prints the terminal token that was read
     * @param token The terminal token
     */
    @Override
    public void insertTerminal( Token token ) {
        for(int i=0; i<indentationLevel; i++)
            System.out.print("\t"); // print a tab 'n' times, where n = level
        super.insertTerminal(token);
    } // end of method insertTerminal


    /**
     * Indicates that a non-terminal method has been exited.
     * Will print tabs, equal to the level of indentation
     * @param name The name of the non-terminal
     */
    @Override
    public void finishNonterminal( String name ) {
        this.indentationLevel--;
        for(int i=0; i<indentationLevel; i++)
            System.out.print("\t"); // print a tab 'n' times, where n = level
        super.finishNonterminal(name);
    } // end of method finishNonterminal

    /**
     * This method is called from the SA and throws a CompilationException
     * @param token The token that caused the error
     * @param explanatoryMessage The message that explains the problem
     * @throws CompilationException Every time that this method is called
     */
    @Override
    public void reportError(Token token, String explanatoryMessage) throws CompilationException {

        throw new CompilationException(explanatoryMessage, token.lineNumber);
    }

    /**
     * This method is called from the SA and throws a CompilationException
     * @param token The token that caused the error
     * @param explanatoryMessage The message that explains the problem
     * @throws CompilationException Every time that this method is called
     */
    void reportError(Token token, String explanatoryMessage, CompilationException exception)
            throws CompilationException {

        throw new CompilationException(explanatoryMessage, token.lineNumber, exception);
    }
}
