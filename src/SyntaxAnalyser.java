import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * This class will analyse the syntax of a program written in a very simple language (a bit similar to Ada) and
 * will print the final results of the analysis, as well as the intermediate steps of the analysis in 2 separate files.
 * @author Petros Soutzis, 34023852
 */
public class SyntaxAnalyser extends AbstractSyntaxAnalyser {

    /*Error messages that the SA will use*/
    private final String INV_TKN_ERR = ": invalid token: \"{0}\"";
    private final String XPCTD_OTHR_TKN = ": invalid token. Expected: token \"{0}\", but got \"{1}\" instead!";
    private final String TRACE_METHOD = ": an error in method \"{0}\" of the Syntax Analyser. Error is";
    private final String VAR_NAME_NOT_EXISTS = ": could not find \"{0}\". This variable has not been initialised yet.";
    private final String INV_OPERATION =  ": invalid operation. It is not possible to perform operation";

    /*These are used to indicate the recursion depth of an expression <_expression_()>
    * I.e. NO_EXPRESSION means that the first expression has not been identified by the syntax analyser and
    * NESTED_EXPRESSION will mean that the syntax analyser has already seen both first and second expressions*/
    private final int NO_EXPRESSION = 0;
    private final int FIRST_EXPRESSION = 1;
    private final int SECOND_EXPRESSION = 2;
    private final int NESTED_EXPRESSION = 3;

    /*Containers of global and local variables respectively*/
    private HashMap<String, Variable> globalVariables;
    private HashMap<Integer, HashMap<String, Variable>> localVariables;

    /*Flags
    * 1) If the SA is currently in the body of a for loop
    * 2) The current scope. (The depth of nesting of for-loop)*/
    private boolean forStatementBody;
    private int forStatementCount;

    /*Flags
    * 1) expressionsSeen marks the number of consecutive (recursive) expressions that the SA parses
    * 2) varType1 and varType2 are the variable types given in an expressions. Used in semantic evaluation*/
    private int expressionsSeen;
    private Variable.Type varType1, varType2;

    /**
     * Constructor of Syntax Analyser
     * @param filename the name of the file that will be analysed
     * @throws IOException In case that Hulk tries to put his foot in the input stream, or any read errors happen
     * during parsing of the file
     */
    SyntaxAnalyser(String filename) throws IOException {

        // Instantiates the Lexical Analyser, so it can be accessed during execution of the parse method
        this.lex = new LexicalAnalyser(filename);
        this.globalVariables = new HashMap<>();
        this.localVariables = new HashMap<>();
        this.forStatementBody = false;
        this.forStatementCount = 0;
        this.expressionsSeen = NO_EXPRESSION;
    }

    /**
     * Method for non-terminal 'StatementPart'.  StatementPart is the distinguished symbol of this language.
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
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

        myGenerate.finishNonterminal(nonTerminalName); // Indicate end of the distinguished symbol
    }

    /**
     * Method for non-terminal 'StatementList'
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _statementList_() throws IOException, CompilationException{
        final String nonTerminalName = "StatementList";
        myGenerate.commenceNonterminal(nonTerminalName); // indicate that a statement list has commenced

        try {
            _statement_(); // parse the statement

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
        myGenerate.finishNonterminal(nonTerminalName); // mark the end of statement list
    }

    /**
     * Method for non-terminal 'Statement'
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _statement_() throws IOException, CompilationException {
        final String nonTerminalName = "Statement";
        myGenerate.commenceNonterminal(nonTerminalName);  //mark the beginning of a 'statement'

        try {
            //Switch cases to check if an accepted symbol is read. If not, then default is to throw CompilationError.
            //If one of the following cases is entered, then call the appropriate method for the non-terminal read.
            switch (nextToken.symbol) {
                case Token.identifier:
                    _assignmentStatement_();
                    break;
                case Token.ifSymbol:
                    _ifStatement_();
                    break;
                case Token.whileSymbol:
                    _whileStatement_();
                    break;
                case Token.callSymbol:
                    _procedureStatement_();
                    break;
                case Token.doSymbol:
                    _untilStatement_();
                    break;
                case Token.forSymbol:
                    if(this.forStatementCount == 0)  // check if this is the most outer loop
                        this.forStatementBody = true; // set flag that indicates we are in a for loop
                    // increment counter by 1, so the analyser can determine whether the outer for loop has finished
                    this.forStatementCount ++;

                    /*ENTER FOR STATEMENT*/
                    _forStatement_();
                    /*EXIT FOR STATEMENT and delete all variables of this for-loop's scope*/

