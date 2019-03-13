import java.util.HashMap;

/**
 * This is the generate class, that will be used by the SyntaxAnalyser to print the results of the
 * analysis to the output stream.
 *
 * @author Petros Soutzis, 34023852
 */
public class Generate extends AbstractGenerate {

    private int indentationLevel; // This variable indicates the current indentation level

    /**
     * The constructor of the Generate class. Initialises indentationLevel variable to 0, as it is the start of
     * a program
     */
    Generate(){

        this.indentationLevel = 0; // initialise to 0, as it is the start of the program. Indentation is 0
    }

    /**
     * This method will indicate that a temporary variable V was created
     * @param v The variable to add
     */
    @Override
    public void addVariable(Variable v) {
        for(int i=0; i<indentationLevel; i++)
            System.out.print("\t"); // print a tab 'n' times, where n = indentation level
        super.addVariable(v);  //print added variable
    }

    /**
     * Remove a variable from the current symbol list.
     * @param v The variable to remove
     */
    public void removeVariable( Variable v ){
        for(int i=0; i<indentationLevel; i++)
            System.out.print("\t"); // print a tab 'n' times, where n = indentation level
        super.removeVariable(v); //print the removed variable
    }


    /**
     * Indicates that a non-terminal method has been entered.
     * Will print tabs, equal to the level of indentation
     * @param name The name of the non-terminal
     */
    @Override
    public void commenceNonterminal( String name ) {
        for(int i=0; i<indentationLevel; i++)
            System.out.print("\t"); // print a tab 'n' times, where n = indentation level
        super.commenceNonterminal(name);  //indicate the beginning of a non-terminal
        this.indentationLevel++;
    } // end of method commenceNonterminal

    /**
     * Prints the terminal token that was read
     * @param token The terminal token
     */
    @Override
    public void insertTerminal( Token token ) {
        for(int i=0; i<indentationLevel; i++)
            System.out.print("\t"); // print a tab 'n' times, where n = indentation level
        super.insertTerminal(token);  //indicate that a terminal has been accepted
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
            System.out.print("\t"); // print a tab 'n' times, where n = indentation level
        super.finishNonterminal(name);  // indicate the end of a non-terminal
    } // end of method finishNonterminal

    /**
     * This method is called from the SA and throws a CompilationException
     * @param token The token that caused the error
     * @param explanatoryMessage The message that explains the problem
     * @throws CompilationException Every time that this method is called
     */
    @Override
    public void reportError(Token token, String explanatoryMessage) throws CompilationException {
        for(int i=0; i<indentationLevel; i++)
            System.out.print("\t"); // print a tab 'n' times, where n = indentation level
        System.out.println("rggERROR "+ token.toString());  //indicate the error identified
        throw new CompilationException(explanatoryMessage, token.lineNumber);  //throw a compilation exception
    }

    /**
     * This method is called from the SA and throws a CompilationException. It overloads the method with the same name.
     * It is used to pass information about the error already thrown, to the method above, so it can be finally be
     * caught and printed as a stack-trace.
     * @param token The token that caused the error
     * @param explanatoryMessage The message that explains the problem
     * @throws CompilationException Every time that this method is called
     */
    void reportError(Token token, String explanatoryMessage, CompilationException exception)
            throws CompilationException {

        throw new CompilationException(explanatoryMessage, token.lineNumber, exception);
    }
}
