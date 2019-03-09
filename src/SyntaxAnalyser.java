import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Petros Soutzis
 */
public class SyntaxAnalyser extends AbstractSyntaxAnalyser {

    /*Error messages that the SA will use*/
    private final String INV_TKN_ERR = "invalid token: \"{0}\"";
    private final String XPCTD_OTHR_TKN = "invalid token. Expected: token \"{0}\", but got \"{1}\" instead!";
    private final String TRACE_METHOD = "an error caught in method \"{0}\" of the Syntax Analyser. Error is";

    /**
     * Constructor of Syntax Analyser
     * @param filename the name of the file that will be analysed
     * @throws IOException In case that Hulk tries to put his foot in the input stream, or when the file is not found.
     */
    SyntaxAnalyser(String filename) throws IOException {

        this.lex = new LexicalAnalyser(filename);
    }

    /**
     * Method for non-terminal <StatementPart>
     */
    @Override
    public void _statementPart_() throws IOException, CompilationException {
        final String nonTerminalName = "StatementPart";
        myGenerate.commenceNonterminal(nonTerminalName); // Indicate start of Non-terminal recursion

        acceptTerminal(Token.beginSymbol); // first thing to do is accept the begin symbol (terminal)
        try {
            _statementList_();  // call method for non-terminal <statement list>
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        acceptTerminal(Token.endSymbol); // last thing to do is to accept the end symbol (terminal)

        myGenerate.finishNonterminal(nonTerminalName); // Indicate end of Non-terminal recursion
    }

    /**
     * Method for non-terminal <StatementList>
     */
    private void _statementList_() throws IOException, CompilationException{
        final String nonTerminalName = "StatementList";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            _statement_(); //call method for non-terminal <statement>

            // Iff the next token is a semicolon, then accept that terminal symbol and call _statementList_() again
            // otherwise, finish this non-terminal, after returning from _statement_()
            if (nextToken.symbol == Token.semicolonSymbol) {
                acceptTerminal(Token.semicolonSymbol);
                _statementList_();
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);
    }

    /**
     *Method for non-terminal <Statement>
     */
    private void _statement_() throws IOException, CompilationException {
        final String nonTerminalName = "Statement";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            //Switch cases to check if an accepted symbol is read. If not, then default is to throw CompilationError.
            //If one of the following cases is entered, then call the appropriate method for the non-terminal read.
            switch (nextToken.symbol) {
                case Token.identifier: _assignmentStatement_(); break;
                case Token.ifSymbol: _ifStatement_(); break;
                case Token.whileSymbol: _whileStatement_(); break;
                case Token.callSymbol: _procedureStatement_(); break;
                case Token.doSymbol: _untilStatement_(); break;
                case Token.forSymbol: _forStatement_(); break;

                default:
                    myGenerate.reportError(nextToken, MessageFormat.format(INV_TKN_ERR, nextToken.text));
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);
    }

    /**
     * Method for non-terminal <AssignmentStatement>
     */
    private void _assignmentStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "AssignmentStatement";
        myGenerate.commenceNonterminal(nonTerminalName);
        try {
            acceptTerminal(Token.identifier);  // first accept the identifier. If it is a valid identifier, keep on
            acceptTerminal(Token.becomesSymbol); // accept the terminal symbol ':=' that assigns value to identifier

            int symbol = nextToken.symbol;

            // If next symbol is either identifier, number constant of a left parenthesis, then call method
            //for non-terminal <expression>
            if (symbol == Token.identifier || symbol == Token.numberConstant || symbol == Token.leftParenthesis)
                _expression_();
            // Otherwise, if symbol is a string constant, then accept it as a terminal
            else if (symbol == Token.stringConstant)
                acceptTerminal(Token.stringConstant);
            // If everything else fails, then throw an exception, which will be caught later
            else
                myGenerate.reportError(nextToken, MessageFormat.format(INV_TKN_ERR, nextToken.toString()));
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);
    }

    /**
     *Method for non-terminal <if statement>
     */
    private void _ifStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "IfStatement";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            /*1. Accept "if" token*/
            acceptTerminal(Token.ifSymbol);
            /*2. Get the condition and ultimately accept the identifier OR throw an error*/
            _condition_();
            /*3. Accept the "then" token*/
            acceptTerminal(Token.thenSymbol);
            /*4. Call statementList*/
            _statementList_();