                    //Print the destroyed variables
                    HashMap<String, Variable> scope = localVariables.get(forStatementCount);
                    for(Variable v : scope.values()){
                        myGenerate.removeVariable(v);
                    }

                    localVariables.remove(forStatementCount);//Remove the scope (which contains the destroyed variables)
                    this.forStatementCount --; // decrement by 1, to indicate that for-loop has exited

                    if(this.forStatementCount == 0)  // check if this is the most outer loop
                        this.forStatementBody = false; // set flag that indicates we are in a for loop

                    break;
                // if none of the above cases, throw an error for invalid token
                default:
                    myGenerate.reportError(nextToken, MessageFormat.format(INV_TKN_ERR, nextToken.text));
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName); // mark the end of a 'statement'
    }

    /**
     * Method for non-terminal 'AssignmentStatement'
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _assignmentStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "AssignmentStatement";
        myGenerate.commenceNonterminal(nonTerminalName);  // mark the beginning of an 'assignment statement'
        try {
            String variableIdentifier = nextToken.text;
            Variable v;
            acceptTerminal(Token.identifier);  // first accept the identifier. If it is a valid identifier, keep on
            acceptTerminal(Token.becomesSymbol); // accept the terminal symbol ':=' that assigns value to identifier

            switch(nextToken.symbol){
                // If next symbol is either identifier, number constant of a left parenthesis, then call method
                //for non-terminal <expression>
                case Token.identifier:
                    _expression_();
                    expressionsSeen = NO_EXPRESSION; //when expression returns, reset variable
                    // if no error is thrown after expression has finished, then varType1 is the type of this variable
                    v = new Variable(variableIdentifier, varType1);
                    createVariable(variableIdentifier, v);
                    break;
                case Token.numberConstant:
                    v = new Variable(variableIdentifier, Variable.Type.NUMBER);
                    _expression_();
                    expressionsSeen = NO_EXPRESSION; //when expression returns, reset variable
                    createVariable(variableIdentifier, v);
                    break;
                case Token.leftParenthesis:
                    _expression_();
                    expressionsSeen = NO_EXPRESSION; //when expression returns, reset variable
                    break;
                // Otherwise, if symbol is a string constant, then accept it as a terminal
                case Token.stringConstant:
                    v = new Variable(variableIdentifier, Variable.Type.STRING);
                    acceptTerminal(Token.stringConstant);
                    createVariable(variableIdentifier, v);
                    break;

                // If everything else fails, then throw an exception, which will be caught later
                default: myGenerate.reportError(nextToken, MessageFormat.format(INV_TKN_ERR, nextToken.toString()));
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName);
    }

    /**
     * Method for non-terminal "ifStatement"
     * This method will evaluate an if statement.
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
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
             *6. Finally accept the tokens that mark the end of the if statement, if not already accepted.*/
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
     * Method for non-terminal "Procedure Statement".
     * Accepted grammar: call identifier ( <argument list> )
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed.
     */
    private void _procedureStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "ProcedureStatement";
        myGenerate.commenceNonterminal(nonTerminalName); //mark the beginning of non-terminal

        try {
            acceptTerminal(Token.callSymbol); // accept terminal "call"
            acceptTerminal(Token.identifier); // accept an identifier as terminal (reserved "get")
            acceptTerminal(Token.leftParenthesis); // accept a left parenthesis terminal
            _argumentList_(); // call method for non-terminal <argument list>
            acceptTerminal(Token.rightParenthesis); // finally accept a right parenthesis terminal
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }
        myGenerate.finishNonterminal(nonTerminalName); // mark the end of non-terminal
    }

    /**
     * Method for non-terminal "WhileStatement"
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _whileStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "WhileStatement";
        myGenerate.commenceNonterminal(nonTerminalName); //mark beginning of non terminal

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
        myGenerate.finishNonterminal(nonTerminalName); //mark the end of non-terminal

    }

    /**
     *Method for non-terminal "until statement"
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _untilStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "UntilStatement";
        myGenerate.commenceNonterminal(nonTerminalName); //mark beginning of non-terminal

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
        myGenerate.finishNonterminal(nonTerminalName); //mark the end of nonterminal

    }

    /**
     *Method for non-terminal "for statement"
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
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
     *Method for non-terminal "argument list"
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _argumentList_() throws IOException, CompilationException {
        final String nonTerminalName = "ArgumentList";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            // Check if this variable exists, before accepting it
            if(variableExists(nextToken.text))
                acceptTerminal(Token.identifier);
            // if this variable does not exist, then throw an error
            else
                myGenerate.reportError(nextToken, MessageFormat.format(VAR_NAME_NOT_EXISTS, nextToken.text));
            // if next token is a comma, then call this method recursively
            if (nextToken.symbol == Token.commaSymbol) {
                acceptTerminal(Token.commaSymbol);
                _argumentList_(); //recursive call
            }
        }
        // catch error thrown here and throw a new error, to build a stack trace
        catch (CompilationException ce){
            myGenerate.reportError(nextToken, MessageFormat.format(TRACE_METHOD, nonTerminalName), ce);
        }

        myGenerate.finishNonterminal(nonTerminalName);
    }

    /**
     * Method for non-terminal "Condition".
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _condition_() throws IOException, CompilationException {
        final String nonTerminalName = "Condition";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            // STEP 1. Check if this variable exists
            if(variableExists(nextToken.text))
                acceptTerminal(Token.identifier); //accept an identifier terminal
            // if this variable does not exist, then throw an error
            else
                myGenerate.reportError(nextToken, MessageFormat.format(VAR_NAME_NOT_EXISTS, nextToken.text));

            _conditionalOperator_();

            // when returning from conditionalOperator, accept one of the predefined terminals, or throw error
            switch (nextToken.symbol) {
                case Token.identifier:
                    // STEP 2: Same as STEP 1
                    if(variableExists(nextToken.text))
                        acceptTerminal(Token.identifier); //accept an identifier terminal
                    // if this variable does not exist, then throw an error
                    else
                        myGenerate.reportError(nextToken, MessageFormat.format(VAR_NAME_NOT_EXISTS, nextToken.text));
                    break;
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
     * Method for non-terminal "ConditionalOperator"
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _conditionalOperator_() throws IOException, CompilationException {
        final String nonTerminalName = "ConditionalOperator";
        final String expectedTokens = "> or >= or = or /= or < or <="; //the expected tokens (terminals)
        myGenerate.commenceNonterminal(nonTerminalName); //mark beginning of non terminal

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
        myGenerate.finishNonterminal(nonTerminalName); //mark end of non terminal

    }

    /**
     * Method for non-terminal "expression". This method will not allow a String and a Number in same operation.
     * I.e. You can not do: myVariable := "Hello world" + 128;
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _expression_() throws IOException, CompilationException {
        final String nonTerminalName = "Expression";
        myGenerate.commenceNonterminal(nonTerminalName);

        //Check which part of the sequence this is. If expressionsSeen is No expression, then this is the first.
        //If expressionsSeen is first expression, then this is the second expression. And so on and so forth
        switch (expressionsSeen){
            case NO_EXPRESSION:
                expressionsSeen = FIRST_EXPRESSION;
                break;
            case FIRST_EXPRESSION:
                expressionsSeen = SECOND_EXPRESSION;
                break;
            case SECOND_EXPRESSION:
                expressionsSeen = NESTED_EXPRESSION;
                break;
        }

        try {
            // if symbol is either identifier, number constant or a left parenthesis, then call <term>'s method
            switch (nextToken.symbol) {
                case Token.identifier:
                    String temp = nextToken.text;  //Store this. If _term_() returns, then variable exists. Get it.
                    _term_();
                    // If this is the first expression, assign only varType1
                    if(expressionsSeen == FIRST_EXPRESSION)
                        varType1 = getVariable(temp).type;
                    //If this is the second expression, assign only varType2
                    else if(expressionsSeen == SECOND_EXPRESSION)
                        varType2 = getVariable(temp).type;
                    // If this is a nested expression, deeper than 2 expressions,
                    //then assign the value of varType2 to varType1 and assign current type to varType2
                    else {
                        varType1 = varType2;
                        varType2 = getVariable(temp).type;
                    }
                    break;
                case Token.numberConstant:
                    _term_();
                    if(expressionsSeen == FIRST_EXPRESSION)
                        varType1 = Variable.Type.NUMBER;
                    else if(expressionsSeen == SECOND_EXPRESSION)
                        varType2 = Variable.Type.NUMBER;
                    else {
                        varType1 = varType2;
                        varType2 = Variable.Type.NUMBER;
                    }
                    break;
                case Token.stringConstant:
                    if(expressionsSeen == FIRST_EXPRESSION)
                        varType1 = Variable.Type.STRING;
                    else if(expressionsSeen == SECOND_EXPRESSION)
                        varType2 = Variable.Type.STRING;
                    else {
                        varType1 = varType2;
                        varType2 = Variable.Type.STRING;
                    }
                    break;
                case Token.leftParenthesis:
                    _term_();
                    break;
            }
            //Variable to store temporary token, in order to pass as an error parameter
            Token tempToken;
            // When returning from _term_(), check if symbol is '+' or '-'. If yes, then call current method recursively
            switch (nextToken.symbol) {
                case Token.plusSymbol:
                    acceptTerminal(Token.plusSymbol);
                    tempToken = nextToken;
                    _expression_();
                    //Make sure this is not the first expression, otherwise varType2 will be un-initialised
                    if(expressionsSeen != FIRST_EXPRESSION){
                        //Disallow different types
                        if(varType1 != varType2){
                            myGenerate.reportError(tempToken, INV_OPERATION);
                        }
                    }
                    break;
                case Token.minusSymbol:
                    acceptTerminal(Token.minusSymbol);
                    tempToken = nextToken;
                    _expression_();
                    //make sure this is not the first expression
                    if(expressionsSeen != FIRST_EXPRESSION) {
                        //disallow different types
                        if (varType1 != varType2) {
                            myGenerate.reportError(tempToken, INV_OPERATION);
                        }
                        //disallow subtraction of strings
                        if (varType1 == Variable.Type.STRING || varType2 == Variable.Type.STRING) {
                            myGenerate.reportError(tempToken, INV_OPERATION);
                        }
                    }
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
     * Method for non-terminal "term"
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _term_() throws IOException, CompilationException {
        final String nonTerminalName = "Term";
        myGenerate.commenceNonterminal(nonTerminalName);

        // Store this as temp token, so that it can be passed to reportError
        Token tempToken = null;
        //Variable to store temporary token, in order to pass as an error parameter
        Variable.Type type = Variable.Type.UNKNOWN;

        try {
            // if symbol is either identifier, number constant or a left parenthesis, then call <factor>'s method
            // method _factor_() returns and does not throw an error, then store Type, for check afterwards.
            switch (nextToken.symbol) {
                case Token.identifier:
                    String temp = nextToken.text;  //Store this. If _term_() returns, then variable exists. Get it.
                    tempToken = nextToken;
                    _factor_();
                    type = getVariable(temp).type;
                    break;
                case Token.numberConstant:
                    tempToken = nextToken;
                    _factor_();
                    type = Variable.Type.NUMBER;
                    break;
                case Token.leftParenthesis:
                    _factor_();
                    break;
            }

            // When returning from _factor_(), check if symbol is '*' or '/'. If yes, then call current method recursively
            // If the type of the variable before * or / is String, then disallow
            switch (nextToken.symbol) {
                case Token.timesSymbol:
                    //disallow multiplication of strings
                    if (type == Variable.Type.STRING) {
                        myGenerate.reportError(tempToken, INV_OPERATION);
                    }
                    // accept '*'
                    acceptTerminal(Token.timesSymbol);

                    //get type of token that is after the '*' symbol
                    String temp_identifier1 = nextToken.text;
                    if (nextToken.symbol == Token.identifier)
                        type = getVariable(temp_identifier1).type;
                    else if (nextToken.symbol == Token.stringConstant)
                        type = Variable.Type.STRING;

                    _term_();

                    // check type
                    if (type == Variable.Type.STRING) {
                        myGenerate.reportError(tempToken, INV_OPERATION);
                    }
                    break;
                case Token.divideSymbol:
                    //disallow string division
                    if (type == Variable.Type.STRING) {
                        myGenerate.reportError(tempToken, INV_OPERATION);
                    }
                    //accept '/'
                    acceptTerminal(Token.divideSymbol);

                    String temp_identifier2 = nextToken.text;
                    if (nextToken.symbol == Token.identifier)
                        type = getVariable(temp_identifier2).type;
                    else if (nextToken.symbol == Token.stringConstant)
                        type = Variable.Type.STRING;

                    _term_();

                    if (type == Variable.Type.STRING) {
                        myGenerate.reportError(tempToken, INV_OPERATION);
                    }
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
     * Method for non-terminal "factor"
     * @throws IOException If an i/o exception occurs.
     * @throws CompilationException If an invalid token is parsed
     */
    private void _factor_() throws IOException, CompilationException {
        final String nonTerminalName = "Factor";
        myGenerate.commenceNonterminal(nonTerminalName);

        try {
            // based on what the token is, take the appropriate action
            switch (nextToken.symbol) {
                case Token.identifier:
                    if(variableExists(nextToken.text)) {
                        acceptTerminal(Token.identifier);
                    }
                    // if this variable does not exist, then throw an error
                    else
                        myGenerate.reportError(nextToken, MessageFormat.format(VAR_NAME_NOT_EXISTS, nextToken.text));
                    break;
                case Token.numberConstant:
                    acceptTerminal(Token.numberConstant);
                    break;
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

    /**
     * This method will check the collections of temp variables and global variables. If an identifier does
     * not exist, then create the variable, insert it in the appropriate collection and print that the.
     * variable has been created ("rggDECL {variable}")
     * @param identifier The variable identifier
     * @param v The variable
     */
    private void createVariable(String identifier, Variable v) {
        /*THE COMMENTED CODE-SNIPPET CREATES A NEW VARIABLE, ONLY IF THE VARIABLE IS NOT ALREADY INSTANTIATED*/

//        if(!variableExists(identifier)){
//            myGenerate.addVariable(v);  //declare the creation of this variable
//            //check if we are in a for-loop
//            if(forStatementBody) {
//                //check if the scope already exists. If not exists, a new HashMap will be added
//                if(localVariables.containsKey(forStatementCount))
//                    localVariables.get(forStatementCount).put(identifier, v); //put variable in scope
//                else {
//                    HashMap<String, Variable> scope = new HashMap<>(); //create new hashmap to hold the scope
//                    scope.put(identifier, v); //put local variable of this scope
//                    localVariables.put(forStatementCount, scope); //put scope identifier + scope variables
//                }
//            }
//            else
//                globalVariables.put(identifier, v);
//        }

        /*THIS SNIPPET WILL CREATE A NEW VARIABLE WHEN THERE IS AN ASSIGNMENT*/
        myGenerate.addVariable(v);  //declare the creation of this variable
        //check if we are in a for-loop
        if(forStatementBody) {
            //check if the scope already exists. If not exists, a new HashMap will be added
            if(localVariables.containsKey(forStatementCount))
                localVariables.get(forStatementCount).put(identifier, v); //put/replace variable in scope
            else {
                HashMap<String, Variable> scope = new HashMap<>(); //create new hashmap to hold the scope
                scope.put(identifier, v); //put local variable of this scope
                localVariables.put(forStatementCount, scope); //put scope identifier + scope variables
            }
        }

        //otherwise, put in global-variables container
        else
            globalVariables.put(identifier, v);
    }

    /**
     * Will return true if variable with specific identifier exists (is assigned).
     * @param identifier The identifier of the variable
     * @return true if variable exists
     */
    private boolean variableExists(String identifier){

        //first check temporary variables
        for(HashMap<String, Variable> scope : localVariables.values()){
            for(Map.Entry<String, Variable> entry : scope.entrySet()) {
                if(identifier.equals(entry.getKey()))
                    return true;
            }
        }

        return globalVariables.containsKey(identifier);
    }

    /**
     * @param identifier The name of the variable to retrieve (aka the variable's identifier)
     * @return The variable object that has the name specified in "identifier". Returns null otherwise
     * @throws NullPointerException If the identifier is the name of a variable that does not exist
     */
    private Variable getVariable(String identifier) throws NullPointerException{

        Variable var = null;

        if(globalVariables.containsKey(identifier))
            var = globalVariables.get(identifier);
        else{
            for(HashMap<String, Variable> scope : localVariables.values()){
                for(Variable v : scope.values()) {
                    if(identifier.equals(v.identifier))
                        var = v;
                }
            }
        }

        return var;
    }
}