            /*5. Check if nextToken is to end the if statement or if it is an else clause.
             * 6. Finally accept the tokens that mark the end of the if statement, if not already accepted.*/
            switch (nextToken.symbol) {
                case Token.endSymbol:
                    acceptTerminal(Token.endSymbol);
                    acceptTerminal(Token.ifSymbol);
                    break;
                case Token.elseSymbol:
                    acceptTerminal(Token.elseSymbol);
                    _statementList_();
                    acceptTerminal(Token.endSymbol);
                    acceptTerminal(Token.ifSymbol);
                    break;
                default:
                    myGenerate.reportError(nextToken, MessageFormat.format(INV_TKN_ERR, nextToken.toString()));
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);
    }

    /**
     * Method for non-terminal <Procedure Statement>.
     * Accepted grammar: call identifier ( <argument list> )
     * @throws IOException In the case of apparently a dinosaur attack on the input stream.
     * @throws CompilationException In the case that an invalid token is parsed during syntax analysis.
     */
    private void _procedureStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "ProcedureStatement";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            acceptTerminal(Token.callSymbol); // accept terminal "call"
            acceptTerminal(Token.identifier); // accept an identifier as terminal
            acceptTerminal(Token.leftParenthesis); // accept a left parenthesis terminal
            _argumentList_(); // call method for non-terminal <argument list>
            acceptTerminal(Token.rightParenthesis); // finally accept a right parenthesis terminal
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);
    }

    /**
     * Method for non-terminal <WhileStatement>
     */
    private void _whileStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "WhileStatement";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            acceptTerminal(Token.whileSymbol); // accept terminal 'while'
            _condition_();  //call method of non-terminal <condition>
            acceptTerminal(Token.loopSymbol); // accept terminal symbol 'loop'
            _statementList_(); // call method for non terminal <statement list>
            acceptTerminal(Token.endSymbol); //accept terminal symbol 'end'
            acceptTerminal(Token.loopSymbol); // accept terminal symbol 'loop'
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);

    }

    /**
     *Method for non-terminal <until statement>
     */
    private void _untilStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "UntilStatement";
        myGenerate.commenceNonterminal(nonTerminalName);

        // execute in this sequence, based on grammar
        try {
            acceptTerminal(Token.doSymbol);
            _statementList_();
            acceptTerminal(Token.untilSymbol);
            _condition_();
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);

    }

    /**
     *Method for non-terminal <for statement>
     */
    private void _forStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "ForStatement";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            acceptTerminal(Token.forSymbol);  // for
            acceptTerminal(Token.leftParenthesis);  // (
            _assignmentStatement_();  // <assignment statement>
            acceptTerminal(Token.semicolonSymbol); // ;
            _condition_(); // <condition>
            acceptTerminal(Token.semicolonSymbol);  // ;
            _assignmentStatement_();  // <assignment statement>
            acceptTerminal(Token.rightParenthesis); // )
            acceptTerminal(Token.doSymbol);  // do
            _statementList_();
            acceptTerminal(Token.endSymbol);  // end
            acceptTerminal(Token.loopSymbol);  // loop
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);

    }

    /**
     *Method for non-terminal <argument list>
     */
    private void _argumentList_() throws IOException, CompilationException {
        final String nonTerminalName = "ArgumentList";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            acceptTerminal(Token.identifier);
            // if next token is a comma, then call this method recursively
            if (nextToken.symbol == Token.commaSymbol) {
                acceptTerminal(Token.commaSymbol);
                _argumentList_();
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }

        myGenerate.finishNonterminal(nonTerminalName);
    }

    /**
     *Method for non-terminal <condition>
     */
    private void _condition_() throws IOException, CompilationException {
        final String nonTerminalName = "Condition";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            acceptTerminal(Token.identifier); //accept an identifier terminal
            _conditionalOperator_();

            // when returning from conditionalOperator, accept one of the predefined terminals, or throw error
            switch (nextToken.symbol) {
                case Token.identifier: acceptTerminal(Token.identifier); break;
                case Token.numberConstant: acceptTerminal(Token.numberConstant); break;
                case Token.stringConstant: acceptTerminal(Token.stringConstant); break;
                default:
                    myGenerate.reportError(nextToken, MessageFormat.format(INV_TKN_ERR, nextToken.toString()));
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);
    }

    /**
     * Method for non-terminal <ConditionalOperator>
     */
    private void _conditionalOperator_() throws IOException, CompilationException {
        final String nonTerminalName = "ConditionalOperator";
        final String expectedTokens = "> or >= or = or /= or < or <="; //the expected tokens (terminals)
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            // accept one of the expected tokens, or throw an error
            switch (nextToken.symbol) {
                case Token.lessThanSymbol: acceptTerminal(Token.lessThanSymbol); break;
                case Token.lessEqualSymbol:acceptTerminal(Token.lessEqualSymbol);break;
                case Token.equalSymbol: acceptTerminal(Token.equalSymbol); break;
                case Token.notEqualSymbol: acceptTerminal(Token.notEqualSymbol); break;
                case Token.greaterThanSymbol: acceptTerminal(Token.greaterThanSymbol); break;
                case Token.greaterEqualSymbol: acceptTerminal(Token.greaterEqualSymbol); break;
                default:
                    myGenerate.reportError(
                            nextToken, MessageFormat.format(XPCTD_OTHR_TKN, expectedTokens, nextToken.toString())
                    );
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);

    }

    /**
     * Method for non-terminal <expression>
     */
    private void _expression_() throws IOException, CompilationException {
        final String nonTerminalName = "Expression";
        myGenerate.commenceNonterminal(nonTerminalName);
        int symbol = nextToken.symbol;

        try {
            // if symbol is either identifier, number constant or a left parenthesis, then call <term>'s method
            if (symbol == Token.identifier || symbol == Token.numberConstant || symbol == Token.leftParenthesis)
                _term_();
            // When returning from _term_(), check if symbol is '+' or '-'. If yes, then call current method recursively
            if (nextToken.symbol == Token.plusSymbol) {
                acceptTerminal(Token.plusSymbol);
                _expression_();
            } else if (nextToken.symbol == Token.minusSymbol) {
                acceptTerminal(Token.minusSymbol);
                _expression_();
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }

        myGenerate.finishNonterminal(nonTerminalName);

    }

    /**
     * Method for non-terminal <term>
     */
    private void _term_() throws IOException, CompilationException {
        final String nonTerminalName = "Term";
        myGenerate.commenceNonterminal(nonTerminalName);
        int symbol = nextToken.symbol;

        try {
            // if symbol is either identifier, number constant or a left parenthesis, then call <factor>'s method
            if (symbol == Token.identifier || symbol == Token.numberConstant || symbol == Token.leftParenthesis)
                _factor_();

            // When returning from _term_(), check if symbol is '*' or '/'. If yes, then call current method recursively
            if (nextToken.symbol == Token.timesSymbol) {
                acceptTerminal(Token.timesSymbol);
                _term_();
            } else if (nextToken.symbol == Token.divideSymbol) {
                acceptTerminal(Token.divideSymbol);
                _term_();
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }

        myGenerate.finishNonterminal(nonTerminalName);
    }

    /**
     * Method for non-terminal <factor>
     */
    private void _factor_() throws IOException, CompilationException {
        final String nonTerminalName = "Factor";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            // based on what the token is, take the appropriate action
            switch (nextToken.symbol) {
                case Token.identifier: acceptTerminal(Token.identifier); break;
                case Token.numberConstant: acceptTerminal(Token.numberConstant); break;
                case Token.leftParenthesis:
                    acceptTerminal(Token.leftParenthesis); // accept left parenthesis if one is read
                    _expression_(); //call expression, but when the (SP) returns, expect a right parenthesis for each call!
                    acceptTerminal(Token.rightParenthesis);
                    break;
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);

    }

    /**
     * This method will accept a token, based on context.
     * @param symbol Is the symbol that the current token (nextToken) will be compared to.
     * @throws IOException Don't we all?
     * @throws CompilationException If the current token is not the one expected to read.
     */
    @Override
    public void acceptTerminal(int symbol) throws IOException, CompilationException {
        int examinedSymbol = nextToken.symbol;

        if (examinedSymbol == symbol){
            myGenerate.insertTerminal(nextToken); //print terminal name to output
            nextToken = lex.getNextToken();  //get next token from lexical analyser
        }
        else {
            myGenerate.reportError(nextToken,
                    MessageFormat.format(XPCTD_OTHR_TKN, Token.getName(symbol), nextToken.text));
        }
    }
}
